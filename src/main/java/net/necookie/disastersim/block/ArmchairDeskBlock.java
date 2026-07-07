package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The classic Philippine classroom armchair: plywood seat and writing-tablet arm on a steel frame. Flammable. */
public class ArmchairDeskBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 2, 14, 10, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 2, 14, 10, 14);

    public ArmchairDeskBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
