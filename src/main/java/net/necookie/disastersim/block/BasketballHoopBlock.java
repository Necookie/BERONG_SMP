package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.necookie.disastersim.registry.ModBlocks;

/**
 * Basketball backboard + rim + net unit — the top anchor of the expandable pole (see
 * {@link BasketballHoopPostBlock}). FACING-only: the backboard faces outward, the mount arm and
 * pole sit behind it. Placing this above a {@link BasketballHoopPostBlock} (up to
 * {@link #MAX_DISTANCE} blocks straight down, clear air/pole path) fills the gap with
 * {@link BasketballPoleSegmentBlock}s. {@link #playerWillDestroy} tears the connected pole run
 * back down.
 */
public class BasketballHoopBlock extends HorizontalFacingBlock {

    private static final int MAX_DISTANCE = 24;
    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 16, 16);

    public BasketballHoopBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        for (int i = 1; i <= MAX_DISTANCE; i++) {
            BlockPos candidate = pos.below(i);
            BlockState candidateState = level.getBlockState(candidate);
            if (candidateState.getBlock() instanceof BasketballHoopPostBlock) {
                fillRun(level, pos, i);
                return;
            }
            if (!candidateState.isAir() && !(candidateState.getBlock() instanceof BasketballPoleSegmentBlock)) {
                return;
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos p = pos.below();
            while (level.getBlockState(p).getBlock() instanceof BasketballPoleSegmentBlock) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                p = p.below();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void fillRun(Level level, BlockPos from, int steps) {
        BlockState segment = ModBlocks.BASKETBALL_POLE.get().defaultBlockState();
        for (int i = 1; i < steps; i++) {
            level.setBlockAndUpdate(from.below(i), segment);
        }
    }
}
