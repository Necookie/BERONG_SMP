package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Extendable study/library table, two blocks tall ({@link #HALF} LOWER/UPPER, same idiom as
 * vanilla {@code DoublePlantBlock}). Unlike {@link ComputerTableBlock} (a flat 1-block desk you
 * stand next to), the LOWER half's collision only occupies four corner-post legs, leaving a
 * walkable kneehole; the UPPER half's tabletop slab occupies just its bottom 8px, leaving exactly
 * enough headroom for a sneaking player's ~1.5-block hitbox to fit underneath. That makes this the
 * first placeable object that satisfies {@code DuckCoverHoldManager}/{@code TutorialManager}'s
 * existing "solid block 1-2 above the player's feet" cover check — no changes needed there, since
 * a player standing in the kneehole already has the tabletop at {@code feet.above(1)}.
 *
 * <p>{@link #NORTH}/{@link #SOUTH}/{@link #EAST}/{@link #WEST} (the same idiom as vanilla fences
 * and glass panes) track same-half neighbouring {@code TableBlock}s so lining tables up extends
 * one continuous tabletop (the model hides the edge apron on connected sides) instead of showing
 * two separate tables side by side.
 */
public class TableBlock extends Block {

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    // Four corner legs, floor to tabletop — leaves the centre kneehole (px 3-13 both axes) open.
    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            Block.box(1, 0, 1, 3, 16, 3),
            Block.box(13, 0, 1, 15, 16, 3),
            Block.box(1, 0, 13, 3, 16, 15),
            Block.box(13, 0, 13, 15, 16, 15));
    // Tabletop only fills the bottom half of the block; the open top half is what gives a
    // sneaking player (~1.5-block hitbox) enough clearance to fit under it while standing in the
    // LOWER half's kneehole — the same "half-slab overhang" gap vanilla builds rely on.
    private static final VoxelShape UPPER_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public TableBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(NORTH, connectsAt(level, pos.north(), DoubleBlockHalf.LOWER))
                .setValue(SOUTH, connectsAt(level, pos.south(), DoubleBlockHalf.LOWER))
                .setValue(EAST, connectsAt(level, pos.east(), DoubleBlockHalf.LOWER))
                .setValue(WEST, connectsAt(level, pos.west(), DoubleBlockHalf.LOWER));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos abovePos = pos.above();
        BlockState upper = defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(NORTH, connectsAt(level, abovePos.north(), DoubleBlockHalf.UPPER))
                .setValue(SOUTH, connectsAt(level, abovePos.south(), DoubleBlockHalf.UPPER))
                .setValue(EAST, connectsAt(level, abovePos.east(), DoubleBlockHalf.UPPER))
                .setValue(WEST, connectsAt(level, abovePos.west(), DoubleBlockHalf.UPPER));
        level.setBlock(abovePos, upper, 3);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                level.removeBlock(otherPos, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction dir, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y) {
            boolean facingLinkedNeighbour = (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP);
            boolean linkIntact = neighbourState.is(this) && neighbourState.getValue(HALF) != half;
            if (facingLinkedNeighbour && !linkIntact) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
        }
        BooleanProperty prop = connectionProperty(dir);
        if (prop == null) {
            return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
        }
        boolean connected = neighbourState.is(this) && neighbourState.getValue(HALF) == half;
        return state.setValue(prop, connected);
    }

    private boolean connectsAt(Level level, BlockPos neighbourPos, DoubleBlockHalf half) {
        BlockState state = level.getBlockState(neighbourPos);
        return state.is(this) && state.getValue(HALF) == half;
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
