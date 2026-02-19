package zyx.araxia.shrouded.lobby;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import zyx.araxia.shrouded.game.PlayerClass;
import zyx.araxia.shrouded.menu.ClassSelectMenu;

public class LobbyManager {

    /** Result codes returned by {@link #addPlayerToSession}. */
    public enum JoinSessionResult {
        /** Player successfully joined the lobby. */
        SUCCESS,
        /** Player is already a member of this session. */
        ALREADY_IN_LOBBY,
        /** The lobby has reached its maximum player count. */
        LOBBY_FULL,
        /** The lobby's world is not loaded on this server. */
        WORLD_NOT_FOUND
    }

    private final JavaPlugin plugin;
    private final Gson gson;
    private final Map<String, Lobby> lobbies = new HashMap<>();
    private final Map<String, LobbySession> sessions = new HashMap<>();
    private ArenaManager arenaManager;

    public LobbyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadAll();
    }

    /**
     * Injects the ArenaManager after construction. Must be called before any
     * session attempts to start a match.
     */
    public void setArenaManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    public boolean lobbyExists(String name) {
        return lobbies.containsKey(name);
    }

    /**
     * Creates a new lobby and writes it to <lobby_name>.json in the plugin
     * folder. Overwrites any existing lobby with the same name.
     */
    public boolean registerLobby(String name, String world,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            int maxPlayers) {
        Lobby lobby = new Lobby(name, world, x1, y1, z1, x2, y2, z2, maxPlayers);
        lobbies.put(name, lobby);
        sessions.put(name, new LobbySession(lobby, plugin, arenaManager));
        return saveLobby(lobby);
    }

    /**
     * Appends a join sign location to an existing lobby and re-saves the JSON file.
     *
     * @return false if the lobby does not exist or the file could not be written.
     */
    public boolean registerSign(String lobbyName, String world, int x, int y, int z) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        lobby.addSign(new Lobby.SignLocation(world, x, y, z));
        return saveLobby(lobby);
    }

    /**
     * Appends a leave sign location to an existing lobby and re-saves the JSON
     * file.
     *
     * @return false if the lobby does not exist or the file could not be written.
     */
    public boolean registerLeaveSign(String lobbyName, String world, int x, int y, int z) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        lobby.addLeaveSign(new Lobby.SignLocation(world, x, y, z));
        return saveLobby(lobby);
    }

    /**
     * Finds the active session whose lobby has a join sign at the given
     * world/coords.
     *
     * @return the matching session, or null if no registered sign matches.
     */
    public LobbySession getSessionBySign(String world, int x, int y, int z) {
        for (LobbySession session : sessions.values()) {
            for (Lobby.SignLocation sign : session.getLobby().getSigns()) {
                if (sign.getWorld().equals(world)
                        && sign.getX() == x
                        && sign.getY() == y
                        && sign.getZ() == z) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * Finds the active session whose lobby has a leave sign at the given
     * world/coords.
     *
     * @return the matching session, or null if no registered leave sign matches.
     */
    public LobbySession getSessionByLeaveSign(String world, int x, int y, int z) {
        for (LobbySession session : sessions.values()) {
            for (Lobby.SignLocation sign : session.getLobby().getLeaveSigns()) {
                if (sign.getWorld().equals(world)
                        && sign.getX() == x
                        && sign.getY() == y
                        && sign.getZ() == z) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * Adds a player to the given lobby session: validates preconditions,
     * teleports the player to the lobby spawn, opens the class-select menu,
     * and returns a result code the caller can use to send feedback.
     *
     * <p>
     * TODO: Store the player's inventory and equipment so it can be
     * restored when they leave.
     * <p>
     * TODO: Clear the player's inventory and give them lobby items
     * (class selector, leave item, etc.) with appropriate tags.
     */
    public JoinSessionResult addPlayerToSession(Player player, LobbySession session) {
        if (session.contains(player.getUniqueId())) {
            return JoinSessionResult.ALREADY_IN_LOBBY;
        }
        if (session.isFull()) {
            return JoinSessionResult.LOBBY_FULL;
        }

        Lobby lobby = session.getLobby();
        World world = Bukkit.getWorld(lobby.getWorld());
        if (world == null) {
            return JoinSessionResult.WORLD_NOT_FOUND;
        }

        player.teleport(lobby.getSpawnLocation(world));
        session.add(player);
        ClassSelectMenu.open(player);
        return JoinSessionResult.SUCCESS;
    }

    /**
     * Removes a player from whichever lobby session they are currently in.
     *
     * @return true if the player was found in a session and removed.
     */
    public boolean removePlayerFromSession(Player player) {
        for (LobbySession session : sessions.values()) {
            if (session.contains(player.getUniqueId())) {
                session.remove(player.getUniqueId());
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an arena to a lobby's valid_arenas list and re-saves the lobby JSON.
     *
     * @return false if the lobby does not exist, the arena name is already
     *         registered, or the file could not be written.
     */
    public boolean registerArenaToLobby(String lobbyName, String arenaName) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        if (!lobby.addValidArena(arenaName)) {
            return false;
        }
        return saveLobby(lobby);
    }

    /**
     * Sets the countdown timer for a lobby and re-saves the JSON file.
     *
     * @return false if the lobby does not exist or the file could not be
     *         written.
     */
    public boolean setCountdown(String lobbyName, int seconds) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        lobby.setStartCountdownSeconds(seconds);
        return saveLobby(lobby);
    }

    /**
     * Removes an arena from a lobby's valid_arenas list and re-saves the lobby
     * JSON.
     *
     * @return false if the lobby does not exist, the arena was not listed, or
     *         the file could not be written.
     */
    public boolean removeArenaFromLobby(String lobbyName, String arenaName) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        if (!lobby.removeValidArena(arenaName)) {
            return false;
        }
        return saveLobby(lobby);
    }

    /**
     * Force-starts the session for the named lobby, cancelling any pending
     * countdown and immediately invoking {@link LobbySession#startSession()}.
     *
     * @return false if the lobby does not exist or has no active session.
     */
    public boolean forceStartSession(String lobbyName) {
        LobbySession session = sessions.get(lobbyName);
        if (session == null) {
            return false;
        }
        session.forceStart();
        return true;
    }

    /**
     * Stores the chosen class for a player in whichever session they currently
     * belong to.
     */
    public void setPlayerClass(Player player, PlayerClass playerClass) {
        for (LobbySession session : sessions.values()) {
            if (session.contains(player.getUniqueId())) {
                session.setClass(player.getUniqueId(), playerClass);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // File I/O
    // -------------------------------------------------------------------------
    /**
     * Reads every .json file in the plugin data folder on startup.
     */
    private void loadAll() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Lobby lobby = gson.fromJson(reader, Lobby.class);
                if (lobby != null) {
                    lobbies.put(lobby.getName(), lobby);
                    sessions.put(lobby.getName(), new LobbySession(lobby, plugin, arenaManager));
                    plugin.getLogger().log(
                            Level.INFO,
                            "[TheShrouded] Loaded lobby ''{0}'' (countdown: {1}s).",
                            new Object[] { lobby.getName(), lobby.getStartCountdownSeconds() });
                }
            } catch (IOException e) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "[TheShrouded] Failed to load lobby file: {0} - {1}",
                        new Object[] { file.getName(), e.getMessage() });
            }
        }
    }

    private boolean saveLobby(Lobby lobby) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, lobby.getName() + ".json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(lobby, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "[TheShrouded] Failed to save lobby ''{0}'': {1}",
                    new Object[] { lobby.getName(), e.getMessage() });
            return false;
        }
    }
}
