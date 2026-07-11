package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Back-of-house stainless prep counter with a cutting board; FACING-only, not flammable (steel). */
public class KitchenPrepCounterBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public KitchenPrepCounterBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
