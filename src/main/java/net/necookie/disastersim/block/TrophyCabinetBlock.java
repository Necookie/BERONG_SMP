package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Glass-front trophy display cabinet; glows softly (light 3) to showcase the school's awards. */
public class TrophyCabinetBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 4, 15, 16, 12);
    private static final VoxelShape SHAPE_EW = box(4, 0, 1, 12, 16, 15);

    public TrophyCabinetBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
