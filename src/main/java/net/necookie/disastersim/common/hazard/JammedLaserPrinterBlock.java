package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Faculty-room laser printer with paper jammed against the fuser unit; smokes heavily when hazardous. */
public class JammedLaserPrinterBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(2, 0, 2, 14, 10, 14);
    private static final VoxelShape SHAPE_EW = box(2, 0, 2, 14, 10, 14);

    public JammedLaserPrinterBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
        double y = pos.getY() + 0.55 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 0, 0.02, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 400;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You cleared the paper jam before the fuser could ignite it.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The jammed paper against the printer's hot fuser bursts into flame!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
