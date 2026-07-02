package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall-mounted fire hose reel cabinet; FACING-only, purely decorative safety-equipment prop
 * (not wired into {@code HazardManager} — it never fails or ignites, it's just furniture).
 */
public class FireHoseCabinetBlock extends HorizontalFacingBlock {

    // Cabinet body sits at Z=11-16 (~5px deep), matching WhiteboardBlock/FireAlarmBlock's
    // back-flush-against-the-wall convention.
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 2, 11, 16, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 2, 0,  16, 15, 5);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 0,  5,  15, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(11, 2, 0, 16, 15, 16);

    public FireHoseCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }
}
