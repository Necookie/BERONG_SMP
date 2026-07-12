package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bench microscope on a small stand; FACING-only, not flammable, low profile. */
public class MicroscopeStationBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 9, 12);

    public MicroscopeStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
