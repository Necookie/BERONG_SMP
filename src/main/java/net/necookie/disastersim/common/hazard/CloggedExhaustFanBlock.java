package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Kitchen/workshop window exhaust fan caked in dust; the stalled motor smoulders when hazardous. */
public class CloggedExhaustFanBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SHAPE_EW = box(13, 0, 0, 16, 16, 16);

    public CloggedExhaustFanBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.15 + rand.nextDouble() * 0.7;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + 0.15 + rand.nextDouble() * 0.7;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ASH, x, y + 0.1, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cleared the caked dust off the fan blades and housing.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The dust-choked exhaust fan motor stalls, overheats, and ignites its own dust cake!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
