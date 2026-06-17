# Phase 4 — Low Priority / Design Improvements

> **Prompt for Claude:**
> Apply all three improvements below to the BerongSMP project, in order (L-1 → L-2 → L-3).
> Phases 1, 2, and 3 must already be applied before starting this phase.
> Files to edit:
> - `src/main/java/net/necookie/disastersim/world/SimulationManager.java`
> - `src/main/java/net/necookie/disastersim/world/LobbyManager.java`
>
> L-1 is a larger refactor — create the new class file and update SimulationManager fully.
> L-2 and L-3 are smaller cleanups. Read each file before editing.
> After all three are done, confirm what changed and list any new files created.

---

## L-1 · Only one player can run a simulation at a time

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`

**Problem:**
The single `activePlayer` + single `currentState` design is inherently single-player. For a
classroom context with multiple students, everyone must wait their turn. Two groups cannot run
fire and earthquake simultaneously in different parts of the world.

**Fix — wrap per-session state into a `SimulationSession` data class and maintain a map keyed by player UUID.**

**Step 1** — create a new file `src/main/java/net/necookie/disastersim/world/SimulationSession.java`:
```java
package net.necookie.disastersim.world;

import net.minecraft.server.level.ServerPlayer;

public class SimulationSession {
    public SimulationState state;
    public int timer;
    public ServerPlayer player;

    public SimulationSession(ServerPlayer player, SimulationState state) {
        this.player = player;
        this.state  = state;
        this.timer  = 2400;
    }
}
```

**Step 2** — in `SimulationManager.java`, replace the three static fields:
```java
// Remove these:
private static volatile SimulationState currentState = SimulationState.IDLE;
private static volatile int timer = 0;
private static volatile ServerPlayer activePlayer = null;

// Replace with:
private static final Map<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();
```

Add the required imports:
```java
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
```

**Step 3** — update `startSimulation`, `endSimulation`, and `onServerTick` to operate per-session:

`startSimulation`:
```java
public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
    UUID uuid = player.getUUID();
    if (activeSessions.containsKey(uuid)) {
        player.sendSystemMessage(Component.literal("You already have a simulation in progress!"));
        return;
    }
    activeSessions.put(uuid, new SimulationSession(player, state));
    // ... teleport player, load structure, send HUD packet (same as before but use the session)
}
```

`endSimulation` (by player UUID):
```java
public static synchronized void endSimulation(UUID uuid) {
    SimulationSession session = activeSessions.remove(uuid);
    if (session == null) return;

    ServerPlayer player = session.player;
    if (player != null && player.isAlive()) {
        PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0));
        player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
        loadStructure(level, SIM_POS);
    }
}
```

`onServerTick`:
```java
public static void onServerTick(ServerTickEvent.Post event) {
    for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
        SimulationSession session = activeSessions.get(uuid);
        if (session == null) continue;

        session.timer--;
        if (session.timer <= 0) { endSimulation(uuid); continue; }

        ServerPlayer player = session.player;
        if (player == null || !player.isAlive()) { endSimulation(uuid); continue; }

        ServerLevel level = (ServerLevel) player.level();

        if (session.state == SimulationState.FIRE && session.timer % 20 == 0) {
            simulateFire(level);
        } else if (session.state == SimulationState.EARTHQUAKE && session.timer % 10 == 0) {
            simulateEarthquake(level);
        }

        if (session.timer % 10 == 0) {
            PacketDistributor.sendToPlayer(player,
                new SimulationStatusPayload(session.state.name(), (session.timer + 19) / 20));
        }
    }
}
```

Also update `onPlayerLogout` (from C-1) to use the new map:
```java
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    endSimulation(event.getEntity().getUUID());
}
```

---

## L-2 · All durations, rates, and positions are hard-coded magic numbers

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 35, 72, 146, 161

**Problem:**
Duration (2400 ticks), fire/quake spawn counts, intervals, and area sizes are buried as inline
literals. Changing simulation length or difficulty requires editing source and rebuilding.

**Fix — extract all tuneable values to named constants at the top of `SimulationManager`:**

```java
private static final int SIM_DURATION_TICKS  = 2400; // 2 minutes
private static final int FIRE_SPAWN_COUNT    = 3;    // fires placed per second
private static final int FIRE_SPAWN_INTERVAL = 20;   // ticks between fire spreads
private static final int QUAKE_BREAK_COUNT   = 2;    // blocks broken per interval
private static final int QUAKE_INTERVAL      = 10;   // ticks between quake effects
private static final int SIM_AREA_SIZE       = 25;   // XZ spread of effects
private static final int SIM_AREA_HEIGHT     = 10;   // Y spread of effects
```

Then replace every inline magic number in the method bodies with the matching constant.

---

## L-3 · Button detection uses an O(n³) full structure scan on every server start

**File:** `src/main/java/net/necookie/disastersim/world/LobbyManager.java`, lines 65–87

**Problem:**
`scanForButtons()` loops over every X, Y, Z coordinate in the lobby bounding box — 4000+ block reads
at startup. The assignment of which button is "fire" vs "quake" depends on Z-ordering, a fragile
implicit contract that breaks silently if buttons are ever repositioned.

**Fix — replace the scan with hardcoded `BlockPos` constants derived from known lobby coordinates.**

**Step 1** — before applying this fix, start the server once and check the log for:
```
Lobby buttons found: 2 total. Fire={x, y, z}, Quake={x, y, z}
```
Copy those exact coordinates.

**Step 2** — in `LobbyManager.java`, replace the two nullable fields with fixed offsets from `LOBBY_POS`:
```java
// Remove these:
private static BlockPos fireButtonPos = null;
private static BlockPos quakeButtonPos = null;

// Replace with (fill in real offset values from the log):
private static final BlockPos FIRE_BUTTON_OFFSET  = new BlockPos(5, 2, 3);  // update with real values
private static final BlockPos QUAKE_BUTTON_OFFSET = new BlockPos(5, 2, 9);  // update with real values

private static final BlockPos fireButtonPos  = LOBBY_POS.offset(FIRE_BUTTON_OFFSET);
private static final BlockPos quakeButtonPos = LOBBY_POS.offset(QUAKE_BUTTON_OFFSET);
```

**Step 3** — delete `scanForButtons()` entirely and remove its call inside `createLobby()`.

**Step 4** — since `fireButtonPos` and `quakeButtonPos` are now never null, remove the `lobbyReady`
null-guard from M-1 **only if** all startup timing concerns are resolved. Otherwise, keep the
`lobbyReady` flag but remove the null checks on the button positions themselves.
