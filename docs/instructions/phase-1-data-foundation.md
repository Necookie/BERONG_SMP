# Phase 1 — Data Foundation (Mod Side)

> **Status:** `[ ] not started`
> **Repo:** `berongsmp-template-26.1.2`
> **Update `docs/major_plan.md` phase status when complete, then push to main.**

---

## Context

This is a NeoForge 26.1.2 mod for Minecraft. The mod already has:
- `TursoClient.java` — HTTP REST client writing to Turso libSQL
- `SimulationSession.java` — per-player mutable state during simulation
- `SimulationManager.java` — tick driver, session registry
- `LobbyManager.java` — button click handler that starts simulations
- `FireExtinguisherItem.java` — right-click spray extinguishes fire
- `/bfp checkin` — OP registers students (being replaced by `/register`)
- `LOBBY_POS = BlockPos(0, -33, 0)`, `SIM_POS = BlockPos(30, -34, 83)`

Read `CLAUDE.md` and `docs/major_plan.md` before starting.

---

## Goal

Every student identifies themselves before simulating. Every simulation session produces a rich `event_log` JSON blob in Turso that captures behavioral telemetry for the RF classifier.

---

## Step 1 — Turso Schema Migration

In `TursoClient.java`, find the `init()` method that creates the schema. Add the new columns using `ALTER TABLE IF NOT EXISTS` pattern (Turso supports `ALTER TABLE ADD COLUMN` — safe to run repeatedly):

```java
// Add to the schema init SQL after existing CREATE TABLE:
"ALTER TABLE sessions ADD COLUMN student_id TEXT",
"ALTER TABLE sessions ADD COLUMN section TEXT",
"ALTER TABLE sessions ADD COLUMN event_log TEXT",
"ALTER TABLE sessions ADD COLUMN prep_level TEXT",
"ALTER TABLE sessions ADD COLUMN confidence REAL",
"ALTER TABLE sessions ADD COLUMN bfp_notes TEXT"
```

Run each as a separate pipeline statement wrapped in try/catch — Turso returns an error if column already exists, which is fine to swallow.

---

## Step 2 — Registration System

### `RegistrationManager.java` (new, `registration/` package)

```java
// Stores: Map<UUID, StudentRegistration> where StudentRegistration has:
//   String studentName, studentId, section
// Backed by SavedData so it persists across server restarts
// Key methods:
//   register(UUID, name, id, section)
//   isRegistered(UUID) → boolean
//   getRegistration(UUID) → StudentRegistration
//   reset(UUID)  ← called by /bfp reset
```

### `RegisteredPlayerData.java` (new, `registration/` package)

Extends `SavedData`. Persists the `Map<UUID, StudentRegistration>` to `world/data/berongsmp_registrations.dat`.

Pattern: copy `TutorialSavedData.java` structure exactly — it already does UUID→stage persistence. Replace `TutorialStage` with `StudentRegistration`.

---

## Step 3 — `/register` Command

In `ModCommands.register()`, add before the `/bfp` block:

```java
dispatcher.register(Commands.literal("register")
    .then(Commands.argument("full_name", StringArgumentType.greedyString())
        // BUT: greedyString can't be followed by more args
        // Solution: parse "name | student_id | section" with a delimiter
        // OR: use 3 separate word args: name (greedyString last)
        // Best approach: /register <student_id> <section> <full_name>
        //   student_id = StringArgumentType.word()
        //   section    = StringArgumentType.word()
        //   full_name  = StringArgumentType.greedyString()  ← must be last
    ));
```

**Exact syntax:** `/register <student_id> <section> <full_name>`
Example: `/register 2021-12345 BSIT-3A Juan dela Cruz`

On execute:
1. `RegistrationManager.register(player.getUUID(), fullName, studentId, section)`
2. Also call `SessionManager.checkin(uuid, stationAccount, fullName)` to create the Turso row
3. Update the Turso row with `student_id` and `section` immediately after
4. Send: `§a✓ Registered as §f<fullName> §7(§f<studentId>§7) — §fSection <section>`
5. Send: `§eYou may now begin the tutorial.`

---

## Step 4 — Registration Gate

In `LobbyManager.onRightClickBlock`, after the tutorial-complete check, add:

```java
if (!RegistrationManager.isRegistered(uuid)) {
    player.sendSystemMessage(Component.literal(
        "§cPlease /register first before starting a simulation."));
    return;
}
```

---

## Step 5 — `SimRoom` Enum

New file `world/SimRoom.java`. Offsets are relative to `SIM_POS = (30, -34, 83)`.

```java
public enum SimRoom {
    COMPUTER_LAB,
    MAIN_HALL,
    ENTRANCE,
    STAIRWELL,
    UPPER_FLOOR,
    OUTSIDE;

    // Rough bounding boxes — tune after running ./gradlew runServer
    // and walking through the LSPU library structure
    private static final Map<SimRoom, AABB> BOUNDS = Map.of(
        COMPUTER_LAB,  new AABB(0, 0, 0, 12, 5, 10),
        MAIN_HALL,     new AABB(0, 0, 10, 20, 5, 25),
        ENTRANCE,      new AABB(4, 0, 22, 10, 5, 28),
        STAIRWELL,     new AABB(18, 0, 5, 22, 10, 12),
        UPPER_FLOOR,   new AABB(0, 5, 0, 20, 10, 25)
    );

    public static SimRoom fromPos(BlockPos playerPos, BlockPos simOrigin) {
        Vec3 rel = Vec3.atLowerCornerOf(playerPos.subtract(simOrigin));
        for (var entry : BOUNDS.entrySet()) {
            if (entry.getValue().contains(rel)) return entry.getKey();
        }
        return OUTSIDE;
    }
}
```

**Important:** These bounding boxes are placeholders. Run `./gradlew runServer`, walk each room at `SIM_POS`, and note the relative coordinates. Update values before Phase 3.

---

## Step 6 — `SimulationEventLogger`

New file `world/SimulationEventLogger.java`.

```java
public class SimulationEventLogger {
    public record SimEvent(String type, long tOffsetMs, Map<String, Object> data) {}

    private final List<SimEvent> events = new ArrayList<>();
    private final long startMs = System.currentTimeMillis();

    public void log(String type, Map<String, Object> data) {
        events.add(new SimEvent(type, System.currentTimeMillis() - startMs, data));
    }

    public String toJson() {
        // Serialize using Gson (already on classpath via NeoForge)
        return new Gson().toJson(events);
    }
}
```

Add `SimulationEventLogger logger = new SimulationEventLogger()` to `SimulationSession`.

---

## Step 7 — Wire Event Logging

### In `SimulationManager.onServerTick` (PLAYER_TICK, every 20 ticks):
```java
if (tickCount % 20 == 0) {
    BlockPos pos = player.blockPosition();
    double nearestFire = nearestFireDistance(player, level);
    SimRoom room = SimRoom.fromPos(pos, LobbyManager.SIM_POS);  // adjust for actual arena origin
    session.logger.log("PLAYER_TICK", Map.of(
        "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
        "room", room.name(),
        "nearest_fire_dist", nearestFire
    ));
}
```

### In `FireExtinguisherItem` on spray:
```java
session.logger.log("EXT_SPRAY", Map.of(
    "hit_fire", hitFire,
    "distance_to_fire", distToNearestFire,
    "fire_blocks_in_fov", fireBlocksInRadius
));
```

### In `FireExtinguisherItem` on pin pull (state change):
```java
session.logger.log("EXT_PIN_PULL", Map.of("pulled", true));
```

### In `SimulationEffects` when fire block placed:
```java
session.logger.log("FIRE_SPREAD", Map.of(
    "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
    "total_count", session.getFireSpreadCount()
));
```

### In `SimulationManager.endSimulation`:
```java
// Before closing the Turso row:
String eventLogJson = session.logger.toJson();
TursoClient.updateEventLog(sessionId, eventLogJson);
```

---

## Step 8 — TursoClient additions

Add to `TursoClient.java`:
```java
public static void updateStudentInfo(String uuid, String studentId, String section) {
    // UPDATE sessions SET student_id=?, section=? WHERE account_uuid=? AND status='active'
}

public static void updateEventLog(int sessionId, String eventLogJson) {
    // UPDATE sessions SET event_log=? WHERE id=?
}
```

---

## Verification

1. `./gradlew compileJava` — must compile clean
2. `./gradlew runServer`
3. Login, try clicking sim button → should get "Please /register first"
4. `/register 2021-00001 BSIT-3A Test Player`
5. Click sim button → simulation starts
6. Run through fire sim, extinguish some fires
7. Sim ends → check Turso: `SELECT event_log FROM sessions ORDER BY id DESC LIMIT 1`
8. Paste the JSON — should contain PLAYER_TICK, EXT_SPRAY, EXT_PIN_PULL entries

---

## Files Modified/Created

- `src/.../command/ModCommands.java` — add `/register`
- `src/.../registration/RegistrationManager.java` — NEW
- `src/.../registration/RegisteredPlayerData.java` — NEW
- `src/.../world/SimRoom.java` — NEW
- `src/.../world/SimulationEventLogger.java` — NEW
- `src/.../world/SimulationSession.java` — add logger field, fire spread counter
- `src/.../world/SimulationManager.java` — add PLAYER_TICK logging
- `src/.../world/LobbyManager.java` — add registration gate
- `src/.../item/FireExtinguisherItem.java` — add EXT_PIN_PULL + EXT_SPRAY logging
- `src/.../world/SimulationEffects.java` — add FIRE_SPREAD logging
- `src/.../session/TursoClient.java` — schema migration + new update methods

When done: update `docs/major_plan.md` Phase 1 status → `[x] done`, commit, push.
