package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChargingCartBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public ChargingCartBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + rand.nextDouble();
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + rand.nextDouble();
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 100;
    }

    @Override
    public String failureMessage() {
        return "§4💥 The overloaded charging cart explodes from battery thermal runaway!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 3);
    }
}
