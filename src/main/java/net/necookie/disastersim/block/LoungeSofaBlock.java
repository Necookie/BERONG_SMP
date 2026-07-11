package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-seat fabric reception sofa; FACING-only, flammable. */
public class LoungeSofaBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 11, 15);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 15, 11, 16);

    public LoungeSofaBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
