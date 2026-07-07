# new-stage

Add a new TutorialStage enum value and wire up all the required handler code.

## Usage

Call with the stage name and trigger type:
- `new-stage <NAME> click` — stage advances when the player right-clicks a specific block/NPC
- `new-stage <NAME> tick` — stage advances based on a tick-driven condition (crouching, holding, timer, etc.)
- `new-stage <NAME> extinguish` — stage advances when the player extinguishes N fire blocks

## Steps

1. Read these files in full before making any changes:
   - `src/main/java/net/necookie/disastersim/tutorial/TutorialStage.java`
   - `src/main/java/net/necookie/disastersim/tutorial/TutorialManager.java`
   - `src/main/java/net/necookie/disastersim/common/structure/LobbyManager.java` (for click dispatch)

2. Add the new enum value to `TutorialStage.java` in the correct position in the progression sequence

3. Wire up the handler based on trigger type:
   - **click**: add a case in `TutorialManager.onNpcInteract()` or `TutorialManager.onStationInteract()` matching the new stage; advance stage at end of handler
   - **tick**: add a branch in `TutorialManager.tick()` for the new stage; define the condition and `setStage(player, TutorialStage.<NAME>)` when met
   - **extinguish**: add a check in `TutorialManager.onExtinguish()` for the new stage alongside the existing `PASS_SPRAY` count logic

4. Ensure `TutorialSavedData` does not need changes — it stores stages by enum name so new values are automatically persisted

5. If the stage needs a HUD prompt: send a `TutorialStatusPayload` with the prompt text via `PacketDistributor` to the player

6. Run `/check-tutorial` after the change to verify the full stage graph is consistent

## Notes

- Never skip a stage in the enum order — `TutorialManager.isComplete()` only checks for `COMPLETED`; earlier stages don't gate each other by position
- Transient per-player state (timers, counters) goes in `ConcurrentHashMap` fields at the top of `TutorialManager`, not in `TutorialSavedData`
- The QUAKE tick stages (`QUAKE_DROP`, `QUAKE_COVER`, `QUAKE_HOLDON`) are good examples of tick-driven stages — refer to them when adding a new tick stage
