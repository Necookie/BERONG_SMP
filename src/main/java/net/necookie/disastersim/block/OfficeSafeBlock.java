package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Squat steel safe with a dial and handle; FACING-only, not flammable. */
public class OfficeSafeBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 2, 14, 12, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 2, 14, 12, 14);

    public OfficeSafeBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
