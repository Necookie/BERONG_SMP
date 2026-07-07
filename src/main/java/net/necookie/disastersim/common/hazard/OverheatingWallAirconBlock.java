package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Window-type wall aircon with a condensation leak dripping toward its own wiring; drips and sparks when hazardous. */
public class OverheatingWallAirconBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 5, 10, 15, 13, 16);
    private static final VoxelShape SHAPE_EW = box(10, 5, 1, 16, 13, 15);

    public OverheatingWallAirconBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.35;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, 0, -0.03, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + 0.2, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You switched the aircon off before the leak reached the wiring.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Condensation drips into the aircon's live wiring — it shorts and catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
