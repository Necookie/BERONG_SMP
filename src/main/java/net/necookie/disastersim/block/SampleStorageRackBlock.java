package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shelf of labeled cardboard sample boxes and vial trays — a research archive, distinct from the
 * leaking {@code reagent_storage_shelf} hazard. FACING-only, flammable (cardboard).
 */
public class SampleStorageRackBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(1, 1, 12, 15, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = box(1, 1, 0,  15, 15, 4);
    private static final VoxelShape SHAPE_EAST  = box(0, 1, 1,  4,  15, 15);
    private static final VoxelShape SHAPE_WEST  = box(12, 1, 1, 16, 15, 15);

    public SampleStorageRackBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
