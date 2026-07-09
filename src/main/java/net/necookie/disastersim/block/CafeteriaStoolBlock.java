package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Round cafeteria stool — red vinyl seat on a chrome center pole and base; symmetric, no facing needed. */
public class CafeteriaStoolBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

    public CafeteriaStoolBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
