package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative planter where a discarded cigarette smolders in dry peat potting mix; symmetric.
 * Plain Class A soil fire — the one Conference Room hazard left off
 * {@code AbstractExtinguisherItem.WET_CHEMICAL_UNSAFE_IDS}, since any extinguisher is genuinely
 * safe on it in real life.
 */
public class SmolderingPlanterBlock extends HazardBlock {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 10, 12);

    public SmolderingPlanterBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.015, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You dug out the smoldering butt and watered the peat down.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The smoldering peat finally catches, and the planter goes up!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
