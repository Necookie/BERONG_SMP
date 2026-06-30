package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dark-oak chest-of-drawers; FACING-only, flammable. */
public class DrawersBlock extends FlammableFacingBlock {

    // Cabinet body from Z=4-16, drawer fronts protrude to Z=3 — FACING=NORTH
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 2,  16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0,  16, 16, 14);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 0,  14, 16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(2, 0, 0,  16, 16, 16);

    public DrawersBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
