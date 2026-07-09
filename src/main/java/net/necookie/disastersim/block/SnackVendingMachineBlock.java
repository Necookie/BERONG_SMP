package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Glass-front snack vending machine showing rows of packaged snacks, red frame, coin slot; FACING-only. */
public class SnackVendingMachineBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public SnackVendingMachineBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
