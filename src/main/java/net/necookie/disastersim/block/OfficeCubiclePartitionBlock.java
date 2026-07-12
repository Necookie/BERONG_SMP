package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fabric-covered cubicle divider panel; FACING-only, flammable. */
public class OfficeCubiclePartitionBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape SHAPE_EW = box(6, 0, 0, 10, 16, 16);

    public OfficeCubiclePartitionBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
