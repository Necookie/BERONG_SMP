# Phase 3 — Medium Priority Fixes

> **Prompt for Claude:**
> Apply both fixes below to the BerongSMP project, in order (M-1 → M-2).
> Phase 1 and Phase 2 must already be applied before starting this phase.
> Files to edit:
> - `src/main/java/net/necookie/disastersim/world/LobbyManager.java`
> - `src/main/java/net/necookie/disastersim/world/SimulationManager.java`
>
> Read each file before editing. After both fixes are done, confirm what changed.

---

## M-1 · Button positions are null if a player joins before the lobby scan finishes

**File:** `src/main/java/net/necookie/disastersim/world/LobbyManager.java`, lines 42–43, 82–83, 107–115

**Problem:**
`fireButtonPos` and `quakeButtonPos` start as `null` and are only set inside `scanForButtons()`,
which runs at server startup. If a player joins before the scan completes and clicks a button,
`onRightClickBlock` silently does nothing — no message, no feedback. This is especially likely
during the brief window when the server first starts and chunk generation is still happening.

**Fix — add a `lobbyReady` flag that is set only after `scanForButtons()` completes:**

**Step 1** — add the flag after the existing fields in `LobbyManager.java`:
```java
private static boolean lobbyReady = false;
```

**Step 2** — at the end of `createLobby()`, after `scanForButtons()` succeeds, set it to `true`:
```java
scanForButtons(level, LOBBY_POS, template.getSize());
lobbyReady = true; // add this line
BerongSMP.LOGGER.info("BerongSMP Lobby loaded from NBT at {}", LOBBY_POS);
```

**Step 3** — at the top of `onRightClickBlock()`, add the not-ready guard:
```java
if (!lobbyReady) {
    ((ServerPlayer) event.getEntity()).sendSystemMessage(
        Component.literal("The lobby is still loading, please wait a moment.")
    );
    return;
}
```

---

## M-2 · No player-alive check before teleport

**File:** `src/main/java/net/necookie/disastersim/world/SimulationManager.java`, lines 174–191

**Problem:**
Both teleport calls in `endSimulation()` run unconditionally. If the player disconnected between
the guard check and the teleport, `player.teleportTo(...)` silently fails and `loadStructure` uses
`activePlayer.level()` even though `activePlayer` is set to `null` just a few lines later, leaving
the structure in a broken state. Also, if anything in the teleport block throws, the manager is
left stuck in a non-IDLE state.

**Fix — capture `activePlayer` into a local, reset state first, then validate before teleporting:**

Replace the full body of `endSimulation()` with:
```java
public static synchronized void endSimulation() {
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

Also add the missing import at the top of the file if not already present:
```java
import java.util.Collections;
```
