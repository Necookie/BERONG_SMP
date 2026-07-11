package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Glass-top induction hob whose cooling fan fails; power electronics overheat under a dry, boiled pan. */
public class InductionCooktopStationBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 4, 15);

    public InductionCooktopStationBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.25 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You switched it off at the wall and lifted the pan.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ The induction cooktop's overheated electronics catch fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
