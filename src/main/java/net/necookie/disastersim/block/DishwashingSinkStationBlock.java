package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Commercial triple-basin wash sink with a gooseneck sprayer; FACING-only, not flammable (steel/ceramic). */
public class DishwashingSinkStationBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public DishwashingSinkStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
