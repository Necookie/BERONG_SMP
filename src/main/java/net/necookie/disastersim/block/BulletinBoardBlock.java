package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cork bulletin board; FACING-only, flammable. */
public class BulletinBoardBlock extends FlammableFacingBlock {

    // Cork board + dark oak frame, thin wall-mounted panel
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0,  16, 16, 4);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 0,  4,  16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 0, 0, 16, 16, 16);

    public BulletinBoardBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
