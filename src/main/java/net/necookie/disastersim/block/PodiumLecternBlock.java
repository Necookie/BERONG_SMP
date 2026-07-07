package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wooden speaker's podium with the school seal; stage and classroom front. Flammable. */
public class PodiumLecternBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 2, 14, 14, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 2, 14, 14, 14);

    public PodiumLecternBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
