package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Art/shop-room shelf of unsealed paint-thinner and lacquer cans, giving off flammable vapor. */
public class UnsealedSolventShelfBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 8, 15, 16, 16);

    public UnsealedSolventShelfBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.6 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You sealed the cans and moved them away from any heat source.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Solvent vapor from the unsealed cans catches and flashes over!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
