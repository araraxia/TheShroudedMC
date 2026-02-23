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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import zyx.araxia.shrouded.TheShrouded;
import zyx.araxia.shrouded.game.PlayerClass;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.menu.ClassSelectMenu;

public class LobbyManager {

    private static final Logger LOGGER = JavaPlugin.getPlugin(TheShrouded.class).getLogger();

    /**
     * Result codes returned by {@link #addPlayerToSession}.
     */
    public enum JoinSessionResult {
        /**
         * Player successfully joined the lobby.
         */
        SUCCESS,
        /**
         * Player is already a member of this session.
         */
        ALREADY_IN_LOBBY,
        /**
         * The lobby has reached its maximum player count.
         */
        LOBBY_FULL,
        /**
         * The lobby's world is not loaded on this server.
         */
        WORLD_NOT_FOUND,
        /**
         * An error occurred while reading or writing the player's file.
         */
        PLAYER_FILE_ERROR,
        /**
         * The player's file already exists and needs to be handled to avoid
         * overwrite.
         */
        PLAYER_FILE_EXISTS,
        /**
         * An unknown error occurred.
         */
        UNKNOWN_ERROR
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
        LOGGER.log(Level.FINE,
                "[TheShrouded] Registering lobby ''{0}'' with world={1}, bounds=({2},{3},{4}) to ({5},{6},{7}), maxPlayers={8}",
                new Object[] { name, world, x1, y1, z1, x2, y2, z2, maxPlayers });
        Lobby lobby = new Lobby(name, world, x1, y1, z1, x2, y2, z2, maxPlayers);
        lobbies.put(name, lobby);
        sessions.put(name, new LobbySession(lobby, plugin, arenaManager));
        return saveLobby(lobby);
    }

    /**
     * Appends a join sign location to an existing lobby and re-saves the JSON
     * file.
     *
     * @return false if the lobby does not exist or the file could not be
     *         written.
     */
    public boolean registerSign(String lobbyName, String world, int x, int y, int z) {
        LOGGER.log(Level.FINE, "[TheShrouded] Registering join sign for lobby ''{0}'' at {1}:{2},{3},{4}",
                new Object[] { lobbyName, world, x, y, z });
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
     * @return false if the lobby does not exist or the file could not be
     *         written.
     */
    public boolean registerLeaveSign(String lobbyName, String world, int x, int y, int z) {
        LOGGER.log(Level.FINE, "[TheShrouded] Registering leave sign for lobby ''{0}'' at {1}:{2},{3},{4}",
                new Object[] { lobbyName, world, x, y, z });
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
     * @return the matching session, or null if no registered leave sign
     *         matches.
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
     * Adds a player to the given lobby session: validates preconditions, saves
     * the player's current state (world, location, inventory, equipment, XP,
     * and active potion effects) to a per-UUID JSON file inside the
     * {@code playerData} directory, teleports the player to the lobby spawn,
     * and opens the class-select menu.
     *
     * <p>
     * Returns {@link JoinSessionResult#PLAYER_FILE_EXISTS} (with a warning sent
     * to the player) if a data file for this UUID already exists, to prevent
     * silent overwrites of a previous saved state.
     */
    public JoinSessionResult addPlayerToSession(Player player, LobbySession session) {
        LOGGER.log(Level.FINE, "[TheShrouded] Adding player {0} ({1}) to session {2}",
                new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName() });

        // Check join validity
        JoinSessionResult validity = joinSessionValidity(session, player);
        if (validity != null) {
            return validity;
        }

        // Save player state before teleporting
        JoinSessionResult snapshotResult = savePlayerSnapshot(player, session);
        if (snapshotResult != null) {
            return snapshotResult;
        }

        // Teleport player to lobby spawn and open class select menu
        if (!movePlayerToLobby(player, session)) {
            return JoinSessionResult.UNKNOWN_ERROR;
        }

        session.add(player);
        // Give the player the class-selector item so they can re-open the menu
        // at any time during the lobby phase. The item carries plugin NBT tags
        // (shrouded:is_shrouded_item + shrouded:item_type) so it can be swept
        // from inventories on session end without touching the player's saved gear.
        player.getInventory().addItem(ShroudedItems.createClassSelector());
        ClassSelectMenu.open(player);

        LOGGER.log(Level.INFO, "[TheShrouded] Player {0} ({1}) joined lobby session {2}",
                new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName() });

        return JoinSessionResult.SUCCESS;
    }

    private boolean movePlayerToLobby(Player player, LobbySession session) {
        try {
            Lobby lobby = session.getLobby();
            World world = Bukkit.getWorld(lobby.getWorld());
            if (world == null) {
                LOGGER.log(Level.WARNING,
                        "[TheShrouded] Failed to move player {0} ({1}) to lobby ''{2}'': world ''{3}'' not found",
                        new Object[] { player.getName(), player.getUniqueId(), lobby.getName(), lobby.getWorld() });
                return false;
            }
            player.teleport(lobby.getSpawnLocation(world));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "[TheShrouded] Failed to move player {0} ({1}) to lobby session {2}: {3}",
                    new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName(), e.getMessage() });
            return false;
        }
    }

    /**
     * Saves a snapshot of the player's current state to a JSON file. Returns null
     * on success or an appropriate JoinSessionResult on failure.
     *
     * @param player the Player whose state is being saved
     * @param session the LobbySession the player is joining
     * @return null if the snapshot was saved successfully, or a JoinSessionResult indicating the error
     */
    private JoinSessionResult savePlayerSnapshot(Player player, LobbySession session) {
        File playerFile = getPlayerFile(player);
        if (checkExistingPlayerFile(playerFile, player, session)) {
            return JoinSessionResult.PLAYER_FILE_EXISTS;
        }

        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        try (Writer writer = new FileWriter(playerFile, StandardCharsets.UTF_8)) {
            LOGGER.log(Level.INFO, "[TheShrouded] Saving snapshot for player {0} ({1})",
                    new Object[] { player.getName(), player.getUniqueId() });
            gson.toJson(snapshot, writer);
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "[TheShrouded] Failed to save player snapshot for {0}: {1}",
                    new Object[] { player.getName(), e.getMessage() });
            return JoinSessionResult.PLAYER_FILE_ERROR;
        }
        return null;
    }

    /**
     * Checks whether a player can join a lobby session, 
     * returning null if valid or an appropriate JoinSessionResult if not. 
     * Does not check for the presence of an existing player file, 
     * which should be handled separately to allow for custom handling of that case 
     * (e.g. prompting the player to resolve the conflict rather than outright denying the join attempt).
     * 
     * @param session the LobbySession the player is attempting to join
     * @param player the Player attempting to join the session
     * 
     * @return null if the player can join the session, or a JoinSessionResult indicating why they cannot join.
     */
    private JoinSessionResult joinSessionValidity(LobbySession session, Player player) {
        if (session.contains(player.getUniqueId())) {
            LOGGER.log(Level.FINE, "[TheShrouded] Player {0} ({1}) is already in session {2}",
                    new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName() });
            return JoinSessionResult.ALREADY_IN_LOBBY;
        }
        if (session.isFull()) {
            LOGGER.log(Level.FINE, "[TheShrouded] Session {0} is full; cannot add player {1} ({2})",
                    new Object[] { session.getLobby().getName(), player.getName(), player.getUniqueId() });
            return JoinSessionResult.LOBBY_FULL;
        }
        
        Lobby lobby = session.getLobby();
        World world = Bukkit.getWorld(lobby.getWorld());
        if (world == null) {
            LOGGER.log(Level.WARNING,
                    "[TheShrouded] Lobby ''{0}'' has world ''{1}'' which is not loaded on this server",
                    new Object[] { lobby.getName(), lobby.getWorld() });
            return JoinSessionResult.WORLD_NOT_FOUND;
        }
        return null;
    }

    /**
     * Return the file for storing a player's snapshot
     *
     * @param player the Player object representing the player to get the file
     *               for
     * @return the File object representing the player's snapshot file
     * @throws IOException if the playerData directory cannot be created
     */
    private File getPlayerFile(Player player) {
        File playerDataDir = new File(plugin.getDataFolder(), "playerData");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }

        UUID uuid = player.getUniqueId();
        return new File(playerDataDir, uuid + ".json");
    }

    /**
     * Check if a player's snapshot file already exists
     *
     * @param playerFile the File object representing the player's snapshot file
     * @param player     the Player object
     * @param session    the LobbySession the player is attempting to join
     * @return true if the player's snapshot file already exists, false
     *         otherwise
     */
    private boolean checkExistingPlayerFile(File playerFile, Player player, LobbySession session) {
        if (playerFile.exists()) {
            player.sendMessage("§c A saved state already exists for your account. "
                    + "You cannot join a lobby until your previous session has been resolved.");
            LOGGER.log(Level.WARNING,
                    "[TheShrouded] Player {0} ({1}) attempted to join session {2} but their player file already exists: {3}",
                    new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName(),
                            playerFile.getAbsolutePath() });
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the player is currently inside any lobby session.
     */
    public boolean isPlayerInSession(Player player) {
        for (LobbySession session : sessions.values()) {
            if (session.contains(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a player from whichever lobby session they are currently in.
     *
     * @param player the Player to remove from their current session
     * @return true if the player was found in a session and removed.
     */
    public boolean removePlayerFromSession(Player player) {
        for (LobbySession session : sessions.values()) {
            if (session.contains(player.getUniqueId())) {
                session.remove(player.getUniqueId());
                LOGGER.log(Level.INFO, "[TheShrouded] Player {0} ({1}) left lobby session {2}",
                        new Object[] { player.getName(), player.getUniqueId(), session.getLobby().getName() });
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
        LOGGER.log(Level.FINE, "[TheShrouded] Registering arena ''{0}'' to lobby ''{1}''",
                new Object[] { arenaName, lobbyName });
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
        LOGGER.log(Level.FINE, "[TheShrouded] Setting countdown for lobby ''{0}'' to {1} seconds",
                new Object[] { lobbyName, seconds });
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        lobby.setStartCountdownSeconds(seconds);
        return saveLobby(lobby);
    }

    /**
     * Sets (or clears) the spawn point for a lobby and re-saves the JSON file.
     * The coordinates must lie within the lobby's bounding box.
     *
     * @return false if the lobby does not exist, the location is out of bounds,
     *         or the file could not be written.
     */
    public boolean setLobbySpawn(String lobbyName, double x, double y, double z,
            float yaw, float pitch) {
        LOGGER.log(Level.FINE, "[TheShrouded] Setting spawn point for lobby ''{0}'' to ({1}, {2}, {3}, {4}, {5})",
                new Object[] { lobbyName, x, y, z, yaw, pitch });
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            return false;
        }
        if (!lobby.isWithinBounds(x, y, z)) {
            return false;
        }
        lobby.setSpawnPoint(new Lobby.SpawnPoint(x, y, z, yaw, pitch));
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
        LOGGER.log(Level.FINE, "[TheShrouded] Removing arena ''{0}'' from lobby ''{1}''",
                new Object[] { arenaName, lobbyName });
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
     * countdown and immediately invoking {@link LobbySession#startMatch()}.
     *
     * @return false if the lobby does not exist or has no active session.
     */
    public boolean forceStartSession(String lobbyName) {
        LobbySession session = sessions.get(lobbyName);
        if (session == null) {
            return false;
        }
        session.forceStart();
        LOGGER.log(Level.FINE, "[TheShrouded] Force-starting session for lobby ''{0}''", new Object[] { lobbyName });
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
                    LOGGER.log(
                            Level.INFO,
                            "[TheShrouded] Loaded lobby ''{0}'' (countdown: {1}s).",
                            new Object[] { lobby.getName(), lobby.getStartCountdownSeconds() });
                }
            } catch (IOException e) {
                LOGGER.log(
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
            LOGGER.log(
                    Level.INFO,
                    "[TheShrouded] Saving lobby ''{0}'' to file",
                    new Object[] { lobby.getName() });
            gson.toJson(lobby, writer);
            return true;
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "[TheShrouded] Failed to save lobby ''{0}'': {1}",
                    new Object[] { lobby.getName(), e.getMessage() });
            return false;
        }
    }
}
