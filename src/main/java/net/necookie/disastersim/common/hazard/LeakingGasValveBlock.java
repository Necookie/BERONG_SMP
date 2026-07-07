package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cafeteria-kitchen LPG line with a leaking valve; hissing gas cloud when hazardous, wide flash fire on failure. Kitchen-class: only the wet chemical extinguisher may defuse it. */
public class LeakingGasValveBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 4, 12, 16, 12, 16);
    private static final VoxelShape SHAPE_EW = box(12, 4, 0, 16, 12, 16);

    public LeakingGasValveBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.4 + rand.nextDouble() * 0.3;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You wrenched the leaking valve shut before the gas found a spark.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The leaked LPG cloud finds an ignition source — flash fire across the kitchen!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
