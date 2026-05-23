# Project Architecture

BerongSMP follows a standard NeoForge mod structure, organized into several logical layers.

## 1. Core Mod Initialization
- **`BerongSMP.java`**: The main entry point. Handles registration of blocks, items, creative tabs, and common setup events. It also initializes the lobby and world settings upon server start.
- **`Config.java`**: Manages mod configuration using NeoForge's configuration system.

## 2. World and Simulation Logic
- **`SimulationManager.java`**: The "brain" of the disaster simulation. It handles starting/ending simulations, loading structures, and ticking the simulation logic (spreading fire, destroying blocks in earthquakes).
- **`LobbyManager.java`**: Responsible for creating and managing the lobby area.
- **`building/`**: Contains logic for complex building construction and modular components.

## 3. Items
- **`FireExtinguisherItem.java`**: A complex item that handles spray logic, particle effects, and extinguishing fires in a cone-shaped area.

## 4. Networking
- **`SimulationStatusPayload.java`**: Handles custom network packets to sync simulation state (state name, timer) from the server to the client.

## 5. Client-Side Logic
- **`BerongSMPClient.java`**: Client-side entry point.
- **`ClientEvents.java`**: Handles client-specific events like HUD rendering.
- **`SimulationHud.java`**: Renders the simulation timer and status on the player's screen.
- **`KeyMappings.java`**: Defines custom keybindings for player interactions.

## 6. Commands
- **`ModCommands.java`**: Registers and handles custom commands (e.g., to start or stop simulations).
