# place-structure

Test that a structure places correctly in the dev server by running the relevant command, then diagnose any placement issues.

## Usage

Call with an argument specifying which structure to test:
- `lobby` — main sim lobby (`LobbyManager.LOBBY_POS = BlockPos(0,-33,0)`)
- `lspu` — LSPU Library NBT at `SIM_POS = BlockPos(30,-34,83)` via `/spawn_lspu`
- `ssc` — SSC Building `.schem` at `SSC_POS = BlockPos(11,-33,90)` with 1 CCW rotation (placed automatically with LSPU on sim start)
- `tutorial` — Tutorial lobby NBT at `TutorialLobbyManager.TUTORIAL_LOBBY_POS`
- `all` — starts a fire sim (places lobby + LSPU + SSC together) via `/sim_fire`

## Steps

1. Read the relevant loader/manager file to confirm the current `BlockPos` and structure file path
2. Instruct the user to run the appropriate in-game command:
   - lobby: server auto-places on startup — check `run/logs/latest.log` for placement errors
   - lspu / ssc: `/spawn_lspu` (op required)
   - all: `/sim_fire`
3. Ask the user to confirm: did the structure appear? Any blocks floating/misaligned? Any console errors?
4. If the user reports errors, grep `SimulationStructureLoader.java` and `SchemLoader.java` for the relevant load path and diagnose
5. If rotation is wrong on SSC: `SchemLoader` supports 0–3 CCW 90° rotations — check the rotation argument passed in `SimulationManager.BUILDINGS`

## Structure file locations

All under `src/main/resources/data/berongsmp/structure/`:
- `lobby_structure.nbt`
- `lspulibrarymain.nbt`
- `ssc_building.schem`

## Common issues

- Structure not appearing: check that the `.nbt` / `.schem` file exists at the path above
- Blocks offset by 1: SchemLoader origin is corner of bounding box — verify offset math in the placer
- SSC floating or embedded: adjust `SSC_POS` Y value in `SimulationManager`
