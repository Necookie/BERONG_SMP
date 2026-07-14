package net.necookie.disastersim.common.simulation;

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
 * Stateless earthquake-scenario world-mutation helpers, split out of {@code SimulationEffects}
 * (see {@link FireEffects} for the fire half). All methods run on the server thread, driven from
 * {@link SimulationManager}'s tick loop.
 *
 * <p>Phase-dispatched from {@link #simulateEarthquake}: RUMBLE/AFTERSHOCK break random blocks in a
 * magnitude-scaled radius; PEAK scans for unsupported blocks and enqueues them closest-first into
 * {@link SimulationSession#getPendingDestructions()}, drained a few per tick by
 * {@link #drainEarthquakePending}. {@link #breakOrDebris} turns wood/glass into gravity-driven
 * {@link FallingBlockEntity} debris (with fall damage) and destroys everything else outright.
 *
 * <p><b>Performance note:</b> the scan-heavy methods here are the hot path during a simulation.
 * Block states are read once per position where possible, since these loops cover thousands of
 * positions every few ticks.
 */
public class EarthquakeEffects {

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

    public void simulateEarthquake(ServerLevel level, SimulationSession session) {
        if (session.getEpicenter() == null || session.getQuakePhase() == null) return;
        switch (session.getQuakePhase()) {
            case RUMBLE     -> doRumble(level, session);
            case PEAK       -> enqueuePeakDestructions(level, session);
            case AFTERSHOCK -> doAftershock(level, session);
            case END        -> {} // nothing to do
        }
    }

    public void drainEarthquakePending(ServerLevel level, SimulationSession session) {
        if (session.getQuakePhase() != SimulationSession.EarthquakePhase.PEAK) return;
        var queue = session.getPendingDestructions();
        int toBreak = Math.min(2, queue.size());
        for (int i = 0; i < toBreak; i++) {
            BlockPos pos = queue.poll();
            if (pos != null) breakOrDebris(level, pos);
        }
    }

    private void breakOrDebris(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) return;
        if (DEBRIS_BLOCKS.contains(state.getBlock())) {
            FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
            debris.setHurtsEntities(8.0f, 80); // realistic structural debris damage
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
                    // Cache the state once: this loop visits thousands of positions per
                    // PEAK interval, and re-reading the same block 3x was pure overhead.
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()
                            && state.getBlock() != Blocks.BEDROCK
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
}
