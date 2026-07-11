package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted UV knife/utensil sterilizing cabinet whose UV-lamp ballast overheats and scorches the housing. */
public class KnifeSterilizerCabinetBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(2, 4, 13, 14, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = box(2, 4, 0,  14, 12, 3);
    private static final VoxelShape SHAPE_EAST  = box(0, 4, 2,  3,  12, 14);
    private static final VoxelShape SHAPE_WEST  = box(13, 4, 2, 16, 12, 14);

    public KnifeSterilizerCabinetBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(2) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.5 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You switched off the lamp and let it cool.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The sterilizer's overheated UV ballast scorches its housing alight!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
