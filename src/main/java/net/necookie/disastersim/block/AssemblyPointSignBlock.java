package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Standing green "ASSEMBLY AREA" sign on a post, marking an {@code AssemblyZone} muster point;
 * FACING-only, always lit like {@link ExitSignBlock}. Distinct from that block, which marks doors,
 * not the outdoor assembly point.
 */
public class AssemblyPointSignBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_EW = Block.box(6, 0, 6, 10, 16, 10);

    public AssemblyPointSignBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
