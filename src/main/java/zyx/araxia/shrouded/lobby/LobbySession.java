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
    private final ArenaManager arenaManager;
    private final Random random = new Random();

    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;

    // null value = player joined but hasn't picked a class yet
    private final Map<UUID, PlayerClass> players = new HashMap<>();
    private final Map<UUID, Instant> joinTimes = new HashMap<>();
    private final String lobbyName;

    private BukkitTask countdownTask = null;

    /** Arenas chosen for the upcoming vote (or the single auto-selected arena). */
    private List<Arena> candidateArenas = new ArrayList<>();

    public LobbySession(Lobby lobby, JavaPlugin plugin, ArenaManager arenaManager) {
        this.lobby = lobby;
        this.lobbyName = lobby.getName();
        this.plugin = plugin;
        this.arenaManager = arenaManager;
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
        startSession();
    }

    /**
     * Cancels any pending countdown and immediately starts the session.
     * Has no effect if the session has fewer than 2 players.
     */
    public void forceStart() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        startSession();
    }

    /**
     * Starts the game session: assigns classes and performs any other
     * setup needed to begin the round.
     * Add future game-start calls here (e.g. teleport players, spawn items).
     */
    public void startSession() {
        assignClasses();
        selectArenas();
        // TODO: teleport players to arena
        // TODO: apply class kits
        // TODO: begin round timer
    }

    /**
     * Chooses up to 3 available arenas from the lobby's configured arena pool,
     * claims each one, then either starts a vote (multiple candidates) or
     * proceeds directly to arena transition (single candidate).
     *
     * <p>If no arenas are available the session is aborted with a log warning.
     */
    private void selectArenas() {
        List<String> validNames = lobby.getValidArenas();
        if (validNames.isEmpty()) {
            logger.log(Level.WARNING,
                    "Lobby '{0}' has no valid arenas configured — cannot start match.",
                    lobbyName);
            return;
        }

        // Collect all available (not in-use) arenas
        List<Arena> available = new ArrayList<>();
        for (String name : validNames) {
            Arena arena = arenaManager.getArena(name);
            if (arena != null && !arena.isInUse()) {
                available.add(arena);
            }
        }

        if (available.isEmpty()) {
            logger.log(Level.WARNING,
                    "Lobby '{0}' has no free arenas right now — cannot start match.",
                    lobbyName);
            return;
        }

        // Shuffle and take up to 3 candidates
        Collections.shuffle(available, random);
        int count = Math.min(3, available.size());
        candidateArenas = new ArrayList<>(available.subList(0, count));

        // Claim every candidate so other lobbies can't grab them
        for (Arena arena : candidateArenas) {
            arena.claim(lobbyName);
        }

        logger.log(Level.FINE,
                "Lobby '{0}' selected {1} arena candidate(s): {2}.",
                new Object[]{lobbyName, candidateArenas.size(),
                        candidateArenas.stream().map(Arena::getName).toList()});

        if (candidateArenas.size() == 1) {
            // Only one option — skip the vote and move straight to arena transition
            beginArenaTransition(candidateArenas.get(0));
        } else {
            // Multiple candidates — let players vote
            beginArenaVote(candidateArenas);
        }
    }

    /**
     * Called when exactly one arena was available (or the vote has concluded).
     * Starts moving players into the arena to begin the round.
     *
     * @param arena the arena that will be used for this match.
     */
    private void beginArenaTransition(Arena arena) {
        logger.log(Level.FINE,
                "Lobby '{0}' beginning transition to arena '{1}'.",
                new Object[]{lobbyName, arena.getName()});
        // TODO: teleport players to arena.getSpawnLocation()
    }

    /**
     * Called when two or three arenas are available and players should vote.
     *
     * @param candidates the arenas presented for the vote.
     */
    private void beginArenaVote(List<Arena> candidates) {
        logger.log(Level.FINE,
                "Lobby '{0}' starting arena vote with candidates: {1}.",
                new Object[]{lobbyName,
                        candidates.stream().map(Arena::getName).toList()});
        // TODO: implement voting logic
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
