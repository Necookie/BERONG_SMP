package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A-frame flip-chart easel with a paper pad; FACING-only, flammable. */
public class FlipChartEaselBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(3, 0, 6, 13, 16, 10);
    private static final VoxelShape SHAPE_EW = box(6, 0, 3, 10, 16, 13);

    public FlipChartEaselBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
