package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Plastic waste bin overflowing with paper towels and a smoldering vape battery (has_vape state). */
public class PlasticTrashBinBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public PlasticTrashBinBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE; // cylindrical — same bounding box from all 4 directions
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
        double y = pos.getY() + 0.85 + rand.nextDouble() * 0.15;
        double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.03, 0.0);
    }

    @Override
    public String failureMessage() {
        return "§c🔥 The overfilled trash bin's smoldering battery bursts into a Class A fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
