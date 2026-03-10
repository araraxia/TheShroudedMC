package zyx.araxia.shrouded.lobby;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import zyx.araxia.shrouded.TheShrouded;
import zyx.araxia.shrouded.game.PlayerClass;
import zyx.araxia.shrouded.game.ShroudedClass;
import zyx.araxia.shrouded.game.SurvivorClass;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.menu.ArenaVoteMenu;

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
    private BukkitTask roundTask = null;
    private BukkitTask voteTask = null;

    /** Votes cast during the arena-vote phase (player UUID → chosen arena). */
    private final Map<UUID, Arena> votes = new HashMap<>();

    /** Arenas chosen for the upcoming vote (or the single auto-selected arena). */
    private List<Arena> candidateArenas = new ArrayList<>();

    /**
     * The arena currently hosting an active round, or {@code null} when no round
     * is in progress. Set by {@link #beginRoundTimer} and cleared by
     * {@link #endMatch}.
     */
    private Arena activeArena = null;

    /**
     * Players who died during the active round and must be redirected to the
     * lobby spawn point when their {@link org.bukkit.event.player.PlayerRespawnEvent}
     * fires, rather than respawning at the world default.
     */
    private final Set<UUID> pendingLobbyRespawn = new HashSet<>();

    public LobbySession(Lobby lobby, JavaPlugin plugin,
            ArenaManager arenaManager) {
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
        logger.log(Level.FINE,
                "Player {0} joined lobby '{1}' (total players: {2}).",
                new Object[] {
                        player.getName(), lobbyName, players.size()
                });

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
        pendingLobbyRespawn.remove(uuid);

        if (players.size() < 2 && countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            logger.log(Level.FINE,
                    "Countdown for lobby '{0}' cancelled — not enough players.",
                    this.lobbyName);
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
        return (joined != null) ? Duration.between(joined, Instant.now())
                : null;
    }

    /**
     * Returns the most recent join time among all players in the session, or
     * null if no players are present.
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
        plugin.getLogger().log(Level.FINE,
                "Scheduling countdown task for lobby '{0}' with delay of {1} ticks.",
                new Object[] {
                        lobby.getName(), delayTicks
                });

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
        if (latestJoin != null && Duration.between(latestJoin, Instant.now())
                .getSeconds() < 15) {
            logger.log(Level.FINE,
                    "Recent join detected for lobby '{0}', rescheduling countdown by 5 seconds.",
                    lobbyName);
            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    onCountdownFire();
                }
            }.runTaskLater(plugin, 100L);
            return;
        }

        countdownTask = null;
        selectArenas();
    }

    /**
     * Cancels any pending countdown and immediately starts the session. Has no
     * effect if the session has fewer than 2 players.
     */
    public void forceStart() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        selectArenas();
    }

    /**
     * Chooses up to 3 available arenas from the lobby's configured arena pool,
     * claims each one, then either starts a vote (multiple candidates) or
     * proceeds directly to arena transition (single candidate).
     * <p>
     * If no arenas are available the session is aborted with a log warning.
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

        // Shuffle and take up to the configured maximum number of candidates
        Collections.shuffle(available, random);
        int maxCandidates = plugin.getConfig()
                .getInt("game.arena-vote-candidates", 3);
        int count = Math.min(Math.max(maxCandidates, 1), available.size());
        candidateArenas = new ArrayList<>(available.subList(0, count));

        // Claim every candidate so other lobbies can't grab them
        for (Arena arena : candidateArenas) {
            arena.claim(lobbyName);
        }

        logger.log(Level.FINE,
                "Lobby '{0}' selected {1} arena candidate(s): {2}.",
                new Object[] {
                        lobbyName, candidateArenas.size(),
                        candidateArenas.stream().map(Arena::getName).toList()
                });

        if (candidateArenas.size() == 1) {
            // Only one option — skip the vote and move straight to arena
            // transition
            doArenaTransition(candidateArenas.get(0));
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
    private void doArenaTransition(Arena arena) {
        logger.log(Level.FINE,
                "[TheShrouded] Lobby ''{0}'' beginning transition to arena ''{1}''.",
                new Object[] {
                        lobbyName, arena.getName()
                });

        World world = Bukkit.getWorld(arena.getWorld());
        if (world == null) {
            logger.log(Level.WARNING,
                    "Arena world ''{0}'' is not loaded — cannot start match.",
                    arena.getWorld());
            return;
        }

        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline())
                continue;
            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.getInventory().setItemInOffHand(null);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                    20, 0, false, false));
        }

        Location spawnLocation = arena.getSpawnLocation(world);
        SurvivorClass survivorKit = new SurvivorClass(plugin);
        ShroudedClass shroudedKit = new ShroudedClass(plugin);
        int playerSpawnIndex = 0;
        int shroudedSpawnIndex = 0;

        // TODO: Run assignClases() to handle any players who haven't picked a class by now, rather than treating them as unassigned. Unassigned players could be randomly assigned to either role, or defaulted to Survivor and forced to play without their kit items.
        // TODO: Make sure at least one player is randomly assigned to the Shrouded role. No one should be able to select the Shrouded as a choosable class. It should overwrite one player at random.

        for (Map.Entry<UUID, PlayerClass> entry : players.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline())
                continue;

            PlayerClass playerClass = entry.getValue();

            // Pick a spawn from the role-appropriate list (round-robin)
            if (playerClass == PlayerClass.SHROUDED) {
                spawnLocation = arena.getShroudedSpawnAt(shroudedSpawnIndex++,
                        world);
            } else {
                spawnLocation = arena.getPlayerSpawnAt(playerSpawnIndex++,
                        world);
            }

            // Teleport to arena spawn
            player.teleport(spawnLocation);

            // Apply class kit
            if (playerClass == PlayerClass.SURVIVOR) {
                survivorKit.equip(player);
            }
            if (playerClass == PlayerClass.SHROUDED) {
                shroudedKit.equip(player);
            }

            // Announce round start with a title
            String roleText = playerClass != null ? playerClass.getDisplayName()
                    : "Unknown";
            player.showTitle(Title.title(
                    Component.text("Match Started!", NamedTextColor.GOLD),
                    Component.text("You are the " + roleText,
                            NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(300),
                            Duration.ofSeconds(3), Duration.ofMillis(500))));

            // Stinger sound effect
            player.playSound(player.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.1f);
        }

        beginRoundTimer(arena);
    }

    private void beginArenaVote(List<Arena> candidates) {
        logger.log(Level.FINE,
                "[TheShrouded] Lobby ''{0}'' starting arena vote with candidates: {1}.",
                new Object[] {
                        lobbyName,
                        candidates.stream().map(Arena::getName).toList()
                });

        votes.clear();

        // Open the vote menu for every online player in this session
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline())
                ArenaVoteMenu.open(player, this, candidates);
        }

        // Announce
        Component voteMsg = Component
                .text("Vote for an arena! You have "
                        + plugin.getConfig()
                                .getInt("game.arena-vote-timeout-seconds", 15)
                        + " seconds.", NamedTextColor.AQUA);
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline())
                player.sendActionBar(voteMsg);
        }

        // Timeout task — resolve when time runs out
        int timeoutSeconds = plugin.getConfig()
                .getInt("game.arena-vote-timeout-seconds", 15);
        voteTask = new BukkitRunnable() {
            @Override
            public void run() {
                voteTask = null;
                resolveVote(candidates);
            }
        }.runTaskLater(plugin, timeoutSeconds * 20L);
    }

    /**
     * Records a vote for {@code arena} from {@code uuid}. If every player in
     * the session has now voted the vote is resolved immediately.
     *
     * @param uuid  the voting player's UUID
     * @param arena the arena they chose
     */
    public void recordVote(UUID uuid, Arena arena) {
        if (!players.containsKey(uuid))
            return;
        votes.put(uuid, arena);
        logger.log(Level.FINE,
                "[TheShrouded] Player {0} voted for arena ''{1}''.",
                new Object[] {
                        uuid, arena.getName()
                });

        // Early resolution if everyone has voted
        if (votes.size() >= players.size()) {
            if (voteTask != null) {
                voteTask.cancel();
                voteTask = null;
            }
            resolveVote(candidateArenas);
        }
    }

    /**
     * Tallies votes, picks an arena weighted by vote count (each arena gets a
     * guaranteed base weight of 1 plus one additional weight per vote cast for
     * it), releases all unchosen candidates and starts the arena transition.
     */
    private void resolveVote(List<Arena> candidates) {
        // Close any still-open vote menus
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline())
                player.closeInventory();
        }

        // Count votes per candidate
        Map<Arena, Integer> voteCounts = new HashMap<>();
        for (Arena a : candidates)
            voteCounts.put(a, 0);
        for (Arena voted : votes.values())
            voteCounts.merge(voted, 1, Integer::sum);

        // Build weighted pool: 1 base weight + 1 per vote
        List<Arena> pool = new ArrayList<>();
        for (Map.Entry<Arena, Integer> entry : voteCounts.entrySet()) {
            int weight = 1 + entry.getValue();
            for (int i = 0; i < weight; i++)
                pool.add(entry.getKey());
        }

        Arena chosen = pool.get(random.nextInt(pool.size()));

        logger.log(Level.FINE,
                "[TheShrouded] Arena vote resolved for lobby ''{0}'': ''{1}'' chosen.",
                new Object[] {
                        lobbyName, chosen.getName()
                });

        // Release every candidate that was not chosen
        for (Arena a : candidates) {
            if (!a.getName().equals(chosen.getName()))
                a.release();
        }

        // Announce result
        Component result = Component
                .text("Arena selected: ", NamedTextColor.GREEN)
                .append(Component.text(chosen.getName(), NamedTextColor.AQUA));
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline())
                player.sendMessage(result);
        }

        votes.clear();
        doArenaTransition(chosen);
    }

    /**
     * Starts a per-second countdown for the active round. Sends action-bar
     * reminders at 60 s, 30 s, 10 s and each of the final 5 seconds, then calls
     * {@link #endMatch(Arena)} when time expires.
     */
    private void beginRoundTimer(Arena arena) {
        activeArena = arena;
        int durationSeconds = plugin.getConfig()
                .getInt("game.match-duration-seconds", 300);

        roundTask = new BukkitRunnable() {
            int secondsRemaining = durationSeconds;

            @Override
            public void run() {
                if (secondsRemaining <= 0) {
                    cancel();
                    roundTask = null;
                    endMatch(arena);
                    return;
                }

                boolean announce = secondsRemaining == 60
                        || secondsRemaining == 30 || secondsRemaining == 10
                        || secondsRemaining <= 5;

                if (announce) {
                    Component bar = Component.text(
                            secondsRemaining + "s remaining",
                            secondsRemaining <= 10 ? NamedTextColor.RED
                                    : NamedTextColor.YELLOW);
                    for (UUID uuid : players.keySet()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline())
                            player.sendActionBar(bar);
                    }
                }

                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Called when a player dies during an active round. If the dead player was
     * a {@link PlayerClass#SURVIVOR} and no other alive Survivors remain in the
     * session, the match is ended immediately via {@link #endMatch(Arena)}.
     * <p>
     * This method is a no-op when no round is in progress ({@code activeArena
     * == null}) or when the dead player is not a Survivor.
     *
     * @param dead UUID of the player who just died
     */
    public void onPlayerDied(UUID dead) {
        if (activeArena == null)
            return; // no active match

        // Queue the dead player for lobby respawn regardless of role so that
        // PlayerRespawnListener can redirect them when they click Respawn.
        pendingLobbyRespawn.add(dead);

        PlayerClass deadClass = players.get(dead);
        // TODO: Check if the dead player was the Shrouded role instead, and end the match if so. All mercaneries will be winners, the Shrouded player loses.

        // TODO: Change from PlayerClass.SURVIVOR to any non-shrouded class.
        if (deadClass != PlayerClass.SURVIVOR)
            return; // only track survivor deaths

        // Check whether any other Survivor is still alive
        for (Map.Entry<UUID, PlayerClass> entry : players.entrySet()) {
            if (entry.getValue() != PlayerClass.SURVIVOR)
                continue;
            if (entry.getKey().equals(dead))
                continue; // skip the player who just died
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline() && !p.isDead())
                return; // at least one survivor is still alive
        }

        logger.log(Level.FINE,
                "[TheShrouded] All survivors eliminated in lobby ''{0}'' — ending match early.",
                lobbyName);
        endMatch(activeArena);
    }

    /**
     * Ends the current match: notifies all players, releases the arena, and
     * cancels the round timer if it is still running.
     */
    private void endMatch(Arena arena) {
        logger.log(Level.FINE,
                "[TheShrouded] Match ended for lobby ''{0}'' in arena ''{1}''.",
                new Object[] {
                        lobbyName, arena.getName()
                });

        activeArena = null;

        if (roundTask != null) {
            roundTask.cancel();
            roundTask = null;
        }

        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }

        Title endTitle = Title.title(
                Component.text("Round Over!", NamedTextColor.RED),
                Component.empty(), Title.Times.times(Duration.ofMillis(300),
                        Duration.ofSeconds(3), Duration.ofMillis(500)));

        // TODO: Differentiate win/lose messages based on whether Survivors were eliminated or time ran out. If the Shrouded was killed, they lose and everyone else wins. If time ran out with at least one survivor alive, survivors win and the shrouded loses. If all survivors were killed, the shrouded wins and everyone else loses. For now just show a generic "Round Over" message to everyone.
        // TODO: Delay post-match lobby return by a few seconds to allow players to see the end title and soak in the victory/defeat for a moment, rather than immediately teleporting them back to the lobby as soon as the match ends.
        // TODO: Track player win/loss and other metircs in a persistent player profile that can be viewed on a website or via an in-game menu.

        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline())
                continue;
            player.showTitle(endTitle);

            if (player.isDead()) {
                // Player is still on the death screen; queue them for lobby
                // respawn and let PlayerRespawnListener handle the teleport.
                pendingLobbyRespawn.add(uuid);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH,
                        0.5f, 1.0f);
                restorePlayerToLobbyState(player);
            }
        }

        arena.release();

        votes.clear();
        candidateArenas = new ArrayList<>();

        // Return to lobby phase: wait a fresh countdown before the next round.
        if (players.size() >= 2 && countdownTask == null) {
            startCountdown();
        }
    }

    /**
     * Returns {@code true} and removes the player from the pending-respawn set
     * if they were queued for a lobby respawn after dying mid-match,
     * {@code false} otherwise.
     *
     * <p>Called by {@code PlayerRespawnListener} when the player's respawn
     * event fires.
     */
    public boolean consumePendingRespawn(UUID uuid) {
        return pendingLobbyRespawn.remove(uuid);
    }

    /**
     * Returns the lobby's spawn {@link Location}, or {@code null} if the lobby
     * world is not currently loaded.
     */
    public Location getLobbySpawnLocation() {
        World world = Bukkit.getWorld(lobby.getWorld());
        if (world == null) {
            logger.log(Level.WARNING,
                    "Lobby world ''{0}'' is not loaded — cannot provide respawn location.",
                    lobby.getWorld());
            return null;
        }
        return lobby.getSpawnLocation(world);
    }

    /**
     * Returns {@code true} if there is currently an active round in progress
     * (i.e. the round timer is running and {@code activeArena} is set).
     */
    public boolean isMatchActive() {
        return activeArena != null;
    }

    /**
     * Applies the standard post-match lobby state to a player who is currently
     * dead and will be redirected to the lobby on respawn. Called from
     * {@code PlayerRespawnListener} immediately before the respawn location is
     * set.
     *
     * <p>Intentionally omits {@link Player#closeInventory()} (no-op on a dead
     * player) and the teleport (handled via
     * {@link org.bukkit.event.player.PlayerRespawnEvent#setRespawnLocation}).
     */
    public void applyLobbyStateOnRespawn(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        player.getInventory().setItemInOffHand(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        ((TheShrouded) plugin).getEquipmentSpoofer().stopSpoofing(player);

        players.put(player.getUniqueId(), null);

        player.getInventory().setItem(0, ShroudedItems.createClassSelector());
    }

    private void restorePlayerToLobbyState(Player player) {
        // Close inventory FIRST so crafting-grid contents are flushed into the
        // current world before the player is teleported away.
        player.closeInventory();

        World lobbyWorld = Bukkit.getWorld(lobby.getWorld());
        if (lobbyWorld != null) {
            player.teleport(lobby.getSpawnLocation(lobbyWorld));
        } else {
            logger.log(Level.WARNING,
                    "Lobby world ''{0}'' is not loaded while restoring player ''{1}''.",
                    new Object[] {
                            lobby.getWorld(), player.getName()
                    });
        }

        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        player.getInventory().setItemInOffHand(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Stop the equipment spoofer in case this player was the Shrouded role.
        // stopSpoofing() is a safe no-op if they were not being spoofed.
        ((TheShrouded) plugin).getEquipmentSpoofer().stopSpoofing(player);

        // Reset the class to null so the player is prompted to re-select for
        // the next round, and so assignClasses() treats them as unassigned.
        players.put(player.getUniqueId(), null);

        player.getInventory().setItem(0, ShroudedItems.createClassSelector());
    }
}