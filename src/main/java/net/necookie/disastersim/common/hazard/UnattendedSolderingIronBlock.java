package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Benchtop soldering iron left powered at full temperature, tip burning through the mat next to
 * flux and wire spools. Distinct from {@code unattended_welding_station} (shop-scale arc welder,
 * not a benchtop electronics tool). FACING.
 */
public class UnattendedSolderingIronBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 4, 13);

    public UnattendedSolderingIronBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.25;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You returned the iron to its holder and switched it off.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The unattended iron burns through the mat and ignites the flux and spools!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
