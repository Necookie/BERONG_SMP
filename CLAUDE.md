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
  → places all buildings (LSPU Library + SSC Building) via BUILDINGS list
  → teleports player inside library structure
  → (FIRE only) gives fire extinguisher in hotbar slot 0
  → (EARTHQUAKE only) session.initEarthquake(random, magnitude) places epicenter near the library interior
      → epicenter fixed 3–9 blocks from SIM_POS in XZ so destruction concentrates inside the structure
      → aftershockCount initialised to 2–4 (random) for multi-wave aftershock support
      → button press uses random strong magnitude (6.0–9.5); command uses config default or explicit arg
      → player receives "§c⚠ Magnitude X.X Earthquake has begun!" message
SimulationManager.onServerTick (every tick):
  → session.tick() decrements timer
  → (FIRE) SimulationEffects.simulateFire at fireSpawnInterval; cleanupFireOutsideBounds every 40 ticks
  → (EARTHQUAKE) session.tickQuakePhase(level.getRandom()) advances RUMBLE→PEAK→AFTERSHOCK(×2–4)→END
      → phase transitions send chat messages: "intensifying!" / "Aftershock!" / "shaking has stopped"
      → AFTERSHOCK re-enters itself aftershockCount times; each wave gets a random scale (0.2–0.55×
        normal; 25% chance of stronger 0.6–1.1× wave) before finally reaching END
      → at quakeInterval: SimulationEffects.simulateEarthquake(level, session) — phase dispatch:
          RUMBLE: breakOrDebris count = ceil(baseCount × magnitude / 5) within magnitude×3 radius
          PEAK: enqueuePeakDestructions — scans for unsupported (air below) blocks sorted
                closest-first; adds batch to pendingDestructions queue
          AFTERSHOCK: breakOrDebris count = ceil(baseCount × effectiveMag / 5); effectiveMag =
                      sessionMagnitude × aftershockMagnitudeScale; radius = effectiveMag × 2.5
      → all three phases use breakOrDebris(): wood/glass blocks spawn FallingBlockEntity
        (visible, gravity-driven, deals 2×fallBlocks fall damage up to 40, then discards);
        non-debris blocks vanish instantly via destroyBlock. DEBRIS_BLOCKS is a static Set for O(1) lookup.
        Debris count scales with effective magnitude so stronger quakes produce more falling blocks.
      → every tick: drainEarthquakePending — breaks 2 blocks from cascade queue (PEAK only)
      → every 20 ticks: clearFireInArena — removes vanilla fire that spreads during block destruction
  → sends SimulationStatusPayload HUD sync every 10 ticks
      → includes per-player intensity = magnitude * exp(-decayRate * distance) * phaseScale
      → client SimulationHud.onCameraAngles applies sinusoidal rumble + random jitter scaled by intensity
Session expires / player dies / /sim_stop → SimulationManager.endSimulation
  → restores all buildings via BUILDINGS list
  → teleports alive player to lobby OR marks UUID in pendingLobbyRespawn (dead player)
Player respawns → SimulationManager.onPlayerRespawn → redirects to lobby if pending
```

### Key Classes

| Class | Responsibility |
|---|---|
| `BerongSMP` | Mod entry point, item/block registration, server startup init |
| `SimulationManager` | Session registry (`ConcurrentHashMap<UUID, SimulationSession>`), tick driver, event handlers for tick/respawn/logout |
| `SimulationSession` | Per-player mutable state: timer ticks, disaster type, fires extinguished count, earthquake epicenter/phase/cascade queue/magnitude/aftershockCount/aftershockMagnitudeScale |
| `SimulationSession.EarthquakePhase` | Inner enum: `RUMBLE → PEAK → AFTERSHOCK(×2–4) → END`; AFTERSHOCK loops with a random magnitude scale before advancing to END |
| `SimulationEffects` | World mutation: fire placement, fire cleanup; phase-aware earthquake (RUMBLE/PEAK/AFTERSHOCK helpers + cascade drain + `breakOrDebris` for falling debris) |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = fire, higher Z = quake), login/button-click handlers |
| `StructurePlacer` | Interface for placing a structure at a `BlockPos`; implemented by both loaders below |
| `SimulationStructureLoader` | Implements `StructurePlacer`; wraps `StructureTemplateManager` for `.nbt` files |
| `SchemLoader` | Implements `StructurePlacer`; parses Sponge Schematic v2/v3 `.schem` files, supports 0–3 CCW 90° rotations (rotates offsets and block states), and places blocks |
| `SimulationStatusPayload` | Server→client packet (record + `StreamCodec`, channel v2) carrying `status`, `timeLeft`, and `intensity` |
| `SimulationHud` | Client-side HUD renderer; also drives camera shake via `ViewportEvent.ComputeCameraAngles` using `intensity` |
| `Config` | `ModConfigSpec` entries for all simulation tuning knobs |
| `ModCommands` | Brigadier commands: `/sim_fire`, `/sim_earthquake [magnitude]`, `/sim_magnitude <value>` (op), `/sim_stop`, `/spawn_lspu`, `/get_extinguisher` |
| `FireExtinguisherItem` | Custom item; right-click extinguishes fire blocks and calls `SimulationManager.getSession(uuid).recordExtinguish()` |

### World Coordinates

- **Lobby**: origin `BlockPos(0, -33, 0)`, player spawn at `(8.8, -31, 8)`
- **Simulation arena**: `SIM_POS = BlockPos(30, -34, 83)`, player entry offset `+5.5, +2, +5.5`
- **SSC Building**: `SSC_POS = BlockPos(11, -33, 90)` (~19 blocks west of the library origin), placed with 1 CCW rotation

### Structures

Stored under `src/main/resources/data/berongsmp/structure/`:
- `lobby_structure.nbt` — lobby building with two buttons (NBT, placed once at server start)
- `lspulibrarymain.nbt` — simulation arena (NBT, placed/restored each session)
- `ssc_building.schem` — SSC building adjacent to the arena (Sponge Schematic v3, placed/restored each session with 1 CCW rotation)

`SimulationManager.BUILDINGS` holds the full list of `StructurePlacer`+`BlockPos` pairs iterated on session start and end.

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
| `quakeMagnitude` | 5.0 | Epicenter intensity (0.1–10.0); also scales destruction radius |
| `quakeDecayRate` | 0.05 | Intensity falloff per block of distance from epicenter |
| `quakeRumbleDuration` | 200 | Ticks in RUMBLE phase (10 s) |
| `quakePeakDuration` | 900 | Ticks in PEAK phase (45 s) |
| `quakeAftershockDuration` | 300 | Ticks per aftershock wave (15 s); 2–4 waves follow the main quake |

### Thread Safety

`SimulationManager.activeSessions` is a `ConcurrentHashMap`. `startSimulation` and `endSimulation` are `synchronized`. Tick-driven mutations are single-threaded via `ServerTickEvent.Post`. Network packet handling uses `context.enqueueWork()` to marshal HUD updates (including the new `intensity` field) onto the main client thread. `SimulationHud.intensity` is written on the main client thread and read from `ViewportEvent.ComputeCameraAngles`, which also fires on the main client thread — no extra synchronisation needed.

### Client–Server Split

`BerongSMPClient` is annotated `@Mod(dist = CLIENT)` and only loads on the physical client. `SimulationHud` and `KeyMappings` are client-only classes registered through the mod event bus, keeping the server JAR free of rendering dependencies.
