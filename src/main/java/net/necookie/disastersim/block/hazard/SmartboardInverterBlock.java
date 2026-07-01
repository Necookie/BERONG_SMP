package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmartboardInverterBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 1, 16, 15, 3);
    private static final VoxelShape SHAPE_EW = box(1, 0, 0, 3, 15, 16);

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    public SmartboardInverterBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.9;
        double z = pos.getZ() + 0.1;
        level.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, 0, -0.02, 0);
    }

    @Override
    public String failureMessage() {
        return "§c🖥 Water-shorted circuitry arcs, igniting the wall substrate behind the smartboard!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
