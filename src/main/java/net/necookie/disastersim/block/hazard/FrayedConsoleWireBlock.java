package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FrayedConsoleWireBlock extends HazardBlock {

    public FrayedConsoleWireBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.12;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
        if (rand.nextBoolean())
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.05, z, 0, 0.01, 0);
    }
}
