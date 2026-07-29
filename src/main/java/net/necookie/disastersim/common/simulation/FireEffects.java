package net.necookie.disastersim.common.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Stateless fire-scenario world-mutation helpers, split out of {@code SimulationEffects} (see
 * {@link EarthquakeEffects} for the earthquake half). All methods run on the server thread, driven
 * from {@link SimulationManager}'s tick loop.
 *
 * <p>{@link #simulateFire} seeds random fire blocks inside the arena (library);
 * {@link #spreadComputerFire} ignites cached {@link ComputerBlock} positions (CCS electrical fire);
 * {@link #applyFireProximityEffects} applies nausea + air-drain near flames;
 * {@link #cleanupFireOutsideBounds} / {@link #clearFireInArena} keep vanilla fire-spread contained.
 */
public class FireEffects {

    /** How far (blocks, horizontal/vertical) a new fire block can land from the source it's spreading from. */
    private static final int FIRE_SPREAD_RADIUS = 4;

    /**
     * Scatters new fire blocks near an actually-burning source — never at an arbitrary position
     * anywhere in the arena. Each call picks a random member of {@link SimulationSession#getActiveFireSources()}
     * (pruned first of anything that's burned itself out since it was recorded) and offsets within
     * {@link #FIRE_SPREAD_RADIUS} of it; a newly-placed block joins the source pool itself, so fire
     * grows outward in a real chain instead of teleporting around the building. If nothing is
     * burning yet, this is a no-op — every fire-type scenario is responsible for igniting an actual
     * hazard first (see {@code SimulationManager}'s Library-FIRE bootstrap ignition and CCS's
     * `igniteRandomComputers`), so there's always an identifiable "hazard that caught fire."
     */
    public void simulateFire(ServerLevel level, SimulationSession session) {
        if (session == null) return;

        Set<BlockPos> sourceSet = session.getActiveFireSources();
        sourceSet.removeIf(pos -> !isActuallyOnFire(level, pos));
        if (sourceSet.isEmpty()) return;
        List<BlockPos> sources = new ArrayList<>(sourceSet);

        int count      = Config.FIRE_SPAWN_COUNT.get();
        BlockPos base  = session.getArenaOrigin();
        int spanX      = session.getArenaSpanX();
        int spanZ      = session.getArenaSpanZ();
        int areaHeight = session.getArenaHeight();

        for (int i = 0; i < count; i++) {
            BlockPos source = sources.get(level.getRandom().nextInt(sources.size()));
            BlockPos firePos = source.offset(
                    level.getRandom().nextInt(FIRE_SPREAD_RADIUS * 2 + 1) - FIRE_SPREAD_RADIUS,
                    level.getRandom().nextInt(3) - 1,
                    level.getRandom().nextInt(FIRE_SPREAD_RADIUS * 2 + 1) - FIRE_SPREAD_RADIUS);

            // Stay inside the arena the source itself lives in, even if it's near an edge.
            if (firePos.getX() < base.getX() || firePos.getX() >= base.getX() + spanX
                    || firePos.getZ() < base.getZ() || firePos.getZ() >= base.getZ() + spanZ
                    || firePos.getY() < base.getY() || firePos.getY() >= base.getY() + areaHeight) {
                continue;
            }

            if (level.getBlockState(firePos).isAir()
                    && !level.getBlockState(firePos.below()).isAir()
                    && !isFrameNear(level, firePos)) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                BlockPos immutableFirePos = firePos.immutable();
                session.incrementFireSpread();
                session.addFireSource(immutableFirePos);
                session.logger.log("FIRE_SPREAD", java.util.Map.of(
                    "x", firePos.getX(), "y", firePos.getY(), "z", firePos.getZ(),
                    "total_count", session.getFireSpreadCount()
                ));
                session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                        session.elapsedSeconds(), firePos.getX(), firePos.getY(), firePos.getZ(), "ignite"));
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

    /**
     * True if an item frame (or glow item frame) occupies {@code pos} — see the matching helpers
     * and full explanation on {@code HazardBlock.isFrameNear}/{@code ComputerBlock.isFrameNear}.
     * An item frame's own floating cell still reports {@code BlockState.isAir() == true}, so
     * without this check New Sim Building 2.0's ~24 wall-mounted extinguisher frames (scattered
     * through the same arena volume this scatters fire near) were fair game for a new fire block.
     */
    private static boolean isFrameNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(ItemFrame.class, new AABB(pos)).isEmpty();
    }

    /** True if {@code pos} is a real fire source right now: literal fire/soul fire, a burning hazard prop, or a burning computer. */
    private static boolean isActuallyOnFire(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return true;
        if (state.hasProperty(net.necookie.disastersim.common.hazard.HazardBlock.ON_FIRE)
                && state.getValue(net.necookie.disastersim.common.hazard.HazardBlock.ON_FIRE)) return true;
        if (state.getBlock() instanceof ComputerBlock && state.getValue(ComputerBlock.BURNING)) return true;
        return false;
    }

    /**
     * CCS fire variant: spreads electrical fire to one additional un-burned computer per interval.
     * Uses positions cached in the session so there's no per-tick arena scan.
     */
    public void spreadComputerFire(ServerLevel level, SimulationSession session) {
        List<BlockPos> computers = session.getComputerPositions();
        if (computers.isEmpty()) return;

        int toSpread = Config.FIRE_SPAWN_COUNT.get();
        // Build candidate list (non-burning, non-broken computers)
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : computers) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ComputerBlock
                    && !state.getValue(ComputerBlock.BURNING)
                    && !state.getValue(ComputerBlock.BROKEN)) {
                candidates.add(pos);
            }
        }
        if (candidates.isEmpty()) return;

        for (int i = 0; i < toSpread && i < candidates.size(); i++) {
            int idx = level.getRandom().nextInt(candidates.size() - i);
            BlockPos pos = candidates.get(idx);
            candidates.set(idx, candidates.get(candidates.size() - 1 - i));
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(ComputerBlock.BURNING, true), 3);
            if (session != null) {
                session.incrementFireSpread();
                session.logger.log("FIRE_SPREAD", java.util.Map.of(
                    "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                    "total_count", session.getFireSpreadCount(),
                    "type", "computer_ignition"
                ));
                session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                        session.elapsedSeconds(), pos.getX(), pos.getY(), pos.getZ(), "ignite"));
            }
            // Electrical ignition sound + spark burst
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.5f, 0.7f);
            for (int j = 0; j < 8; j++) {
                double ex = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.9;
                double ey = pos.getY() + 0.8 + level.getRandom().nextDouble() * 0.6;
                double ez = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.9;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ex, ey, ez, 1, 0.1, 0.1, 0.1, 0.05);
            }
            // Dense smoke plume above each newly ignited computer
            for (int s = 0; s < 14; s++) {
                double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.2;
                double py = pos.getY() + 1.0 + level.getRandom().nextDouble() * 3.0;
                double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.2;
                level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.0, 0.15, 0.0, 0.03);
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
