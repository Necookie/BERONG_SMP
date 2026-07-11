package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted flat-panel meeting display, dark standby screen; FACING-only, not flammable. */
public class ConferenceWallDisplayBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 4, 14, 15, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 4, 0,  15, 14, 2);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 4, 1,  2,  14, 15);
    private static final VoxelShape SHAPE_WEST  = Block.box(14, 4, 1, 16, 14, 15);

    public ConferenceWallDisplayBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
