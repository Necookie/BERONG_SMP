# Multiplayer & Concurrency Audit Log (2026-07-17)

A follow-up pass to [`code-review-remediation-log-2026-07-17.md`](code-review-remediation-log-2026-07-17.md),
specifically investigating: (1) whether any simulation run can cause an item to be silently lost or
dropped, and (2) whether the mod actually holds up under many concurrent/repeated students. Frozen
log, kept for context but not updated going forward.

Every code fix below compiles clean and passes the full unit test suite (44 tests across 8 classes
as of this pass). Where a claim is backed by direct inspection of the real asset files (not
guesswork), that's called out explicitly, since this pass leaned on that technique heavily in place
of a live multi-client test the assistant cannot run.

## Item-drop / item-loss audit

**Method:** grepped every `destroyBlock`/`removeBlock`/`dropItem`/`popResource`/`harvestBlock`/
`Containers`-style call across the codebase, and directly parsed the compiled `.nbt`/`.schem`
structure assets (via Python + `nbtlib`) rather than guessing at their contents.

- Every `destroyBlock` call in the codebase (earthquake debris, extinguisher block-clearing, safety
  sprinklers, fire cleanup) already passes `dropBlock=false`; every `removeBlock` call is the
  no-drop vanilla method. `FallingBlockEntity` earthquake debris already calls `disableDrop()`.
  Confirmed: **no block-destruction path in the mod ever drops a physical block item.**
- No block in the mod implements `Container`/`WorldlyContainer` — confirmed via grep. Direct NBT
  inspection of all 6 structure files (`lspu_library_main.nbt`, `lobby_structure.nbt`,
  `ssc_building.schem`, `ccs_admin_building.schem`, `academy_building.schem`,
  `new_sim_building2.0.schem`) found **zero vanilla chest/barrel/furnace/hopper/dispenser blocks in
  any palette** — so the "a quake destroys a chest and spills its contents" scenario, a real vanilla
  mechanic (`Containers.dropContents` fires independent of `destroyBlock`'s `dropBlock` flag), is
  not physically possible with the current asset set.
- The CCS Admin Building and New Sim Building 2.0 each bake in 25 item frames holding extinguisher
  items (confirmed via NBT `Entities`/`Data.Item` inspection) — these ARE in active fire/earthquake
  arenas. Sampled several frame positions in New Sim Building 2.0: every support wall is
  `minecraft:white_concrete` (non-flammable), so fire can never reach them. CCS_EARTHQUAKE's
  generic (material-blind) block destruction could theoretically break one, dropping its item — but
  this is self-healing (the arena is fully rebuilt from the pristine schematic on every session
  end, per the C1 per-arena-placement fix) and thematically appropriate for an earthquake drill
  (buildings are supposed to take damage). Not fixed; documented as verified-and-accepted, not a bug.
- **Found and fixed a real bug:** `SimulationManager.startSimulation` issued fresh extinguishers via
  plain `inventory.setItem(slot, fresh)` for every fire-type scenario, unconditionally overwriting
  whatever was already in that one hotbar slot. A player who already carried one of the 3
  extinguisher types anywhere in their inventory — most plausibly the ones they legitimately
  collected in the Academy's Room 2 (Sgt. Reyes), but also a leftover from an incomplete prior
  attempt or `/item kit` — had it silently destroyed (no drop, no message) if it landed in the
  target slot, or left as a confusing stale duplicate if it didn't. Added
  `clearExistingExtinguishers()`, mirroring `ReyesRoomManager.stripExistingExtinguishers`'s
  already-established pattern, called before any fire-type run hands out its own copies. Every run
  now hands out a clean, deterministic, fully-known loadout. (`SimulationManager.java`)

## Per-player transient state audit ("repeated users" / memory hygiene)

**Method:** enumerated all 34 static `Map<UUID, ...>`/`Set<UUID>` fields across the codebase and
traced each one's cleanup path (a `PlayerLifecycleRegistry` logout hook, a self-healing tick-loop
check, or a bounded/negligible non-issue).

- **Found and fixed:** `SessionManager` had **no logout hook at all**. A student who disconnected
  without an instructor running `/bfp checkout` left their `StudentSession` in `activeSessions` and
  the Turso row stuck at `status='active'` indefinitely — self-healing only at that same account's
  next `/bfp checkin`/`/register`/`/login` (which always calls `checkout(..., "replaced")` first),
  or at full server shutdown. Added a logout hook calling the existing `checkout(uuid,
  "disconnected")` path. (`SessionManager.java`, `BerongSMP.java`)
- **Found and fixed:** `DuckCoverHoldManager.ticksHeld`/`achievedThisHold` were only cleared for a
  player found non-compliant while iterating the *currently online* player list — a player who
  disconnects mid-hold (still compliant at that instant) never reappears in that list, so the
  cleanup branch never runs and both entries persist for the server's lifetime. Added a
  `PlayerLifecycleRegistry` logout hook clearing both directly. (`DuckCoverHoldManager.java`)
- Verified correct and already covered: `AuthManager.active` (logout hook), `BfpAdminCommands.
  bfpAuthorized`/`testBypassActive`/`pinFailures` (logout hook, added in the prior pass),
  `DropAndRollManager.stickyBurning` (logout hook) and `droppedTicksRemaining` (self-heals via
  null-player check), `SafetyDeviceManager.deviceCache` (session-based `removeIf`) and
  `compassClearAt` (time-based self-expiry), `TutorialManager`'s three maps (`rollbackOnLogout`),
  `AcademyManager.activeSessions` (`cancelDialogue` in its logout hook), `CruzRoomManager`/
  `ReyesRoomManager`/`SantosRoomManager`'s per-room maps (`clearPlayer`, called from `AcademyManager.
  clearTransientState`), `AcademyVisuals`/`AcademyGuardrails` (`clearPlayer`, same call chain).
- Checked and left alone as genuinely negligible (bounded by distinct-player-count, tiny per-entry
  footprint, no functional impact): `AbstractExtinguisherItem.lastWrongToolWarningTick`,
  `AuthManager.loginFailures` (self-resets on next attempt regardless), `AcademyTelemetry.
  sessionIds` (overwritten, not accumulated, on every real new attempt).

## Concurrency capacity

**Method:** re-examined the actual physical-instance model post the prior pass's C1/C2 fixes, and
the two dedicated executor pools.

- **Hard architectural ceiling, not a bug:** each of the 3 graded-simulation buildings (Library,
  CCS Admin, New Sim Building 2.0) exists as exactly **one physical copy at a fixed world location**.
  The C1 fix (prior pass) makes this a correctly-enforced limit — a second session in an
  already-occupied arena is refused with a clear message — instead of the previous silent
  corruption. The real ceiling for simultaneous **graded simulation** sessions is therefore
  **3 total, one per building**, regardless of how many students are otherwise online. Scaling
  beyond that needs either multiple physical copies of a building at different world coordinates,
  or per-player instancing — both out of scope for this pass (see "Explicitly deferred" below).
- The Academy tutorial building is similarly a single physical copy, and its room managers assume a
  single active trainee: `CruzRoomManager.tick` escorts only the first eligible player found each
  tick, and `ReyesRoomManager`'s 3 scripted hazard props are shared world blocks two simultaneous
  Room-2 trainees would fight over. This matches the deployment model CLAUDE.md already documents
  (shared stations, one-at-a-time rotation) — not a defect, but a real capacity fact worth being
  explicit about when the user asks "how many students at once."
- Registration/login/`/bfp` query throughput is **not** capped by the building-instance limit above
  — it scales with the number of distinct stations/students, not with arena count. Bumped
  `TursoClient.writeExecutor` (2→8 threads — genuinely I/O-bound HTTP round-trips, safe to
  over-provision) and `AuthManager.AUTH_EXECUTOR` (2→4 threads — PBKDF2 hashing is CPU-bound, sized
  more conservatively) to absorb a classroom's worth of near-simultaneous `/register` calls at the
  start of a period without queueing noticeably. (`TursoClient.java`, `AuthManager.java`)

## Automated testing

- Extracted `SimulationManager`'s private `Arena` enum + `arenaFor()` (the state→building mapping
  the C1 occupancy guard depends on) into a new standalone `SimulationArenas` class with zero
  dependency on `SimulationManager`'s NeoForge-coupled statics — merely referencing
  `SimulationManager` transitively touches `BerongSMP.MODID`, whose static initializer needs
  NeoForge's `LogUtils` (unavailable outside a live game runtime, the same blocker noted for
  `SimulationSession`'s phase machines in the prior pass's log). `SimulationArenasTest` (6 tests)
  verifies the exact property the occupancy guard depends on: FIRE/EARTHQUAKE share the Library
  arena, CCS_FIRE/CCS_EARTHQUAKE share the CCS arena, New Sim Building 2.0 has its own, all three
  are mutually distinct, `IDLE` throws, and every real state maps to something. This is genuine
  automated coverage, not static analysis — it runs and passes under `./gradlew test`.
  (`SimulationArenas.java` new, `SimulationArenasTest.java` new)
- **Investigated and deliberately did not attempt:** a full NeoForge GameTest simulating two fake
  players starting sessions in the same/different arenas. Confirmed via NeoForge's own docs that
  every `@GameTest` requires a physical structure-template NBT (not optional/auto-generated), and
  the exact fake-player-spawning API wasn't confirmed for this project's specific (pre-release-
  labeled) NeoForge 26.1.2 build — the available documentation covers older versions (1.20.6,
  1.21.0, 1.21.1) only. Combined with real per-attempt cost (this project's own NeoForge/decompile
  toolchain takes real wall-clock time to boot a test server) and the risk of producing a test that
  looks like it verifies something but is subtly wrong, this was judged not worth attempting blind
  in this session. It remains a legitimate, valuable follow-up if pursued as its own dedicated task
  with room to iterate against a real gameTestServer run.

## Manual multiplayer verification checklist (for the user)

Everything above was verified by direct code/data inspection and automated tests where the
architecture allowed it. The following still needs a real multi-station in-game pass:

- [ ] Two different accounts start sessions in **different** arenas simultaneously (e.g. Library
      FIRE + CCS_FIRE) — confirm neither disturbs the other.
- [ ] Two different accounts attempt the **same** arena (e.g. both try New Sim Building 2.0) —
      confirm the second gets "that building is currently in use" and the first is undisturbed.
- [ ] A player collects the 3 extinguishers from Sgt. Reyes's room in the Academy, then completes
      Room 4 and gets deployed into New Sim Building 2.0 — confirm they still end up with exactly 3
      fresh extinguishers (not 6, not their old worn ones).
- [ ] A player disconnects mid-check-in (before `/bfp checkout`) — confirm `/bfp sessions list`
      shows their row as closed (`disconnected`), not stuck `active`, shortly after.
  - A player disconnects while crouched under cover during an earthquake drill — no way to directly
    observe the fixed leak from outside the game, but nothing should behave differently; this is a
    "doesn't regress" check, not a new visible behavior.
- [ ] A `/register` burst — have several people register within the same few seconds — confirm none
      of them see unusually long delays or errors.

## Explicitly deferred (not attempted this pass)

Same categories as the prior pass's log, still out of scope: multiple physical building instances
or full per-player world instancing (needed for true N-simultaneous-students-per-scenario-type
scaling beyond 3), a NeoForge GameTest suite (investigated above, not attempted), and the
already-tracked `HazardSpec` hazard-prop migration backlog.
