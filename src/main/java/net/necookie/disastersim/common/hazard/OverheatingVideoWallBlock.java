package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Multi-panel LED video wall whose driver stack overheats behind the panels; FACING. */
public class OverheatingVideoWallBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(1, 3, 14, 15, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = box(1, 3, 0,  15, 15, 2);
    private static final VoxelShape SHAPE_EAST  = box(0, 3, 1,  2,  15, 15);
    private static final VoxelShape SHAPE_WEST  = box(14, 3, 1, 16, 15, 15);

    public OverheatingVideoWallBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.3;
        double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You powered down the video wall and let the driver stack cool.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The overheated driver stack ignites behind the video wall!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
