package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Long cafeteria lunch table with attached molded-plastic bench seats on both sides; FACING-only, flammable (laminate top). */
public class CafeteriaTableBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    public CafeteriaTableBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
