# Technical Details - Class & Registration Structure

## Registry Information
- **Item ID:** `fireextinguisherprototype:fire_extinguisher`
- **Item Settings:**
    - `maxCount(1)`: The item is non-stackable.

## Key Classes
### `FireExtinguisherPrototype` (Main Class)
- Implements `ModInitializer`.
- Handles static registration of the `FIRE_EXTINGUISHER` item.

### `FireExtinguisherItem` (Item Class)
- Extends `Item`.
- **Methods to Migrate:**
    - `use(World, PlayerEntity, Hand)`: Starts the item usage and performs the initial spray.
    - `usageTick(World, LivingEntity, ItemStack, int)`: Handles the continuous spray logic while the button is held.
    - `spray(ServerWorld, PlayerEntity, boolean)`: The core logic for calculating the spray cone and calling `extinguishAt`.
    - `extinguishAt(ServerWorld, BlockPos)`: The logic for modifying the world state.
    - `spawnSprayParticles(ServerWorld, Vec3d, Vec3d, Vec3d, Vec3d)`: Handles particle spawning logic.
    - `appendTooltip(ItemStack, TooltipContext, List<Text>, TooltipType)`: Adds custom descriptions to the item.

## Dependencies (Fabric)
- `fabric-loader`: 0.16.6
- `fabric-api`: 0.102.0+1.21
- `minecraft`: 1.21
- `yarn` mappings
