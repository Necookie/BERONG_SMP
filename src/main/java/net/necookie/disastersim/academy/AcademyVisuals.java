package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

/**
 * Shared visual helpers for the Academy — currently just a per-block green highlight (Sgt.
 * Santos's safe-zone table row); world-space waypoint arrows are added alongside it once all 4
 * rooms exist. Both extend {@code world/AssemblyZone.spawnBorderParticles}'s periodic
 * particle-loop idiom (green {@code HAPPY_VILLAGER} particles, matching the mod's existing
 * "green = safe/target" visual language) down to a single block instead of a whole zone perimeter
 * — there's no glow/highlight system anywhere else in the mod to build on.
 */
public final class AcademyVisuals {

    private AcademyVisuals() {}

    /** Traces a green wireframe outline around each block in {@code positions}. Call periodically (e.g. every 5 ticks). */
    public static void highlightBlocks(ServerLevel level, Collection<BlockPos> positions) {
        for (BlockPos pos : positions) {
            highlightBlock(level, pos);
        }
    }

    private static void highlightBlock(ServerLevel level, BlockPos pos) {
        double x0 = pos.getX(), x1 = pos.getX() + 1;
        double y0 = pos.getY(), y1 = pos.getY() + 1;
        double z0 = pos.getZ(), z1 = pos.getZ() + 1;

        // Bottom perimeter
        edge(level, x0, y0, z0, x1, y0, z0);
        edge(level, x1, y0, z0, x1, y0, z1);
        edge(level, x1, y0, z1, x0, y0, z1);
        edge(level, x0, y0, z1, x0, y0, z0);
        // Top perimeter
        edge(level, x0, y1, z0, x1, y1, z0);
        edge(level, x1, y1, z0, x1, y1, z1);
        edge(level, x1, y1, z1, x0, y1, z1);
        edge(level, x0, y1, z1, x0, y1, z0);
        // Vertical corners
        edge(level, x0, y0, z0, x0, y1, z0);
        edge(level, x1, y0, z0, x1, y1, z0);
        edge(level, x1, y0, z1, x1, y1, z1);
        edge(level, x0, y0, z1, x0, y1, z1);
    }

    private static void edge(ServerLevel level, double x0, double y0, double z0, double x1, double y1, double z1) {
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
