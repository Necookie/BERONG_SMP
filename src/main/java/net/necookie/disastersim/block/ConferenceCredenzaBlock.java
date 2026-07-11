package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low walnut sideboard cabinet with sliding doors; FACING-only, flammable. */
public class ConferenceCredenzaBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 10, 15);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 15, 10, 16);

    public ConferenceCredenzaBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
