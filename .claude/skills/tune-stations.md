# tune-stations

Locate all tutorial station offset constants in TutorialManager and TutorialLobbyManager, then help the user verify and update them against the live lobby interior.

## Context

Station positions are **offsets from `TutorialLobbyManager.TUTORIAL_LOBBY_POS`** (not from `LobbyManager.LOBBY_POS`). The tutorial lobby and the main sim lobby are separate structures.

The constants that need tuning are in:
- `src/main/java/net/necookie/disastersim/tutorial/TutorialManager.java` — `PRACTICE_FIRE` and any `STATION_*` constants
- `src/main/java/net/necookie/disastersim/world/TutorialLobbyManager.java` — `TUTORIAL_LOBBY_POS` and NPC spawn offsets

## Steps

1. Read `TutorialManager.java` and `TutorialLobbyManager.java` fully and list every hardcoded `BlockPos` offset with its current value and purpose
2. Show the user a summary table: constant name | current offset | what it controls
3. Ask the user to run the server (`/run-server`), join the tutorial lobby, and use F3 to read the XYZ of each station block in-game
4. Accept the user's reported coordinates and compute the correct offsets relative to `TUTORIAL_LOBBY_POS`
5. Apply the corrected offsets with Edit — one change per constant, verify no other references break
6. Remind the user to test by: joining fresh (tutorial stage NOT_STARTED), right-clicking each NPC/station in order, and confirming stage advances

## Notes

- `PRACTICE_FIRE` is the center of the 5-block fire cross; the 4 cardinal neighbours are computed automatically — only the center needs tuning
- NPC positions in `TutorialLobbyManager` use `offset(x, y, z)` from `TUTORIAL_LOBBY_POS` — match these to where the villager NPCs actually spawn in the structure
- Stage order: `NOT_STARTED → PASS_SPRAY → EXT_TYPE_A → EXT_TYPE_B → EXT_TYPE_C → QUAKE_DROP → QUAKE_COVER → QUAKE_HOLDON → COMPLETED`
