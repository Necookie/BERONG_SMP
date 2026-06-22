# BerongSMP — Master Implementation Plan

> **Thesis:** "Minecraft as a Platform for AI-Enhanced Disaster Risk Simulation and Adaptive Educational Preparedness"
> **Last updated:** 2026-06-21 — All phases complete
> **Update this file** whenever a phase is completed or a decision changes.

---

## Architecture Overview

```
[Student Player]
      │ /register <name> <id> <section>
      ▼
[Minecraft Mod — NeoForge]
  ├── SimulationEventLogger (buffers per-tick + action events)
  ├── TursoClient (HTTP REST writes to Turso on sim end)
  └── In-game adaptive feedback (computed at sim end, sent as chat)
      │
      ▼ HTTP (batch flush on session end)
[Turso — libSQL Cloud DB]
  ├── sessions table (identity + scores + event_log JSON)
  └── ← groupmate writes prep_level + confidence after RF run
      │
      ├──────────────────────────────────────────────┐
      ▼                                              ▼
[Cloudflare Workers — Dashboard]          [Python RF Script — local]
  Astro SSR + @libsql/client/web            sklearn RandomForestClassifier
  Reads sessions, shows event logs          Reads events → features → predict
  Displays prep_level when available        Writes prep_level + confidence back
      │
      ▼
[BFP Officers / Teachers]
  Dashboard: session list, roster, detail, BFP notes
```

---

## Phase Status

| Phase | Name | Status |
|---|---|---|
| 1 | Data Foundation (mod side) | `[x] done` |
| 2 | Dashboard Live Data | `[x] done` |
| 3 | Synthetic Data Generator | `[x] done` |
| 4 | Thesis Extras | `[x] done` |
| 5 | ML Telemetry Contract | `[ ] not started` |

Update status to `[~] in progress` or `[x] done` as work progresses.

---

## Phase 1 — Data Foundation (Mod)

**Goal:** Every student session produces a rich event log in Turso that the dashboard and RF model can consume.

**Key deliverables:**
- `/register <full_name> <student_id> <section>` command (any player)
- `student_id TEXT`, `section TEXT`, `event_log TEXT` columns added to Turso `sessions` table
- `SimulationEventLogger` class — buffers `SimEvent` records during sim, serializes to JSON on end
- `SimRoom` enum — bounding boxes relative to `SIM_POS` mapping player XYZ → room name
- Registration gate: lobby buttons blocked until `/register` is called
- `RegisteredPlayerData` saved per UUID via `SavedData` (persists across reconnects)

**Event types logged:**
```
PLAYER_TICK     — every 20 ticks: x, y, z, room_id, nearest_fire_dist
EXT_PIN_PULL    — timestamp offset when pin pulled (decision delay)
EXT_SPRAY       — hit_fire (bool), distance_to_fire, fire_blocks_in_fov
FIRE_OUT        — fire_pos, time_since_spawn_ms
FIRE_SPREAD     — new fire placed: pos, total_spread_count
DAMAGE_TAKEN    — nausea/air drain applied: proximity_to_fire
SIM_START       — sim_type, magnitude (if quake)
SIM_END         — fires_extinguished, score, passed
```

**See full brief:** `docs/instructions/phase-1-data-foundation.md`

---

## Phase 2 — Dashboard Live Data

**Goal:** Replace all mock data in the dashboard with real Turso queries.

**Key deliverables:**
- `@libsql/client` installed in `apps/dashboard`
- `apps/dashboard/src/lib/db.ts` — Turso client from Cloudflare env vars
- `apps/dashboard/src/lib/queries.ts` — typed query functions
- All 4 pages wired to live data (Overview, Sessions list, Session detail, Roster)
- Session detail page shows `event_log` JSON rendered in existing `EventLog.tsx` terminal
- `TURSO_URL` + `TURSO_TOKEN` in `wrangler.toml` vars + `.dev.vars` (gitignored)
- `mockData.ts` renamed to `mockData.fallback.ts` (kept for dev without creds)

**See full brief:** `docs/instructions/phase-2-dashboard-live-data.md`

---

## Phase 3 — Synthetic Data Generator

**Goal:** Generate realistic labeled session data so the groupmate can train the RF classifier before real users run sessions.

**Key deliverables:**
- `scripts/generate_synthetic_data.py` — writes 60+ sessions to Turso
- Profiles: 20 HIGH (fast pin pull, accurate sprays, efficient path), 20 MODERATE, 20 LOW (slow, inaccurate, erratic)
- Each session gets a realistic `event_log` JSON matching the Phase 1 schema
- `prep_level` pre-labeled (HIGH/MODERATE/LOW) so groupmate has training targets
- `README` in `scripts/` explaining how to run and what features map to labels

**Computed features (for groupmate's RF):**
| Feature | Source |
|---|---|
| `decision_delay_s` | `EXT_PIN_PULL.t_offset - SIM_START.t_offset` |
| `spray_accuracy` | `EXT_SPRAY hits / total EXT_SPRAY` |
| `path_efficiency` | `straight_dist / cumulative_dist` from PLAYER_TICK |
| `hazard_proximity_ratio` | `ticks where nearest_fire_dist < 3 / total ticks` |
| `interaction_frequency` | `EXT_SPRAY count / duration_s` |
| `panic_proxy` | variance of `sqrt(dx²+dz²)` between PLAYER_TICK rows |

**See full brief:** `docs/instructions/phase-3-synthetic-data.md`

---

## Phase 4 — Thesis Extras

**Goal:** Close the loop — adaptive feedback, richer dashboard, BFP validation.

**Key deliverables:**
- **Adaptive in-game feedback** (Java, no API): at sim end, compute 4 metrics from buffered events → send targeted chat messages
- **Cohort view** on Dashboard Roster: filter/group by `section`
- **BFP validation notes**: text input on session detail page → writes `bfp_notes` to Turso
- **Confidence score display**: if `prep_level` + `confidence` columns populated by groupmate's RF → show on detail page
- **Pre/Post survey** (deferred — future phase)

**In-game feedback logic (no external API):**
```java
// computed from SimulationEventLogger buffer at endSimulation()
if (sprayAccuracy < 0.40) → "§cAim at the base of the flame — not the smoke above it."
if (decisionDelay > 30)   → "§ePull the pin as soon as you locate the extinguisher."
if (panicProxy > threshold) → "§eStay calm — move deliberately toward the exit."
if (pathEfficiency < 0.50) → "§eNavigate directly — avoid backtracking in smoke."
```

**See full brief:** `docs/instructions/phase-4-thesis-extras.md`

---

---

## Phase 5 — ML Telemetry Contract

**Goal:** Emit the full per-tick + event stream required by `telemetry_contract.md` v1.1 so the MiDRR-Classifier can compute its 6 features and train the preparedness model.

**Key deliverables:**
- [x] `FireAlarmBlock` — wall-mounted red alarm switch; activates during FIRE sim; logs `fire_alarm_activate`
- [x] `AssemblyZone` — green particle force-field border outside library; detects player arrival → `assembly_area_reached`
- [x] `TelemetryCsvWriter` — writes `run/telemetry/gameplay_logs_<date>.csv` + `sessions_<date>.csv` + `map_metadata.json`
- [x] `session_start` / `session_end` contract events with `end_reason` (`assembly_reached` / `injured` / `timeout`)
- [x] Per-tick `move` sampler (10 Hz) with `hazard_distance` (nearest fire block or epicenter distance)
- [x] `extinguisher_use` event with `nearby_player_count` (5-block radius scan)
- [x] `door_open` event via `PlayerInteractEvent.RightClickBlock`
- [x] `emergency_exit` zone check (AABB near library doors, placeholder → tune in-game)
- [ ] `map_metadata.json` (one-time static file, placeholder coords → tune after runServer)

**Contract reference:** `telemetry_contract.md` (v1.1) in repo root.
**See full brief:** `docs/instructions/phase-5-telemetry-ml.md`

**Telemetry output location:** `run/telemetry/` (not committed — add to `.gitignore`)

**Coordinate note:** All zone/exit/alarm positions in `AssemblyZone.java` and `map_metadata.json` are PLACEHOLDER. Walk the LSPU library structure in-game with `./gradlew runServer` + F3, note the real coordinates, and update both files before sending data to the ML team.

---

## Cross-Cutting Decisions

| Decision | Choice | Reason |
|---|---|---|
| Event storage | JSON blob in `event_log` column | One column, one HTTP call on flush; easy to parse in JS |
| RF runtime | Python script (local/cron) | Cloudflare Workers can't run sklearn; groupmate runs it separately |
| In-game feedback | Java at sim end, no API | No latency, no external dep; upgrade to API in Phase 4 if needed |
| Dashboard backend | Cloudflare Workers only | No separate server; `@libsql/client/web` queries Turso directly |
| Student registration | `/register` (self-service) | Students identify themselves; OP not required for each student |
| OP auth | Existing `/bfp login <pin>` | Sufficient for thesis classroom setting |

---

## Turso Schema (target after Phase 1)

```sql
CREATE TABLE sessions (
  id                  INTEGER PRIMARY KEY AUTOINCREMENT,
  student_name        TEXT NOT NULL,
  student_id          TEXT,                    -- school ID e.g. 2021-12345
  section             TEXT,                    -- e.g. BSIT-3A
  station_account     TEXT NOT NULL,
  account_uuid        TEXT NOT NULL,
  start_time          TEXT NOT NULL,
  end_time            TEXT,
  status              TEXT DEFAULT 'active',   -- active | completed | aborted
  tutorial_completed  INTEGER DEFAULT 0,
  tutorial_duration_s INTEGER,
  simulation_type     TEXT,                    -- FIRE | EARTHQUAKE
  simulation_score    INTEGER DEFAULT 0,
  passed              INTEGER DEFAULT 0,
  event_log           TEXT,                    -- JSON array of SimEvent objects
  prep_level          TEXT,                    -- HIGH | MODERATE | LOW (written by RF script)
  confidence          REAL,                    -- 0.0-1.0 RF confidence
  bfp_notes           TEXT,                    -- BFP officer validation notes
  notes               TEXT
);
```

---

## Repo Layout (new files)

```
berongsmp-template-26.1.2/
├── docs/
│   ├── major_plan.md                         ← this file
│   └── instructions/
│       ├── phase-1-data-foundation.md
│       ├── phase-2-dashboard-live-data.md
│       ├── phase-3-synthetic-data.md
│       └── phase-4-thesis-extras.md
├── scripts/
│   └── generate_synthetic_data.py            ← Phase 3
└── src/main/java/net/necookie/disastersim/
    ├── command/ModCommands.java               ← add /register
    ├── world/
    │   ├── SimulationEventLogger.java         ← NEW Phase 1
    │   ├── SimRoom.java                       ← NEW Phase 1
    │   └── SimulationSession.java             ← add event buffer
    ├── registration/
    │   ├── RegisteredPlayerData.java          ← NEW Phase 1 (SavedData)
    │   └── RegistrationManager.java           ← NEW Phase 1
    └── session/TursoClient.java               ← add schema migration for new cols
```
