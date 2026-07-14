package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Generic {@link HazardBlock} (no facing) driven entirely by a {@link HazardSpec} instead of a
 * dedicated per-prop subclass — see {@link HazardSpecs} for the props registered against this.
 * Register via {@code BLOCKS.registerBlock(name, p -> new SimpleHazardBlock(p, HazardSpecs.X), () -> props)}.
 * The spec's {@code shapeFor} is called with a {@code null} facing (a no-facing block never varies
 * shape by direction) — always build the spec with {@link HazardSpec#fixedShape} here.
 */
public class SimpleHazardBlock extends HazardBlock {

    private final HazardSpec spec;

    public SimpleHazardBlock(Properties props, HazardSpec spec) {
        super(props);
        this.spec = spec;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return spec.shapeFor().apply(null);
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
