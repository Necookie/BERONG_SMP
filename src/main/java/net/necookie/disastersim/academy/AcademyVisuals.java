package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Shared visual helpers for the Academy: a per-block green highlight (Sgt. Santos's safe-zone
 * table row) and world-space waypoint arrows ("follow the green floor arrows" — several dialogue
 * lines say this literally). Both extend {@code world/AssemblyZone.spawnBorderParticles}'s
 * periodic particle-loop idiom (green {@code HAPPY_VILLAGER} particles, matching the mod's
 * existing "green = safe/target" visual language) — there's no glow/highlight/waypoint system
 * anywhere else in the mod to build on.
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

    /** Furthest a waypoint arrow trail is drawn — keeps particle count sane for long distances. */
    private static final double MAX_WAYPOINT_DRAW_DISTANCE = 12.0;

    /**
     * Draws a dashed trail of green particles from {@code from} toward {@code to}, capped at
     * {@link #MAX_WAYPOINT_DRAW_DISTANCE}, with a small chevron ("&gt;") at the far end pointing
     * onward. Call periodically (e.g. every 10 ticks) while guiding a player toward their next
     * objective — does nothing if they're already essentially there.
     */
    public static void spawnWaypointArrow(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist < 1.0) return;
        Vec3 dir = diff.scale(1.0 / dist);
        double drawDist = Math.min(dist, MAX_WAYPOINT_DRAW_DISTANCE);

        for (double d = 1.0; d < drawDist; d += 1.0) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 0.1, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        Vec3 tip = from.add(dir.scale(drawDist));
        double angle = Math.atan2(dir.z, dir.x);
        double left = angle + Math.toRadians(150);
        double right = angle - Math.toRadians(150);
        for (double t = 0.25; t <= 1.0; t += 0.25) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    tip.x + Math.cos(left) * t, tip.y + 0.1, tip.z + Math.sin(left) * t, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    tip.x + Math.cos(right) * t, tip.y + 0.1, tip.z + Math.sin(right) * t, 1, 0.0, 0.0, 0.0, 0.0);
        }
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
