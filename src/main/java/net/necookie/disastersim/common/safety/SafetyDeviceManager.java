package net.necookie.disastersim.common.safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.block.EmergencyLightBlock;
import net.necookie.disastersim.block.SmokeDetectorBlock;
import net.necookie.disastersim.block.SprinklerHeadBlock;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.common.zones.ExitZones;
import net.necookie.disastersim.academy.AcademyVisuals;

/**
 * Drives the three passive safety-device blocks during simulation sessions, on a 40-tick cycle:
 *
 * <ul>
 *   <li><b>Smoke detectors</b> (FIRE sessions): {@code ALARMING=true} + beep while fire is within
 *       {@value #DETECT_RANGE} blocks (via the memoised
 *       {@link SimulationManager#nearestFireDistance}); one {@code smoke_detector_triggered}
 *       telemetry event per detector per session.</li>
 *   <li><b>Sprinkler heads</b> (FIRE sessions): water-particle curtain + extinguish at most ONE
 *       fire block below per cycle - buys evacuation time, deliberately can't win alone.</li>
 *   <li><b>Emergency lights</b> (any session): {@code LIT=true} while a session runs in their
 *       arena, reset when it ends.</li>
 * </ul>
 *
 * <p>Device positions are scanned once per session (lazy, on the first cycle that sees the
 * session) and cached - mirroring {@code SimulationSession.hazardPositions}'s scan-once idiom.
 * All state is dropped and devices reset when the session disappears.
 *
 * <p>Also owns the evacuation-map assist ({@link #pointToNearestExit}): nearest-exit chat line +
 * a 30s Academy compass target.
 *
 * <p>Registered with {@link TickScheduler} from the static block; {@link #bootstrap()} is called
 * from {@code BerongSMP.commonSetup} to force the class load (the TickScheduler class-loading
 * rule - see DuckCoverHoldManager for the precedent).
 */
public final class SafetyDeviceManager {

    static {
        TickScheduler.register(SafetyDeviceManager::tick);
    }

    /** No-op; forces the class load that runs the static registration block above. */
    public static void bootstrap() {}

    private static final int CYCLE_TICKS = 40;
    private static final double DETECT_RANGE = 8.0;
    private static final int SPRINKLER_RADIUS = 3;
    private static final int SPRINKLER_DEPTH = 6;
    private static final int COMPASS_TICKS = 600;

    private record Devices(List<BlockPos> detectors, List<BlockPos> sprinklers, List<BlockPos> lights) {}

    private static final Map<UUID, Devices> deviceCache = new HashMap<>();
    private static final Set<String> detectorsTriggered = new HashSet<>();
    private static final Map<UUID, Long> compassClearAt = new HashMap<>();

    private SafetyDeviceManager() {}

    private static void tick(ServerLevel level) {
        long time = level.getGameTime();

        // Expire evacuation-map compass targets (cheap, every tick).
        if (!compassClearAt.isEmpty()) {
            compassClearAt.entrySet().removeIf(e -> {
                if (time < e.getValue()) return false;
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (sp != null) AcademyVisuals.setCompassTarget(sp, null);
                return true;
            });
        }

        if (time % CYCLE_TICKS != 0) return;

        Map<UUID, SimulationSession> sessions = SimulationManager.getActiveSessions();

        // Sessions that ended since the last cycle: reset their devices, drop their cache.
        deviceCache.entrySet().removeIf(e -> {
            if (sessions.containsKey(e.getKey())) return false;
            resetDevices(level, e.getValue());
            detectorsTriggered.removeIf(k -> k.startsWith(e.getKey().toString()));
            return true;
        });

        for (Map.Entry<UUID, SimulationSession> entry : sessions.entrySet()) {
            SimulationSession session = entry.getValue();
            Devices devices = deviceCache.computeIfAbsent(entry.getKey(), k -> scan(level, session));

            for (BlockPos pos : devices.lights()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof EmergencyLightBlock && !state.getValue(BlockStateProperties.LIT)) {
                    level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
                }
            }

            if (!session.getState().isFire()) continue;

            for (BlockPos pos : devices.detectors()) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof SmokeDetectorBlock)) continue;
                boolean fireNear = SimulationManager.nearestFireDistance(level, pos) <= DETECT_RANGE;
                if (fireNear != state.getValue(SmokeDetectorBlock.ALARMING)) {
                    level.setBlock(pos, state.setValue(SmokeDetectorBlock.ALARMING, fireNear), 3);
                }
                if (fireNear) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(),
                            SoundSource.BLOCKS, 1.5f, 2.0f);
                    if (detectorsTriggered.add(entry.getKey() + "|" + pos.asLong())) {
                        session.logger.log("smoke_detector_triggered", Map.of(
                                "x", pos.getX(), "y", pos.getY(), "z", pos.getZ()));
                    }
                }
            }

            for (BlockPos pos : devices.sprinklers()) {
                if (!(level.getBlockState(pos).getBlock() instanceof SprinklerHeadBlock)) continue;
                if (SimulationManager.nearestFireDistance(level, pos) > DETECT_RANGE) continue;
                level.sendParticles(ParticleTypes.FALLING_WATER,
                        pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                        24, 1.2, 0.2, 1.2, 0.0);
                extinguishOneBelow(level, pos);
            }
        }
    }

    private static Devices scan(ServerLevel level, SimulationSession session) {
        List<BlockPos> detectors = new ArrayList<>();
        List<BlockPos> sprinklers = new ArrayList<>();
        List<BlockPos> lights = new ArrayList<>();
        BlockPos origin = session.getArenaOrigin();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-2, -1, -2),
                origin.offset(session.getArenaSpanX() * 2 + 2, session.getArenaHeight() + 2, session.getArenaSpanZ() * 2 + 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SmokeDetectorBlock) detectors.add(pos.immutable());
            else if (state.getBlock() instanceof SprinklerHeadBlock) sprinklers.add(pos.immutable());
            else if (state.getBlock() instanceof EmergencyLightBlock) lights.add(pos.immutable());
        }
        return new Devices(detectors, sprinklers, lights);
    }

    private static void extinguishOneBelow(ServerLevel level, BlockPos sprinkler) {
        for (BlockPos pos : BlockPos.betweenClosed(
                sprinkler.offset(-SPRINKLER_RADIUS, -SPRINKLER_DEPTH, -SPRINKLER_RADIUS),
                sprinkler.offset(SPRINKLER_RADIUS, -1, SPRINKLER_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.destroyBlock(pos, false);
                level.levelEvent(null, 1009, pos, 0);
                return;
            }
        }
    }

    private static void resetDevices(ServerLevel level, Devices devices) {
        for (BlockPos pos : devices.lights()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof EmergencyLightBlock && state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            }
        }
        for (BlockPos pos : devices.detectors()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SmokeDetectorBlock && state.getValue(SmokeDetectorBlock.ALARMING)) {
                level.setBlock(pos, state.setValue(SmokeDetectorBlock.ALARMING, false), 3);
            }
        }
    }

    /** Evacuation-map assist: nearest exit's name + distance in chat, compass needle for 30s. */
    public static void pointToNearestExit(ServerPlayer sp) {
        ExitZones.ExitZone nearest = null;
        double best = Double.MAX_VALUE;
        for (List<ExitZones.ExitZone> zones : List.of(ExitZones.ZONES, ExitZones.CCS_ZONES, ExitZones.NEW_SIM2_ZONES)) {
            for (ExitZones.ExitZone zone : zones) {
                double d = zone.bounds().getCenter().distanceTo(sp.position());
                if (d < best) {
                    best = d;
                    nearest = zone;
                }
            }
        }
        if (nearest == null) return;
        Vec3 center = nearest.bounds().getCenter();
        sp.sendSystemMessage(Component.literal(String.format(
                "§a➡ Nearest exit: §e%s§a, %.0f blocks away. Follow the compass needle!",
                nearest.label(), best)));
        AcademyVisuals.setCompassTarget(sp, center);
        compassClearAt.put(sp.getUUID(), sp.level().getGameTime() + COMPASS_TICKS);
    }
}
