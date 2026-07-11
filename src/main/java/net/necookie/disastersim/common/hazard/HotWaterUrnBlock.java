package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tall tea/coffee boiler urn whose spigot drips water down onto the live base electrics. */
public class HotWaterUrnBlock extends HazardBlock {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 13, 12);

    public HotWaterUrnBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.15 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.5, z, 0, -0.02, 0);
        } else {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the urn and wiped the base dry.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ Water dripping onto the urn's base electrics sparks a fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
