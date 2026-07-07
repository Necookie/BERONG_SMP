# New Tutorial Building (Academy)

A second, **fully independent** tutorial — "the Academy" — lives in `academy_building.schem`, placed at `AcademyBuildingManager.POS = BlockPos(-177,-34,8)`. It deliberately does not reuse or extend the old `tutorial/` package (`TutorialStage`/`TutorialManager`/`NpcDialogue`/`NpcRole`) — a single flat stage enum can't represent 4 independently-progressing NPC rooms, so it's a parallel system in its own `net.necookie.disastersim.academy` package instead. The old tutorial keeps functioning exactly as before and is still the only thing gating the fire/quake simulation buttons (`LobbyManager.gatesPassed`) — wiring the Academy into that gate is an explicit future step, not done yet. Full dialogue script, per-room coordinate tables, and a Mermaid flow diagram: `docs/academy_script.md`.

```
Room 1 — Officer Cruz (Movement School): BRIEFING (4 green-tile WASD walk — physical lime
  concrete floor tiles placed at runtime by AcademyBuildingManager.placeGreenMarks, since the
  schematic contains no green blocks; the next unhit one also gets the particle beacon) → MAZE →
  JUMP → GOSTOP_STAGE/GOSTOP_RUN (Officer Cruz calls GO/STOP on a random 3-6s cadence; moving past
  Config.ACADEMY_GOSTOP_GRACE_TICKS after a STOP call warps the player back to the staging line
  and counts a movement mistake) → DONE. **Zone-gated Go/Stop start + starting-line barrier
  (2026-07-05):** the briefing used to begin the instant the jump course's finish line was crossed,
  with nothing stopping the player from wandering into the tunnel before ever talking to Cruz.
  `CruzRoomManager.onInteract` now requires `GOSTOP_ZONE.contains(player.position())` (schem-verified
  pen interior, X -102..-98/Z 47..55) before starting the briefing dialogue at all; `tickGoStopStage`
  pushes the player back east of `GOSTOP_STARTING_LINE_X` (-103, the pen's real entrance gap) with a
  throttled "wait for GO" nudge if they try to cross early — not counted as a movement mistake, just
  an invisible wall. A **fun green countdown** (`GoStopState.currentBanner`, purely cosmetic) now
  also refreshes once a second during `GOSTOP_RUN`, re-appending "next call in Xs" to whatever GO/STOP
  banner is already showing. MAZE and JUMP use **schem-verified waypoint chains**
  (`MAZE_WAYPOINTS` through the serpentine's wall gaps at X=-131/Z=25, X=-127/Z=38, X=-124/Z=25;
  `JUMP_WAYPOINTS` just past each 1-block hurdle row at X=-117/-114/-111) advanced per-player when
  within 2 blocks — both the compass needle and Cruz's escort target the current waypoint, never
  the far exit through a wall. A single Cruz (`CruzRoomManager.findCruz`, 3-tier fallback: cached
  direct ref → UUID lookup → bounded AABB scan tie-broken by proximity) physically escorts the
  player via `CustomNpcEntity.setEscorting(true)` + a periodic `getNavigation().moveTo(...)`
  re-issued every ~15 ticks. Navigation hardening: FOLLOW_RANGE 48 (default 16 also caps the
  pathfinding node budget — the old "gets lost" root cause), STEP_HEIGHT 1.1 (walks the hurdles),
  FloatGoal, door passage + explicit path budget set on escort start. **Stuck recovery**: 6
  consecutive no-progress escort cycles (~4.5s at the 15-tick cadence — `isStuck()`, or <0.25 blocks
  moved, while >2 blocks from target) → `recoverCruz` poof-teleports her to the player's side with
  POOF particles at both ends. **Path-aware stuck detection (2026-07-05, per a Fable-5 architectural
  review — see "Cruz Pathfinding Recommendations" below):** a provably unreachable target
  (`Path == null || !Path.canReach()`, read directly off `getNavigation().getPath()`) now counts for
  `UNREACHABLE_STUCK_STEP` (3) cycles instead of 1 — recovers in ~1-2 cycles instead of waiting out
  the full ~4.5s budget, which is what actually fixed her lagging visibly behind at every Go/Stop
  tunnel barrier (a genuinely unreachable target every time, since she can't crouch under the
  slabs). `stuckCycles`/`lastCruzPos` also reset on every real target change (`lastTarget`,
  `TARGET_CHANGE_EPSILON_SQ`) — including the escort↔`tickReturnHome` handoff — so stall history
  from one target never counts against a different one. `GOSTOP_RUN`/`DONE` now route through the
  same `nearPlayerTarget` 2-block-short offset the too-far chase already used, so she never
  shoulders into the player at those hand-offs either; `CustomNpcEntity.setEscorting(false)` now
  also calls `getNavigation().stop()` so a stale in-progress path can't silently resume next time
  escorting turns back on. **Wandered-too-far chase (2026-07-05, retuned twice same day)**: if the
  escorted player strays more than `PLAYER_TOO_FAR_DISTANCE_SQ` blocks (tuning history: 14 → 7 for
  quicker reaction → 20, the current value, per explicit follow-up feedback wanting a larger
  chasing radius — she should let the player roam a bit before reeling them in) from Cruz's current
  position, `updateCruzEscort` abandons the phase's waypoint for that re-issue and walks toward the
  player instead — stopping about `CHASE_STOP_DISTANCE` (2) blocks short via `nearPlayerTarget`
  (same offset-toward-approach-side idiom as `recoverCruz`), so she reads as catching up to the
  player rather than walking through them (`clampToRoom1Bounds` keeps the chase target inside
  `ROOM1_BOUNDS` so it can never pull her outside the building) — resuming the normal waypoint once
  they're close again. The first time this triggers she also speaks up — a throttled
  (`TOO_FAR_NUDGE_COOLDOWN_TICKS`,
  once per 10s) "come back this way, trainee!" line — instead of silently repositioning with no
  in-character acknowledgment. **No teleports at the finish line or on the walk home
  (2026-07-05):** at the Go/Stop finish line, `tickGoStopRun` no longer snaps her beside the player
  at all — `DONE` already keeps her escorting (targeting the player's live position every re-issue,
  same as `GOSTOP_RUN`), so she just naturally walks up over the next couple of seconds instead of
  teleporting, and keeps standing with the player through the whole "you did it, go find Reyes"
  line (a real spoken Cruz line via `forceStartDialogue`, reusing `CRUZ_LINES.get(DONE)`) and beyond,
  until they actually leave through the door — never rushing off mid-sentence. `tickReturnHome`
  itself no longer teleports either: it just keeps re-issuing the walk home every cycle instead of
  giving up after a stuck threshold (`RETURN_HOME_STALL_WARNING_CYCLES` only logs a one-time
  diagnostic warning at ~30s of no progress — it takes no action). The walk back to
  `BRIEFING_ANCHOR` is a short, schem-verified-open route, so she always just walks it for realism.
  **Escort-through-the-door hand-off
  (2026-07-05)**: `CruzPhase.DONE` used to immediately drop the player from escort selection the
  instant the Go/Stop finish line was crossed — well inside Room 1 — sending Cruz into
  `tickReturnHome` before the player had gone anywhere near actually leaving (read by the user as
  her "instantly banishing" herself). Parsed `academy_building.schem`'s raw block data directly
  to find the real wall gap into Sgt. Reyes's room (world X=-162, verified — not guessed) and
  widened `ROOM1_BOUNDS` to that threshold; `DONE` now keeps counting as escortable (targeting the
  player's live position, same idiom as `GOSTOP_RUN`) for as long as the player is still physically
  inside that footprint, so Cruz walks them right up to the doorway before the normal gradual
  `tickReturnHome` walk-back takes over the instant they actually step through. `ESCORT_STUCK_
  CYCLES_MAX` bumped 4→6 so a normal walk across the wider corridor doesn't spuriously trip the
  stuck-recovery poof. **Own-turn-only gating (2026-07-05):** that same `DONE`-escortable check now
  *also* requires `reyesPhase() == ReyesPhase.NOT_STARTED` — the instant the player clicks Reyes and
  her dialogue actually begins, Cruz stops following that same tick regardless of whether they're
  still technically inside `ROOM1_BOUNDS`. She only ever follows during her own turn of the
  tutorial, never once Reyes is teaching. **Crouch-through-the-tunnel (2026-07-05):** she can now
  physically walk the crouch-tunnel instead of poofing past each slab lane —
  `CustomNpcEntity.setCrouchingForObstacle` shrinks her hitbox to a vanilla-crouch-sized 0.6×1.5 via
  an overridden `getDefaultDimensions` + `refreshDimensions()` (the same trick
  `DuckCoverHoldManager.allowCrawlUnderTable` uses for players, adapted for a non-player `Mob`, whose
  bounding box doesn't otherwise change with `Pose` at all), toggled every escort tick by
  `updateCruzEscort` based on her own position against `GOSTOP_TUNNEL_ZONE` (schem-verified to cover
  all three slab lanes at X=-106/-107, -110, -114/-115). Combined with the path-aware stuck detection
  above, the lanes are now genuinely reachable paths instead of provably-unreachable ones needing the
  poof fallback. **Faster idle look (2026-07-05)**:
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
  earlier position-targeted boot-only fix didn't work. `AcademyBuildingManager.sweepStrayCruz`
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
  - **Computer hazard state (2026-07-05):** `ComputerBlock` never went through `HazardManager.activate`
    (its own `LIT`/`BURNING` properties, not the shared `HAZARDOUS` flag) — so the electrical step of
    `PREVENTION_DEMO` used to do nothing at all: the computer sat in its default off state with
    nothing to prevent. `armPreventionHazard` now special-cases `ComputerBlock` by setting
    `LIT=true`, reusing its existing "sparking" `ELECTRIC_SPARK` visual as the hazard cue — no new
    block property or art needed, since the existing bare-hand right-click already toggles `LIT`
    back off. `isPreventionActive` (LIT-based) sits alongside the unchanged `isActive` (BURNING-based,
    still used by the later intervention/`LIVE_FIRE_DEMO` step).
  - `PREVENTION_DEMO`: the same 3 demo-hazard positions used later (`ReyesRoomManager.HAZARDS`:
    Class A archive boxes → electrical computer → kitchen grease pan) are set merely
    `HAZARDOUS=true` via `HazardManager.activate` (never actually ignited; the computer uses `LIT`
    instead, per above). Each hazard's turn plays `AcademyDialogue.REYES_PREVENTION_LINES[idx]` (the everyday habit that prevents it),
    then the player fixes it with a bare-hand right-click — `HazardBlock`/`HazardFacingBlock`'s new
    prevention interaction (see Hazard Prop 3-State Log below) — before the sequence advances to
    the next hazard, then to `TOOL_SELECTION`.
  - `TOOL_SELECTION` (inventory contains all 3 extinguisher items, picked up from wall item
    frames) → `LIVE_FIRE_DEMO`. **Point-then-teach per frame (2026-07-05):** rather than one generic
    "collect all three" line, `AcademyDialogue.REYES_TOOL_LINES[idx]` (indexed like
    `EXTINGUISHER_FRAMES`) gives each extinguisher its own two-beat introduction — pointing at that
    specific frame (compass+beacon, via `nextUncollectedFrame`), then teaching the pop-off-the-wall
    pickup mechanic — played once per frame (`explainedFrame`) the instant it becomes the player's
    target, instead of everything being explained at once regardless of which frame is next. The 3
    glow item frames' positions/facings/items are schem-verified against `academy_building.schem`
    directly (`-170/-168/-166,-32,10`, facing south, fire/CO2/wet-chemical respectively) — they were
    already correct. **Reyes never hands out an extinguisher (2026-07-05):** entering
    `TOOL_SELECTION` now calls `stripExistingExtinguishers`, removing any of the 3 the player already
    holds — otherwise a player who already had one or more (in practice, most often a dev who'd used
    `/item kit` earlier in the same session) skipped straight past the whole phase the instant it
    began, since the completion check only looked at current inventory contents. Every player now
    has to genuinely retrieve all 3 from their wall frames, regardless of prior inventory state.
    `LIVE_FIRE_DEMO`, the "intervention" phase, taught **sequentially** one hazard at a
    time in the same fixed order. Entering a hazard's turn plays its
    `AcademyDialogue.REYES_HAZARD_LINES[idx]` explanation (what's burning, which extinguisher, why)
    through the shared dialogue sequencer, alongside the now-already-armed prop; `igniteHazard`
    calls `HazardManager.forceFailure(level, null, pos, null)` (the same session-nullable entry
    point `HazardWandItem` uses — this now also flips the prop's new `ON_FIRE` state true, per the
    Hazard Prop 3-State Log). Using the wrong extinguisher doesn't just warn — `checkAndHandleDefuse`
    re-ignites the same prop immediately, so the player must get it right before the sequence
    advances (edge detection is per-player: `lastActive` is `Map<UUID, Map<BlockPos, Boolean>>` so
    two concurrent Room-2 players don't corrupt each other). **Controlled fires only (2026-07-05):**
    a Fable-5 investigation traced reports of the player "randomly" catching fire to real vanilla
    fire placed with no player-occupancy check — `HazardBlock`/`HazardFacingBlock.igniteAdjacent`/
    `igniteRadius` (every hazard failure) and especially `ComputerBlock.randomTick`'s 6-direction
    flood while `BURNING=true` (the single most aggressive source, since a player fighting the
    computer's fire stands right next to it) both now skip any target a player occupies or stands
    within ~0.6 blocks of (`isPlayerNear`). `checkAndHandleDefuse`'s success branch also sweeps a
    3-block radius (`clearNearbyFire`) around a just-defused hazard, since `HazardManager.defuse`
    only ever reset the prop's own block state, never the real fire its failure/re-ignitions had
    already placed beside it — residual fire could otherwise linger into the next hazard step.
    **Hard anti-ignition guarantee (2026-07-06):** two more layers closed the remaining paths
    (vanilla fire spread over the demo's duration, residual fire brushed while walking between
    hazards): `ReyesRoomManager.tick` now extinguishes any Room-2 trainee the same tick they catch
    fire unless the scripted ignite demo (`igniteWindow`) is active — that demo is the ONLY
    sanctioned way a trainee ever burns; and `containRoomFire` sweeps the whole room once a second —
    during `LIVE_FIRE_DEMO` sparing only blocks within `RESIDUAL_FIRE_SWEEP_RADIUS` of the *current*
    hazard, during `PREVENTION_DEMO` (where no fire belongs at all) sparing nothing.
    **Explain-before-ignite (2026-07-05):**
    after all 3, `AcademyDialogue.REYES_IGNITE_LINES` plays first — teaching that clothes can still
    catch fire even after doing everything right, the stop-drop-and-roll mechanic, and its Shift+R
    controls — and only its completion (`beginIgniteDemo`) actually sets the player alight
    (`igniteExplained` tracks this per player, same idiom as `explainedHazard`/`explainedFrame`).
    Previously ignition and the "here's the controls" message happened in the same instant, catching
    the player off guard. **5-second roll requirement (2026-07-05, superseding the earlier
    single-press + reaction-floor design):** once ignited, the fire is topped up every tick and
    **never goes out on its own — there is no timeout at all**. The only way out is accumulating
    `ROLL_REQUIRED_TICKS` (100 ticks/5s) of *actual rolling*: `rollHeldTicks` counts only ticks
    spent inside `DropAndRollManager.isDropped`'s dropped window (genuinely holding Shift and
    pressing R; each press extends the window ~5s, so continuous rolling means repeated presses —
    the counter pauses rather than resets while not rolling). A once-per-second timer shows the
    remaining roll time ("Rolling — keep it up! Xs") and re-teaches the Shift+R controls whenever
    the player isn't currently rolling. `Config.ACADEMY_IGNITE_DEMO_TICKS` is **no longer used by
    this demo** (kept only as a config entry) → on completion, `advanceToAlarmCheckpoint`
    (prop/fire cleanup, phase → `ALARM_CHECKPOINT`).
  - `ALARM_CHECKPOINT`: Reyes explicitly teaches "the moment a fire starts, always ring the alarm
    first" and points the compass at the fire alarm at `ALARM_POS = (-143,-32,40)`. **Duplicate
    alarm fix (2026-07-05):** the schematic actually already bakes a
    `berongsmp:fire_alarm[activated=false,facing=south]` in at this exact spot (confirmed by
    parsing `academy_building.schem`'s raw block data) — a since-removed
    `AcademyBuildingManager.placeFireAlarm` was placing a second one one block below every boot,
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
  guided objective in the Academy). After a short delay, triggers the quake → QUAKE_ACTIVE
  reads compliance from a **room-local, table-scoped streak** (`SantosRoomManager.tableHoldTicks`)
  — completion requires 5 *consecutive* seconds (`HOLD_REQUIRED_TICKS`); any non-compliant tick
  resets the countdown to zero with an explicit "the countdown reset!" message.
  **Room-owned detection (2026-07-06, superseding both the ±1-tolerance and the
  `DuckCoverHoldManager.isCompliant` versions):** compliance is now `isUnderTable` (feet literally
  inside one of the 4 `TABLE_ROW` block cells — crouching *beside* the table no longer counts)
  **and** `isDucking` (`isShiftKeyDown() || isCrouching()`). The `isCompliant` gate this used to
  lean on was actively backwards under the table: the kneehole's sub-1-block clearance forces
  vanilla's `Pose.SWIMMING` (crawl), where `isCrouching()` is false — so the exact player who had
  correctly crawled underneath could **never** accumulate hold time, while someone merely crouching
  beside the table could. The sneak key stays true while crawling, hence it's the primary signal.
  `isNearTableRow` (the old ±1 tolerance) survives only to pick the right coaching nudge — three
  distinct tiers: under-but-not-sneaking, beside-but-not-under ("being beside the table isn't
  cover — walk INTO it"), and away-from-the-table. A live countdown caption (once per second —
  "Under cover — hold there... 4s/3s/2s/1s") confirms the detection actually fired.
  **Dedicated shake channel (2026-07-06, superseding the fading-caption-intensity fix):** camera
  shake used to ride along on `AcademyStatusPayload`, so *every* caption in the Academy (dialogue
  lines, the sequencer's end-of-sequence clear, Cruz banners, guardrail nudges) implicitly sent
  `intensity=0` and silently stopped the quake — while a stray 1.5f from a Santos nudge could stick
  around with nothing to overwrite it. Seen as "a random earthquake showing up even though it's not
  for the simulation yet". Shake now travels on its own `AcademyShakePayload` (registrar "6",
  server-side deduped via `AcademyVisuals.setShake`), asserted **every tick** during QUAKE_ACTIVE:
  full 1.5 while not holding (the quake genuinely never stops on its own, wherever the player
  goes), fading 1.5 → 0 across the 5-second hold (mirroring the old tutorial's `QUAKE_HOLDON`), 0
  only on true completion and on `clearPlayer` (so a `/bfp` reset can't strand a shaking client).
  Captions no longer carry an intensity field at all.
  **Quake starts only with the player present (2026-07-06):** `tickPreDrill` holds the trigger
  until the player is within `QUAKE_START_RANGE` (20 blocks) of the table row — the fixed 3-second
  timer used to elapse wherever the player had wandered mid-briefing and hit them with a seemingly
  random earthquake far from the drill. `tickPreDrill`
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

**Scoring scope is deliberate (confirmed 2026-07-06):** Reyes's `PREVENTION_DEMO` and
`ALARM_CHECKPOINT`/`EVACUATION_BRIEF` phases earn zero points in `AcademyScoring` — only movement,
fire correct/wrong ratio, drop-and-roll, and quake compliance feed the 0-100 rubric. Raised during a
full audit and explicitly decided, not an oversight: expanding the rubric to those two phases would
require inventing new fail/mistake criteria for steps that are currently non-punitive by design (Reyes
never tells the player they're graded on prevention or the alarm), which is a pass/fail-bar decision
for the thesis instrument, not something to pick unilaterally. Both phases remain mandatory to
progress and are now fully covered by `AcademyTelemetry` (see above) for later analysis — just not
part of certification. Revisit only if a future rubric redesign is explicitly requested.

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
  the SAME sequence is playing skips immediately to the next line — but only once that line has
  been up for at least `MIN_LINE_DISPLAY_TICKS` (1s, tracked via `DialogueSession.shownAtTick`;
  added 2026-07-05) — a guardrail in this one shared sequencer, protecting every room/course at
  once, against a player mashing the interact key to blow through an entire sequence (and whatever
  phase transition its `onComplete` triggers) within a fraction of a second, most visibly seen as
  Cruz's dialogue racing ahead of what her own tick-driven state machine expected to still be
  teaching. `forceStartDialogue` (tick-driven triggers, not click-driven) is unaffected. A request
  for a *different*
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

**Pacing fix (2026-07-06):** `SANTOS_LINES.NOT_STARTED` was 7 sequential lines (~55-60s of
uninterrupted lecture, by the sequencer's own word-count-based `ticksFor` pacing) before the drill's
first interactive rep — the single longest unbroken monologue in the Academy. Trimmed to 4 lines
(intro, one dense safety-facts line, one line merging the table/DROP-COVER-HOLD-ON mnemonic, the
"ready?" prompt); the aftershock fact (previously its own line 6, before the drill even started) now
lands in `SantosPhase.DONE`'s wrap-up instead, right after the player has actually felt the shake
stop — better-timed for a fact about *not* stopping too early. `REYES_LINES.ALARM_CHECKPOINT`'s
opening line did two jobs in one sentence (praise + the alarm rule); split into two single-purpose
lines so the rule itself isn't buried mid-compliment.

**Logout safety net** — a player who disconnects mid-effect doesn't just lose transient timers, they
  can end up with the effect itself silently resuming (or literally still burning) on reconnect:
  `SantosRoomManager.clearPlayer` rolls `PRE_DRILL`/`QUAKE_ACTIVE` back to `NOT_STARTED` on logout
  (the persisted phase alone would otherwise re-enter the earthquake drill's tick loop the instant
  they reconnect, with no dialogue re-triggered); `ReyesRoomManager.clearPlayer` clears the player's
  actual fire if the scripted ignite demo was active (vanilla persists remaining fire ticks in the
  player's own save data, so without this they'd rejoin still on fire). **The same rollback also
  runs on login (2026-07-06, `AcademyManager.onPlayerLogin` → `clearTransientState`)**: the logout
  hook never runs after a server crash/force-kill, leaving the mid-drill phase persisted in
  `AcademySavedData` — the room tick loop would re-enter a full-strength, context-free earthquake
  the moment the player rejoined. Every `clearPlayer` step is idempotent and phase-gated, so on a
  clean rejoin the login pass is a no-op. Separately, every HUD's
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

**Academy telemetry (2026-07-06):** every other subsystem in this mod (fire, quake, alarm presses,
extinguisher use) persists its events to `TelemetryCsvWriter`/Turso — the Academy previously recorded
nothing durable at all, only the in-memory `AcademyProgress` blob Capt. Morfe's evaluation reads once.
`AcademyTelemetry` (own per-attempt session id, since the Academy has no `SimulationSession`) now
writes `academy_start`/`academy_movement_mistake`/`academy_room1_complete` (Cruz),
`academy_prevention_fixed`/`academy_fire_correct`/`academy_fire_wrong`/`academy_alarm_pressed`/
`academy_room2_complete`/`academy_drop_and_roll` (Reyes), `academy_room3_complete`/
`academy_quake_hold_broken` (Santos), and `academy_certified`/`academy_failed` (Morfe, carrying
`score=NN` and, on fail, `;weak=...` in the existing `interaction_target` column) via
`TelemetryCsvWriter.writeRow` — no CSV schema changes, `scenario_type` is always `"ACADEMY"`.

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
`MORFE_FAIL_LINES`, transcribed from `docs/academy_script.md`); `AcademyTelemetry` (own
per-attempt session id + `TelemetryCsvWriter.writeRow` adapter — see "Academy telemetry" above);
`AcademyGuardrails` (block protection + out-of-bounds rescue + death/respawn recovery — see
Guardrails above); `AcademyStatusPayload`/`AcademyHud` (own caption channel, pattern-cloned from
`TutorialStatusPayload`/`TutorialHud`, deferring to either if already showing — carries **no**
shake intensity anymore); `AcademyShakePayload` + `AcademyVisuals.setShake` (dedicated
caption-independent camera-shake channel, deduped server-side, asserted every tick by
`SantosRoomManager` during the drill — see Room 3 above);
`AcademyCompassPayload`/`client.AcademyCompassHud` (own channel/HUD pair for the client-rendered
compass needle — see "World-space navigation" above);
`room1.CruzRoomManager`/`room2.ReyesRoomManager`/`room3.SantosRoomManager`/`room4.MorfeRoomManager`
+ `room4.AcademyScoring`. `AcademyManager.tick` is hooked into `SimulationManager.onServerTick`
alongside the existing `TutorialManager`/`DropAndRollManager`/`DuckCoverHoldManager` calls.

All Academy coordinates are now **schematic-verified** (the `.schem` was parsed directly): building
position, all 4 NPC anchors, the Room 3 table row, the maze wall gaps, the jump hurdles, the
Go/Stop tunnel slabs, and the 4 WASD mark cells (which had no green blocks in the schematic at all
— they're placed as lime concrete at runtime, see `AcademyBuildingManager.placeGreenMarks`).

