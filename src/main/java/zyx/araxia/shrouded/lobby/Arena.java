package zyx.araxia.shrouded.lobby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

public class Arena {

    // -------------------------------------------------------------------------
    // Spawn-point record (Gson-serialisable)
    // -------------------------------------------------------------------------

    /**
     * A single named spawn location inside an arena, with look direction.
     * Stored as plain primitives so Gson serialises it without any adapter.
     */
    public static class SpawnPoint {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public SpawnPoint(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Location toLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    // -------------------------------------------------------------------------

    private final String name;
    private final String world;
    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final int maxPlayers;

    /**
     * Spawn points used for non-Shrouded (player-class) participants.
     * Null when loaded from old JSON that pre-dates this field — treated as empty.
     */
    private List<SpawnPoint> playerSpawns;

    /**
     * Spawn points used for the Shrouded player.
     * Null when loaded from old JSON that pre-dates this field — treated as empty.
     */
    private List<SpawnPoint> shroudedSpawns;

    // Runtime state — not persisted to JSON
    private transient boolean inUse = false;
    private transient String usingLobby = null;

    public Arena(String name, String world, int x1, int y1, int z1, int x2,
            int y2, int z2, int maxPlayers) {
        this.name = name;
        this.world = world;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.maxPlayers = maxPlayers;
        this.playerSpawns = new ArrayList<>();
        this.shroudedSpawns = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getZ2() {
        return z2;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    public boolean isInUse() {
        return inUse;
    }

    public String getUsingLobby() {
        return usingLobby;
    }

    /**
     * Marks this arena as in use by the given lobby.
     *
     * @return false if the arena is already claimed by another lobby.
     */
    public boolean claim(String lobbyName) {
        if (inUse)
            return false;
        inUse = true;
        usingLobby = lobbyName;
        return true;
    }

    /** Releases the arena, making it available for use again. */
    public void release() {
        inUse = false;
        usingLobby = null;
    }

    // -------------------------------------------------------------------------
    // Spawn management
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable view of all registered player-class spawn points. */
    public List<SpawnPoint> getPlayerSpawns() {
        return playerSpawns != null
                ? Collections.unmodifiableList(playerSpawns)
                : Collections.emptyList();
    }

    /** Returns an unmodifiable view of all registered Shrouded spawn points. */
    public List<SpawnPoint> getShroudedSpawns() {
        return shroudedSpawns != null
                ? Collections.unmodifiableList(shroudedSpawns)
                : Collections.emptyList();
    }

    /** Appends a spawn point to the player-class list. */
    public void addPlayerSpawn(SpawnPoint sp) {
        if (playerSpawns == null)
            playerSpawns = new ArrayList<>();
        playerSpawns.add(sp);
    }

    /** Appends a spawn point to the Shrouded list. */
    public void addShroudedSpawn(SpawnPoint sp) {
        if (shroudedSpawns == null)
            shroudedSpawns = new ArrayList<>();
        shroudedSpawns.add(sp);
    }

    /**
     * Returns the player-class spawn for the given zero-based index,
     * cycling round-robin. Falls back to the arena centre when none are set.
     */
    public Location getPlayerSpawnAt(int index, World world) {
        List<SpawnPoint> list = getPlayerSpawns();
        if (!list.isEmpty())
            return list.get(index % list.size()).toLocation(world);
        return getSpawnLocation(world);
    }

    /**
     * Returns the Shrouded spawn for the given zero-based index,
     * cycling round-robin. Falls back to the arena centre when none are set.
     */
    public Location getShroudedSpawnAt(int index, World world) {
        List<SpawnPoint> list = getShroudedSpawns();
        if (!list.isEmpty())
            return list.get(index % list.size()).toLocation(world);
        return getSpawnLocation(world);
    }

    /**
     * Returns {@code true} if {@code location} lies within this arena's
     * bounding box (inclusive on all faces). The world name must match.
     */
    public boolean contains(org.bukkit.Location location) {
        if (location.getWorld() == null
                || !location.getWorld().getName().equals(world)) {
            return false;
        }
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        return bx >= Math.min(x1, x2) && bx <= Math.max(x1, x2)
                && by >= Math.min(y1, y2) && by <= Math.max(y1, y2)
                && bz >= Math.min(z1, z2) && bz <= Math.max(z1, z2);
    }

    /**
     * Returns the center of the arena region at floor level, used as the
     * teleport/spawn destination when a match begins.
     */
    public Location getSpawnLocation(World bukkitWorld) {
        double cx = (x1 + x2) / 2.0 + 0.5;
        double cy = Math.min(y1, y2);
        double cz = (z1 + z2) / 2.0 + 0.5;
        return new Location(bukkitWorld, cx, cy, cz);
    }
}
