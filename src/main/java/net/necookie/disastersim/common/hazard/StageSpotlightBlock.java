package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StageSpotlightBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 16, 13);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public StageSpotlightBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.5 + rand.nextGaussian() * 0.1;
        double y = pos.getY() + 0.9;
        double z = pos.getZ() + 0.5 + rand.nextGaussian() * 0.1;
        if (rand.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.04, 0);
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 200;
    }

    @Override
    public String failureMessage() {
        return "§c🎭 The overheated spotlight ignites the curtains — a climbing Class A fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
