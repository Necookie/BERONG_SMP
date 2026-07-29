package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Drives the normal→hazardous→failure state machine for the 20 hazard prop blocks (plus the
 * sawdust accumulation layer) placed inside an active FIRE-type arena, per {@code docs/hazard_props_spec.md}.
 *
 * <p>Positions are scanned once at session start ({@link #scanHazardProps}) and cached on the
 * session, mirroring {@code SimulationManager.findComputersInCCS}. Each subsequent tick only
 * touches that cached list — no per-tick arena rescans.
 */
public final class HazardManager {

    private static final int SCAN_INTERVAL_TICKS = 100;
    private static final int DEVELOP_CHANCE_DENOM = 30;
    private static final int SAWDUST_STEP_CHANCE_DENOM = 20;
    /** Shorter than the generic 300-tick default — electrical fires escalate fast. */
    private static final int COMPUTER_FAILURE_DELAY_TICKS = 240;

    private HazardManager() {}

    /** One-time scan of the arena bounds for hazard prop blocks; call at session start. */
    public static List<BlockPos> scanHazardProps(ServerLevel level, BlockPos base, int spanX, int spanZ, int height) {
        List<BlockPos> found = new ArrayList<>();
        for (int dx = 0; dx < spanX; dx++) {
            for (int dz = 0; dz < spanZ; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    Block block = level.getBlockState(pos).getBlock();
                    if (block instanceof HazardBlock || block instanceof HazardFacingBlock
                            || block instanceof WoodshopSawdustLayerBlock || block instanceof ComputerBlock) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        return found;
    }

    /**
     * Called every tick a FIRE-type session runs. New Sim Building 2.0 drives its own 5-hazard
     * prevention/intervention/evacuation state machine ({@code NewSim2FireTicker.tick})
     * instead of this class's organic random-escalation ({@link #developHazards}) — every other
     * hazard prop in that building must stay in its default state until deliberately armed, so
     * this returns before touching anything for that state. Library/CCS are unaffected.
     */
    public static void tick(ServerLevel level, SimulationSession session, int ticks) {
        if (session.getState() == SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE) return;
        if (session.getHazardPositions().isEmpty()) return;
        if (ticks % SCAN_INTERVAL_TICKS == 0) developHazards(level, session);
        seedComputerTimers(level, session);
        advanceFailureTimers(level, session);
    }

    /**
     * Filters {@code pool} down to props carrying the shared {@code HAZARDOUS} property (excludes
     * {@link ComputerBlock}/{@link WoodshopSawdustLayerBlock}, which use their own state instead
     * of the discrete armed-hazard model), shuffles, and {@link #activate}s up to {@code count} of
     * them. Returns the positions actually armed — used by New Sim Building 2.0 to pick 5 random
     * hazards from whatever the building's own hazard-prop scan finds, not a hardcoded pool.
     */
    public static List<BlockPos> armRandomHazards(ServerLevel level, SimulationSession session,
                                                   List<BlockPos> pool, int count, RandomSource random) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : pool) {
            if (isHazardCapable(level.getBlockState(pos))) candidates.add(pos);
        }
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        List<BlockPos> armed = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (armed.size() >= count) break;
            if (activate(level, session, pos)) armed.add(pos);
        }
        return armed;
    }

    /**
     * Called by {@link HazardBlock}/{@link HazardFacingBlock#useWithoutItem} right after a
     * bare-hand prevention interaction succeeds — the interaction itself has no session/telemetry
     * awareness, so this is the hook that turns "player right-clicked a hazardous prop" into a
     * {@code hazard_neutralize} telemetry row and, for New Sim Building 2.0, a found-hazard tally.
     */
    public static void onManualPrevention(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState previousState) {
        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || !session.getState().isFire()) return;

        String hazardId = BuiltInRegistries.BLOCK.getKey(previousState.getBlock()).getPath();
        double t = session.elapsedSeconds();
        double tRounded = Math.round(t * 100.0) / 100.0;
        session.logger.log("hazard_neutralize", Map.of(
                "t", tRounded, "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                "hazard", hazardId, "method", "prevention"));
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(), session.getState().name().toLowerCase(),
                tRounded, "hazard_neutralize",
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0,
                hazardId, null, null, null, session.firePhaseLabel()));

        if (session.getState() == SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE
                && session.isArmedHazard(pos)) {
            session.recordPrevented(pos);
            player.sendSystemMessage(Component.literal(String.format(
                    "§a✔ Hazard prevented! (%d/%d found)",
                    session.getPreventedHazards().size(), session.getArmedHazards().size())));
        }
    }

    /**
     * ComputerBlock never goes through {@link #activate}, since it uses its own BURNING property
     * instead of the shared HAZARDOUS flag and has three independent ignition triggers elsewhere
     * (flint & steel, session-start, periodic CCS spread). This lazily starts a failure timer the
     * first tick any of those is observed to have set BURNING=true, so a computer left burning
     * unattended still escalates like every other hazard prop.
     */
    private static void seedComputerTimers(ServerLevel level, SimulationSession session) {
        for (BlockPos pos : session.getHazardPositions()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ComputerBlock && state.getValue(ComputerBlock.BURNING)) {
                session.getHazardTimers().putIfAbsent(pos, 0);
            }
        }
    }

    private static void developHazards(ServerLevel level, SimulationSession session) {
        RandomSource random = level.getRandom();
        for (BlockPos pos : session.getHazardPositions()) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof WoodshopSawdustLayerBlock) {
                stepSawdust(level, session, pos, state, random);
                continue;
            }

            if (isHazardCapable(state) && !isHazardous(state) && random.nextInt(DEVELOP_CHANCE_DENOM) == 0) {
                activate(level, session, pos);
            }
        }
    }

    private static void stepSawdust(ServerLevel level, SimulationSession session, BlockPos pos,
                                     BlockState state, RandomSource random) {
        int amount = state.getValue(WoodshopSawdustLayerBlock.ACCUMULATION);
        if (amount >= 5) return;
        if (random.nextInt(SAWDUST_STEP_CHANCE_DENOM) != 0) return;
        setSawdustLevel(level, session, pos, amount + 1, session.getPlayer());
    }

    /** Sets the sawdust layer's accumulation directly (0–5); flash-ignites if it lands on 5. Returns true if it flashed. */
    public static boolean setSawdustLevel(ServerLevel level, SimulationSession session, BlockPos pos,
                                          int amount, ServerPlayer notifyPlayer) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WoodshopSawdustLayerBlock)) return false;
        int clamped = Math.max(0, Math.min(5, amount));
        level.setBlockAndUpdate(pos, state.setValue(WoodshopSawdustLayerBlock.ACCUMULATION, clamped));
        if (clamped == 5) {
            flashIgniteSawdust(level, session, pos, notifyPlayer);
            return true;
        }
        return false;
    }

    private static void flashIgniteSawdust(ServerLevel level, SimulationSession session, BlockPos pos, ServerPlayer notifyPlayer) {
        int lit = 0;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))) {
            if (lit >= 6) break;
            // isAir() reports true for an item frame's own floating cell too — see HazardBlock's
            // isFrameNear javadoc. Without this guard the 3x3 flash-ignite could place fire right
            // on top of a nearby wall-mounted extinguisher frame, popping/breaking it.
            if (level.getBlockState(target).isAir() && !HazardBlock.isFrameNear(level, target)) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit++;
            }
        }
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(WoodshopSawdustLayerBlock.ACCUMULATION, 0));
        level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0f, 0.6f);
        if (session != null) session.getHazardTimers().remove(pos);
        notifyFailure(notifyPlayer, "§c🪚 Thick sawdust flash-ignites across the shop floor!");
    }

    /** Sets a hazard-capable prop to hazardous=true and starts its failure timer. Returns false if not applicable. */
    public static boolean activate(ServerLevel level, SimulationSession session, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isHazardCapable(state) || isHazardous(state)) return false;
        level.setBlockAndUpdate(pos, state.setValue(HazardBlock.HAZARDOUS, true));
        if (session != null) {
            session.getHazardTimers().put(pos, 0);
            session.logger.log("hazard_activated", Map.of(
                    "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                    "hazard", BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()));
        }
        return true;
    }

    private static void advanceFailureTimers(ServerLevel level, SimulationSession session) {
        Iterator<Map.Entry<BlockPos, Integer>> it = session.getHazardTimers().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (!isStillActive(state)) {
                it.remove(); // defused by an extinguisher, or block gone
                continue;
            }
            int elapsed = entry.getValue() + 1;
            int delay = state.getBlock() instanceof ComputerBlock ? COMPUTER_FAILURE_DELAY_TICKS
                    : state.getBlock() instanceof HazardBlock hb ? hb.failureDelayTicks()
                    : state.getBlock() instanceof HazardFacingBlock hfb ? hfb.failureDelayTicks()
                    : 300;
            if (elapsed >= delay) {
                triggerFailure(level, session, pos, state, session.getPlayer());
                it.remove();
            } else {
                entry.setValue(elapsed);
            }
        }
    }

    /**
     * Forces a hazard prop straight to its failure consequence, regardless of its current state
     * or timer — used by the hazard wand for instant testing. Activates it first if it was still
     * in its normal state. Returns false if {@code pos} isn't a hazard prop.
     */
    public static boolean forceFailure(ServerLevel level, SimulationSession session, BlockPos pos, ServerPlayer notifyPlayer) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WoodshopSawdustLayerBlock) {
            setSawdustLevel(level, session, pos, 5, notifyPlayer);
            return true;
        }
        if (state.getBlock() instanceof ComputerBlock) {
            if (!state.getValue(ComputerBlock.BURNING)) {
                level.setBlock(pos, state.setValue(ComputerBlock.BURNING, true).setValue(ComputerBlock.LIT, true), 3);
                state = level.getBlockState(pos);
            }
            triggerFailure(level, session, pos, state, notifyPlayer);
            if (session != null) session.getHazardTimers().remove(pos);
            return true;
        }
        if (!isHazardCapable(state)) return false;
        if (!isHazardous(state)) {
            // Skipping straight to failure from the normal state (as Reyes's scripted hazard
            // demos always do — they place a prop at its default state and force-fail it
            // immediately, never going through activate() first) used to leave HAZARDOUS=false
            // forever: triggerFailure only ever sets ON_FIRE, so isHazardous()/defuse() never
            // recognized the prop as hazardous and an extinguisher spray silently did nothing.
            level.setBlock(pos, state.setValue(HazardBlock.HAZARDOUS, true), 3);
            state = level.getBlockState(pos);
        }
        triggerFailure(level, session, pos, state, notifyPlayer);
        if (session != null) session.getHazardTimers().remove(pos);
        return true;
    }

    private static void triggerFailure(ServerLevel level, SimulationSession session, BlockPos pos,
                                        BlockState state, ServerPlayer notifyPlayer) {
        Block block = state.getBlock();
        if (state.hasProperty(HazardBlock.ON_FIRE)) {
            state = state.setValue(HazardBlock.ON_FIRE, true);
            level.setBlock(pos, state, 3);
        }
        String message = "§c⚠ A neglected hazard just started a fire!";
        if (block instanceof ComputerBlock) {
            HazardBlock.igniteAdjacent(level, pos, 2);
            message = "§4⚠ The unattended electrical fire has spread to nearby equipment!";
        } else if (block instanceof HazardBlock hb) {
            hb.onHazardFailure(level, pos, state);
            message = hb.failureMessage();
        } else if (block instanceof HazardFacingBlock hfb) {
            hfb.onHazardFailure(level, pos, state);
            message = hfb.failureMessage();
        }
        if (session != null) {
            session.incrementFireSpread();
            session.logger.log("hazard_failure", Map.of(
                    "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                    "hazard", BuiltInRegistries.BLOCK.getKey(block).getPath()));
            session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                    session.elapsedSeconds(), pos.getX(), pos.getY(), pos.getZ(), "ignite"));
            // This hazard is now a real "the fire started here" anchor — FireEffects.simulateFire
            // only ever scatters new fire near an active source, never at an arbitrary arena position.
            session.addFireSource(pos.immutable());
        }
        notifyFailure(notifyPlayer, message);
    }

    private static void notifyFailure(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    /** True if the block state carries the shared {@code hazardous} property (HazardBlock or HazardFacingBlock). */
    private static boolean isHazardCapable(BlockState state) {
        return state.hasProperty(HazardBlock.HAZARDOUS);
    }

    /** True if {@code state} carries the hazardous flag and it's currently set. */
    public static boolean isHazardous(BlockState state) {
        return isHazardCapable(state) && state.getValue(HazardBlock.HAZARDOUS);
    }

    /** True if a failure timer for {@code state} should keep running — HAZARDOUS, or a burning ComputerBlock. */
    private static boolean isStillActive(BlockState state) {
        return isHazardous(state)
                || (state.getBlock() instanceof ComputerBlock && state.getValue(ComputerBlock.BURNING));
    }

    /** Defuses a hazardous (or already on-fire) prop at {@code pos} back to fully safe, clears its failure timer. */
    public static boolean defuse(ServerLevel level, SimulationSession session, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isHazardous(state)) return false;
        boolean wasOnFire = state.hasProperty(HazardBlock.ON_FIRE) && state.getValue(HazardBlock.ON_FIRE);
        String hazardId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        BlockState safe = state.setValue(HazardBlock.HAZARDOUS, false);
        if (safe.hasProperty(HazardBlock.ON_FIRE)) safe = safe.setValue(HazardBlock.ON_FIRE, false);
        level.setBlock(pos, safe, 3);
        level.levelEvent(null, 1009, pos, 0);
        if (session != null) {
            session.getHazardTimers().remove(pos);
            if (wasOnFire) {
                session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                        session.elapsedSeconds(), pos.getX(), pos.getY(), pos.getZ(), "extinguish"));
                session.removeFireSource(pos);
            }
            if (session.getState().isFire()) {
                double t = session.elapsedSeconds();
                double tRounded = Math.round(t * 100.0) / 100.0;
                ServerPlayer sessionPlayer = session.getPlayer();
                String playerId = sessionPlayer != null ? sessionPlayer.getUUID().toString() : "";
                session.logger.log("hazard_neutralize", Map.of(
                        "t", tRounded, "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                        "hazard", hazardId, "method", "extinguisher"));
                session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                        session.getSessionId(), playerId, session.getState().name().toLowerCase(),
                        tRounded, "hazard_neutralize",
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0,
                        hazardId, null, null, null, session.firePhaseLabel()));
            }
            if (session.getState() == SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE
                    && session.isEscalatedHazard(pos)
                    && !session.getExtinguishedEscalated().contains(pos)) {
                session.recordEscalatedExtinguished(pos);
                if (session.getPlayer() != null) {
                    session.getPlayer().sendSystemMessage(Component.literal(String.format(
                            "§a✔ Fire contained! (%d/%d resolved)",
                            session.getExtinguishedEscalated().size(), session.getEscalatedHazards().size())));
                }
            }
        }
        return true;
    }
}
