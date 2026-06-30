package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dark-oak classroom/office chair; FACING-only, flammable. */
public class ChairBlock extends FlammableFacingBlock {

    // Seat + backrest footprint; backrest is at south (Z=11-13) when FACING=NORTH
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 2,  14, 13, 13);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 3,  14, 13, 14);
    private static final VoxelShape SHAPE_EAST  = Block.box(3, 0, 2,  14, 13, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(2, 0, 2,  13, 13, 14);

    public ChairBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
