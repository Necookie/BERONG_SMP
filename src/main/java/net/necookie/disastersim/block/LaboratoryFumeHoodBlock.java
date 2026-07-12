package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Full-height fume hood with a glass sash and interior work light; FACING-only, not flammable. */
public class LaboratoryFumeHoodBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 2, 15, 16, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 1, 14, 16, 15);

    public LaboratoryFumeHoodBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
