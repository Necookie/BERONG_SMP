package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Baseboard cable run gnawed to bare copper behind the cabinets, arcing hidden from view.
 * Distinct from {@code frayed_console_wire} (worn insulation on an open floor cord, not a hidden
 * rodent-chewed baseboard run). FACING.
 */
public class RodentChewedWiringBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NORTH = box(2, 0, 15, 14, 2, 16);
    private static final VoxelShape SHAPE_SOUTH = box(2, 0, 0,  14, 2, 1);
    private static final VoxelShape SHAPE_EAST  = box(0, 0, 2,  1,  2, 14);
    private static final VoxelShape SHAPE_WEST  = box(15, 0, 2, 16, 2, 14);

    public RodentChewedWiringBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(3) != 0) return;
        double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
        double y = pos.getY() + 0.15;
        double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.008, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 350;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You spliced and re-insulated the chewed section.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ The gnawed wiring arcs through the baseboard and ignites it!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
