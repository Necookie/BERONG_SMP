package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Portable single-burner butane ("gasul") camp stove leaking gas at the canister seam — a
 * Class F/K kitchen hazard distinct from {@link LeakingGasValveBlock}'s fixed wall LPG line.
 */
public class LeakingButaneCanisterStoveBlock extends HazardFacingBlock {

    // Full-footprint bounding box: the canister now protrudes asymmetrically toward one side
    // (to make FACING visually legible), so a single conservative box avoids per-rotation math.
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 6, 16);

    public LeakingButaneCanisterStoveBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y - 0.05, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You sealed the canister and shut the valve.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Leaking butane gas catches — a flash fire erupts around the stove!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
