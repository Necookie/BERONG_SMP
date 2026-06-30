package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * A {@link HorizontalFacingBlock} made of wood/paper that burns during fire simulations.
 * Uses the same flammability profile as vanilla planks-tier furniture so the CCS and library
 * fires spread realistically through classrooms.
 */
public abstract class FlammableFacingBlock extends HorizontalFacingBlock {

    private static final int FLAMMABILITY = 20;
    private static final int FIRE_SPREAD_SPEED = 5;

    protected FlammableFacingBlock(Properties props) {
        super(props);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return FIRE_SPREAD_SPEED;
    }
}
