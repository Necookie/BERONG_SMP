package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** FDM 3D printer whose heater cartridge dislodges mid-print, filament smoking on the bed; FACING. */
public class Runaway3dPrinterBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 12, 14);

    public Runaway3dPrinterBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        if (rand.nextBoolean()) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
        }
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You reseated the heater cartridge and stopped the print.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The dislodged heater cartridge sets the filament and bed alight!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
