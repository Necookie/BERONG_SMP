package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wheeled two-shelf book return trolley — a mobile fuel-load prop for library fire scenarios; FACING-only, flammable. */
public class RollingBookCartBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public RollingBookCartBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
