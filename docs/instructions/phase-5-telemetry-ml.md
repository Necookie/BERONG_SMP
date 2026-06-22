# Phase 5 — ML Telemetry Contract Implementation

> **Status:** `[ ] not started`
> **Repo:** Mod only (`berongsmp-template-26.1.2`)
> **Depends on:** Phase 1–4 complete
> **Telemetry contract:** `telemetry_contract.md` (v1.1) — read it before implementing anything
> **After each numbered goal below:** update `CLAUDE.md` with any new classes/architecture, commit, and push to `main`.

---

## Context

The ML team needs per-tick behavioral telemetry from every simulation session in order to train the MiDRR-Classifier preparedness model. The mod currently logs session-level data only (Turso `sessions` table). Phase 5 adds the full per-tick + event stream defined in `telemetry_contract.md §3–§4`, writes it to a local CSV batch file, and adds two new in-game features the rubric depends on: a **fire alarm block** and a **visual assembly zone**.

All coordinates marked `PLACEHOLDER` must be tuned after walking the structure in-game with `./gradlew runServer` + F3.

---

## Workflow rule for every goal

1. Implement the goal completely.
2. Update `CLAUDE.md` — add/update the Key Classes table and any architecture notes that changed.
3. Update `docs/major_plan.md` Phase 5 deliverables — mark the item `[x]`.
4. `git add` the changed files, commit with a descriptive message, `git push origin main`.

Do not batch multiple goals into one commit. One goal = one commit = one push.

---

## Goal 1 — `FireAlarmBlock`

### New file: `src/main/java/net/necookie/disastersim/block/FireAlarmBlock.java`

```java
package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

public class FireAlarmBlock extends Block {

    public static final Property<Direction> FACING   = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty     ACTIVATED = BooleanProperty.create("activated");

    // Small wall-panel shape (6×8×3 px, centred on block face)
    private static final VoxelShape SHAPE_NORTH = Block.box(5, 4, 13, 11, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5, 4,  0, 11, 12,  3);
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 4, 5, 16, 12, 11);
    private static final VoxelShape SHAPE_EAST  = Block.box( 0, 4, 5,  3, 12, 11);

    public FireAlarmBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ACTIVATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(ACTIVATED, false);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (state.getValue(ACTIVATED)) {
            player.sendSystemMessage(Component.literal("§c[ALARM] Already activated!"));
            return InteractionResult.SUCCESS;
        }

        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || session.getState() != SimulationManager.SimulationState.FIRE) {
            player.sendSystemMessage(Component.literal("§7[ALARM] No active fire — alarm not triggered."));
            return InteractionResult.SUCCESS;
        }

        // Activate
        level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§c🔔 FIRE ALARM ACTIVATED — Evacuate immediately!"));

        // Log contract event
        double t = (net.necookie.disastersim.Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        double hazardDist = SimulationManager.nearestFireDistance((net.minecraft.server.level.ServerLevel) level, player.blockPosition());
        session.logger.log("fire_alarm_activate", java.util.Map.of(
                "t", Math.round(t * 100.0) / 100.0,
                "x", player.getX(),
                "y", player.getY(),
                "z", player.getZ(),
                "hazard_distance", Math.round(hazardDist * 100.0) / 100.0
        ));

        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(ACTIVATED)) return;
        // Pulsing red glow when active
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
        }
    }
}
```

### Register in `BerongSMP.java`

Add after the `COMPUTER_BLOCK` entry:

```java
public static final DeferredBlock<FireAlarmBlock> FIRE_ALARM_BLOCK = BLOCKS.registerBlock(
        "fire_alarm",
        FireAlarmBlock::new,
        () -> Block.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                .strength(0.5f, 4.0f)
                .sound(net.minecraft.world.level.block.SoundType.METAL)
                .lightLevel(s -> s.getValue(FireAlarmBlock.ACTIVATED) ? 7 : 0));

public static final DeferredItem<BlockItem> FIRE_ALARM_ITEM =
        ITEMS.registerSimpleBlockItem("fire_alarm", FIRE_ALARM_BLOCK);
```

Add `FIRE_ALARM_ITEM` to the creative tab `displayItems` lambda.

### Resource files

**`src/main/resources/assets/berongsmp/blockstates/fire_alarm.json`**
```json
{
  "variants": {
    "activated=false,facing=north": { "model": "berongsmp:block/fire_alarm_off" },
    "activated=true,facing=north":  { "model": "berongsmp:block/fire_alarm_on" },
    "activated=false,facing=south": { "model": "berongsmp:block/fire_alarm_off", "y": 180 },
    "activated=true,facing=south":  { "model": "berongsmp:block/fire_alarm_on",  "y": 180 },
    "activated=false,facing=west":  { "model": "berongsmp:block/fire_alarm_off", "y": 270 },
    "activated=true,facing=west":   { "model": "berongsmp:block/fire_alarm_on",  "y": 270 },
    "activated=false,facing=east":  { "model": "berongsmp:block/fire_alarm_off", "y": 90  },
    "activated=true,facing=east":   { "model": "berongsmp:block/fire_alarm_on",  "y": 90  }
  }
}
```

**`src/main/resources/assets/berongsmp/models/block/fire_alarm_off.json`**
```json
{
  "parent": "minecraft:block/cube",
  "textures": {
    "particle": "berongsmp:block/fire_alarm_off",
    "north": "berongsmp:block/fire_alarm_off",
    "south": "berongsmp:block/fire_alarm_off",
    "east": "berongsmp:block/fire_alarm_off",
    "west": "berongsmp:block/fire_alarm_off",
    "up": "berongsmp:block/fire_alarm_off",
    "down": "berongsmp:block/fire_alarm_off"
  }
}
```

**`src/main/resources/assets/berongsmp/models/block/fire_alarm_on.json`** — same but `fire_alarm_on` textures.

**`src/main/resources/assets/berongsmp/models/item/fire_alarm.json`**
```json
{ "parent": "berongsmp:block/fire_alarm_off" }
```

**Textures:** Create two 16×16 PNGs:
- `fire_alarm_off.png` — red box with white pull-lever detail, dark border
- `fire_alarm_on.png` — bright red/orange with glowing button

---

## Goal 2 — Assembly Zone (green force field + `assembly_area_reached`)

### New file: `src/main/java/net/necookie/disastersim/world/AssemblyZone.java`

```java
package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Defines the assembly zone outside the LSPU Library.
 * Emits a green particle force-field border during simulations and fires
 * assembly_area_reached when the player steps inside.
 *
 * IMPORTANT: All coordinates are PLACEHOLDER — tune with F3 after runServer.
 */
public class AssemblyZone {

    // PLACEHOLDER — zone in front of the library (Z < SIM_POS.z = 83).
    // Adjust after walking in-game: should be a clear open area ~10 blocks in front of the exit.
    private static final AABB ZONE = new AABB(25, -35, 65, 55, -28, 82);

    private static final double PARTICLE_SPACING = 1.0;
    private static final int    PARTICLE_HEIGHT   = 5; // how many Y layers of particles

    /**
     * Spawn the green force-field perimeter particles. Call every 5 server ticks
     * from SimulationManager.onServerTick while a simulation is active.
     */
    public static void spawnBorderParticles(ServerLevel level) {
        double minX = ZONE.minX, maxX = ZONE.maxX;
        double minZ = ZONE.minZ, maxZ = ZONE.maxZ;
        double baseY = ZONE.minY;

        for (int layer = 0; layer < PARTICLE_HEIGHT; layer++) {
            double y = baseY + layer;

            // North wall (Z = minZ), South wall (Z = maxZ)
            for (double x = minX; x <= maxX; x += PARTICLE_SPACING) {
                spawnGreen(level, x, y, minZ);
                spawnGreen(level, x, y, maxZ);
            }
            // West wall (X = minX), East wall (X = maxX)
            for (double z = minZ; z <= maxZ; z += PARTICLE_SPACING) {
                spawnGreen(level, minX, y, z);
                spawnGreen(level, maxX, y, z);
            }
        }
    }

    private static void spawnGreen(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
        if (level.getRandom().nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.SCRAPE, x, y + 0.5, z, 1, 0.05, 0.2, 0.05, 0.0);
        }
    }

    /**
     * Returns true if the given position is inside the assembly zone.
     * Called per-tick in SimulationManager to detect assembly_area_reached.
     */
    public static boolean isInside(Vec3 pos) {
        return ZONE.contains(pos);
    }

    /**
     * Called the first tick a player enters the zone during a simulation.
     * Fires the telemetry event, sends a message, and bursts celebration particles.
     */
    public static void onPlayerArrived(ServerPlayer player, SimulationSession session,
                                       ServerLevel level, double hazardDist) {
        player.sendSystemMessage(Component.literal(
                "§a✓ You reached the ASSEMBLY AREA — you are safe!"));

        double t = (net.necookie.disastersim.Config.SIM_DURATION_TICKS.get()
                    - session.getTimerTicks()) / 20.0;
        session.logger.log("assembly_area_reached", java.util.Map.of(
                "t",               Math.round(t * 100.0) / 100.0,
                "x",               player.getX(),
                "y",               player.getY(),
                "z",               player.getZ(),
                "hazard_distance", Math.round(hazardDist * 100.0) / 100.0
        ));

        // Celebration burst
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.x, pos.y + 1, pos.z, 40, 1.0, 0.5, 1.0, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                pos.x, pos.y + 1, pos.z, 15, 0.5, 0.5, 0.5, 0.2);
    }

    public static AABB getZone() { return ZONE; }
}
```

### Wire into `SimulationManager.onServerTick`

In the per-player tick loop, add after the existing fire/quake blocks:

```java
// --- Assembly zone force-field particles (every 5 ticks) ---
if (ticks % 5 == 0) {
    AssemblyZone.spawnBorderParticles(level);
}

// --- Assembly zone arrival check ---
if (!session.hasReachedAssembly() && AssemblyZone.isInside(player.position())) {
    session.markAssemblyReached();
    double hazDist = (session.getState() == SimulationState.FIRE)
            ? nearestFireDistance(level, player.blockPosition())
            : player.position().distanceTo(Vec3.atCenterOf(session.getEpicentre()));
    AssemblyZone.onPlayerArrived(player, session, level, hazDist);
    endSimulation(level, player.getUUID(), "assembly_reached");
    return; // session ended
}
```

Add to `SimulationSession`:
```java
private boolean assemblyReached = false;
public boolean hasReachedAssembly() { return assemblyReached; }
public void markAssemblyReached()   { assemblyReached = true; }
```

---

## Goal 3 — `TelemetryCsvWriter`

### New file: `src/main/java/net/necookie/disastersim/world/TelemetryCsvWriter.java`

Responsibilities:
- On `init()`: create `run/telemetry/` directory if absent; write `map_metadata.json` once if missing.
- `openSession(sessionId, playerId, scenarioType)`: opens/appends `gameplay_logs_<YYYYMMDD>.csv`; writes header row if the file is new; buffers writes.
- `writeRow(...)`: writes one CSV row per the contract §3 schema.
- `closeSession(sessionId, sessionMetadata)`: flushes the event CSV; appends one row to `sessions_<YYYYMMDD>.csv`.
- All file I/O on the server thread (fast, synchronous — this is not Turso).

**CSV schema (contract §3):**
```
player_id,session_id,scenario_type,timestamp,event_type,x,y,z,
hazard_distance,interaction_target,nearby_player_count
```

`nearby_player_count` is blank except on `extinguisher_use` rows.
`interaction_target` is blank except on `door_open` / `extinguisher_use` rows.

**`map_metadata.json` skeleton** (written once, all coords PLACEHOLDER):
```json
{
  "contract_version": "1.1",
  "sim_pos": {"x": 30, "y": -34, "z": 83},
  "scenarios": {
    "fire": {
      "exits": [
        {"label": "main_exit", "x": 0, "y": 0, "z": 0, "note": "PLACEHOLDER"}
      ],
      "assembly_area": {
        "min": {"x": 25, "y": -35, "z": 65},
        "max": {"x": 55, "y": -28, "z": 82},
        "note": "PLACEHOLDER — tune after walking in-game"
      },
      "fire_alarm_positions": [
        {"x": 0, "y": 0, "z": 0, "note": "PLACEHOLDER"}
      ],
      "extinguisher_positions": [
        {"x": 0, "y": 0, "z": 0, "note": "PLACEHOLDER"}
      ],
      "hazard_spawn_zone": {
        "min": {"x": 30, "y": -34, "z": 83},
        "max": {"x": 76, "y": -24, "z": 95},
        "note": "Approximate library footprint — PLACEHOLDER"
      }
    },
    "earthquake": {
      "exits": [],
      "assembly_area": {
        "min": {"x": 25, "y": -35, "z": 65},
        "max": {"x": 55, "y": -28, "z": 82},
        "note": "PLACEHOLDER"
      },
      "hazard_spawn_zone": {
        "note": "Epicenter varies per session — see session metadata"
      }
    }
  }
}
```

Call `TelemetryCsvWriter.init()` from `SimulationManager` static initialiser or from `BerongSMP.onServerStarting`.

---

## Goal 4 — `session_start` / `session_end` contract events

### Modified: `SimulationManager.startSimulation`

After placing buildings and teleporting the player, add:

```java
// t = 0.0 anchor
double startHazDist = (state == SimulationState.FIRE)
        ? 99.0  // fire hasn't spawned yet
        : player.position().distanceTo(Vec3.atCenterOf(session.getEpicentre()));
session.logger.log("session_start", java.util.Map.of(
        "t", 0.0,
        "x", player.getX(), "y", player.getY(), "z", player.getZ(),
        "hazard_distance", startHazDist,
        "scenario_type", state.name().toLowerCase(),
        "session_id", session.getSessionId()
));
TelemetryCsvWriter.openSession(session.getSessionId(),
        player.getUUID().toString(), state.name().toLowerCase());
TelemetryCsvWriter.writeRow(session.getSessionId(), player.getUUID().toString(),
        state.name().toLowerCase(), 0.0, "session_start",
        player.getX(), player.getY(), player.getZ(), startHazDist, null, null);
```

### Modified: `SimulationManager.endSimulation`

Accept an `endReason` String parameter (`"assembly_reached"`, `"injured"`, `"timeout"`). Update all call sites:
- Timer expiry → `"timeout"`
- Player death → `"injured"`
- `AssemblyZone.onPlayerArrived` → `"assembly_reached"`
- `/sim_stop` → `"timeout"`

At the start of `endSimulation`:
```java
double t = (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
session.logger.log("session_end", java.util.Map.of(
        "t", Math.round(t * 100.0) / 100.0,
        "end_reason", endReason,
        "x", player.getX(), "y", player.getY(), "z", player.getZ()
));
TelemetryCsvWriter.writeRow(session.getSessionId(), ...);  // session_end row
TelemetryCsvWriter.closeSession(session.getSessionId(), buildMetadataMap(session, endReason));
```

`buildMetadataMap` collects the §5 fields: `scenario_type`, `started_at`, `ended_at`, `duration_ticks`, `end_reason`, `fires_extinguished_count`, `magnitude`, `aftershock_count`, etc.

`SimulationSession` needs a `sessionId` field (UUID string, set at construction).

---

## Goal 5 — Per-tick `move` sampler + `hazard_distance`

### Modified: `SimulationManager.onServerTick` (inside the per-player loop)

Add a `moveSampleCounter` field to `SimulationSession` (or use `ticks % 2`):

```java
// Sample movement at 10 Hz (every 2 ticks = 0.1 s)
if (ticks % 2 == 0) {
    double t = (Config.SIM_DURATION_TICKS.get() - ticks) / 20.0;
    double hazDist;
    if (session.getState() == SimulationState.FIRE) {
        hazDist = nearestFireDistance(level, player.blockPosition());
    } else {
        hazDist = (session.getEpicentre() != null)
                ? player.position().distanceTo(Vec3.atCenterOf(session.getEpicentre()))
                : 99.0;
    }
    TelemetryCsvWriter.writeRow(
            session.getSessionId(), player.getUUID().toString(),
            session.getState().name().toLowerCase(),
            Math.round(t * 100.0) / 100.0,
            "move",
            player.getX(), player.getY(), player.getZ(),
            Math.round(hazDist * 100.0) / 100.0,
            null, null);
}
```

`nearestFireDistance` already exists at `SimulationManager:431` — make it `public static`.

---

## Goal 6 — `extinguisher_use` contract event with `nearby_player_count`

### Modified: `FireExtinguisherItem.extinguishAt`

After `session.recordExtinguish(1)`, add (fire the event only on the *first* extinguish per spray
gesture — gate with `session.isExtinguishEventPending()`):

```java
if (session.consumeExtinguishEventPending()) {
    int nearbyCount = (int) level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            net.minecraft.world.phys.AABB.ofSize(serverPlayer.position(), 10, 4, 10))
            .stream().filter(p -> !p.getUUID().equals(serverPlayer.getUUID())).count();
    double t = (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
    double hazDist = nearestFireDist(level, serverPlayer.blockPosition());
    TelemetryCsvWriter.writeRow(
            session.getSessionId(), serverPlayer.getUUID().toString(),
            "fire", Math.round(t * 100.0) / 100.0,
            "extinguisher_use",
            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            Math.round(hazDist * 100.0) / 100.0,
            null, nearbyCount);
}
```

Add to `SimulationSession`:
```java
private boolean extinguishEventPending = true;
public boolean consumeExtinguishEventPending() {
    if (!extinguishEventPending) return false;
    extinguishEventPending = false;
    return true;
}
public void resetExtinguishEventPending() { extinguishEventPending = true; }
```

Call `session.resetExtinguishEventPending()` in `FireExtinguisherItem.use()` when the player
releases the item (or at pin-pull time) so the next spray gesture fires the event again.

---

## Goal 7 — `door_open` event

### Modified: `SimulationManager` — new event handler

```java
@SubscribeEvent
public static void onPlayerInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    SimulationSession session = getSession(player.getUUID());
    if (session == null) return;

    BlockState clicked = event.getLevel().getBlockState(event.getPos());
    if (!clicked.is(net.minecraft.tags.BlockTags.DOORS)
            && !clicked.is(net.minecraft.tags.BlockTags.TRAPDOORS)) return;

    double t = (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
    double hazDist = (session.getState() == SimulationState.FIRE)
            ? nearestFireDistance((ServerLevel) event.getLevel(), player.blockPosition())
            : player.position().distanceTo(Vec3.atCenterOf(session.getEpicentre()));
    TelemetryCsvWriter.writeRow(
            session.getSessionId(), player.getUUID().toString(),
            session.getState().name().toLowerCase(),
            Math.round(t * 100.0) / 100.0, "door_open",
            player.getX(), player.getY(), player.getZ(),
            Math.round(hazDist * 100.0) / 100.0,
            clicked.getBlock().getDescriptionId(), null);
}
```

Register this handler: `NeoForge.EVENT_BUS.register(SimulationManager.class)` already covers it since `SimulationManager` uses `@EventBusSubscriber`.

---

## Goal 8 — `emergency_exit` zone check

### New: Exit zone definitions in `AssemblyZone.java` (or a sibling `ExitZones.java`)

```java
// PLACEHOLDER — adjust to match the actual LSPU library door positions after runServer
private static final AABB[] EXIT_ZONES = {
    new AABB(33, -35, 80, 38, -30, 86),  // main front door
    new AABB(70, -35, 87, 76, -30, 93),  // side exit (PLACEHOLDER)
};
```

### Modified: `SimulationSession`

```java
private boolean hasPassedExit = false;
public boolean hasPassedExit()   { return hasPassedExit; }
public void markPassedExit()     { hasPassedExit = true; }
```

### Modified: `SimulationManager.onServerTick` (in move sample block)

```java
if (!session.hasPassedExit()) {
    for (AABB exitZone : ExitZones.getZones()) {
        if (exitZone.contains(player.position())) {
            session.markPassedExit();
            double hazDist = ...; // same pattern as above
            TelemetryCsvWriter.writeRow(..., "emergency_exit", ...);
            break;
        }
    }
}
```

---

## Goal 9 — `map_metadata.json`

Already covered in Goal 3 (`TelemetryCsvWriter.init()` writes it once). Verify it is written to
`run/telemetry/map_metadata.json` on first server start.

After the mod is working in-game, update `map_metadata.json` manually with the real coordinates
found using F3. This file is gitignored from the `run/` directory; commit a template copy to
`docs/map_metadata_template.json` instead.

---

## Verification checklist

- [ ] Run `./gradlew runServer`. Console shows `[TelemetryCsvWriter] Initialized telemetry dir`.
- [ ] Fire alarm block available in creative tab; right-click during FIRE sim activates it (sound + message).
- [ ] Green particles visible outside library during any simulation.
- [ ] Walking into assembly zone ends the simulation with "assembly area" message.
- [ ] After one full FIRE session: `run/telemetry/gameplay_logs_<date>.csv` exists with `session_start`, `move`, `extinguisher_use`, `door_open`, `emergency_exit`, `assembly_area_reached`, `session_end` rows.
- [ ] `run/telemetry/sessions_<date>.csv` has one row for the session.
- [ ] `run/telemetry/map_metadata.json` exists.
- [ ] Send a 1-session sample to the ML team and confirm it passes `validate_raw_schema()`.

When all items checked: update `docs/major_plan.md` Phase 5 → `[x] done`, push to main.
