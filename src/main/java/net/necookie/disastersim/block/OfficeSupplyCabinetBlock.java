package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tall stationery cabinet, doors ajar showing paper reams; FACING-only, flammable. */
public class OfficeSupplyCabinetBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 1, 15, 16, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 1, 15, 16, 15);

    public OfficeSupplyCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
