package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Auto-filled pole segment between a {@link BasketballHoopPostBlock} and a
 * {@link BasketballHoopBlock}; not normally hand-placed. Symmetric, no facing.
 */
public class BasketballPoleSegmentBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BasketballPoleSegmentBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
