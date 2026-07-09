package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Refrigerated salad bar — steel body, chilled produce wells, and a glass sneeze guard; FACING-only. */
public class SaladBarBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public SaladBarBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
