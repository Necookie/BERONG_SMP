package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two compressed-gas cylinders properly chained to a wall rack — the safe counterpart to the
 * {@code leaking_oxygen_cylinder} hazard prop. FACING-only, not flammable.
 */
public class SecuredCylinderRackBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 3, 14, 16, 13);
    private static final VoxelShape SHAPE_EW = box(3, 0, 2, 13, 16, 14);

    public SecuredCylinderRackBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
