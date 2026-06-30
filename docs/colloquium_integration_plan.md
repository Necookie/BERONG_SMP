# Colloquium Integration Plan (code-level)

Code-level companion to `Colloquium_Plan/Implementation Plan.md`. This document is the engineering blueprint: exact files, hooks, fields, event strings, and config knobs. It does **not** restate the rationale — see the colloquium plan and `CLAUDE.md` for that.

**Hard constraint:** modular, additive only. The new 3-phase fire ships as a new `SimulationState` and a new building; the existing `FIRE` / `CCS_FIRE` / `EARTHQUAKE` / `CCS_EARTHQUAKE` flows must behave identically afterward.

All paths are under `src/main/java/net/necookie/disastersim/`.

---

## 1. Integration Map (existing files that gain hooks)

| File | Change | Notes |
|---|---|---|
| `world/SimulationManager.java` | Add `SIM_3PHASE` to the `SimulationState` enum; update `isFire()` / `isCCS()`. Register `lspu_sim_building.schem` + `SIM3PHASE_POS` in `BUILDINGS`. In `onServerTick`, branch `SIM_3PHASE` → new `tickFirePhaseSession(...)`. In `startSimulation`, bind arena + hand off to `FirePhaseManager.begin(...)`. In `endSimulation`, compute outcome→score via `FirePhaseManager`. | The dispatch already splits `isFire()` vs `isQuake()` at lines ~366–370; add a `state == SIM_3PHASE` branch *before* the generic `isFire()` branch so it doesn't fall into `tickFireSession`. |
| `world/SimulationSession.java` | Add fields: `FirePhase firePhase`, `int hazardsNeutralized`, `int firePhaseTimer`, `Set<BlockPos> neutralizedHazards`, `boolean catchFireActive`, `int catchFireTimer`, `int dropRollTimer`. Add getters/setters mirroring the existing `EarthquakePhase` block (lines ~45–53, ~170–178). | Per-player state lives here, never static. The `EarthquakePhase` field group is the exact pattern to copy. |
| `world/SimRoom.java` | Add a `List<CcsRoom>`-style record list for the new building's zones (`SIM3PHASE_LAB_ROOMS`, `SIM3PHASE_CLASSROOM_ROOMS`, `SIM3PHASE_CAFETERIA_ROOMS`) using absolute AABBs; add a `fromSim3PhasePos(BlockPos)` returning the zone. Add enum constants `LAB`, `CLASSROOM`, `CAFETERIA`. | Reuse the existing `CcsRoom(String name, AABB bounds)` record and the `fromCCSPos` iteration idiom (lines 25–108). Placeholder AABBs until Track A F3. |
| `tutorial/TutorialStage.java` | Insert new stages for the movement course + PASS target range gates. Keep existing order; append rather than reorder where possible (persisted to disk via `TutorialSavedData`). | If reordering is unavoidable, note that `TutorialSavedData` stores the enum **name**, so renames are safe but deletions orphan saved data. |
| `tutorial/TutorialManager.java` | Add room-lock enforcement in `tick(...)` (confine player to current `TutorialRoom` AABB); add new gate handlers in `handleStageAdvancement(...)`; add PASS target-hit counting (parallel to `onExtinguish` campfire counting at lines 143–158). | Reuse `advanceTo`, `sendPrompt`, `getStage`. The HOLD-ON detection (lines 196–219) is the template for any "hold a posture" gate. |
| `tutorial/NpcDialogue.java` | Add dialogue maps for new stages and wire the 4th instructor (Capt. Morfe) for the Debrief Hall. | Dialogue is `Map<TutorialStage, List<DialogueLine>>` keyed by stage; `DialogueLine(text, advancesStage, soundKey)`. |
| `world/TutorialLobbyManager.java` | Switch lobby source to the user-built `bfp_tutorial_academy.schem` (NBT-first logic already supports a fallback). Spawn the 4th instructor; update NPC offsets to the new building. | `buildLobby` already tries a structure file first, then programmatic fallback (lines 74–84). Add the new schem id; keep the programmatic build as fallback. |
| `world/SimulationEventLogger.java` | **No change.** `log(String type, Map<String,Object> data)` is generic. | Just emit the new event strings from §3. |
| `world/TelemetryCsvWriter.java` | Add new CSV event rows (same `writeRow(...)` signature) for the new events that need 10 Hz/2 Hz correlation; add a `zone` value where relevant. | The `interaction_target` column can carry the zone/hazard label without a header change; a dedicated `zone` column is optional (§4). |
| `Config.java` | New `ModConfigSpec` knobs (§5). | Read at call-time via `.get()` like every existing knob. |
| `command/SimulationCommands.java` | Add a trigger for `SIM_3PHASE` (e.g. `/sim_fire phased`) for testing. | Mirror the existing `/sim_fire library|ccs` arg parsing. |

---

## 2. New Files

| File | Responsibility | Key methods |
|---|---|---|
| `world/FirePhaseManager.java` | Drives the Prevention→Intervention→Evacuation state machine for a `SIM_3PHASE` session. Static, stateless dispatcher operating on a `SimulationSession` (mirrors how `SimulationEffects` is a stateless mutator). | `begin(ServerLevel, SimulationSession)`; `tick(ServerLevel, ServerPlayer, SimulationSession, int ticks)`; `onHazardNeutralized(SimulationSession, BlockPos, SimRoom zone)`; `onExtinguisherUse(SimulationSession, ExtinguisherClass, SimRoom zone)`; `resolveOutcome(SimulationSession) → FireOutcome`. |
| `world/FirePhase.java` *(or inner enum on `SimulationSession`)* | `PREVENTION, INTERVENTION, EVACUATION, RESOLVED`. | Mirrors `SimulationSession.EarthquakePhase`. Prefer an inner enum on `SimulationSession` for consistency with `EarthquakePhase`. |
| `world/FireOutcome.java` | `PREVENTION_SUCCESS, INTERVENTION_SUCCESS, EVACUATION_SUCCESS, FAILED` + a `prepLevel()` helper. | Maps to HIGH / MODERATE / time-dependent (§6). |
| `world/HazardMarker.java` *(or reuse a block)* | Represents the 5 Phase-1 hazards. Cheapest path: tag 5 known `BlockPos` (from the building plan) as hazards and detect right-click in the existing `SimulationManager.onPlayerInteract` handler; richer path: a small custom block like `ComputerBlock`. | Recommend the tagged-`BlockPos` approach for the 1-week deadline; `onPlayerInteract` already intercepts right-clicks during a session (lines 171–206). |
| `world/TutorialRoom.java` | AABB registry, one entry per tutorial gate room, for the room-lock layer. | `record Room(TutorialStage stage, AABB bounds)`; `forStage(TutorialStage) → AABB`; `contains(stage, pos)`. Placeholder AABBs until F3. |
| `world/CatchFireHandler.java` | Catch-Fire DoT + Drop-and-Roll detection. Called from the fire-tick path. | `maybeIgnite(ServerLevel, ServerPlayer, SimulationSession)`; `tick(ServerLevel, ServerPlayer, SimulationSession)`; success/fail logging. |
| `item/ExtinguisherClass.java` *(enum)* | `ABC, CO2, DRY_POWDER, WATER` + which `SimRoom` zones each correctly handles. | Lets Phase 2 validate the right tool: `correctFor(SimRoom zone)`. `FireExtinguisherItem`→ABC, `CO2ExtinguisherItem`→CO2 already map cleanly. |

---

## 3. New Event Taxonomy (emit via `session.logger.log(type, data)`)

Keep names stable — the dashboard and `web_root/generate_synthetic_v2.py` must match. Follow the existing convention (UPPER_SNAKE for lifecycle, lower_snake for actions, matching `CLAUDE.md` §3).

**3-phase fire:**
`PHASE1_START`, `hazard_found`, `hazard_neutralized` (`{zone, hazard_id, x,y,z}`), `phase1_success`, `phase1_fail`,
`phase2_trigger` (`{zone}`), `fire_alarm_activate` *(existing — reuse)*, `extinguisher_class_correct` / `extinguisher_class_wrong` (`{zone, class}`), `phase2_success`, `phase2_fail`,
`phase3_trigger`, `assembly_area_reached` *(existing — reuse)*, `phase3_success`, `SIM_END` *(existing — extend payload with `outcome`)*.

**Catch Fire / Drop and Roll:**
`catch_fire` (`{x,y,z}`), `drop_and_roll_success` (`{reaction_ms}`), `drop_and_roll_fail`.

**Earthquake collapse / posture (in-sim):**
`duck_cover_hold_correct` (`{hold_s, zone}`), `duck_cover_hold_missing`, `structural_collapse` (`{zone, blocks}`) *(optional, for dashboard timeline)*.

**Zone telemetry:**
`zone_enter` (`{zone}`) / `zone_exit` (`{zone, dwell_s}`) — or per-zone variants `zone_enter_LAB` etc. Prefer the **single event with a `zone` field** (easier to query). Also add `zone` to the existing `PLAYER_TICK` payload (it already carries `room`).

---

## 4. Telemetry / CSV

- `event_log` (Turso `sessions.event_log`) is a JSON array blob → **all new events are non-breaking; no migration.**
- CSV (`TelemetryCsvWriter`): the header is fixed (`...,interaction_target,nearby_player_count`). New discrete events (`hazard_neutralized`, `zone_enter`, etc.) can ride the existing `writeRow(...)` using `interaction_target` to carry the zone/hazard label. Only add a dedicated `zone` column if the ML pipeline needs it as a first-class field — if so, update the header in `SimulationSession.CSV_HEADER` (line 26–27) **and** `telemetry_contract.md` together.
- Update `telemetry_contract.md` whenever an event string is added (the contract is the source of truth the dashboard + synthetic generator follow).

---

## 5. New Config Knobs (`Config.java` / `berongsmp-common.toml`)

| Key | Default | Meaning |
|---|---|---|
| `phase1DurationTicks` | 2400 (120 s) | Prevention window before Phase 2 triggers |
| `phase1HazardCount` | 5 | Hazards required to win Prevention |
| `phase2SpreadThreshold` | 24 | Fire-block count that escalates Phase 2 → Phase 3 |
| `catchFireProximity` | 2 | Blocks from active fire that ignites the player |
| `catchFireDamagePerTick` | 1.0 | DoT while Catch-Fire active |
| `dropRollWindowTicks` | 100 (5 s) | Time to perform Drop-and-Roll before damage/fail |
| `quakeCollapseMagnitude` | 7.5 | Structural-limit magnitude for collapse intensity |
| `duckCoverHoldTicks` | 60 (3 s) | Hold time for `duck_cover_hold_correct` |

---

## 6. Outcome → Preparedness Level

- `PREVENTION_SUCCESS` → **HIGH**
- `INTERVENTION_SUCCESS` → **MODERATE**
- `EVACUATION_SUCCESS` → time-dependent: fast assembly → **MODERATE**, slow → **LOW**
- `FAILED` (no assembly, injured) → **LOW**

Implementation: `FireOutcome.prepLevel()` returns the level; `endSimulation` writes a numeric `simulation_score` (so existing dashboard/Turso columns stay populated) and includes `outcome` in `SIM_END`. The dashboard's rule-based thresholds in `web_root` `queries.ts` (`≥75 HIGH / 40–74 MODERATE / <40 LOW`, per `CLAUDE.md` §10) consume the score; the `prep_level` column is still overridable by instructor/ML.

---

## 7. Placeholder-Coordinate Strategy

Code immediately against stubs; swap after Track A delivers F3 data.

- `SimRoom` new-building zone AABBs → placeholder boxes around `SIM3PHASE_POS`; final values from F3.
- `TutorialRoom` gate AABBs → placeholders relative to the new lobby origin.
- 5 hazard `BlockPos` → placeholders; final from the building's hazard locations (`Building_Architecture_Plan.md` §4).
- Assembly/exit zones for the new building → add to `AssemblyZone` / `ExitZones` (which already support multiple buildings via the `isCCS` flag — generalize to a building id or add a third zone set).

---

## 8. Cross-Repository Touchpoints

- **`web_root` dashboard** — new session-detail panels (zone breakdown, phase outcome); `queries.ts` outcome→prep mapping. Reads the same Turso `event_log`, so it's forward-compatible the moment the mod emits the new events.
- **`web_root/generate_synthetic_v2.py`** — extend the synthetic generator to emit the new event taxonomy so dashboard development isn't blocked on live mod data.
- **Turso schema** — unchanged (`event_log` JSON blob absorbs new events).

---

## 9. Sequencing — what blocks on assets vs. what's codeable now

**Codeable immediately (against stubs):**
- `SimulationState.SIM_3PHASE` + BUILDINGS wiring (stub schem ok)
- `FirePhaseManager` / `FirePhase` / `FireOutcome` state machine + branching logic
- `SimulationSession` fields + getters
- `ExtinguisherClass` enum + correct-class validation
- `CatchFireHandler` mechanic
- Event emissions + config knobs
- Dashboard + synthetic generator (against the event taxonomy)

**Blocked on Track A (`.schem` + F3 coords):**
- Final `SimRoom` / `TutorialRoom` / hazard / assembly AABBs
- Real building placement + the gate-by-gate demo run
- NPC offset finalization in `TutorialLobbyManager`

---

## 10. Regression Checklist (must stay green)

- `/sim_fire library`, `/sim_fire ccs`, `/sim_earthquake library|ccs` behave identically.
- Every `switch`/branch on `SimulationState` handles `SIM_3PHASE` explicitly (grep: `isFire()`, `isCCS()`, `isQuake()`, `getState()` usages in `SimulationManager`, `SimulationFeedback`, `TelemetryCsvWriter`, dashboard).
- Existing tutorial completion still gates simulations via `TutorialManager.isComplete(uuid)`.
- Unit tests (`EarthquakePhaseOrderTest`, `TutorialStageTest`, `TursoClientParseTest`) still pass; add a `FirePhaseOrderTest` mirroring `EarthquakePhaseOrderTest`.
