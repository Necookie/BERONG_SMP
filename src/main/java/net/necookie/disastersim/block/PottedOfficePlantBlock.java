package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative floor planter with a broadleaf plant; symmetric, no facing, noOcclusion. */
public class PottedOfficePlantBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public PottedOfficePlantBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
