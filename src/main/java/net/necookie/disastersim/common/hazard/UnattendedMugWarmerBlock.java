package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Desk mug warmer left on and buried under a memo pile; symmetric, no facing. */
public class UnattendedMugWarmerBlock extends HazardBlock {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 3, 12);

    public UnattendedMugWarmerBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.2;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You switched off the warmer and cleared the memo pile.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The buried mug warmer scorches the memo pile alight!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
