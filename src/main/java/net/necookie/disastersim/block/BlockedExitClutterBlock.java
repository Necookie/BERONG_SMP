package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stacked chairs and boxes jammed against a doorway — a training/discussion prop illustrating an
 * obstructed egress. Not a {@code HazardBlock} (it doesn't ignite on its own); it burns like any
 * other flammable furniture if a nearby fire reaches it. FACING-only.
 */
public class BlockedExitClutterBlock extends FlammableFacingBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public BlockedExitClutterBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
