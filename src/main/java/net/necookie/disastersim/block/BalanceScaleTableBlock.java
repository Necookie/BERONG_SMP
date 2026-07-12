package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Anti-vibration granite balance table with an analytical balance under a draft shield; FACING-only, not flammable. */
public class BalanceScaleTableBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 2, 15, 11, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 1, 14, 11, 15);

    public BalanceScaleTableBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
