package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Teacher's desk with a drawer pedestal; the classroom's front-of-room anchor. Flammable. */
public class TeachersDeskBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 12, 15);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 15, 12, 16);

    public TeachersDeskBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
