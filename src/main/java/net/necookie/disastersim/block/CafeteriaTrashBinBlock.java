package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dual recycle/trash bin station — green recycling bin beside a gray trash bin, both with swing-flap lids; symmetric, no facing needed. */
public class CafeteriaTrashBinBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 13, 15);

    public CafeteriaTrashBinBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
