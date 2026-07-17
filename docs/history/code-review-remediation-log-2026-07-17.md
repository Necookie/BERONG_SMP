# Code Review Remediation Log (2026-07-17)

Tracks fixes applied from [`docs/code_review_2026-07-17.md`](../code_review_2026-07-17.md)'s
Immediate + Short-term action items. Frozen log, kept for context but not updated going forward —
see that review for the full findings, severity ranking, and the items deliberately **not**
attempted here (see "Explicitly deferred" at the bottom).

Every fix below compiles clean (`./gradlew compileJava`) and passes the full unit test suite
(`./gradlew test`, 38 tests across 7 classes as of this pass). Anything touching live session
state, concurrent players, or Turso I/O could not be exercised without a running server — those
gaps are called out explicitly and belong on the in-game verification checklist handed to the user
at the end of this pass.

## Fixes

**H1 — `/sim_fire`, `/sim_earthquake`, `/sim_stop` had no permission gate.** Added the same
`Commands.LEVEL_GAMEMASTERS` `.requires()` check every other `/sim_*` command already had.
(`SimulationCommands.java`)

**H3 — BFP admin grant/test-bypass survived station handoff.** `bfpAuthorized`/`testBypassActive`
were only cleared on server restart. Added a `PlayerLifecycleRegistry` logout hook
(`BfpAdminCommands.clearStationAuth`, bootstrapped the same way `AuthManager`/
`DuckCoverHoldManager` force their static block to load) and a call from `SessionManager.checkin`
for the stays-connected case. (`BfpAdminCommands.java`, `BerongSMP.java`, `SessionManager.java`)

**L5 — Dead NeoForge mod-template config entries.** Removed `LOG_DIRT_BLOCK`, `MAGIC_NUMBER`,
`MAGIC_NUMBER_INTRODUCTION`, `ITEM_STRINGS`, and the now-unused `validateItemName` helper.
(`Config.java`)

**L1 — `/bfp sessions stats` excluded CCS and New Sim Building 2.0 runs.** The fire/quake breakdown
only matched `simulation_type='FIRE'`/`'EARTHQUAKE'`; widened to an `IN (...)` list covering all 5
real scenario values. (`BfpAdminCommands.java`)

**M1 — `TursoClient.shutdown()` raced its own write queue.** Nulled `httpClient` immediately after
`writeExecutor.shutdown()`, which only stops new submissions — anything already queued (e.g.
`SessionManager.shutdown()`'s "mark session aborted" writes) ran against a null client and lost the
write. Now awaits drain (5s) before tearing down the client; both `executeAsync`/`silentAlter` guard
their submission against `RejectedExecutionException`. (`TursoClient.java`)

**M2 — `TickScheduler` had no exception isolation.** An uncaught exception from any one registered
handler (Academy, DropAndRoll, DuckCoverHold, SafetyDevice, ...) propagated out of `onServerTick` and
crashed the whole server. Each handler and delayed task now runs in its own try/catch, logged
(throttled to once/10s per handler). (`TickScheduler.java`)

**M4 — `/sim_time` corrupted the telemetry elapsed-time anchor.** `SimulationSession.setTimerTicks`
changed remaining ticks without adjusting `initialTimerTicks`, so `elapsedTicks()`/
`elapsedSeconds()` — the documented anchor for every telemetry `t` value — went wrong (could go
negative) after a GM ran `/sim_time`. Fixed by shifting `initialTimerTicks` by the same clamped
delta. Also fixed the two remaining stragglers still computing elapsed from
`Config.SIM_DURATION_TICKS - timerTicks` directly instead of the session's own anchor:
`DuckCoverHoldManager.onHoldAchieved` and `LegacyFireTicker.tickCcsFireNarrative` (now takes the
session instead of a raw tick count). (`SimulationSession.java`, `DuckCoverHoldManager.java`,
`LegacyFireTicker.java`)

**M6 — Login rate-limit race, username enumeration, unbounded password length.**
`AuthManager.loginAsync`'s `long[]` rate-limit record was written on `AUTH_EXECUTOR` threads and
read/reset on the server thread with no synchronization, and only counted confirmed failures
(recorded after the async lookup returned) rather than dispatched attempts — a burst of rapid
`/login` submissions could all pass the lockout check before any one's result came back. Replaced
with a `synchronized` `LoginAttempts` record whose counter increments at dispatch time. Also:
collapsed `"not_found"`/`"bad_password"` into one `"invalid_credentials"` message (distinguishing
them lets a shared station enumerate registered usernames), added a 128-char password cap checked
before PBKDF2/Turso are touched, and switched `/bfp login`'s PIN comparison to
`MessageDigest.isEqual`. (`AuthManager.java`, `RegistrationCommands.java`, `BfpAdminCommands.java`)

**H4 — Fire pass/score computed two different, disagreeing ways.** `SimulationManager.endSimulation`
checked `score >= passThresholdFire` (i.e. `fires >= threshold/2`, since score is `fires*2`);
`SessionManager.onSimulationEnd` checked `fires >= passThresholdFire` directly. The config comment
("Fires extinguished required to pass") documents the second as intent — the first was certifying
runs on half the required fires, and that wrong value went to Turso. Extracted both the score
formula and the pass rule into a new `SimulationScoring` class (deliberately NeoForge-free — takes
`passThreshold` as a parameter rather than reading `Config` directly, so it's unit-testable outside
a loaded mod environment). `SessionManager.onSimulationEnd` no longer recomputes anything — it takes
the exact `finalScore`/`passed` `SimulationManager` already computed, which also fixes `/bfp session
info` silently showing the wrong score for New Sim Building 2.0 runs (it used to overwrite
`NewSimScoring`'s real result with the legacy fire-only formula). Added `SimulationScoringTest` with
an explicit regression case for the exact bug (3 fires / threshold 5: score 6 ≥ 5 but must still
fail). (`SimulationScoring.java` new, `SimulationManager.java`, `SessionManager.java`,
`SimulationScoringTest.java` new)

**Unit tests for `NewSimScoring`.** Added `NewSimScoringTest` (7 cases: perfect prevention, partial
intervention, timeout failure, assembly-reached partial credit, zero-armed-hazards edge case,
injured-never-passes, score cap). `SimulationSession.tickFirePhase`/`tickQuakePhase` are **not**
covered — constructing a `SimulationSession` calls `Config.SIM_DURATION_TICKS.get()`, and NeoForge's
`ModConfigSpec.get()` throws outside a fully mod-loaded environment, so no plain JUnit test can
touch it. Extracting just the transition rule into a separate pure function was considered and
rejected: the real methods also mutate random-driven aftershock magnitude and the
pending-destruction queue in the same branch, so a hand-extracted copy would either miss that
behavior or duplicate it — recreating the exact two-sources-of-truth drift H4 just fixed. Real
coverage needs either a NeoForge test-config bootstrap harness or an in-game gametest; tracked as a
longer-term item. (`NewSimScoringTest.java` new)

**H5 — Per-tick CCS hazard-distance world scan; `SafetyDeviceManager` over-scanning.**
`nearestCCSHazardDistance` scanned a 31×31×11 box (~10,600 block reads) every tick for every
`CCS_FIRE` player, feeding the 20 Hz `move_tick` row — the heaviest per-tick world read in the mod,
and the one `nearestFireDistance`'s memoisation didn't cover. Replaced with a loop over the
session's already-cached computer positions, checking only which are currently `BURNING` — CCS's
fire only ever originates from a burning computer, so no world scan or fallback is needed (one
documented accuracy trade-off: a computer that ignited nearby vanilla fire and was since
extinguished could leave a residual fire patch this won't see, acceptable for descriptive
telemetry). Also fixed `SafetyDeviceManager.scan`'s device-position scan, which multiplied the
arena's `spanX`/`spanZ` by 2 (every other arena-bounds loop in the mod treats span as the full
extent from origin, not a radius — confirmed against `HazardManager.scanHazardProps`'s own loop),
over-scanning ~3.7× the actual New Sim Building 2.0 footprint. (`SimulationManager.java`,
`SafetyDeviceManager.java`)

**M3 — Unbounded telemetry buffers; single oversized UPDATE.**
`SimulationSession.csvBuffer`/`fireLogBuffer` grew unboundedly at 20 Hz with no cap, sent as a
single bound parameter alongside `simulation_score`/`passed`/`event_log` in one UPDATE — since Turso
write failures fail the whole statement, an oversized blob could take the run's actual score down
with it. `bufferCsvRow`/`bufferFireLogRow` now cap at 3 MB / 512 KB respectively with a truncation
marker (the local `run/telemetry/gameplay_logs_*.csv` always has the full record). Split
`endSimulation`'s single UPDATE into `persistSessionEnd()`'s two statements: core fields
(score/status/event_log) filtered by `status='active'`, and blob columns
(`move_log_csv`/`fire_log_csv`) filtered by `account_uuid + ORDER BY id DESC LIMIT 1` alone — since
these are two independent fire-and-forget writes with no ordering guarantee, the blob write can't
assume `status='active'` still holds by the time it fires. (`SimulationSession.java`,
`SimulationManager.java`)

**C2 — Blocking Turso HTTP calls on the server/command thread.** Added
`TursoClient.queryAsync`/`insertAsync` (same executor writes already use). Converted
`SessionManager.checkin`'s INSERT (previously blocking even when called from an already-async
`/register`/`/login` success callback — `StudentSession.dbRowId` is now `volatile` for the resulting
cross-thread write), `/history`, and every remaining `/bfp` read (`sessions list/search/stats/
today/export`, `student`, `user`, `user delete`) to async, replying via `server.execute(...)` once
the query resolves — the same pattern already established by `/register`/`/login`.
`TursoClient.findActiveSessionRowId` still blocks but has zero callers anywhere in the codebase
(confirmed via search); left as-is. (`TursoClient.java`, `SessionManager.java`, `StudentSession.java`,
`RegistrationCommands.java`, `BfpAdminCommands.java`)

**C1 — Global building re-placement corrupted concurrent sessions.** `startSimulation`/
`endSimulation` re-placed all 4 buildings (Library, SSC, CCS, New Sim Building 2.0) on every single
session start/end/logout regardless of which one the session used — a second player starting or
ending any session would silently rewrite another in-progress session's arena to its clean
schematic state (wiping armed hazards, fire, damage) with no error to either player. Split the old
`BUILDINGS` list into named per-building constants grouped by physical `Arena` (`LIBRARY`: Library +
the co-located SSC decor building; `CCS`; `NEW_SIM_BUILDING2`) — two `SimulationState`s can share an
arena (FIRE/EARTHQUAKE both use Library, CCS_FIRE/CCS_EARTHQUAKE both use CCS), so occupancy is
tracked per-arena via `arenaOccupants: Map<Arena, UUID>`, not per-state. `startSimulation` now places
only its own arena and refuses to start if another still-active session already claims that arena
(self-healing: only blocks while the recorded occupant genuinely still has an active session).
`endSimulation` releases the arena (compare-and-remove) and restores only that same arena. The full
`BUILDINGS` list is now reserved exclusively for the existing `/place_buildings` OP dev command.
**Highest-risk change in this pass** — compiles clean and the unit suite passes, but
`SimulationSession` requires a live `ServerPlayer`/`ServerLevel` to construct, so the actual
concurrent-session behavior needs an in-game multi-station test; see the verification checklist.
(`SimulationManager.java`)

## Explicitly deferred (not attempted this pass)

These require either a design decision only the project owner can make, live-server integration
testing this session couldn't perform, or are large enough architectural efforts to warrant their
own planning pass rather than a bundled fix:

- **Session holds a live `ServerPlayer` reference for its whole lifetime** (M7 in the review) — a
  broad refactor touching every `session.getPlayer()` call site; risky without integration testing.
- **Turso write-journal + replay, startup sweep of orphaned `active` rows** (M5) — durability
  architecture, not a bug fix.
- **Multi-trainee Academy** (Cruz escorts the first eligible player found; Reyes's hazard props are
  shared world blocks) — a real design/scope decision about whether serializing students through
  the Academy is acceptable, not a defect.
- **Gametests for the three scenario lifecycles** — `gameTestServer` is already configured in
  `build.gradle` but unused; writing these needs a running-server iteration loop.
- **Batch-migrating the remaining ~77 hazard props to `HazardSpec`** — already-tracked backlog
  (see `HazardSpecs`'s own javadoc), unrelated in scope to this review.
- **Credential exposure at shared stations** (H2) — mitigations depend on verifying whether this
  NeoForge build logs full command lines (a live-server check only the project owner can run) and
  on a UX decision (accept chat-command login with documented station hygiene, vs. building a
  GUI/one-time-code flow).
