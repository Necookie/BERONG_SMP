package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tall reach-in culinary-lab refrigerator; dust-caked condenser coils and a failing start-relay overheat the compressor. */
public class CulinaryFridgeBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    public CulinaryFridgeBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.1 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        if (rand.nextInt(3) != 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        } else {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 450;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the fridge and cleared the dust from the coils.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ The fridge's overheated compressor relay sparks and catches fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
