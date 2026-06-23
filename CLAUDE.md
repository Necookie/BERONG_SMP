# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Master Plan

See **`docs/major_plan.md`** for the full phased implementation plan, architecture overview, Turso schema, and phase status tracking. All phases (1–5) are complete.

## Development Workflow

**After completing any feature or phase goal:**
1. Update `CLAUDE.md` — add/update the Key Classes table and architecture notes to reflect what changed.
2. Update `docs/major_plan.md` — mark the completed deliverable `[x]`.
3. Commit the changes with a descriptive message and push to `main`.

This keeps the project documentation in sync with the code at all times.

---

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

### Tutorial Flow

Players must complete a safety tutorial before simulation buttons become active. Progress persists across disconnects via `TutorialSavedData`.

```
Stage order: NOT_STARTED → PASS_PULL → PASS_SPRAY → EXT_TYPE_A → EXT_TYPE_B → EXT_TYPE_C
             → QUAKE_INTRO → QUAKE_DROP → QUAKE_COVER → QUAKE_HOLDON → COMPLETED

Sgt. Reyes NPC (TRAINER): talks during NOT_STARTED/PASS_PULL → gives extinguisher, spawns 5 campfires at PRACTICE_FIRE (+7,2,11)
  → stage = PASS_SPRAY

FireExtinguisherItem.extinguishAt → TutorialManager.onExtinguish (unconditional):
  → counts extinguishes while stage == PASS_SPRAY; at 3 → remove campfires, stage = EXT_TYPE_A

Officer Cruz NPC (EXT_EXPERT): talks during EXT_TYPE_A/B/C in order → class info, stage advances A→B→C→QUAKE_INTRO

Capt. Santos NPC (SAFETY_OFFICER): talks during QUAKE_INTRO (5 lines, last advancesStage=true)
  → final line triggers QUAKE_DROP; earthquake drill begins

TutorialManager.tick (called from SimulationManager.onServerTick every tick):
  QUAKE_DROP  → shake prompt every 10 ticks; player crouches → QUAKE_COVER
  QUAKE_COVER → player crouches + solid block at blockPos.above(2) → QUAKE_HOLDON
  QUAKE_HOLDON → hold condition 100 ticks; intensity fades 1.5→0; break cover resets timer
               → at 100 ticks: stage = COMPLETED, confetti particles, clear HUD

LobbyManager.onRightClickBlock gates fire/quake buttons:
  if (!TutorialManager.isComplete(uuid)) → "Complete the safety tutorial first!" — no simulation starts
```

Station constants in `TutorialManager` (offsets from `LobbyManager.LOBBY_POS = (0,-33,0)`) are **placeholder values** — tune them against the actual lobby interior when running `./gradlew runServer`.

### Simulation Flow

```
Player logs in → LobbyManager.onPlayerLogin → teleport to lobby
Player clicks button → LobbyManager.onRightClickBlock → SimulationManager.startSimulation
  → places all buildings (LSPU Library + SSC Building) via BUILDINGS list
  → teleports player to a random valid position inside the library (any floor); scans arena for solid-floor + 2-air-block-tall gaps, picks one at random; falls back to front-door entry if none found
  → (FIRE only) gives fire extinguisher in hotbar slot 0
  → (EARTHQUAKE only) session.initEarthquake(random, magnitude) places epicenter near the library interior
      → epicenter fixed 3–9 blocks from SIM_POS in XZ so destruction concentrates inside the structure
      → aftershockCount initialised to 2–4 (random) for multi-wave aftershock support
      → button press uses random strong magnitude (6.0–9.5); command uses config default or explicit arg
      → player receives "§c⚠ Magnitude X.X Earthquake has begun!" message
SimulationManager.onServerTick (every tick):
  → session.tick() decrements timer
  → (FIRE) SimulationEffects.simulateFire at fireSpawnInterval (spawns 14 LARGE_SMOKE particles per fire block placed with wide 2.2-block XZ spread and 3-block Y rise);
           cleanupFireOutsideBounds every 40 ticks;
           applyFireProximityEffects every 20 ticks — scans 7-block radius, applies Nausea (amp 0–2) and
           drains air supply (oxygen depletion; vanilla suffocation triggers at zero) scaling with fire density +
           proximity; no direct damage, no blindness
  → (EARTHQUAKE) session.tickQuakePhase(level.getRandom()) advances RUMBLE→PEAK→AFTERSHOCK(×2–4)→END
      → phase transitions send chat messages: "intensifying!" / "Aftershock!" / "shaking has stopped"
      → AFTERSHOCK re-enters itself aftershockCount times; each wave gets a random scale (0.2–0.55×
        normal; 25% chance of stronger 0.6–1.1× wave) before finally reaching END
      → every 60 ticks: Nausea applied based on phase (PEAK: amp 0–3 scaled by magnitude;
        AFTERSHOCK: amp 0–2 scaled by effectiveMag) for realistic dizziness
      → at quakeInterval: SimulationEffects.simulateEarthquake(level, session) — phase dispatch:
          RUMBLE: breakOrDebris count = ceil(baseCount × magnitude / 5) within magnitude×3 radius
          PEAK: enqueuePeakDestructions — scans for unsupported (air below) blocks sorted
                closest-first; adds batch to pendingDestructions queue
          AFTERSHOCK: breakOrDebris count = ceil(baseCount × effectiveMag / 5); effectiveMag =
                      sessionMagnitude × aftershockMagnitudeScale; radius = effectiveMag × 2.5
      → all three phases use breakOrDebris(): wood/glass blocks spawn FallingBlockEntity
        (visible, gravity-driven, deals 8×fallBlocks fall damage up to 80, then discards);
        non-debris blocks vanish instantly via destroyBlock. DEBRIS_BLOCKS is a static Set for O(1) lookup.
        Debris count scales with effective magnitude so stronger quakes produce more falling blocks.
      → every tick: drainEarthquakePending — breaks 2 blocks from cascade queue (PEAK only)
      → every 20 ticks: clearFireInArena — removes vanilla fire that spreads during block destruction
  → sends SimulationStatusPayload HUD sync every 10 ticks
      → includes per-player intensity = magnitude * exp(-decayRate * distance) * phaseScale
      → client SimulationHud.onCameraAngles applies multi-layer shake (slow 1 Hz + mid 3 Hz oscillations +
        random jitter + roll tilt) all scaled by intensity for realistic dizziness; roll uses setRoll()
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
| `SimulationEffects` | World mutation: fire placement + smoke particles (14×, wide plume) + proximity nausea/air-drain; phase-aware earthquake (RUMBLE/PEAK/AFTERSHOCK helpers + cascade drain + `breakOrDebris` for falling debris with 8× damage multiplier) |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = fire, higher Z = quake), login/button-click handlers |
| `StructurePlacer` | Interface for placing a structure at a `BlockPos`; implemented by both loaders below |
| `SimulationStructureLoader` | Implements `StructurePlacer`; wraps `StructureTemplateManager` for `.nbt` files |
| `SchemLoader` | Implements `StructurePlacer`; parses Sponge Schematic v2/v3 `.schem` files, supports 0–3 CCW 90° rotations (rotates offsets and block states), and places blocks |
| `SimulationStatusPayload` | Server→client packet (record + `StreamCodec`, channel v2) carrying `status`, `timeLeft`, and `intensity` |
| `SimulationHud` | Client-side HUD renderer; drives multi-layer camera shake (1 Hz + 3 Hz oscillations + jitter + roll) via `ViewportEvent.ComputeCameraAngles` using `intensity` |
| `Config` | `ModConfigSpec` entries for all simulation tuning knobs |
| `ModCommands` | Thin registration shell — delegates to `RegistrationCommands`, `ItemCommands`, `SimulationCommands`, `BfpAdminCommands`; also forwards `clearAuthorizations()` |
| `RegistrationCommands` | `/register <student_id> <section> <full_name>` |
| `ItemCommands` | `/spawn_lspu`, `/get_extinguisher`, `/get_co2_extinguisher` |
| `SimulationCommands` | `/sim_fire`, `/sim_earthquake`, `/sim_magnitude`, `/sim_stop`, `/sim_status`, `/sim_list`, `/sim_freeze`, `/sim_unfreeze`, `/sim_time` |
| `BfpAdminCommands` | All `/bfp` admin commands; owns `bfpAuthorized` Set and `isBfpAuthorized()` predicate |
| `FireExtinguisherItem` | Custom item; right-click extinguishes fire blocks and calls `SimulationManager.getSession(uuid).recordExtinguish()` |
| `CO2ExtinguisherItem` | Green CO2 extinguisher for Class C electrical fires. Same pin-pull → hold-spray flow as `FireExtinguisherItem`. Targets `ComputerBlock` with `BURNING=true` → sets BURNING=false + LIT=false + BROKEN=true (computer is destroyed after fire). Also suppresses regular fire/soul fire. 200 durability. |
| `ComputerBlock` | Custom block in `block/` package; has `FACING` (horizontal), `LIT`, `BURNING`, and `BROKEN` states. State machine: OFF↔ON (right-click), ANY→BURNING (flint & steel — immediately places fire on all adjacent air), BURNING→BROKEN (CO2 extinguisher). `BURNING=true`: scans 2-block radius every `randomTick` for `ignitedByLava()` blocks and seeds vanilla fire next to them (enabling chain-spread into wood/wool/leaves); `animateTick` emits FLAME from top + all 4 sides, SOUL_FIRE_FLAME (cyan electrical signature), wild ELECTRIC_SPARK arcs, LARGE_SMOKE columns, LAVA ember drips; light level 15. `BROKEN=true`: cracked screen + scorched case texture, all interactions blocked, emits occasional SMOKE wisps. Only CO2 extinguisher ends the fire (and causes BROKEN). Registered via `BLOCKS.registerBlock(name, Constructor::new, () -> Props)` pattern required by NeoForge 26.x. |
| `TutorialStage` | Enum of all tutorial stages: `NOT_STARTED → PASS_SPRAY → EXT_TYPE_A/B/C → QUAKE_DROP/COVER/HOLDON → COMPLETED` |
| `TutorialManager` | Static utility: station placement, interaction dispatch, extinguish counting, QUAKE tick detection, completion. Gates simulation buttons via `isComplete(UUID)` |
| `TutorialSavedData` | Extends `SavedData`; persists `Map<UUID, TutorialStage>` to `world/data/berongsmp_tutorial.dat` |
| `TutorialStatusPayload` | Server→client packet carrying `prompt` (String) and `intensity` (float) for tutorial HUD and camera shake |
| `TutorialHud` | Client-side HUD renderer for tutorial prompts; drives camera shake during QUAKE stages; hidden when SimulationHud is active |
| `StudentSession` | POJO holding per-student data: name, account UUID, start/end times, tutorial timing, simulation type/score/passed, Turso row ID |
| `TursoClient` | HTTP wrapper for the Turso libSQL REST API (`/v2/pipeline`); fire-and-forget async writes via `CompletableFuture`, synchronous reads for commands; creates schema on first init |
| `SessionManager` | Manages `Map<UUID, StudentSession>` for shared station accounts; hooks into tutorial completion and simulation end to persist scores; exposes `/bfp` admin flow |
| `FireAlarmBlock` | Wall-mounted block in `block/` package; states `FACING` + `ACTIVATED`. Right-click during an active FIRE simulation sets `ACTIVATED=true`, plays bell sound, logs `fire_alarm_activate` telemetry event. Auto-resets when simulation structure is restored. |
| `AssemblyZone` | Static utility in `world/`; defines the safe-zone AABB outside the LSPU Library. Spawns green `HAPPY_VILLAGER`+`SCRAPE` particle force-field border every 5 ticks during simulations. Detects player entry → fires `assembly_area_reached` event + ends simulation with `end_reason=assembly_reached`. Coordinates are PLACEHOLDER — tune with F3 in-game. |
| `TelemetryCsvWriter` | Writes per-tick and event rows to `run/telemetry/gameplay_logs_<YYYYMMDD>.csv` per telemetry contract v1.1 (§3). Also writes session-level sidecar `sessions_<YYYYMMDD>.csv` (§5) and one-time `map_metadata.json` on first server start. Buffered, synchronous, server-thread only. |
| `ExitZones` | Static record list in `world/`; defines three named AABB emergency-exit zones (`main_exit`, `side_exit`, `rear_exit`) near the LSPU Library doors. Per-tick check in `SimulationManager` fires `emergency_exit` CSV event once per session crossing. All coordinates are PLACEHOLDER — tune with F3 in-game. |

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
| `tursoUrl` | `""` | Turso database HTTPS URL (e.g. `https://mydb-org.turso.io`). Leave blank to disable session tracking |
| `tursoToken` | `""` | Turso Bearer auth token from the Turso dashboard |
| `passThresholdFire` | 5 | Fires extinguished required for a FIRE session to be marked `passed=1` |
| `quakeRumbleDuration` | 200 | Ticks in RUMBLE phase (10 s) |
| `quakePeakDuration` | 900 | Ticks in PEAK phase (45 s) |
| `quakeAftershockDuration` | 300 | Ticks per aftershock wave (15 s); 2–4 waves follow the main quake |
| `bfpAdminPin` | `""` | PIN for `/bfp login`. Empty = PIN login disabled (OP-only access). A WARN is logged at startup if left blank. |

### Student Session System (`session/` package)

Shared station accounts (e.g. `station1`) rotate through multiple students. `SessionManager` tracks a `StudentSession` per account UUID, persisted to the **Turso** cloud database via HTTP (no JDBC driver — uses Java's built-in `HttpClient` + Gson). All writes are fire-and-forget (`CompletableFuture.runAsync`). `TursoClient` creates the schema on first `init()` call.

**`/bfp` admin commands** (OP level 2 or `/bfp login <pin>`):

| Command | Effect |
|---|---|
| `/bfp login <pin>` | Authenticate with config PIN; grants all /bfp access |
| `/bfp logout` | Revoke PIN-based access |
| `/bfp checkin <student_name>` | Start session for caller; resets tutorial state |
| `/bfp checkin <player> <student_name>` | Start session for target player |
| `/bfp checkout` | Finalise and save the caller's session |
| `/bfp reset [player]` | Wipe tutorial + delete DB row (no record kept) |
| `/bfp tutorial [player]` | Reset tutorial + teleport to lobby + re-init NPCs |
| `/bfp note <text>` | Append instructor observation to active session (bfp_notes column) |
| `/bfp confidence <1-5>` | Set instructor confidence rating 1.0–5.0 (confidence column) |
| `/bfp prep_level <none|low|moderate|high>` | Set prep-level assessment (prep_level column) |
| `/bfp score <0-100> [player]` | Manually override simulation score |
| `/bfp pass [player]` | Mark session as passed (passed=1) |
| `/bfp fail [player]` | Mark session as failed (passed=0) |
| `/bfp session info` | Print current session details to chat |
| `/bfp sessions list [page]` | List 10 most recent sessions from DB |
| `/bfp sessions today` | List all sessions started today |
| `/bfp sessions stats` | Aggregate stats: total, pass rate, avg score, fire vs quake breakdown |
| `/bfp sessions search <query>` | Search sessions by student name or station account (partial match) |
| `/bfp sessions export` | Write all sessions to `run/bfp_sessions_export.csv` |
| `/bfp student <name>` | Look up last 10 sessions for a student name |

**Simulation management commands** (OP level 2):

| Command | Effect |
|---|---|
| `/sim_status [player]` | Live snapshot: type, phase, time remaining, fires extinguished |
| `/sim_list` | List all active simulations across all players |
| `/sim_freeze [player]` | Pause simulation timer (effects continue) |
| `/sim_unfreeze [player]` | Resume a frozen timer |
| `/sim_time set <seconds>` | Set remaining simulation time |
| `/sim_time add <seconds>` | Add/subtract seconds from remaining time |
| `/get_co2_extinguisher` | Give CO2 extinguisher for Class C fires (any player) |

**Auto-hooks**: `TutorialManager.completeTutorial` → `SessionManager.onTutorialComplete` (records tutorial duration); `SimulationManager.endSimulation` → `SessionManager.onSimulationEnd` (records type/score/passed, closes row).

**Event log flush ordering**: `endSimulation` must capture `studentDbRowId` via `SessionManager.getActiveSession(uuid)` **before** calling `onSimulationEnd`, because `onSimulationEnd` calls `activeSessions.remove()` which would make the subsequent lookup return null and skip the `TursoClient.updateEventLog()` call. Fixed in commit `c5316d0`.

### Thread Safety

`SimulationManager.activeSessions` is a `ConcurrentHashMap`. `startSimulation` and `endSimulation` are `synchronized`. Tick-driven mutations are single-threaded via `ServerTickEvent.Post`. Network packet handling uses `context.enqueueWork()` to marshal HUD updates (including the new `intensity` field) onto the main client thread. `SimulationHud.intensity` is written on the main client thread and read from `ViewportEvent.ComputeCameraAngles`, which also fires on the main client thread — no extra synchronisation needed.

### Block Registration Pattern (NeoForge 26.x)

Custom block subclasses **must** use `BLOCKS.registerBlock(name, Constructor::new, () -> Block.Properties.of()...)` — NOT `BLOCKS.register(name, () -> new MyBlock(Block.Properties.of()...))`. In NeoForge 26.1.2, `BlockBehaviour.<init>` calls `effectiveDrops()` which requires the registry key to already be set on the Properties object. The `registerBlock` overload injects the key before passing Properties to the constructor; the plain `Supplier` overload does not. Using a plain `Supplier` causes a `NullPointerException: Block id not set` crash at startup.

### Client–Server Split

`BerongSMPClient` is annotated `@Mod(dist = CLIENT)` and only loads on the physical client. `SimulationHud` and `KeyMappings` are client-only classes registered through the mod event bus, keeping the server JAR free of rendering dependencies.

---

## Health-Check Remediation Log

Tracks fixes applied from the 2026-06-23 health check report.

| # | Severity | Item | Status | Commit |
|---|---|---|---|---|
| C-1a | 🔴 | Assembly zone force-field on wrong face (Z+ instead of Z-) | ✅ Done | `cf57f50` |
| C-1b | 🔴 | AssemblyZone/ExitZones placeholder coordinates | ⏳ Pending in-game F3 tuning | — |
| C-2  | 🔴 | Default BFP PIN was hardcoded `"1234"` | ✅ Done | (this commit) |
| W-1  | 🟡 | ModCommands.java monolith (807 lines) | ✅ Done | (this commit) |
| W-2  | 🟡 | onServerTick() mixes fire/quake/telemetry/HUD | ✅ Done | (this commit) |
| W-3  | 🟡 | Silent `catch (Exception ignored)` in TursoClient | ✅ Done | (this commit) |
| W-4  | 🟡 | Zero unit test coverage | ⏳ Pending | — |
| L-1–4 | 🟢 | Low-risk items (metadata coupling, station offsets, PIN rate-limit, Turso warn) | ⏳ Pending | — |
