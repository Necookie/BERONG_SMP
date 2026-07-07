package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CommercialDeepFryerBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public CommercialDeepFryerBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
        double y = pos.getY() + 0.9 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
        if (rand.nextInt(2) == 0) level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
        if (rand.nextInt(2) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.05, z, 0, 0.04, 0);
        if (rand.nextInt(6) == 0) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 100;
    }

    @Override
    public String failureMessage() {
        return "§4🍟 The fryer oil reaches auto-ignition temperature — a massive kitchen grease fire erupts!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 4);
    }
}
