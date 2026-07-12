package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Standing wooden coat rack with a jacket and umbrella hung on it; symmetric, no facing. */
public class CoatRackStandBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public CoatRackStandBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
