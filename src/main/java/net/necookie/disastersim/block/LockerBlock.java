package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tall school locker; FACING-only. */
public class LockerBlock extends HorizontalFacingBlock {

    // Tall single-door locker; door panel protrudes 2px toward facing direction
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 2,  14, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 0,  14, 16, 14);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 2,  14, 16, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(2, 0, 2,  16, 16, 14);

    public LockerBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
