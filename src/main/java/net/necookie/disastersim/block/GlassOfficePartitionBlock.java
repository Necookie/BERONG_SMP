package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Frosted-glass partition panel in an aluminum frame; FACING-only, not flammable, noOcclusion. */
public class GlassOfficePartitionBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape SHAPE_EW = box(7, 0, 0, 9, 16, 16);

    public GlassOfficePartitionBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
