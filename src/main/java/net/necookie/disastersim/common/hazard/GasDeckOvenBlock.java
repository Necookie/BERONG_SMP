package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Gas baking deck oven with a stuck thermostat; a cracked door radiates heat onto nearby combustibles. Class F/K. */
public class GasDeckOvenBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public GasDeckOvenBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.4 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You closed the door and shut the gas.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Radiant heat from the cracked oven door ignites nearby combustibles!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
