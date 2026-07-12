package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Large floor office copier with paper trays and a control panel; FACING-only, not flammable, purely decorative. */
public class OfficePhotocopierBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 1, 15, 12, 15);
    private static final VoxelShape SHAPE_EW = box(1, 0, 1, 15, 12, 15);

    public OfficePhotocopierBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
