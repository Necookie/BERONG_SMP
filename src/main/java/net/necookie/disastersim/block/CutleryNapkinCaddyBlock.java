package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Countertop cutlery cups + napkin holder caddy; symmetric, no facing needed. */
public class CutleryNapkinCaddyBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 6, 13);

    public CutleryNapkinCaddyBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
