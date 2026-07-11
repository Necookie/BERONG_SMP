package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fixed gas-line elbow joint rusted through with corrosion pinholes weeping gas — distinct from
 * {@link LeakingGasValveBlock} (an opened handwheel valve): here the permanent pipe infrastructure
 * itself is failing from age. Class F/K.
 */
public class CorrodedGasLineJointBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(4, 4, 12, 12, 10, 16);
    private static final VoxelShape SHAPE_SOUTH = box(4, 4, 0,  12, 10, 4);
    private static final VoxelShape SHAPE_EAST  = box(0, 4, 4,  4,  10, 12);
    private static final VoxelShape SHAPE_WEST  = box(12, 4, 4, 16, 10, 12);

    public CorrodedGasLineJointBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
        double y = pos.getY() + 0.35 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You shut the upstream line and clamped the joint.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Gas from the corroded pipe joint finds a spark and flashes over!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
