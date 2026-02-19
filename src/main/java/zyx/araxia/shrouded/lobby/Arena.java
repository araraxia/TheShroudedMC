package zyx.araxia.shrouded.lobby;

import org.bukkit.Location;
import org.bukkit.World;

public class Arena {

    private final String name;
    private final String world;
    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final int maxPlayers;

    // Runtime state — not persisted to JSON
    private transient boolean inUse = false;
    private transient String usingLobby = null;

    public Arena(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, int maxPlayers) {
        this.name = name;
        this.world = world;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.x2 = x2; this.y2 = y2; this.z2 = z2;
        this.maxPlayers = maxPlayers;
    }

    public String getName()    { return name; }
    public String getWorld()   { return world; }
    public int getX1()         { return x1; }
    public int getY1()         { return y1; }
    public int getZ1()         { return z1; }
    public int getX2()         { return x2; }
    public int getY2()         { return y2; }
    public int getZ2()         { return z2; }
    public int getMaxPlayers() { return maxPlayers; }

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    public boolean isInUse()       { return inUse; }
    public String getUsingLobby()  { return usingLobby; }

    /**
     * Marks this arena as in use by the given lobby.
     *
     * @return false if the arena is already claimed by another lobby.
     */
    public boolean claim(String lobbyName) {
        if (inUse) return false;
        inUse = true;
        usingLobby = lobbyName;
        return true;
    }

    /** Releases the arena, making it available for use again. */
    public void release() {
        inUse = false;
        usingLobby = null;
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
