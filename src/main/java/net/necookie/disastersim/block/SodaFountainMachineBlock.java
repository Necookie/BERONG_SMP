package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Red-and-chrome soda fountain machine with a drink-selection panel and cup-fill nozzle; FACING-only. */
public class SodaFountainMachineBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public SodaFountainMachineBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
