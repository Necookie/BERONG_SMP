# BerongSMP – Simulation Fix & Improvement Checklist

> Items are grounded in the actual source code with exact file paths, line numbers, and code snippets.
> Work top-to-bottom — each tier builds on the previous being stable.

---

## 🔴 Critical — Fix Before Any Release

These bugs can permanently break the simulation for **all players** until the server is restarted.
No other bugs matter if the server gets stuck in this state.

---

### C-1 · Player disconnect permanently freezes the simulation

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`

**Why this is a problem**

When a player starts a simulation and then disconnects (closes the game, loses connection, or crashes),
`activePlayer` still holds a reference to that player object and `currentState` stays `FIRE` or `EARTHQUAKE`.
The tick handler (`onServerTick`, line 115) guards itself with `if (currentState == IDLE || activePlayer == null) return`,
so it keeps running the dead simulation forever. When any other player tries to start a simulation,
`startSimulation` (line 65) sees `currentState != IDLE` and rejects them:

```java
// SimulationManager.java line 65-68
if (currentState != SimulationState.IDLE) {
    player.sendSystemMessage(Component.literal("A simulation is already in progress!"));
    return;
}
```

The server is now permanently locked out of simulations until a restart.

**What needs to change**

A `PlayerLoggedOutEvent` listener needs to be added to `SimulationManager`.
When the event fires, check if the disconnecting player is the `activePlayer`. If so, call `endSimulation()`.

**How to implement**

Add this method inside `SimulationManager.java`:

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

### C-2 · Null pointer race between the null-guard and actual usage

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 113–137

**Why this is a problem**

`onServerTick` checks `activePlayer == null` on line 115, but then uses `activePlayer` again on lines 127, 131, and 136.
Between the guard check and those usages, another thread (a command, a network event) could call `endSimulation()`
which sets `activePlayer = null` on line 190. The tick handler then tries to call `.level()` on null — a `NullPointerException` that crashes the server.

```java
// SimulationManager.java line 113-136 — the dangerous pattern
public static void onServerTick(ServerTickEvent.Post event) {
    if (currentState == SimulationState.IDLE || activePlayer == null) return; // ← checked here

    timer--;
    if (timer <= 0) { endSimulation(); return; }

    if (currentState == SimulationState.FIRE && timer % 20 == 0) {
        simulateFire((ServerLevel) activePlayer.level()); // ← used here — activePlayer may now be null!
    }
    else if (currentState == SimulationState.EARTHQUAKE && timer % 10 == 0) {
        simulateEarthquake((ServerLevel) activePlayer.level()); // ← and here
    }

    if (timer % 10 == 0) {
        PacketDistributor.sendToPlayer(activePlayer, ...); // ← and here
    }
}
```

**What needs to change**

Capture `activePlayer` into a local variable immediately after the null-guard.
A local variable cannot be changed by another thread mid-method,
so every usage below it is guaranteed to be the same non-null reference.

**How to implement**

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

### C-3 · Unguarded static state allows two players to corrupt each other

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 38–47

**Why this is a problem**

`currentState`, `timer`, and `activePlayer` are plain `static` fields:

```java
// SimulationManager.java lines 38-47
private static SimulationState currentState = SimulationState.IDLE;
private static int timer = 0;
private static ServerPlayer activePlayer = null;
private static final Random random = new Random();
```

The Minecraft server tick runs on one thread, but commands and player interactions can arrive on
network-handler threads. If Player A and Player B both click a lobby button at the exact same tick:

1. Thread A reads `currentState == IDLE` → passes the guard
2. Thread B reads `currentState == IDLE` → passes the guard
3. Thread A writes `currentState = FIRE`, `activePlayer = playerA`
4. Thread B writes `currentState = EARTHQUAKE`, `activePlayer = playerB` — **overwrites Thread A**

Now `currentState` is EARTHQUAKE but both players were told they started a simulation. Player A gets
an earthquake sim she didn't choose, and her timer and state are half-owned by Player B's thread.

**What needs to change**

`startSimulation()` and `endSimulation()` need to be `synchronized` so only one thread can modify
state at a time.

**How to implement**

```java
public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
    if (currentState != SimulationState.IDLE) {
        player.sendSystemMessage(Component.literal("A simulation is already in progress!"));
        return;
    }
    // ... rest of the method unchanged
}

private static synchronized void endSimulation() {
    // ... method body unchanged
}
```

Also add `volatile` to the three shared fields so that changes made inside `synchronized` blocks
are immediately visible to the tick thread without requiring it to also synchronize:

```java
private static volatile SimulationState currentState = SimulationState.IDLE;
private static volatile int timer = 0;
private static volatile ServerPlayer activePlayer = null;
```

---

## 🟠 High — Fix Soon

These cause noticeable bugs during normal play.

---

### H-1 · Shared `Random` is not thread-safe

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 47, 147, 162

**Why this is a problem**

`java.util.Random` maintains internal state that it updates on every call. If two threads call
`random.nextInt()` simultaneously, both read the old state, compute different values, and then
both try to write back — one write is lost, producing duplicate values or no change at all.
Over time this causes fire/earthquake to cluster in the same spots rather than being random,
or can throw `ArrayIndexOutOfBoundsException` internally.

```java
// SimulationManager.java line 47
private static final Random random = new Random(); // shared, not thread-safe

// line 147
BlockPos firePos = SIM_POS.offset(random.nextInt(25), random.nextInt(10), random.nextInt(25));

// line 162
BlockPos breakPos = SIM_POS.offset(random.nextInt(25), random.nextInt(10), random.nextInt(25));
```

**What needs to change**

Remove the shared `Random` field entirely. Minecraft's `ServerLevel` already provides a
thread-safe random source through `level.getRandom()`. Use that instead.

**How to implement**

Delete line 47:
```java
private static final Random random = new Random(); // DELETE THIS LINE
```

Delete the import on line 23:
```java
import java.util.Random; // DELETE THIS LINE
```

In `simulateFire()` at line 147, change to:
```java
BlockPos firePos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
```

In `simulateEarthquake()` at line 162, change to:
```java
BlockPos breakPos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
```

---

### H-2 · No way to stop a simulation early

**File:** `src/main/java/net/necookie/disastersim/command/ModCommands.java`

**Why this is a problem**

Once a simulation starts it runs for exactly 2400 ticks (2 minutes) with no way out.
If a bug occurs, the player gets stuck in the void, the wrong simulation type started,
or the teacher needs to reset mid-session, there is no mechanism to do so without a server restart.
This also makes testing slow because you must always wait 2 minutes for the structure to reset.

**What needs to change**

Add a `/sim_stop` command in `ModCommands.java` that calls `endSimulation()`.
`endSimulation()` is currently `private` — it needs to be changed to `public static` so `ModCommands` can call it.

**How to implement**

In `SimulationManager.java` line 174, change `private` to `public`:
```java
public static synchronized void endSimulation() { ... }
```

In `ModCommands.register()`, add the new command after the existing ones:
```java
dispatcher.register(Commands.literal("sim_stop")
    .executes(context -> {
        SimulationManager.endSimulation();
        context.getSource().sendSuccess(() -> Component.literal("Simulation stopped."), true);
        return 1;
    }));
```

---

### H-3 · Timer HUD shows wrong value for up to 1 full second

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, line 136
**File:** `src/main/java/net/necookie/disastersim/network/SimulationStatusPayload.java`

**Why this is a problem**

On line 136, `timer / 20` uses integer division to convert ticks to seconds before sending to the client:

```java
// SimulationManager.java line 136
PacketDistributor.sendToPlayer(activePlayer, new SimulationStatusPayload(currentState.name(), timer / 20));
```

When `timer = 2400`, `2400 / 20 = 120` — correct.
When `timer = 2399`, `2399 / 20 = 119` — correct.
When `timer = 2381`, `2381 / 20 = 119` — still 119, even though 119.05 seconds remain.
When `timer = 2380`, `2380 / 20 = 119` — still 119.
When `timer = 2361`, `2361 / 20 = 118` — jumps from 119 to 118, skipping a step visually.

The HUD effectively shows the timer floored to the nearest second rather than rounded up,
so the display reads `0s` during the entire first second of the simulation.

**What needs to change**

Use ceiling division `(timer + 19) / 20` so the display rounds up instead of truncating.
Ticks 1–19 will display `1s`, ticks 20–39 will display `2s`, and so on.

**How to implement**

On line 136, change:
```java
// Before
new SimulationStatusPayload(currentState.name(), timer / 20)

// After
new SimulationStatusPayload(currentState.name(), (timer + 19) / 20)
```

---

## 🟡 Medium — Fix When Possible

These cause silent failures or confusing behavior on edge cases, but won't crash the server.

---

### M-1 · Button positions are null if a player joins before the lobby scan finishes

**File:** `src/main/java/net/necookie/disastersim/world/LobbyManager.java`, lines 42–43, 82–83, 107–115

**Why this is a problem**

`fireButtonPos` and `quakeButtonPos` start as `null` and are only set inside `scanForButtons()`,
which runs as part of `createLobby()` at server startup:

```java
// LobbyManager.java lines 42-43
private static BlockPos fireButtonPos = null;
private static BlockPos quakeButtonPos = null;
```

`onRightClickBlock` guards against this with `if (fireButtonPos != null && ...)` (line 107),
so clicking a button while positions are null does nothing — no message, no feedback.
If a player joins before the lobby structure fully loads and the scan completes,
they can click the buttons and get complete silence. This is especially likely during the brief
window when the server first starts and chunk generation is still happening.

**What needs to change**

Add a `lobbyReady` flag that is set to `true` only after `scanForButtons()` completes.
Guard `onRightClickBlock` at the top with a check on this flag and send a message if not ready.

**How to implement**

Add the flag after the existing fields in `LobbyManager.java`:
```java
private static boolean lobbyReady = false;
```

At the end of `createLobby()`, after `scanForButtons()` succeeds, set it:
```java
scanForButtons(level, LOBBY_POS, template.getSize());
lobbyReady = true; // add this line
BerongSMP.LOGGER.info("BerongSMP Lobby loaded from NBT at {}", LOBBY_POS);
```

At the top of `onRightClickBlock()`, add the guard:
```java
if (!lobbyReady) {
    ((ServerPlayer) event.getEntity()).sendSystemMessage(
        Component.literal("The lobby is still loading, please wait a moment.")
    );
    return;
}
```

---

### M-2 · No player-alive check before teleport

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 80 and 182

**Why this is a problem**

Both teleport calls run unconditionally. If the player's connection dropped between the guard check
and the teleport call, `player.teleportTo(...)` silently fails and the player remains wherever they were.
More importantly, line 185 calls `loadStructure((ServerLevel) activePlayer.level(), SIM_POS)` using `activePlayer`
even though `activePlayer` is already set to null on line 190 in the reset block that follows —
the `level()` call on line 185 happens first, but on a player whose connection state is unknown.

```java
// SimulationManager.java lines 174-191
private static void endSimulation() {
    if (activePlayer != null) {
        PacketDistributor.sendToPlayer(activePlayer, new SimulationStatusPayload("", 0));
        activePlayer.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
        activePlayer.teleportTo(...); // line 182 — what if they're disconnected?
        loadStructure((ServerLevel) activePlayer.level(), SIM_POS); // line 185 — uses activePlayer again
    }
    currentState = SimulationState.IDLE;
    activePlayer = null; // line 190
    timer = 0;
}
```

**What needs to change**

Capture `activePlayer` into a local variable at the start of `endSimulation()` (same pattern as C-2),
and validate the player is still alive before teleporting. The structure reload does not depend on
the player being alive, so it should use a cached `level` reference, not `activePlayer.level()`.

**How to implement**

```java
private static synchronized void endSimulation() {
    ServerPlayer player = activePlayer; // capture before clearing

    // Reset state first so the tick handler stops immediately
    currentState = SimulationState.IDLE;
    activePlayer = null;
    timer = 0;

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

Note: Resetting state **before** the teleport means if anything in the teleport throws,
the manager is still in a clean IDLE state rather than stuck.

---

## 🔵 Low — Design Improvements

These do not break anything today but limit the project as it grows.

---

### L-1 · Only one player can run a simulation at a time

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 38–44

**Why this is a problem**

The single `activePlayer` + single `currentState` design means this is inherently single-player.
For a classroom context with multiple students, everyone must wait their turn.
If two groups want to run fire and earthquake simultaneously in different parts of the world, that is impossible.

**What needs to change**

Wrap all per-session state into a `SimulationSession` data class, and maintain a
`Map<UUID, SimulationSession>` keyed by player UUID. Each player gets their own independent session.

**How to implement**

Create a new class `SimulationSession.java`:
```java
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

In `SimulationManager`, replace the three static fields with a map:
```java
private static final Map<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();
```

Update `startSimulation`, `endSimulation`, and `onServerTick` to operate per-session
by iterating the map. This is a larger refactor but unlocks true multi-player support.

---

### L-2 · All durations, rates, and positions are hard-coded magic numbers

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 35, 72, 146, 161

**Why this is a problem**

The following values are buried in code with no way to change them without recompiling:

```java
public static final BlockPos SIM_POS = new BlockPos(30, -34, 83); // line 35
timer = 2400; // line 72 — 2 minutes, no explanation in context
for (int i = 0; i < 3; i++) { ... } // line 146 — 3 fires per second, why 3?
for (int i = 0; i < 2; i++) { ... } // line 161 — 2 blocks per 0.5s, why 2?
```

To change the simulation from 2 minutes to 3 minutes, or increase fire intensity for a harder run,
you must edit source code and rebuild the mod. For a demo/educational tool this is impractical.

**What needs to change**

Move all tuneable values to named constants at the top of `SimulationManager`
(a quick fix), or better yet to the existing `Config.java` file (the proper fix).

**How to implement (quick fix — named constants):**

```java
// At the top of SimulationManager.java, replace the scattered magic numbers:
private static final int SIM_DURATION_TICKS  = 2400; // 2 minutes
private static final int FIRE_SPAWN_COUNT    = 3;    // fires placed per second
private static final int FIRE_SPAWN_INTERVAL = 20;   // ticks between fire spreads
private static final int QUAKE_BREAK_COUNT   = 2;    // blocks broken per interval
private static final int QUAKE_INTERVAL      = 10;   // ticks between quake effects
private static final int SIM_AREA_SIZE       = 25;   // XZ spread of effects
private static final int SIM_AREA_HEIGHT     = 10;   // Y spread of effects
```

Then replace the magic numbers in the method bodies with these constants.

---

### L-3 · Button detection uses an O(n³) full structure scan on every server start

**File:** `src/main/java/net/necookie/disastersim/world/LobbyManager.java`, lines 65–87

**Why this is a problem**

`scanForButtons()` loops over every X, Y, Z coordinate in the lobby structure's bounding box
to find button blocks:

```java
// LobbyManager.java lines 68-77
for (int x = 0; x < size.getX(); x++) {
    for (int y = 0; y < size.getY(); y++) {
        for (int z = 0; z < size.getZ(); z++) {
            BlockPos pos = origin.offset(x, y, z);
            if (level.getBlockState(pos).getBlock() instanceof ButtonBlock) {
                buttons.add(pos.immutable());
            }
        }
    }
}
```

For a 20×10×20 lobby that is 4000 block reads at startup — not terrible now,
but every `getBlockState()` call is a chunk lookup. If the lobby ever grows or moves to a larger
structure, this becomes a noticeable hitch. More importantly, the entire assignment logic
depends on Z-ordering (`buttons.sort(Comparator.comparingInt(BlockPos::getZ))`),
which is a fragile implicit contract: whoever places the buttons must remember that lower Z = fire.

**What needs to change**

Replace the scan with hardcoded `BlockPos` constants derived from known lobby coordinates.
Since the lobby always loads at `LOBBY_POS = (0, -33, 0)` with a fixed structure, the button
positions are always predictable after the first manual lookup.

**How to implement**

1. Load the server once, run `/sim_fire` and inspect the log output that already prints:
   `"Lobby buttons found: 2 total. Fire={}, Quake={}"`
2. Copy those exact coordinates into constants:

```java
// Replace the two nullable fields with fixed offsets from LOBBY_POS
private static final BlockPos FIRE_BUTTON_OFFSET  = new BlockPos(5, 2, 3);  // fill in real values
private static final BlockPos QUAKE_BUTTON_OFFSET = new BlockPos(5, 2, 9);  // fill in real values

// Derive absolute positions
private static final BlockPos fireButtonPos  = LOBBY_POS.offset(FIRE_BUTTON_OFFSET);
private static final BlockPos quakeButtonPos = LOBBY_POS.offset(QUAKE_BUTTON_OFFSET);
```

3. Delete `scanForButtons()` entirely and remove the call to it in `createLobby()`.

---

## Reference — Files to Edit

| Fix ID | File | Lines Affected |
|--------|------|----------------|
| C-1 | `world/SimulationManager.java` | Add new method + import |
| C-2 | `world/SimulationManager.java` | 113–137 (onServerTick body) |
| C-3 | `world/SimulationManager.java` | 38–44 (fields), 63 & 174 (method signatures) |
| H-1 | `world/SimulationManager.java` | 47 (field), 147, 162 (usages) |
| H-2 | `world/SimulationManager.java` | 174 (make endSimulation public) |
| H-2 | `command/ModCommands.java` | Add `/sim_stop` in register() |
| H-3 | `world/SimulationManager.java` | 136 (timer division) |
| M-1 | `world/LobbyManager.java` | 42–43 (add flag), 57 (set flag), 101 (guard) |
| M-2 | `world/SimulationManager.java` | 174–191 (endSimulation body) |
| L-1 | `world/SimulationManager.java` | Full refactor — new SimulationSession class |
| L-2 | `world/SimulationManager.java` | 35, 72, 146, 161 (extract constants) |
| L-3 | `world/LobbyManager.java` | 42–43 (replace nullables), 65–87 (delete scan) |
