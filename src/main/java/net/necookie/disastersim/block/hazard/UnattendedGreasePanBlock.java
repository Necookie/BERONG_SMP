package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UnattendedGreasePanBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 10, 15);

    @Override
    protected VoxelShape shapeFor(Direction facing) { return SHAPE; }

    public UnattendedGreasePanBlock(Properties props) { super(props); }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        if (rand.nextInt(2) == 0) level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.03, 0);
        if (rand.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 0, 0.04, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 150;
    }

    @Override
    public String failureMessage() {
        return "§4🍳 The unattended oil pan erupts into a Class F/K grease fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 2);
    }
}
