# BerongSMP — Session Summary
> Date: 2026-06-21
> Thesis: "Minecraft as a Platform for AI-Enhanced Disaster Risk Simulation and Adaptive Educational Preparedness"

---

## What Was Built

### Phase 1 — Data Foundation (Mod)
- **`/register <student_id> <section> <full_name>`** — students self-identify before entering simulations
- **`RegisteredPlayerData`** + **`RegistrationManager`** — persists registration across reconnects via `SavedData`
- **`SimulationEventLogger`** — buffers behavioral events (EXT_PIN_PULL, EXT_SPRAY, PLAYER_TICK, FIRE_SPREAD, SIM_START, SIM_END) during simulation, serializes to JSON on end
- **`SimRoom`** enum — maps player XYZ coordinates to named rooms (COMPUTER_LAB, MAIN_HALL, ENTRANCE, etc.)
- **Registration gate** — lobby buttons blocked until `/register` is called
- **Turso schema migration** — added `student_id`, `section`, `event_log`, `prep_level`, `confidence`, `bfp_notes` columns to `sessions` table
- **`TursoClient`** additions — `updateStudentInfo()`, `updateEventLog()`, `silentAlter()` for schema migration

### Phase 2 — Dashboard Live Data (Web)
- Replaced all mock data in the Astro dashboard with live Turso queries
- **`src/lib/db.ts`** — `getDb()` using `@libsql/client/web` (Cloudflare Workers compatible); `getEnv()` with CF runtime → `.env.local` fallback
- **`src/lib/queries.ts`** — typed interfaces (`LiveSession`, `RosterRow`, `OverviewStats`); query functions (`getOverviewStats`, `getRecentSessions`, `getAllSessions`, `getSessionById`, `getRosterStats`, `getDistinctSections`); helpers (`formatDuration`, `formatDate`, `derivePrepLevel`, `parseEventLog`)
- All 4 pages rewritten: `index.astro`, `sessions/index.astro`, `sessions/[id].astro`, `roster.astro`
- Graceful "no DB configured" fallback when env vars are absent
- `mockData.ts` renamed to `mockData.fallback.ts` (kept for reference)

### Phase 3 — Synthetic Training Data
- **`scripts/generate_synthetic_data.py`** — generates 60 labeled sessions (20 HIGH / 20 MODERATE / 20 LOW) directly into Turso
- Each session has a realistic `event_log` JSON with FIRE or EARTHQUAKE events
- Profile-bounded scores matching prep_level ranges
- `--dry-run` flag for testing without writing to DB
- Schema migration step built-in (`setup_schema()`) — idempotent, adds missing columns
- **60 sessions successfully written to Turso** and visible in dashboard

### Phase 4 — Thesis Extras
- **4A: `SimulationFeedback.java`** — rule-based adaptive feedback at sim end using buffered event log:
  - FIRE: decision delay tip, spray accuracy %, panic proxy (erratic movement), path efficiency (backtracking)
  - EARTHQUAKE: DROP → COVER → HOLD ON reminder
- **4B: Section filter on roster** — pill buttons filter students by class section; `getDistinctSections()` query
- **4C: BFP notes API** — `POST /api/sessions/[id]/notes` writes to `bfp_notes` column; textarea form on session detail page
- **4D: RF confidence bar** — shows confidence % when `prep_level` populated by groupmate's RF script; "Pending classification" message otherwise

---

## Bugs Fixed During Testing

| Bug | Fix |
|---|---|
| `Astro.locals.runtime?.env` throws instead of returning undefined | Wrapped in try-catch in `getEnv()` |
| `ThemeToggle` and `EventLog` failing with "null useState" in SSR | Changed `client:load` → `client:only="react"` |
| `wrangler pages dev` fails with `No such module wrangler:modules-watch` | Use `pnpm dev` with `.env.local` fallback instead |
| Turso 400 error — integer args must be strings in REST API | Changed all `{"type":"integer","value":146}` → `{"type":"integer","value":"146"}` |
| `ops.json` had wrong UUID for "Dev" | Found real UUID `380df991-f603-344c-a090-369bad2a924a` from playerdata files |
| Stale Vite SSR cache causing missing module errors | Deleted `node_modules/.vite` directory |

---

## Architecture

```
[Student] /register + lobby buttons
      ↓
[NeoForge Mod] SimulationEventLogger buffers events
      ↓ HTTP on sim end
[Turso — libSQL Cloud DB]
  sessions table: student_name, student_id, section,
                  simulation_type, simulation_score, passed,
                  event_log (JSON), prep_level, confidence, bfp_notes
      ↓                              ↓
[Cloudflare Workers]         [Python RF Script]
 Astro SSR Dashboard          reads event_log → extracts 6 features
 /sessions, /roster           → trains RandomForest
 /sessions/[id]               → writes prep_level + confidence back
 BFP notes form
```

---

## Turso Database
- **URL:** `https://berong-smp-necookie.aws-ap-northeast-1.turso.io`
- **60 synthetic sessions** pre-loaded for RF training (20 HIGH / 20 MODERATE / 20 LOW)
- **Columns:** id, student_name, student_id, section, station_account, account_uuid, start_time, end_time, status, tutorial_completed, tutorial_duration_s, simulation_type, simulation_score, passed, event_log, prep_level, confidence, bfp_notes

---

## Dev Setup

### Mod
```powershell
cd C:\Users\dheyn\Documents\02_Dev\berongsmp-template-26.1.2
.\gradlew runServer   # Terminal 1
.\gradlew runClient   # Terminal 2
```

### Dashboard
```powershell
cd C:\Users\dheyn\Documents\04_School\BERONG_SMP_WEB\apps\dashboard
pnpm dev              # http://localhost:4322
```

### Synthetic Data (run once)
```bash
cd scripts
python generate_synthetic_data.py
```

---

## In-Game Commands

| Command | Who | Effect |
|---|---|---|
| `/register <id> <section> <name>` | Any player | Self-register before simulation |
| `/bfp checkin <name>` | OP | Start BFP session for current player |
| `/bfp checkout` | OP | Finalise and save session to DB |
| `/bfp session info` | OP | Show current session details |
| `/bfp sessions list` | OP | List recent sessions from DB |
| `/bfp reset` | OP | Wipe tutorial + delete DB row |
| `/sim_fire` | OP | Start fire simulation |
| `/sim_earthquake [mag]` | OP | Start earthquake simulation |
| `/sim_stop` | OP | End current simulation |
| `/get_extinguisher` | OP | Give fire extinguisher |

---

## What's Next (For Groupmate)

1. Read `event_log` JSON from Turso — each row is an array of `{type, tOffsetMs, data}` events
2. Extract 6 features per session:
   - `decision_delay_s` — time from SIM_START to EXT_PIN_PULL
   - `spray_accuracy` — EXT_SPRAY hits / total EXT_SPRAY
   - `path_efficiency` — straight_dist / cumulative_dist from PLAYER_TICK
   - `hazard_proximity_ratio` — ticks with nearest_fire_dist < 3 / total ticks
   - `interaction_frequency` — EXT_SPRAY count / duration_s
   - `panic_proxy` — variance of dx²+dz² between PLAYER_TICK rows
3. Train RandomForest on `prep_level` label (60 synthetic sessions available)
4. Write `prep_level` + `confidence` back to `sessions` table for each real session
5. Dashboard confidence bar will auto-populate once values are written

---

## Repos
- **Mod:** https://github.com/Necookie/BERONG_SMP
- **Dashboard:** https://github.com/Necookie/BERONG_SMP_WEB
