package zyx.araxia.shrouded.lobby;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ArenaManager {
    // TODO: Add command to save arena block data to a file that will be used to
    // reload the arena at the start of each match, allowing for block
    // build/break mid-match without permanently altering the arena. This will
    // also allow for easy sharing of arenas between servers and players.

    private static final String ARENAS_FOLDER = "arenas";

    private final JavaPlugin plugin;
    private final Gson gson;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadAll();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    public boolean arenaExists(String name) {
        return arenas.containsKey(name);
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    /**
     * Returns the first registered arena whose bounding box contains
     * {@code location}, or {@code null} if the location is not inside any
     * arena.
     */
    public Arena getArenaContaining(org.bukkit.Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(location)) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Adds a player-class spawn point to the named arena and persists the change.
     *
     * @return false if the arena does not exist or the file could not be written.
     */
    public boolean addPlayerSpawn(String arenaName, double x, double y, double z,
            float yaw, float pitch) {
        Arena arena = arenas.get(arenaName);
        if (arena == null)
            return false;
        arena.addPlayerSpawn(new Arena.SpawnPoint(x, y, z, yaw, pitch));
        return saveArena(arena);
    }

    /**
     * Adds a Shrouded spawn point to the named arena and persists the change.
     *
     * @return false if the arena does not exist or the file could not be written.
     */
    public boolean addShroudedSpawn(String arenaName, double x, double y, double z,
            float yaw, float pitch) {
        Arena arena = arenas.get(arenaName);
        if (arena == null)
            return false;
        arena.addShroudedSpawn(new Arena.SpawnPoint(x, y, z, yaw, pitch));
        return saveArena(arena);
    }

    /**
     * Creates a new arena and writes it to arenas/<arena_name>.json in the
     * plugin data folder. Overwrites any existing arena with the same name.
     */
    public boolean registerArena(String name, String world, int x1, int y1,
            int z1, int x2, int y2, int z2, int maxPlayers) {
        Arena arena = new Arena(name, world, x1, y1, z1, x2, y2, z2,
                maxPlayers);
        arenas.put(name, arena);
        return saveArena(arena);
    }

    // -------------------------------------------------------------------------
    // File I/O
    // -------------------------------------------------------------------------
    /**
     * Reads every .json file in the plugin's arenas/ subfolder on startup.
     */
    private void loadAll() {
        File folder = arenasFolder();
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
                Arena arena = gson.fromJson(reader, Arena.class);
                if (arena != null) {
                    arenas.put(arena.getName(), arena);
                    plugin.getLogger().log(Level.INFO, "Loaded arena: {0}",
                            arena.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load arena file: {0} - {1}", new Object[] {
                                file.getName(), e.getMessage()
                        });
            }
        }
    }

    private boolean saveArena(Arena arena) {
        File folder = arenasFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, arena.getName() + ".json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(arena, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to save arena ''{0}'': {1}", new Object[] {
                            arena.getName(), e.getMessage()
                    });
            return false;
        }
    }

    private File arenasFolder() {
        return new File(plugin.getDataFolder(), ARENAS_FOLDER);
    }
}
