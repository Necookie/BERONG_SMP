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

### Event Bus Duality

NeoForge uses two separate event buses — a core pattern throughout this codebase:

- **`modEventBus`** — mod lifecycle events (registration, client setup). Used in `BerongSMP` constructor for `DeferredRegister`, network payload registration, and `BuildCreativeModeTabContentsEvent`.
- **`NeoForge.EVENT_BUS`** — runtime game events (server tick, player join, block interact). Classes annotated with `@EventBusSubscriber` auto-register here.

### Tutorial Flow

Players must complete a safety tutorial before simulation buttons become active. Progress persists across disconnects via `TutorialSavedData` — **except** `QUAKE_DROP`/`QUAKE_COVER`/`QUAKE_HOLDON`, which `TutorialManager.onPlayerLogout` rolls back to `QUAKE_INTRO` on logout. Without that, `TutorialManager.tick` (unconditional, every online player, every tick, driven purely by the persisted stage) would resume sending the 1.5f shake prompt on its own 10-tick clock the instant the player reconnected, with no dialogue re-triggered — this was the actual cause of an "earthquake never stops after exiting and reloading" report (a separate, since-fixed instance of the same bug class also existed in the Academy's `academy.room3.SantosRoomManager`). `TutorialManager`'s transient maps (`holdOnTimers`/`extinguishCounts`/`dialogueSteps`) are cleared on the same logout hook — they are plain static fields that do **not** reset on their own for a same-session "Save and Quit to Title" (only a real JVM restart clears static state).

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
  → places all buildings (LSPU Library + SSC Building + CCS Admin Building) via BUILDINGS list
  → teleports player to a random valid position inside the target building (library for FIRE/EARTHQUAKE, CCS for CCS_FIRE/CCS_EARTHQUAKE); scans arena for solid-floor + 2-air-block-tall gaps, picks one at random; falls back to building centre if none found
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
           proximity; no direct damage, no blindness;
           HazardManager.tick every tick — develops placed hazard props (normal→hazardous, ~1-in-30 chance
           every 100 ticks) and advances their failure timers, igniting a structural fire per prop if left
           hazardous past its failureDelayTicks(); extinguishers can defuse a hazardous prop back to safe;
           ComputerBlock participates too (lazily-seeded 240-tick timer while BURNING=true, since it uses
           its own property instead of the shared HAZARDOUS flag — see Hazard Prop State Management Log)
  → (EARTHQUAKE) applyDuckCoverHold every tick — live version of the tutorial's scripted drill; while
           isCrouching() + solid block at blockPosition().above(2), grants a refreshed Resistance buff and,
           the first time 5s of continuous compliance is reached, a one-time chat congratulation + a
           duck_cover_hold telemetry event
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

### Player Safety Mechanics

Two real-world drill techniques are modeled as live gameplay, not just tutorial content:

- **Duck, Cover, Hold** — `SimulationManager.applyDuckCoverHold`, ticked every EARTHQUAKE/CCS_EARTHQUAKE tick (see Simulation Flow above). Reuses the exact `isCrouching()` + solid-block-`above(2)` detection already proven in `TutorialManager`'s scripted QUAKE_DROP/COVER/HOLDON sequence, but live: compliance grants a refreshed Resistance buff, and reaching 5s of continuous compliance fires a one-time chat message + `duck_cover_hold` telemetry event (`SimulationSession.duckCoverHoldTicks`/`duckCoverHoldAchieved`).
- **Drop and Roll + crawl** — pressing the "Drop and Roll" key (default `R`, `KeyMappings.DROP_AND_ROLL`) while on fire sends `DropAndRollPayload` (the mod's first serverbound network payload) to `DropAndRollManager.onDropAndRollRequest`, which knocks 30 ticks (1.5s) off the player's remaining fire ticks per press and opens a 100-tick (5s) "dropped" window during which `MobEffects.SLOWNESS` is continuously refreshed — a stand-in for crawling low instead of walking normally. Repeated presses both keep extinguishing and extend the window. Works globally (not gated to an active session), since a player can catch fire from any hazard. `DropAndRollManager.tick` runs from `SimulationManager.onServerTick` alongside `TutorialManager.tick`.
  - **Distribution-safety note**: `KeyMappings` holds a real client-only `KeyMapping` field, so it's registered from `BerongSMPClient`'s own constructor (dist-gated by `@Mod(dist=CLIENT)`), never from the common `BerongSMP` constructor — registering it there would crash a real dedicated server (`NoClassDefFoundError`) even though local `./gradlew runServer` dev testing wouldn't catch it, since dev classpaths merge client+server classes.
  - **Physical cover object**: `TableBlock` (see Key Classes below) is a real, single-block table a player can shelter next to. Since a real table is under a metre tall, it can never occupy the "solid block above the player's feet" cell that `DuckCoverHoldManager`'s original check looks for — so `DuckCoverHoldManager.hasNearbyTable` separately scans a 1-block radius around the player for a `TableBlock` while crouching and counts that as valid cover too. `TutorialManager`'s scripted QUAKE_COVER/HOLDON drill is untouched (still the original station-based above-the-feet check only).
  - **Crawling under the table**: a table's kneehole (under 1 block tall) is shorter than even vanilla's `Pose.CROUCHING` hitbox, so normal collision would stop a player at its edge before they could ever shrink into it. `DuckCoverHoldManager.allowCrawlUnderTable` pre-emptively forces `Pose.SWIMMING` (vanilla's own crawl-through-tight-gaps hitbox) whenever a crouching player is facing or beside a `TableBlock` (`facingOrBesideTable` — stricter than `hasNearbyTable`: adjacent or a short look-direction raycast only, not "somewhere nearby"), letting ordinary movement carry them into the kneehole. Sneak/shift itself is untouched; this only changes what fits under the player once they're already crouching near a table, and vanilla's own per-tick pose logic (`Player.updatePlayerPose`) takes back over seamlessly on the way back out.

### New Tutorial Building (Academy)

A second, **fully independent** tutorial — "the Academy" — lives in `new_tut_building1.0.schem`, placed at `NewTutBuildingManager.POS = BlockPos(-177,-34,8)`. It deliberately does not reuse or extend the old `tutorial/` package (`TutorialStage`/`TutorialManager`/`NpcDialogue`/`NpcRole`) — a single flat stage enum can't represent 4 independently-progressing NPC rooms, so it's a parallel system in its own `net.necookie.disastersim.academy` package instead. The old tutorial keeps functioning exactly as before and is still the only thing gating the fire/quake simulation buttons (`LobbyManager.gatesPassed`) — wiring the Academy into that gate is an explicit future step, not done yet. Full dialogue script, per-room coordinate tables, and a Mermaid flow diagram: `docs/new_tutorial_script.md`.

```
Room 1 — Officer Cruz (Movement School): BRIEFING (4 green-tile WASD walk — physical lime
  concrete floor tiles placed at runtime by NewTutBuildingManager.placeGreenMarks, since the
  schematic contains no green blocks; the next unhit one also gets the particle beacon) → MAZE →
  JUMP → GOSTOP_STAGE/GOSTOP_RUN (Officer Cruz calls GO/STOP on a random 3-6s cadence; moving past
  Config.ACADEMY_GOSTOP_GRACE_TICKS after a STOP call warps the player back to the staging line
  and counts a movement mistake) → DONE. MAZE and JUMP use **schem-verified waypoint chains**
  (`MAZE_WAYPOINTS` through the serpentine's wall gaps at X=-131/Z=25, X=-127/Z=38, X=-124/Z=25;
  `JUMP_WAYPOINTS` just past each 1-block hurdle row at X=-117/-114/-111) advanced per-player when
  within 2 blocks — both the compass needle and Cruz's escort target the current waypoint, never
  the far exit through a wall. A single Cruz (`CruzRoomManager.findCruz`, 3-tier fallback: cached
  direct ref → UUID lookup → bounded AABB scan tie-broken by proximity) physically escorts the
  player via `CustomNpcEntity.setEscorting(true)` + a periodic `getNavigation().moveTo(...)`
  re-issued every ~15 ticks. Navigation hardening: FOLLOW_RANGE 48 (default 16 also caps the
  pathfinding node budget — the old "gets lost" root cause), STEP_HEIGHT 1.1 (walks the hurdles),
  FloatGoal, door passage + explicit path budget set on escort start. **Stuck recovery**: 4
  consecutive no-progress escort cycles (~3s — path not created, isStuck(), or <0.25 blocks moved
  while >2 blocks from target) → `recoverCruz` poof-teleports her to the player's side with POOF
  particles at both ends. **Wandered-too-far chase (2026-07-05, retuned same day)**: if the
  escorted player strays more than 7 blocks (halved from an original 14 — felt unresponsive in
  practice) from Cruz's current position, `updateCruzEscort` abandons the phase's waypoint for that
  re-issue and walks straight toward the player instead (`clampToRoom1Bounds` keeps the chase
  target inside `ROOM1_BOUNDS` so it can never pull her outside the building), resuming the normal
  waypoint once they're close again. At the Go/Stop finish line specifically, `tickGoStopRun` also
  guarantees she's actually standing beside the player for the "you did it, go find Reyes" beat
  (`recoverCruz`'s snap-to-side poof if she isn't already close, rather than wherever the last
  15-tick escort re-issue left her), delivered as a real spoken Cruz line via `forceStartDialogue`
  (reusing `CRUZ_LINES.get(DONE)`) instead of a bare caption. **Escort-through-the-door hand-off
  (2026-07-05)**: `CruzPhase.DONE` used to immediately drop the player from escort selection the
  instant the Go/Stop finish line was crossed — well inside Room 1 — sending Cruz into
  `tickReturnHome` before the player had gone anywhere near actually leaving (read by the user as
  her "instantly banishing" herself). Parsed `new_tut_building1.0.schem`'s raw block data directly
  to find the real wall gap into Sgt. Reyes's room (world X=-162, verified — not guessed) and
  widened `ROOM1_BOUNDS` to that threshold; `DONE` now keeps counting as escortable (targeting the
  player's live position, same idiom as `GOSTOP_RUN`) for as long as the player is still physically
  inside that footprint, so Cruz walks them right up to the doorway before the normal gradual
  `tickReturnHome` walk-back takes over the instant they actually step through. `ESCORT_STUCK_
  CYCLES_MAX` bumped 4→6 so a normal walk across the wider corridor doesn't spuriously trip the
  stuck-recovery poof. **Faster idle look (2026-07-05)**:
  `CustomNpcEntity.updateGaze` now turns her head/body noticeably faster whenever she isn't
  escorting (`HEAD_TURN_SPEED_IDLE`/`BODY_TURN_SPEED_IDLE`, applies to every `CustomNpcEntity`, not
  just Cruz) — a stationary NPC reacts to the player right away instead of the slower ease used
  mid-walk. The jump phase's finish line is **directional** (`X >= JUMP_FINISH_X`
  (-106), east past the last hurdle) — leaving the zone backward into the maze no longer skips the
  player ahead. During GOSTOP_STAGE (the briefing) she waits at the staging line — her fixed
  0.6×1.8 hitbox can't crouch under the slabs' 1.5-block headroom — but during GOSTOP_RUN her
  escort target is **the player themselves**: each slab lane blocks her path and the stuck-recovery
  poofs her to the player's side, so she visibly keeps pace through the tunnel barrier by barrier.
  A STOP call gives a true **reaction window** (`academyGoStopGraceTicks`, default 30 = 1.5s): the
  reference position keeps re-anchoring for the whole window, so sliding to a halt is never
  punished — only movement after it, measured from where the player actually came to rest, counts
  as a violation. **She can never strand**:
  with nobody in an escortable phase, `tickReturnHome` walks her back to the briefing anchor
  (-153.5,-33,32.5) (stuck → poof-teleport home) and drops escort mode on arrival;
  `CruzRoomManager.resetCruz` (called by `/bfp new_tutorial [reset]` and Morfe's fail-reset)
  instantly snaps her there so a restarting trainee always finds her waiting; and a `ROOM1_BOUNDS`
  safety net poof-recovers her to the escorted player if she's ever outside Room 1's footprint.
  The duplicate second Cruz from the old two-NPC handoff design was **removed from the .schem file
  itself** (NBT-edited, 29 → 28 entities — confirmed by parsing the schematic directly: it now
  bakes in exactly one Officer Cruz, at the briefing anchor), but a stray copy kept reappearing at
  the old handoff spot `(-122,-33,49)` anyway — a disk-persisted leftover entity from a session
  predating that fix, sitting in a chunk that's merely block-loaded (from schematic placement) but
  never promoted to Minecraft's entity-ticking ring, since that requires an online player nearby.
  **A one-shot boot-time scan can never actually find her** (`getEntitiesOfClass` only sees
  entities the level has attached, which doesn't happen until her chunk activates) — that's why an
  earlier position-targeted boot-only fix didn't work. `NewTutBuildingManager.sweepStrayCruz`
  (a tiny single-chunk-sized AABB query around `(-122,-33,49)`) is now called every tick from
  `AcademyManager.tick()` instead, so she's discarded the exact tick her chunk activates — which
  only happens because a player walked close enough to see her, so in practice she never renders
  as encountered. `discardDuplicateCruz` still runs `sweepStrayCruz` once at boot too, then applies
  the original keep-nearest-anchor tie-break only when more than one Cruz still remains (a lone
  legitimate Cruz is never touched by it). The whole-map scan bounds remain wide for the same
  reason as before (future schematic re-save, accidental spawner copy, or another stray from a
  session predating either fix).

Room 2 — Sgt. Reyes (Fire Safety): gated on Cruz DONE. Teaches the full response ladder in order —
  **prevention → intervention → alarm → evacuation** — via `ReyesPhase`:
  `NOT_STARTED → PREVENTION_DEMO → TOOL_SELECTION → LIVE_FIRE_DEMO → ALARM_CHECKPOINT →
  EVACUATION_BRIEF → DONE`.
  - **Sync fix (2026-07-05):** `armPreventionHazard`/`igniteHazard` used to only run as the
    explanation dialogue's `onComplete` callback — meaning the prop didn't exist in the world until
    the full multi-line explanation finished playing several seconds later, so the player watched
    Reyes narrate a fire that wasn't there yet. Both now arm/ignite the prop **synchronously**, the
    instant a hazard's turn begins, before `forceStartDialogue` even starts narrating it;
    `checkAndHandlePrevention`/`checkAndHandleDefuse` were already tick-driven independent of
    dialogue completion, so this was a pure reordering fix, not a new mechanic.
  - **Extinguisher-doesn't-work fix (2026-07-05):** `HazardManager.forceFailure` (what
    `igniteHazard` calls) used to jump straight to `triggerFailure` without ever setting
    `HAZARDOUS=true` on a prop still in its default state — `triggerFailure` only ever sets
    `ON_FIRE`, and both `isHazardous()`/`defuse()` gate on `HAZARDOUS`, so an extinguisher spray
    against a Reyes-ignited hazard genuinely did nothing; the game never considered it hazardous in
    the first place. `forceFailure` now sets `HAZARDOUS=true` first when it wasn't already, restoring
    the invariant the normal `activate()` → timer → `triggerFailure` flow already maintained.
  - **Missing phase-entry announcements fix (2026-07-05):** `TOOL_SELECTION`'s "walk up to each
    extinguisher and left-click it" and `LIVE_FIRE_DEMO`'s "press the number key, hold right-click"
    `REYES_LINES` entries were never actually heard in normal play — both phase transitions are
    tick-driven (`tickPreventionDemo`/`tickToolSelection`), not `onInteract`-driven, so nothing ever
    played them unless the player happened to re-click Reyes on their own. Both transitions now
    `forceStartDialogue` their phase's instructional line the instant the phase begins.
  - `PREVENTION_DEMO`: the same 3 demo-hazard positions used later (`ReyesRoomManager.HAZARDS`:
    Class A archive boxes → electrical computer → kitchen grease pan) are set merely
    `HAZARDOUS=true` via `HazardManager.activate` (never actually ignited). Each hazard's turn
    plays `AcademyDialogue.REYES_PREVENTION_LINES[idx]` (the everyday habit that prevents it),
    then the player fixes it with a bare-hand right-click — `HazardBlock`/`HazardFacingBlock`'s new
    prevention interaction (see Hazard Prop 3-State Log below) — before the sequence advances to
    the next hazard, then to `TOOL_SELECTION`.
  - `TOOL_SELECTION` (inventory contains all 3 extinguisher items, picked up from wall item
    frames) → `LIVE_FIRE_DEMO`, the "intervention" phase, taught **sequentially** one hazard at a
    time in the same fixed order. Entering a hazard's turn plays its
    `AcademyDialogue.REYES_HAZARD_LINES[idx]` explanation (what's burning, which extinguisher, why)
    through the shared dialogue sequencer, alongside the now-already-armed prop; `igniteHazard`
    calls `HazardManager.forceFailure(level, null, pos, null)` (the same session-nullable entry
    point `HazardWandItem` uses — this now also flips the prop's new `ON_FIRE` state true, per the
    Hazard Prop 3-State Log). Using the wrong extinguisher doesn't just warn — `checkAndHandleDefuse`
    re-ignites the same prop immediately, so the player must get it right before the sequence
    advances (edge detection is per-player: `lastActive` is `Map<UUID, Map<BlockPos, Boolean>>` so
    two concurrent Room-2 players don't corrupt each other). After all 3, the scripted ignite-demo
    fires: the player is set alight and `tickIgniteDemo` continuously refreshes their fire ticks
    every tick (`Math.max(current, FIRE_REFRESH_TICKS)`) so the fire never burns out on its own — it
    only clears the instant `DropAndRollManager.isDropped(uuid)` is observed true (the player
    actually performed drop-and-roll). `Config.ACADEMY_IGNITE_DEMO_TICKS` (200 ticks/10s) is a
    safety-cap timeout for a player who never rolls, not the real burn duration →
    `advanceToAlarmCheckpoint` (prop/fire cleanup, phase → `ALARM_CHECKPOINT`).
  - `ALARM_CHECKPOINT`: Reyes explicitly teaches "the moment a fire starts, always ring the alarm
    first" and points the compass at the fire alarm at `ALARM_POS = (-143,-32,40)`. **Duplicate
    alarm fix (2026-07-05):** the schematic actually already bakes a
    `berongsmp:fire_alarm[activated=false,facing=south]` in at this exact spot (confirmed by
    parsing `new_tut_building1.0.schem`'s raw block data) — a since-removed
    `NewTutBuildingManager.placeFireAlarm` was placing a second one one block below every boot,
    which was the "two fire alarms stacked on top of each other" report. `ALARM_POS` now points at
    the schem's real alarm and nothing places a second one. `FireAlarmBlock.useWithoutItem` calls
    `ReyesRoomManager.tryHandleAlarmPress` first (returns true/short-circuits if handled), so the
    Academy's own alarm-ringing logic works there without touching the block's existing
    `SimulationSession`-gated old-tutorial behavior at all. Returning to Reyes while it's ringing
    (`onInteract`'s special-cased branch, not the generic dialogue dispatch) stops the alarm and
    advances straight into `EVACUATION_BRIEF`'s dialogue.
  - `EVACUATION_BRIEF`: a short final "once that alarm rings, you evacuate — don't go back for
    anything" briefing; its completion (`finishRoom`) is the room's true end → `DONE`, pointing at
    Sgt. Santos.
  **Nav-aid parity (2026-07-05):** every phase now pairs `AcademyVisuals.highlightBlocks`'s green
  beacon with `setCompassTarget`'s needle (the same pairing Room 1 already used for its waypoints;
  previously only `ALARM_CHECKPOINT` had a compass, and it had no beacon) — `TOOL_SELECTION` points
  at the next not-yet-collected extinguisher frame (`nextUncollectedFrame`), `PREVENTION_DEMO`/
  `LIVE_FIRE_DEMO` point at the current hazard the instant it's armed (`pointAtHazard`), and
  `ALARM_CHECKPOINT` now beacons the alarm itself, not just the compass.
  **Prop cleanup** (`cleanupHazardProps`): the 3 code-spawned props + any leftover vanilla fire in
  Room 2's box are removed when `LIVE_FIRE_DEMO` finishes, on a mid-demo logout/death (which also
  rolls the phase back to a clean re-entry point — `TOOL_SELECTION` from `LIVE_FIRE_DEMO`,
  `NOT_STARTED` from `PREVENTION_DEMO`), and on a Capt. Morfe fail-reset. A mid-`ALARM_CHECKPOINT`
  logout with the alarm already ringing turns it back off. **Frame restock**
  (`restockExtinguisherFrames`): the 3 glow item frames on the Tool Selection Wall are refilled
  (and respawned if missing) on both reset paths and after building placement — the schematic only
  restores them on a full reboot, so a mid-session reset previously left them empty and made
  TOOL_SELECTION uncompletable.

Room 3 — Sgt. Santos (Earthquake Drill): gated on Reyes DONE. PRE_DRILL highlights the safe-zone
  TableBlock row (-170,-33,29 to -167,-33,29) green every 5 ticks (AcademyVisuals.highlightBlocks,
  a per-block wireframe adaptation of AssemblyZone.spawnBorderParticles's particle-loop idiom —
  there's no glow/highlight system elsewhere in the mod) and, since 2026-07-05, also points the
  compass at `TABLE_ROW_CENTER` (`setCompassTarget` — previously beacon-only, unlike every other
  guided objective in the Academy). After a short delay, triggers the quake
  (own shake-intensity caption, same CameraShake mechanism TutorialHud already uses) → QUAKE_ACTIVE
  reads compliance from a **room-local, table-scoped streak** (`SantosRoomManager.tableHoldTicks`)
  instead of `DuckCoverHoldManager.ticksHeld` directly: the latter is a *global* crouch+cover
  streak that accumulates anywhere in the world, so reading it directly previously let a player
  build the full streak elsewhere and merely step near the table row for one tick to pass
  instantly. `tableHoldTicks` only increments on ticks where the player is genuinely near
  `TABLE_ROW` *and* `DuckCoverHoldManager.isCompliant` is true that tick (resets to zero
  otherwise), so completion can only be earned by actually holding at Santos's table. `tickPreDrill`
  also seeds `preDrillStartTick` via `computeIfAbsent` (previously `getOrDefault`, which never wrote
  the fallback back — a stale/missing entry after an ungraceful shutdown mid-drill could never
  reach the 60-tick threshold and stalled the drill forever) → DONE, pointing at Capt. Morfe.
  `SANTOS_LINES` now also teaches real earthquake safety measures (stay clear of windows/glass and
  unsecured heavy furniture, no running outside mid-shake, no elevators, expect aftershocks)
  alongside the Drop-Cover-Hold-On core.

Room 4 — Capt. Morfe (Evaluation): gated on Santos DONE. Morfe speaks through the same timed
  sequencer as the other instructors now — static `AcademyDialogue.MORFE_LINES` (greeting +
  already-certified) play first; the greeting's onComplete runs `AcademyScoring.evaluate` (fresh
  0-100 rubric: 25 points each for movement mistakes, fire correct/wrong ratio, drop-and-roll
  performed, quake compliant — pattern-cloned from SimulationFeedback's threshold-driven scoring
  style), prints the score (+ weak areas on a fail), then plays `MORFE_PASS_LINES` or
  `MORFE_FAIL_LINES`. Pass → certified, no further gating changes. Fail → the reset (full
  AcademyProgress wipe + Reyes prop cleanup + teleport back to Room 1's briefing zone, where a
  Cruz welcome-back prompt greets them) fires from the fail sequence's onComplete — only after the
  player has read Morfe's send-off where they stand.

World-space navigation, "compass" style ("follow the green floor arrows", said literally in
  several lines): a real client-rendered HUD needle (`client.AcademyCompassHud`), not particles —
  a concave 4-point dart (not a plain triangle — reads as a distinct compass needle) drawn over a
  fixed, non-rotating dark circular backdrop plate at the top-center of the screen, continuously
  rotated to point at the current objective, recomputed from the local player's own exact
  position/view angle every render frame (so it's perfectly smooth regardless of tick rate, unlike
  the original particle-based versions it replaced — a 10-tick dashed trail, then a 20Hz
  particle-arrowhead). The server only syncs *where* the target is via `AcademyCompassPayload`;
  `AcademyVisuals.setCompassTarget` dedupes so a packet is only actually sent when the target
  changes (safe to call every tick from whichever room is guiding the player) — `target == null`
  hides the needle, used whenever a room's "point at the next NPC" objective has already been
  reached so the arrow doesn't linger stale. Cruz's Room 1 also calls `highlightBlocks` on the
  NEXT unhit WASD tile every 5 ticks (the other tiles stay visible as physical lime concrete),
  pairing the beacon with the compass needle pointing at it. `highlightBlocks` itself (also used
  for Santos's safe-zone table row) draws a **beacon-style marker**: 4 corner posts framing the
  tile, an 8-point ring breathing above it (radius pulses with game time), a beam column of fading
  `DustParticleOptions` rising ~3 blocks (spottable over furniture from across the room), and a
  bright END_ROD sparkle at the beam top — the third design iteration (HAPPY_VILLAGER swirl → flat
  dust wireframe → this), ~31 particles/block/call. Takes an optional color argument so future
  red/yellow markers reuse the exact same routine.

Dialogue is a timed auto-advancing sequence, not click-per-line: one right-click starts a phase's
  whole line sequence via `AcademyManager.startOrAdvanceDialogue`; each line auto-advances after a
  word-count-based reading-pace delay (`tickDialogues`, clamped ~3-10s), and clicking again while
  the SAME sequence is playing skips immediately to the next line. A request for a *different*
  sequence while one is already active is now **ignored, not clobbered** — re-clicking Sgt. Reyes
  while her tick-auto-triggered per-hazard explanation was still on screen used to silently
  overwrite that session (and its `onComplete`, which is what actually ignites the hazard),
  permanently stranding Room 2 with nothing ever catching fire. The one call site that must still
  start regardless of what's playing — the hazard-explanation auto-trigger itself, since it's
  driven by tick-order not by a click — uses the new `AcademyManager.forceStartDialogue` instead
  (safe there specifically because anything it could be clobbering is a re-click reminder with a
  no-op `onComplete`). `advanceSession` also clears the caption the instant a sequence naturally
  finishes (`sendPrompt(player, "")`, immediately overwritten if `onComplete` has something new to
  say) — previously the last line stayed glued to the screen forever once a short conversation
  ended, until some unrelated prompt happened to overwrite it. `AcademyManager.cancelDialogue`
  drops a player's in-flight session without firing its `onComplete` — used at the two spots that
  mutate phase state out from under a possibly-active sequence (Cruz's Go/Stop violation revert,
  Morfe's fail-path reset) — plus a `PlayerEvent.PlayerLoggedOutEvent` hook that cancels dialogue
  and clears every room manager's transient per-player maps. Each room's periodic "idle nudge"
  reminders check `AcademyManager.isDialogueActive(uuid)` before firing, so a coincidental idle
  nudge can no longer stomp an in-progress dialogue line's caption while its voice line may still
  be playing. **Caption auto-clear (2026-07-05):** every non-dialogue-sequence caption (idle
  nudges, phase banners, Go/Stop calls) used to have no expiry at all — the only place that ever
  sent an explicit clear was `advanceSession` finishing a timed dialogue sequence, so walking away
  right after an idle nudge left it glued to the screen forever. `AcademyStatusPayload` now carries
  a `displayTicks` duration (computed in `AcademyManager.sendPrompt` from the same word-count
  reading-pace formula as dialogue lines, `ticksFor`, plus a grace window); `AcademyHud` tracks
  `promptExpiresAtMillis` client-side and clears its own prompt once past it — no extra server
  round trip, and dialogue sequences are unaffected since they're overwritten well before expiry.

**Logout safety net** — a player who disconnects mid-effect doesn't just lose transient timers, they
  can end up with the effect itself silently resuming (or literally still burning) on reconnect:
  `SantosRoomManager.clearPlayer` rolls `PRE_DRILL`/`QUAKE_ACTIVE` back to `NOT_STARTED` on logout
  (the persisted phase alone would otherwise re-enter the earthquake drill's tick loop the instant
  they reconnect, with no dialogue re-triggered); `ReyesRoomManager.clearPlayer` clears the player's
  actual fire if the scripted ignite demo was active (vanilla persists remaining fire ticks in the
  player's own save data, so without this they'd rejoin still on fire). Separately, every HUD's
  caption/shake-intensity/compass state (`SimulationHud`/`TutorialHud`/`AcademyHud`/
  `AcademyCompassHud`) is reset client-side on `ClientPlayerNetworkEvent.LoggingOut`/`LoggingIn`
  (`client.ClientEvents`) — those are plain static fields that otherwise survive for the life of the
  client JVM across a "Save and Quit to Title", which was the actual root cause of "the earthquake
  is still going after exiting and reloading the world" (a stale nonzero shake intensity with
  nothing left to overwrite it).

**Guardrails** (`academy/AcademyGuardrails.java`, @EventBusSubscriber): (1) block break/place
  inside BUILDING_BOUNDS (AABB(-178,-40,7,-94,-20,86)) cancelled for non-admins (OP 2+ or /bfp
  bypass are exempt) with a throttled friendly caption — deliberate side effect: punching fire out
  by hand is also cancelled, so extinguishers (setBlock, no break event) are the only defuse,
  matching what Room 2 teaches; item-frame pickup (entity attack) unaffected. (2) tickRescue
  (every 20 ticks from AcademyManager.tick): a non-admin player mid-tutorial (started Room 1, not
  yet certified) outside the bounds or below Y=-36 teleports to currentRoomAnchor (the furthest
  room reached). (3) onPlayerRespawn: dying mid-tutorial = same rollback as logout
  (clearTransientState) + teleport to currentRoomAnchor instead of world spawn.
```

**Key classes**: `AcademyManager` (the single `PlayerInteractEvent.EntityInteract` handler for
`CustomNpcEntity` — none existed anywhere else in the mod; dispatches by `NpcType`, ignores the
schematic's 5 other decorative NPCs) + the shared dialogue sequencer
(`startOrAdvanceDialogue`/`tickDialogues`/`cancelDialogue`, see above) and
`sendPrompt`/`playNpcSound` helpers; `AcademyProgress`/`AcademySavedData` (per-player persisted
state — 4 phases + Capt. Morfe's scoring inputs, `SavedData`+`Codec` pattern-cloned from
`TutorialSavedData` but storing a compound record instead of one enum; all mutation goes through
`AcademySavedData.mutate` so `setDirty()` is never forgotten); `AcademyDialogue`
(Cruz/Reyes/Santos/Morfe static line content — non-gamer voice, exact key names in §e — plus
`REYES_HAZARD_LINES` per-hazard explanations and `MORFE_LINES`/`MORFE_PASS_LINES`/
`MORFE_FAIL_LINES`, transcribed from `docs/new_tutorial_script.md`);
`AcademyGuardrails` (block protection + out-of-bounds rescue + death/respawn recovery — see
Guardrails above); `AcademyStatusPayload`/`AcademyHud` (own caption channel, pattern-cloned from
`TutorialStatusPayload`/`TutorialHud`, deferring to either if already showing);
`AcademyCompassPayload`/`client.AcademyCompassHud` (own channel/HUD pair for the client-rendered
compass needle — see "World-space navigation" above);
`room1.CruzRoomManager`/`room2.ReyesRoomManager`/`room3.SantosRoomManager`/`room4.MorfeRoomManager`
+ `room4.AcademyScoring`. `AcademyManager.tick` is hooked into `SimulationManager.onServerTick`
alongside the existing `TutorialManager`/`DropAndRollManager`/`DuckCoverHoldManager` calls.

All Academy coordinates are now **schematic-verified** (the `.schem` was parsed directly): building
position, all 4 NPC anchors, the Room 3 table row, the maze wall gaps, the jump hurdles, the
Go/Stop tunnel slabs, and the 4 WASD mark cells (which had no green blocks in the schematic at all
— they're placed as lime concrete at runtime, see `NewTutBuildingManager.placeGreenMarks`).

### Key Classes

| Class | Responsibility |
|---|---|
| `BerongSMP` | Mod entry point, item/block registration, server startup init. Three creative tabs: `SIM_TAB` (sim_tab — extinguishers, computer, fire alarm), `FURN_TAB` (furn_tab — all 11 furniture blocks), and `HAZARD_TAB` (hazards_tab — all 20 hazard prop blocks, icon = daisy_chain_extension). `HAZARD_ITEM_MAP` (LinkedHashMap) keeps hazard items in insertion order for the tab and `/item hazard` command. `ALL_ITEM_MAP` (LinkedHashMap, superset including `HAZARD_ITEM_MAP`) covers every custom item (extinguishers, hazard wand, computer/fire alarm, NPC spawners, furniture, hazards) for `/item get` and `/item kit`. |
| `SimulationManager` | Session registry (`ConcurrentHashMap<UUID, SimulationSession>`), tick driver, event handlers for tick/respawn/logout |
| `SimulationSession` | Per-player mutable state: timer ticks, disaster type, fires extinguished count, earthquake epicenter/phase/cascade queue/magnitude/aftershockCount/aftershockMagnitudeScale; `arenaOrigin/spanX/spanZ/height` set by SimulationManager to target the correct building |
| `SimulationSession.EarthquakePhase` | Inner enum: `RUMBLE → PEAK → AFTERSHOCK(×2–4) → END`; AFTERSHOCK loops with a random magnitude scale before advancing to END |
| `SimulationEffects` | World mutation: fire placement + smoke particles (14×, wide plume) + proximity nausea/air-drain; phase-aware earthquake (RUMBLE/PEAK/AFTERSHOCK helpers + cascade drain + `breakOrDebris` for falling debris with 8× damage multiplier) |
| `LobbyManager` | Lobby NBT placement, button discovery (sorted by Z: lower Z = fire, higher Z = quake), login/button-click handlers |
| `StructurePlacer` | Interface for placing a structure at a `BlockPos`; implemented by both loaders below |
| `SimulationStructureLoader` | Implements `StructurePlacer`; wraps `StructureTemplateManager` for `.nbt` files |
| `SchemLoader` | Implements `StructurePlacer`; parses Sponge Schematic v2/v3 `.schem` files, supports 0–3 CCW 90° rotations (rotates offsets and block states), places blocks, and spawns entities from the `Entities` tag — including modded mobs like `berongsmp:custom_npc`, not just vanilla decoration entities. Any pre-existing non-player entity in the placement footprint is discarded before re-placing to prevent duplicates (broadened from an item-frame-only check once schematics started baking in mobs/armor stands too — session restores never touch players, since they're explicitly excluded). **Item frame placement invariant (MC 26.x):** Sponge v3 top-level `Pos` = the entity's own AIR block (not the wall). `Data.Facing` = OUTWARD direction (frame face toward viewer, no `.getOpposite()` needed). `ItemFrame(level, pos, direction)` takes the entity's own block as `pos`; the wall is `pos.relative(direction.getOpposite())` — handled by a dedicated `spawnItemFrame` path since item frames need this wall-relative math the generic path doesn't do. **Generic entity deserialization gotcha (found while loading `new_tut_building1.0.schem`):** Sponge v3 nests an entity's *real* Minecraft save data under a `Data` sub-compound — `Id`/`Pos` are Sponge-level siblings, not part of it. Deserializing the raw entity tag directly (as this used to) left every actual field invisible to `EntityType.create`, so `CustomNpcEntity` silently read a missing `NpcType` and fell back to its default role for every copy. Fixed by merging `Data` into a fresh root before deserializing. That same root also needs `block_pos` (used by `BlockAttachedEntity` subclasses like `Painting`, checked with a 16-block sanity radius against the entity's real position) and any `facing`/`Facing` byte re-derived/rotated — left stale, `block_pos` still pointed at the original copy location, failed the sanity check, and vanilla logged "Block-attached entity at invalid position" while the entity failed to attach. |
| `NewTutBuildingManager` | Places `new_tut_building1.0.schem` (a WorldEdit `//copy -e` capture) at the fixed `POS = BlockPos(-177, -34, 8)` via `SchemLoader`, 0 rotations. The schematic bakes in its own 10 `berongsmp:custom_npc` NPCs (the duplicate second Officer Cruz at the old two-NPC-handoff spot was NBT-edited out of the .schem itself — 29 → 28 entities), 10 gear-display armor stands, item frames/glow item frames, and a painting — unlike `TutorialLobbyManager` (structure + hardcoded-offset NPCs as two separate passes), everything here comes from one `SchemLoader.place` call. Called from `onServerStarted`, not `onServerStarting`, for the same reason `TutorialLobbyManager.initNpcs` is: entity chunk storage must be fully loaded first, or freshly-spawned entities can collide with same-UUID copies the previous server run persisted to disk. Also owns `VIEWPOINTS` (`Map<String, Viewpoint>`) — named F3-captured admin teleport targets inside the building, one per named station, surfaced via `/bfp new_tutorial <name>`. `sweepStrayCruz` (public, tiny single-chunk AABB query around `(-122,-33,49)`) is called every tick from `AcademyManager.tick()` — not just once at boot — since a disk-persisted stray entity there only ever becomes visible to `getEntitiesOfClass` once a player walks close enough for that chunk to reach Minecraft's entity-ticking ring, so a boot-only scan could never actually catch her. `discardDuplicateCruz` calls `sweepStrayCruz` once at boot too, then — only if more than one legitimate `OFFICER_CRUZ` copy still remains — keeps the one nearest the briefing anchor (a lone survivor is never touched). `placeGreenMarks` is a permanent per-placement fixup: swaps the 4 floor blocks under `CruzRoomManager.GREEN_MARKS` to lime concrete (the schematic has no green blocks in the briefing zone), re-run after every placement since `SchemLoader` restores the original floor each boot. (A `placeFireAlarm` method used to also place a `FireAlarmBlock` for Sgt. Reyes's alarm checkpoint here — removed 2026-07-05 once parsing the schem revealed it already bakes one in at that exact spot, see Room 2 above.) |
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
| `AssemblyZone` | Static utility in `world/`; defines assembly-zone AABBs for both buildings. `ZONE = AABB(30,-35,64,76,-28,82)` (LSPU Library, verified north of building) + `CCS_ZONE = AABB(76,-35,73,136,-28,90)` (open area immediately south of CCS Admin Building, Z:73–90). `spawnBorderParticles(level, isCCS)` and `isInside(pos, isCCS)` select the correct zone. Fires `assembly_area_reached` telemetry + ends simulation. |
| `TelemetryCsvWriter` | Writes per-tick and event rows to `run/telemetry/gameplay_logs_<YYYYMMDD>.csv` per telemetry contract v1.1 (§3). Also writes session-level sidecar `sessions_<YYYYMMDD>.csv` (§5) and one-time `map_metadata.json` on first server start. Buffered, synchronous, server-thread only. CSV event types: `session_start`, `move_tick` (10 Hz, x/y/z + hazard_distance), `extinguisher_use`, `fire_alarm_activate`, `assembly_area_reached`, `emergency_exit`, `door_open`, `session_end`. |
| `ExitZones` | Static record list in `world/`; defines named AABB exit zones for both buildings. `ZONES` (LSPU Library, `main_exit = AABB(50,-34,93,54,-30,96)` tuned) + `CCS_ZONES` (`ccs_main_exit = AABB(95,-33,68,125,-29,74)` — centre of south wall). `find(pos, isCCS)` searches the correct list. Per-tick check in `SimulationManager` fires `emergency_exit` CSV event once per session crossing. |

### World Coordinates

- **Lobby**: origin `BlockPos(0, -33, 0)`, player spawn at `(8.8, -31, 8)`
- **Simulation arena**: `SIM_POS = BlockPos(30, -34, 83)`, player entry offset `+5.5, +2, +5.5`
- **SSC Building**: `SSC_POS = BlockPos(11, -33, 90)` (~19 blocks west of the library origin), placed with 1 CCW rotation
- **CCS Admin Building**: `CCS_POS = BlockPos(76, -34, 4)`, placed with 0 CCW rotations (`ccs_admin_building2.4.schem`)
- **New Tutorial Building**: `NewTutBuildingManager.POS = BlockPos(-177, -34, 8)`, placed with 0 CCW rotations (`new_tut_building1.0.schem`) — far west of every other structure, its own standalone footprint

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
- `lspulibrarymain.nbt` — simulation arena (NBT, placed/restored each session)
- `ssc_building.schem` — SSC building adjacent to the arena (Sponge Schematic v3, placed/restored each session with 1 CCW rotation)
- `ccs_admin_building2.4.schem` — CCS Admin building (Sponge Schematic v3, placed/restored each session with 0 CCW rotations)

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
| `academyIgniteDemoTicks` | 200 | Safety-cap timeout (10 s) for Sgt. Reyes's scripted drop-and-roll demo — the fire itself stays lit until the player actually rolls; this only forces it out if they never do |
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
| `/bfp new_tutorial [player]` | **Activates** the Academy exactly like `/bfp tutorial` activates the old tutorial — wipes `AcademyProgress` back to a fresh start, clears every room manager's transient state, teleports to `NewTutBuildingManager.DEFAULT_VIEWPOINT` (Room 1, currently `officer_cruz`). Previously teleport-only; fixed so it actually "starts" a clean run instead of dropping the player back into their last phase. |
| `/bfp new_tutorial reset [player]` | Explicit, discoverable alias for the same reset-and-teleport the bare command above performs. |
| `/bfp new_tutorial <section> [player]` | Teleport to a named F3-captured reference viewpoint inside the new tutorial building (`NewTutBuildingManager.VIEWPOINTS`) — plain dev-navigation teleport, no reset. One literal subcommand per map entry; currently only `officer_cruz`. For tuning NPC placement in-game (see `docs/major_plan.md`-style "needs in-game F3 tuning" tasks). |
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

All block models are now **custom-texture models** — hand-drawn 16×16 PNGs under `textures/block/` instead of stretched vanilla textures. The 11 furniture blocks (`WhiteboardBlock`, `ToiletBlock`, `SinkBlock`, `DrawersBlock`, `ComputerTableBlock`, `ChairBlock`, `FilingCabinetBlock`, `LockerBlock`, `TrashCanBlock`, `BulletinBoardBlock`, `CeilingFanBlock`) moved off vanilla-texture reuse in the Furniture Visual Remediation Log below (`scripts/generate_furniture_textures.py`), joining `ComputerBlock`, `FireAlarmBlock`, and all 20 `block/hazard/*` blocks (`scripts/generate_hazard_textures.py`) — the same way `computer_block.json` uses `computer_case`/`computer_screen_off`/`computer_keyboard`/`computer_mouse` instead of vanilla blocks.

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

Tracks fixes applied from the 2026-06-23 health check report.

| # | Severity | Item | Status | Commit |
|---|---|---|---|---|
| C-1a | 🔴 | Assembly zone force-field on wrong face (Z+ instead of Z-) | ✅ Done | `cf57f50` |
| C-1b | 🔴 | AssemblyZone/ExitZones placeholder coordinates | ✅ Done | AssemblyZone AABB confirmed correct (same AABB drives force-field + detection); main_exit tuned from F3 (`defebb0`); side/rear exits deferred until additional buildings added |
| C-2  | 🔴 | Default BFP PIN was hardcoded `"1234"` | ✅ Done | (this commit) |
| W-1  | 🟡 | ModCommands.java monolith (807 lines) | ✅ Done | (this commit) |
| W-2  | 🟡 | onServerTick() mixes fire/quake/telemetry/HUD | ✅ Done | (this commit) |
| W-3  | 🟡 | Silent `catch (Exception ignored)` in TursoClient | ✅ Done | (this commit) |
| W-4  | 🟡 | Zero unit test coverage | ✅ Done | (this commit) |
| L-1  | 🟢 | Telemetry metadata hardcoded; coords out of sync with code | ✅ Done | (this commit) |
| L-2  | 🟢 | Tutorial station offsets placeholder | ⏳ Blocked (needs in-game F3 tuning) | — |
| L-3  | 🟢 | No rate-limit on /bfp login PIN | ✅ Done | (this commit) |
| L-4  | 🟢 | Turso URL-set/token-missing warning | ✅ Done | W-3 commit |

---

## Telemetry Gap Remediation Log

Tracks fixes applied from the 2026-06-23 telemetry gap analysis (ranked Critical → Low).

| # | Priority | Item | Status | Notes |
|---|---|---|---|---|
| T-1 | 🔴 Critical | `fire_alarm_activate` not written to CSV | ✅ Done | Added `TelemetryCsvWriter.writeRow()` in `FireAlarmBlock.useWithoutItem()` alongside existing `session.logger.log()` |
| T-2 | 🔴 Critical | `assembly_area_reached` not written to CSV | ✅ Done | Added `TelemetryCsvWriter.writeRow()` in `AssemblyZone.onPlayerArrived()` alongside existing `session.logger.log()` |
| T-3 | 🟠 High | `session_end` hazard_distance hardcoded to `99.0` | ✅ Done | Replaced with `hazardDistance(session, level, playerForCsv)` in `SimulationManager.endSimulation()` |
| T-4 | 🟠 High | CO2ExtinguisherItem emits no telemetry | ✅ Done | Added `extinguisher_use` row with `nearby_player_count` in `CO2ExtinguisherItem.sprayServer()`; `extinguishAt` now returns `boolean`; added `countNearbyPlayers` helper |
| T-5 | 🟡 Medium | AssemblyZone coordinates are placeholder | ✅ Done | Library `AABB(30,-35,64,76,-28,82)` north of building (verified); CCS `AABB(76,-35,73,136,-28,90)` outside south wall (Z:73–90) |
| T-6 | 🟡 Medium | ExitZones coordinates are placeholder | ✅ Done | `main_exit AABB(50,-34,93,54,-30,96)` tuned; `ccs_main_exit AABB(95,-33,68,125,-29,74)` set (south-wall centre) |
| T-7 | 🟡 Medium | `fire_alarm_positions` in map_metadata.json was empty `[]` | ✅ Done | Added `TelemetryCsvWriter.scanAndRegisterFireAlarms()` which scans the arena for `FireAlarmBlock` after first structure placement; rewrites `map_metadata.json` with discovered positions |
| T-8 | 🟢 Low | `mod_version` missing from sessions CSV | ✅ Done | Added `mod_version` column to `sessions_*.csv` header and rows; resolved via `ModList.get()` + cached in `TelemetryCsvWriter` |
| T-9 | 🟢 Low | `extinguisher_use` throttled to one per 40 ticks | ✅ Done | Decoupled `resetExtinguishEventPending()` from `cleanupFireOutsideBounds()`; now resets every 20 ticks (1 s window) for better temporal resolution |
| T-10 | 🔴 Critical | CSV `move` event misnamed — ML pipeline expects `move_tick` | ✅ Done | Renamed `"move"` → `"move_tick"` in `SimulationManager.tickTelemetry()` |
| T-11 | 🟠 High | CO2 extinguisher not in Turso `event_log` | ✅ Done | Added `session.logger.log("extinguisher_use", ...)` to `CO2ExtinguisherItem.sprayServer()` every 20 damage ticks; dashboard `extractRubricSignals` now counts both `EXT_SPRAY` and `extinguisher_use` |
| T-12 | 🟢 Low | `SIM_START` event missing spawn position | ✅ Done | Moved `SIM_START` logger call to after `spawnPos` resolution; now includes `x/y/z` so dashboard event timeline shows where the player spawned |
| T-13 | 🟠 High | `extinguisher_positions: []` always empty in `map_metadata.json` | ✅ Done | Added static nominal positions (`LIBRARY_EXTINGUISHER_POS`, `CCS_EXTINGUISHER_POS`) in `TelemetryCsvWriter`; `map_metadata.json` now written on every server start (not just first) |
| T-14 | 🟡 Medium | Contract doc used `move` but mod emitted `move_tick` | ✅ Done | Updated `telemetry_contract.md` §3, §4, §6, §7 to say `move_tick` throughout |
| T-15 | 🟡 Medium | CCS scenario types (`ccs_fire`/`ccs_earthquake`) missing from contract and DB | ✅ Done | Updated `telemetry_contract.md` §2/§3/§5; `endSimulation` stores `session.getState().name()` so CCS sessions write `CCS_FIRE`/`CCS_EARTHQUAKE` to Turso; dashboard `simulation_type` type and SQL aggregations updated |
| T-16 | 🟢 Low | New `duck_cover_hold` event type (live duck/cover/hold drill) not in contract/dashboard | ⏳ Pending | Emitted by `SimulationManager.applyDuckCoverHold` via `session.logger.log()` + `TelemetryCsvWriter.writeRow()`, same shape as `fire_alarm_activate` (x/y/z, no `hazard_distance`). Dashboard session-timeline event rendering needs to account for it — tracked as a follow-up in the `BERONG_SMP_WEB` repo. |

---

## Hazard Prop Visual Remediation Log

Tracks fixes applied after an in-game screenshot showed hazard prop blocks rendering with a missing-texture (magenta/black) face and looking like flat colored boxes rather than recognizable objects.

| # | Item | Status | Notes |
|---|---|---|---|
| H-1 | `plastic_trash_bin_hazardous.json` `vape_glow` face used a raw `"minecraft:block/redstone_lamp_on"` string instead of a `"#variable"` reference | ✅ Done | Model faces can only resolve `#name` references — this rendered as the missing-texture placeholder in-game. Routed through a proper texture variable. |
| H-2 | All 20 hazard blocks stretched plain vanilla concrete/andesite textures over cuboids, reading as abstract colored boxes | ✅ Done | Added `scripts/generate_hazard_textures.py` (53 hand-drawn 16×16 PNGs) and rewired every block/hazardous model pair to use them, matching the custom-texture quality bar set by `computer_block.json` / `fire_alarm_bell.png`. |
| H-3 | Several models didn't match their Items.md state description (e.g. spotlight/projector lens always lit even in "normal/off" state; panini press lid always closed even when "open, safely cutting power") | ✅ Done | `stage_spotlight`/`ceiling_projector` normal state now shows a dark/idle lens; `jammed_panini_press` normal state now shows the lid propped open on its hinge; `dust_choked_pc` hazardous now models an actual backpack element wedged against the vent instead of a flat dust-textured slab; `swollen_phone_battery` hazardous now drapes an actual leather-jacket element over the phone. |
| H-4 | Post-H-2 textures were still flat, single-tone color fields — the props read as generic colored boxes even with the right silhouette; several models were only 2 cuboids with no protruding detail | ✅ Done | `scripts/generate_hazard_textures.py` rewritten: every one of the 53 textures now uses a diagonal top-left light `gradient_shade`, a beveled `inset_edges` highlight/shadow pair, and object-specific iconography drawn directly into the pixels (visible plug prongs on `cord_strip_black`, screw heads on `spotlight_housing_black`/`projector_housing_white`, a power/fault LED strip on `pa_rack_metal`, a control dial on `press_body_metal`/`fryer_body_steel`, a camera-lens glint on `phone_body_black`, a torn-foil puncture on `lipo_foil_damaged`, corrugation ribs on `bin_plastic_gray`, product-column dividers on `vending_body_blue`) instead of flat `hbands`/`speckle` fills. `frayed_console_wire`(+hazardous) gained a `connector_plug` cuboid, `vape_in_iron_locker` gained a protruding `handle` cuboid, `swollen_phone_battery` gained a `bottom_port` cuboid, and `contaminated_kitchen_bin` gained `lid_handle` + `foot_pedal` cuboids — all four were previously flat 2-element boxes. |

---

## Furniture Visual Remediation Log

A 2026-07-02 rendering-bug report turned out, after a full structural audit (missing textures, raw non-`#variable` face-texture refs, blockstate FACING/property coverage, orphaned item models, out-of-range geometry — checked across all 34 custom blocks), to have **zero real rendering bugs**: every texture resolved, every face used a proper `#variable`, every FACING/property combination had a blockstate variant. What actually needed fixing was the same gap already closed for hazard props in H-2/H-4: the 11 furniture blocks were still "vanilla-texture models" (raw `oak_planks`/`iron_block`/`*_concrete` stretched over cuboids), reading as flat and generic rather than "modern and recognizable" — most visibly on `ComputerTableBlock`, which looked like plain stacked wood.

| # | Item | Status | Notes |
|---|---|---|---|
| F-1 | `plastic_trash_bin.json` blockstate missing explicit `y` rotation values (found during the audit) | ✅ Done | Every sibling FACING hazard-prop blockstate sets `y=90/180/270` for east/south/west; this one omitted them. Harmless only because the model happens to be radially symmetric — fixed for consistency. |
| F-2 | `ComputerTableBlock` looked like raw `oak_planks`/`oak_log` — "just old wood" | ✅ Done | `desk_laminate_white` (desktop) + `desk_leg_metal_black` (legs/crossbar) + `desk_cable_panel_dark` (modesty panel w/ grommet + vents) — modern office desk. |
| F-3 | `ChairBlock` was a plain `dark_oak_planks` stool | ✅ Done | `chair_frame_black` + `chair_mesh_fabric` (backrest) + `chair_cushion_fabric` (seat) — modern task chair. |
| F-4 | `DrawersBlock` was raw `dark_oak_planks`/`birch_planks` | ✅ Done | `cabinet_body_painted` + `drawer_front_painted` + `handle_bar_metal` — flat-panel modern dresser. |
| F-5 | `FilingCabinetBlock` was raw `iron_block` | ✅ Done | `cabinet_body_graphite` + `drawer_face_graphite` + `handle_bar_black` + `label_holder_white` — dark brushed-metal cabinet. |
| F-6 | `LockerBlock` was raw `iron_block` + a literal `gold_block` padlock | ✅ Done | `locker_body_painted`/`locker_door_painted` (navy matte steel) + `locker_seam_dark` + `vent_slats_dark` + `keypad_lock` (LED keypad, replacing the gold block) — reuses `handle_bar_black`/`label_holder_white` from F-5. |
| F-7 | `TrashCanBlock` was raw concrete | ✅ Done | `trash_body_charcoal` + `trash_rim_metal` — matte pedal-bin look; reuses `handle_bar_black`. |
| F-8 | `WhiteboardBlock` was raw `white_concrete`/`gray_concrete` | ✅ Done | `whiteboard_surface` (glossy, faint marker streaks) + `whiteboard_frame_metal` (brushed aluminum, also reused for the tray). |
| F-9 | `BulletinBoardBlock` used `note_block` for cork + raw concrete "paper" | ✅ Done | `board_felt_charcoal` + `note_paper_yellow`/`note_paper_blue` + `pin_metal_red`/`pin_metal_teal` — reuses `whiteboard_frame_metal` (F-8) for the frame. |
| F-10 | `ToiletBlock` was raw `white_concrete`/`light_gray_concrete` | ✅ Done | `ceramic_glossy_white` (specular-highlight porcelain) + `fixture_chrome` — shared with `SinkBlock`. |
| F-11 | `SinkBlock` was raw `white_concrete`/`iron_block` | ✅ Done | Reuses `ceramic_glossy_white`/`fixture_chrome` from F-10 — no new textures needed. |
| F-12 | `CeilingFanBlock` was raw concrete/iron/`dark_oak_planks` | ✅ Done | `fan_housing_white` + `fan_blade_matte` (flat, no wood grain) + reused `handle_bar_black` (rod) + `light_bulb`'s `led_diffuser_glow` for the light bowl — matches the mod's other ceiling fixture. |

All new textures generated by **`scripts/generate_furniture_textures.py`** (mirrors the `generate_hazard_textures.py` helper toolkit — `gradient_shade`, `inset_edges`, `border`, `speckle`, etc. — plus a `vertical_brush`/`mesh_weave` pair for brushed-metal and fabric reads), aimed at a clean modern-office palette (matte panels, brushed metal, felt/fabric) instead of the hazard set's grime accents:
```bash
python3 scripts/generate_furniture_textures.py
```

---

## Hazard Prop 3-State Log (2026-07-05)

Before this pass, `HAZARDOUS=true` served double duty: it was both "developing danger" (the window
during which a prop could be defused) *and* the direct trigger for `onHazardFailure` (real adjacent
fire) once its failure timer elapsed — there was no distinct "this prop is now actually burning"
block state, and no way to fix a hazard except spraying it with an extinguisher.

All 19 hazardous-property props (`woodshop_sawdust_layer` keeps its own `ACCUMULATION` mechanic)
now have a genuine 3-state lifecycle: **normal → hazardous → on-fire**.

- `HazardBlock`/`HazardFacingBlock` gained a new `ON_FIRE` `BooleanProperty` alongside `HAZARDOUS`.
- `HazardManager.triggerFailure` sets `ON_FIRE=true` on the prop itself before igniting adjacent
  blocks — "on fire" is now a real terminal state instead of an invisible transition.
- A new bare-hand right-click **prevention** interaction (`HazardBlock`/`HazardFacingBlock.useWithoutItem`)
  resets a merely-hazardous prop back to safe — teaches "prevention beats intervention" without
  needing an extinguisher. It does nothing once the prop is genuinely `ON_FIRE` (too late for a
  bare-handed fix at that point; matches Sgt. Reyes's new prevention → intervention lesson order,
  see Room 2 above).
- `HazardManager.defuse()` (the extinguisher-spray path) resets both `HAZARDOUS` and `ON_FIRE`.
- **Scope decision:** "on fire" is signaled via intensified particles (extra `spawnHazardParticles`
  calls + `FLAME`) and the existing real adjacent-fire ignition, reusing each prop's existing
  `_hazardous` model for the new `on_fire=true` blockstate variant — not 20 new sets of bespoke
  burning textures/models (a disproportionate art task; can be iterated later).
  `scripts/add_onfire_blockstates.py` regenerated all 19 blockstate JSONs with the new permutation.

## Hazard Prop State Management Log

Tracks the rollout of gameplay-driven state management for the 20 hazard prop blocks. Previously, `HAZARDOUS` (and the sawdust layer's `ACCUMULATION`) was set once to its safe default at placement and never changed at runtime — see the 2026-07-01 audit that found no `HazardManager`/`HazardSpawner` and no `SimulationManager`/`SimulationEffects` references to the `block.hazard` package at all. All 20 items below now have a working normal→hazardous→failure lifecycle per `docs/md files/Items.md`, driven by the new `HazardManager`.

| # | Item | Status | Failure consequence (Items.md) |
|---|---|---|---|
| S-1 | `plastic_trash_bin` | ✅ Done | Class A fire from the smoldering vape battery |
| S-2 | `daisy_chain_extension` | ✅ Done | Class E electrical wall fire at the cord junction |
| S-3 | `woodshop_sawdust_layer` | ✅ Done | Flash-ignites a 3×3 area at accumulation=5 (state machine lives entirely in `HazardManager`, no `HAZARDOUS` property to hook) |
| S-4 | `stage_spotlight` | ✅ Done | Ignites the curtains into a climbing Class A fire |
| S-5 | `archive_box_stack` | ✅ Done | Deep, smoldering Class A archive fire |
| S-6 | `dust_choked_pc` | ✅ Done | Class E hardware fire from the popped power supply |
| S-7 | `charging_cart` | ✅ Done | Explosion from battery thermal runaway (short delay, wide radius) |
| S-8 | `frayed_console_wire` | ✅ Done | Arcs and ignites the carpet underneath |
| S-9 | `malfunctioning_vending` | ✅ Done | Internal plastics catch fire, Class E smoke |
| S-10 | `ceiling_projector` | ✅ Done | Shattered bulb drops burning plastic clusters |
| S-11 | `swollen_phone_battery` | ✅ Done | Torch-like chemical fire (short delay) |
| S-12 | `damaged_lipo_pack` | ✅ Done | Violent white-hot burst, 2-block radius |
| S-13 | `vape_in_iron_locker` | ✅ Done | Explodes internally, ignites neighboring lockers |
| S-14 | `pa_system_backup` | ✅ Done | Severe Class E electrical panel fire, PA blackout |
| S-15 | `smartboard_inverter` | ✅ Done | Water-shorted circuitry ignites the wall behind it |
| S-16 | `unattended_grease_pan` | ✅ Done | Class F/K grease fire (short delay) |
| S-17 | `grease_clogged_hood` | ✅ Done | Sparks ignite the duct work (long delay — slow buildup) |
| S-18 | `contaminated_kitchen_bin` | ✅ Done | Instant ignition, unquenchable floor flames |
| S-19 | `jammed_panini_press` | ✅ Done | Carbonized oils engulf the countertop line |
| S-20 | `commercial_deep_fryer` | ✅ Done | Oil reaches auto-ignition, massive grease fire (short delay, wide radius) |
| S-21 | `computer` (`ComputerBlock`) | ✅ Done | Electrical fire spreads to nearby equipment (240-tick delay — faster than the 300-tick generic default). Special-cased like sawdust: no `HAZARDOUS` property, so `HazardManager` tracks a lazily-seeded timer keyed off `BURNING=true` instead of the generic activate/defuse flow. Its 3 existing ignition triggers (flint & steel, session-start, periodic CCS spread) are untouched. |

**Not yet implemented:** the water-triggers-explosion interaction called out for `unattended_grease_pan` ("Water triggers a 3x3 fiery explosion!") is flavor text only for now — no `neighborChanged`/fluid-contact hook exists yet. Score impact from hazard failures (beyond incrementing `fireSpreadCount`) is also not tuned.

**Dev testing tool:** `HazardManager.tick()` only runs inside an active FIRE/CCS_FIRE session (hazard positions are cached once at `startSimulation`), so there's no automatic way to exercise the state machine outside one. `HazardWandItem` (`berongsmp:hazard_wand`, get it via `/item get hazard_wand`) closes that gap — right-click any hazard prop to toggle normal↔hazardous (or `ComputerBlock`'s `BURNING` state) or force its failure consequence immediately, with or without a live session, instead of typing `/setblock` coordinates.

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
