package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall comms cabinet with a clogged fan filter, switch stack running over temperature; FACING. */
public class OverheatedNetworkCabinetBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(2, 3, 12, 14, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = box(2, 3, 0,  14, 15, 4);
    private static final VoxelShape SHAPE_EAST  = box(0, 3, 2,  4,  15, 14);
    private static final VoxelShape SHAPE_WEST  = box(12, 3, 2, 16, 15, 14);

    public OverheatedNetworkCabinetBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.6;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cleared the clogged filter and let the switch stack cool.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The overheated switch stack ignites inside the cabinet!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
