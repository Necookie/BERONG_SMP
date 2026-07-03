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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Extendable study/library table — a single block, no FACING (symmetric, like
 * {@link TrashCanBlock}). Tabletop slab plus four corner-post legs, matching a real table's
 * silhouette rather than {@link ComputerTableBlock}'s solid uniform-box desk.
 *
 * <p>{@link #NORTH}/{@link #SOUTH}/{@link #EAST}/{@link #WEST} (the same idiom as vanilla fences
 * and glass panes) track same-type neighbours so lining tables up merges them into one continuous
 * tabletop — the blockstate hides the corner apron model on any connected side — instead of
 * reading as separate tables placed side by side.
 *
 * <p>A real table is under a metre tall, so a single block of it can never satisfy
 * {@code DuckCoverHoldManager}'s "solid block above the player's feet" test on its own cell —
 * {@code DuckCoverHoldManager} separately checks for a {@code TableBlock} within a 1-block radius
 * while crouching, treating sheltering next to/under one as valid duck-and-cover.
 */
public class TableBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    // Tabletop (Y12-15) + four corner-post legs (Y0-12) — an open kneehole between the legs,
    // unlike ComputerTableBlock's uniform box(0,0,0,16,14,16).
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 12, 0, 16, 15, 16),
            Block.box(1, 0, 1, 3, 12, 3),
            Block.box(13, 0, 1, 15, 12, 3),
            Block.box(1, 0, 13, 3, 12, 15),
            Block.box(13, 0, 13, 15, 12, 15));

    public TableBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, connects(level, pos.north()))
                .setValue(SOUTH, connects(level, pos.south()))
                .setValue(EAST, connects(level, pos.east()))
                .setValue(WEST, connects(level, pos.west()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction dir, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BooleanProperty prop = connectionProperty(dir);
        if (prop == null) {
            return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
        }
        return state.setValue(prop, neighbourState.is(this));
    }

    private boolean connects(BlockGetter level, BlockPos neighbourPos) {
        return level.getBlockState(neighbourPos).is(this);
    }

    private static @Nullable BooleanProperty connectionProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> null;
        };
    }
}
