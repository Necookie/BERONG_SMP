package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * E-bike parked and charging indoors overnight on a cheap, uncertified charger — the pack warms
 * and hisses. Distinct from {@code swollen_phone_battery}/{@code damaged_lipo_pack}/{@code
 * charging_cart}: this is a vehicle-scale pack + charger, not a pocket device or a laptop cart.
 * FACING.
 */
public class EbikeChargingStationBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 0, 4, 15, 12, 12);
    private static final VoxelShape SHAPE_EW = box(4, 0, 1, 12, 12, 15);

    public EbikeChargingStationBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.4;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the cheap charger before the pack ran away.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The e-bike battery goes into thermal runaway and bursts into flame!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
