package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DustChokedPcBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 0, 2, 13, 14, 14);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public DustChokedPcBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(6) != 0) return;
        double x = pos.getX() + 0.5 + rand.nextGaussian() * 0.2;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + 0.1;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
    }
}
