# Game Start Process — `LobbySession`

Complete call chain from player joining to round end, in execution order.

---

## Entry Points

### `add(Player)`

Registers a player in the session. Once the count reaches 2, automatically calls `startCountdown()`.

### `forceStart()`

Cancels any pending countdown and jumps directly to `selectArenas()`, bypassing the timer entirely.

---

## Phase 1 — Countdown

### `startCountdown()`

Schedules a delayed `BukkitRunnable` (length = `lobby.getStartCountdownSeconds()`). When it fires it calls `onCountdownFire()`.

### `onCountdownFire()`

Checks if any player joined in the last 15 seconds. If so, reschedules itself +5 seconds to give late joiners time to pick a class. Otherwise calls `selectArenas()`.

---

## Phase 2 — Arena Selection

### `selectArenas()`

Filters the lobby's configured arena pool down to arenas that are free, shuffles them, and claims up to `game.arena-vote-candidates` (default 3). Branches:

- **One candidate** → calls `doArenaTransition()` directly.
- **Multiple candidates** → calls `beginArenaVote()`.

---

## Phase 2a — Vote Path *(multiple candidates only)*

### `beginArenaVote(candidates)`

Opens `ArenaVoteMenu` for every online player, sends an action-bar message, and schedules a timeout task (`game.arena-vote-timeout-seconds`, default 15 s) that calls `resolveVote()`.

### `recordVote(uuid, arena)`

Records a player's vote. If all players have voted before the timeout, cancels the timeout task and calls `resolveVote()` early.

### `resolveVote(candidates)`

Tallies votes with weighted randomness (each arena gets 1 base weight + 1 per vote cast for it), releases all unchosen candidates, announces the winner, then calls `doArenaTransition()` with the chosen arena.

---

## Phase 3 — Arena Transition & Kit Assignment

### `doArenaTransition(arena)`

The final pre-match setup step:

1. Clears every player's inventory/armour and applies 1 second of blindness.
2. Iterates all players, resolves their role-appropriate spawn point (round-robin from `arena.getPlayerSpawnAt` / `arena.getShroudedSpawnAt`), and teleports them.
3. Calls `SurvivorClass.equip(player)` or `ShroudedClass.equip(player)` depending on assigned class.
4. Shows a "Match Started! / You are the X" title and plays a stinger sound.
5. Calls `beginRoundTimer(arena)`.

> **⚠️ Known issue:** `assignClasses()` is defined but is **never called** in the current flow. Class selection relies entirely on players choosing via `ClassSelectMenu` during the lobby phase. Any player who did not pick a class will receive no kit and be shown an "Unknown" role title. Consider calling `assignClasses()` at the start of `selectArenas()` or `doArenaTransition()` to auto-fill unassigned classes.

---

## Phase 4 — Round Timer

### `beginRoundTimer(arena)`

Runs a per-second `BukkitRunnable` for `game.match-duration-seconds` (default 300 s). Sends action-bar countdown messages at 60 s, 30 s, 10 s, and the final 5 seconds. When the counter reaches 0, calls `endMatch(arena)`.

---

## Phase 5 — End of Match / Return to Lobby

### `endMatch(arena)`

1. Cancels any running `roundTask` and `voteTask`.
2. Shows a "Round Over!" title and plays a wither-death sound for each online player.
3. Calls `restorePlayerToLobbyState(player)` for each online player.
4. Calls `arena.release()`.
5. Clears `votes` and `candidateArenas`.
6. If ≥ 2 players remain, calls `startCountdown()` to begin the next round.

### `restorePlayerToLobbyState(player)`

1. `player.closeInventory()` — flushes the crafting grid **before** teleporting (must happen in the current world).
2. Teleports the player to the lobby spawn (`lobby.getSpawnLocation(lobbyWorld)`).
3. Wipes all inventory slots, armour, and off-hand.
4. Removes all active potion effects.
5. Calls `equipmentSpoofer.stopSpoofing(player)` — safe no-op if the player was not the Shrouded.
6. Resets the player's stored class to `null` so they are prompted to re-select for the next round.
7. Gives the player the class-selector item in hotbar slot 0.
