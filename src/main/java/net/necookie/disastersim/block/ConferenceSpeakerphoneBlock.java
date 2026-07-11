package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low three-legged "spider" conference-table speakerphone; symmetric, no facing. */
public class ConferenceSpeakerphoneBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 4, 12);

    public ConferenceSpeakerphoneBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
