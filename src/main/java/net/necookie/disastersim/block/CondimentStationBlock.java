package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Countertop condiment station — ketchup/mustard squeeze bottles and a chrome napkin holder on a metal tray; FACING-only. */
public class CondimentStationBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 9, 14);

    public CondimentStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
