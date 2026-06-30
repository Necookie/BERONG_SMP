package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Flat wall-mounted classroom whiteboard; FACING-only. */
public class WhiteboardBlock extends HorizontalFacingBlock {

    // Board body (Z=11-16) + marker tray (Z=11) when FACING=NORTH
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 2, 11, 16, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 2, 0,  16, 14, 5);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 0,  5,  14, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(11, 2, 0, 16, 14, 16);

    public WhiteboardBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
