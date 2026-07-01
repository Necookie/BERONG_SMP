package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PaSystemBackupBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 16, 3);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 3, 16, 16);

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    public PaSystemBackupBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.4;
        double z = pos.getZ() + 0.05;
        level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.05, z, 0, 0.01, 0);
    }

    @Override
    public String failureMessage() {
        return "§c📢 The failing battery cell ignites a severe electrical panel fire — PA system down!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
