# Cruz Pathfinding Recommendations (2026-07-05, Fable-5 architectural review)

A dedicated Plan-agent review (model `claude-fable-5`) of `CruzRoomManager.java`'s escort logic and
`CustomNpcEntity.java`'s navigation setup, requested explicitly as a standalone deliverable rather
than another one-line tweak. The small/trivial items (path-aware stuck detection, target-change
resets, `nearPlayerTarget` on `GOSTOP_RUN`/`DONE`, `getNavigation().stop()` on escort-off, stale
comment fixes) were implemented the same day — see the Room 1 section above.

**Escort state moved onto the entity (2026-07-06):** `nextEscortMoveTick`/`lastCruzPos`/`lastTarget`/
`stuckCycles`/`lastTooFarNudgeTick` used to be `static` fields on `CruzRoomManager` itself — harmless
while exactly one `OFFICER_CRUZ` instance exists (duplicates are swept every tick, see
`AcademyBuildingManager.sweepStrayCruz`), but state describing "the escort in progress" rather than
"this specific NPC," and a latent trap if that one-instance invariant were ever violated (two
entities would silently corrupt each other's bookkeeping). Moved onto `CustomNpcEntity` itself
(`getNextEscortMoveTick`/`getLastEscortPos`/`getLastEscortTarget`/`getEscortStuckCycles`/
`getLastTooFarNudgeTick` + setters) so it travels with the object it describes. **This is a defensive
hygiene fix only — it does not enable escorting two students at once.** There is still exactly one
physical Cruz entity, so a second concurrent student in Room 1 still isn't escorted; that remains the
already-accepted "single-station-at-a-time" limitation documented in `CruzRoomManager.updateCruzEscort`'s
own javadoc, and would need spawning multiple building/NPC instances to actually fix — a much larger
feature, not attempted here.

**Not implemented**, kept as a documented recommendation for a future dedicated pass:

- **Move the escort mechanics into a proper `Goal`** (`EscortGoal`/`setEscortTarget(...)` API on
  `CustomNpcEntity`) instead of `CruzRoomManager` externally calling `getNavigation().moveTo(...)`
  every 15 ticks. `CruzRoomManager` would keep 100% of the "where should she go" phase/waypoint
  logic; the goal would own "how does she get there" — ticking every server tick (not every 15),
  able to watch `Path` node advancement directly, with a guaranteed `start()`/`stop()` lifecycle.
  This is the highest long-term-value change (and would also move `lastCruzPos`/`stuckCycles`/
  `nextEscortMoveTick`/`lastTarget` from static manager fields into per-entity instance state,
  closing a latent multiplayer-safety gap), but it's a real refactor deserving its own dedicated
  pass rather than folding into a round of targeted fixes.
- **Issue `moveTo` once per target change for the *fixed* waypoints** (briefing marks, maze/jump
  waypoints, the Go/Stop staging line, the briefing-anchor walk-home) instead of blindly recomputing
  an identical path every 15 ticks — re-issue only on `isDone()`-not-arrived, not on a fixed clock.
  The *moving* targets (`GOSTOP_RUN`, `DONE`, the too-far chase) are fine re-issuing on the existing
  cadence, matching vanilla follow-goal idiom. Worth revisiting `FOLLOW_RANGE`/
  `setRequiredPathLength` (currently 48, Room 1's longest leg is close to that) if this cadence
  change lands, since issue-once makes path-length headroom matter more.

