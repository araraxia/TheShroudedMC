package zyx.araxia.shrouded.lobby;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


import zyx.araxia.shrouded.game.PlayerClass;

/**
 * Tracks the players currently inside a lobby and their chosen class. This is a
 * runtime-only object — it is not persisted to JSON.
 */
public class LobbySession {

    private final Lobby lobby;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    
    // null value = player joined but hasn't picked a class yet
    private final Map<UUID, PlayerClass> players = new HashMap<>();
    private final Map<UUID, Instant> joinTimes = new HashMap<>();
    private final String lobbyName;

    private BukkitTask countdownTask = null;

    public LobbySession(Lobby lobby, JavaPlugin plugin) {
        this.lobby = lobby;
        this.lobbyName = lobby.getName();
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public Lobby getLobby() {
        return lobby;
    }

    public boolean isFull() {
        return players.size() >= lobby.getMaxPlayers();
    }

    public boolean contains(UUID uuid) {
        return players.containsKey(uuid);
    }

    /**
     * Adds a player to the session. Starts the countdown if this is the second
     * player to join.
     *
     * @return false if the lobby is already full.
     */
    public boolean add(Player player) {
        if (isFull()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        players.put(uuid, null);
        joinTimes.put(uuid, Instant.now());
        logger.log(
            Level.FINE,
            "Player {0} joined lobby '{1}' (total players: {2}).",
            new Object[]{player.getName(), lobbyName, players.size()}
        );

        if (players.size() >= 2 && countdownTask == null) {
            startCountdown();
        }
        return true;
    }

    /**
     * Removes a player from the session. Cancels the countdown if the player
     * count drops below 2.
     */
    public void remove(UUID uuid) {
        players.remove(uuid);
        joinTimes.remove(uuid);

        if (players.size() < 2 && countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            logger.log(
                Level.FINE,
                "Countdown for lobby '{0}' cancelled — not enough players.",
                this.lobbyName
            );
        }
    }

    public void setClass(UUID uuid, PlayerClass playerClass) {
        if (players.containsKey(uuid)) {
            players.put(uuid, playerClass);
        }
    }

    public PlayerClass getChosenClass(UUID uuid) {
        return players.get(uuid);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Map<UUID, PlayerClass> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    /**
     * Returns the time at which the player joined this session, or null if they
     * are not present.
     */
    public Instant getJoinTime(UUID uuid) {
        return joinTimes.get(uuid);
    }

    /**
     * Returns how long the player has been in the lobby, or null if they are
     * not present.
     */
    public Duration getTimeInLobby(UUID uuid) {
        Instant joined = joinTimes.get(uuid);
        return (joined != null) ? Duration.between(joined, Instant.now()) : null;
    }

    /**
     * Returns the most recent join time among all players in the session,
     * or null if no players are present.
     */
    public Instant getLatestJoinTime() {
        Instant latest = null;
        for (Instant t : joinTimes.values()) {
            if (latest == null || t.isAfter(latest)) {
                latest = t;
            }
        }
        return latest;
    }

    // -------------------------------------------------------------------------
    // Countdown & class assignment
    // -------------------------------------------------------------------------
    private void startCountdown() {
        long delayTicks = lobby.getStartCountdownSeconds() * 20L;
        plugin.getLogger().log(
            Level.FINE, 
            "Scheduling countdown task for lobby '{0}' with delay of {1} ticks.",
            new Object[]{lobby.getName(), delayTicks}
        );

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                onCountdownFire();
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Called when the countdown task fires. If the most recent player joined
     * less than 15 seconds ago, the task is rescheduled by 5 seconds to give
     * them time to pick a class. Otherwise, classes are assigned immediately.
     */
    private void onCountdownFire() {
        Instant latestJoin = getLatestJoinTime();
        if (latestJoin != null && Duration.between(latestJoin, Instant.now()).getSeconds() < 15) {
            logger.log(
                Level.FINE,
                "Recent join detected for lobby '{0}', rescheduling countdown by 5 seconds.",
                lobbyName
            );
            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    onCountdownFire();
                }
            }.runTaskLater(plugin, 100L);
            return;
        }

        countdownTask = null;
        assignClasses();
    }

    /**
     * Picks one random player to be the Shrouded. Any remaining player whose
     * class is still null is randomly assigned a regular class.
     */
    private void assignClasses() {
        List<UUID> playerList = new ArrayList<>(players.keySet());
        if (playerList.isEmpty()) {
            return;
        }

        // Assign the Shrouded role to one random player
        UUID shroudedUuid = playerList.get(random.nextInt(playerList.size()));
        players.put(shroudedUuid, PlayerClass.SHROUDED);

        // Assign a random regular class to any player who hasn't chosen one
        PlayerClass[] regularClasses = PlayerClass.regularClasses();
        for (UUID uuid : playerList) {
            if (!uuid.equals(shroudedUuid) && players.get(uuid) == null) {
                players.put(uuid, regularClasses[random.nextInt(regularClasses.length)]);
            }
        }

        plugin.getLogger().log(Level.FINE, "Classes assigned for lobby '{0}'.", lobby.getName());
    }
}
