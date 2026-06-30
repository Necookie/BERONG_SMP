package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for the simple decorative furniture blocks that carry a single horizontal {@code FACING}
 * property. It centralises the placement/rotation/mirror boilerplate every such block was copying;
 * subclasses only supply their collision shape via {@link #shapeFor(Direction)} (and any extra
 * behaviour like an interaction sound).
 *
 * <p>Complex blocks with additional state (e.g. {@link ComputerBlock}, {@link FireAlarmBlock}) keep
 * their own definitions — this base is intentionally limited to FACING-only blocks.
 */
public abstract class HorizontalFacingBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;

    protected HorizontalFacingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    /** Collision/outline shape for the given facing. */
    protected abstract VoxelShape shapeFor(Direction facing);

    /** Convenience for the common "one shape per facing" case. */
    protected static VoxelShape byFacing(Direction facing, VoxelShape north, VoxelShape south,
                                         VoxelShape east, VoxelShape west) {
        return switch (facing) {
            case SOUTH -> south;
            case EAST  -> east;
            case WEST  -> west;
            default    -> north;
        };
    }
}
