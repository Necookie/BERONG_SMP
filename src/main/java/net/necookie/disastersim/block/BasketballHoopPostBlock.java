package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.necookie.disastersim.registry.ModBlocks;

/**
 * Basketball hoop stand base — the bottom anchor of the expandable pole. Placing a
 * {@link BasketballHoopBlock} anywhere straight up (up to {@link #MAX_DISTANCE} blocks, clear
 * air/pole path) automatically fills the gap with {@link BasketballPoleSegmentBlock}s, so the
 * stand "expands" to whatever height the hoop is placed at instead of needing a fixed prefab
 * height. {@link #playerWillDestroy} tears the connected pole run back down.
 */
public class BasketballHoopPostBlock extends Block {

    private static final int MAX_DISTANCE = 24;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public BasketballHoopPostBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        for (int i = 1; i <= MAX_DISTANCE; i++) {
            BlockPos candidate = pos.above(i);
            BlockState candidateState = level.getBlockState(candidate);
            if (candidateState.getBlock() instanceof BasketballHoopBlock) {
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
            BlockPos p = pos.above();
            while (level.getBlockState(p).getBlock() instanceof BasketballPoleSegmentBlock) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                p = p.above();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void fillRun(Level level, BlockPos from, int steps) {
        BlockState segment = ModBlocks.BASKETBALL_POLE.get().defaultBlockState();
        for (int i = 1; i < steps; i++) {
            level.setBlockAndUpdate(from.above(i), segment);
        }
    }
}
