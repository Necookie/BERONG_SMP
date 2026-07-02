package net.necookie.disastersim.world;

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
import net.necookie.disastersim.block.hazard.HazardBlock;
import net.necookie.disastersim.block.hazard.HazardFacingBlock;
import net.necookie.disastersim.block.hazard.WoodshopSawdustLayerBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Drives the normal→hazardous→failure state machine for the 20 hazard prop blocks (plus the
 * sawdust accumulation layer) placed inside an active FIRE-type arena, per {@code docs/md files/Items.md}.
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

    /** Called every tick a FIRE-type session runs. */
    public static void tick(ServerLevel level, SimulationSession session, int ticks) {
        if (session.getHazardPositions().isEmpty()) return;
        if (ticks % SCAN_INTERVAL_TICKS == 0) developHazards(level, session);
        seedComputerTimers(level, session);
        advanceFailureTimers(level, session);
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
            if (level.getBlockState(target).isAir()) {
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
        triggerFailure(level, session, pos, state, notifyPlayer);
        if (session != null) session.getHazardTimers().remove(pos);
        return true;
    }

    private static void triggerFailure(ServerLevel level, SimulationSession session, BlockPos pos,
                                        BlockState state, ServerPlayer notifyPlayer) {
        Block block = state.getBlock();
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

    /** Defuses a hazardous prop at {@code pos} (sets hazardous=false, clears its failure timer). */
    public static boolean defuse(ServerLevel level, SimulationSession session, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isHazardous(state)) return false;
        level.setBlock(pos, state.setValue(HazardBlock.HAZARDOUS, false), 3);
        level.levelEvent(null, 1009, pos, 0);
        if (session != null) session.getHazardTimers().remove(pos);
        return true;
    }
}
