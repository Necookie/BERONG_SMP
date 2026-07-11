package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Over-lamped wall dimmer switch whose triac buzzes and scorches the faceplate; FACING. */
public class FaultyDimmerSwitchBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(6, 6, 15, 10, 10, 16);
    private static final VoxelShape SHAPE_SOUTH = box(6, 6, 0,  10, 10, 1);
    private static final VoxelShape SHAPE_EAST  = box(0, 6, 6,  1,  10, 10);
    private static final VoxelShape SHAPE_WEST  = box(15, 6, 6, 16, 10, 10);

    public FaultyDimmerSwitchBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.45 + rand.nextDouble() * 0.1;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.45 + rand.nextDouble() * 0.1;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You removed the extra lamp load and let the dimmer cool.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The over-lamped dimmer scorches through the faceplate and ignites the wall!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
