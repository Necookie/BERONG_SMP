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
 * Compressed-oxygen cylinder with a leaking regulator seat — an <b>oxidizer-enrichment</b> hazard,
 * not a fuel-gas leak like every other gas prop in the mod. Real-world basis: dry-chemical agents
 * are not reliable against an oxidizer-fed fire, and a wet-chemical agent is doubly wrong — only
 * CO2 (dilutes/displaces the excess oxygen) may defuse this prop; see
 * {@code AbstractExtinguisherItem.OXIDIZER_HAZARD_IDS}. Symmetric, no facing.
 */
public class LeakingOxygenCylinderBlock extends HazardBlock {

    private static final VoxelShape SHAPE = box(5, 0, 5, 11, 15, 11);

    public LeakingOxygenCylinderBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You closed the cylinder valve and cleared the enriched air.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The oxygen-enriched air finds a spark — everything nearby burns hotter and faster!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
