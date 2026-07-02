package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightBulbBlock extends Block {

    // Flush-mount LED panel: thin diffuser + recessed housing bezel near the ceiling; symmetric, no facing.
    private static final VoxelShape SHAPE = Block.box(1, 14, 1, 15, 16, 15);

    public LightBulbBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
