package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall-mounted kitchen-to-line pass-through counter/hatch; FACING-only, not flammable (steel).
 * Distinct from the freestanding {@link net.necookie.disastersim.block.ServingCounterBlock} — this
 * is a wall fixture, not a countertop.
 */
public class ServingHatchWindowBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(0, 6, 11, 16, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 6, 0,  16, 12, 5);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 6, 0,  5,  12, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(11, 6, 0, 16, 12, 16);

    public ServingHatchWindowBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
