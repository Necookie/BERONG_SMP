# NeoForge Migration Guide

When migrating this mod to NeoForge 1.21, keep the following changes in mind:

## 1. Project Setup
- Use the NeoForge MDK for 1.21.
- Update `neoforge.mods.toml` instead of `fabric.mod.json`.
- The main class should use `@Mod(MOD_ID)` annotation and the NeoForge event bus for registration.

## 2. Registration
- Use `DeferredRegister<Item>` for registering the Fire Extinguisher.
- Fabric: `Registry.register(Registries.ITEM, ...)`
- NeoForge: `ITEMS.register("fire_extinguisher", () -> new FireExtinguisherItem(...))`

## 3. Item Logic
- **`use` method:** In NeoForge, this is often `use(Level, Player, InteractionHand)`.
- **`usageTick` method:** Similar, but check for `Level` instead of `World` (though in 1.21 NeoForge/Mojang mappings they are very similar).
- **Client/Server check:** Use `!level.isClientSide()` instead of `!world.isClient`.
- **Particles:** `ServerLevel.sendParticles` is equivalent to `ServerWorld.spawnParticles`.

## 4. Package Names
- Fabric uses Yarn mappings (e.g., `net.minecraft.util.math.Vec3d`).
- NeoForge uses official Mojang mappings (e.g., `net.minecraft.world.phys.Vec3`).
- **Translation Map:**
    - `World` -> `Level`
    - `ServerWorld` -> `ServerLevel`
    - `Vec3d` -> `Vec3`
    - `Identifier` -> `ResourceLocation`
    - `BlockPos.ofFloored` -> `BlockPos.containing` (or similar depending on exact mapping version)

## 5. Tooltips
- Use `Component` instead of `Text`.
- Override `appendHoverText` instead of `appendTooltip`.
