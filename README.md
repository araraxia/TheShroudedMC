# TheShrouded

A Minecraft minigame plugin inspired by **The Hidden: Source**, a classic Half-Life 2 mod.

---

## Overview

TheShrouded pits a small team of mercenaries against a single, nearly invisible predator — **The Shrouded**.

One player is secretly chosen to be The Shrouded at the start of each round. The Shrouded is almost completely invisible and possesses powerful supernatural abilities, but can only use their fists and wits. The remaining players take on soldier roles, each with their own set of tools and playstyles, and must work together to hunt down and eliminate The Shrouded before their numbers dwindle.

### The Shrouded

The Shrouded is a near-invisible, highly mobile assassin. Their abilities include:

| Ability | Description |
| --- | --- |
| **Invisibility** | Almost entirely invisible to other players at all times. |
| **Pounce** | Launch in any direction with explosive speed to close gaps or escape combat. |
| **Wall Cling** | Cling to any surface, including walls and ceilings, to stalk prey from unexpected angles. |
| **Pin** | Pin a target player in place momentarily, leaving them helpless. |

### Mercenary Classes

The mercenaries are the hunting team. Each player selects a class before the round begins, filling different combat roles.

| Class | Role | Description |
| --- | --- | --- |
| **Survivor** | Generalist | A well-rounded fighter with no special tools — relies on teamwork and awareness. |
| **Support** | Utility | Provides healing and buffs to keep the team functional under pressure. |
| **Knight** | Tank | Heavy armor and melee capability for close-quarters combat. |
| **Archer** | Skirmisher | Fast and mobile, uses a bow to attack from range and reposition quickly. |
| **Sharpshooter** | Precision | A specialist marksman who excels at picking off targets at long range. |
| **Pyromancer** | Area Denial | Uses fire-based attacks to flush The Shrouded out of hiding. |

> **Note:** Additional classes can be added to this table as they are implemented.

---

## Setup

1. Drop the plugin `.jar` into your server's `plugins/` folder and restart.
2. Register a **lobby** region for players to wait in before a match starts.
3. Register one or more **arena** regions where matches will be played.
4. Link each arena to a lobby.
5. Register a **sign** in the lobby so players can join.
6. *(Optional)* Adjust the lobby countdown timer.

---

## Commands

All setup commands require the `shrouded.admin` permission.

---

### `/shrouded.register.lobby`

Registers a rectangular region as a lobby waiting area.

**Usage:**

```text
/shrouded.register.lobby <lobby_name> <x1> <y1> <z1> <x2> <y2> <z2> [world_name] [max_players]
```

| Argument | Required | Description |
| --- | --- | --- |
| `lobby_name` | Yes | A unique name for this lobby. |
| `x1 y1 z1` | Yes | One corner of the lobby region. |
| `x2 y2 z2` | Yes | The opposite corner of the lobby region. |
| `world_name` | No | The world the region is in. Defaults to the executing player's current world. |
| `max_players` | No | Maximum number of players for this lobby. Defaults to `8`. |

---

### `/shrouded.register.arena`

Registers a rectangular region as a match arena.

**Usage:**

```text
/shrouded.register.arena <arena_name> <x1> <y1> <z1> <x2> <y2> <z2> [world_name] [max_players]
```

| Argument | Required | Description |
| --- | --- | --- |
| `arena_name` | Yes | A unique name for this arena. |
| `x1 y1 z1` | Yes | One corner of the arena region. |
| `x2 y2 z2` | Yes | The opposite corner of the arena region. |
| `world_name` | No | The world the region is in. Defaults to the executing player's current world. |
| `max_players` | No | Maximum concurrent players for this arena. |

---

### `/shrouded.register.sign`

Registers the sign you are looking at as a join point for a lobby. Players can click this sign to enter the lobby queue.

**Usage:**

```text
/shrouded.register.sign <lobby_name>
```

| Argument | Required | Description |
| --- | --- | --- |
| `lobby_name` | Yes | The name of the lobby to link the sign to. |

---

### `/shrouded.lobby.arena`

Links or unlinks an arena from a lobby's rotation. A lobby must have at least one linked arena to start matches.

**Usage:**

```text
/shrouded.lobby.arena <add|remove> <lobby_name> <arena_name>
```

| Argument | Required | Description |
| --- | --- | --- |
| `add` / `remove` | Yes | Whether to link or unlink the arena. |
| `lobby_name` | Yes | The name of the target lobby. |
| `arena_name` | Yes | The name of the arena to add or remove. |

---

### `/shrouded.lobby.countdown`

Sets how long (in seconds) the lobby countdown timer runs before teleporting players to an arena.

**Usage:**

```text
/shrouded.lobby.countdown <lobby_name> <seconds>
```

| Argument     | Required | Description                           |
|--------------|----------|---------------------------------------|
| `lobby_name` | Yes      | The name of the target lobby.         |
| `seconds`    | Yes      | Duration of the countdown in seconds. |

---

## Permissions

| Permission                | Description                                           | Default  |
|---------------------------|-------------------------------------------------------|----------|
| `shrouded.admin`          | Grants access to all setup and configuration commands | OP only  |

> **Note:** Additional permissions can be added to this table as player-facing features are implemented (e.g., per-class permissions, spectator access, etc.).

---

## Project Info

| Key | Value |
| --- | --- |
| **Author** | AriaC. |
| **API Version** | 1.21 |
| **Plugin Version** | 1.0.0 |
| **Inspiration** | [The Hidden: Source](https://www.hidden-source.com/) |
