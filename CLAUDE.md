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
  [hazard-state-management-log.md](docs/history/hazard-state-management-log.md)
- **`docs/major_plan.md`** — phased implementation plan and Turso schema (see Master Plan above)
- **`docs/new_tutorial_script.md`** — Academy dialogue script, coordinate tables, and flow diagram

When a change touches a system with its own doc, update that doc — not a growing section here.

## Development Workflow

**After completing any feature or phase goal:**
1. Update `CLAUDE.md` — add/update the Key Classes table and architecture notes to reflect what changed.
2. Update `docs/major_plan.md` — mark the completed deliverable `[x]`.
3. Commit the changes with a descriptive message and push to `main`.

This keeps the project documentation in sync with the code at all times.

---

## Project Overview

BerongSMP is a NeoForge mod for Minecraft 26.1.2 (NeoForge 26.1.2.36-beta) that implements a disaster simulation minigame. Players enter a lobby, press buttons to trigger fire or earthquake simulations inside an LSPU Library NBT structure, or use commands (`/sim_fire ccs`, `/sim_earthquake ccs`) to run simulations inside the CCS Admin Building. Players are scored on their response. The mod is built with Java 25.

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
| `registry/ModBlocks` | All 37 `DeferredBlock` registrations: computer, fire alarm, furniture, and the 20 hazard props. |
| `registry/ModItems` | All `DeferredItem` registrations (NPC spawners, block items, extinguishers, hazard wand, firefighter uniform + `ArmorMaterial`). Owns `HAZARD_ITEM_MAP` (LinkedHashMap; hazard-tab + `/item hazard` insertion order) and `ALL_ITEM_MAP` (superset for `/item get` / `/item kit`). |
| `registry/ModCreativeTabs` | The three creative tabs: `SIM_TAB` (sim_tab — extinguishers, computer, fire alarm, NPC spawners), `FURN_TAB` (furn_tab — furniture), `HAZARD_TAB` (hazards_tab — all 20 hazard props, icon = daisy_chain_extension). |
| `registry/ModEntities` | `CUSTOM_NPC` entity type + its attribute-creation listener. |
| `registry/ModSounds` | `FIRE_ALARM_RING` sound event. |
| `registry/ModAttachments` | `DROPPED_TICKS` synced attachment driving the client drop-and-roll animation. |
| `SimulationManager` | Session registry (`ConcurrentHashMap<UUID, SimulationSession>`), tick driver, event handlers for tick/respawn/logout |
| `SimulationSession` | Per-player mutable state: timer ticks, disaster type, fires extinguished count, earthquake epicenter/phase/cascade queue/magnitude/aftershockCount/aftershockMagnitudeScale; `arenaOrigin/spanX/spanZ/height` set by SimulationManager to target the correct building |
| `SimulationSession.EarthquakePhase` | Inner enum: `RUMBLE → PEAK → AFTERSHOCK(×2–4) → END`; AFTERSHOCK loops with a random magnitude scale before advancing to END |
| `SimulationEffects` | World mutation: fire placement + smoke particles (14×, wide plume) + proximity nausea/air-drain; phase-aware earthquake (RUMBLE/PEAK/AFTERSHOCK helpers + cascade drain + `breakOrDebris` for falling debris with 8× damage multiplier) |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = fire, higher Z = quake), login/button-click handlers |
| `StructurePlacer` | Interface for placing a structure at a `BlockPos`; implemented by both loaders below |
| `SimulationStructureLoader` | Implements `StructurePlacer`; wraps `StructureTemplateManager` for `.nbt` files |
| `SchemLoader` | Implements `StructurePlacer`; parses Sponge Schematic v2/v3 `.schem` files, supports 0–3 CCW 90° rotations (rotates offsets and block states), places blocks, and spawns entities from the `Entities` tag — including modded mobs like `berongsmp:custom_npc`, not just vanilla decoration entities. Any pre-existing non-player entity in the placement footprint is discarded before re-placing to prevent duplicates (broadened from an item-frame-only check once schematics started baking in mobs/armor stands too — session restores never touch players, since they're explicitly excluded). **Item frame placement invariant (MC 26.x):** Sponge v3 top-level `Pos` = the entity's own AIR block (not the wall). `Data.Facing` = OUTWARD direction (frame face toward viewer, no `.getOpposite()` needed). `ItemFrame(level, pos, direction)` takes the entity's own block as `pos`; the wall is `pos.relative(direction.getOpposite())` — handled by a dedicated `spawnItemFrame` path since item frames need this wall-relative math the generic path doesn't do. **Generic entity deserialization gotcha (found while loading `academy_building.schem`):** Sponge v3 nests an entity's *real* Minecraft save data under a `Data` sub-compound — `Id`/`Pos` are Sponge-level siblings, not part of it. Deserializing the raw entity tag directly (as this used to) left every actual field invisible to `EntityType.create`, so `CustomNpcEntity` silently read a missing `NpcType` and fell back to its default role for every copy. Fixed by merging `Data` into a fresh root before deserializing. That same root also needs `block_pos` (used by `BlockAttachedEntity` subclasses like `Painting`, checked with a 16-block sanity radius against the entity's real position) and any `facing`/`Facing` byte re-derived/rotated — left stale, `block_pos` still pointed at the original copy location, failed the sanity check, and vanilla logged "Block-attached entity at invalid position" while the entity failed to attach. |
| `AcademyBuildingManager` | Places `academy_building.schem` (a WorldEdit `//copy -e` capture) at the fixed `POS = BlockPos(-177, -34, 8)` via `SchemLoader`, 0 rotations. The schematic bakes in its own 10 `berongsmp:custom_npc` NPCs (the duplicate second Officer Cruz at the old two-NPC-handoff spot was NBT-edited out of the .schem itself — 29 → 28 entities), 10 gear-display armor stands, item frames/glow item frames, and a painting — unlike `TutorialLobbyManager` (structure + hardcoded-offset NPCs as two separate passes), everything here comes from one `SchemLoader.place` call. Called from `onServerStarted`, not `onServerStarting`, for the same reason `TutorialLobbyManager.initNpcs` is: entity chunk storage must be fully loaded first, or freshly-spawned entities can collide with same-UUID copies the previous server run persisted to disk. Also owns `VIEWPOINTS` (`Map<String, Viewpoint>`) — named F3-captured admin teleport targets inside the building, one per named station, surfaced via `/bfp new_tutorial <name>`. `sweepStrayCruz` (public, tiny single-chunk AABB query around `(-122,-33,49)`) is called every tick from `AcademyManager.tick()` — not just once at boot — since a disk-persisted stray entity there only ever becomes visible to `getEntitiesOfClass` once a player walks close enough for that chunk to reach Minecraft's entity-ticking ring, so a boot-only scan could never actually catch her. `discardDuplicateCruz` calls `sweepStrayCruz` once at boot too, then — only if more than one legitimate `OFFICER_CRUZ` copy still remains — keeps the one nearest the briefing anchor (a lone survivor is never touched). `placeGreenMarks` is a permanent per-placement fixup: swaps the 4 floor blocks under `CruzRoomManager.GREEN_MARKS` to lime concrete (the schematic has no green blocks in the briefing zone), re-run after every placement since `SchemLoader` restores the original floor each boot. (A `placeFireAlarm` method used to also place a `FireAlarmBlock` for Sgt. Reyes's alarm checkpoint here — removed 2026-07-05 once parsing the schem revealed it already bakes one in at that exact spot, see Room 2 above.) |
| `SimulationStatusPayload` | Server→client packet (record + `StreamCodec`, channel v2) carrying `status`, `timeLeft`, and `intensity` |
| `DropAndRollPayload` | Client→server packet (channel v3) — the mod's first serverbound payload; empty record, `StreamCodec.unit(...)`, sent when the player presses the "Drop and Roll" key. Handler calls `DropAndRollManager.onDropAndRollRequest`. |
| `DropAndRollManager` | Static per-UUID transient state (`droppedTicksRemaining`, same idiom as `TutorialManager.holdOnTimers`) driving the "stop, drop, and roll" fire response: reduces the requester's remaining fire ticks by 30/press if on fire, opens/extends a 100-tick "dropped" window during which `MobEffects.SLOWNESS` is continuously refreshed (crawl stand-in). `tick()` runs from `SimulationManager.onServerTick`. |
| `SimulationHud` | Client-side HUD renderer; drives multi-layer camera shake (1 Hz + 3 Hz oscillations + jitter + roll) via `ViewportEvent.ComputeCameraAngles` using `intensity` |
| `Config` | `ModConfigSpec` entries for all simulation tuning knobs |
| `ModCommands` | Thin registration shell — delegates to `RegistrationCommands`, `ItemCommands`, `SimulationCommands`, `BfpAdminCommands`; also forwards `clearAuthorizations()` |
| `RegistrationCommands` | `/register <student_id> <section> <full_name>` |
| `ItemCommands` | `/spawn_lspu`, `/get_extinguisher`, `/get_co2_extinguisher` |
| `SimulationCommands` | `/sim_fire <library\|ccs>`, `/sim_earthquake <library\|ccs> [magnitude]`, `/sim_magnitude`, `/sim_stop`, `/sim_status`, `/sim_list`, `/sim_freeze`, `/sim_unfreeze`, `/sim_time` |
| `BfpAdminCommands` | All `/bfp` admin commands; owns `bfpAuthorized` Set and `isBfpAuthorized()` predicate |
| `AbstractExtinguisherItem` | Shared base for all three extinguishers: safety-pin gate, held-spray ray geometry, durability drain, sound scaffolding, nearby-player count, and charge tooltip. Also owns `KITCHEN_HAZARD_IDS`/`isKitchenHazard` (the five Class F/K kitchen hazard prop IDs) and `warnWrongTool` (per-player 60-tick-throttled chat warning), used by `FireExtinguisherItem`/`CO2ExtinguisherItem` to refuse those props. Subclasses supply only `extinguishAt`, particles, `sprayPitch`, telemetry (`onSprayResolved`), and flavour messages. |
| `FireExtinguisherItem` | ABC dry-chemical extinguisher (extends `AbstractExtinguisherItem`); extinguishes fire/soul-fire/LIT blocks, counts toward FIRE score via `recordExtinguish()`, drives the tutorial PASS drill. 300 durability. Cannot defuse the five kitchen hazard props (`AbstractExtinguisherItem.isKitchenHazard`) — spraying a hazardous one triggers a throttled "wrong extinguisher" chat warning instead. |
| `CO2ExtinguisherItem` | Green CO2 extinguisher for Class C electrical fires (extends `AbstractExtinguisherItem`). Targets `ComputerBlock` with `BURNING=true` → sets BURNING=false + LIT=false + BROKEN=true (computer is destroyed after fire). Also suppresses regular fire/soul fire. 200 durability. Same kitchen-hazard exclusion and warning as `FireExtinguisherItem`. |
| `WetChemicalExtinguisherItem` | Yellow wet chemical extinguisher (extends `AbstractExtinguisherItem`) — Philippine BFP Class F/K colour-coding for cooking-oil/grease fires. Suppresses regular fire/soul fire and defuses any hazard prop via `HazardManager.defuse`, with distinct "saponification complete" feedback + golden `DustParticleOptions` foam mist when the target is one of the five kitchen hazard props (`unattended_grease_pan`, `grease_clogged_hood`, `contaminated_kitchen_bin`, `jammed_panini_press`, `commercial_deep_fryer`). **It is the only extinguisher that can defuse those five** — ABC and CO2 skip them entirely, mirroring the real Class B vs. Class F/K distinction (a dry-chemical/CO2 blast can splash or re-flash a deep-fat fire instead of smothering it). 240 durability. Not auto-issued at simulation start (no dedicated kitchen scenario state yet) — obtainable via `/get_wet_chemical_extinguisher`, `/item get wet_chemical_extinguisher`, or `/item kit`. |
| `HazardWandItem` | Dev-only tool (`berongsmp:hazard_wand`) for testing hazard prop states without `/setblock` coordinates — right-click a hazard prop to call `HazardManager.activate`/`defuse` (toggle normal↔hazardous) or `setSawdustLevel` (step accumulation 0→5); shift+right-click calls `HazardManager.forceFailure` to trigger the failure consequence immediately. Works with or without an active simulation session (`SimulationManager.getSession` may be null; `HazardManager`'s mutation entry points are all session-nullable). |
| `ComputerBlock` | Custom block in `block/` package; registry ID `berongsmp:computer` (field `BerongSMP.COMPUTER`). Has `FACING` (horizontal), `LIT`, `BURNING`, and `BROKEN` states. State machine: OFF↔ON (right-click), ANY→BURNING (flint & steel — immediately places fire on all adjacent air), BURNING→BROKEN (CO2 extinguisher). `BURNING=true`: scans 2-block radius every `randomTick` for `ignitedByLava()` blocks and seeds vanilla fire next to them (enabling chain-spread into wood/wool/leaves); `animateTick` emits FLAME from top + all 4 sides, SOUL_FIRE_FLAME (cyan electrical signature), wild ELECTRIC_SPARK arcs, LARGE_SMOKE columns, LAVA ember drips; light level 15. `BROKEN=true`: cracked screen + scorched case texture, all interactions blocked, emits occasional SMOKE wisps. Only CO2 extinguisher ends the fire (and causes BROKEN). Registered via `BLOCKS.registerBlock(name, Constructor::new, () -> Props)` pattern required by NeoForge 26.x. |
| `TutorialStage` | Enum of all tutorial stages: `NOT_STARTED → PASS_SPRAY → EXT_TYPE_A/B/C → QUAKE_DROP/COVER/HOLDON → COMPLETED` |
| `TutorialManager` | Static utility: station placement, interaction dispatch, extinguish counting, QUAKE tick detection, completion. Gates simulation buttons via `isComplete(UUID)`. `@EventBusSubscriber`-annotated for `onPlayerLogout`, which clears its transient maps and rolls a mid-earthquake-drill stage (`QUAKE_DROP`/`COVER`/`HOLDON`) back to `QUAKE_INTRO` — see Tutorial Flow above. |
| `TutorialSavedData` | Extends `SavedData`; persists `Map<UUID, TutorialStage>` to `world/data/berongsmp_tutorial.dat` |
| `TutorialStatusPayload` | Server→client packet carrying `prompt` (String) and `intensity` (float) for tutorial HUD and camera shake |
| `TutorialHud` | Client-side HUD renderer for tutorial prompts; drives camera shake during QUAKE stages; hidden when SimulationHud is active |
| `StudentSession` | POJO holding per-student data: name, account UUID, start/end times, tutorial timing, simulation type/score/passed, Turso row ID |
| `TursoClient` | HTTP wrapper for the Turso libSQL REST API (`/v2/pipeline`); fire-and-forget async writes via `CompletableFuture` on a **dedicated 2-thread daemon executor** (kept off `ForkJoinPool.commonPool()`), synchronous reads for commands; creates schema on first init. `shutdown()` stops the executor cleanly. |
| `SessionManager` | Manages `Map<UUID, StudentSession>` for shared station accounts; hooks into tutorial completion and simulation end to persist scores; exposes `/bfp` admin flow |
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
| `HazardBlock` | Abstract base for hazard blocks without FACING (symmetric). Real 3-state lifecycle: `HAZARDOUS` (developing danger) + `ON_FIRE` (actually burning, terminal) `BooleanProperty`s; subclasses implement `spawnHazardParticles`. `animateTick` calls it when `HAZARDOUS=true`, intensified (extra calls + FLAME particles) when `ON_FIRE=true` too. `useWithoutItem` is a base-class bare-hand "prevention" interaction: right-clicking a merely-hazardous (not yet on fire) prop resets it to safe (`preventMessage()`, overridable flavor text) — does nothing once `ON_FIRE=true` (too late for a bare-handed fix). Also declares the failure-consequence hooks (`failureDelayTicks`, `failureMessage`, `onHazardFailure`, plus `igniteAdjacent`/`igniteRadius` helpers) driven by `HazardManager`. |
| `HazardFacingBlock` | Abstract base for hazard blocks with FACING. Combines `HazardBlock`'s `HAZARDOUS`/`ON_FIRE` properties with `HorizontalFacingBlock`'s FACING logic; same intensified-particle `animateTick` and bare-hand prevention `useWithoutItem` as `HazardBlock`. Subclasses implement `shapeFor(Direction)` (use `byFacing(...)`) and `spawnHazardParticles`. Carries the same failure-consequence hooks as `HazardBlock`. |
| `HazardManager` | Drives the normal→hazardous→on-fire state machine for all 20 hazard prop blocks (plus the sawdust layer and `ComputerBlock`) inside an active FIRE-type arena, per `docs/md files/Items.md`. `scanHazardProps` runs once at session start (cached on `SimulationSession.hazardPositions`, mirroring `findComputersInCCS`); `tick()` (called from `SimulationManager.tickFireSession` every tick) randomly develops props to `HAZARDOUS=true` every 100 ticks (1-in-30 chance per prop) and advances a per-prop failure timer (`SimulationSession.hazardTimers`). When `failureDelayTicks()` elapses without being defused, `triggerFailure` now first flips the prop's `ON_FIRE=true` (a real terminal block state, not just an invisible transition) before calling the block's `onHazardFailure` — see Hazard Prop 3-State Log below. `woodshop_sawdust_layer`'s `ACCUMULATION` 0→5 ramp and 3×3 flash-ignite live directly in `HazardManager` (it has no `HAZARDOUS`/`ON_FIRE` property). `ComputerBlock` is similarly special-cased (`BURNING` instead of `HAZARDOUS`): `seedComputerTimers` lazily starts a 240-tick failure timer the first tick any of its 3 existing ignition triggers (flint & steel, session-start, periodic CCS spread) is observed to have set `BURNING=true`, so a computer left burning unattended escalates (`HazardBlock.igniteAdjacent`) like every other prop without duplicating its ignition logic. `HazardManager.defuse()` lets either extinguisher item reset a hazardous (or already on-fire) prop back to fully safe (`HAZARDOUS=false, ON_FIRE=false`) — wired into `FireExtinguisherItem`/`CO2ExtinguisherItem.extinguishAt`. `forceFailure` (used by `ReyesRoomManager.igniteHazard` and `HazardWandItem`) now sets `HAZARDOUS=true` first if a prop is still in its default state before failing it (fixed 2026-07-05 — it used to skip straight to `triggerFailure`, which only ever sets `ON_FIRE`, so `isHazardous()`/`defuse()` never recognized a Reyes-ignited prop as hazardous and an extinguisher spray against it silently did nothing). |
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
| `SimRoom` | Enum mapping player position to a named room (LSPU Library or CCS). Holds `CcsRoom` record + `CCS_UPPER_ROOMS` (9 rooms, Y −25 to −22) and `CCS_GROUND_ROOMS` (7 rooms, Y −32 to −29) — all F3-verified absolute world AABBs. `fromPos()` / `fromCCSPos()` used for telemetry room labels. |
| `AssemblyZone` | Static utility in `common/zones/`; defines assembly-zone AABBs for both buildings. `ZONE = AABB(30,-35,64,76,-28,82)` (LSPU Library, verified north of building) + `CCS_ZONE = AABB(76,-35,73,136,-28,90)` (open area immediately south of CCS Admin Building, Z:73–90). `spawnBorderParticles(level, isCCS)` and `isInside(pos, isCCS)` select the correct zone. Fires `assembly_area_reached` telemetry + ends simulation. |
| `TelemetryCsvWriter` | Writes per-tick and event rows to `run/telemetry/gameplay_logs_<YYYYMMDD>.csv` per telemetry contract v1.1 (§3). Also writes session-level sidecar `sessions_<YYYYMMDD>.csv` (§5) and one-time `map_metadata.json` on first server start. Buffered, synchronous, server-thread only. CSV event types: `session_start`, `move_tick` (10 Hz, x/y/z + hazard_distance), `extinguisher_use`, `fire_alarm_activate`, `assembly_area_reached`, `emergency_exit`, `door_open`, `session_end`. |
| `ExitZones` | Static record list in `common/zones/`; defines named AABB exit zones for both buildings. `ZONES` (LSPU Library, `main_exit = AABB(50,-34,93,54,-30,96)` tuned) + `CCS_ZONES` (`ccs_main_exit = AABB(95,-33,68,125,-29,74)` — centre of south wall). `find(pos, isCCS)` searches the correct list. Per-tick check in `SimulationManager` fires `emergency_exit` CSV event once per session crossing. |

### World Coordinates

- **Lobby**: origin `BlockPos(0, -33, 0)`, player spawn at `(8.8, -31, 8)`
- **Simulation arena**: `SIM_POS = BlockPos(30, -34, 83)`, player entry offset `+5.5, +2, +5.5`
- **SSC Building**: `SSC_POS = BlockPos(11, -33, 90)` (~19 blocks west of the library origin), placed with 1 CCW rotation
- **CCS Admin Building**: `CCS_POS = BlockPos(76, -34, 4)`, placed with 0 CCW rotations (`ccs_admin_building.schem`)
- **New Tutorial Building**: `AcademyBuildingManager.POS = BlockPos(-177, -34, 8)`, placed with 0 CCW rotations (`academy_building.schem`) — far west of every other structure, its own standalone footprint

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
| `academyIgniteDemoTicks` | 200 | **No longer used** (2026-07-06): the ignite demo has no timeout anymore — the fire only goes out after 5 s of accumulated rolling (`ReyesRoomManager.ROLL_REQUIRED_TICKS`). Config entry kept to avoid breaking existing config files. |
| `academyGoStopGraceTicks` | 30 | Reaction window after Officer Cruz calls STOP (1.5 s) — movement during it is never punished (reference position keeps re-anchoring while the player slides to a halt); only movement after it counts as a violation |
| `academyPassThreshold` | 70 | Minimum score (0–100) Capt. Morfe requires to certify a player after the Academy |

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
| `/bfp tutorial [player]` | Reset tutorial + teleport to lobby + re-init NPCs |
| `/bfp new_tutorial [player]` | **Activates** the Academy exactly like `/bfp tutorial` activates the old tutorial — wipes `AcademyProgress` back to a fresh start, clears every room manager's transient state, teleports to `AcademyBuildingManager.DEFAULT_VIEWPOINT` (Room 1, currently `officer_cruz`). Previously teleport-only; fixed so it actually "starts" a clean run instead of dropping the player back into their last phase. |
| `/bfp new_tutorial reset [player]` | Explicit, discoverable alias for the same reset-and-teleport the bare command above performs. |
| `/bfp new_tutorial <section> [player]` | Teleport to a named F3-captured reference viewpoint inside the new tutorial building (`AcademyBuildingManager.VIEWPOINTS`) — plain dev-navigation teleport, no reset. One literal subcommand per map entry; currently only `officer_cruz`. For tuning NPC placement in-game (see `docs/major_plan.md`-style "needs in-game F3 tuning" tasks). |
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
| `/sim_fire library` | Start FIRE simulation in the LSPU Library (gives ABC extinguisher) |
| `/sim_fire ccs` | Start FIRE simulation in the CCS Admin Building (gives CO2 extinguisher) |
| `/sim_earthquake library [magnitude]` | Start EARTHQUAKE simulation in the LSPU Library |
| `/sim_earthquake ccs [magnitude]` | Start EARTHQUAKE simulation in the CCS Admin Building |
| `/sim_status [player]` | Live snapshot: type, phase, time remaining, fires extinguished |
| `/sim_list` | List all active simulations across all players |
| `/sim_freeze [player]` | Pause simulation timer (effects continue) |
| `/sim_unfreeze [player]` | Resume a frozen timer |
| `/sim_time set <seconds>` | Set remaining simulation time |
| `/sim_time add <seconds>` | Add/subtract seconds from remaining time |
| `/get_co2_extinguisher` | Give CO2 extinguisher for Class C fires (any player) |
| `/get_wet_chemical_extinguisher` | Give yellow wet chemical extinguisher for Class F/K kitchen grease fires (any player) |
| `/item hazard <name>` | Give a hazard prop block item by registry name (tab-completes all 20 names). OP level 2. |
| `/item get <name>` | Give any custom BerongSMP item by registry name — extinguishers, hazard wand, computer, fire alarm, NPC spawners, furniture, hazards (tab-completes all `ALL_ITEM_MAP` names). Use `/item get hazard_wand` to grab the state-testing tool. OP level 2. |
| `/item kit` | Give one of every custom BerongSMP item at once, for quick full-scene dev testing. OP level 2. |

**Auto-hooks**: `TutorialManager.completeTutorial` → `SessionManager.onTutorialComplete` (records tutorial duration); `SimulationManager.endSimulation` → `SessionManager.onSimulationEnd` (records type/score/passed, closes row).

**Event log flush ordering**: `endSimulation` must capture `studentDbRowId` via `SessionManager.getActiveSession(uuid)` **before** calling `onSimulationEnd`, because `onSimulationEnd` calls `activeSessions.remove()` which would make the subsequent lookup return null and skip the `TursoClient.updateEventLog()` call. Fixed in commit `c5316d0`.

### Thread Safety

`SimulationManager.activeSessions` is a `ConcurrentHashMap`. `startSimulation` and `endSimulation` are `synchronized`. Tick-driven mutations are single-threaded via `ServerTickEvent.Post`. Network packet handling uses `context.enqueueWork()` to marshal HUD updates (including the new `intensity` field) onto the main client thread. `SimulationHud.intensity` is written on the main client thread and read from `ViewportEvent.ComputeCameraAngles`, which also fires on the main client thread — no extra synchronisation needed.

### Performance Notes

- **Fire-proximity scan memo** — `SimulationManager.nearestFireDistance` (the ~4,800-block hot scan) is memoised per `(game-tick, packed position)`. The several callers that need it for the same player on the same tick (PLAYER_TICK log, move_tick CSV row, `hazardDistance`) share one scan. Server-thread only, so the single-slot static cache needs no synchronisation. Scan radii live in named constants (`FIRE_SCAN_RADIUS_*`, `CCS_HAZARD_RADIUS_*`).
- **Earthquake PEAK scan** — `SimulationEffects.enqueuePeakDestructions` reads each scanned block state once (was 3×).
- **Turso writes** — run on a dedicated 2-thread daemon pool, not the common ForkJoinPool (see `TursoClient`).

### Repo Hygiene

The repo root no longer carries the decompiled vanilla `net/` reference dump, the `old_stuffs/` backup, or untitled dev screenshots — all removed and (where applicable) gitignored. Vanilla source lookups should use an external decompiler, not committed files.

### Shared Base Classes & Helper Conventions

Reuse these instead of re-copying boilerplate:

- **`item/AbstractExtinguisherItem`** — base for handheld extinguishers (pin gate, spray ray, durability, charge tooltip, `isKitchenHazard`/`warnWrongTool`). New extinguisher = subclass + `extinguishAt`/particles/`sprayPitch`/`onSprayResolved` hooks.
- **`block/HorizontalFacingBlock`** / **`block/FlammableFacingBlock`** — base for FACING-only furniture; subclass supplies only `shapeFor(Direction)` (use `byFacing(...)`). Use the flammable variant for wood/paper props.
- **Internal command/event guards** — `BfpAdminCommands.appendSessionRow`, `ItemCommands.requirePlayer`, `LobbyManager.gatesPassed` centralise patterns that were previously duplicated; extend these rather than re-inlining.
- **Logging** — keep per-entity/per-iteration logs at `debug`; reserve `info` for once-per-operation summaries (see `SchemLoader`).

### Custom Texture Assets (Hazard Props + Computer/Fire Alarm)

All block models are now **custom-texture models** — hand-drawn 16×16 PNGs under `textures/block/` instead of stretched vanilla textures. The 11 furniture blocks (`WhiteboardBlock`, `ToiletBlock`, `SinkBlock`, `DrawersBlock`, `ComputerTableBlock`, `ChairBlock`, `FilingCabinetBlock`, `LockerBlock`, `TrashCanBlock`, `BulletinBoardBlock`, `CeilingFanBlock`) moved off vanilla-texture reuse in the Furniture Visual Remediation Log below (`scripts/generate_furniture_textures.py`), joining `ComputerBlock`, `FireAlarmBlock`, and all 20 `common/hazard/*` blocks (`scripts/generate_hazard_textures.py`) — the same way `computer_block.json` uses `computer_case`/`computer_screen_off`/`computer_keyboard`/`computer_mouse` instead of vanilla blocks.

All 20 hazard prop textures are generated by **`scripts/generate_hazard_textures.py`** (Pillow/PIL, deterministic via a fixed RNG seed) — re-run it after editing to regenerate the full set:
```bash
python3 scripts/generate_hazard_textures.py
```
It produces two tiers of texture:
- **Per-object body textures** (`bin_plastic_gray`, `cardboard_box`, `pc_tower_case`, `locker_door_steel`, `stove_burner_metal`, `fryer_body_steel`, etc.) — one look per prop, normal vs. hazardous variants where the object physically changes (e.g. `lipo_foil_silver` → `lipo_foil_damaged`, `hood_steel_clean` → `hood_grease_dirty`).
- **Shared hazard-accent library**, reused across many blocks the same way `computer_block.json` shares `case`/`dark`: `hazard_ember_glow` (hot coil/burner), `hazard_warning_led` / `hazard_ok_led` (red/green indicator clusters), `hazard_spark_arc` / `hazard_spark_arc_green` (electric arcs), `hazard_smoke_stain`, `hazard_grease_stain`, `hazard_scorch_char`, `hazard_crumpled_paper`, `hazard_bare_copper`, `hazard_water_stain`, `hazard_glass_screen_off` / `hazard_glass_screen_glitch`.

**Known model-format gotcha**: a block model face's `"texture"` value **must** be a `"#variable"` reference resolved via that model's own `"textures"` map — a raw `"minecraft:block/xxx"` string directly in a face renders as the missing-texture (magenta/black) placeholder in-game, since Minecraft doesn't resolve namespaced IDs at the face level. (Found and fixed in `plastic_trash_bin_hazardous.json`'s `vape_glow` element.)

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
