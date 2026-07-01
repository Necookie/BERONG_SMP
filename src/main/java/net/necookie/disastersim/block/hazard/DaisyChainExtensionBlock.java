package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three extension cords daisy-chained together — electric sparks when overloaded=true. */
public class DaisyChainExtensionBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = Block.box(1, 0, 5, 15, 5, 11);
    private static final VoxelShape SHAPE_EW = Block.box(5, 0, 1, 11, 5, 15);

    public DaisyChainExtensionBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.1 + rand.nextDouble() * 0.8;
        double y = pos.getY() + 0.2 + rand.nextDouble() * 0.3;
        double z = pos.getZ() + 0.1 + rand.nextDouble() * 0.8;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.05, 0.0);
    }
}
