package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Indoor Philippine flag on a brass pole stand; lobby and stage decor. */
public class PhilippineFlagStandBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_EW = box(4, 0, 4, 12, 16, 12);

    public PhilippineFlagStandBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
