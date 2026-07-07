package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall clock permanently reading ten past ten; classroom and corridor decor. */
public class WallClockBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(4, 4, 13, 12, 12, 16);
    private static final VoxelShape SHAPE_EW = box(13, 4, 4, 16, 12, 12);

    public WallClockBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
