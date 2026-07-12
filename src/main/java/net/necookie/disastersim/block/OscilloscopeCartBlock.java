package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rolling instrument trolley with an oscilloscope on top; FACING-only, not flammable. */
public class OscilloscopeCartBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);

    public OscilloscopeCartBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
