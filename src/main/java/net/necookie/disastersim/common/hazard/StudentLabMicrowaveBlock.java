package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared student-bench countertop microwave; metal cutlery/foil left inside arcs violently.
 * Distinct from {@link OverloadedMicrowaveBlock} (faculty pantry runaway heating cycle) — this one
 * is cold-but-arcing, not glowing hot.
 */
public class StudentLabMicrowaveBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(2, 0, 3, 13, 9, 13);

    public StudentLabMicrowaveBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.3 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.01, 0.01, 0.01);
        if (rand.nextBoolean()) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You opened the door and pulled the foil out.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ Metal inside the microwave arcs violently and catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
