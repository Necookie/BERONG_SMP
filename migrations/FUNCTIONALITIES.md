# Functional Documentation - Fire Extinguisher Logic

## Core Functionality: The Spray Mechanism

The fire extinguisher is a "hold-to-use" item. When a player right-clicks, it triggers a continuous spray action.

### 1. Ray-Casting & Hit Detection
Unlike a simple ray-trace, the spray uses a **cone-based sampling method** to cover an area.
- **Range:** 5.5 blocks (`SPRAY_RANGE`).
- **Logic:**
    - Calculates the player's look vector.
    - Samples points along the direction vector at 7 intervals.
    - At each interval, it also samples points slightly offset to the sides (sideways) and up/down (upwards) to create volume.
    - Total points checked per spray tick: 35 (7 steps * 5 points per step).

### 2. Extinguishing Logic
For each sampled point:
- **Fire/Soul Fire:** If the block is `minecraft:fire` or `minecraft:soul_fire`, the block is broken (removed) without dropping items.
- **Lit Blocks:** If the block state contains the `LIT` property (e.g., Campfires, Candles, Lanterns), it sets `LIT` to `false`.
- **Global Sync:** Triggers `WorldEvents.FIRE_EXTINGUISHED` at the location to play the default extinguish puff.

### 3. Visuals & Audio
- **Particles:**
    - `CLOUD`: Used as the primary "foam" body (24 particles per tick).
    - `POOF`: Adds density to the center of the spray (12 particles per tick).
    - `SMOKE`: Light trailing effect (5 particles per tick).
- **Sound:**
    - `BLOCK_FIRE_EXTINGUISH` played at the player's position.
    - Frequency: Every 6 ticks while holding the item to create a continuous sound loop.

### 4. Continuous Use
- Uses `usageTick` to perform the spray every tick while the item is active.
- `getMaxUseTime` is set to 72,000 (standard for items that can be held indefinitely).
