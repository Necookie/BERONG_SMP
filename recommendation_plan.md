# BerongSMP — Student Session System Recommendation

## Problem

BFP training stations run shared Minecraft accounts (e.g. `station1`, `station2`).
Multiple students rotate through the same account during a test or drill.
Because Mojang authentication ties identity to the account UUID, tutorial progress and
simulation scores persist across students — Student B inherits Student A's completed state.

The goal is to track **each student's performance individually** without requiring
personal Minecraft accounts.

---

## Core Concept: Sessions, Not Accounts

> The Minecraft account is a **terminal**, not a student identity.

A **session** begins when a student sits down and an admin (or the student) checks in
with their name or student ID. Everything — tutorial progress, simulation score,
timestamps — is recorded against that session, not the account UUID. At the end,
the account is reset so the next student starts fresh.

---

## System Components

### 1. In-Game: `SessionManager` (NeoForge Mod)

A new class `SessionManager.java` holds the active session per station account:

```
Map<UUID, StudentSession> activeSessions   // keyed by account UUID
```

`StudentSession` fields:
| Field | Type | Description |
|---|---|---|
| `studentName` | String | Name/ID entered at check-in |
| `stationAccount` | String | Minecraft username of the shared account |
| `accountUuid` | UUID | Minecraft UUID of the shared account |
| `startTime` | Instant | When the session began |
| `tutorialStartTime` | Instant | When tutorial was entered |
| `tutorialEndTime` | Instant | When tutorial completed (null if not yet) |
| `simulationType` | String | `"FIRE"` or `"EARTHQUAKE"` (null if not run) |
| `simulationScore` | int | Fires extinguished / survival time / etc. |
| `passed` | boolean | Whether the session met the pass threshold |

On check-in:
1. Any active session for that account is closed and saved to DB.
2. Tutorial saved data for the account UUID is wiped (`TutorialSavedData.reset(uuid)`).
3. A new `StudentSession` is created and stored in `activeSessions`.

On session end (manual close, logout, or simulation end):
1. End time, score, and pass status are finalised.
2. Session is written to the SQLite database.
3. Account tutorial state is reset for the next student.

---

### 2. Database: SQLite

SQLite is a single-file database — no server process needed. The `.db` file lives in
the `run/` working directory alongside world saves. Add the JDBC driver to `build.gradle`:

```groovy
// build.gradle — dependencies block
implementation 'org.xerial:sqlite-jdbc:3.45.3.0'
```

#### Schema

```sql
CREATE TABLE IF NOT EXISTS sessions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    student_name        TEXT    NOT NULL,
    station_account     TEXT    NOT NULL,
    account_uuid        TEXT    NOT NULL,
    start_time          TEXT    NOT NULL,   -- ISO-8601
    end_time            TEXT,
    tutorial_completed  INTEGER DEFAULT 0,  -- 0 or 1
    tutorial_duration_s INTEGER,            -- seconds from start to completion
    simulation_type     TEXT,               -- 'FIRE' | 'EARTHQUAKE' | NULL
    simulation_score    INTEGER DEFAULT 0,
    passed              INTEGER DEFAULT 0,
    notes               TEXT
);

CREATE INDEX IF NOT EXISTS idx_sessions_student ON sessions(student_name);
CREATE INDEX IF NOT EXISTS idx_sessions_start   ON sessions(start_time);
```

#### Connection handling

Open a single `Connection` when the server starts, close it on server stop.
Use `PreparedStatement` for all inserts — never string-interpolate SQL.

---

### 3. Admin Brigadier Commands

Add these to `ModCommands.java` under a `/bfp` root literal (requires OP level 2):

| Command | Effect |
|---|---|
| `/bfp checkin <student_name>` | Start a new session for the account running the command (or target a specific player: `/bfp checkin <player> <student_name>`) |
| `/bfp checkout` | Manually close and save the current session |
| `/bfp reset [player]` | Wipe tutorial state + close active session without saving a record |
| `/bfp session info` | Print current session details to the admin in chat |
| `/bfp sessions list [page]` | List the 10 most recent sessions from the DB |
| `/bfp sessions export` | Write all sessions to `run/bfp_sessions_export.csv` |
| `/bfp student <name>` | Look up all sessions for a given student name |

---

### 4. Integration Hooks

**`TutorialManager`** — when `completeTutorial()` fires:
```java
SessionManager.onTutorialComplete(player);
// records tutorialEndTime, sets tutorial_completed = 1, tutorial_duration_s
```

**`SimulationManager`** — when `endSimulation()` fires:
```java
SessionManager.onSimulationEnd(player, session);
// records simulation_type, simulation_score, passed, then closes + saves DB record
```

**`LobbyManager.onPlayerLogin`** — if a session was active when the player disconnected
mid-tutorial, restore the session (not wipe it) so they can continue.

---

### 5. Optional: REST API + Web Dashboard

If you want a dashboard accessible from outside the game server:

**REST API** — a small Node.js (Express) or Python (FastAPI) app that reads the SQLite
file and exposes endpoints:

```
GET /sessions                  → paginated session list
GET /sessions?student=<name>   → filter by student
GET /sessions/:id              → single session detail
GET /export/csv                → download CSV
POST /sessions/:id/notes       → add admin note
```

Run it on the same machine as the Minecraft server. Since SQLite is file-based and
supports concurrent readers, the API can open the DB read-only while the mod holds the
write connection — no conflicts.

**Dashboard** — a single HTML page (or a simple React/Vue app) consuming the API.
Columns: Student Name, Station, Date, Tutorial Time, Simulation, Score, Pass/Fail.
Add a date-range filter and a CSV export button.

---

## Admin Workflow (Per Student)

```
1. Student sits down at a station PC.
2. Admin (or student) runs:    /bfp checkin <student_name>
   → Account state wiped, fresh tutorial begins, session clock starts.
3. Student completes tutorial and disaster simulation.
   → Scores recorded automatically.
4. Admin reviews:              /bfp session info
5. Student leaves.
   → Session auto-closes on logout, or admin runs /bfp checkout.
6. Next student: repeat from step 1.
```

---

## Implementation Sequence

| Phase | Deliverable | Notes |
|---|---|---|
| 1 | `SessionManager.java` + SQLite setup | Core data model; no UI yet |
| 2 | `/bfp checkin` + `/bfp reset` commands | Minimum viable admin flow |
| 3 | Hooks in `TutorialManager` + `SimulationManager` | Auto-record completion + score |
| 4 | `/bfp sessions list` + `/bfp sessions export` | Reporting inside the game |
| 5 | REST API | Optional; only if dashboard is needed |
| 6 | Web dashboard | Optional; built on top of the API |

Phase 1–4 are self-contained inside the mod with no external dependencies beyond the
SQLite JDBC driver. Phases 5–6 are independent services that can be added later without
touching the mod.

---

## Technical Considerations

**Concurrent stations** — `SessionManager.activeSessions` is a `ConcurrentHashMap`
keyed by account UUID, same thread-safety pattern as `SimulationManager.activeSessions`.
Multiple stations running simultaneously are fully supported.

**Server restart mid-session** — transient `activeSessions` in memory is lost on restart.
On server start, query the DB for sessions with a null `end_time` and mark them as
incomplete (add a `status TEXT DEFAULT 'active'` column: `active | completed | aborted`).

**SQLite JDBC on NeoForge** — shade the driver into the mod JAR using the Shadow plugin,
or place it in the `run/mods/` folder as a separate JAR. Shading is cleaner for
distribution.

**Score definition** — currently `SimulationSession` tracks `firesTouchedCount` (fires
extinguished). For a meaningful pass/fail, define thresholds in `berongsmp-common.toml`:
```toml
passThresholdFire = 5        # fires extinguished to pass
passThresholdQuake = 80      # ticks survived in PEAK phase to pass
```

**Privacy** — student names are stored as plain text. If this is used in an actual
academic setting, confirm with the institution whether storing student names in a local
file requires data handling consent.
