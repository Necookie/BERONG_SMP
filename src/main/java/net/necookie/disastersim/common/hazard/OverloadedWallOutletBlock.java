package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall outlet with too many plugs drawing too much current; sparks and scorches the faceplate when hazardous. */
public class OverloadedWallOutletBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(6, 3, 14, 10, 7, 16);
    private static final VoxelShape SHAPE_EW = box(0, 3, 6, 2, 7, 10);

    public OverloadedWallOutletBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
        double y = pos.getY() + 0.25 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        if (rand.nextBoolean()) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the extra devices and eased the load on the outlet.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ The overloaded outlet arcs and scorches the wall alight!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
