package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Flat wall poster teaching the PASS extinguisher technique / fire triangle; FACING-only, purely decorative. */
public class FireSafetyPosterBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 15, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0,  14, 14, 1);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 2,  1,  14, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(15, 2, 2, 16, 14, 14);

    public FireSafetyPosterBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
