package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Café-style espresso machine whose scale-blocked pressure-relief valve over-pressurizes the boiler. */
public class EspressoMachineBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 14, 12, 14);

    public EspressoMachineBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.3, z, 0, 0.01, 0);
        } else {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You bled the pressure and killed the power.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ The over-pressurized espresso machine's control board shorts and catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
