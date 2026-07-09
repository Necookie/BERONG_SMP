package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Auto-filled badminton net panel placed between two {@link BadmintonNetPostBlock}s. {@link #AXIS}
 * (the same vanilla {@code EnumProperty<Direction.Axis>} used by logs/pillars) records the axis
 * the panel is <em>thin</em> along — its facing/normal direction, not the direction the run of
 * posts travels: a run of posts along X (east-west) needs a wall containing X and Y, thin in Z, so
 * {@link BadmintonNetPostBlock} sets {@code AXIS=Z}; a run along Z (north-south) sets
 * {@code AXIS=X}; a vertical run (Y) sets {@code AXIS=Y}, giving a thin horizontal mesh layer
 * instead of a wall. Not normally placed by hand — {@code BadmintonNetPostBlock.onPlace} fills the
 * run automatically — but it is still a plain obtainable/breakable block for manual touch-ups.
 */
public class BadmintonNetMeshBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    private static final VoxelShape SHAPE_X = Block.box(7, 0, 0, 9, 16, 16);
    private static final VoxelShape SHAPE_Z = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape SHAPE_Y = Block.box(0, 7, 0, 16, 9, 16);

    public BadmintonNetMeshBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case Y -> SHAPE_Y;
        };
    }
}
