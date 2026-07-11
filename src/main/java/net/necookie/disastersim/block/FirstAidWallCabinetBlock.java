package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted white first-aid cabinet with a green cross; FACING-only, purely decorative. */
public class FirstAidWallCabinetBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(3, 3, 13, 13, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3, 3, 0,  13, 14, 3);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 3, 3,  3,  14, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 3, 3, 16, 14, 13);

    public FirstAidWallCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
