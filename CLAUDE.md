# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Master Plan

See **`docs/major_plan.md`** for the full phased implementation plan, architecture overview, Turso schema, and phase status tracking. All phases (1–5) are complete.

## Documentation Map

This file is a lean index; deep-dive content lives under `docs/`:

- **`docs/systems/`** — living architecture reference for major subsystems:
  [tutorial.md](docs/systems/tutorial.md), [simulation.md](docs/systems/simulation.md),
  [academy.md](docs/systems/academy.md)
- **`docs/history/`** — frozen remediation logs and point-in-time reviews, kept for context but not
  updated going forward: [health-check-log.md](docs/history/health-check-log.md),
  [telemetry-gap-log.md](docs/history/telemetry-gap-log.md),
  [hazard-visual-log.md](docs/history/hazard-visual-log.md),
  [furniture-visual-log.md](docs/history/furniture-visual-log.md),
  [cruz-pathfinding-recommendations.md](docs/history/cruz-pathfinding-recommendations.md),
  [hazard-3state-log.md](docs/history/hazard-3state-log.md),
  [hazard-state-management-log.md](docs/history/hazard-state-management-log.md),
  [hazard-tier-and-tab-reorg-log.md](docs/history/hazard-tier-and-tab-reorg-log.md),
  [code-review-remediation-log-2026-07-17.md](docs/history/code-review-remediation-log-2026-07-17.md),
  [multiplayer-concurrency-audit-log-2026-07-17.md](docs/history/multiplayer-concurrency-audit-log-2026-07-17.md),
  [deep-dive-audit-log-2026-07-17.md](docs/history/deep-dive-audit-log-2026-07-17.md),
  [neoforge-26.2-migration-log-2026-07-17.md](docs/history/neoforge-26.2-migration-log-2026-07-17.md),
  [neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md](docs/history/neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md)
- **`docs/major_plan.md`** — phased implementation plan and Turso schema (see Master Plan above)
- **`docs/code_review_2026-07-17.md`** — full-codebase architecture/security/performance review;
  see the Code Review Remediation Log below for what was fixed from it and what's still open
- **`docs/academy_script.md`** — Academy dialogue script, coordinate tables, and flow diagram
- **`docs/hazard_props_spec.md`** — design spec for all 85 hazard prop blocks (zones, rationale, failure consequences)
- **`docs/skills_manual.md`** — the project skills in `.claude/skills/` and when/how to invoke each
- **`docs/new_sim_building2_rooms.md`** — F3/WorldEdit-surveyed room coordinates for New Sim Building 2.0 (34 rooms, 2 floors), captured via `//copyroom`
- **`docs/usermanual.md`** — player-facing setup guide: installing the client, connecting, and a walkthrough of the Academy + New Sim Building 2.0 flow
- **`docs/adminmanual.md`** — operator-facing guide: hosting on MCServerHost, building/updating the client installer, `/bfp` command quick reference, and the WorldEdit startup-crash fix

When a change touches a system with its own doc, update that doc — not a growing section here.

## Development Workflow

**After completing any feature or phase goal:**
1. Update `CLAUDE.md` — add/update the Key Classes table and architecture notes to reflect what changed.
2. Update `docs/major_plan.md` — mark the completed deliverable `[x]`.
3. Commit the changes with a descriptive message and push to `main`.

This keeps the project documentation in sync with the code at all times.

---

## Project Overview

BerongSMP is a NeoForge mod for Minecraft 26.2.0 (NeoForge 26.2.0.0-beta) that implements a disaster simulation minigame. Players `/register`/`/login`, then enter a lobby with two buttons: the first launches the Academy training tutorial, the second launches New Sim Building 2.0's graded prevention/intervention/evacuation fire scenario (gated on Academy certification). Completing the Academy auto-deploys into that same simulation after a countdown. The old Library/CCS fire-or-earthquake simulations and the legacy tutorial are still reachable via commands (`/sim_fire library`, `/sim_fire ccs`, `/sim_earthquake ccs`, `/bfp old_tutorial`) for dev/admin use. Players are scored on their response. The mod is built with Java 25.

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

### Package Layout

- **`registry/`** — all game-object registrations (`ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModEntities`, `ModSounds`, `ModAttachments`); `BerongSMP` is a thin bootstrap that wires them to the mod event bus.
- **Type-based packages** — `block/` (furniture + interactive blocks and their shared bases), `item/`, `entity/`, `network/` (payloads), `client/` (client-only, see Client–Server Split), `command/`.
- **`common/`** — cross-cutting server subsystems: `common/simulation/`, `common/hazard/` (hazard prop blocks + `HazardManager`), `common/structure/` (loaders, building managers, and the programmatic `building/` modules), `common/player/` (per-player managers + `PlayerLifecycleRegistry`), `common/scheduling/` (`TickScheduler`), `common/telemetry/`, `common/zones/`.
- **Feature packages** — `academy/` (rooms under `room1..room4`), `tutorial/`, `session/`, `registration/`.

New per-tick handlers must self-register via `TickScheduler.register(...)` in a static block **and** be class-loaded by a real code path — Javadoc `{@code}` mentions don't count. If a class has no natural caller, add a `bootstrap()` no-op invoked from `BerongSMP.commonSetup` (see `DuckCoverHoldManager` for the precedent and the bug that motivated it).

### Event Bus Duality

NeoForge uses two separate event buses — a core pattern throughout this codebase:

- **`modEventBus`** — mod lifecycle events (registration, client setup). Used in `BerongSMP` constructor for `DeferredRegister`, network payload registration, and `BuildCreativeModeTabContentsEvent`.
- **`NeoForge.EVENT_BUS`** — runtime game events (server tick, player join, block interact). Classes annotated with `@EventBusSubscriber` auto-register here.

### Tutorial Flow

The old tutorial's stage progression (NOT_STARTED → ... → COMPLETED), NPC dialogue triggers, the
QUAKE drill tick loop, and the logout/reconnect rollback that prevents the shake prompt from
resuming on its own. Full flow: **[docs/systems/tutorial.md](docs/systems/tutorial.md)**.

### Simulation Flow

Session lifecycle (start → fire/quake effects ticking → end/respawn), the Duck-Cover-Hold and
Drop-and-Roll live safety mechanics, and the earthquake phase state machine. Full flow:
**[docs/systems/simulation.md](docs/systems/simulation.md)**.

### New Tutorial Building (Academy)

A second, fully independent tutorial — "the Academy" — with its own 4-room progression (Cruz,
Reyes, Santos, Morfe), telemetry, guardrails, and world-space compass navigation. Deliberately
does not reuse the old `tutorial/` package. Full architecture, dialogue flow, and every fix log:
**[docs/systems/academy.md](docs/systems/academy.md)**.

### Key Classes

| Class | Responsibility |
|---|---|
| `BerongSMP` | Mod entry point — a thin bootstrap since the registry extraction. Wires the `registry/Mod*` classes to the mod event bus, registers network payloads and lifecycle listeners, calls `DuckCoverHoldManager.bootstrap()` in `commonSetup` (forces the class load that registers its tick handler), and performs one-time world setup (`onServerStarting`/`onServerStarted`). Still owns `MODID` + `LOGGER`. |
| `registry/ModBlocks` | Facade over all 179 block registrations — 1311 lines until 2026-07-14, when it was split into 9 package-private domain registrar classes in the same package (`ModBlocksFurnitureClassroom`, `ModBlocksHazardsSchoolKitchen` [55 school/kitchen hazard props], `ModBlocksSchoolDecor`, `ModBlocksSafetyEquipment`, `ModBlocksCafeteria`, `ModBlocksSportsCourts`, `ModBlocksConference`/`ModBlocksOffice`/`ModBlocksLaboratory` [furniture + hazard props 56–85 each, interleaved in original declaration order]). `ModBlocks` itself now only directly registers `EXAMPLE_BLOCK`/`COMPUTER`/`FIRE_ALARM_BLOCK` and re-exports every sub-registrar field as a `public static final` in original declared order — every other file in the codebase still reads `ModBlocks.X`, zero call-site changes from the split. Registration order (and thus creative-tab ordering / `/item hazard` tab-complete order) was verified byte-identical before/after via a static-init-order simulation script. 8 of the 85 hazard props (`corroded_gas_line_joint`, `ebike_charging_station`, `dry_aquarium_heater`, `rodent_chewed_wiring`, `dusty_crt_monitor`, `shorted_bench_supply`, `overheated_vacuum_pump`, `faulty_dehumidifier`) register `SimpleHazardFacingBlock`/`SimpleHazardBlock` (see those + `HazardSpec`) instead of a dedicated class, as a proof-of-concept for eventually deduping the rest. |
| `registry/ModItems` | All `DeferredItem` registrations (NPC spawners, block items, extinguishers, hazard wand, firefighter uniform + `ArmorMaterial`). Owns `HAZARD_ITEM_MAP` (LinkedHashMap; `/item hazard` insertion order, unchanged) plus `ALL_ITEM_MAP` (superset for `/item get` / `/item kit`) and, since the 2026-07-12 tab-reorganization pass, the 4 `HAZARD_ZONE_*` key lists (`HAZARD_ZONE_CLASSROOM`/`_KITCHEN`/`_ELECTRICAL_LAB`/`_CONFERENCE_OFFICE`) — unchanged by the later 5-tab consolidation below; `ModCreativeTabs` now merges pairs of these lists at display time instead of giving each its own tab. `assertHazardZonesCoverMap()` (called from `ModCreativeTabs`'s static init) fails fast if these ever drift out of sync with the map. **2026-07-14:** the 24 NPC spawner registrations were split out to `registry/ModItemsNpcSpawners` (package-private, same re-export-facade pattern as `ModBlocks`'s split) — `ModItems` re-exports them in original order. The remaining ~194 registrations (block items, many with an inline `HAZARD_ITEM_MAP.put(...)` static block right after) stay in `ModItems` for now: unlike `ModBlocks`, most of this file interleaves map population with each item's own registration line, which is a messier split than time allowed for in one pass — documented backlog, not attempted further this session. |
| `registry/ModCreativeTabs` | **Exactly 5 mod creative tabs — this is a hard cap, not a style choice.** `SIM_TAB` (sim_tab — extinguishers, computer, fire alarm, safety-equipment items, fire hose cabinet), `FURN_TAB` (furn_tab — all furniture: school/classroom + cafeteria + Conference Room/Office/Laboratory, 82 items), `HAZARD_SCHOOL_TAB` (hazards_school_tab — merges `HAZARD_ZONE_CLASSROOM` + `HAZARD_ZONE_KITCHEN`, 43 items), `HAZARD_OFFICE_LAB_TAB` (hazards_office_lab_tab — merges `HAZARD_ZONE_ELECTRICAL_LAB` + `HAZARD_ZONE_CONFERENCE_OFFICE`, 42 items), `NPC_TAB` (npc_tab — all 24 NPC spawners). A same-day 2026-07-12 first attempt split into 9 tabs (4 hazard + 3 furniture); NeoForge 26.1.2.36-beta's patched `CreativeModeInventoryScreen` paginates sorted tabs into pages of 10 and splits each page 5-top-row/5-bottom-row, vanilla alone fills page 1 (10 tabs), so every mod tab lands on page 2 — the first 5 (top row) rendered, the remaining 4 (bottom row) did not, matching the user's exact bug report ("only 1 hazard tab shown, others missing"). Verified against the actual decompiled NeoForge source (`CreativeTabsScreenPage.java`'s `maxLength = 10; topLength = maxLength / 2`), not guessed. Fixed same-day by consolidating to exactly 5 — see `ModCreativeTabs`'s class javadoc and `docs/history/hazard-tier-and-tab-reorg-log.md` §4. **Any future tab-count change must stay at ≤5 mod tabs and be verified in-game (page to page 2, confirm every tab actually renders) before trusting it.** |
| `registry/ModEntities` | `CUSTOM_NPC` entity type + its attribute-creation listener. |
| `registry/ModSounds` | `FIRE_ALARM_RING` sound event. |
| `registry/ModAttachments` | `DROPPED_TICKS` synced attachment driving the client drop-and-roll animation. |
| `SimulationManager` | Session registry (`ConcurrentHashMap<UUID, SimulationSession>`), tick driver, event handlers for tick/respawn/logout. **2026-07-14:** the three scenario state machines that used to be private methods here (`tickFireSession`/`tickNewSim2FireSession`/`tickEarthquakeSession`) were extracted verbatim into package-private ticker classes in the same package — `LegacyFireTicker.tick` (Library/CCS), `NewSim2FireTicker.tick` (New Sim Building 2.0's `FirePhase` machine — own branch, not `LegacyFireTicker`), `EarthquakeTicker.tick` — see [docs/systems/simulation.md](docs/systems/simulation.md). `FIRE_EFFECTS`/`EARTHQUAKE_EFFECTS` fields and `emitPhaseTransition` widened from `private` to package-private so the tickers can reach them; `startSimulation`/`endSimulation` stay `synchronized`. `endSimulation`'s New Sim Building 2.0 branch (2026-07-13) no longer teleports to Capt. Morfe's debrief immediately — it schedules it 5 seconds out via `TickScheduler.scheduleOnce`, re-checking the player is still online/alive/session-free at fire time. **2026-07-17 (code review remediation, see [docs/history/code-review-remediation-log-2026-07-17.md](docs/history/code-review-remediation-log-2026-07-17.md)):** `startSimulation`/`endSimulation` no longer re-place all 4 buildings — the old `BUILDINGS` list is now reserved for the `/place_buildings` dev command alone. Each session places/restores only its own physical `Arena` (`LIBRARY`/`CCS`/`NEW_SIM_BUILDING2`, see `arenaFor`/`ARENA_BUILDINGS`/`placeArena`), and a new `arenaOccupants: Map<Arena, UUID>` refuses a second session from starting in an arena another session is still using — previously any session start/end/logout anywhere would silently rewrite every other in-progress session's arena to its clean schematic state. Also: `nearestCCSHazardDistance` no longer scans a 31×31×11 box every tick — it checks the session's cached `computerPositions` for `BURNING` state instead; `persistSessionEnd` splits the final Turso write into a core-fields UPDATE and a separate blob-columns UPDATE (see `SimulationSession`); pass/score for legacy fire runs now goes through `SimulationScoring` instead of being computed inline. **2026-07-17 (multiplayer/concurrency audit):** the private `Arena` enum + `arenaFor` were extracted into a new standalone `SimulationArenas` class (see its own row) so the state→arena mapping is unit-testable; `startSimulation` now calls `clearExistingExtinguishers()` before issuing fresh ones for any fire-type run — it previously overwrote a single hotbar slot unconditionally, silently destroying a player's existing extinguisher (e.g. one legitimately collected in the Academy's Room 2) if it happened to be there. |
| `SimulationArenas` | (2026-07-17) Standalone `Arena` enum (`LIBRARY`/`CCS`/`NEW_SIM_BUILDING2`) + `arenaFor(SimulationState)`, extracted out of `SimulationManager` specifically so this one piece of the arena-occupancy logic is unit-testable (`SimulationArenasTest`) without touching `SimulationManager` itself — merely referencing that class triggers its full static init, which constructs `Identifier`s via `BerongSMP.MODID`, and `BerongSMP`'s own static init needs NeoForge's `LogUtils` (unavailable outside a live game runtime). Only references `SimulationManager.SimulationState`, a nested enum with its own independent static initializer (same precedent as `SimulationStateTest`). |
| `LegacyFireTicker` / `NewSim2FireTicker` / `EarthquakeTicker` | The three per-scenario tick handlers extracted from `SimulationManager` (2026-07-14, see above). Each is a package-private, stateless, single-static-method class in `common/simulation/`; `SimulationManager.onServerTick`'s dispatch calls `X.tick(...)` directly in place of the old private method names. |
| `SimulationSession` | Per-player mutable state: timer ticks, disaster type, fires extinguished count, earthquake epicenter/phase/cascade queue/magnitude/aftershockCount/aftershockMagnitudeScale; `arenaOrigin/spanX/spanZ/height` set by SimulationManager to target the correct building. `initialTimerTicks`/`elapsedTicks()`/`elapsedSeconds()` are the correct anchor for telemetry `t` (not `Config.SIM_DURATION_TICKS − timerTicks`, which breaks for a session whose duration isn't the config default — see `bindDuration`). New Sim Building 2.0 fields: `armedHazards`/`preventedHazards`/`escalatedHazards`/`extinguishedEscalated`/`alarmRung`. `bufferFireLogRow(String row)`/`buildFireLogCsv()` (2026-07-14) buffer every real fire block ignite/extinguish the same way `bufferCsvRow`/`buildMoveCsv` buffer move ticks — rows come pre-formatted from `TelemetryCsvWriter.writeFireLogRow`, sent once at session end as the `fire_log_csv` Turso column. `getActiveFireSources()`/`addFireSource`/`removeFireSource` (2026-07-14, a `Set<BlockPos>`) track real currently-burning positions — `SimulationEffects.simulateFire` only ever scatters new fire near a member of this set, never at an arbitrary arena position; see `SimulationEffects`. **2026-07-17 (code review remediation):** `setTimerTicks` (used by `/sim_time set/add`) now shifts `initialTimerTicks` by the same clamped delta it applies, so a GM adjusting the timer no longer corrupts every subsequent telemetry `t` value. `bufferCsvRow`/`bufferFireLogRow` cap their buffers at 3 MB / 512 KB respectively (with a truncation marker) instead of growing unboundedly at 20 Hz — the local telemetry CSV always has the full record regardless. |
| `SimulationSession.EarthquakePhase` | Inner enum: `RUMBLE → PEAK → AFTERSHOCK(×2–4) → END`; AFTERSHOCK loops with a random magnitude scale before advancing to END |
| `SimulationSession.FirePhase` | Inner enum: `PREVENTION → INTERVENTION → EVACUATION → END`, New Sim Building 2.0 only (null for every other scenario). Mirrors `EarthquakePhase`'s shape — `tickFirePhase()` is pure bookkeeping, `NewSim2FireTicker.tick` does the actual world mutation on the phase-change edge. |
| `NewSimScoring` | Rule-based scorer for a completed New Sim Building 2.0 run — the mod's first implementation of telemetry_contract.md v1.2's "game-computed rule-based `prep_level`" (≥75 HIGH / 40–74 MODERATE / <40 LOW). Independent of `AcademyScoring`/`AcademyProgress`; scores a real `SimulationSession`, not a tutorial attempt. Plain Java (no NeoForge/Minecraft dependency), so it's directly unit-tested (`NewSimScoringTest`, 2026-07-17). |
| `SimulationScoring` | (2026-07-17, code review remediation) Single source of truth for a legacy fire-type run's (Library/CCS) score/pass rule — `fireScore`/`firePassed` — extracted after `SimulationManager.endSimulation` and `SessionManager.onSimulationEnd` were found computing pass/fail two different ways (score-based vs. fires-based directly; the config comment on `passThresholdFire` documents fires-based as the intent). Deliberately takes `passThreshold` as a parameter rather than reading `Config.PASS_THRESHOLD_FIRE` itself, so it stays plain and unit-testable (`SimulationScoringTest`) — NeoForge's `ModConfigSpec.ConfigValue.get()` throws outside a fully loaded mod environment. |
| `FireEffects` / `EarthquakeEffects` | World mutation, split (2026-07-14) from the former single `SimulationEffects` class (356 lines bundling two unrelated concerns) into two singletons held by `SimulationManager`. `FireEffects`: fire placement + smoke particles (14×, wide plume) + proximity nausea/air-drain. **`simulateFire` no longer scatters at an arbitrary arena-wide position** — it picks a random member of `SimulationSession.getActiveFireSources()` (pruned of anything that's burned out, via `isActuallyOnFire`) and offsets within `FIRE_SPREAD_RADIUS` (4 blocks), with each new fire block joining the source pool itself for real outward chain-growth. A no-op if nothing is burning — `HazardManager.triggerFailure`/`defuse` add/remove sources, and `SimulationManager.startSimulation`'s plain-FIRE branch force-fails one random hazard at session start as a bootstrap ignition. `EarthquakeEffects`: phase-aware earthquake (RUMBLE/PEAK/AFTERSHOCK helpers + cascade drain + `breakOrDebris` for falling debris with 8× damage multiplier). |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = tutorial, higher Z = simulation), login/button-click handlers. **2026-07-13 rewire:** button 1 calls `AcademyManager.startAcademyRun` (was `CCS_FIRE`); button 2 calls `SimulationManager.startSimulation(NEW_SIM_BUILDING2_FIRE)` (was `CCS_EARTHQUAKE`), gated by `academyCertified` (this session's Morfe `EVALUATED_PASS`, or a restored `/login` account's `AuthManager.isTutorialCompleted`) on top of the shared `baseGatesPassed` (registration + active session — the old-tutorial-completion gate is gone). `routePlayer` always teleports to the main lobby now; the old routing to a separate tutorial lobby for old-tutorial-incomplete players, and the custom `onPlayerRespawn` handler, were removed (world respawn is already pinned to the lobby by `BerongSMP.onServerStarting`). |
| `NpcType` | Enum of all 24 NPC characters, split into `id` (stable identity string persisted to entity NBT and matched by `fromId` — **never renamed**, since the `academy_building.schem`'s baked-in NPCs and any world's saved entities round-trip through this exact string) and `texturePath` (full path under `textures/entity/npc/`, minus extension — safe to reorganize into subfolders freely since `CustomNpcRenderer` resolves it fresh every render instead of assuming a flat `id + ".png"` layout). Declared in 4 groups matching the texture folder taxonomy: `new_tutorial_instructors/` (4 active Academy room-driving instructors — Reyes, Santos, Cruz, Morfe), `others/` (5 decorative Academy background NPCs — Tuazon, DM Orlanda, Necookie, Sir Bookmark, Student), `sim_building_prof_npc/` (4 faculty), `sim_building_students/` (11 students). Both `sim_building_*` sets were added as hand-made 64×64 skins; `principal_brown.png` shipped in the legacy 64×32 no-overlay-layer format and was padded to 64×64 (transparent bottom half) before use, since `HumanoidModel`'s outer layer samples UV rows that only exist in the modern layout. |
| `StructurePlacer` | Interface for placing a structure at a `BlockPos`; implemented by both loaders below |
| `SimulationStructureLoader` | Implements `StructurePlacer`; wraps `StructureTemplateManager` for `.nbt` files |
| `SchemLoader` | Implements `StructurePlacer`; parses Sponge Schematic v2/v3 `.schem` files, supports 0–3 CCW 90° rotations (rotates offsets and block states), places blocks, and spawns entities from the `Entities` tag — including modded mobs like `berongsmp:custom_npc`, not just vanilla decoration entities. Any pre-existing non-player entity in the placement footprint is discarded before re-placing to prevent duplicates (broadened from an item-frame-only check once schematics started baking in mobs/armor stands too — session restores never touch players, since they're explicitly excluded). **Item frame placement invariant (MC 26.x):** Sponge v3 top-level `Pos` = the entity's own AIR block (not the wall). `Data.Facing` = OUTWARD direction (frame face toward viewer, no `.getOpposite()` needed). `ItemFrame(level, pos, direction)` takes the entity's own block as `pos`; the wall is `pos.relative(direction.getOpposite())` — handled by a dedicated `spawnItemFrame` path since item frames need this wall-relative math the generic path doesn't do. **Generic entity deserialization gotcha (found while loading `academy_building.schem`):** Sponge v3 nests an entity's *real* Minecraft save data under a `Data` sub-compound — `Id`/`Pos` are Sponge-level siblings, not part of it. Deserializing the raw entity tag directly (as this used to) left every actual field invisible to `EntityType.create`, so `CustomNpcEntity` silently read a missing `NpcType` and fell back to its default role for every copy. Fixed by merging `Data` into a fresh root before deserializing. That same root also needs `block_pos` (used by `BlockAttachedEntity` subclasses like `Painting`, checked with a 16-block sanity radius against the entity's real position) and any `facing`/`Facing` byte re-derived/rotated — left stale, `block_pos` still pointed at the original copy location, failed the sanity check, and vanilla logged "Block-attached entity at invalid position" while the entity failed to attach. |
| `AcademyBuildingManager` | Places `academy_building.schem` (a WorldEdit `//copy -e` capture) at the fixed `POS = BlockPos(-177, -34, 8)` via `SchemLoader`, 0 rotations. The schematic bakes in its own 10 `berongsmp:custom_npc` NPCs (the duplicate second Officer Cruz at the old two-NPC-handoff spot was NBT-edited out of the .schem itself — 29 → 28 entities), 10 gear-display armor stands, item frames/glow item frames, and a painting — unlike `TutorialLobbyManager` (structure + hardcoded-offset NPCs as two separate passes), everything here comes from one `SchemLoader.place` call. Called from `onServerStarted`, not `onServerStarting`, for the same reason `TutorialLobbyManager.initNpcs` is: entity chunk storage must be fully loaded first, or freshly-spawned entities can collide with same-UUID copies the previous server run persisted to disk. Also owns `VIEWPOINTS` (`Map<String, Viewpoint>`) — named F3-captured admin teleport targets inside the building, one per named station, surfaced via `/bfp tutorial <name>`. `sweepStrayCruz` (public, tiny single-chunk AABB query around `(-122,-33,49)`) is called every tick from `AcademyManager.tick()` — not just once at boot — since a disk-persisted stray entity there only ever becomes visible to `getEntitiesOfClass` once a player walks close enough for that chunk to reach Minecraft's entity-ticking ring, so a boot-only scan could never actually catch her. `discardDuplicateCruz` calls `sweepStrayCruz` once at boot too, then — only if more than one legitimate `OFFICER_CRUZ` copy still remains — keeps the one nearest the briefing anchor (a lone survivor is never touched). `placeGreenMarks` is a permanent per-placement fixup: swaps the 4 floor blocks under `CruzRoomManager.GREEN_MARKS` to lime concrete (the schematic has no green blocks in the briefing zone), re-run after every placement since `SchemLoader` restores the original floor each boot. (A `placeFireAlarm` method used to also place a `FireAlarmBlock` for Sgt. Reyes's alarm checkpoint here — removed 2026-07-05 once parsing the schem revealed it already bakes one in at that exact spot, see Room 2 above.) |
| `SimulationStatusPayload` | Server→client packet (record + `StreamCodec`, channel v2) carrying `status`, `timeLeft`, and `intensity` |
| `DropAndRollPayload` | Client→server packet (channel v3) — the mod's first serverbound payload; empty record, `StreamCodec.unit(...)`, sent when the player presses the "Drop and Roll" key. Handler calls `DropAndRollManager.onDropAndRollRequest`. |
| `DropAndRollManager` | Static per-UUID transient state (`droppedTicksRemaining`, same idiom as `TutorialManager.holdOnTimers`) driving the "stop, drop, and roll" fire response: reduces the requester's remaining fire ticks by 30/press if on fire, opens/extends a 100-tick "dropped" window during which `MobEffects.SLOWNESS` is continuously refreshed (crawl stand-in). No sound on press (removed 2026-07-14, particles only). `tick()` runs from `SimulationManager.onServerTick`. `tickStickyFire` (2026-07-14) also enforces "fire only goes out via drop-and-roll" during any active fire-type session: a `stickyBurning` `Set<UUID>` (independent of the live `remainingFireTicks` value, so vanilla's own water-contact clear can't extinguish it) keeps re-topping fire ticks to a 60-tick floor and dealing 3.0 damage/20 ticks (vanilla default is 1.0) until the player starts a drop-and-roll, leaves the session, or logs out. **2026-07-17:** `onDropAndRollRequest` (the handler for the mod's only serverbound payload, `DropAndRollPayload`) now rate-limits to one request per 2 ticks per player — it previously had no server-side limit at all, so a modified client bypassing the client-side edge-triggered key check could spam it in a tight loop. |
| `DuckCoverHoldManager` | Checks every online player every tick, independent of any active session, for crouching + solid cover above (or beside a `TableBlock`) — `ticksHeld`/`achievedThisHold` track the continuous-compliance streak toward `TARGET_TICKS` (5s). **2026-07-17:** gained a `PlayerLifecycleRegistry` logout hook clearing both maps directly — previously they were only cleared for a player found non-compliant while iterating the *currently online* list, so a player who disconnected mid-hold (still compliant at that instant) left a permanently stale entry in both maps for the life of the server. |
| `TickScheduler` | Registry of per-tick handlers (`TutorialManager`/`DropAndRollManager`/`DuckCoverHoldManager`/`AcademyManager`/`SafetyDeviceManager`, called in registration order from `SimulationManager.onServerTick`) plus, since 2026-07-13, a one-shot delayed-task queue: `scheduleOnce(delayTicks, task)` decrements/fires alongside the per-tick handlers in the same `tick(level)` call, no extra synchronisation. Backs the two 5-second Academy↔New Sim Building 2.0 handoffs (Morfe's post-PASS deploy, the post-simulation debrief delay) — tasks re-fetch their player by UUID at fire time rather than capturing a live reference, so a logout/death/new-session during the wait is a safe no-op. **2026-07-17 (code review remediation):** every handler and delayed task now runs inside its own try/catch (throttled error logging, once/10s per handler) — an uncaught exception from any one subsystem used to propagate straight out of `tick()` and crash the whole server. |
| `SimulationHud` | Client-side HUD renderer; drives multi-layer camera shake (1 Hz + 3 Hz oscillations + jitter + roll) via `ViewportEvent.ComputeCameraAngles` using `intensity` |
| `ItemDescriptionTooltip` | Client-only `ItemTooltipEvent` listener (registered in `BerongSMPClient.onClientSetup`). Appends a gray tooltip line from `<translation key>.desc` (e.g. `block.berongsmp.overloaded_microwave.desc`) whenever the lang file defines one — items with no `.desc` entry are untouched. This is the convention for the ~90 blocks/items that have no dedicated tooltip class: add the text once in `en_us.json`, no Java changes needed. The 9 hand-held tools with their own `appendHoverText` override (extinguishers, hazard wand, first aid kit, fire blanket, flashlight, megaphone, safety whistle) keep their existing hardcoded usage-instruction tooltips instead — those explain *how to use* the item, which is a different job from the flavor/hazard-reason `.desc` line. |
| `Config` | `ModConfigSpec` entries for all simulation tuning knobs. (2026-07-17: removed the dead NeoForge mod-template scaffolding entries — `LOG_DIRT_BLOCK`/`MAGIC_NUMBER`/`MAGIC_NUMBER_INTRODUCTION`/`ITEM_STRINGS` — that had no reference anywhere in the actual simulation logic.) |
| `ModCommands` | Thin registration shell — delegates to `RegistrationCommands`, `ItemCommands`, `SimulationCommands`, `BfpAdminCommands`; also forwards `clearAuthorizations()` |
| `RegistrationCommands` | Player-facing account commands (2026-07-13 rewrite). `/register <username> <password> <student_id> <section> <full_name>` — creates a persistent Turso `student_accounts` account (via `AuthManager.registerAsync`) alongside the existing local registration/session checkin; falls back to local-only with a chat warning if Turso is offline. `/login <username> <password>` restores a returning student's identity and Academy-certified status without replaying the tutorial. `/logout` clears the station's login state. `/history` lists a logged-in student's own last 10 runs. |
| `ItemCommands` | `/spawn_lspu`, `/get_extinguisher`, `/get_co2_extinguisher` |
| `SimulationCommands` | `/sim_fire <library\|ccs>`, `/sim_earthquake <library\|ccs> [magnitude]`, `/sim_magnitude`, `/sim_stop`, `/sim_status`, `/sim_list`, `/sim_freeze`, `/sim_unfreeze`, `/sim_time` |
| `BfpAdminCommands` | All `/bfp` admin commands; owns `bfpAuthorized` Set and `isBfpAuthorized()` predicate. **2026-07-13 rename:** the Academy is now the default tutorial admin surface — `/bfp new_tutorial*` (bare, named viewpoints, `reset`, `skipto`) became `/bfp tutorial*`; the old tutorial's activation command moved from `/bfp tutorial` to `/bfp old_tutorial`. Also gained `/bfp user <username>` (instructor-facing run-history query by account username, alongside the existing `/bfp student <name>`). |
| `CopyRoomCommand` | `//copyroom [name...]` — dev tool (OP level 2) registered as a raw Brigadier literal named `"/copyroom"` (leading slash baked into the literal string, the same mechanism WorldEdit itself uses for its own `//pos1`-style commands — see `com.sk89q.worldedit.command.SelectionCommands`'s `@Command(name = "/pos1")`). Reads the caller's active WorldEdit cuboid selection (`NeoForgeAdapter.get().fromNativePlayer` → `WorldEdit.getInstance().getSessionManager().get(...)` → `LocalSession.getSelection()`), computes Width/Length/Height (inclusive) + FloorArea/WallArea/CeilingArea/Volume, and copies a labeled human-readable summary (`formatRoomSummary` — corners, dimensions, areas/volume, plus the optional trailing `name` argument as a header line, e.g. `//copyroom Room 201`) to the **server machine's** OS clipboard — built for surveying a building room-by-room the same way `SimRoom`'s `CCS_UPPER_ROOMS`/`CCS_GROUND_ROOMS` tables above were compiled. `copyToClipboard` checks `GraphicsEnvironment.isHeadless()` first and shells out to the OS's native clipboard tool (`clip`/`pbcopy`/`xclip` via `ProcessBuilder`) instead of `java.awt.Toolkit` — NeoForge/FML sets `java.awt.headless=true` at bootstrap for both the client and dedicated server JVM (keeps AWT from fighting LWJGL's native window), so the AWT clipboard path always throws `HeadlessException` inside a running Minecraft process; confirmed via an actual in-game failure before this fallback was added. A dev-only tool in the same spirit as `/bfp`/`HazardWandItem` that assumes admin + client run on the same machine, since a dedicated server has no path to a remote player's client clipboard. `run()`'s own body guards with `ModList.get().isLoaded("worldedit")` before touching any WorldEdit class, so a server without WorldEdit installed fails with a clean chat message instead of `NoClassDefFoundError` — **but that guard alone isn't sufficient**: merely loading `CopyRoomCommand.class` (its `catch (IncompleteRegionException e)` clause needs that type resolvable at JVM verification time) crashes a WorldEdit-less server before the guard method ever runs. `ModCommands.register()` (2026-07-15 fix, found via a real MCServerHost deploy without WorldEdit installed) therefore wraps the `CopyRoomCommand.register(dispatcher)` call itself in the same `ModList.isLoaded("worldedit")` check, so the class is never loaded at all when WorldEdit is absent — the fix has to live at the call site, not inside the optional class. WorldEdit is an **optional** dependency (`compileOnly` + `localRuntime`, not `implementation`, both excluding the transitive `worldeditcui-protocol` companion mod — a `-dev` build with a real classloader `LinkageError` against this NeoForge version, unneeded since `//copyroom` never touches the CUI overlay) — see `worldedit_version` in `gradle.properties` and the EngineHub maven repo block in `build.gradle`; the artifact version must have a `worldedit-neoforge-mc<worldedit_mc_version>` build actually published — **2026-07-17 NeoForge 26.2 migration:** this suffix is a separate `worldedit_mc_version` property, not always identical to `minecraft_version` — EngineHub's own artifact-naming convention changed between MC lines (confirmed via `maven-metadata.xml`: the 26.1 line published under the full patch version, `worldedit-neoforge-mc26.1.2`, but the 26.2 line publishes under major.minor only, `worldedit-neoforge-mc26.2`), so always verify against the live Maven metadata before bumping either version independently. See [neoforge-26.2-migration-log-2026-07-17.md](docs/history/neoforge-26.2-migration-log-2026-07-17.md). **2026-07-18:** `localRuntime` is currently commented out in `build.gradle` — WorldEdit 7.4.4 itself requires NeoForge `26.2.0.7-beta`+ (its own `mods.toml` floor), newer than this project's current `neo_version` (`26.2.0.0-beta`, the only build MCServerHost's installer offers for MC 26.2.0). `compileOnly` stays so this class still compiles; `//copyroom` is simply unavailable in the local dev run and on the live server until one of those two versions moves. See [neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md](docs/history/neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md). |
| `AbstractExtinguisherItem` | Shared base for all three extinguishers: safety-pin gate, held-spray ray geometry, durability drain, sound scaffolding, nearby-player count, and charge tooltip. Also owns `KITCHEN_HAZARD_IDS`/`isKitchenHazard` (the twelve Class F/K kitchen hazard prop IDs), `WET_CHEMICAL_UNSAFE_IDS`/`isWetChemicalUnsafe` (the 28 energized-electrical/flammable-vapor Conference Room/Office/Laboratory props — real-world basis: an aqueous wet-chemical agent is conductive and unsafe on live electrical, checked in `WetChemicalExtinguisherItem.extinguishAt`), `OXIDIZER_HAZARD_IDS`/`isOxidizerHazard` (just `leaking_oxygen_cylinder` — dry chemical is unreliable and wet chemical is doubly wrong on an oxidizer-fed fire, checked in both `FireExtinguisherItem` and `WetChemicalExtinguisherItem`, leaving only CO2 valid), and `warnWrongTool` (per-player 60-tick-throttled chat warning). Subclasses supply only `extinguishAt`, particles, `sprayPitch`, telemetry (`onSprayResolved`), and flavour messages. Also emits contract v1.2's `pin_pull` (on first click) and `ext_spray` (throttled to the existing 20-charge-unit cadence) telemetry for any active fire-type session, via the new abstract `extinguisherClass()` hook (`ABC`/`CO2`/`WET_CHEMICAL`) each subclass implements. |
| `FireExtinguisherItem` | ABC dry-chemical extinguisher (extends `AbstractExtinguisherItem`); extinguishes fire/soul-fire/LIT blocks, counts toward FIRE score via `recordExtinguish()`, drives the tutorial PASS drill. 300 durability. Cannot defuse the five kitchen hazard props (`AbstractExtinguisherItem.isKitchenHazard`) — spraying a hazardous one triggers a throttled "wrong extinguisher" chat warning instead. |
| `CO2ExtinguisherItem` | Green CO2 extinguisher for Class C electrical fires (extends `AbstractExtinguisherItem`). Targets `ComputerBlock` with `BURNING=true` → sets BURNING=false + LIT=false + BROKEN=true (computer is destroyed after fire). Also suppresses regular fire/soul fire. 200 durability. Same kitchen-hazard exclusion and warning as `FireExtinguisherItem`. |
| `WetChemicalExtinguisherItem` | Yellow wet chemical extinguisher (extends `AbstractExtinguisherItem`) — Philippine BFP Class F/K colour-coding for cooking-oil/grease fires. Suppresses regular fire/soul fire and defuses any hazard prop via `HazardManager.defuse`, with distinct "saponification complete" feedback + golden `DustParticleOptions` foam mist when the target is one of the five kitchen hazard props (`unattended_grease_pan`, `grease_clogged_hood`, `contaminated_kitchen_bin`, `jammed_panini_press`, `commercial_deep_fryer`). **It is the only extinguisher that can defuse those five** — ABC and CO2 skip them entirely, mirroring the real Class B vs. Class F/K distinction (a dry-chemical/CO2 blast can splash or re-flash a deep-fat fire instead of smothering it). 240 durability. Not auto-issued at simulation start (no dedicated kitchen scenario state yet) — obtainable via `/get_wet_chemical_extinguisher`, `/item get wet_chemical_extinguisher`, or `/item kit`. |
| `HazardWandItem` | Dev-only tool (`berongsmp:hazard_wand`) for testing hazard prop states without `/setblock` coordinates — right-click a hazard prop to call `HazardManager.activate`/`defuse` (toggle normal↔hazardous) or `setSawdustLevel` (step accumulation 0→5); shift+right-click calls `HazardManager.forceFailure` to trigger the failure consequence immediately. Works with or without an active simulation session (`SimulationManager.getSession` may be null; `HazardManager`'s mutation entry points are all session-nullable). |
| `ComputerBlock` | Custom block in `block/` package; registry ID `berongsmp:computer` (field `BerongSMP.COMPUTER`). Has `FACING` (horizontal), `LIT`, `BURNING`, and `BROKEN` states. State machine: OFF↔ON (right-click), ANY→BURNING (flint & steel — immediately places fire on all adjacent air), BURNING→BROKEN (CO2 extinguisher). `BURNING=true`: scans a 2-block radius every `randomTick` and seeds vanilla fire next to real flammable furniture (**2026-07-17 fix:** checks `BlockTags.PLANKS`/`LOGS`/`WOOL` now — it used to check `BlockState.ignitedByLava()`, a vanilla method that's `true` almost exclusively for TNT-like blocks, not ordinary wood/wool; verified via direct schematic inspection that real `dark_oak_planks`/`stripped_oak_log` furniture near CCS computers was silently never catching fire through this path); `animateTick` emits FLAME from top + all 4 sides, SOUL_FIRE_FLAME (cyan electrical signature), wild ELECTRIC_SPARK arcs, LARGE_SMOKE columns, LAVA ember drips; light level 15. `BROKEN=true`: cracked screen + scorched case texture, all interactions blocked, emits occasional SMOKE wisps. Only CO2 extinguisher ends the fire (and causes BROKEN). Registered via `BLOCKS.registerBlock(name, Constructor::new, () -> Props)` pattern required by NeoForge 26.x. |
| `TutorialStage` | Enum of all tutorial stages: `NOT_STARTED → PASS_SPRAY → EXT_TYPE_A/B/C → QUAKE_DROP/COVER/HOLDON → COMPLETED` |
| `TutorialManager` | Static utility: station placement, interaction dispatch, extinguish counting, QUAKE tick detection, completion. `isComplete(UUID)` **no longer gates the main lobby's buttons** (2026-07-13 — the Academy's own certification does, via `LobbyManager.academyCertified`); the legacy tutorial itself is unchanged and still reachable via `/bfp old_tutorial`. `@EventBusSubscriber`-annotated for `onPlayerLogout`, which clears its transient maps and rolls a mid-earthquake-drill stage (`QUAKE_DROP`/`COVER`/`HOLDON`) back to `QUAKE_INTRO` — see Tutorial Flow above. |
| `TutorialSavedData` | Extends `SavedData`; persists `Map<UUID, TutorialStage>` to `world/data/berongsmp_tutorial.dat` |
| `TutorialStatusPayload` | Server→client packet carrying `prompt` (String) and `intensity` (float) for tutorial HUD and camera shake |
| `TutorialHud` | Client-side HUD renderer for tutorial prompts; drives camera shake during QUAKE stages; hidden when SimulationHud is active |
| `StudentSession` | POJO holding per-student data: name, account UUID, start/end times, tutorial timing, simulation type/score/passed, Turso row ID |
| `TursoClient` | HTTP wrapper for the Turso libSQL REST API (`/v2/pipeline`); fire-and-forget async writes via `CompletableFuture` on a **dedicated 2-thread daemon executor** (kept off `ForkJoinPool.commonPool()`); creates schema on first init. Schema (2026-07-13) gained a `student_accounts` table (`username` UNIQUE, `password_hash`, student identity fields, `tutorial_completed`, `created_at`/`last_login`) backing `/register`+`/login`, and a `sessions.username` column/index so run history can be queried by account. **Named `student_accounts`, not `users`** (2026-07-13 hotfix): this Turso database is shared with the `BERONG_SMP_WEB` dashboard, which already owns a table literally named `users` for its own admin/staff logins (different schema entirely — no student_id/section/full_name/tutorial_completed). `CREATE TABLE IF NOT EXISTS users` was a silent no-op against that pre-existing table, so `/register` always failed with "db_error" (INSERT referenced nonexistent columns) and any student whose chosen username happened to match a real dashboard admin login got a spurious "already taken". Always check `SELECT sql FROM sqlite_master WHERE name=...` against the live DB before picking a bare, generic table name in this shared database. Schema (2026-07-14) also gained `sessions.fire_log_csv TEXT` — the same once-per-session-blob idiom as `move_log_csv`, holding every fire block ignite/extinguish event so the dashboard can animate fire spread (see `TelemetryCsvWriter`/`SimulationSession`). **2026-07-17 (code review remediation):** `queryAsync`/`insertAsync` added — reads are no longer synchronous-only; every server/command-thread caller (`SessionManager.checkin`, `/history`, every `/bfp` read) now uses these instead of blocking for up to the 10s HTTP timeout. `shutdown()` now awaits the write queue draining (5s) before tearing down `httpClient`, instead of nulling it immediately and losing whatever was still queued (e.g. `SessionManager.shutdown()`'s abort-marker writes); both `executeAsync`/`silentAlter` guard their submission against `RejectedExecutionException`. **2026-07-17 (multiplayer/concurrency audit):** `writeExecutor` bumped 2→8 threads — now that reads share it too, 2 was a real bottleneck risk for a classroom-scale `/register` burst; safe to over-provision since every task is I/O-bound (blocking HTTP), not CPU-bound. |
| `SessionManager` | Manages `Map<UUID, StudentSession>` for shared station accounts; hooks into tutorial completion and simulation end to persist scores; exposes `/bfp` admin flow. `checkin` gained a `username` overload (threads a `/login`/`/register` account into the `sessions` row) and now also resets `AcademySavedData` for the incoming station UUID (2026-07-13) — otherwise a new student at a shared station inherits whatever Academy certification the previous student left behind, since `AcademyProgress` is keyed by station UUID, not student identity. **2026-07-17:** `checkin`'s Turso INSERT is now async (`TursoClient.insertAsync`, previously blocking even when called from an already-async `/register`/`/login` success callback) and also clears any leftover BFP admin grant/test-bypass for the incoming station UUID (`BfpAdminCommands.clearStationAuth`). `onSimulationEnd` no longer recomputes a fire run's score/passed itself (that duplicated, drifted-from `SimulationManager`'s own formula — see `SimulationScoring`) — it now takes the already-computed `finalScore`/`passed` as parameters. Also gained a `PlayerLifecycleRegistry` logout hook (it previously had none at all) closing out the in-memory `StudentSession`/Turso row via the existing `checkout(uuid, "disconnected")` path — a station that disconnected without an explicit `/bfp checkout` used to leave its row stuck at `status='active'` indefinitely. |
| `AuthManager` | Per-station "who is logged in right now" transient state (`Map<UUID, AuthAccount>`, same idiom as `DropAndRollManager`'s per-UUID maps) backing `/register`/`/login`. `registerAsync`/`loginAsync` run the PBKDF2 hash + Turso round-trip off the server thread on a small dedicated executor, re-entering via `server.execute(...)` before touching game state; `loginAsync` rate-limits attempts (5/5min lockout, mirroring `/bfp login`'s PIN lockout). `isTutorialCompleted(uuid)` is what lets a restored `/login` account satisfy `LobbyManager`'s Academy-certification gate without replaying the tutorial; `markTutorialCompleted` (called from `MorfeRoomManager` on PASS) persists that flag. Wired to `PlayerLifecycleRegistry`'s logout hook so login state doesn't leak to the next student at a station. `bootstrap()` forces the static block to class-load (`DuckCoverHoldManager` precedent), called from `BerongSMP.commonSetup`. **2026-07-17:** the rate-limit counter (renamed `LoginAttempts`, a small synchronized record) now increments at dispatch time rather than only on a confirmed failure — closes a TOCTOU race where a burst of concurrent `/login` attempts could all pass the lockout check before any one's async result returned. `"not_found"`/`"bad_password"` collapsed into one `"invalid_credentials"` error (distinguishing them let a shared station enumerate registered usernames); added a 128-char password cap checked before PBKDF2/Turso are touched. `AUTH_EXECUTOR` bumped 2→4 threads for classroom-scale registration bursts — more conservatively than `TursoClient.writeExecutor`'s bump, since PBKDF2 hashing is genuinely CPU-bound and too much parallelism here would compete with the server tick thread for cores. |
| `PasswordHasher` | Salted `PBKDF2WithHmacSHA256` password hashing (`javax.crypto`, no new Gradle dependency) for the account system — 210,000 iterations, 16-byte random salt, constant-time verification (`MessageDigest.isEqual`). Encoded form `pbkdf2_sha256$<iterations>$<salt>$<hash>` carries its own iteration count so a future bump doesn't invalidate existing rows. |
| `FireAlarmBlock` | Wall-mounted block in `block/` package; states `FACING` + `ACTIVATED`. Right-click during an active FIRE simulation sets `ACTIVATED=true`, plays bell sound, logs `fire_alarm_activate` telemetry event. Auto-resets when simulation structure is restored. |
| `HorizontalFacingBlock` | Abstract base for all FACING-only furniture blocks: centralises the FACING property, default-state registration, `getStateForPlacement`, `rotate`/`mirror`, and the `getShape` switch. Subclasses implement only `shapeFor(Direction)` (helper `byFacing(...)`). |
| `FlammableFacingBlock` | `HorizontalFacingBlock` subclass adding the standard wood/paper flammability (flammability 20, spread 5). Parent of Chair/Drawers/ComputerTable/BulletinBoard. |
| `WhiteboardBlock` | Flat wall-mounted classroom whiteboard; FACING only (extends `HorizontalFacingBlock`). Model: glossy `whiteboard_surface` board (faint marker-streak sheen) + brushed-aluminum `whiteboard_frame_metal` frame/tray (custom textures, see Furniture Visual Remediation Log). Tile side-by-side for a wide whiteboard (already seamless — `board_body` spans the full block width). Stack vertically to grow one tall board: `CONNECTED_UP`/`CONNECTED_DOWN` (set at placement, kept live via `updateShape`) select between the base model and `whiteboard_bottom`/`_top`/`_middle`, which extend `board_body`'s Y-range to close the gap between tiles and hide `bottom_frame`/`marker_tray` on any tile that isn't the true bottom of the stack. |
| `ToiletBlock` | Ceramic toilet; FACING only. Model: pedestal base + bowl + seat lid + tank + flush button, all in glossy `ceramic_glossy_white` (specular-highlight porcelain) + `fixture_chrome` trim (shared with `SinkBlock`). Right-click plays water/flush sound. |
| `SinkBlock` | Wall-mounted sink; FACING only. Model: back plate + ceramic basin + faucet body/neck + left/right handles, reusing `ceramic_glossy_white`/`fixture_chrome` from `ToiletBlock`. Right-click plays water-ambient sound. |
| `DrawersBlock` | Flat-panel modern dresser; FACING only. Model: `cabinet_body_painted`/`drawer_front_painted` (matte off-white MDF panels) + `handle_bar_metal` (brushed-silver pull bar) — no wood grain. Flammable (`getFlammability=20, spreadSpeed=5`). |
| `ComputerTableBlock` | Modern office desk; FACING only. Model: `desk_laminate_white` tabletop + `desk_leg_metal_black` legs/crossbar (matte black brushed metal) + `desk_cable_panel_dark` modesty panel (grommet + vent slats) — replaced the original raw oak_planks/oak_log look. Flammable — burns during fire simulations. |
| `TableBlock` | Extendable study/library table — single block, no FACING (symmetric). Model: `table_top.json` (`table_top_oak` tabletop, Y12-15, always shown) + up to 4× `table_leg.json` (corner posts, Y0-12) + up to 4× `table_stretcher.json` (support beams) + up to 4× `table_apron.json` (skirt trim), all `table_leg_wood_dark`. `NORTH`/`SOUTH`/`EAST`/`WEST` `BooleanProperty`s (vanilla fence/glass-pane idiom) track same-type neighbours. A corner leg only renders/collides when *both* sides meeting there are unconnected (e.g. the NW leg needs `north=false AND west=false`) — precomputed into a 16-entry `VoxelShape[]` lookup in `getShape` — so pushing two tables together drops the shared interior legs/stretchers/apron entirely instead of just hiding a seam trim, reading as one longer table rather than two placed side by side (same visual goal as `WhiteboardBlock`'s `CONNECTED_UP`/`CONNECTED_DOWN`, just on the horizontal axes and per-corner instead of per-face). Too short to ever satisfy `DuckCoverHoldManager`'s above-the-feet cover check on its own cell — see `DuckCoverHoldManager.hasNearbyTable`/`allowCrawlUnderTable`. Not flammable-flagged — plain `Block.Properties`. |
| `ChairBlock` | Modern task chair; FACING only. Model: `chair_frame_black` (matte black frame/legs) + `chair_mesh_fabric` (backrest) + `chair_cushion_fabric` (seat, speckled weave) — replaced the plain wooden-stool look. Flammable. |
| `FilingCabinetBlock` | Tall graphite-metal filing cabinet; FACING only. Model: `cabinet_body_graphite`/`drawer_face_graphite` (dark brushed metal, not raw iron_block) + `handle_bar_black` + `label_holder_white`. Strength 2.0/6.0. |
| `LockerBlock` | Tall painted-steel school locker; FACING only. Model: `locker_body_painted`/`locker_door_painted` (navy matte steel) + `locker_seam_dark` + `vent_slats_dark` + `keypad_lock` (LED keypad, replacing the gold-block padlock) + reused `handle_bar_black`/`label_holder_white`. |
| `TrashCanBlock` | Matte pedal-bin style trash can; no FACING (symmetric). Model: `trash_body_charcoal` + `trash_rim_metal` (brushed rim), reuses `handle_bar_black` for the dark interior. |
| `BulletinBoardBlock` | Felt pinboard with modern sticky notes; FACING only. Model: `board_felt_charcoal` (not note_block cork) + `note_paper_yellow`/`note_paper_blue` + `pin_metal_red`/`pin_metal_teal`, reuses `whiteboard_frame_metal` for the frame. Flammable. |
| `CeilingFanBlock` | Matte modern ceiling fan; no FACING (symmetric). Model: `fan_housing_white` + `fan_blade_matte` (flat matte blades, no wood grain) + reused `handle_bar_black` (mounting rod) + `light_bulb`'s `led_diffuser_glow` for the light bowl (light level 5) — shares its cool-white glow with `LightBulbBlock` for a consistent fixture look. |
| `LightBulbBlock` | Full-cube glowing ceiling tile (plain `extends Block`, default full-cube shape, no custom `getShape`/FACING). Single seamless near-pure-white texture (`led_diffuser_glow.png`, generated by `scripts/generate_light_bulb_textures.py` — deliberately flat with no border/bevel/grid so tiles read as one continuous glowing surface with zero visible seams when placed edge-to-edge, e.g. as ceiling material). `lightLevel=15`, which **is vanilla's hard cap** (block light is a 4-bit value, 0–15 — no block can go brighter than this; "20x brighter" isn't achievable without rewriting the core lighting engine). Vanilla light still decays 1 level per block from any source — to evenly flood a large room, tile several across the ceiling rather than relying on one. |
| `GlowingOakPlanksBlock` | Disguised light source: full-cube block that reuses the vanilla `minecraft:block/oak_planks` texture directly (via `cube_all` parent model) and matches vanilla oak planks' `strength`/`sound`/flammability profile (20/5, same constants as `FlammableFacingBlock`), but registered with `lightLevel=15` — indistinguishable from a real oak planks block by sight, sound, or burn behavior; only its registry name (`berongsmp:glowing_oak_planks`) and lit-block-light behavior give it away. Lang key deliberately reads "Oak Planks" (matches vanilla) for the same reason. Intended for hiding light sources in ceilings/walls without a visible fixture. |
| `CourtLineBlock` | Smooth white court-marking line — the vanilla redstone-dust auto-connecting-wire idiom (small hub "dot" when isolated, thin arms extending toward every neighbouring `CourtLineBlock`), just plain white with no power mechanic. Reuses `TableBlock`'s `NORTH`/`SOUTH`/`EAST`/`WEST` self-connection properties/placement/`updateShape` pattern and its `SHAPES_BY_CONNECTIONS`-style precomputed-per-mask `VoxelShape[]`, but additive (dot + arm) instead of subtractive (leg removal). Rendered via a `multipart` blockstate (`court_line_dot` always-on + `court_line_side` per connected direction, same technique as `table.json`) — thin (Y 0–1), so it draws real badminton court lines at proper narrow proportions instead of a full-width painted tile. |
| `BadmintonNetPostBlock` | Badminton net anchor post (the "stitch"). `onPlace` scans north/south/east/west/up/down (in that priority order) up to 24 blocks for another `BadmintonNetPostBlock` with a clear air/mesh path, then fills every block between with `BadmintonNetMeshBlock` — one continuous net regardless of the gap, oriented automatically via `BadmintonNetMeshBlock.AXIS`. `playerWillDestroy` walks outward from the broken post in all 6 directions clearing any connected mesh run back to the first non-mesh block. Not FACING — a plain centered pole shape. |
| `BadmintonNetMeshBlock` | Auto-filled net panel between two posts; not normally hand-placed. `AXIS` (`BlockStateProperties.AXIS`, the same 3-way enum vanilla logs use) is the panel's *thin*/facing axis, not the travel axis: a post run along X (east–west) needs a wall containing X+Y so `BadmintonNetPostBlock` sets `AXIS=Z`, a run along Z sets `AXIS=X`, and a vertical (Y) run sets `AXIS=Y` for a thin horizontal mesh layer instead of a wall. Blockstate needs only 2 authored models — `badminton_net_mesh` (thin-Z wall) reused at `y:90` for the thin-X case, plus a separate `badminton_net_mesh_horizontal` for the Y case — since rotating a Y-axis-authored model by 90° correctly swaps X/Z. `badminton_net_mesh.png` is an RGBA diamond-lattice texture (transparent gaps, solid top tape band) generated without the shared rim-light signature pass (would tint the mostly-transparent canvas); the thin edge faces use the separate opaque `badminton_net_mesh_edge` texture. |
| `BasketballHoopPostBlock` | Basketball hoop stand base — the bottom anchor of the expandable pole. `onPlace` scans straight up to 24 blocks for a `BasketballHoopBlock` with a clear air/pole path, fills the gap with `BasketballPoleSegmentBlock`s — the stand "expands" to whatever height the hoop is placed at instead of a fixed prefab height. `playerWillDestroy` walks up clearing the connected pole run. Symmetric, no facing. |
| `BasketballPoleSegmentBlock` | Auto-filled pole segment between a post and a hoop; not normally hand-placed (same idiom as `BadmintonNetMeshBlock`). Symmetric, no facing. |
| `BasketballHoopBlock` | Backboard + rim + net (top anchor of the expandable pole), FACING-only. `onPlace` scans straight down to 24 blocks for a `BasketballHoopPostBlock` and fills the gap the same way as the post does looking up; `playerWillDestroy` walks down clearing the run. Unlike `BadmintonNetPostBlock`'s symmetric same-type endpoints, this pair is asymmetric-by-role (post only ever scans up, hoop only ever scans down) since a hoop is always mounted above its pole — simpler than the badminton net's 6-direction scan. Net mesh element reuses the existing `badminton_net_mesh`/`badminton_net_mesh_edge` textures. |
| `HazardBlock` | Abstract base for hazard blocks without FACING (symmetric). Real 3-state lifecycle: `HAZARDOUS` (developing danger) + `ON_FIRE` (actually burning, terminal) `BooleanProperty`s; subclasses implement `spawnHazardParticles`. `animateTick` calls it when `HAZARDOUS=true`, intensified (extra calls + FLAME particles) when `ON_FIRE=true` too. `useWithoutItem` is a base-class bare-hand "prevention" interaction: right-clicking a merely-hazardous (not yet on fire) prop resets it to safe (`preventMessage()`, overridable flavor text) — does nothing once `ON_FIRE=true` (too late for a bare-handed fix). Also declares the failure-consequence hooks (`failureDelayTicks`, `failureMessage`, `onHazardFailure`, plus `igniteAdjacent`/`igniteRadius` helpers) driven by `HazardManager`. `igniteNearestFlammable(level, origin, maxTargets, searchRadius)` (2026-07-14) finds the nearest `BlockTags.PLANKS` blocks in range and lights an adjacent air cell next to each instead of a lone fire block with nothing to catch — used by `NewSim2FireTicker.tick`'s PREVENTION→INTERVENTION edge only (not the shared `onHazardFailure` default, so it doesn't change Library/CCS/Academy hazard-failure behavior). `igniteAdjacent`/`igniteRadius`/`igniteNearestFlammable` all now return `List<BlockPos>` of what they actually lit (2026-07-14, was `void`) — callers with a live `SimulationSession` use this to log each fire block into `SimulationSession.bufferFireLogRow`/`TelemetryCsvWriter.writeFireLogRow`; callers that don't care just ignore the return value. |
| `HazardFacingBlock` | Abstract base for hazard blocks with FACING. Combines `HazardBlock`'s `HAZARDOUS`/`ON_FIRE` properties with `HorizontalFacingBlock`'s FACING logic; same intensified-particle `animateTick` and bare-hand prevention `useWithoutItem` as `HazardBlock`. Subclasses implement `shapeFor(Direction)` (use `byFacing(...)`) and `spawnHazardParticles`. Carries the same failure-consequence hooks as `HazardBlock`. **2026-07-17:** `igniteAdjacent`/`igniteRadius` now return `List<BlockPos>` (were `void`), matching `HazardBlock`'s sibling methods — a consistency fix with zero live functional impact (every current `onHazardFailure` override discards the value anyway). |
| `HazardManager` | Drives the normal→hazardous→on-fire state machine for all 85 hazard prop blocks (plus the sawdust layer and `ComputerBlock`) inside an active FIRE-type arena, per `docs/hazard_props_spec.md`. `scanHazardProps` runs once at session start (cached on `SimulationSession.hazardPositions`, mirroring `findComputersInCCS`); `tick()` (called from `LegacyFireTicker.tick` every tick) randomly develops props to `HAZARDOUS=true` every 100 ticks (1-in-30 chance per prop) and advances a per-prop failure timer (`SimulationSession.hazardTimers`). When `failureDelayTicks()` elapses without being defused, `triggerFailure` now first flips the prop's `ON_FIRE=true` (a real terminal block state, not just an invisible transition) before calling the block's `onHazardFailure` — see Hazard Prop 3-State Log below. `woodshop_sawdust_layer`'s `ACCUMULATION` 0→5 ramp and 3×3 flash-ignite live directly in `HazardManager` (it has no `HAZARDOUS`/`ON_FIRE` property). `ComputerBlock` is similarly special-cased (`BURNING` instead of `HAZARDOUS`): `seedComputerTimers` lazily starts a 240-tick failure timer the first tick any of its 3 existing ignition triggers (flint & steel, session-start, periodic CCS spread) is observed to have set `BURNING=true`, so a computer left burning unattended escalates (`HazardBlock.igniteAdjacent`) like every other prop without duplicating its ignition logic. `HazardManager.defuse()` lets either extinguisher item reset a hazardous (or already on-fire) prop back to fully safe (`HAZARDOUS=false, ON_FIRE=false`) — wired into `FireExtinguisherItem`/`CO2ExtinguisherItem.extinguishAt`. `forceFailure` (used by `ReyesRoomManager.igniteHazard` and `HazardWandItem`) now sets `HAZARDOUS=true` first if a prop is still in its default state before failing it (fixed 2026-07-05 — it used to skip straight to `triggerFailure`, which only ever sets `ON_FIRE`, so `isHazardous()`/`defuse()` never recognized a Reyes-ignited prop as hazardous and an extinguisher spray against it silently did nothing). For `NEW_SIM_BUILDING2_FIRE`, `tick()` early-returns before any of the above — that scenario's 5 armed hazards are driven entirely by `NewSim2FireTicker.tick`'s explicit phase machine, not organic escalation. `armRandomHazards` picks and `activate()`s N random hazard-capable props from a scanned pool (used to arm those 5). `onManualPrevention` (called from `HazardBlock`/`HazardFacingBlock.useWithoutItem`) and the extended `defuse()` both emit `hazard_neutralize` telemetry for any active fire-type session and track New Sim Building 2.0's found/resolved hazard tallies. `triggerFailure`/`defuse` also add/remove the prop's position from `SimulationSession.getActiveFireSources()` (2026-07-14) — the set `FireEffects.simulateFire` scatters new fire near, instead of an arbitrary arena position. |
| `HazardSpec` / `HazardSpecs` / `SimpleHazardBlock` / `SimpleHazardFacingBlock` | Data-driven alternative to a dedicated `Block` subclass per hazard prop, added 2026-07-14 as a proof-of-concept dedup of the 85-prop skeleton (shape, particle spawn, failure delay, prevent/failure messages, failure action). `HazardSpec` is a record holding all five as data (`shapeFor: Function<Direction,VoxelShape>` via `HazardSpec.fixedShape`/`shapeNsEw`/`shape4Way`, a `ParticleEmitter` lambda, `failureDelayTicks`, both message strings, a `FailureAction` lambda); `SimpleHazardBlock`/`SimpleHazardFacingBlock` extend `HazardBlock`/`HazardFacingBlock` and delegate all five hooks to a `HazardSpec` instance passed into their constructor instead of overriding them. `HazardSpecs` (public, not package-private, since the `ModBlocks*` registrars that reference it live in a different package) is the table of migrated specs — currently 8 props (`CORRODED_GAS_LINE_JOINT`, `EBIKE_CHARGING_STATION`, `DRY_AQUARIUM_HEATER`, `RODENT_CHEWED_WIRING`, `DUSTY_CRT_MONITOR`, `SHORTED_BENCH_SUPPLY`, `OVERHEATED_VACUUM_PUMP`, `FAULTY_DEHUMIDIFIER`), each verified field-by-field against the deleted original class before that file was removed. The remaining ~77 hazard props keep dedicated classes — migrating them in batches using this same pattern is documented backlog, not attempted further this session. Registration still uses the mandatory NeoForge shape (`BLOCKS.registerBlock(name, p -> new SimpleHazardFacingBlock(p, HazardSpecs.X), () -> props)`), so the registry-key-before-`effectiveDrops()` constraint is unaffected. |
| `WoodshopSawdustLayerBlock` | Floor sawdust accumulation layer; `ACCUMULATION` 0–5 integer state drives height (1–6 px). Emits ASH particles at accumulation ≥ 3. No FACING. |
| `PlasticTrashBinBlock` | Classroom trash bin with vape inside; SMOKE particles when hazardous. |
| `DaisyChainExtensionBlock` | Overloaded extension cord; ELECTRIC_SPARK particles when hazardous. |
| `StageSpotlightBlock` | Theatre spotlight overheating; FLAME + LARGE_SMOKE; light level 10 when hazardous. |
| `ArchiveBoxStackBlock` | Stack of flammable document boxes; CAMPFIRE_COSY_SMOKE when hazardous. |
| `DustChokedPcBlock` | PC tower with dust-blocked vents; SMOKE; light level 3 when hazardous. |
| `ChargingCartBlock` | Rolling Chromebook charging cart; ELECTRIC_SPARK; light level 5 when hazardous. |
| `FrayedConsoleWireBlock` | Floor-level AV wire with bare copper; ELECTRIC_SPARK + SOUL_FIRE_FLAME. No FACING. |
| `MalfunctioningVendingBlock` | Vending machine with shorted compressor; SMOKE. |
| `CeilingProjectorBlock` | Ceiling projector with failed cooling fan; LARGE_SMOKE + LAVA; light level 7 when hazardous. |
| `SwollenPhoneBatteryBlock` | Thermally-swollen phone; SOUL_FIRE_FLAME gas. No FACING. |
| `DamagedLipoPackBlock` | Punctured drone LiPo pack; CAMPFIRE_COSY_SMOKE. No FACING. |
| `VapeInIronLockerBlock` | Locker with vaping device inside; ELECTRIC_SPARK + SMOKE leaking from vent. |
| `PaSystemBackupBlock` | Wall-mounted PA amplifier rack; LAVA + ELECTRIC_SPARK; light level 8 when hazardous. Thin wall-panel shape. |
| `SmartboardInverterBlock` | Smartboard panel with roof-leak water damage; DRIPPING_WATER. Thin wall-panel shape. |
| `UnattendedGreasePanBlock` | Stove with frying pan left unattended; FLAME + LARGE_SMOKE; light level 10 when hazardous. |
| `GreaseCloggedHoodBlock` | Ceiling range hood with clogged filters; LARGE_SMOKE. Raised shape (Y:8–15). |
| `ContaminatedKitchenBinBlock` | Kitchen bin with oil-soaked rags; CAMPFIRE_COSY_SMOKE steam wisps. |
| `JammedPaniniPressBlock` | Countertop panini press with burning food; LARGE_SMOKE + SMOKE. Low profile (Y:0–6). |
| `CommercialDeepFryerBlock` | Commercial deep fryer; FLAME + LARGE_SMOKE + LAVA; light level 12 when hazardous. |
| `OverloadedMicrowaveBlock` | Faculty-pantry microwave on a runaway heating cycle; ELECTRIC_SPARK + SMOKE. 400-tick failure → Class C electrical fire. |
| `BunsenBurnerStationBlock` | Science-lab bunsen burner left running unattended; FLAME + SOUL_FIRE_FLAME open flame. 300-tick failure. No FACING. |
| `ReagentStorageShelfBlock` | Science-lab shelf with a tipped, leaking reagent bottle; CAMPFIRE_COSY_SMOKE fumes + DRIPPING_WATER. 500-tick failure — widest school-zone failure radius (2 blocks). |
| `OverloadedBreakerPanelBlock` | Utility-corridor electrical panel drawing far more load than rated; dense ELECTRIC_SPARK. 350-tick failure → Class C fire. |
| `OverheatingWallAirconBlock` | Window-type wall aircon leaking condensation into its own wiring; DRIPPING_WATER + ELECTRIC_SPARK. 450-tick failure. |
| `JammedLaserPrinterBlock` | Faculty-room laser printer with paper jammed against the fuser; SMOKE + LARGE_SMOKE. 400-tick failure. |
| `UnattendedShrineCandleBlock` | Lobby shrine candle burning beside an altar cloth; FLAME + SMOKE, light level 10. 250-tick failure — fastest-failing hazard in the set. No FACING. |
| `LeakingGasValveBlock` | Cafeteria LPG line with a leaking valve — **Class F/K kitchen hazard**, in `AbstractExtinguisherItem.KITCHEN_HAZARD_IDS`; only the wet chemical extinguisher defuses it. CAMPFIRE_COSY_SMOKE gas haze. 300-tick failure → 2-block flash fire. |
| `AlcoholDispenserStationBlock` | Hallway sanitizer stand leaking onto the floor beside a power outlet; SOUL_FIRE_FLAME vapor wisps. 500-tick failure → blue flash fire. |
| `CloggedExhaustFanBlock` | Kitchen/workshop exhaust fan caked in dust; SMOKE + ASH from the stalled, smouldering motor. 450-tick failure. |
| `OverloadedWallOutletBlock` | Classroom/office wall outlet with too many devices drawing current; ELECTRIC_SPARK + SMOKE. 350-tick failure → Class C fire. |
| `JammedCircuitBreakerBlock` | Utility-corridor breaker box whose trip lever has been physically jammed (coin/matchstick) so it can't cut power on overload — distinct from `OverloadedBreakerPanelBlock`, which is simply overloaded; here the safety device itself is sabotaged. Dense ELECTRIC_SPARK + occasional LAVA ember. 400-tick failure. |
| `UnsealedSolventShelfBlock` | Art-room/shop-class shelf of open paint-thinner/lacquer cans (Class B flammable liquid); CAMPFIRE_COSY_SMOKE fumes. No FACING. 500-tick failure → 2-block flash fire, widest radius alongside the reagent shelf. |
| `UnattendedWeldingStationBlock` | Vocational-shop arc welder left running; spark-shower ELECTRIC_SPARK + occasional FLAME. 300-tick failure → igniteAdjacent(2), the widest adjacency radius of any hazard prop. |
| `LeakingButaneCanisterStoveBlock` | Portable single-burner butane ("gasul") camp stove leaking at the canister seam — **Class F/K kitchen hazard** distinct from `LeakingGasValveBlock`'s fixed wall LPG line; only the wet chemical extinguisher defuses it. CAMPFIRE_COSY_SMOKE gas haze + FLAME. 300-tick failure → 2-block flash fire. |
| `ChefsPrepDrawersBlock` | Culinary-classroom prep table with an open drawer and an oily rag draped beside a nearby burner; SMOKE + CAMPFIRE_COSY_SMOKE. 400-tick failure. |
| `CulinaryFridgeBlock` | Tall reach-in fridge; dust-caked condenser coils + a failing start-relay overheat the compressor. First refrigerator in the mod — deliberately an electrical angle, not a coolant-gas leak. SMOKE + ELECTRIC_SPARK; light 3 when hazardous. 450-tick failure. |
| `StudentLabMicrowaveBlock` | Shared student-bench countertop microwave; metal/foil left inside arcs violently — distinct from `OverloadedMicrowaveBlock`'s runaway-heating-cycle failure (that one glows hot, this one is cold-but-arcing). ELECTRIC_SPARK + SMOKE. 350-tick failure. |
| `CorrodedGasLineJointBlock` | Fixed gas-line elbow joint rusted through with corrosion pinholes weeping gas — **Class F/K**, distinct from `LeakingGasValveBlock`'s opened handwheel valve. Wall-mounted, CAMPFIRE_COSY_SMOKE haze. 300-tick failure → 2-block flash fire. |
| `GasRangeStuckBurnerBlock` | Full 4-burner gas range whose control knob has melted and jammed a burner open — **Class F/K**, distinct from `UnattendedGreasePanBlock` (no pan involved, the range's own control has failed). FLAME + SOUL_FIRE_FLAME; light 11 when hazardous. 300-tick failure. |
| `CommercialStandMixerBlock` | Countertop planetary stand mixer whose motor jams under stiff dough and overheats; SMOKE. 400-tick failure. |
| `GasDeckOvenBlock` | Gas baking deck oven with a stuck thermostat and cracked door radiating heat onto nearby combustibles — **Class F/K**. FLAME + LARGE_SMOKE; light 10 when hazardous. 350-tick failure. |
| `InductionCooktopStationBlock` | Glass-top induction hob whose cooling fan fails, overheating the electronics under a dry pan; SMOKE + ELECTRIC_SPARK; light 4 when hazardous. Low-profile counter-mounted shape. 400-tick failure. |
| `RiceCookerBankBlock` | Row of student rice cookers boiled dry, scorching the exposed element; symmetric, custom half-height collision shape. SMOKE; light 3 when hazardous. 350-tick failure. |
| `EspressoMachineBlock` | Café-style espresso machine whose scale-blocked pressure-relief valve over-pressurizes the boiler; DRIPPING_WATER (steam) + ELECTRIC_SPARK. 450-tick failure. |
| `HotWaterUrnBlock` | Tall tea/coffee boiler urn whose spigot drips water onto the live base electrics; symmetric cylinder. DRIPPING_WATER + ELECTRIC_SPARK. 400-tick failure. |
| `ToasterOvenCrumbBlock` | Countertop toaster oven whose crumb tray, packed with old crumbs and grease, ignites under the glowing element; FLAME + SMOKE; light 9 when hazardous. 350-tick failure. |
| `DryGoodsPantryShelfBlock` | Pantry shelf of open flour/sugar sacks beside a hot light-fixture ballast — an airborne flour-dust deflagration hazard, distinct from `ReagentStorageShelfBlock` (chemicals) and `ArchiveBoxStackBlock` (paper). CAMPFIRE_COSY_SMOKE; light 4 when hazardous. 500-tick failure → 2-block flash, the widest culinary radius alongside the reagent shelf. |
| `GreaseDuctRunBlock` | Horizontal ceiling exhaust duct above the cooking line caked with flammable grease — **Class F/K**, distinct from `GreaseCloggedHoodBlock` (the hood canopy directly over a stove, not the overhead ductwork). Ceiling-mounted, LARGE_SMOKE. 450-tick failure. |
| `CommercialDishSanitizerBlock` | Under-counter high-temp dish sanitizer whose booster element boils dry while door-seal steam seeps into the control board; DRIPPING_WATER + ELECTRIC_SPARK. 450-tick failure. |
| `GarbageDisposalUnitBlock` | Under-sink food-waste disposal whose grinder jams on a dropped utensil and burns out its motor; symmetric squat cylinder, sits under a `SinkBlock`. SMOKE + ELECTRIC_SPARK. 350-tick failure. |
| `KnifeSterilizerCabinetBlock` | Wall-mounted UV knife/utensil sterilizing cabinet whose UV-lamp ballast overheats and scorches the housing; SOUL_FIRE_FLAME (UV-glow stand-in) + SMOKE; light 6 when hazardous. 400-tick failure. |
| `SternoSteamTableBlock` | Buffet steam table heated by canned gel-fuel (Sterno) chafing burners left lit under a boiled-dry pan — Class B open flame, any extinguisher. SOUL_FIRE_FLAME (blue gel flame) + SMOKE; light 8 when hazardous. 300-tick failure. |
| `ConvectionOvenBlock` | Electric convection/combi oven whose heating element and fan motor short behind a failed door gasket — Class C, deliberately paired with `GasDeckOvenBlock`'s matching full-height silhouette to teach the gas-vs-electric distinction. SMOKE + ELECTRIC_SPARK; light 7 when hazardous. 400-tick failure. |
| `LechonRotisserieSpitBlock` | Charcoal rotisserie spit with live coals unattended; rendered fat dripping onto the coals flares up — **Class F/K**, the only solid-fuel hazard in the mod. FLAME + LAVA; light 12 when hazardous (brightest hazard prop). 300-tick failure. |
| `PortableSpaceHeaterBlock` … `FaultyDehumidifierBlock` (props 56–85) | The 30-block Conference Room/Office/Laboratory hazard batch, added 2026-07-12. All follow the standard `HazardBlock`/`HazardFacingBlock` 3-state lifecycle; see `docs/hazard_props_spec.md`'s "Conference Room Zone", "Office Zone", and "Research & Instrumentation Laboratory Zone" sections for the full per-prop table (normal/hazardous states, failure delay, consequence). 28 of the 30 are energized-electrical/flammable-vapor hazards on `AbstractExtinguisherItem.WET_CHEMICAL_UNSAFE_IDS` (ABC/CO2 only); `smoldering_planter` is the one plain Class A prop (any extinguisher); `leaking_oxygen_cylinder` is the mod's only oxidizer hazard (CO2-only, on `OXIDIZER_HAZARD_IDS`). |
| `TeachersDeskBlock` | Teacher's desk with a drawer pedestal (extends `FlammableFacingBlock`) — the classroom's front-of-room anchor. |
| `ArmchairDeskBlock` | Classic Philippine classroom armchair-desk: plywood seat + writing-tablet arm (extends `FlammableFacingBlock`). |
| `TallBookshelfBlock` | Full-height library bookshelf (extends `FlammableFacingBlock`) — a library fire's favourite fuel. |
| `PhilippineFlagStandBlock` | Indoor Philippine flag on a brass pole stand; lobby/stage decor, no flammability. |
| `TrophyCabinetBlock` | Glass-front trophy display cabinet; glows softly (light 3). |
| `WaterDispenserBlock` | Bottled-water dispenser; right-click plays a drink sound (no other effect). |
| `WallClockBlock` | Wall clock permanently reading ten past ten; classroom/corridor decor. |
| `BlackboardBlock` | Green chalkboard with tray, `WhiteboardBlock`'s traditional sibling (extends `FlammableFacingBlock`, wood frame). |
| `PodiumLecternBlock` | Wooden speaker's podium with the school seal (extends `FlammableFacingBlock`); stage/classroom front. |
| `ClassroomGlobeBlock` | Desk globe on a dark stand; geography-corner decor, no FACING model split needed beyond orientation. |
| `ModernStudentDeskBlock` | Individual laminate-top student desk on slim black metal legs with a small under-desk book shelf, no built-in seat (pairs with the existing modern `ChairBlock`); FACING, flammable. Distinct from `ArmchairDeskBlock` (built-in seat + writing arm, classic PH wood style). |
| `ScienceLabWorkbenchBlock` | Chemical-resistant lab bench (graphite epoxy-look top) with a corner gas-tap nub; FACING, not flammable. Surface for `BunsenBurnerStationBlock`/`ReagentStorageShelfBlock` to sit on. |
| `ComputerLabDeskRowBlock` | Long laminate desk with a raised monitor-divider back edge; FACING, flammable. Simplified from the original self-connecting-run concept to a single-unit desk (no `TableBlock`-style neighbour state). |
| `LibraryStudyCarrelBlock` | Partitioned individual study cubicle — desk surface with tall back+side privacy panels; FACING, flammable. |
| `RollingBookCartBlock` | Wheeled two-shelf book-return trolley; FACING, flammable — extra fuel-load prop for library fire drills. |
| `CafeteriaTableBlock` | Long lunch table with attached molded-plastic bench seats on both sides, laminate top, steel tube frame, two end legs + crossbar (extends `FlammableFacingBlock`). |
| `TrayStackBlock` | Stack of 3 distinct colored (green/blue/orange) glossy plastic lunch trays with compartment-groove texture, divided by light plastic rims; symmetric, no facing. |
| `ServingCounterBlock` | Brushed-steel steam-table serving counter with recessed food wells and a glass sneeze guard; FACING-only. Carries `CONNECTED_UP`/`CONNECTED_DOWN` (same idiom as `WhiteboardBlock`) — stacking a second counter directly on top swaps in `serving_counter_bottom`/`_top`/`_middle` models so the short standalone sneeze guard becomes one continuous full-height guard rising out of the counter below. |
| `CafeteriaMenuBoardBlock` | Wall-mounted chalk-surface menu board with a bold "TODAY'S MENU" header stripe, wood frame (extends `FlammableFacingBlock`). |
| `CondimentStationBlock` | Countertop condiment stand: ketchup + mustard squeeze bottles and a chrome napkin holder on a metal tray; FACING-only. |
| `CafeteriaTrashBinBlock` | Dual recycle (green) / trash (gray) bin with swing-flap lids; symmetric, no facing. |
| `SodaFountainMachineBlock` | Red-and-chrome soda dispenser with a drink-selection button panel and cup-fill nozzle; FACING-only. |
| `CafeteriaStoolBlock` | Round red-vinyl-seat stool on a chrome center pole and base; symmetric, no facing. |
| `SaladBarBlock` | Refrigerated salad bar: steel body, chilled produce wells, glass sneeze guard; FACING-only. Same `CONNECTED_UP`/`CONNECTED_DOWN` stacking idiom as `ServingCounterBlock` for a full-height guard when stacked two tall. |
| `SnackVendingMachineBlock` | Glass-front snack vending machine showing rows of packaged snacks behind the glass on metal coils, red frame, coin slot; FACING-only. `CONNECTED_UP`/`CONNECTED_DOWN` stacking (same idiom as `ServingCounterBlock`): a standalone unit is a small countertop machine, but stacking two turns it into one full-height vending machine topped with a lit `vending_marquee_header` cap (`ModBlocks.SNACK_VENDING_MACHINE`'s `lightLevel` reads `CONNECTED_DOWN` so only the top piece glows). |
| `KitchenPrepCounterBlock` | Back-of-house stainless prep counter; FACING, not flammable. Distinct from `ServingCounterBlock` (front-line steam table + sneeze guard). |
| `DishwashingSinkStationBlock` | Commercial triple-basin wash sink with a gooseneck sprayer; FACING, not flammable. Distinct from the small wall-mounted `SinkBlock`. |
| `BeverageJuiceDispenserBlock` | Twin-tank gravity iced-tea/juice dispenser; FACING, not flammable. Distinct from the carbonated `SodaFountainMachineBlock` and the bottled `WaterDispenserBlock`. |
| `CutleryNapkinCaddyBlock` | Small countertop cutlery-cup + napkin-holder caddy; symmetric, no facing. |
| `ServingHatchWindowBlock` | Wall-mounted kitchen-to-line pass-through hatch; FACING, not flammable. A wall fixture, distinct from any freestanding counter. |
| `ConferenceTableBlock` | Self-connecting dark-laminate boardroom table on chrome legs; extends `TableBlock` directly (reuses its NORTH/SOUTH/EAST/WEST connection logic unchanged — a subclass instance only ever connects to other `ConferenceTableBlock`s), so tiled conference tables merge into one long table the same way study tables do. Also counts for `DuckCoverHoldManager`'s duck-and-cover check (an `instanceof TableBlock` test). |
| `ExecutiveOfficeChairBlock`, `ConferenceCredenzaBlock`, `ConferenceWallDisplayBlock`, `FlipChartEaselBlock`, `ConferenceSpeakerphoneBlock`, `GlassOfficePartitionBlock`, `LoungeSofaBlock`, `PottedOfficePlantBlock`, `WindowBlindsBlock` | The remaining 9 Conference Room furniture blocks (added 2026-07-12), purely decorative. `ConferenceWallDisplayBlock`'s dark screen face reuses the existing `hazard_glass_screen_off` texture. |
| `OfficeCubiclePartitionBlock`, `ReceptionDeskBlock`, `MailSortingShelfBlock`, `OfficePhotocopierBlock`, `DocumentTrayStackBlock`, `WallBinderShelfBlock`, `OfficeSafeBlock`, `CoatRackStandBlock`, `BundyTimeClockBlock`, `OfficeSupplyCabinetBlock` | The 10 Office furniture blocks (added 2026-07-12), purely decorative. `OfficeSupplyCabinetBlock` reuses `FilingCabinetBlock`'s `cabinet_body_graphite`/`drawer_face_graphite` textures. |
| `LaboratoryFumeHoodBlock`, `EquipmentRackBlock`, `LabStoolBlock`, `OscilloscopeCartBlock`, `MicroscopeStationBlock`, `EyeWashStationBlock`, `ComponentDrawerCabinetBlock`, `SecuredCylinderRackBlock`, `SampleStorageRackBlock`, `BalanceScaleTableBlock` | The 10 Laboratory furniture blocks (added 2026-07-12), purely decorative — a research/testing/instrumentation angle deliberately distinct from the existing science-class hazard props (`bunsen_burner_station`, `reagent_storage_shelf`). `SecuredCylinderRackBlock` is the safe counterpart to the `leaking_oxygen_cylinder` hazard prop. |
| `FireBlanketItem` | One-shot smothering tool: used while on fire clears the player's fire ticks instantly; used on a hazardous kitchen/grease prop (`AbstractExtinguisherItem.isKitchenHazard`) smothers it via `HazardManager.defuse` — a valid Class F/K response distinct from a dry-chemical blast. 3 uses (durability), not auto-issued. |
| `FirstAidKitItem` | Heals 3 hearts, clears negative effects, applies a brief Slowness ("treatment time"). 5 uses. |
| `MegaphoneItem` | Instructor tool (OP level 2): broadcasts an evacuation chat line + klaxon to every player within 30 blocks, 5s cooldown. |
| `SafetyWhistleItem` | Loud positional ping + particle burst overhead, for group drills ("on me!"); 2s cooldown. |
| `FlashlightItem` | 60s of Night Vision — stand-in for staying oriented in smoke/blackout; 1-tick cooldown gate. |
| `ExitSignBlock` | Always-lit green EXIT sign (light level 7 via registration) — wayfinding decor readable even in a blackout scene. |
| `SmokeDetectorBlock` | Ceiling detector with `ALARMING` `BooleanProperty`; passive on its own — `SafetyDeviceManager` drives it during FIRE sessions (red-LED texture, blinking DustParticleOptions, beep). |
| `SprinklerHeadBlock` | Ceiling sprinkler; passive on its own — `SafetyDeviceManager` rains a water-particle curtain and extinguishes at most one fire block below per 40-tick cycle during FIRE sessions (buys time, doesn't win the fire alone). |
| `EmergencyLightBlock` | Wall light, dark by default (`LIT=false`, light level 10 when lit); `SafetyDeviceManager` sets `LIT=true` on every unit in an arena for the duration of any active session there. |
| `EvacuationMapBlock` | Wall-mounted map; right-click calls `SafetyDeviceManager.pointToNearestExit` — nearest-exit chat line + 30s Academy compass needle target. |
| `FireExtinguisherCabinetBlock` | Wall-mounted red cabinet with a glass front holding an extinguisher; FACING, purely decorative, distinct from the hose-reel `FireHoseCabinetBlock`. |
| `AssemblyPointSignBlock` | Standing green "ASSEMBLY AREA" sign on a post marking an `AssemblyZone` muster point; FACING, always lit (light 7) like `ExitSignBlock`, but marks the outdoor assembly point rather than a door. |
| `FirstAidWallCabinetBlock` | Wall-mounted white first-aid cabinet with a green cross; FACING, purely decorative. |
| `FireSafetyPosterBlock` | Flat wall poster teaching the PASS extinguisher technique / fire triangle; FACING, purely decorative. |
| `BlockedExitClutterBlock` | Stacked chairs/boxes jammed against a doorway — a training/discussion prop illustrating an obstructed egress; FACING, flammable, but not a `HazardBlock` (never ignites on its own). |
| `SafetyDeviceManager` | Drives the three passive safety blocks above on a 40-tick cycle: smoke-detector alarm + `smoke_detector_triggered` telemetry (FIRE sessions, fire within 8 blocks via the memoised `SimulationManager.nearestFireDistance`), sprinkler water curtain + single-fire-block extinguish, and emergency-light `LIT` toggling (any session, reset when it ends). Device positions scanned once per session and cached (mirrors `SimulationSession.hazardPositions`). Also owns the evacuation-map assist (`pointToNearestExit`). Registered via `TickScheduler` from a static block; `bootstrap()` called from `BerongSMP.commonSetup` forces the class load (same rule as `DuckCoverHoldManager`). |
| `SimRoom` | Enum mapping player position to a named room (LSPU Library or CCS). Holds `CcsRoom` record + `CCS_UPPER_ROOMS` (9 rooms, Y −25 to −22) and `CCS_GROUND_ROOMS` (7 rooms, Y −32 to −29) — all F3-verified absolute world AABBs. `fromPos()` / `fromCCSPos()` used for telemetry room labels. |
| `AssemblyZone` | Static utility in `common/zones/`; defines assembly-zone AABBs for all three buildings. `ZONE = AABB(30,-35,64,76,-28,82)` (LSPU Library, verified north of building) + `CCS_ZONE = AABB(76,-35,73,136,-28,90)` (open area immediately south of CCS Admin Building, Z:73–90) + `NEW_SIM2_ZONE = AABB(-164,-35,466,-148,-28,512)` (2026-07-14: **F3/WorldEdit-verified** via `//copyroom` — real open ground west of the building itself, X -118..-81; superseded the earlier 2026-07-13 "Lobby room" best-effort derivation, which turned out to be the wrong location entirely). `spawnBorderParticles(level, isCCS)`/`isInside(pos, isCCS)` are the original Library/CCS 2-way dispatch, left untouched; New Sim Building 2.0 gets its own parallel `spawnBorderParticlesNewSim2`/`isInsideNewSim2` rather than forcing a 3-way boolean. Fires `assembly_area_reached` telemetry + ends simulation via `onPlayerArrived` (shared by all three). |
| `TelemetryCsvWriter` | Writes per-tick and event rows to `run/telemetry/gameplay_logs_<YYYYMMDD>.csv` per telemetry contract v1.2 (§3). Also writes session-level sidecar `sessions_<YYYYMMDD>.csv` (§5, now including `final_fire_phase`), a dedicated `fire_logs_<YYYYMMDD>.csv` (2026-07-14 — `writeFireLogRow(sessionId, timestamp, x, y, z, event)`, `event` = `ignite`/`extinguish`; every real fire block igniting or being put out, kept separate from the general gameplay CSV so it isn't buried among move-tick rows), and one-time `map_metadata.json` on first server start (now including a `new_sim_building2_fire` scenario section with its own `fire_alarm_positions` from `scanAndRegisterNewSim2FireAlarms`, `survey_status: "assembly_area F3-verified via //copyroom (2026-07-14); exits are STALE placeholders derived from an earlier (now-wrong) assembly location, need a fresh F3 walk-through"`). Buffered, synchronous, server-thread only. `writeRow` gained a 14-arg overload carrying v1.2's `hit_fire`/`extinguisher_class`/`phase` columns — the legacy 11-arg call shape delegates with them blank, so every row in the file stays the same width. CSV event types: `session_start`, `move_tick` (genuinely 20 Hz as of 2026-07-13 — previously gated behind `ticks % 2 == 0`, silently sampling at 10 Hz despite this doc's claim; fixed to match telemetry_contract.md v1.2 §2, x/y/z + hazard_distance), `extinguisher_use`, `pin_pull`, `ext_spray`, `hazard_neutralize`, `phase_transition`, `fire_alarm_activate`, `assembly_area_reached`, `emergency_exit`, `door_open`, `session_end`. |
| `ExitZones` | Static record list in `common/zones/`; defines named AABB exit zones for all three buildings. `ZONES` (LSPU Library, `main_exit = AABB(50,-34,93,54,-30,96)` tuned) + `CCS_ZONES` (`ccs_main_exit = AABB(95,-33,68,125,-29,74)` — centre of south wall) + `NEW_SIM2_ZONES` (`new_sim2_lobby_south_door`/`new_sim2_lobby_north_door` — **STALE as of 2026-07-14**: derived back when the assembly zone was approximated as the Lobby room; now that `AssemblyZone.NEW_SIM2_ZONE` is F3-verified to sit well outside the building to the west, these Hallway↔Lobby doorway coords no longer point toward it and are known-wrong, kept only so `emergency_exit` fires *something* — see `docs/f3_tuning_todo.md` §7). `find(pos, isCCS)` is the original 2-way dispatch; New Sim Building 2.0 gets its own `findNewSim2(pos)`. Per-tick check in `SimulationManager` fires `emergency_exit` CSV event once per session crossing. |

### World Coordinates

- **Lobby**: origin `BlockPos(0, -33, 0)`, player spawn at `(8.8, -31, 8)`
- **Simulation arena**: `SIM_POS = BlockPos(30, -34, 83)`, player entry offset `+5.5, +2, +5.5`
- **SSC Building**: `SSC_POS = BlockPos(11, -33, 90)` (~19 blocks west of the library origin), placed with 1 CCW rotation
- **CCS Admin Building**: `CCS_POS = BlockPos(76, -34, 4)`, placed with 0 CCW rotations (`ccs_admin_building.schem`)
- **New Tutorial Building**: `AcademyBuildingManager.POS = BlockPos(-177, -34, 8)`, placed with 0 CCW rotations (`academy_building.schem`) — far west of every other structure, its own standalone footprint
- **New Sim Building 2.0**: `SimulationManager.NEW_SIM_BUILDING2_POS = BlockPos(-182, -34, 358)`, placed with 0 CCW rotations (`new_sim_building2.0.schem`) — far north of every other structure, its own standalone footprint. Room-level coordinates: [docs/new_sim_building2_rooms.md](docs/new_sim_building2_rooms.md)

#### CCS 1st Floor Named Rooms (`SimRoom.CCS_GROUND_ROOMS`)

Floor Y=−32, ceiling Y=−29 (3 blocks tall). Absolute world coords verified with F3.

| Room | X min | X max | Z min | Z max |
|---|---|---|---|---|
| Room 105 | 94 | 99 | 6 | 11 |
| Room 106 | 101 | 105 | 6 | 11 |
| Room 107 | 107 | 112 | 6 | 11 |
| Dean's Office | 114 | 119 | 6 | 11 |
| Faculty Room | 121 | 126 | 6 | 11 |
| ICTS | 130 | 136 | 17 | 26 |
| ICTS 2 | 131 | 136 | 28 | 31 |

#### CCS 2nd Floor Named Rooms (`SimRoom.CCS_UPPER_ROOMS`)

Floor Y=−25, ceiling Y=−22 (3 blocks tall). Absolute world coords verified with F3.

| Room | X min | X max | Z min | Z max |
|---|---|---|---|---|
| CCS Mini Library | 94 | 99 | 6 | 11 |
| Room 202 | 101 | 105 | 6 | 11 |
| Room 203 | 107 | 112 | 6 | 11 |
| Room 204 | 114 | 119 | 6 | 11 |
| Room 205 | 121 | 126 | 6 | 11 |
| TESOL | 130 | 136 | 17 | 22 |
| Computer Lab | 130 | 136 | 24 | 31 |
| MacLab | 130 | 136 | 33 | 39 |
| Room 207 | 132 | 136 | 41 | 49 |

`SimulationManager.findRandomSpawnInCCS()` shuffles both `CCS_GROUND_ROOMS` and `CCS_UPPER_ROOMS` into a single pool, scanning for a valid solid-floor + 2-air-above position. No blind arena scan — player always spawns inside a named room.

### Structures

Stored under `src/main/resources/data/berongsmp/structure/`:
- `lobby_structure.nbt` — lobby building with two buttons (NBT, placed once at server start)
- `lspu_library_main.nbt` — simulation arena (NBT, placed/restored each session)
- `ssc_building.schem` — SSC building adjacent to the arena (Sponge Schematic v3, placed/restored each session with 1 CCW rotation)
- `ccs_admin_building.schem` — CCS Admin building (Sponge Schematic v3, placed/restored each session with 0 CCW rotations)

- `academy_building.schem` — the Academy tutorial building (Sponge Schematic v3, placed once at server start by `AcademyBuildingManager`, 0 CCW rotations, NPCs/armor stands baked in)
- `new_sim_building2.0.schem` — New Sim Building 2.0 (Sponge Schematic v3, placed/restored each session with 0 CCW rotations, alongside SSC/CCS in `SimulationManager.BUILDINGS`); a real graded `/sim_fire new_sim_building2` scenario (`SimulationState.NEW_SIM_BUILDING2_FIRE` — prevention/intervention/evacuation, see [docs/systems/simulation.md](docs/systems/simulation.md)). No `/sim_earthquake` variant for this building yet.

`SimulationManager.BUILDINGS` holds the full list of `StructurePlacer`+`BlockPos` pairs iterated on session start and end.

### Config Knobs (`berongsmp-common.toml`)

All values are read at call time via `.get()` — changes take effect without restart:

| Key | Default | Meaning |
|---|---|---|
| `simDurationTicks` | 2400 | Session length (20 ticks = 1 second) |
| `fireSpawnCount` | 5 | Fire blocks placed per spawn event (bumped from 3 on 2026-07-14, faster spread) |
| `fireSpawnInterval` | 10 | Ticks between fire spawns (halved from 20 on 2026-07-14, faster spread) |
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
| `academyIgniteDemoTicks` | 200 | **No longer used** (2026-07-06): the ignite demo has no timeout anymore — the fire only goes out after 5 s of accumulated rolling (`ReyesRoomManager.ROLL_REQUIRED_TICKS`). Config entry kept to avoid breaking existing config files. |
| `academyGoStopGraceTicks` | 30 | Reaction window after Officer Cruz calls STOP (1.5 s) — movement during it is never punished (reference position keeps re-anchoring while the player slides to a halt); only movement after it counts as a violation |
| `academyPassThreshold` | 70 | Minimum score (0–100) Capt. Morfe requires to certify a player after the Academy |
| `newSimBuilding2PreventionTicks` | 2400 | New Sim Building 2.0: time to find/prevent all 5 armed hazards before unfound ones ignite (2 min) |
| `newSimBuilding2InterventionTicks` | 1800 | New Sim Building 2.0: time to extinguish every escalated hazard before it goes big and forces evacuation (90s) |
| `newSimBuilding2EvacuationTicks` | 1800 | New Sim Building 2.0: additional budget for the evacuation phase, added to the session's total duration (90s) |

### Student Session System (`session/` package)

Shared station accounts (e.g. `station1`) rotate through multiple students. `SessionManager` tracks a `StudentSession` per account UUID, persisted to the **Turso** cloud database via HTTP (no JDBC driver — uses Java's built-in `HttpClient` + Gson). All writes are fire-and-forget (`CompletableFuture.runAsync`). `TursoClient` creates the schema on first `init()` call.

**`/bfp` admin commands** (OP level 2 or `/bfp login <pin>`):

| Command | Effect |
|---|---|
| `/bfp bypass on [player]` | Skip lobby gates (registration, session, tutorial) for quick testing. Resets on server restart. |
| `/bfp bypass off [player]` | Re-enable lobby gates for the player. |
| `/bfp login <pin>` | Authenticate with config PIN; grants all /bfp access |
| `/bfp logout` | Revoke PIN-based access |
| `/bfp checkin <student_name>` | Start session for caller; resets tutorial state |
| `/bfp checkin <player> <student_name>` | Start session for target player |
| `/bfp checkout` | Finalise and save the caller's session |
| `/bfp reset [player]` | Wipe tutorial + delete DB row (no record kept) |
| `/bfp old_tutorial [player]` | Reset the **legacy** tutorial + teleport to its own lobby + re-init NPCs. Renamed from `/bfp tutorial` on 2026-07-13 now that the Academy is the default. |
| `/bfp tutorial [player]` | Renamed from `/bfp new_tutorial` on 2026-07-13 — **Activates** the Academy (the default tutorial): wipes `AcademyProgress` back to a fresh start, clears every room manager's transient state, teleports to `AcademyBuildingManager.DEFAULT_VIEWPOINT` (Room 1, currently `officer_cruz`). Same helper the main lobby's first button calls (`AcademyManager.startAcademyRun`). |
| `/bfp tutorial reset [player]` | Explicit, discoverable alias for the same reset-and-teleport the bare command above performs. |
| `/bfp tutorial <section> [player]` | Teleport to a named F3-captured reference viewpoint inside the Academy building (`AcademyBuildingManager.VIEWPOINTS`: `officer_cruz`, `sgt_reyes`, `sgt_santos`, `capt_morfe`) — plain dev-navigation teleport, no reset. One literal subcommand per map entry. For tuning NPC placement in-game (see `docs/major_plan.md`-style "needs in-game F3 tuning" tasks). |
| `/bfp tutorial skipto <instructor> [player]` | Dev shortcut to jump straight into any Academy room without replaying every prior one by hand. `<instructor>` is one of `cruz`/`reyes`/`santos`/`morfe`; marks every room strictly before it `DONE`, resets the target room (and everything after) to `NOT_STARTED`, applies the same cleanup `reset` does (dialogue cancelled, room-manager transient state cleared, Cruz snapped to her anchor, Reyes's extinguisher frames restocked, leftover Room 2 hazard props cleared), then teleports to that instructor's `VIEWPOINTS` entry. `skipto cruz` is equivalent to a full reset. |
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
| `/bfp user <username>` | Look up last 10 sessions for a `/register`/`/login` account username (2026-07-13). |
| `/bfp user delete <username>` | Deletes a `student_accounts` row so that username can be `/register`ed again (2026-07-13) — DB hygiene for stale/test accounts; leaves that account's `sessions` run history untouched. |

**Player-facing account commands** (2026-07-13, `RegistrationCommands`):

| Command | Effect |
|---|---|
| `/register <username> <password> <student_id> <section> <full_name>` | Creates a persistent Turso `student_accounts` account (salted PBKDF2 hash) and starts a session — same effect as before plus a durable login. Falls back to local-only registration with a warning if Turso is offline. |
| `/login <username> <password>` | Restores a returning student's identity and Academy-certified status (if `student_accounts.tutorial_completed=1`) without replaying the tutorial. Rate-limited (5 attempts / 5-minute lockout). |
| `/logout` | Clears the station's login state and checks out the active session. |
| `/history` | Lists the logged-in student's own last 10 runs. |

**Simulation management commands** (OP level 2):

| Command | Effect |
|---|---|
| `/sim_fire library` | Start FIRE simulation in the LSPU Library (gives ABC extinguisher) |
| `/sim_fire ccs` | Start FIRE simulation in the CCS Admin Building (gives CO2 extinguisher) |
| `/sim_fire new_sim_building2` | Start the prevention/intervention/evacuation FIRE scenario in New Sim Building 2.0 (gives all 3 extinguishers) — see [docs/systems/simulation.md](docs/systems/simulation.md) |
| `/sim_earthquake library [magnitude]` | Start EARTHQUAKE simulation in the LSPU Library |
| `/sim_earthquake ccs [magnitude]` | Start EARTHQUAKE simulation in the CCS Admin Building |
| `/sim_status [player]` | Live snapshot: type, phase, time remaining, fires extinguished |
| `/sim_list` | List all active simulations across all players |
| `/sim_freeze [player]` | Pause simulation timer (effects continue) |
| `/sim_unfreeze [player]` | Resume a frozen timer |
| `/sim_time set <seconds>` | Set remaining simulation time |
| `/sim_time add <seconds>` | Add/subtract seconds from remaining time |
| `/sim_scan_hazards` | GM-only dev tool: runs New Sim Building 2.0's building-wide hazard scan without starting a session, prints total + per-named-room breakdown — for verifying the arena bounds cover every placed hazard/computer/outlet. |
| `/get_co2_extinguisher` | Give CO2 extinguisher for Class C fires (any player) |
| `/get_wet_chemical_extinguisher` | Give yellow wet chemical extinguisher for Class F/K kitchen grease fires (any player) |
| `/item hazard <name>` | Give a hazard prop block item by registry name (tab-completes all 30 names). OP level 2. |
| `/item get <name>` | Give any custom BerongSMP item by registry name — extinguishers, hazard wand, computer, fire alarm, NPC spawners, furniture, hazards (tab-completes all `ALL_ITEM_MAP` names). Use `/item get hazard_wand` to grab the state-testing tool. OP level 2. |
| `/item kit` | Give one of every custom BerongSMP item at once, for quick full-scene dev testing. OP level 2. |
| `//copyroom [name...]` | Reads the caller's active WorldEdit selection, computes room dimensions/areas/volume, and copies a labeled human-readable summary (corners, dimensions, areas/volume) to the **server machine's** OS clipboard. Optional trailing name (e.g. `//copyroom Room 201`) is included as a header line. Requires the optional WorldEdit dependency (`worldedit-neoforge`, see `CopyRoomCommand`) to be installed; fails cleanly with a chat error otherwise. OP level 2. |

**Auto-hooks**: `TutorialManager.completeTutorial` → `SessionManager.onTutorialComplete` (records tutorial duration); `SimulationManager.endSimulation` → `SessionManager.onSimulationEnd` (records type/score/passed, closes row).

**Event log flush ordering**: `endSimulation` must capture `studentDbRowId` via `SessionManager.getActiveSession(uuid)` **before** calling `onSimulationEnd`, because `onSimulationEnd` calls `activeSessions.remove()` which would make the subsequent lookup return null and skip the `TursoClient.updateEventLog()` call. Fixed in commit `c5316d0`.

### Thread Safety

`SimulationManager.activeSessions` is a `ConcurrentHashMap`. `startSimulation` and `endSimulation` are `synchronized`. Tick-driven mutations are single-threaded via `ServerTickEvent.Post`. Network packet handling uses `context.enqueueWork()` to marshal HUD updates (including the new `intensity` field) onto the main client thread. `SimulationHud.intensity` is written on the main client thread and read from `ViewportEvent.ComputeCameraAngles`, which also fires on the main client thread — no extra synchronisation needed.

### Performance Notes

- **Fire-proximity scan memo** — `SimulationManager.nearestFireDistance` (the ~4,800-block hot scan) is memoised per `(game-tick, packed position)`. The several callers that need it for the same player on the same tick (PLAYER_TICK log, move_tick CSV row, `hazardDistance`) share one scan. Server-thread only, so the single-slot static cache needs no synchronisation. Scan radii live in named constants (`FIRE_SCAN_RADIUS_*`, `CCS_HAZARD_RADIUS_*`).
- **Earthquake PEAK scan** — `EarthquakeEffects.enqueuePeakDestructions` reads each scanned block state once (was 3×).
- **Turso writes** — run on a dedicated 2-thread daemon pool, not the common ForkJoinPool (see `TursoClient`).

### Repo Hygiene

The repo root no longer carries the decompiled vanilla `net/` reference dump, the `old_stuffs/` backup, or untitled dev screenshots — all removed and (where applicable) gitignored. Vanilla source lookups should use an external decompiler, not committed files.

### Client Installer & Distribution (`distribution/client-installer/`)

A player-facing one-click setup tool, added 2026-07-15 once manual "install NeoForge, drop a jar in
a folder" instructions proved to be a real friction point (Windows `Run` dialog can't open a
non-existent `mods` folder; players don't reliably have Java on PATH). `install-berongsmp.ps1` is
the tracked source (checks `.minecraft` exists, locates Java via the Minecraft Launcher's own
bundled runtime — checked in order: classic installer path, Program Files installer path, Microsoft
Store package path — falling back to system PATH; silently runs the bundled NeoForge installer with
`--install-client`; copies the mod jar into `mods/`, creating the folder if missing), compiled to a
standalone `Install-BerongSMP.exe` via the `ps2exe` PowerShell module. **Compiled-exe gotcha:**
`$PSScriptRoot`/`$MyInvocation.MyCommand.Path` don't reliably resolve inside a `ps2exe` binary (came
back empty in testing), which crashed the very first `Split-Path` call before any output could stay
on screen — looked exactly like "the installer window flashes red and closes instantly." Fixed by
falling back to `[System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName` when
`$PSScriptRoot` is empty, and wrapping the whole script in `try/catch/finally` so any future error
pauses on screen instead of vanishing — verified against this exact machine's real Minecraft
install (Java-detection paths matched) and, separately, with a throwaway compiled test exe that
confirmed the path-resolution fallback actually engages when compiled. The exe, its `payload/`
folder (bundled NeoForge installer jar + current mod jar), and the packaged
`BerongSMP-Client-Installer.zip` are all gitignored (regenerable binaries, not source) — only the
`.ps1` and the plain-language `README.txt` shipped inside the zip are tracked. Full build/update
steps and the distribution workflow (Google Drive upload, keeping the landing page's instructions in
sync) live in [`docs/adminmanual.md`](docs/adminmanual.md).

### Shared Base Classes & Helper Conventions

Reuse these instead of re-copying boilerplate:

- **`item/AbstractExtinguisherItem`** — base for handheld extinguishers (pin gate, spray ray, durability, charge tooltip, `isKitchenHazard`/`warnWrongTool`). New extinguisher = subclass + `extinguishAt`/particles/`sprayPitch`/`onSprayResolved` hooks.
- **`block/HorizontalFacingBlock`** / **`block/FlammableFacingBlock`** — base for FACING-only furniture; subclass supplies only `shapeFor(Direction)` (use `byFacing(...)`). Use the flammable variant for wood/paper props.
- **Internal command/event guards** — `BfpAdminCommands.appendSessionRow`, `ItemCommands.requirePlayer`, `LobbyManager.gatesPassed` centralise patterns that were previously duplicated; extend these rather than re-inlining.
- **Logging** — keep per-entity/per-iteration logs at `debug`; reserve `info` for once-per-operation summaries (see `SchemLoader`).

### Custom Texture Assets (Hazard Props + Computer/Fire Alarm)

All block models are now **custom-texture models** — hand-drawn 16×16 PNGs under `textures/block/` instead of stretched vanilla textures. The 11 furniture blocks (`WhiteboardBlock`, `ToiletBlock`, `SinkBlock`, `DrawersBlock`, `ComputerTableBlock`, `ChairBlock`, `FilingCabinetBlock`, `LockerBlock`, `TrashCanBlock`, `BulletinBoardBlock`, `CeilingFanBlock`) moved off vanilla-texture reuse in the Furniture Visual Remediation Log below (`scripts/generate_furniture_textures.py`), joining `ComputerBlock`, `FireAlarmBlock`, and all 20 `common/hazard/*` blocks (`scripts/generate_hazard_textures.py`) — the same way `computer_block.json` uses `computer_case`/`computer_screen_off`/`computer_keyboard`/`computer_mouse` instead of vanilla blocks.

Most hazard prop textures are generated by **`scripts/generate_hazard_textures.py`** (Pillow/PIL, deterministic via a fixed RNG seed) — re-run it after editing to regenerate that set:
```bash
python3 scripts/generate_hazard_textures.py
```
The 30-block Conference Room/Office/Laboratory batch (10 furniture + 10 hazard props per room,
props 56–85) has its own batch script, **`scripts/generate_room_textures.py`**, matching the
repo's precedent of one script per content drop rather than growing the original script further —
it reuses the shared hazard-accent textures (`hazard_ember_glow`, `hazard_warning_led`,
`hazard_spark_arc`, `hazard_smoke_stain`, `hazard_glass_screen_off`/`_glitch`, `hazard_bare_copper`,
`hazard_water_stain`, …) by reference in model JSON wherever a prop's failure state matches an
existing accent, rather than redrawing it.
It produces two tiers of texture:
- **Per-object body textures** (`bin_plastic_gray`, `cardboard_box`, `pc_tower_case`, `locker_door_steel`, `stove_burner_metal`, `fryer_body_steel`, etc.) — one look per prop, normal vs. hazardous variants where the object physically changes (e.g. `lipo_foil_silver` → `lipo_foil_damaged`, `hood_steel_clean` → `hood_grease_dirty`).
- **Shared hazard-accent library**, reused across many blocks the same way `computer_block.json` shares `case`/`dark`: `hazard_ember_glow` (hot coil/burner), `hazard_warning_led` / `hazard_ok_led` (red/green indicator clusters), `hazard_spark_arc` / `hazard_spark_arc_green` (electric arcs), `hazard_smoke_stain`, `hazard_grease_stain`, `hazard_scorch_char`, `hazard_crumpled_paper`, `hazard_bare_copper`, `hazard_water_stain`, `hazard_glass_screen_off` / `hazard_glass_screen_glitch`.

The 10 newest school-zone hazard props (`scripts/generate_hazard_textures.py`, later additions), the 10 school-decor/furniture blocks (`scripts/generate_school_textures.py`), and the 5 safety-equipment blocks (`scripts/generate_safety_textures.py`) all follow this same hand-drawn-PNG convention. `hazard_wand`'s item icon was upgraded off the vanilla `minecraft:item/blaze_rod` placeholder to a themed caution-taped rod with a glowing tip via `scripts/generate_hazard_wand_texture.py` → `textures/item/hazard_wand.png`.

**Known model-format gotcha**: a block model face's `"texture"` value **must** be a `"#variable"` reference resolved via that model's own `"textures"` map — a raw `"minecraft:block/xxx"` string directly in a face renders as the missing-texture (magenta/black) placeholder in-game, since Minecraft doesn't resolve namespaced IDs at the face level. (Found and fixed in `plastic_trash_bin_hazardous.json`'s `vape_glow` element.)

**Second known model-format gotcha (found 2026-07-08, item-rendering audit)**: every custom block model **must** declare `"parent": "minecraft:block/block"` (directly, or via a chain that resolves to it) even though the model also lists its own `"elements"`. Without a parent, the model has no inherited `"display"` transforms block, so any item that uses `"parent": "berongsmp:block/xxx"` as its inventory icon renders unscaled and unrotated in the GUI — the block appears full-size/front-on in the slot instead of the standard isometric, scaled-down icon every other block in the mod uses. This silently affected 35 files: all 5 of the newest safety-equipment blocks (`exit_sign`, `smoke_detector`(+`_on`), `sprinkler_head`, `emergency_light`(+`_on`), `evacuation_map`), all 10 school-decor blocks (`teachers_desk`, `armchair_desk`, `tall_bookshelf`, `philippine_flag_stand`, `trophy_cabinet`, `water_dispenser`, `wall_clock`, `blackboard`, `podium_lectern`, `classroom_globe`), and 10 of the 30 hazard props (`overloaded_microwave`, `bunsen_burner_station`, `reagent_storage_shelf`, `overloaded_breaker_panel`, `overheating_wall_aircon`, `jammed_laser_printer`, `unattended_shrine_candle`, `leaking_gas_valve`, `alcohol_dispenser_station`, `clogged_exhaust_fan`, plus each one's `_hazardous` variant). Fixed by adding the parent key to all 37 files. When authoring a new block model by hand (not through a `generate_*_textures.py` script), always include `"parent": "minecraft:block/block"` as the first key — every correctly-rendering block in the mod (furniture, the original 20 hazard props, `computer`/`fire_alarm`) already does this.

### Hazard-State Tiers & Flat-Modern Restyle (2026-07-12)

Every hazard prop's `HAZARDOUS`/`ON_FIRE` blockstate now maps to genuinely distinct art instead of
`on_fire=true` silently reusing the `_hazardous` model (the pre-existing behavior — see
`scripts/add_onfire_blockstates.py`'s original docstring, which explicitly deferred bespoke burning
art). `scripts/_texture_style.py` gained `hazardize()`/`charify()`: a universal "Caution Amber"
(warm shift + amber/black chevron band) treatment for HAZARDOUS and a "Char & Ember" (darkened +
glowing crack) treatment for ON_FIRE, applied identically to every prop regardless of its own
bespoke art. `scripts/generate_hazard_state_tiers.py` mechanically derives a `<texture>_hz`/`_of`
copy of every texture referenced by each `*_hazardous.json` model (deduplicated by source filename
so a shared texture like `fixture_chrome` is only transformed once across all 85 props), generates
the matching `*_on_fire.json` model, and repoints every blockstate's `on_fire=true` variants —
**never mutating a source texture in place**, since some accent files (e.g. `hazard_glass_screen_off`)
are also referenced by unrelated normal-state furniture models. Re-run after editing any hazard
prop's normal-state texture or model:
```bash
python3 scripts/generate_hazard_state_tiers.py
```
Separately, every `generate_*_textures.py` script's local `gradient_shade` helper (previously a
smooth per-pixel diagonal gradient, duplicated near-identically across 8 scripts) now quantizes into
4 discrete tone bands at higher contrast (`light=1.45, dark=0.62`, matching `_texture_style.py`'s
`LIGHT_FACTOR`/`SHADOW_FACTOR`) — a flat-shaded, cel-shaded look instead of a soft airbrush gradient,
applied mod-wide by editing one shared helper per script rather than each object's individual
drawing function. Whenever a script that also feeds hazard prop bodies is re-run, re-run
`generate_hazard_state_tiers.py` immediately after so the `_hz`/`_of` variants stay derived from the
current normal-state art. `light_bulb`'s texture is deliberately excluded (a seamless glow tile,
banding would break the edge-to-edge tiling). Full log:
**[docs/history/hazard-tier-and-tab-reorg-log.md](docs/history/hazard-tier-and-tab-reorg-log.md)**.

### Block Registration Pattern (NeoForge 26.x)

Custom block subclasses **must** use `BLOCKS.registerBlock(name, Constructor::new, () -> Block.Properties.of()...)` — NOT `BLOCKS.register(name, () -> new MyBlock(Block.Properties.of()...))`. In NeoForge 26.1.2, `BlockBehaviour.<init>` calls `effectiveDrops()` which requires the registry key to already be set on the Properties object. The `registerBlock` overload injects the key before passing Properties to the constructor; the plain `Supplier` overload does not. Using a plain `Supplier` causes a `NullPointerException: Block id not set` crash at startup.

### Client–Server Split

`BerongSMPClient` is annotated `@Mod(dist = CLIENT)` and only loads on the physical client. `SimulationHud` and `KeyMappings` are client-only classes registered through the mod event bus, keeping the server JAR free of rendering dependencies.

---

## Health-Check Remediation Log

Tracks fixes applied from the 2026-06-23 health check report. Full log:
**[docs/history/health-check-log.md](docs/history/health-check-log.md)**.

---

## Telemetry Gap Remediation Log

Tracks fixes applied from the 2026-06-23 telemetry gap analysis (ranked Critical → Low), including
Academy telemetry (T-17). Full log: **[docs/history/telemetry-gap-log.md](docs/history/telemetry-gap-log.md)**.

---

## Hazard Prop Visual Remediation Log

Tracks fixes applied after an in-game screenshot showed hazard prop blocks rendering with a
missing-texture face and looking like flat colored boxes. Full log:
**[docs/history/hazard-visual-log.md](docs/history/hazard-visual-log.md)**.

---

## Furniture Visual Remediation Log

A 2026-07-02 audit found zero real rendering bugs; the 11 furniture blocks just needed the same
vanilla-texture-to-custom-texture upgrade already done for hazard props. Full log:
**[docs/history/furniture-visual-log.md](docs/history/furniture-visual-log.md)**.

---

## Cruz Pathfinding Recommendations (2026-07-05, Fable-5 architectural review)

A dedicated Plan-agent review of `CruzRoomManager`'s escort logic and `CustomNpcEntity`'s navigation
setup. Full review, what was implemented, and what remains a documented-but-not-done recommendation:
**[docs/history/cruz-pathfinding-recommendations.md](docs/history/cruz-pathfinding-recommendations.md)**.

---

## Hazard Prop 3-State Log (2026-07-05)

All 19 hazardous-property props gained a genuine 3-state lifecycle (normal → hazardous → on-fire). Full log: **[docs/history/hazard-3state-log.md](docs/history/hazard-3state-log.md)**.

---

## Hazard Prop State Management Log

Tracks the rollout of gameplay-driven state management for the 20 hazard prop blocks. Full log: **[docs/history/hazard-state-management-log.md](docs/history/hazard-state-management-log.md)**.

---

## Code Review Remediation Log (2026-07-17)

A full-codebase architecture/security/performance/concurrency review (`docs/code_review_2026-07-17.md`)
found, most notably: `startSimulation`/`endSimulation` re-placing all 4 buildings on every session
start/end (corrupting any other concurrently-running session sharing a building), blocking Turso
HTTP calls on the server/command thread, `/sim_fire`/`/sim_earthquake`/`/sim_stop` missing their OP
permission gate, and fire-run pass/fail computed two different, disagreeing ways between
`SimulationManager` and `SessionManager`. All Immediate + Short-term action items were fixed and
verified via `./gradlew compileJava test` (38 tests, 0 failures) in this pass; the concurrent-session
and Turso-async behavior still needs an in-game multi-station test since `SimulationSession`
requires a live `ServerPlayer`/`ServerLevel` to construct. Full list of fixes and what was explicitly
deferred (session-holds-Player refactor, Turso write-journal, multi-trainee Academy, gametests,
hazard-prop `HazardSpec` migration): **[docs/history/code-review-remediation-log-2026-07-17.md](docs/history/code-review-remediation-log-2026-07-17.md)**.

---

## Multiplayer & Concurrency Audit Log (2026-07-17)

A follow-up to the Code Review Remediation Log above, specifically checking (1) whether any run
can silently lose/drop an item, and (2) real concurrent/repeated-user capacity. Verified via direct
NBT inspection of all 6 structure files (not guesswork) that no block-destruction path in the mod
ever drops a physical item and no vanilla containers exist in any structure to spill contents. Found
and fixed a real extinguisher-loss bug (`SimulationManager.startSimulation` silently destroyed a
player's existing extinguishers when issuing fresh ones) and two real per-player state leaks with no
logout cleanup (`SessionManager` had no logout hook at all; `DuckCoverHoldManager`'s compliance
maps only cleared for still-online players). Documented the real architectural ceiling: exactly 3
simultaneous graded-simulation sessions (one per physical building — Library, CCS, New Sim Building
2.0), enforced correctly by the prior pass's arena-occupancy guard, not a bug. Sized up the Turso/
Auth executor pools for classroom-scale registration bursts. Extracted `SimulationArenas` (the
state→arena mapping) into its own NeoForge-free class with real passing unit tests, since
`SimulationManager` itself can't be safely referenced from plain JUnit. Full log, including why a
full NeoForge GameTest was investigated and deliberately not attempted, and a manual multiplayer
verification checklist for the user: **[docs/history/multiplayer-concurrency-audit-log-2026-07-17.md](docs/history/multiplayer-concurrency-audit-log-2026-07-17.md)**.

---

## Deep-Dive Audit Log (2026-07-17)

A third follow-up pass covering areas not yet examined by the two logs above: command permissions
beyond `/sim_*` (all correct, no fixes needed), SQL injection surface across every `TursoClient`
call site (none found — every call parameterizes user data correctly), client-side code (correct),
network payload validation, entity/AI code, and the hazard/computer block state machines. Found and
fixed two real bugs: `DropAndRollPayload` had no server-side rate limit at all (a modified client
could spam it far faster than any real key-press); `ComputerBlock.randomTick`'s "seed fire near
flammable furniture" logic checked `BlockState.ignitedByLava()` — verified via research and direct
schematic inspection that this vanilla method is true almost exclusively for TNT, so real
`dark_oak_planks`/`stripped_oak_log` furniture near CCS computers was never actually catching fire
through this path. Also brought `HazardFacingBlock`'s ignite helpers back in sync with `HazardBlock`'s
(a latent, zero-impact consistency gap). Full log, including one documented-but-not-fixed
architectural finding (hazard ignition helpers lack an arena-bounds check, though the exposure is
narrow and self-healing via an existing cleanup sweep):
**[docs/history/deep-dive-audit-log-2026-07-17.md](docs/history/deep-dive-audit-log-2026-07-17.md)**.

---

## NeoForge 26.2 Migration Log (2026-07-17)

Moved from NeoForge `26.1.2.36-beta` (Minecraft `26.1.2`) to NeoForge `26.2.0.23-beta` (Minecraft
`26.2.0`), purely to stay current. Every fix was a mechanical call-site adaptation to a Mojang-side
API rename/restructure, verified against the actual decompiled NeoForm sources rather than guessed:
colored block constants (`Blocks.RED_CONCRETE`, etc.) consolidated into a `ColorCollection<Block>`
+ `.pick(DyeColor)`; `I18n.exists(String)` removed (replaced with `Language.getInstance().has`);
`EntityType.create` now takes an `EntitySpawnRequest` instead of a bare `EntitySpawnReason`;
`EntityType.VILLAGER` and all other vanilla entity-type constants moved to a new `EntityTypes`
class. Also fixed a WorldEdit dependency-resolution break: EngineHub's artifact-naming convention
changed between MC lines, so `gradle.properties` gained a separate `worldedit_mc_version` property
decoupled from `minecraft_version` (see the `CopyRoomCommand` row above). No building/texture asset
was touched — proven via `git diff --stat` and a before/after SHA-256 checksum comparison, not just
asserted. `compileJava`/`test`/`runServer` all verified clean, plus a full manual playtest. The
client installer was updated to this exact NeoForge build, closing the previously-noted gap where
the live server ran a newer NeoForge patch (`26.1.2.80`) than this repo's own dev pin. Full log:
**[docs/history/neoforge-26.2-migration-log-2026-07-17.md](docs/history/neoforge-26.2-migration-log-2026-07-17.md)**.

**2026-07-18 retarget:** the live deploy revealed MCServerHost's installer only offers NeoForge
build `26.2.0.0-beta` for MC 26.2.0 (no field to pick a later patch build like `.23-beta`), and that
WorldEdit 7.4.4 itself requires `26.2.0.7-beta` or above — incompatible with `.0-beta` regardless of
this mod's own pin. `neo_version` was retargeted down to `26.2.0.0-beta` (same Minecraft version,
so none of the MC-API fixes above needed revisiting) and WorldEdit's `localRuntime` dev dependency
was disabled (`compileOnly` stays; `//copyroom` already tolerates WorldEdit's absence). All gates
re-verified clean. Full log:
**[docs/history/neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md](docs/history/neoforge-26.2.0.0-beta-retarget-log-2026-07-18.md)**.

---

## Synthetic Dataset Reference

The companion dashboard repo (`BERONG_SMP_WEB/apps/dashboard/scripts/seed-synthetic.mjs`) contains a seeder for 20 sessions used for dashboard testing: 7 Library FIRE, 5 Library EARTHQUAKE, 5 CCS_FIRE, 3 CCS_EARTHQUAKE. All movement paths use real building coordinates and correctly cross the exit zones and assembly zones. Run with `node apps/dashboard/scripts/seed-synthetic.mjs` from the web repo root.

### Dashboard Movement Map (`MapPlayer.tsx`)

The session detail page (`sessions/[id].astro`) renders a `MapPlayer` React island when `move_log_csv` is non-null. The map:
- Parses `move_log_csv` client-side; filters rows by `event_type === 'move_tick'` for path rendering
- Initialises to the last frame so the full player journey is visible immediately (click ↺ to replay)
- Uses `var(--text-muted)` / `var(--border-card)` CSS variables for ghost path and room labels — renders correctly on both dark and light themes
- **Light theme gotcha:** SVG ghost path was previously `rgba(255,255,255,0.07)` (invisible on `--bg-log-panel: #ede9e5`); now uses `var(--text-muted)` with 0.3 opacity

### Event log invariants verified by the seed script

These constraints reflect what real mod sessions must produce — if the mod deviates, the synthetic baseline will diverge from live data:

| Invariant | What the mod must emit | Location in mod |
|---|---|---|
| `EXT_PIN_PULL` before CO2 use | `session.logger.log("EXT_PIN_PULL", ...)` emitted when CO2 pin is pulled | `CO2ExtinguisherItem` (same flow as `FireExtinguisherItem`) |
| Library assembly zone reachable | Players evacuating the Library walk north (z decreasing) from ~Z:83 to reach `AABB(30,-35,64,76,-28,82)` | `AssemblyZone.isInside(pos, false)` |
| CCS assembly zone reachable | Players evacuating CCS walk south (z increasing from ~Z:4–72) to reach `AABB(76,-35,73,136,-28,90)` outside the south wall | `AssemblyZone.isInside(pos, true)` |
| `assembly_area_reached` x/y/z inside AABB | Library coords inside `(30,-35,64,76,-28,82)`; CCS coords inside `(76,-35,73,136,-28,90)` | `AssemblyZone.onPlayerArrived()` |
| `SIM_START` includes `x/y/z` | Spawn position logged after `spawnPos` is resolved | `SimulationManager.startSimulation()` |
| Section codes no-hyphen | e.g. `BSCS3A` not `BSCS-3A` | `/register` command user input |
