# check-tutorial

Audit all TutorialStage transitions across the codebase and verify they match the documented stage order. Flags any gaps, dead stages, or handler mismatches.

## Expected stage order

`NOT_STARTED → PASS_SPRAY → EXT_TYPE_A → EXT_TYPE_B → EXT_TYPE_C → QUAKE_DROP → QUAKE_COVER → QUAKE_HOLDON → COMPLETED`

## Steps

1. Read `TutorialStage.java` — confirm all enum values are present and in the expected order
2. Grep for every `TutorialStage.` reference across the entire `src/` tree
3. Build a transition map: for each stage, what code advances TO it, and FROM it
4. Check `TutorialManager.java` tick logic handles `QUAKE_DROP`, `QUAKE_COVER`, `QUAKE_HOLDON` with correct player-action detection
5. Check `LobbyManager.onRightClickBlock` gates sim buttons with `TutorialManager.isComplete(uuid)` — if this guard is missing, simulations can start without tutorial completion
6. Check `TutorialSavedData` loads/saves all stages without data loss (especially after a server restart mid-tutorial)
7. Report: list of stages with ✓ (handler found) or ✗ (no handler / unreachable); any stages in `TutorialStage.java` not handled anywhere; any hardcoded string comparisons instead of enum references

## Notes

- `TutorialManager.isComplete(UUID)` should return `true` only when stage == `COMPLETED`
- The QUAKE stages are tick-driven (not click-driven) — missing a `TutorialManager.tick()` call in `SimulationManager.onServerTick` would silently stall all players at `QUAKE_DROP`
- `TutorialSavedData` persists to `world/data/berongsmp_tutorial.dat` — corruption here would reset all progress
