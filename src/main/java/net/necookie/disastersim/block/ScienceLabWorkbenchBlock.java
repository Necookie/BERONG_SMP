package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Chemical-resistant science-lab bench with a corner sink cutout and gas-tap nub; FACING-only, not flammable (resin/steel). */
public class ScienceLabWorkbenchBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public ScienceLabWorkbenchBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
