package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Chef's stainless prep table with an open drawer and an oily rag draped beside a nearby burner. */
public class ChefsPrepDrawersBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 13, 16);

    public ChefsPrepDrawersBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.55 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.05, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You moved the rag clear of the burner.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The oily rag on the prep table catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
