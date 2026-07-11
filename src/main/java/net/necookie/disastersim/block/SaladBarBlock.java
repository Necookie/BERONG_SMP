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
 * Refrigerated salad bar — steel body, chilled produce wells, and a glass sneeze guard;
 * FACING-only. Same {@link #CONNECTED_UP}/{@link #CONNECTED_DOWN} stacking idiom as
 * {@link ServingCounterBlock}/{@link WhiteboardBlock}: stacking a second salad bar on top turns
 * the short standalone guard into one continuous full-height guard.
 */
public class SaladBarBlock extends HorizontalFacingBlock {

    public static final BooleanProperty CONNECTED_UP   = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public SaladBarBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState()
                .setValue(CONNECTED_UP, false)
                .setValue(CONNECTED_DOWN, false));
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
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
        return neighbourState.getBlock() instanceof SaladBarBlock;
    }
}
