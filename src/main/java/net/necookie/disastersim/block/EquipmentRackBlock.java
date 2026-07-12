package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-standing 19-inch instrument rack with blank and patch panels; FACING-only, not flammable. */
public class EquipmentRackBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 3, 14, 16, 13);
    private static final VoxelShape SHAPE_EW = box(3, 0, 2, 13, 16, 14);

    public EquipmentRackBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
