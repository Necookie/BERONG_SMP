package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CeilingProjectorBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 10, 4, 13, 16, 12);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public CeilingProjectorBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + 0.5 + rand.nextGaussian() * 0.15;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.5 + rand.nextGaussian() * 0.15;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, -0.01, 0);
        if (rand.nextBoolean())
            level.addParticle(ParticleTypes.LAVA, x, y + 0.05, z, 0, 0, 0);
    }

    @Override
    public String failureMessage() {
        return "§c📽 The projector bulb shatters, dropping melting plastic fire clusters onto the benches!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 1, 2);
    }
}
