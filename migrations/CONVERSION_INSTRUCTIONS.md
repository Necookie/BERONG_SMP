# AI Conversion Instructions: Fabric to NeoForge 1.21

This folder contains a complete reference of the "Fire Extinguisher Prototype" mod originally built for Fabric 1.21. Your task is to convert this mod to **NeoForge 1.21**.

## Included Files for Reference:
- **`src/`**: Full Java source code (Fabric/Yarn mappings).
- **`assets/`**: Full resources including textures, models, and lang files.
- **`FUNCTIONALITIES.md`**: Detailed breakdown of the spray logic and mechanics.
- **`MIGRATION_GUIDE.md`**: Technical mapping of Fabric concepts to NeoForge.

## Conversion Requirements:
1.  **Mappings:** Convert from Yarn mappings to official Mojang mappings.
2.  **Mod Setup:** 
    - Create a main class using `@Mod("fireextinguisherprototype")`.
    - Use `DeferredRegister<Item>` for the fire extinguisher.
3.  **Logic Migration:** 
    - Port the `FireExtinguisherItem` class logic.
    - Ensure `usageTick` and `use` methods are correctly overridden for NeoForge.
    - Update particle and sound calls to use NeoForge/Mojang equivalents (e.g., `ServerLevel.sendParticles`).
4.  **Resources:** 
    - Move files from `assets/` to the standard NeoForge structure.
    - Ensure the namespace `fireextinguisherprototype` is preserved.
5.  **Metadata:** Use information from `assets/fabric.mod.json` to populate `neoforge.mods.toml`.

## Goal:
Produce a functional NeoForge 1.21 version that replicates the exact spray behavior, visual effects, and sounds described in the documentation.
