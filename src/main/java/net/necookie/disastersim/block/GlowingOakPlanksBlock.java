package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A full-cube block that looks, sounds, and burns exactly like vanilla oak planks (same texture,
 * same flammability profile) but is lit at {@code lightLevel=15} via its {@code Block.Properties}
 * — a disguised light source for builds that want illumination without a visible fixture. Display
 * name and texture deliberately match vanilla oak planks; only the registry name
 * ({@code berongsmp:glowing_oak_planks}) gives it away.
 */
public class GlowingOakPlanksBlock extends Block {

    private static final int FLAMMABILITY = 20;
    private static final int FIRE_SPREAD_SPEED = 5;

    public GlowingOakPlanksBlock(Properties props) {
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
