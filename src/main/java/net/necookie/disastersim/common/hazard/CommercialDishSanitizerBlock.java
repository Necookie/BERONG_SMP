package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Under-counter high-temp dish sanitizer whose booster heating element boils dry while door-seal steam seeps into the control board. */
public class CommercialDishSanitizerBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 14, 16);

    public CommercialDishSanitizerBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, 0, 0.01, 0);
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
        return "§a✔ Prevented! You killed power and topped up the wash tank.";
    }

    @Override
    public String failureMessage() {
        return "§c⚡ Steam seeping into the dish sanitizer's control board sparks a fire!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
