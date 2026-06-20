# Phase 4 — Thesis Extras

> **Status:** `[ ] not started`
> **Repos:** Both (mod + dashboard)
> **Depends on:** Phase 2 complete (dashboard live), groupmate's RF writes `prep_level` + `confidence` back to Turso
> **Update `docs/major_plan.md` when complete, push both repos.**

---

## Context

At this point:
- Mod logs behavioral events to Turso (Phase 1)
- Dashboard shows real data (Phase 2)
- Groupmate's RF script writes `prep_level` + `confidence` to Turso
- Phase 4 closes the loop: students get feedback in-game, teachers see richer dashboard views, BFP officers can annotate

---

## 4A — Adaptive In-Game Feedback (Mod, no external API)

Compute feedback at simulation end using the already-buffered event log in `SimulationEventLogger`.

### New class: `world/SimulationFeedback.java`

```java
public class SimulationFeedback {

    public static void send(ServerPlayer player, SimulationEventLogger logger, int score) {
        List<SimulationEventLogger.SimEvent> events = logger.getEvents();

        double sprayAccuracy = computeSprayAccuracy(events);
        double decisionDelaySec = computeDecisionDelay(events);
        double panicProxy = computePanicProxy(events);
        double pathEfficiency = computePathEfficiency(events);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6--- Simulation Feedback ---"));

        if (decisionDelaySec > 30) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Slow response: §fyou took " + (int)decisionDelaySec +
                "s to pull the pin. In a real fire, act within 10 seconds."));
        } else {
            player.sendSystemMessage(Component.literal(
                "§a✓ Good response time: §fyou pulled the pin in " +
                (int)decisionDelaySec + "s."));
        }

        if (sprayAccuracy < 0.40) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Low accuracy (" + Math.round(sprayAccuracy*100) +
                "%): §faim at the §lbase§r§f of the flame, not the smoke above it."));
        } else {
            player.sendSystemMessage(Component.literal(
                "§a✓ Spray accuracy: §f" + Math.round(sprayAccuracy*100) + "%"));
        }

        if (panicProxy > 2.0) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Erratic movement detected: §fstay calm and move deliberately toward the exit."));
        }

        if (pathEfficiency < 0.50) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Inefficient path: §favoid backtracking in smoke — know your exit before the fire starts."));
        }

        player.sendSystemMessage(Component.literal(
            "§eScore: §f" + score + "/100"));
        player.sendSystemMessage(Component.literal("§6----------------------------"));
    }

    private static double computeSprayAccuracy(List<SimEvent> events) {
        long hits = events.stream()
            .filter(e -> e.type().equals("EXT_SPRAY"))
            .filter(e -> Boolean.TRUE.equals(e.data().get("hit_fire")))
            .count();
        long total = events.stream().filter(e -> e.type().equals("EXT_SPRAY")).count();
        return total == 0 ? 0 : (double) hits / total;
    }

    private static double computeDecisionDelay(List<SimEvent> events) {
        long simStart = events.stream().filter(e -> e.type().equals("SIM_START"))
            .mapToLong(SimEvent::tOffsetMs).findFirst().orElse(0);
        long pinPull = events.stream().filter(e -> e.type().equals("EXT_PIN_PULL"))
            .mapToLong(SimEvent::tOffsetMs).findFirst().orElse(-1);
        return pinPull < 0 ? 999 : (pinPull - simStart) / 1000.0;
    }

    private static double computePanicProxy(List<SimEvent> events) {
        List<SimEvent> ticks = events.stream()
            .filter(e -> e.type().equals("PLAYER_TICK")).toList();
        if (ticks.size() < 2) return 0;
        double[] speeds = new double[ticks.size() - 1];
        for (int i = 1; i < ticks.size(); i++) {
            double dx = getD(ticks.get(i), "x") - getD(ticks.get(i-1), "x");
            double dz = getD(ticks.get(i), "z") - getD(ticks.get(i-1), "z");
            speeds[i-1] = dx*dx + dz*dz;
        }
        double mean = Arrays.stream(speeds).average().orElse(0);
        return Arrays.stream(speeds).map(s -> (s - mean) * (s - mean)).average()
            .map(Math::sqrt).orElse(0);
    }

    private static double computePathEfficiency(List<SimEvent> events) {
        List<SimEvent> ticks = events.stream()
            .filter(e -> e.type().equals("PLAYER_TICK")).toList();
        if (ticks.size() < 2) return 1;
        SimEvent first = ticks.get(0), last = ticks.get(ticks.size()-1);
        double straight = Math.sqrt(
            Math.pow(getD(last,"x")-getD(first,"x"), 2) +
            Math.pow(getD(last,"z")-getD(first,"z"), 2));
        double cumulative = 0;
        for (int i = 1; i < ticks.size(); i++) {
            cumulative += Math.sqrt(
                Math.pow(getD(ticks.get(i),"x")-getD(ticks.get(i-1),"x"),2) +
                Math.pow(getD(ticks.get(i),"z")-getD(ticks.get(i-1),"z"),2));
        }
        return cumulative == 0 ? 1 : straight / cumulative;
    }

    private static double getD(SimEvent e, String key) {
        Object v = e.data().get(key);
        return v instanceof Number n ? n.doubleValue() : 0;
    }
}
```

### Wire in `SimulationManager.endSimulation`:
```java
SimulationFeedback.send(player, session.logger, session.getSimulationScore());
```

---

## 4B — Section/Cohort Filtering on Dashboard Roster

In `apps/dashboard/src/pages/roster.astro`:

1. Read all distinct sections from Turso:
   ```sql
   SELECT DISTINCT section FROM sessions WHERE section IS NOT NULL ORDER BY section
   ```
2. Add a filter pill row above the table (same pattern as `/sessions` page)
3. Client-side JS filters the table by selected section
4. Add a cohort stat bar: avg score per section

---

## 4C — BFP Validation Notes

### Dashboard: `pages/sessions/[id].astro`

Add a notes panel below the stats panel (right column):

```html
<form method="POST" action={`/api/sessions/${session.id}/notes`}>
  <textarea name="notes" placeholder="BFP officer validation notes..."
            class="notes-input">{session.bfp_notes ?? ''}</textarea>
  <button type="submit">Save Notes</button>
</form>
```

### New API route: `pages/api/sessions/[id]/notes.ts`

```ts
export const POST: APIRoute = async ({ params, request, locals }) => {
  const env = locals.runtime.env;
  const db = getDb(env);
  const form = await request.formData();
  const notes = form.get('notes') as string;
  await db.execute(
    `UPDATE sessions SET bfp_notes = ? WHERE id = ?`,
    [notes, Number(params.id)]
  );
  return new Response(null, { status: 303,
    headers: { Location: `/sessions/${params.id}` } });
};
```

---

## 4D — Confidence Score Display

In `pages/sessions/[id].astro` stats panel, after the prep_level badge:

```astro
{session.prep_level && (
  <div class="confidence-bar">
    <span class="label">RF Confidence</span>
    <div class="bar-track">
      <div class="bar-fill"
           style={`width: ${Math.round((session.confidence ?? 0) * 100)}%`} />
    </div>
    <span class="value">{Math.round((session.confidence ?? 0) * 100)}%</span>
  </div>
)}
{!session.prep_level && (
  <p class="text-muted">Preparedness level pending RF classification.</p>
)}
```

Add `.confidence-bar` styles to `styles/global.css` using existing CSS variables.

---

## 4E — API Hook for Future In-Game Feedback (optional upgrade)

If the team later wants server-computed feedback (e.g., from the RF model's feature importances):

Mod sends a POST to a Cloudflare Workers route at sim end:
```
POST /api/feedback
{ session_id, features: { accuracy, delay, ... } }
→ returns { messages: ["tip 1", "tip 2"] }
```

Dashboard route computes tips using the RF feature importances. Mod displays the returned messages.

This is optional Phase 4 upgrade — only needed if you want ML-informed feedback rather than rule-based. Start with 4A (rule-based) and upgrade if time allows.

---

## Verification

**4A:** Play a fire sim deliberately badly (don't pull pin for 60s, spray randomly) → sim ends → feedback messages appear in chat.

**4B:** Dashboard `/roster` → section filter shows separate rows per class.

**4C:** Open any session detail → type BFP notes → submit → notes persist on refresh.

**4D:** Groupmate's RF script runs → `prep_level` populated → confidence bar appears on detail page.

When done: update `docs/major_plan.md` Phase 4 → `[x] done`, push both repos.
