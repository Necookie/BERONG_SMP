package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall pigeonhole mail-slot unit with envelopes in some slots; FACING-only, flammable. */
public class MailSortingShelfBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 3, 13, 15, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 3, 0,  15, 15, 3);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 3, 1,  3,  15, 15);
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 3, 1, 16, 15, 15);

    public MailSortingShelfBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
