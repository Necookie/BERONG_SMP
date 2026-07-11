package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted automatic air-freshener dispenser misting flammable propellant near a heat register; FACING. */
public class AerosolFreshenerDispenserBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(5, 9, 14, 11, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = box(5, 9, 0,  11, 15, 2);
    private static final VoxelShape SHAPE_EAST  = box(0, 9, 5,  2,  15, 11);
    private static final VoxelShape SHAPE_WEST  = box(14, 9, 5, 16, 15, 11);

    public AerosolFreshenerDispenserBlock(Properties props) { super(props); }

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
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You relocated the dispenser away from the heat register.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The misting propellant finds the heat register and flashes alight!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
