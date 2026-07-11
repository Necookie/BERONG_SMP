package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall-mounted red cabinet with a glass front holding an extinguisher; FACING-only, purely
 * decorative safety-equipment prop, distinct from the hose-reel {@link FireHoseCabinetBlock}.
 */
public class FireExtinguisherCabinetBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 12, 14, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0,  14, 15, 4);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 2,  4,  15, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 2, 2, 16, 15, 14);

    public FireExtinguisherCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
