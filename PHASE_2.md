# Phase 2 — High Priority Fixes

> **Prompt for Claude:**
> Apply all three fixes below to the BerongSMP project, in order (H-1 → H-2 → H-3).
> Phase 1 (C-1, C-2, C-3) must already be applied before starting this phase.
> Files to edit:
> - `src/main/java/net/necookie/disastersim/world/SimulationManager.java`
> - `src/main/java/net/necookie/disastersim/command/ModCommands.java`
>
> Read each file before editing. After all three fixes are done, confirm what changed.

---

## H-1 · Shared `Random` is not thread-safe

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 47, 147, 162

**Problem:**
`java.util.Random` is not thread-safe. If two threads call `random.nextInt()` simultaneously, one
write is lost, producing duplicate positions or internal `ArrayIndexOutOfBoundsException`. Fire and
earthquake effects end up clustering in the same spots instead of being randomized.

**Fix — remove the shared `Random` field and use `level.getRandom()` instead:**

Delete this field (around line 47):
```java
private static final Random random = new Random(); // DELETE THIS LINE
```

Delete this import (around line 23):
```java
import java.util.Random; // DELETE THIS LINE
```

In `simulateFire()` (around line 147), change to:
```java
BlockPos firePos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
```

In `simulateEarthquake()` (around line 162), change to:
```java
BlockPos breakPos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
```

---

## H-2 · No way to stop a simulation early

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, line 174
**File:** `src/main/java/net/necookie/disastersim/command/ModCommands.java`

**Problem:**
Once a simulation starts it runs for exactly 2400 ticks (2 minutes) with no exit. There is no way
to stop a stuck simulation, reset mid-session, or speed up testing without a full server restart.

**Fix 1 — make `endSimulation()` public in `SimulationManager.java`:**

Change (around line 174):
```java
// Before
private static synchronized void endSimulation() {

// After
public static synchronized void endSimulation() {
```

**Fix 2 — add a `/sim_stop` command in `ModCommands.java`:**

Inside the `register()` method, after the existing command registrations, add:
```java
dispatcher.register(Commands.literal("sim_stop")
    .executes(context -> {
        SimulationManager.endSimulation();
        context.getSource().sendSuccess(() -> Component.literal("Simulation stopped."), true);
        return 1;
    }));
```

---

## H-3 · Timer HUD shows wrong value for up to 1 full second

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, line 136

**Problem:**
`timer / 20` uses integer division (floor). When `timer = 2399`, it shows `119s` even though
`119.95s` remain. The display reads `0s` for the entire first second of the simulation, and the
countdown visually skips a step on each transition.

**Fix — use ceiling division `(timer + 19) / 20` so the display rounds up:**

On line 136, change:
```java
// Before
new SimulationStatusPayload(currentState.name(), timer / 20)

// After
new SimulationStatusPayload(currentState.name(), (timer + 19) / 20)
```
