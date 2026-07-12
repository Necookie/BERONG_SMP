package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall shelf of colored ring binders; FACING-only, flammable. */
public class WallBinderShelfBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 2, 12, 15, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 2, 0,  15, 14, 4);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 1,  4,  14, 15);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 2, 1, 16, 14, 15);

    public WallBinderShelfBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
