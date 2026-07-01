package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GreaseCloggedHoodBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 8, 2, 16, 16, 14);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public GreaseCloggedHoodBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.01, 0);
    }
}
