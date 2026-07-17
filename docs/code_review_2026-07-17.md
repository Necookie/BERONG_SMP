# Comprehensive Codebase Review — 2026-07-17

Full-codebase review (architecture, correctness, performance, security, concurrency) performed by
Claude (Fable 5). Every finding below was verified against the actual source; anything that could
not be confirmed from code alone is explicitly labeled **hypothesis**.

Scope: all 298 Java files under `src/main/java`, the 5 unit tests, `build.gradle`, `Config`,
and the docs. Deep reads: `SimulationManager`, `TursoClient`, `SessionManager`, `AuthManager`,
`PasswordHasher`, `RegistrationCommands`, `BfpAdminCommands`, `TelemetryCsvWriter`,
`SimulationSession`, `HazardManager`, `TickScheduler`, the three scenario tickers, `FireEffects`,
`EarthquakeEffects`, `SafetyDeviceManager`, `LobbyManager`, `BerongSMP`, `SchemLoader`,
`AcademyManager`, `CruzRoomManager`, `ReyesRoomManager`, `MorfeRoomManager`,
`DropAndRollManager`, `DuckCoverHoldManager`, `AbstractExtinguisherItem`, `TutorialManager`,
`NewSimScoring`, `AcademySavedData`, `PlayerLifecycleRegistry`, `SimulationCommands`,
`ModCommands`, `StudentSession`, `SimulationEventLogger`.

---

## 1. Architectural Overview

BerongSMP is a NeoForge 26.1.2 server-authoritative minigame mod with four layers:

1. **Registry layer** (`registry/`) — deferred registration facades (`ModBlocks` + 9 package-private
   domain registrars, `ModItems`, `ModCreativeTabs`, …). Purely declarative; order-sensitive by
   design and documented as such.
2. **Content layer** (`block/`, `item/`, `entity/`, `common/hazard/*Block`) — ~180 blocks and ~15
   items, most inheriting from a small set of shared bases (`HorizontalFacingBlock`,
   `HazardBlock`/`HazardFacingBlock`, `AbstractExtinguisherItem`). A data-driven alternative
   (`HazardSpec`/`SimpleHazardBlock`) exists for 8 of 85 hazard props.
3. **Game-flow layer** — three independent state-machine subsystems that all hang off one server
   tick event: the legacy tutorial (`tutorial/`), the Academy (`academy/` — 4 room managers +
   dialogue sequencer + guardrails), and the graded simulations (`common/simulation/` —
   `SimulationManager` dispatching to `LegacyFireTicker` / `NewSim2FireTicker` /
   `EarthquakeTicker`, plus `HazardManager`, `FireEffects`, `EarthquakeEffects`,
   `SafetyDeviceManager`). Cross-cutting helpers: `TickScheduler` (handler registry + one-shot
   delayed tasks), `PlayerLifecycleRegistry` (login/logout rollback hooks).
4. **Persistence/telemetry layer** — `TursoClient` (raw HTTP to libSQL pipeline API),
   `SessionManager`/`StudentSession` (per-station sessions), `AuthManager`/`PasswordHasher`
   (accounts), `TelemetryCsvWriter` (local CSV per telemetry contract v1.2),
   world-attached `SavedData` (`TutorialSavedData`, `AcademySavedData`, `RegisteredPlayerData`).

**Data flow of a graded run:** lobby button → gates (`LobbyManager`) → `SimulationManager.startSimulation`
(re-place all buildings, scan hazards, arm 5, teleport, issue items) → per-tick: telemetry sampling
(20 Hz move ticks buffered in-memory), scenario ticker, exit/assembly checks, HUD packet →
`endSimulation` → `NewSimScoring` → one Turso `UPDATE` carrying `event_log` JSON +
`move_log_csv` + `fire_log_csv` blobs → Morfe debrief.

**Dominant conventions:** static-utility managers with `ConcurrentHashMap<UUID, …>` transient
state; server-thread confinement for all world access; async-with-server-re-entry only for auth;
class-load-forcing `bootstrap()` for tick handlers. The style is consistent and heavily documented,
but almost everything is static and world-coupled, which is the root of both the testability gap
and the multi-session limitations below.

---

## 2. Findings (ranked by impact)

### C1 — Every session start/end re-places ALL four buildings, destroying concurrent sessions and freezing the server — **Critical**

`SimulationManager.startSimulation` (line ~180) and `endSimulation` (line ~452) both do
`for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());` over all 4 structures.
`SchemLoader.place` rewrites every block of each schematic synchronously on the server thread and
`placeEntities` **discards every non-player entity in the footprint**.

Why it's a problem:
- **Concurrent sessions are corrupted.** Player A is mid-`NEW_SIM_BUILDING2_FIRE`; player B starts
  (or ends, or logs out of) any simulation → A's building is rewritten: armed hazards reset to
  their default state, all fire blocks removed, `activeFireSources`/`armedHazards` in A's session
  now point at clean blocks. A's prevention phase becomes unwinnable/meaningless with no error.
  The classroom deployment model (multiple stations) makes this a normal event, not an edge case.
- **Lag spike.** Four full schematic placements + entity discard/respawn + a 97k-block hazard scan
  (`HazardManager.scanHazardProps` over the NSB2 arena: 40×111×22) run synchronously inside a
  `synchronized` static method on the tick thread. Every session start/end (including every
  mid-sim logout, via `onPlayerLogout → endSimulation`) stalls the whole server for the duration.

Fix (short-term): place only the building the session actually uses, and track arena occupancy —
refuse (or queue) a second session in an occupied arena while allowing different arenas to run
concurrently:

```java
// SimulationManager
private static final Map<SimulationState, UUID> arenaOccupants = new EnumMap<>(SimulationState.class);

public static synchronized void startSimulation(ServerPlayer player, SimulationState state, double magnitude) {
    UUID occupant = arenaOccupants.get(arenaKey(state));
    if (occupant != null && activeSessions.containsKey(occupant)) {
        player.sendSystemMessage(Component.literal("§cThat building is in use — try again in a moment."));
        return;
    }
    ...
    buildingFor(state).place(level, posFor(state));   // only this arena
    arenaOccupants.put(arenaKey(state), uuid);
}
```

Long-term: restore arenas incrementally (N blocks per tick via `TickScheduler`) so restoration never
blocks a tick, and only restore the diff (blocks actually changed during the session) instead of the
whole schematic.

### C2 — Blocking Turso HTTP calls on the server thread — **Critical**

`TursoClient.insert()` and `query()` block up to `TIMEOUT_SECONDS = 10`. Confirmed server-thread
call sites:

- `SessionManager.checkin` (`TursoClient.insert`, `SessionManager.java:97`) — called on the server
  thread from `/bfp checkin` and from the `/register`//`/login` success callbacks (which
  `AuthManager` deliberately re-enters via `server.execute`). So the *login path that was made
  async* still ends with a blocking INSERT on the server thread.
- `RegistrationCommands.doHistory` (`/history`), and in `BfpAdminCommands`: `sessions list`,
  `search`, `stats`, `today`, `export`, `student`, `user`, `user delete` — all synchronous
  `TursoClient.query` on the command (server) thread.
- `SessionManager.findActiveSessionRowId` (currently only a fallback).

Real-world impact: with a slow or offline Turso endpoint, every `/register`, `/login`, or `/bfp`
query freezes the entire server — all stations — for up to 10 seconds. In a classroom where 20
students `/register` at the start of a period, that's minutes of cumulative freeze exactly at the
moment of peak load.

Fix: give `TursoClient` an async query API and route command replies back via `server.execute`:

```java
public static CompletableFuture<JsonArray> queryAsync(String sql, Object... args) {
    if (!ready) return CompletableFuture.completedFuture(new JsonArray());
    return CompletableFuture.supplyAsync(() -> parseRows(query(sql, args)), writeExecutor);
}

// call site
TursoClient.queryAsync(SQL, username).thenAccept(rows ->
    server.execute(() -> reply(ctx, rows)));
```

`checkin` should thread its INSERT through the same executor (the row ID can be delivered to the
`StudentSession` via callback; nothing needs it synchronously — `endSimulation` already targets the
row by `account_uuid` subquery, not by ID).

### H1 — `/sim_fire`, `/sim_earthquake`, `/sim_stop`, `/sim_status` have no permission gate — **High**

`SimulationCommands.register` attaches `.requires(LEVEL_GAMEMASTERS)` to `sim_magnitude`,
`sim_list`, `sim_freeze`, `sim_unfreeze`, `sim_time`, `sim_scan_hazards` — but **not** to
`sim_fire`, `sim_earthquake`, `sim_stop`, or `sim_status`. CLAUDE.md documents all of these as
"OP level 2", so this is a drift bug, not a decision.

Impact: any player can (a) bypass the Academy-certification gate the whole product is built around
by typing `/sim_fire new_sim_building2`, (b) trigger C1's building re-placement at will, griefing
every active run on the server, (c) skip registration/session gating entirely.

Fix: one line per command —
`.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))` on all four roots
(`sim_status` for self can stay open if desired).

### H2 — Passwords and the admin PIN travel through chat commands — **High** (shared-station context)

`/register <username> <password> …`, `/login <username> <password>`, `/bfp login <pin>`.

Two exposure paths:
1. **Client chat history (confirmed by design of MC's chat screen):** the sent-message history at a
   shared station is recallable with ↑ by the *next* student. Station sharing is this project's
   explicit deployment model, so student B can recover student A's password minutes later.
2. **Server logs (hypothesis — verify on your NeoForge build):** vanilla dedicated servers
   historically log `<player> issued server command: /login user pass` to console/log files. If
   26.1.2 still does, plaintext passwords are being written to `logs/latest.log` on MCServerHost.

Mitigations, in order of practicality: verify+suppress the command-issued log line for these three
commands; document that stations should relaunch the client between students (clears history);
longer-term, move credential entry off chat (e.g., a server-driven sign/anvil/book GUI, or
instructor-issued one-time codes). Also note `entered.equals(correct)` for the PIN is not
constant-time — irrelevant at chat latency, but `MessageDigest.isEqual` costs nothing.

### H3 — BFP admin grant and test-bypass survive student turnover at a station — **High**

`BfpAdminCommands.bfpAuthorized` and `testBypassActive` are only cleared by
`clearAuthorizations()` on server start. Neither is hooked to `PlayerLifecycleRegistry` logout, and
neither is cleared by `/logout` or `SessionManager.checkin`. On shared stations the UUID is the
station, not the person: an instructor who `/bfp login`s at station3 and walks away leaves full
admin (score overrides, DB deletes, `/bfp user delete`) and gate-bypass to every subsequent student
at that station.

Fix:

```java
// BfpAdminCommands — static init
PlayerLifecycleRegistry.registerLogoutHook(p -> {
    bfpAuthorized.remove(p.getUUID());
    testBypassActive.remove(p.getUUID());
});
```

and clear both in `SessionManager.checkin` (new student sitting down) for the stays-connected case.

### H4 — Pass/fail is computed two different, disagreeing ways — **High** (data integrity)

- `SimulationManager.endSimulation` (line ~364): `passed = finalScore >= PASS_THRESHOLD_FIRE`
  where `finalScore = min(100, firesExtinguished * 2)`. Default threshold 5 → **3 fires passes**
  (score 6 ≥ 5). This value goes to Turso.
- `SessionManager.onSimulationEnd` (line ~190): `passed = firesExtinguished >= PASS_THRESHOLD_FIRE`
  → **5 fires required**. This value goes to the in-memory `StudentSession` (and `/bfp session info`).

The config comment ("Fires extinguished required to pass") says the second is the intent, so the
DB is recording wrong passes. `onSimulationEnd` also applies this legacy formula to
`NEW_SIM_BUILDING2_FIRE` sessions, ignoring `NewSimScoring` entirely.

Fix: one scoring function, used by both:

```java
// SimulationScoring (new, pure — also unit-testable)
static int fireScore(int extinguished) { return Math.min(100, extinguished * 2); }
static boolean firePassed(int extinguished) { return extinguished >= Config.PASS_THRESHOLD_FIRE.get(); }
```

and have `onSimulationEnd` accept the already-computed score/passed from `endSimulation` instead of
recomputing.

### H5 — Per-tick world-scan hotspots — **High** (performance)

Confirmed hot paths, all on the tick thread:

| Scan | Cost | Frequency | Where |
|---|---|---|---|
| `nearestCCSHazardDistance` | 31×31×11 ≈ **10,600 block reads** | **every tick** per CCS_FIRE player (drives the 20 Hz `move_tick` row) — ~212k reads/s/player | `SimulationManager.java:712` |
| `cleanupFireOutsideBounds` | NSB2: 46×28×117 ≈ **150k reads** | every 40 ticks per fire session | `FireEffects.java:163`, called from both fire tickers |
| `clearFireInArena` | full arena + margin | **every 20 ticks** per quake session | `EarthquakeTicker.java:47` |
| `SafetyDeviceManager.scan` | NSB2: ~(spanX·2)×(spanZ·2)×h ≈ **500k reads** | once per session (first 40-tick cycle) | `SafetyDeviceManager.java:149` |
| `nearestFireDistance` per device | 21×21×11 ≈ 4.8k reads × N detectors/sprinklers | every 40 ticks | `SafetyDeviceManager.java:119,135` (memo doesn't help — every device is a different position) |

Note the contrast: `nearestFireDistance` was carefully memoised, but the CCS variant that is
*strictly heavier and called strictly more often* was not.

Fixes:
- CCS: don't scan at all — the session already caches `computerPositions`; distance-to-nearest
  burning computer is a ~40-element loop like `nearestArmedHazardDistance`. Add fire blocks via
  `activeFireSources` (already maintained) instead of scanning the world.
- Fire containment: iterate `activeFireSources` (plus a per-placement bounds check when igniting)
  instead of sweeping the arena for stray fire. `FireEffects.simulateFire` already refuses to place
  outside bounds, so the sweep only exists to catch *vanilla* spread — cheaper to place fire with
  `doFireTick`-safe age or maintain the source set as the single source of truth.
- `SafetyDeviceManager.scan`: the `spanX * 2` bound looks unintentional (**hypothesis** — the arena
  spans are already full widths, not radii, everywhere else); halving it cuts the NSB2 scan 4×.
  Also cache `nearestFireDistance` results per cycle by scanning fire once and testing devices
  against the fire list, not the world per device.

### M1 — Turso shutdown race loses final writes; executor/client teardown unsafe — **Medium**

`SessionManager.shutdown()` enqueues "mark aborted" writes, then immediately calls
`TursoClient.shutdown()`, which sets `ready=false`, calls `writeExecutor.shutdown()` **and nulls
`httpClient`**. Queued tasks still run after `shutdown()` (that's its contract) but now read a null
`httpClient` → NPE, write silently lost (`TursoClient.java:72-79`). Separately, `executeAsync`
checks `ready` then submits — a task submitted during teardown gets `RejectedExecutionException`.

Fix:

```java
public static void shutdown() {
    ready = false;
    if (writeExecutor != null) {
        writeExecutor.shutdown();
        try { writeExecutor.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        writeExecutor = null;
    }
    httpClient = null; // only after the queue has drained
}
```

and wrap the `runAsync` submit in try/catch for `RejectedExecutionException`.

### M2 — TickScheduler has no exception isolation — **Medium**

`TickScheduler.tick` (`TickScheduler.java:49-63`) runs every registered handler and every delayed
task bare. One uncaught exception from any of the 6+ handlers (TutorialManager, DropAndRoll,
DuckCoverHold, Academy, SafetyDevice, Auth hooks) propagates out of
`SimulationManager.onServerTick` and crashes the server. These handlers do world reads keyed by
possibly-stale positions — exactly the kind of code that throws once in a blue moon.

Fix: per-handler try/catch with a throttled error log; same for delayed tasks. Cheap, prevents a
whole-class of "one bad tick kills the class period" incidents.

### M3 — Unbounded in-session telemetry buffers → one giant UPDATE parameter — **Medium**

`SimulationSession.csvBuffer` grows one row per tick at 20 Hz (`SimulationManager.tickTelemetry`
deliberately removed the modulo gate). A default NSB2 run is 6,600 ticks ≈ 6,600 rows ≈ 0.5–1 MB;
`/sim_time add` (or a generous config) can push this into multi-MB territory. It's sent as a single
bound parameter in one `UPDATE` (`endSimulation`). **Hypothesis:** large payloads will hit Turso's
HTTP body limit and the whole session write (score, event_log, everything in that statement) fails
as one unit, surfacing only as a `[TursoClient] Write failed` warn.

Fix: cap the buffer (e.g., stop appending past N MB with a truncation marker), or downsample move
ticks for the DB blob (the local CSV keeps full fidelity), or split blob columns into their own
UPDATE statements so a too-big `move_log_csv` can't take `simulation_score` down with it.

### M4 — `/sim_time set/add` silently corrupts the telemetry time axis — **Medium**

`SimulationSession.setTimerTicks` changes `timerTicks` without touching `initialTimerTicks`, so
`elapsedTicks() = initialTimerTicks - timerTicks` goes wrong — negative if time is added above the
original duration. Every subsequent `move_tick`/event row and `duration_ticks` inherits the bogus
`t`. Two other spots still use the exact anti-pattern the codebase already documented as wrong:
`DuckCoverHoldManager.onHoldAchieved` (`Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()`)
and `LegacyFireTicker.tickCcsFireNarrative`.

Fix: `setTimerTicks` should adjust `initialTimerTicks` by the same delta (preserving elapsed), and
the two stragglers should call `session.elapsedSeconds()`.

### M5 — DB write model: fire-and-forget with row-targeting by subquery — **Medium**

- Writes have no retry/journal. A transient outage during `endSimulation` loses the run's score
  permanently (local CSVs survive, but nothing reconciles them into Turso later).
- `endSimulation`'s `UPDATE … WHERE id=(SELECT id FROM sessions WHERE account_uuid=? AND
  status='active' ORDER BY id DESC LIMIT 1)` depends on `checkin`'s INSERT having succeeded; if
  Turso was down at checkin (the offline-registration path warns and continues), the session row
  never exists and the final UPDATE silently updates nothing.
- Crash-orphaned `status='active'` rows accumulate (only `shutdown()` marks aborts, and only for
  in-memory sessions) and can shadow row-targeting after a crash.

Fix direction: journal failed writes to `run/telemetry/pending_writes.jsonl` and replay on next
init; on `init()`, mark stale `active` rows (older than a day) `aborted`.

### M6 — AuthManager rate-limit races and user enumeration — **Medium-Low**

- `loginFailures` `long[]` is read/reset on the server thread and incremented on `AUTH_EXECUTOR`
  threads with no synchronization (`AuthManager.java:144-186`) — a data race (JMM-visible), and
  concurrent attempts can exceed the 5-try budget (checked before dispatch, recorded after).
- Distinct `"not_found"` vs `"bad_password"` errors enumerate valid usernames.
- No max password length: PBKDF2 at 210k iterations over an arbitrarily long chat string is a
  cheap CPU-burn primitive (bounded by chat packet size; minor).

Fixes: `AtomicIntegerArray`/synchronized record, count the attempt at dispatch time, collapse both
errors into "invalid username or password", clamp password length (e.g., 128).

### M7 — Stale `ServerPlayer` reference held for the session lifetime — **Medium-Low (smell with teeth)**

`SimulationSession.player` is a final reference captured at start. After death+respawn the entity
is replaced; the code mostly survives because death ends the session, but `sim_list`,
`endSimulation`'s `(ServerLevel) playerForCsv.level()`, and `SessionManager.onSimulationEnd` all
operate on the possibly-removed instance. The codebase's own convention elsewhere
(`TickScheduler.scheduleOnce` docs) is "re-fetch by UUID at use time" — the session class predates
that rule. Fix: store the UUID, resolve via `server.getPlayerList().getPlayer(uuid)` on demand.

### Low-severity / smells (grouped)

| # | Finding | Where | Note |
|---|---|---|---|
| L1 | `/bfp sessions stats` counts only `FIRE`/`EARTHQUAKE`, silently excluding `CCS_*` and `NEW_SIM_BUILDING2_FIRE` from the breakdown | `BfpAdminCommands.java:686-687` | Misleading instructor-facing stats |
| L2 | Date-stamped CSV writers open once and never roll past midnight | `TelemetryCsvWriter.ensure*Writer` | Evening sessions land in yesterday's file |
| L3 | `sessionId` = 8 hex chars of a UUID (32 bits) | `SimulationSession.java:62` | Birthday collision ≈1% at ~9.3k sessions; telemetry joins on it |
| L4 | `Double`/`Float` args serialized as `"type":"text"` | `TursoClient.buildBody` | `confidence REAL` stored as text; SQLite coerces, but comparisons/AVG may surprise |
| L5 | Template leftovers: `LOG_DIRT_BLOCK`, `MAGIC_NUMBER`, `ITEM_STRINGS` | `Config.java:13-27` | Dead config surface; delete |
| L6 | Stale comment: "CCS … after 3-CCW placement" but placement is 0 rotations | `SimulationManager.java:126` | Docs drift |
| L7 | `lastWrongToolWarningTick` never pruned | `AbstractExtinguisherItem.java:102` | Unbounded only in theory (one entry per UUID ever seen) |
| L8 | `lastStartingLineNudgeTick` is global, not per-player | `CruzRoomManager.java:211` | Two simultaneous Room-1 trainees share the throttle |
| L9 | `PlayerLifecycleRegistry.registerLoginHook(ClearsOnLogout)` — login hooks typed/named `onLogout` | `PlayerLifecycleRegistry.java:42-56` | Naming smell; rename interface to `PlayerHook` |
| L10 | Single-slot fire-scan memo is correctness-fragile (depends on callers for the same position being adjacent in time) | `SimulationManager.java:88-90` | Works today; document the invariant or key a small map by position |
| L11 | `NewSimScoring` penalizes "ring the alarm" even on a perfect all-prevented run where no fire ever existed | `NewSimScoring.java:61-65` | Design nit — weak-area feedback reads as wrong to a perfect student |
| L12 | `SchemLoader.place` catches `Exception` and logs only `getMessage()` | `SchemLoader.java:171-174` | Loses stack traces for the hairiest parser in the mod |

### Testing gaps — **Medium**

Five real unit tests exist (`PasswordHasher`, `TursoClient.parseRows`, three enum tests) and they
test the right kind of thing (pure logic). But: no coverage for `NewSimScoring`/`AcademyScoring`
(pure, high-stakes, trivially testable), none for the pass/fail logic (which would have caught H4),
none for `SimulationSession.tickFirePhase`/`tickQuakePhase` (pure state machines), and no gametests
at all despite `gameTestServer` being configured in `build.gradle`. The static/world-coupled style
makes everything else untestable without refactoring — a reason to extract more pure logic, not to
skip tests.

---

## 3. Top 10 Highest-Priority Improvements

1. **Scope building placement per arena + arena occupancy guard** (C1) — the one change that makes
   multi-station classroom use actually correct.
2. **Make all Turso calls on the server thread async** (C2) — `checkin` first, then `/history` and
   `/bfp` queries.
3. **Add permission gates to `/sim_fire`, `/sim_earthquake`, `/sim_stop`** (H1) — one-line fixes.
4. **Unify fire pass/score logic in one pure class** (H4) — and decide which semantics the
   threshold means; add a unit test.
5. **Clear `bfpAuthorized`/`testBypassActive` on logout and on checkin** (H3).
6. **Exception-isolate TickScheduler handlers and delayed tasks** (M2).
7. **Kill the per-tick CCS hazard scan; derive distance from cached positions** (H5).
8. **Fix TursoClient shutdown ordering (drain before teardown)** (M1).
9. **Cap/downsample the in-memory telemetry blobs and split the final UPDATE** (M3).
10. **Address the credential exposure at shared stations** (H2) — at minimum verify/suppress
    command logging and document station hygiene.

---

## 4. Scalability Assessment

**Verdict: engineered for one active player per building; degrades sharply beyond that.**

- **Hard correctness ceiling (C1):** concurrent sessions in *any* buildings corrupt each other via
  global re-placement. Until fixed, the real capacity is one simulation at a time server-wide.
- **Per-player tick cost is high:** 20 Hz telemetry with a hazard-distance world scan per row
  (up to ~10.6k block reads/tick for CCS), periodic 75k–150k-block containment sweeps per session,
  plus the Academy's per-player-per-tick room dispatch. A rough budget puts 5+ concurrent CCS_FIRE
  sessions past a 50 ms tick on modest hosting.
- **Academy is single-trainee by design:** Cruz escorts the *first* eligible player found; Reyes's
  three hazard props are shared world blocks — two simultaneous Room-2 trainees fight over the
  same blocks and both get credit/confusion from one player's defuse. Fine for the current
  one-station-at-a-time flow, but it means "scale the classroom" = "serialize students through the
  Academy", which should be an explicit operational rule.
- **DB layer scales fine** for this workload (fire-and-forget writes, 2-thread pool) *except* the
  blocking reads (C2) and the megabyte-blob session write (M3).
- **Memory:** per-session buffers are bounded by session length; static maps are cleaned on logout
  via `PlayerLifecycleRegistry` with only trivial exceptions (L7). No meaningful leak found.

## 5. Maintainability Assessment

**Verdict: far above average for a mod codebase in documentation and convention-consistency; below
average in coupling and testability.**

Strengths: CLAUDE.md + `docs/systems/` are genuinely synchronized with the code; almost every
non-obvious decision carries a why-comment with its bug history; shared bases
(`AbstractExtinguisherItem`, `HazardBlock`, `HorizontalFacingBlock`) keep the 180-block surface
uniform; `PlayerLifecycleRegistry` institutionalized a recurring bug fix; the registrar split kept
a 1,300-line file maintainable without call-site churn.

Weaknesses: everything is static — state, tick handlers, and world access are welded together, so
logic like scoring, phase machines, and gate checks can't be tested or reused without a running
server (H4 is the concrete cost of that: duplicated logic drifted). Duplication of the
"round to 2 decimals + buffer CSV row + log event" telemetry block appears ~10 times across files
and invites drift (a `TelemetryEmitter.emit(session, player, type, …)` helper would collapse it).
Comment volume occasionally substitutes for structure (`CruzRoomManager` is 850 lines of tuning
constants + narrative comments around ~300 lines of logic). The 77 remaining bespoke hazard-prop
classes are acknowledged backlog with a proven replacement (`HazardSpec`) already shipped.

## 6. Production Readiness Score: **5.5 / 10**

For the intended deployment (one supervised MCServerHost instance, shared-station classroom,
one-or-few active runs): the happy path is solid, crash-recovery around logins/logouts is unusually
well handled, and local CSV telemetry gives a durability backstop. What holds it back: any student
can bypass the entire certification system with an ungated command (H1); two simultaneous runs
corrupt each other (C1); a DB hiccup freezes the whole server during the highest-load minute of a
class (C2); and recorded pass/fail doesn't match the documented rule (H4). Items 1–5 of the action
plan below are all small; fixing just those raises this to ~7.5.

## 7. Technical Debt Assessment

- **Deliberate, documented debt (healthy):** 77 hazard blocks awaiting `HazardSpec` migration;
  `ModItems` split deferred; TickScheduler's 4-separate-player-loops note; stale NSB2 exit zones
  flagged in-code and in `map_metadata.json`. This debt is tracked where it lives — best practice.
- **Undocumented drift (the risky kind):** command-permission drift vs. docs (H1); dual scoring
  (H4); stats excluding new scenario types (L1); template config leftovers (L5); stale rotation
  comment (L6). These are all cases where code moved and its mirror didn't.
- **Structural debt:** static/world-coupled managers (blocks testing and multi-arena work);
  telemetry emission duplicated at every call site; `SimulationManager` still owns arena constants,
  spawn scans, telemetry, zone checks, and lifecycle (851 lines after the ticker extraction — the
  extraction pattern should continue: spawn-finding and telemetry sampling are the next candidates).
- **Debt interest is currently low** because one person + docs discipline holds it together, but
  the bus factor is 1 and the docs are the only test suite for most behavior.

## 8. Phased Action Plan

**Immediate (hours, no design work):**
1. Permission-gate `/sim_fire`, `/sim_earthquake`, `/sim_stop` (H1).
2. Clear `bfpAuthorized`/`testBypassActive` on logout + checkin (H3).
3. Unify fire pass/score into one pure helper + unit test (H4).
4. Try/catch around TickScheduler handlers and delayed tasks (M2).
5. Fix `TursoClient.shutdown` drain ordering + rejected-submit guard (M1).
6. `/bfp sessions stats` covers all scenario types (L1); delete template config entries (L5).

**Short-term (days):**
7. Per-arena building placement + occupancy guard; stop re-placing all buildings on logout (C1 core).
8. Async `checkin` and async `/bfp`//`/history` queries (C2).
9. Replace `nearestCCSHazardDistance` world scan with cached-position math; derive fire containment
   from `activeFireSources` (H5).
10. Cap/downsample telemetry blobs; split the final session UPDATE (M3).
11. Fix `/sim_time` elapsed anchor + the two `Config.SIM_DURATION_TICKS` stragglers (M4).
12. Verify/suppress command logging for credentials; document station hygiene (H2).
13. Unit tests for `NewSimScoring`, `AcademyScoring`, `tickFirePhase`/`tickQuakePhase`.

**Long-term (weeks, architectural):**
14. Incremental (per-tick budgeted) arena restoration; diff-based restore.
15. Session refactor: store player UUID not entity; extract telemetry emission into one helper;
    continue carving `SimulationManager` (spawn-finding, zone checks).
16. Multi-trainee Academy: per-player hazard-prop instancing or explicit room reservation.
17. Write-journal + replay for Turso; startup sweep of orphaned `active` rows (M5).
18. Gametests for the three scenario lifecycles (the harness is already configured).
19. Migrate remaining hazard props to `HazardSpec` in batches.

## 9. Notable Strengths

- **Documentation discipline that most professional teams don't achieve** — living architecture
  docs, per-fix rationale comments with reproduction context, and a CLAUDE.md that actually matches
  the code (the few mismatches found are listed above precisely because the baseline is so good).
- **`PasswordHasher` is textbook-correct**: per-hash random salt, 210k-iteration PBKDF2-SHA256,
  constant-time comparison, self-describing encoded format that survives iteration bumps.
- **Thread model is coherent**: server-thread confinement everywhere, with the two genuinely slow
  paths (auth hashing+HTTP, Turso writes) pushed to dedicated daemon executors and re-entering via
  `server.execute` — the remaining violations (C2) are omissions, not misunderstandings.
- **`PlayerLifecycleRegistry`** turned a thrice-recurring bug class (stale per-player state across
  logout/crash) into a registration pattern, with idempotent login-side rollback for crash recovery.
- **Defensive game-flow engineering**: the dialogue sequencer's click-skip gating, Reyes's
  fire-containment sweeps, Cruz's unreachable-path fast-fail, sticky-fire rules — each encodes a
  real observed failure and its fix.
- **Local CSV telemetry as a durability backstop** for the fire-and-forget DB — the right call for
  a research-data product on flaky hosting.
- **`SchemLoader`** handles genuinely obscure Sponge v2/v3 edge cases (nested `Data`, item-frame
  facing math, `block_pos` sanity radius) with the evidence written down next to the code.
- **Tests exist where testing is possible** — small, but aimed at exactly the pure seams
  (`parseRows`, hashing, enum order) the architecture exposes.
