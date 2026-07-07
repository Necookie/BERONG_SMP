package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Science-lab chemical shelf with a tipped, leaking reagent bottle; fumes when hazardous, wide flash-ignition on failure. */
public class ReagentStorageShelfBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(0, 2, 11, 16, 14, 16);
    private static final VoxelShape SHAPE_EW = box(11, 2, 0, 16, 14, 16);

    public ReagentStorageShelfBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.1 + rand.nextDouble() * 0.8;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.5;
        double z = pos.getZ() + 0.1 + rand.nextDouble() * 0.8;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.015, 0);
        level.addParticle(ParticleTypes.DRIPPING_WATER, x, y - 0.2, z, 0, -0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You righted the tipped reagent bottle and sealed it properly.";
    }

    @Override
    public String failureMessage() {
        return "§c⚗ Leaking reagents mix on the shelf and flash into a chemical fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
