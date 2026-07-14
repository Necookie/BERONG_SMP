package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

/**
 * Data-driven description of a "standard skeleton" hazard prop: everything {@link HazardBlock}/
 * {@link HazardFacingBlock}'s five overridable hooks (shape, particles, failure delay, prevent/
 * failure messages, failure action) need, without a dedicated {@code Block} subclass per prop.
 * {@link SimpleHazardBlock} (no facing) and {@link SimpleHazardFacingBlock} (FACING) delegate to
 * an instance of this record — see {@link HazardSpecs} for the migrated-prop table.
 *
 * <p>Only props matching the standard skeleton belong here — anything with extra block state
 * (e.g. {@code woodshop_sawdust_layer}'s {@code ACCUMULATION}) or a non-standard collision idiom
 * (e.g. {@code rice_cooker_bank}) keeps its own dedicated class. This dedup was scoped as a
 * proof-of-concept over 8 representative props (2026-07-14) — most of the mod's 85 hazard props
 * still have dedicated classes; migrating the rest is documented backlog, not attempted here.
 */
public record HazardSpec(
        Function<Direction, VoxelShape> shapeFor,
        ParticleEmitter particleEmitter,
        int failureDelayTicks,
        String preventMessage,
        String failureMessage,
        FailureAction failureAction
) {

    /** Mirrors {@code HazardBlock#spawnHazardParticles}'s signature exactly. */
    @FunctionalInterface
    public interface ParticleEmitter {
        void emit(Level level, BlockPos pos, BlockState state, RandomSource rand);
    }

    /** Mirrors {@code HazardBlock#onHazardFailure}'s signature exactly. */
    @FunctionalInterface
    public interface FailureAction {
        void apply(Level level, BlockPos pos, BlockState state);
    }

    /** A single fixed shape regardless of facing — also used by {@link SimpleHazardBlock}, which has no facing to vary on. */
    public static Function<Direction, VoxelShape> fixedShape(VoxelShape shape) {
        return facing -> shape;
    }

    /** North/south share one shape, east/west share another (a prop that's visually symmetric on one axis). */
    public static Function<Direction, VoxelShape> shapeNsEw(VoxelShape ns, VoxelShape ew) {
        return facing -> (facing == Direction.NORTH || facing == Direction.SOUTH) ? ns : ew;
    }

    /** Four independent shapes, one per cardinal facing — same mapping as {@code HazardFacingBlock#byFacing}. */
    public static Function<Direction, VoxelShape> shape4Way(VoxelShape north, VoxelShape south,
                                                              VoxelShape east, VoxelShape west) {
        return facing -> switch (facing) {
            case SOUTH -> south;
            case EAST -> east;
            case WEST -> west;
            default -> north;
        };
    }
}
