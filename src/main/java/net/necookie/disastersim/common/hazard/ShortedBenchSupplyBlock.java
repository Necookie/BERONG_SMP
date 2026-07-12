package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Benchtop DC power supply whose crocodile leads short under a paper schematic, current limit
 * defeated — test equipment energizing a circuit, distinct from building electrical (outlet,
 * breaker, panel). FACING.
 */
public class ShortedBenchSupplyBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 8, 14);

    public ShortedBenchSupplyBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.4;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cleared the shorted leads and reset the current limit.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The shorted leads arc and ignite the schematic paper!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
