package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lab dehumidifier whose coil ices over, compressor short-cycling and overheating — a real
 * recall-class appliance fire, distinct from {@code overheating_wall_aircon} (window unit,
 * condensation-into-wiring). FACING.
 */
public class FaultyDehumidifierBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 12, 14);

    public FaultyDehumidifierBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.2, z, 0, -0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the unit and let the iced coil thaw.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The short-cycling compressor overheats and ignites!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
