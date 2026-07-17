package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for hazard prop blocks that carry a horizontal {@code FACING} alongside {@code hazardous}
 * and {@code on_fire}. Replicates the FACING boilerplate from
 * {@link net.necookie.disastersim.block.HorizontalFacingBlock} while adding the shared hazard
 * state machine, animateTick particle hook, and bare-hand "prevention" interaction.
 */
public abstract class HazardFacingBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAZARDOUS = HazardBlock.HAZARDOUS;
    public static final BooleanProperty ON_FIRE = HazardBlock.ON_FIRE;

    protected HazardFacingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAZARDOUS, false)
                .setValue(ON_FIRE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAZARDOUS, ON_FIRE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    /** Collision/outline shape for the given facing direction. */
    protected abstract VoxelShape shapeFor(Direction facing);

    /** Convenience helper: maps a facing to one of the four directional shapes. */
    protected static VoxelShape byFacing(Direction facing, VoxelShape north, VoxelShape south,
                                          VoxelShape east, VoxelShape west) {
        return switch (facing) {
            case SOUTH -> south;
            case EAST  -> east;
            case WEST  -> west;
            default    -> north;
        };
    }

    /** Emit client-side particles that signal the hazardous state. Called every animateTick. */
    protected abstract void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(HAZARDOUS)) return;
        spawnHazardParticles(level, pos, state, rand);
        if (state.getValue(ON_FIRE)) {
            spawnHazardParticles(level, pos, state, rand);
            if (rand.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    /**
     * Bare-hand "prevention" action: fixing a hazard while it's merely hazardous (unplug the
     * frayed cord, clear the vent, etc.) before it becomes a real fire. Once {@code on_fire} is
     * true it's too late for a bare-handed fix — an extinguisher (or evacuation) is required.
     */
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(HAZARDOUS) && !state.getValue(ON_FIRE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAZARDOUS, false), 3);
                level.levelEvent(null, 1009, pos, 0);
                player.sendSystemMessage(Component.literal(preventMessage()));
                if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                    HazardManager.onManualPrevention(serverLevel, serverPlayer, pos, state);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Chat flavor text shown when a player prevents this hazard via bare-hand right-click. */
    public String preventMessage() {
        return "§a✔ Prevented! You fixed it before it could catch fire.";
    }

    /** Ticks a hazardous prop stays active before its failure consequence fires. Override to tune pacing. */
    public int failureDelayTicks() {
        return 300;
    }

    /** Chat flavor text broadcast to the player when this prop's failure consequence triggers. */
    public String failureMessage() {
        return "§c⚠ A neglected hazard just started a fire!";
    }

    /** World mutation performed when this prop is left hazardous for {@link #failureDelayTicks()}. */
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }

    /**
     * Lights up to {@code maxBlocks} adjacent air blocks on fire, never a block a player is
     * standing in/near. Returns the positions actually lit, matching {@link HazardBlock#igniteAdjacent}
     * exactly (kept in sync since the 2026-07-14 pass that added this return value to that sibling
     * method — every current {@code onHazardFailure} override across both hazard base classes
     * discards it as a bare statement, so this was previously left {@code void} with no live
     * functional impact, but a facing-type hazard prop written to mirror {@code NewSim2FireTicker}'s
     * pattern of iterating the result would have hit a confusing compile error).
     */
    public static List<BlockPos> igniteAdjacent(Level level, BlockPos pos, int maxBlocks) {
        List<BlockPos> lit = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (lit.size() >= maxBlocks) break;
            BlockPos target = pos.relative(dir);
            if (level.getBlockState(target).isAir() && !HazardBlock.isPlayerNear(level, target)) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit.add(target.immutable());
            }
        }
        return lit;
    }

    /**
     * Lights up to {@code maxBlocks} air blocks within a horizontal {@code radius} — for explosive
     * failures. Returns the positions actually lit, same convention as {@link #igniteAdjacent}.
     */
    public static List<BlockPos> igniteRadius(Level level, BlockPos pos, int radius, int maxBlocks) {
        List<BlockPos> lit = new ArrayList<>();
        for (BlockPos target : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            if (lit.size() >= maxBlocks) break;
            if (level.getBlockState(target).isAir() && !HazardBlock.isPlayerNear(level, target)) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit.add(target.immutable());
            }
        }
        return lit;
    }
}
