package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Generic {@link HazardFacingBlock} driven entirely by a {@link HazardSpec} instead of a
 * dedicated per-prop subclass — see {@link HazardSpecs} for the props registered against this.
 * Register via {@code BLOCKS.registerBlock(name, p -> new SimpleHazardFacingBlock(p, HazardSpecs.X), () -> props)}
 * — the standard NeoForge function-supplier overload is preserved, just with a spec-bearing
 * constructor instead of a bare no-arg one.
 */
public class SimpleHazardFacingBlock extends HazardFacingBlock {

    private final HazardSpec spec;

    public SimpleHazardFacingBlock(Properties props, HazardSpec spec) {
        super(props);
        this.spec = spec;
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return spec.shapeFor().apply(facing);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        spec.particleEmitter().emit(level, pos, state, rand);
    }

    @Override
    public int failureDelayTicks() {
        return spec.failureDelayTicks();
    }

    @Override
    public String preventMessage() {
        return spec.preventMessage();
    }

    @Override
    public String failureMessage() {
        return spec.failureMessage();
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        spec.failureAction().apply(level, pos, state);
    }
}
