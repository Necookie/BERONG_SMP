# Phase 2 — Dashboard Live Data

> **Status:** `[ ] not started`
> **Repo:** `BERONG_SMP_WEB` (`apps/dashboard`)
> **Depends on:** Phase 1 complete (Turso has real sessions with event_log)
> **Update `docs/major_plan.md` phase status when complete, then push both repos.**

---

## Context

The dashboard is an Astro 6 SSR app deployed on Cloudflare Workers (`apps/dashboard`).
Currently all 4 pages use `src/lib/mockData.ts` — 8 fake sessions, no Turso connection.

Turso DB has these columns after Phase 1:
```
id, student_name, student_id, section, station_account, account_uuid,
start_time, end_time, status, tutorial_completed, tutorial_duration_s,
simulation_type, simulation_score, passed, event_log (JSON),
prep_level, confidence, bfp_notes, notes
```

Cloudflare Workers env vars are accessed via `Astro.locals.runtime.env` in SSR pages.

Read `CLAUDE.md` in `BERONG_SMP_WEB` before starting. All styling through CSS variables in `styles/global.css` — no hardcoded hex in component files.

---

## Step 1 — Install `@libsql/client`

```bash
cd apps/dashboard
pnpm add @libsql/client
```

Use the `/web` import path everywhere — it uses fetch internally, compatible with Cloudflare Workers. Do NOT use the Node.js default import.

---

## Step 2 — Env Vars

**`apps/dashboard/wrangler.toml`** — add vars block:
```toml
[vars]
TURSO_URL = ""
TURSO_TOKEN = ""
```

**`apps/dashboard/.dev.vars`** (create, gitignored):
```
TURSO_URL=https://yourdb-yourorg.turso.io
TURSO_TOKEN=your-token-here
```

**`apps/dashboard/.gitignore`** — add line: `.dev.vars`

**`apps/dashboard/src/env.d.ts`** — add to `App.Locals` or `Runtime`:
```ts
interface CloudflareEnv {
  TURSO_URL: string;
  TURSO_TOKEN: string;
  ASSETS: Fetcher;
}
```
Check existing `env.d.ts` first — add to whatever `Runtime` type already exists there.

---

## Step 3 — `src/lib/db.ts`

```ts
import { createClient } from "@libsql/client/web";

export function getDb(env: { TURSO_URL: string; TURSO_TOKEN: string }) {
  return createClient({
    url: env.TURSO_URL,
    authToken: env.TURSO_TOKEN,
  });
}
```

---

## Step 4 — `src/lib/queries.ts`

Define types matching the Turso schema, then query functions. The existing `Session` type in `mockData.ts` is a guide — align the new types to it where possible so page templates need minimal changes.

```ts
import { getDb } from "./db";

export interface LiveSession {
  id: number;
  student_name: string;
  student_id: string | null;
  section: string | null;
  station_account: string;
  start_time: string;
  end_time: string | null;
  status: string;
  tutorial_completed: number;
  tutorial_duration_s: number | null;
  simulation_type: "FIRE" | "EARTHQUAKE" | null;
  simulation_score: number;
  passed: number;
  event_log: string | null;   // JSON string — parse on use
  prep_level: "HIGH" | "MODERATE" | "LOW" | null;
  confidence: number | null;
  bfp_notes: string | null;
}

export async function getOverviewStats(env) {
  const db = getDb(env);
  // total sessions, avg score, fire/quake split, this-week count, prep distribution
  const res = await db.execute(`
    SELECT
      COUNT(*) as total,
      AVG(simulation_score) as avg_score,
      SUM(CASE WHEN simulation_type='FIRE' THEN 1 ELSE 0 END) as fire_count,
      SUM(CASE WHEN simulation_type='EARTHQUAKE' THEN 1 ELSE 0 END) as quake_count,
      SUM(CASE WHEN date(start_time) >= date('now','-7 days') THEN 1 ELSE 0 END) as this_week,
      SUM(CASE WHEN prep_level='HIGH' THEN 1 ELSE 0 END) as high_count,
      SUM(CASE WHEN prep_level='MODERATE' THEN 1 ELSE 0 END) as mod_count,
      SUM(CASE WHEN prep_level='LOW' THEN 1 ELSE 0 END) as low_count
    FROM sessions WHERE status='completed'
  `);
  return res.rows[0];
}

export async function getRecentSessions(env, limit = 5): Promise<LiveSession[]> {
  const db = getDb(env);
  const res = await db.execute(
    `SELECT * FROM sessions WHERE status='completed' ORDER BY start_time DESC LIMIT ?`,
    [limit]
  );
  return res.rows as unknown as LiveSession[];
}

export async function getAllSessions(env): Promise<LiveSession[]> {
  const db = getDb(env);
  const res = await db.execute(
    `SELECT * FROM sessions ORDER BY start_time DESC`
  );
  return res.rows as unknown as LiveSession[];
}

export async function getSessionById(env, id: number): Promise<LiveSession | null> {
  const db = getDb(env);
  const res = await db.execute(`SELECT * FROM sessions WHERE id = ?`, [id]);
  return (res.rows[0] as unknown as LiveSession) ?? null;
}

export async function getRosterStats(env) {
  const db = getDb(env);
  // Group by student_name, aggregate across all their sessions
  const res = await db.execute(`
    SELECT
      student_name,
      student_id,
      section,
      COUNT(*) as session_count,
      MAX(simulation_score) as best_score,
      AVG(simulation_score) as avg_score,
      SUM(passed) as pass_count,
      MAX(prep_level) as best_prep_level
    FROM sessions
    WHERE status='completed'
    GROUP BY student_name, student_id, section
    ORDER BY avg_score DESC
  `);
  return res.rows;
}
```

---

## Step 5 — Update Pages

### `pages/index.astro` (Overview)
Replace mock data import with:
```ts
---
const env = Astro.locals.runtime.env;
const stats = await getOverviewStats(env);
const recent = await getRecentSessions(env, 5);
---
```
Update template to use `stats.total`, `stats.avg_score`, `recent` array, etc.

### `pages/sessions/index.astro` (Sessions list)
```ts
---
const sessions = await getAllSessions(Astro.locals.runtime.env);
---
```
Client-side filter pills already exist — they filter on `simulation_type` and `prep_level`. Map `LiveSession` fields to what the filter expects. Add `section` as a third filter pill.

### `pages/sessions/[id].astro` (Session detail)
```ts
---
const id = Number(Astro.params.id);
const session = await getSessionById(Astro.locals.runtime.env, id);
if (!session) return Astro.redirect('/sessions');

// Parse event_log for EventLog.tsx
const events = session.event_log ? JSON.parse(session.event_log) : [];
// Map to EventLog's expected format: { ts: string, code: string, msg: string }
const logEntries = events.map(e => ({
  ts: `+${(e.tOffsetMs / 1000).toFixed(1)}s`,
  code: e.type.padEnd(16),
  msg: JSON.stringify(e.data)
}));
---
```
Pass `logEntries` to `<EventLog entries={logEntries} client:load />`.

Add `prep_level` badge and `confidence` bar to the stats panel on the right side. Show as `null`/pending if not yet classified.

### `pages/roster.astro` (Roster)
```ts
---
const roster = await getRosterStats(Astro.locals.runtime.env);
---
```
Add section column/filter to the table.

---

## Step 6 — Fallback for Missing Creds

At top of each page's frontmatter, before DB calls:
```ts
const env = Astro.locals.runtime.env;
if (!env.TURSO_URL || !env.TURSO_TOKEN) {
  // import fallback mock data for local dev without creds
  const { mockSessions } = await import('../lib/mockData.fallback');
  // ... use mock
}
```

Rename `mockData.ts` → `mockData.fallback.ts` so it's clearly not production.

---

## Verification

1. Fill in `apps/dashboard/.dev.vars` with real Turso creds
2. `pnpm dev:dashboard` → `http://localhost:4322`
3. Overview shows real session counts (not 8)
4. `/sessions` shows all real sessions; filters work
5. `/sessions/1` shows event log from Turso JSON
6. `/roster` groups by student_name with real scores
7. `pnpm build:dashboard` — Cloudflare SSR build compiles clean

---

## Files Modified/Created

- `apps/dashboard/wrangler.toml` — add `[vars]`
- `apps/dashboard/.gitignore` — add `.dev.vars`
- `apps/dashboard/.dev.vars` — NEW (gitignored)
- `apps/dashboard/src/env.d.ts` — add env types
- `apps/dashboard/src/lib/db.ts` — NEW
- `apps/dashboard/src/lib/queries.ts` — NEW
- `apps/dashboard/src/lib/mockData.ts` → renamed `mockData.fallback.ts`
- `apps/dashboard/src/pages/index.astro` — live data
- `apps/dashboard/src/pages/sessions/index.astro` — live data
- `apps/dashboard/src/pages/sessions/[id].astro` — live data + event log
- `apps/dashboard/src/pages/roster.astro` — live data + section filter

When done: update `docs/major_plan.md` Phase 2 status → `[x] done`, commit, push both repos.
