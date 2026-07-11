package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Full-size 4-burner gas range whose control knob has melted and jammed — a burner won't shut off
 * and cross-lights the empty grate. Distinct from {@link UnattendedGreasePanBlock} (a pan of oil
 * on a burner) — no pan is involved, the appliance's own control has failed. Class F/K.
 */
public class GasRangeStuckBurnerBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public GasRangeStuckBurnerBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.55 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
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
        return "§a✔ Prevented! You reached behind and shut the range's gas cock.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The stuck burner's gas flare ignites the range and counter!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
