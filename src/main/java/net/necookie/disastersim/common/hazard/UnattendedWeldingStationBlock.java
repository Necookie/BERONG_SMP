package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vocational-shop arc welder left running unattended; a shower of hot spatter risks nearby combustibles. */
public class UnattendedWeldingStationBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 10, 15);

    public UnattendedWeldingStationBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.02, 0.03, 0.02);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, -0.02, 0.02, -0.02);
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y - 0.1, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You shut off the torch and cleared the spatter zone.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Hot spatter from the unattended welder ignites nearby debris!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
