# Coordinate System and Spawning Logic

This document explains the specific coordinates used in BerongSMP for structures, player teleportation, and world spawning. Understanding these values is crucial for adjusting the simulation environment to your liking.

## 1. Key Locations

The mod defines several fixed points in the world for different purposes. These are primarily located in `SimulationManager.java` and `LobbyManager.java`.

| Location Name | Coordinates (X, Y, Z) | Description |
| :--- | :--- | :--- |
| **Lobby Center** | `(0, 64, 0)` | The central point of the lobby area. |
| **Simulation Center** | `(100, 64, 100)` | The base position where the `.nbt` structure is loaded. |
| **World Spawn** | `(0, 65, 0)` | The default entry point for new players. |

## 2. Structure Placement (.nbt files)

Structures are loaded using Minecraft's `StructureTemplateManager`.

- **Target Position**: The `.nbt` file (e.g., `lspulibrary_main.nbt`) is placed with its **bottom-left-front corner** at the `SIM_POS` (`100, 64, 100`).
- **Clearing Area**: Before placement, the `SimulationManager` clears a box from `(-5, -5, -5)` to `(+35, +25, +35)` relative to the `SIM_POS` to ensure a clean simulation space.

## 3. Player Spawning and Teleportation

### Entering a Simulation
When a simulation starts, the player is teleported to a specific spot inside the loaded structure:
- **Calculation**: `SIM_POS + (5.5, 1.0, 5.5)`
- **Resulting Coord**: `(105.5, 65.0, 105.5)`
- **Reasoning**: The `+0.5` on X and Z centers the player on a block, and `+1.0` on Y ensures they are standing on the floor of the structure.

### Returning to Lobby
When a simulation ends or the player first joins the server:
- **Calculation**: `LOBBY_POS + (0.5, 1.0, 0.5)`
- **Resulting Coord**: `(0.5, 65.0, 0.5)`
- **Reasoning**: This drops the player exactly in the middle of the lobby floor.

## 4. Interaction Points

The lobby contains invisible "hotspots" or specific blocks that trigger actions:

- **Fire Simulation Button**: `(3, 65, -1)` (Relative to Lobby Center)
- **Earthquake Button**: `(3, 65, 1)` (Relative to Lobby Center)

## 5. How to Adjust These Values

If you wish to move the simulation or change where players land, modify the following fields:

1.  **To move the simulation area**: Change `SIM_POS` in `SimulationManager.java`.
2.  **To move the lobby**: Change `LOBBY_POS` in `SimulationManager.java` AND `LobbyManager.java`.
3.  **To change player landing spot**: Adjust the offsets in `SimulationManager.startSimulation()` (line ~51) or `endSimulation()` (line ~131).
4.  **To change the .nbt file**: Update `STRUCTURE_ID` in `SimulationManager.java`.

> **Note**: Always use `.5` for X and Z coordinates when teleporting players to ensure they are centered on the block and don't glitch into walls.
