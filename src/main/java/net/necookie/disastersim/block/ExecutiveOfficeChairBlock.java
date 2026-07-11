package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** High-back padded leather swivel chair; FACING-only, flammable (upholstery). */
public class ExecutiveOfficeChairBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 2, 14, 16, 13);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 3, 14, 16, 14);
    private static final VoxelShape SHAPE_EAST  = Block.box(3, 0, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(2, 0, 2, 13, 16, 14);

    public ExecutiveOfficeChairBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
