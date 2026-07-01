package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for hazard prop blocks that carry a horizontal {@code FACING} alongside {@code hazardous}.
 * Replicates the FACING boilerplate from {@link net.necookie.disastersim.block.HorizontalFacingBlock}
 * while adding the shared hazard state and animateTick particle hook.
 */
public abstract class HazardFacingBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAZARDOUS = HazardBlock.HAZARDOUS;

    protected HazardFacingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAZARDOUS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAZARDOUS);
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
        if (state.getValue(HAZARDOUS)) {
            spawnHazardParticles(level, pos, state, rand);
        }
    }
}
