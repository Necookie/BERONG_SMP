package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    /** Ticks a hazardous prop stays active before its failure consequence fires. Override to tune pacing. */
    public int failureDelayTicks() {
        return 300;
    }

    /** Chat flavor text broadcast to the player when this prop's failure consequence triggers. */
    public String failureMessage() {
        return "§c⚠ A neglected hazard just started a fire!";
    }

    /** World mutation performed when this prop is left hazardous for {@link #failureDelayTicks()}. */
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }

    /** Lights up to {@code maxBlocks} adjacent air blocks on fire. */
    protected static void igniteAdjacent(Level level, BlockPos pos, int maxBlocks) {
        int lit = 0;
        for (Direction dir : Direction.values()) {
            if (lit >= maxBlocks) break;
            BlockPos target = pos.relative(dir);
            if (level.getBlockState(target).isAir()) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit++;
            }
        }
    }

    /** Lights up to {@code maxBlocks} air blocks within a horizontal {@code radius} — for explosive failures. */
    protected static void igniteRadius(Level level, BlockPos pos, int radius, int maxBlocks) {
        int lit = 0;
        for (BlockPos target : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            if (lit >= maxBlocks) break;
            if (level.getBlockState(target).isAir()) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit++;
            }
        }
    }
}
