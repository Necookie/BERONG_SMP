package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Utility-corridor electrical breaker panel drawing far more load than rated; dense sparking when hazardous. */
public class OverloadedBreakerPanelBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SHAPE_EW = box(13, 0, 0, 16, 16, 16);

    public OverloadedBreakerPanelBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.15 + rand.nextDouble() * 0.7;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + 0.15 + rand.nextDouble() * 0.7;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.15, z, 0.01, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You tripped the main switch and took the load off the panel.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ The overloaded breaker panel flashes over — an electrical fire erupts!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
