package zyx.araxia.shrouded.lobby;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

public class Lobby {

    private final String name;
    private final String world;
    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final int maxPlayers;
    private int startCountdownSeconds;
    private final List<SignLocation> signs = new ArrayList<>();
    private final List<SignLocation> leaveSigns = new ArrayList<>();
    private final List<String> validArenas = new ArrayList<>();

    public Lobby(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, int maxPlayers) {
        this.name = name;
        this.world = world;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.x2 = x2; this.y2 = y2; this.z2 = z2;
        this.maxPlayers = maxPlayers;
        this.startCountdownSeconds = 30;
    }

    public String getName()     { return name; }
    public String getWorld()    { return world; }
    public int getX1()          { return x1; }
    public int getY1()          { return y1; }
    public int getZ1()          { return z1; }
    public int getX2()          { return x2; }
    public int getY2()          { return y2; }
    public int getZ2()          { return z2; }
    public int getMaxPlayers()  { return maxPlayers; }

    /**
     * How long to wait (in seconds) after the second player joins before
     * starting the game. Falls back to 30 if the value is missing from JSON.
     */
    public int getStartCountdownSeconds() {
        return startCountdownSeconds > 0 ? startCountdownSeconds : 30;
    }

    public void setStartCountdownSeconds(int seconds) {
        this.startCountdownSeconds = seconds;
    }

    public List<SignLocation> getSigns() { return signs; }

    public List<SignLocation> getLeaveSigns() { return leaveSigns; }

    /**
     * Returns the center of the lobby region at floor level, used as the teleport destination.
     */
    public Location getSpawnLocation(World bukkitWorld) {
        double cx = (x1 + x2) / 2.0 + 0.5;
        double cy = Math.min(y1, y2);
        double cz = (z1 + z2) / 2.0 + 0.5;
        return new Location(bukkitWorld, cx, cy, cz);
    }

    public void addSign(SignLocation sign) {
        signs.add(sign);
    }

    public void addLeaveSign(SignLocation sign) {
        leaveSigns.add(sign);
    }

    /** Returns the mutable list of arena names that are valid for this lobby. */
    public List<String> getValidArenas() { return validArenas; }

    /**
     * Registers an arena name as valid for this lobby.
     *
     * @return false if the arena is already in the list.
     */
    public boolean addValidArena(String arenaName) {
        if (validArenas.contains(arenaName)) return false;
        validArenas.add(arenaName);
        return true;
    }

    /**
     * Removes an arena name from this lobby's valid arenas.
     *
     * @return false if the arena was not in the list.
     */
    public boolean removeValidArena(String arenaName) {
        return validArenas.remove(arenaName);
    }

    // -------------------------------------------------------------------------

    public static class SignLocation {
        private final String world;
        private final int x, y, z;

        public SignLocation(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getWorld() { return world; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
    }
}
