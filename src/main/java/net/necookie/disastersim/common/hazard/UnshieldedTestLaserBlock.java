package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bench Class-4 laser running with its beam dump missing, burning a spot on the cable tray; FACING (beam axis = front). */
public class UnshieldedTestLaserBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 6, 13);

    public UnshieldedTestLaserBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 250;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You closed the shutter and refit the missing beam dump.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The unshielded beam burns through the cable tray and ignites it!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
