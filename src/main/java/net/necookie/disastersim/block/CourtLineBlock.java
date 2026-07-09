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
 * A smooth white court-marking line — the same auto-connecting-wire idiom as vanilla redstone
 * dust (small hub "dot" when isolated, thin arms extending toward every neighbouring
 * {@code CourtLineBlock}), just plain white and with no power mechanic, for drawing badminton
 * court boundary/service lines at real-world line proportions instead of a full-width painted
 * tile. Reuses {@link TableBlock}'s {@link #NORTH}/{@link #SOUTH}/{@link #EAST}/{@link #WEST}
 * self-connection idiom (same properties, same {@code updateShape}/{@code getStateForPlacement}
 * pattern) and the {@code multipart} blockstate technique already used by {@code table.json}, with
 * the collision shape precomputed per connection-mask like {@link TableBlock#SHAPES_BY_CONNECTIONS}.
 */
public class CourtLineBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final VoxelShape DOT = Block.box(6, 0, 6, 10, 1, 10);
    private static final VoxelShape ARM_NORTH = Block.box(6, 0, 0, 10, 1, 6);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 0, 10, 10, 1, 16);
    private static final VoxelShape ARM_EAST = Block.box(10, 0, 6, 16, 1, 10);
    private static final VoxelShape ARM_WEST = Block.box(0, 0, 6, 6, 1, 10);

    // Indexed by (north?1:0)|(south?2:0)|(east?4:0)|(west?8:0) — same precomputation idiom as
    // TableBlock.SHAPES_BY_CONNECTIONS, additive here (dot + one arm per connected side) instead
    // of subtractive (leg removal).
    private static final VoxelShape[] SHAPES_BY_CONNECTIONS = buildShapes();

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = DOT;
            if ((mask & 1) != 0) shape = Shapes.or(shape, ARM_NORTH);
            if ((mask & 2) != 0) shape = Shapes.or(shape, ARM_SOUTH);
            if ((mask & 4) != 0) shape = Shapes.or(shape, ARM_EAST);
            if ((mask & 8) != 0) shape = Shapes.or(shape, ARM_WEST);
            shapes[mask] = shape;
        }
        return shapes;
    }

    public CourtLineBlock(Properties props) {
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
        int mask = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(SOUTH) ? 2 : 0)
                | (state.getValue(EAST) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
        return SHAPES_BY_CONNECTIONS[mask];
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
