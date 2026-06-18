# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BerongSMP is a NeoForge mod for Minecraft 26.1.2 (NeoForge 26.1.2.36-beta) that implements a disaster simulation minigame. Players enter a lobby, press buttons to trigger fire or earthquake simulations inside an LSPU Library NBT structure, and are scored on their response. The mod is built with Java 25.

## Build & Run Commands

```bash
# Build the mod JAR
./gradlew build

# Run the development server (headless)
./gradlew runServer

# Run the development client
./gradlew runClient

# Run data generators (outputs to src/generated/resources/)
./gradlew runData

# Compile only (fast check)
./gradlew compileJava
```

The `run/` directory is the working directory for dev runs and contains world saves, server config, and op lists. Config file for the mod is `run/config/berongsmp-common.toml`.

## Architecture

### Event Bus Duality

NeoForge uses two separate event buses — a core pattern throughout this codebase:

- **`modEventBus`** — mod lifecycle events (registration, client setup). Used in `BerongSMP` constructor for `DeferredRegister`, network payload registration, and `BuildCreativeModeTabContentsEvent`.
- **`NeoForge.EVENT_BUS`** — runtime game events (server tick, player join, block interact). Classes annotated with `@EventBusSubscriber` auto-register here.

### Simulation Flow

```
Player logs in → LobbyManager.onPlayerLogin → teleport to lobby
Player clicks button → LobbyManager.onRightClickBlock → SimulationManager.startSimulation
  → places LSPU Library NBT structure
  → teleports player inside structure
  → (FIRE only) gives fire extinguisher in hotbar slot 0
SimulationManager.onServerTick (every tick):
  → session.tick() decrements timer
  → SimulationEffects.simulateFire / simulateEarthquake at configured intervals
  → cleanupFireOutsideBounds every 40 ticks (FIRE only)
  → sends SimulationStatusPayload HUD sync every 10 ticks
Session expires / player dies / /sim_stop → SimulationManager.endSimulation
  → restores structure via SimulationStructureLoader
  → teleports alive player to lobby OR marks UUID in pendingLobbyRespawn (dead player)
Player respawns → SimulationManager.onPlayerRespawn → redirects to lobby if pending
```

### Key Classes

| Class | Responsibility |
|---|---|
| `BerongSMP` | Mod entry point, item/block registration, server startup init |
| `SimulationManager` | Session registry (`ConcurrentHashMap<UUID, SimulationSession>`), tick driver, event handlers for tick/respawn/logout |
| `SimulationSession` | Per-player mutable state: timer ticks, disaster type, fires extinguished count |
| `SimulationEffects` | Stateless world mutation: fire placement, earthquake block destruction, fire cleanup |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = fire, higher Z = quake), login/button-click handlers |
| `SimulationStructureLoader` | Wraps `StructureTemplateManager` to place the LSPU Library NBT on session start/end |
| `SimulationStatusPayload` | Server→client packet (record + `StreamCodec`) for HUD sync |
| `SimulationHud` | Client-side HUD renderer reading `currentStatus` / `timeLeft` static fields |
| `Config` | `ModConfigSpec` entries for all simulation tuning knobs |
| `ModCommands` | Brigadier commands: `/sim_fire`, `/sim_earthquake`, `/sim_stop`, `/spawn_lspu`, `/get_extinguisher` |
| `FireExtinguisherItem` | Custom item; right-click extinguishes fire blocks and calls `SimulationManager.getSession(uuid).recordExtinguish()` |

### World Coordinates

- **Lobby**: origin `BlockPos(0, -33, 0)`, player spawn at `(8.8, -31, 8)`
- **Simulation arena**: `SIM_POS = BlockPos(30, -34, 83)`, player entry offset `+5.5, +2, +5.5`

### NBT Structures

Stored under `src/main/resources/data/berongsmp/structure/`:
- `lobby_structure.nbt` — the lobby building with two buttons
- `lspulibrarymain.nbt` — the simulation arena placed/restored each session

### Config Knobs (`berongsmp-common.toml`)

All values are read at call time via `.get()` — changes take effect without restart:

| Key | Default | Meaning |
|---|---|---|
| `simDurationTicks` | 2400 | Session length (20 ticks = 1 second) |
| `fireSpawnCount` | 3 | Fire blocks placed per spawn event |
| `fireSpawnInterval` | 20 | Ticks between fire spawns |
| `quakeBreakCount` | 2 | Blocks destroyed per quake event |
| `quakeInterval` | 10 | Ticks between quake events |
| `simAreaSize` | 25 | XZ arena radius for random effects |
| `simAreaHeight` | 10 | Y arena height for random effects |

### Thread Safety

`SimulationManager.activeSessions` is a `ConcurrentHashMap`. `startSimulation` and `endSimulation` are `synchronized`. Tick-driven mutations are single-threaded via `ServerTickEvent.Post`. Network packet handling uses `context.enqueueWork()` to marshal HUD updates onto the main client thread.

### Client–Server Split

`BerongSMPClient` is annotated `@Mod(dist = CLIENT)` and only loads on the physical client. `SimulationHud` and `KeyMappings` are client-only classes registered through the mod event bus, keeping the server JAR free of rendering dependencies.
