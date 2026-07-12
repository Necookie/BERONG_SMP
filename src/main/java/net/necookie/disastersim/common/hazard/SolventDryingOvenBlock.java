package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lab drying oven loaded with solvent-wet glassware, flammable vapor accumulating in a heated
 * chamber — the hazard is vapor ignition, distinct from the kitchen {@code gas_deck_oven}/{@code
 * convection_oven} (appliance failure, not vapor accumulation). FACING.
 */
public class SolventDryingOvenBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 13, 15);

    public SolventDryingOvenBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
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
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You opened the door and vented the accumulated vapor.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The accumulated solvent vapor flashes over inside the chamber!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
