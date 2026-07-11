package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ceiling-mounted retractable projection screen, tube motor stalled mid-retract; FACING. */
public class JammedProjectionScreenBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 11, 3, 13, 16, 13);

    public JammedProjectionScreenBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        if (rand.nextBoolean()) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cut power to the stalled motor before the housing overheated.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The jammed screen motor overheats and ignites its housing!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
