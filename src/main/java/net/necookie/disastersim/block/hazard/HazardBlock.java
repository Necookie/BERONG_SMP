package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Base for symmetric (no-facing) hazard prop blocks. Adds a single {@code hazardous} boolean
 * state and wires {@link #animateTick} to call {@link #spawnHazardParticles} when hazardous=true.
 */
public abstract class HazardBlock extends Block {

    public static final BooleanProperty HAZARDOUS = BooleanProperty.create("hazardous");

    protected HazardBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HAZARDOUS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAZARDOUS);
    }

    /** Emit client-side particles that signal the hazardous state. Called every animateTick. */
    protected abstract void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (state.getValue(HAZARDOUS)) {
            spawnHazardParticles(level, pos, state, rand);
        }
    }
}
