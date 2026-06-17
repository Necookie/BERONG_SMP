# Phase 1 — Critical Fixes

> **Prompt for Claude:**
> Apply all three fixes below to the BerongSMP project, in order (C-1 → C-2 → C-3).
> All changes are in `src/main/java/net/necookie/disastersim/world/SimulationManager.java`.
> Read the file first, then apply each fix. After all three are done, confirm what changed.

---

## C-1 · Player disconnect permanently freezes the simulation

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`

**Problem:**
When a player starts a simulation and then disconnects, `activePlayer` still holds a stale reference
and `currentState` stays `FIRE` or `EARTHQUAKE`. The tick handler keeps running the dead simulation
forever. Any other player who tries to start a simulation is rejected because `currentState != IDLE`.
The server is permanently locked out of simulations until a restart.

**Fix — add a `PlayerLoggedOutEvent` listener inside `SimulationManager.java`:**

```java
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (activePlayer != null && event.getEntity().getUUID().equals(activePlayer.getUUID())) {
        endSimulation();
    }
}
```

Also add the missing import at the top of the file:

```java
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
```

---

## C-2 · Null pointer race between the null-guard and actual usage

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 113–137

**Problem:**
`onServerTick` checks `activePlayer == null` on line 115, but then uses `activePlayer` again on
lines 127, 131, and 136. Between the guard and those usages, another thread could call
`endSimulation()` and set `activePlayer = null`, causing a `NullPointerException` that crashes the server.

**Fix — capture `activePlayer` into a local variable immediately after the null-guard:**

```java
public static void onServerTick(ServerTickEvent.Post event) {
    ServerPlayer player = activePlayer; // capture once
    if (currentState == SimulationState.IDLE || player == null) return;

    timer--;
    if (timer <= 0) { endSimulation(); return; }

    ServerLevel level = (ServerLevel) player.level(); // safe to use now

    if (currentState == SimulationState.FIRE && timer % 20 == 0) {
        simulateFire(level);
    } else if (currentState == SimulationState.EARTHQUAKE && timer % 10 == 0) {
        simulateEarthquake(level);
    }

    if (timer % 10 == 0) {
        PacketDistributor.sendToPlayer(player, new SimulationStatusPayload(currentState.name(), timer / 20));
    }
}
```

---

## C-3 · Unguarded static state allows two players to corrupt each other

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 38–47

**Problem:**
`currentState`, `timer`, and `activePlayer` are plain `static` fields. Commands and player
interactions can arrive on network-handler threads concurrently with the server tick thread.
Two players clicking a lobby button at the exact same tick can both pass the `currentState == IDLE`
guard, then overwrite each other's state, causing corrupted simulation state for both players.

**Fix 1 — add `synchronized` to `startSimulation` and `endSimulation`:**

```java
public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
    // ... method body unchanged
}

private static synchronized void endSimulation() {
    // ... method body unchanged
}
```

**Fix 2 — add `volatile` to the three shared fields:**

```java
private static volatile SimulationState currentState = SimulationState.IDLE;
private static volatile int timer = 0;
private static volatile ServerPlayer activePlayer = null;
```
