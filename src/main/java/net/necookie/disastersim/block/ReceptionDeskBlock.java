package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tall front-counter reception desk with a raised transaction ledge; FACING-only, flammable. */
public class ReceptionDeskBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 14, 15);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 15, 14, 16);

    public ReceptionDeskBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
