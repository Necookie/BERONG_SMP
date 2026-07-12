package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Green wall eyewash fountain with twin nozzles and a sign panel; FACING-only, not flammable. */
public class EyeWashStationBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(3, 4, 12, 13, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3, 4, 0,  13, 12, 4);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 4, 3,  4,  12, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 4, 3, 16, 12, 13);

    public EyeWashStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
