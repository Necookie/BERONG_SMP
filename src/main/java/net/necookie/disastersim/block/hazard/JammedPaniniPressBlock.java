package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JammedPaniniPressBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 2, 15, 7, 14);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public JammedPaniniPressBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.45;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.03, 0);
        if (rand.nextBoolean())
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.02, 0);
    }
}
