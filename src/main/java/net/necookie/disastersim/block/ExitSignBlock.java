package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Always-lit green EXIT sign (light level 7 via registration) - wayfinding decor with a real
 * light source, so corridors read correctly even in a blackout scene.
 */
public class ExitSignBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(3, 8, 14, 13, 14, 16);
    private static final VoxelShape SHAPE_EW = box(14, 8, 3, 16, 14, 13);

    public ExitSignBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
