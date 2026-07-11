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

/** Row of student rice cookers boiled dry; the exposed heating element scorches the plastic base. */
public class RiceCookerBankBlock extends HazardBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 7, 15);

    public RiceCookerBankBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
        double y = pos.getY() + 0.35 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the bank and added water.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ A dry-fired rice cooker scorches its base and catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
