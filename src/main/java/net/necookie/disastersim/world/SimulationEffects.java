package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Applies the per-tick world effects for each disaster type.
 *
 * <p>This class is intentionally decoupled from session management and networking:
 * it receives only the {@link ServerLevel} it should mutate and operates purely
 * on world state. Effect parameters (spawn counts, intervals, area size) are read
 * from {@link Config} at call time so that server-side config reloads take effect
 * without a restart.
 *
 * <p>All methods must be called on the server thread.
 */
public class SimulationEffects {

    /**
     * Blocks that become visible falling entities (and deal fall damage) when broken by the earthquake.
     * Uses Set.of for O(1) membership test — extend this list to add more debris types.
     */
    private static final Set<Block> DEBRIS_BLOCKS = Set.of(
        Blocks.OAK_PLANKS,  Blocks.OAK_SLAB,  Blocks.OAK_STAIRS,
        Blocks.OAK_FENCE,   Blocks.OAK_FENCE_GATE, Blocks.OAK_TRAPDOOR,
        Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS,
        Blocks.SPRUCE_FENCE,  Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_TRAPDOOR,
        Blocks.BIRCH_PLANKS, Blocks.BIRCH_SLAB, Blocks.BIRCH_STAIRS,
        Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS,
        Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE_GATE,
        Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS,
        Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.DARK_OAK_LOG,
        Blocks.BOOKSHELF, Blocks.CHISELED_BOOKSHELF,
        Blocks.GLASS, Blocks.GLASS_PANE
    );

    /**
     * Places a small cluster of fire blocks at random air positions within the
     * simulation arena.
     *
     * <p>The number of fires placed and the area they can appear in are controlled
     * by {@link Config#FIRE_SPAWN_COUNT} and {@link Config#SIM_AREA_SIZE}/
     * {@link Config#SIM_AREA_HEIGHT}.
     *
     * @param level The server level containing the simulation arena.
     */
    public void simulateFire(ServerLevel level) {
        int count      = Config.FIRE_SPAWN_COUNT.get();   // How many fire blocks to try placing this call
        int areaSize   = Config.SIM_AREA_SIZE.get();      // X and Z range (e.g., 25 blocks)
        int areaHeight = Config.SIM_AREA_HEIGHT.get();    // Y range above SIM_POS (e.g., 10 blocks)

        for (int i = 0; i < count; i++) {
            // Pick a random position inside the simulation arena bounding box.
            // nextInt(areaSize) returns 0..(areaSize-1), so fire stays within the box.
            BlockPos firePos = SimulationManager.SIM_POS.offset(
                    level.getRandom().nextInt(areaSize),
                    level.getRandom().nextInt(areaHeight),
                    level.getRandom().nextInt(areaSize));

            // Only place fire if:
            //   1. The target position is air (don't overwrite solid blocks).
            //   2. The block directly below is NOT air (require a solid floor).
            // Condition 2 prevents fire from appearing mid-air in open spaces outside
            // the library walls, which would look wrong and spread uncontrollably.
            if (level.getBlockState(firePos).isAir()
                    && !level.getBlockState(firePos.below()).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    /**
     * Removes any fire blocks that have spread outside the simulation arena bounds.
     * Scans a margin of 3 blocks beyond the configured area on each axis and
     * extinguishes fire found there.
     *
     * @param level The server level to clean up.
     */
    public void cleanupFireOutsideBounds(ServerLevel level) {
        int size   = Config.SIM_AREA_SIZE.get();   // Legitimate X/Z span of the arena
        int height = Config.SIM_AREA_HEIGHT.get(); // Legitimate Y span of the arena
        int margin = 3; // Extra blocks scanned beyond the arena edge to catch fire that spread out

        // The triple loop walks every position in the arena + its surrounding margin.
        // dx/dy/dz are offsets relative to SIM_POS, so the full scan covers:
        //   X: SIM_POS.x - 3  ..  SIM_POS.x + size + 2
        //   Y: SIM_POS.y - 3  ..  SIM_POS.y + height + 2
        //   Z: SIM_POS.z - 3  ..  SIM_POS.z + size + 2
        for (int dx = -margin; dx < size + margin; dx++) {
            for (int dy = -margin; dy < height + margin; dy++) {
                for (int dz = -margin; dz < size + margin; dz++) {

                    // Skip positions that are INSIDE the legitimate arena bounds —
                    // fire there is intentional and should not be removed.
                    // A position is inside if all three offsets are in [0, size/height).
                    if (dx >= 0 && dx < size && dy >= 0 && dy < height && dz >= 0 && dz < size) {
                        continue;
                    }

                    // For every position in the margin zone, check for rogue fire and
                    // remove it.  removeBlock(pos, false) removes without dropping items.
                    BlockPos pos = SimulationManager.SIM_POS.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    /**
     * Phase-aware earthquake effect dispatcher. Delegates to a phase-specific helper
     * and is called at {@link Config#QUAKE_INTERVAL} ticks by SimulationManager.
     * PEAK-phase also queues unsupported blocks; drain those each tick via
     * {@link #drainEarthquakePending}.
     *
     * @param level   The server level containing the simulation arena.
     * @param session The active session carrying epicenter, phase, and cascade queue.
     */
    public void simulateEarthquake(ServerLevel level, SimulationSession session) {
        if (session.getEpicenter() == null || session.getQuakePhase() == null) return;
        switch (session.getQuakePhase()) {
            case RUMBLE     -> doRumble(level, session);
            case PEAK       -> enqueuePeakDestructions(level, session);
            case AFTERSHOCK -> doAftershock(level, session);
            case END        -> {} // nothing to do
        }
    }

    /**
     * Drains up to 2 entries from the cascade queue and destroys them.
     * Called every tick (not just at the quake interval) so destruction spreads gradually.
     * No-op when the queue is empty or the session is not in PEAK phase.
     */
    public void drainEarthquakePending(ServerLevel level, SimulationSession session) {
        if (session.getQuakePhase() != SimulationSession.EarthquakePhase.PEAK) return;
        var queue = session.getPendingDestructions();
        int toBreak = Math.min(2, queue.size());
        for (int i = 0; i < toBreak; i++) {
            BlockPos pos = queue.poll();
            if (pos != null) breakOrDebris(level, pos);
        }
    }

    // --- Phase helpers ---

    /**
     * Breaks one block. Debris-eligible blocks (wood, glass) become a FallingBlockEntity
     * that is visible, falls with gravity, and deals fall damage on landing.
     * Other blocks vanish instantly. Bedrock and air are skipped.
     */
    private void breakOrDebris(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) return;
        if (DEBRIS_BLOCKS.contains(state.getBlock())) {
            FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
            debris.setHurtsEntities(2.0f, 40);
            debris.disableDrop();
        } else {
            level.destroyBlock(pos, false);
        }
    }

    private void doRumble(ServerLevel level, SimulationSession session) {
        double mag = session.getSessionMagnitude();
        int count  = Math.max(1, (int) Math.ceil(Config.QUAKE_BREAK_COUNT.get() * mag / 5.0));
        int radius = Math.max(5, (int) (mag * 3));
        BlockPos epicenter = session.getEpicenter();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();
        for (int i = 0; i < count; i++) {
            int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
            int dy = level.getRandom().nextInt(areaHeight);
            int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
            breakOrDebris(level, epicenter.offset(dx, dy, dz));
        }
    }

    // Scans for unsupported blocks near the epicenter and enqueues them closest-first
    // for cascading destruction by drainEarthquakePending.
    private void enqueuePeakDestructions(ServerLevel level, SimulationSession session) {
        var queue = session.getPendingDestructions();
        int batchMax = Config.QUAKE_BREAK_COUNT.get() * 3;
        if (queue.size() >= batchMax) return; // queue already full, let the drain catch up

        BlockPos epicenter = session.getEpicenter();
        int radius     = (int) Math.min(Config.SIM_AREA_SIZE.get() / 2.0, session.getSessionMagnitude() * 4);
        int areaHeight = Config.SIM_AREA_HEIGHT.get();

        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < areaHeight; dy++) {
                    BlockPos pos = epicenter.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir()
                            && level.getBlockState(pos).getBlock() != Blocks.BEDROCK
                            && level.getBlockState(pos.below()).isAir()) { // unsupported = nothing below
                        candidates.add(pos);
                    }
                }
            }
        }
        // Sort closest to epicenter first so the cascade radiates outward.
        candidates.sort(Comparator.comparingDouble(p -> epicenter.distSqr(p)));
        int toAdd = Math.min(batchMax - queue.size(), candidates.size());
        for (int i = 0; i < toAdd; i++) {
            queue.add(candidates.get(i));
        }
    }

    private void doAftershock(ServerLevel level, SimulationSession session) {
        double mag = session.getSessionMagnitude() * session.getAftershockMagnitudeScale();
        int count  = Math.max(1, (int) Math.ceil(Config.QUAKE_BREAK_COUNT.get() * mag / 5.0));
        int radius = Math.max(3, (int) (mag * 2.5));
        BlockPos epicenter = session.getEpicenter();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();
        for (int i = 0; i < count; i++) {
            int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
            int dy = level.getRandom().nextInt(areaHeight);
            int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
            breakOrDebris(level, epicenter.offset(dx, dy, dz));
        }
    }

    /**
     * Removes all fire blocks within (and just outside) the simulation arena.
     * Called each tick during EARTHQUAKE sessions to suppress vanilla fire spreading
     * that can occur when blocks are broken near campfires or other ignition sources.
     */
    public void clearFireInArena(ServerLevel level) {
        int size   = Config.SIM_AREA_SIZE.get();
        int height = Config.SIM_AREA_HEIGHT.get();
        int margin = 3;
        for (int dx = -margin; dx < size + margin; dx++) {
            for (int dy = -margin; dy < height + margin; dy++) {
                for (int dz = -margin; dz < size + margin; dz++) {
                    BlockPos pos = SimulationManager.SIM_POS.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
