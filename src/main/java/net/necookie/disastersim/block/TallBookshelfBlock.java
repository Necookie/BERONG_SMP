package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Full-height library bookshelf packed with book spines. Flammable — a library fire's favourite fuel. */
public class TallBookshelfBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 2, 16, 16, 16);
    private static final VoxelShape SHAPE_EW = box(2, 0, 0, 16, 16, 16);

    public TallBookshelfBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
