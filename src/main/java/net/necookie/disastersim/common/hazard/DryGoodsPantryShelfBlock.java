package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pantry shelf of open flour/sugar sacks beside a hot light-fixture ballast — an airborne flour-dust deflagration hazard. */
public class DryGoodsPantryShelfBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = box(0, 0, 0,  16, 16, 4);
    private static final VoxelShape SHAPE_EAST  = box(0, 0, 0,  4,  16, 16);
    private static final VoxelShape SHAPE_WEST  = box(12, 0, 0, 16, 16, 16);

    public DryGoodsPantryShelfBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.4 + rand.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.3, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You sealed the sacks and moved them from the hot fixture.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Airborne flour dust finds the hot ballast and flashes over!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteRadius(level, pos, 2, 2);
    }
}
