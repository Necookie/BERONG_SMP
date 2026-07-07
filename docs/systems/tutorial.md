# Tutorial Flow

Players must complete a safety tutorial before simulation buttons become active. Progress persists across disconnects via `TutorialSavedData` — **except** `QUAKE_DROP`/`QUAKE_COVER`/`QUAKE_HOLDON`, which `TutorialManager.rollbackOnLogout` (registered with `PlayerLifecycleRegistry`, see `common/player/`) rolls back to `QUAKE_INTRO` on logout. Without that, `TutorialManager.tick` (unconditional, every online player, every tick, driven purely by the persisted stage) would resume sending the 1.5f shake prompt on its own 10-tick clock the instant the player reconnected, with no dialogue re-triggered — this was the actual cause of an "earthquake never stops after exiting and reloading" report (a separate, since-fixed instance of the same bug class also existed in the Academy's `academy.room3.SantosRoomManager`). `TutorialManager`'s transient maps (`holdOnTimers`/`extinguishCounts`/`dialogueSteps`) are cleared on the same logout hook — they are plain static fields that do **not** reset on their own for a same-session "Save and Quit to Title" (only a real JVM restart clears static state).

```
Stage order: NOT_STARTED → PASS_PULL → PASS_SPRAY → EXT_TYPE_A → EXT_TYPE_B → EXT_TYPE_C
             → QUAKE_INTRO → QUAKE_DROP → QUAKE_COVER → QUAKE_HOLDON → COMPLETED

Sgt. Reyes NPC (TRAINER): talks during NOT_STARTED/PASS_PULL → gives extinguisher, spawns 5 campfires at PRACTICE_FIRE (+7,2,11)
  → stage = PASS_SPRAY

FireExtinguisherItem.extinguishAt → TutorialManager.onExtinguish (unconditional):
  → counts extinguishes while stage == PASS_SPRAY; at 3 → remove campfires, stage = EXT_TYPE_A

Officer Cruz NPC (EXT_EXPERT): talks during EXT_TYPE_A/B/C in order → class info, stage advances A→B→C→QUAKE_INTRO

Capt. Santos NPC (SAFETY_OFFICER): talks during QUAKE_INTRO (5 lines, last advancesStage=true)
  → final line triggers QUAKE_DROP; earthquake drill begins

TutorialManager.tick (registered with TickScheduler, dispatched from SimulationManager.onServerTick):
  QUAKE_DROP  → shake prompt every 10 ticks; player crouches → QUAKE_COVER
  QUAKE_COVER → player crouches + solid block at blockPos.above(2) → QUAKE_HOLDON
  QUAKE_HOLDON → hold condition 100 ticks; intensity fades 1.5→0; break cover resets timer
               → at 100 ticks: stage = COMPLETED, confetti particles, clear HUD

LobbyManager.onRightClickBlock gates fire/quake buttons:
  if (!TutorialManager.isComplete(uuid)) → "Complete the safety tutorial first!" — no simulation starts
```

Station constants in `TutorialManager` (offsets from `LobbyManager.LOBBY_POS = (0,-33,0)`) are **placeholder values** — tune them against the actual lobby interior when running `./gradlew runServer`.

