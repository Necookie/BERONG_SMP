package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall punch clock with a timecard rack — Philippine office staple; FACING-only, not flammable. */
public class BundyTimeClockBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(2, 4, 13, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 4, 0,  14, 14, 3);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 4, 2,  3,  14, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 4, 2, 16, 14, 14);

    public BundyTimeClockBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
