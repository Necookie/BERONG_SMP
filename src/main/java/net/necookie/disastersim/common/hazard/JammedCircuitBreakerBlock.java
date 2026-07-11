package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Circuit breaker box whose trip switch has been jammed (a coin or matchstick wedged in the
 * lever) so it physically can't cut power on overload — distinct from {@link OverloadedBreakerPanelBlock},
 * whose danger is simply drawing more load than it's rated for. Here the safety device itself has
 * been sabotaged, so an overload has nothing left to stop it.
 */
public class JammedCircuitBreakerBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(3, 4, 13, 13, 14, 16);
    private static final VoxelShape SHAPE_EW = box(0, 4, 3, 3, 14, 13);

    public JammedCircuitBreakerBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.15, z, 0.01, 0.01, 0);
        if (rand.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y - 0.1, z, 0, -0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cleared the jam — the breaker can trip again.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ With its trip switch jammed, the breaker box overheats and flashes over!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
