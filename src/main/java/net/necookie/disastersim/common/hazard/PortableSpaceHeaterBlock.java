package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Conference-room electric space heater tipped against a stack of papers; FACING. */
public class PortableSpaceHeaterBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 10, 13);

    public PortableSpaceHeaterBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.02, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 300;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You unplugged the heater and moved the papers clear of the grille.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The space heater ignites the papers left tipped against its grille!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
