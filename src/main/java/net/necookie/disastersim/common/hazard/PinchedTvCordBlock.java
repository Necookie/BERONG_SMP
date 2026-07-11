package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TV power cord crushed flat by its wall mount bracket, insulation split; FACING. */
public class PinchedTvCordBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(3, 8, 15, 13, 11, 16);
    private static final VoxelShape SHAPE_SOUTH = box(3, 8, 0,  13, 11, 1);
    private static final VoxelShape SHAPE_EAST  = box(0, 8, 3,  1,  11, 13);
    private static final VoxelShape SHAPE_WEST  = box(15, 8, 3, 16, 11, 13);

    public PinchedTvCordBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
        double y = pos.getY() + 0.6;
        double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You rerouted the cord clear of the mount bracket's pinch point.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The crushed cord arcs through its split insulation and ignites the wall!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
