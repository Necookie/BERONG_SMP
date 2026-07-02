package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Flat wall-mounted classroom whiteboard; FACING-only, plus {@link #CONNECTED_UP}/
 * {@link #CONNECTED_DOWN} so stacking boards vertically grows one continuous surface instead of
 * leaving a gap between tiles. Horizontal tiling (placing boards side by side) is already flush —
 * the board geometry spans the block's full width — so no left/right connection state is needed.
 */
public class WhiteboardBlock extends HorizontalFacingBlock {

    public static final BooleanProperty CONNECTED_UP   = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");

    // Board body (Z=11-16) + marker tray (Z=11) when FACING=NORTH
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 2, 11, 16, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 2, 0,  16, 14, 5);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 0,  5,  14, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(11, 2, 0, 16, 14, 16);

    public WhiteboardBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState()
                .setValue(CONNECTED_UP, false)
                .setValue(CONNECTED_DOWN, false));
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTED_UP, CONNECTED_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = super.getStateForPlacement(ctx);
        BlockPos pos = ctx.getClickedPos();
        return base
                .setValue(CONNECTED_UP, connectsTo(ctx.getLevel(), pos.above()))
                .setValue(CONNECTED_DOWN, connectsTo(ctx.getLevel(), pos.below()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction dir, BlockPos neighbourPos, BlockState neighbourState,
            RandomSource random) {
        if (dir == Direction.UP) {
            return state.setValue(CONNECTED_UP, connectsTo(neighbourState));
        }
        if (dir == Direction.DOWN) {
            return state.setValue(CONNECTED_DOWN, connectsTo(neighbourState));
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
    }

    private static boolean connectsTo(BlockGetter level, BlockPos pos) {
        return connectsTo(level.getBlockState(pos));
    }

    private static boolean connectsTo(BlockState neighbourState) {
        return neighbourState.getBlock() instanceof WhiteboardBlock;
    }
}
