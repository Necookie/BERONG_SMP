package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Charcoal rotisserie spit — live coals unattended, rendered fat dripping onto the coals flares up.
 * The only solid-fuel (charcoal) hazard in the set. Class F/K.
 */
public class LechonRotisserieSpitBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);

    public LechonRotisserieSpitBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y - 0.1, z, 0, -0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You raked the coals down and shielded the drip pan.";
    }

    @Override
    public String failureMessage() {
        return "§c🔥 Fat dripping onto the unattended coals flares up into a fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
