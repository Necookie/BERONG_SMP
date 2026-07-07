package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Green chalkboard with tray — the traditional sibling of the whiteboard. Flammable (wood frame). */
public class BlackboardBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 1, 13, 16, 15, 16);
    private static final VoxelShape SHAPE_EW = box(13, 1, 0, 16, 15, 16);

    public BlackboardBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
