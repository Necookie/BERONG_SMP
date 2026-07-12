package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Under-bench rotary-vane vacuum pump running on degraded black oil, belt slipping and smoking; symmetric. */
public class OverheatedVacuumPumpBlock extends HazardBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 8, 13);

    public OverheatedVacuumPumpBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.4;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You shut off the pump and scheduled an oil change.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The degraded oil and slipping belt finally ignite!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
