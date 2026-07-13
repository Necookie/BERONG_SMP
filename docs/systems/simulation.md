# Simulation Flow

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
  - **Message scoping fix (2026-07-05)**: `DuckCoverHoldManager.onHoldAchieved`'s "Duck, Cover, and Hold maintained" chat message (and its telemetry log) now requires an active quake-type `SimulationSession` before firing — it used to fire unconditionally, since `tick()` deliberately runs for every online player regardless of session (so the drill can be tested by just crouching under any block). That meant it popped up during completely unrelated activity, most visibly crouching under Officer Cruz's Go/Stop tunnel slabs in Room 1 (which satisfies the same crouch+cover check), and would have doubled up with `SantosRoomManager`'s own tailored completion message in Room 3 too.

### New Sim Building 2.0 — Prevention/Intervention/Evacuation (`/sim_fire new_sim_building2`)

A third, real graded FIRE scenario (`SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE`), targeting
the 34-room/2-floor `new_sim_building2.0.schem` building. Unlike Library/CCS FIRE (continuous fire spread
with organic hazard escalation until timeout/exit), this scenario runs its own explicit 3-phase story via
`SimulationSession.FirePhase` (`PREVENTION → INTERVENTION → EVACUATION → END`), mirroring how
`EarthquakePhase`/`tickQuakePhase` already work — `tickFirePhase()` is pure bookkeeping; all world mutation
happens in `SimulationManager.tickNewSim2FireSession` on the phase-change edge, exactly like
`tickEarthquakeSession` does for quake phases.

```
startSimulation(NEW_SIM_BUILDING2_FIRE)
  → arena bound to the whole building (both floors); session.bindDuration(prevention+intervention+evacuation ticks)
  → HazardManager.scanHazardProps across the full arena finds every hazard-capable prop/computer/outlet
    already baked into the schematic (no new blocks spawned) — HazardManager.armRandomHazards shuffles
    that pool and HazardManager.activate()s 5 of them (NEW_SIM2_HAZARD_COUNT)
  → session.initPhasedFire(armed) → PREVENTION; player spawns in a random 2nd-floor room
    (findRandomSpawnInNewSim2Upper), issued all 3 extinguishers (ABC/CO2/Wet Chemical — armed
    hazards may need any class)
  → phase_transition(prevention) telemetry emitted

PREVENTION (Config.NEW_SIM2_PREVENTION_TICKS, default 2:00):
  → bare-hand right-click on a hazardous prop (HazardBlock/HazardFacingBlock.useWithoutItem's existing
    prevention interaction) now also calls HazardManager.onManualPrevention — logs hazard_neutralize,
    and for this state records the hazard as found (session.recordPrevented)
  → HazardManager.tick() early-returns for this state, so no *other* prop in the building can go
    hazardous organically — only the deliberately-armed 5 are ever live
  → all 5 found before the timer → phase jumps straight to END (endReason=all_hazards_prevented)
  → timer expires with some unfound → HazardManager.forceFailure ignites every unfound hazard
    (small fire, ON_FIRE=true) → INTERVENTION; phase_transition(intervention) emitted

INTERVENTION (Config.NEW_SIM2_INTERVENTION_TICKS, default 1:30):
  → each escalated hazard needs its class-correct extinguisher — this is free: the existing
    AbstractExtinguisherItem block-ID-set gating (KITCHEN_HAZARD_IDS/WET_CHEMICAL_UNSAFE_IDS/
    OXIDIZER_HAZARD_IDS) and HazardManager.defuse already enforce it for every hazard prop in the mod
  → HazardManager.defuse, extended: for an armed+escalated hazard, records it resolved
    (session.recordEscalatedExtinguished) and logs hazard_neutralize
  → ringing the building's fire alarm (FireAlarmBlock, unchanged — just gates on session.getState().isFire())
    marks session.alarmRung — scored as a bonus, never a hard gate
  → all escalated hazards extinguished before the timer → END (endReason=intervention_success)
  → timer expires with any still burning → HazardBlock.igniteRadius widens those fires and real
    arena-wide fire spread begins (EFFECTS.simulateFire, library-style) → EVACUATION;
    phase_transition(evacuation) emitted

EVACUATION:
  → AssemblyZone/ExitZones' New Sim Building 2.0-specific methods (isInsideNewSim2/findNewSim2/
    spawnBorderParticlesNewSim2) only start mattering here — tickAssemblyZone gates on
    firePhase == EVACUATION so standing in the zone during prevention/intervention doesn't
    end the run early. Both zones are PLACEHOLDER pending an in-game F3 survey.
  → reaching assembly → endSimulation("assembly_reached"); overall session timer expiring first → "timeout"

endSimulation (any endReason):
  → NewSimScoring.evaluate() (armed/prevented/escalated/extinguished counts + alarmRung +
    assemblyReached + endReason) → score, prep_level (>=75 HIGH / 40-74 MODERATE / <40 LOW, per
    telemetry_contract.md v1.2), passed — written to the Turso sessions row (prep_level column is
    ONLY touched for this scenario, never clobbering a /bfp-set value on Library/CCS runs)
  → player alive: teleported to Capt. Morfe's Academy viewpoint; MorfeRoomManager.startSimDebrief
    plays an in-character score readout (Morfe narrates, never computes) — fully independent of
    AcademyProgress/SantosPhase gating, so it works even for a player who never touched the Academy
  → player dead: falls back to the existing pendingLobbyRespawn path, no debrief
```

**Telemetry contract v1.2 note:** this scenario is what motivated closing several gaps that applied
mod-wide, not just here — `phase_transition`, `hazard_neutralize`, and `pin_pull`/`ext_spray` (with
`hit_fire`/`extinguisher_class`) are now emitted for *any* active fire-type session via
`AbstractExtinguisherItem`/`HazardManager`, and every session's telemetry timestamp now anchors off
`SimulationSession.elapsedSeconds()` instead of the `Config.SIM_DURATION_TICKS` default (needed because
this scenario's total duration isn't the default, but it fixed the same math for every other scenario too).
`TelemetryCsvWriter`'s CSV header (and the embedded `move_log_csv` header) gained the corresponding
`hit_fire,extinguisher_class,phase` columns — a breaking change for existing CSV consumers, flagged in the
New Sim Building 2.0 plan for dashboard/ML-side coordination.

**Dev tooling:** `/sim_scan_hazards` (GM-only) runs the building-wide hazard scan without starting a
session and prints a per-named-room breakdown (`SimRoom.NEW_SIM2_UPPER_ROOMS`/`NEW_SIM2_GROUND_ROOMS`,
`SimRoom.nameInNewSim2`) — the verification tool for confirming the arena bounds in `SimulationManager`
actually cover every hazard prop/computer/outlet placed in the building, since an early palette-level
NBT scan undercounted them.

