# File Descriptions

This document provides a breakdown of each file in the project and its purpose.

## Java Source Files (`src/main/java/net/necookie/disastersim/`)

### Root Package
- **`BerongSMP.java`**: Main mod class. Handles registration and initialization.
- **`BerongSMPClient.java`**: Client-side initialization.
- **`Config.java`**: Mod configuration.

### `api.building`
- **`BuildingComponent.java`**: Interface/base for building-related logic.

### `client`
- **`ClientEvents.java`**: Client-side event listeners (e.g., for HUD).
- **`KeyMappings.java`**: Custom keybinding definitions.
- **`SimulationHud.java`**: HUD overlay for simulation status.

### `command`
- **`ModCommands.java`**: Command registration and logic.

### `item`
- **`FireExtinguisherItem.java`**: Implementation of the Fire Extinguisher tool.

### `network`
- **`SimulationStatusPayload.java`**: Network packet for simulation status.

### `world`
- **`LobbyManager.java`**: Logic for lobby creation and management.
- **`SimulationManager.java`**: Core simulation logic and state management.

### `world.building`
- **`CCSBuildingConstructor.java`**: Builder for CCS buildings.
- **`FurnitureFactory.java`**: Factory for generating furniture in buildings.
- **`modules/`**: Contains modular building components (LSPU Facade, Hallway, etc.).

## Resources (`src/main/resources/`)
- **`assets/berongsmp/`**: Client-side resources (models, textures, lang files).
- **`data/berongsmp/`**: Server-side data (structures, tags).
- **`templates/META-INF/neoforge.mods.toml`**: Mod metadata file.

## Build and Configuration
- **`build.gradle`**: Build script for Gradle.
- **`gradle.properties`**: Project properties (versions, IDs).
- **`settings.gradle`**: Gradle project settings.
