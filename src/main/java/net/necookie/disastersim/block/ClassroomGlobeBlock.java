package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Desk globe on a dark stand; geography-corner decor. */
public class ClassroomGlobeBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(4, 0, 4, 12, 11, 12);
    private static final VoxelShape SHAPE_EW = box(4, 0, 4, 12, 11, 12);

    public ClassroomGlobeBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
