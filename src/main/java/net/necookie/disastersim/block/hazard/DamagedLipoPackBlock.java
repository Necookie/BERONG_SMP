package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DamagedLipoPackBlock extends HazardBlock {

    public DamagedLipoPackBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(5) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.25;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 100;
    }

    @Override
    public String failureMessage() {
        return "§4🔋 The damaged LiPo pack expels a violent white-hot chemical burst!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 4);
    }
}
