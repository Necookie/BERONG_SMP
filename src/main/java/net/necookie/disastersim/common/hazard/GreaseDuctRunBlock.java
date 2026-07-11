package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Horizontal ceiling exhaust duct above the cooking line, its interior caked with flammable
 * grease — distinct from {@link GreaseCloggedHoodBlock} (the hood canopy directly over a stove):
 * this is the overhead ductwork carrying smoke away. Class F/K.
 */
public class GreaseDuctRunBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 12, 0, 16, 16, 16);

    public GreaseDuctRunBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.7 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You scraped the grease from the accessible duct panel.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Grease built up in the exhaust duct catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
