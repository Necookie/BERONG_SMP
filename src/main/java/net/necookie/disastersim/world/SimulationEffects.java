package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SimulationEffects {

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

    public void simulateFire(ServerLevel level, SimulationSession session) {
        int count      = Config.FIRE_SPAWN_COUNT.get();
        BlockPos base  = session.getArenaOrigin();
        int spanX      = session.getArenaSpanX();
        int spanZ      = session.getArenaSpanZ();
        int areaHeight = session.getArenaHeight();

        for (int i = 0; i < count; i++) {
            BlockPos firePos = base.offset(
                    level.getRandom().nextInt(spanX),
                    level.getRandom().nextInt(areaHeight),
                    level.getRandom().nextInt(spanZ));

            if (level.getBlockState(firePos).isAir()
                    && !level.getBlockState(firePos.below()).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                if (session != null) {
                    session.incrementFireSpread();
                    session.logger.log("FIRE_SPREAD", java.util.Map.of(
                        "x", firePos.getX(), "y", firePos.getY(), "z", firePos.getZ(),
                        "total_count", session.getFireSpreadCount()
                    ));
                }
                // Dense smoke plume above each new fire block
                for (int s = 0; s < 14; s++) {
                    double px = firePos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.2;
                    double py = firePos.getY() + 0.6 + level.getRandom().nextDouble() * 3.0;
                    double pz = firePos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.2;
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.0, 0.15, 0.0, 0.03);
                }
            }
        }
    }

    public void cleanupFireOutsideBounds(ServerLevel level, SimulationSession session) {
        BlockPos base  = session.getArenaOrigin();
        int sizeX  = session.getArenaSpanX();
        int sizeZ  = session.getArenaSpanZ();
        int height = session.getArenaHeight();
        int margin = 3;

        for (int dx = -margin; dx < sizeX + margin; dx++) {
            for (int dy = -margin; dy < height + margin; dy++) {
                for (int dz = -margin; dz < sizeZ + margin; dz++) {
                    if (dx >= 0 && dx < sizeX && dy >= 0 && dy < height && dz >= 0 && dz < sizeZ) {
                        continue;
                    }

                    BlockPos pos = base.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

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

    public void applyFireProximityEffects(ServerLevel level, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        int fireCount = 0;
        double closestDist = Double.MAX_VALUE;
        int radius = 7;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (level.getBlockState(playerPos.offset(dx, dy, dz)).getBlock() == Blocks.FIRE) {
                        fireCount++;
                        double d = Math.sqrt((double) (dx * dx + dy * dy + dz * dz));
                        if (d < closestDist) closestDist = d;
                    }
                }
            }
        }

        if (fireCount == 0) return;

        // Nausea from smoke; amplifier 0–2 based on fire density
        int nauseaAmp = Math.min(2, fireCount / 4);
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, nauseaAmp, false, true));

        // Oxygen depletion — drains air supply; vanilla suffocation damage triggers at zero
        int proximityBonus = closestDist <= 3.0 ? 30 : 0;
        int airDrain = Math.min(80, fireCount * 6 + proximityBonus);
        player.setAirSupply(Math.max(-20, player.getAirSupply() - airDrain));
    }

    public void clearFireInArena(ServerLevel level, SimulationSession session) {
        BlockPos base  = session.getArenaOrigin();
        int sizeX  = session.getArenaSpanX();
        int sizeZ  = session.getArenaSpanZ();
        int height = session.getArenaHeight();
        int margin = 3;
        for (int dx = -margin; dx < sizeX + margin; dx++) {
            for (int dy = -margin; dy < height + margin; dy++) {
                for (int dz = -margin; dz < sizeZ + margin; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
