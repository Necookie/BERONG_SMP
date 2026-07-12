package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall cabinet of many small labeled electronics-parts drawers; FACING-only, not flammable. */
public class ComponentDrawerCabinetBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(1, 1, 11, 15, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = box(1, 1, 0,  15, 15, 5);
    private static final VoxelShape SHAPE_EAST  = box(0, 1, 1,  5,  15, 15);
    private static final VoxelShape SHAPE_WEST  = box(11, 1, 1, 16, 15, 15);

    public ComponentDrawerCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
