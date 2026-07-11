package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Buffet steam table heated by canned gel-fuel (Sterno) chafing burners left lit under a boiled-dry water pan. */
public class SternoSteamTableBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);

    public SternoSteamTableBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.4 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.02, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You snuffed the gel-fuel cans and covered them.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Unattended chafing fuel flares up under the steam table!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
