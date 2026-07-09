package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted cafeteria menu board — chalk surface with a bold header stripe, wood frame; FACING-only, flammable. */
public class CafeteriaMenuBoardBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 1, 14, 16, 15, 16);
    private static final VoxelShape SHAPE_EW = box(14, 1, 0, 16, 15, 16);

    public CafeteriaMenuBoardBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
