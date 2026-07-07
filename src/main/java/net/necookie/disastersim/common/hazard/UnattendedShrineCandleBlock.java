package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lobby shrine candle burning unattended next to an altar cloth; the fastest-failing hazard in the set. */
public class UnattendedShrineCandleBlock extends HazardBlock {

    private static final VoxelShape SHAPE = box(5, 0, 5, 11, 10, 11);

    public UnattendedShrineCandleBlock(Properties props) { super(props); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.45 + rand.nextDouble() * 0.1;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.45 + rand.nextDouble() * 0.1;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 250;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You snuffed the candle — no open flames near the altar cloth.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The unattended shrine candle tips onto the altar cloth — it catches instantly!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
