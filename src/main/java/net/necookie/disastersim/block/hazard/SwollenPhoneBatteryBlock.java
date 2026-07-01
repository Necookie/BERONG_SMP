package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SwollenPhoneBatteryBlock extends HazardBlock {

    public SwollenPhoneBatteryBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.2;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 150;
    }

    @Override
    public String failureMessage() {
        return "§c📱 The overheating phone battery ruptures into an intense, torch-like chemical fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
