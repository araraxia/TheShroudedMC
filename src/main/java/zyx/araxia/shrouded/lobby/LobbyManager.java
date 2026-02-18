package zyx.araxia.shrouded.lobby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import zyx.araxia.shrouded.game.PlayerClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LobbyManager {

    private final JavaPlugin plugin;
    private final Gson gson;
    private final Map<String, Lobby> lobbies = new HashMap<>();
    private final Map<String, LobbySession> sessions = new HashMap<>();

    public LobbyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadAll();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public boolean lobbyExists(String name) {
        return lobbies.containsKey(name);
    }

    /**
     * Creates a new lobby and writes it to <lobby_name>.json in the plugin folder.
     * Overwrites any existing lobby with the same name.
     */
    public boolean registerLobby(String name, String world,
                                  int x1, int y1, int z1,
                                  int x2, int y2, int z2,
                                  int maxPlayers) {
        Lobby lobby = new Lobby(name, world, x1, y1, z1, x2, y2, z2, maxPlayers);
        lobbies.put(name, lobby);
        sessions.put(name, new LobbySession(lobby));
        return saveLobby(lobby);
    }

    /**
     * Appends a sign location to an existing lobby and re-saves the JSON file.
     *
     * @return false if the lobby does not exist or the file could not be written.
     */
    public boolean registerSign(String lobbyName, String world, int x, int y, int z) {
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) return false;
        lobby.addSign(new Lobby.SignLocation(world, x, y, z));
        return saveLobby(lobby);
    }

    /**
     * Finds the active session whose lobby has a sign at the given world/coords.
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
     * Stores the chosen class for a player in whichever session they currently belong to.
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

    /** Reads every .json file in the plugin data folder on startup. */
    private void loadAll() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Lobby lobby = gson.fromJson(reader, Lobby.class);
                if (lobby != null) {
                    lobbies.put(lobby.getName(), lobby);
                    sessions.put(lobby.getName(), new LobbySession(lobby));
                    plugin.getLogger().info("Loaded lobby: " + lobby.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load lobby file: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private boolean saveLobby(Lobby lobby) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, lobby.getName() + ".json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(lobby, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save lobby '" + lobby.getName() + "': " + e.getMessage());
            return false;
        }
    }
}
