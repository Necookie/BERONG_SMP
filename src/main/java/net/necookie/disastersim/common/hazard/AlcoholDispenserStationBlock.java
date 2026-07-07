package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hallway alcohol-sanitizer stand leaking onto the floor beside a power outlet; faint blue vapor wisps when hazardous. */
public class AlcoholDispenserStationBlock extends HazardFacingBlock {

    private static final VoxelShape SHAPE_NS = box(5, 0, 5, 11, 14, 11);
    private static final VoxelShape SHAPE_EW = box(5, 0, 5, 11, 14, 11);

    public AlcoholDispenserStationBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
        double y = pos.getY() + 0.2 + rand.nextDouble() * 0.4;
        double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.008, 0);
    }

    @Override
    public int failureDelayTicks() {
        return 500;
    }

    @Override
    public String preventMessage() {
        return "§a✔ Prevented! You moved the leaking sanitizer bottle away from the outlet.";
    }

    @Override
    public String failureMessage() {
        return "§c⚠ Pooled alcohol sanitizer ignites in a ghostly blue flash!";
    }

    @Override
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }
}
