package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Shared visual helpers for the Academy: a per-block green highlight (Sgt. Santos's safe-zone
 * table row, Officer Cruz's green floor marks) and a compass-style waypoint arrow ("follow the
 * green floor arrows" — several dialogue lines say this literally). Both extend
 * {@code world/AssemblyZone.spawnBorderParticles}'s periodic particle-loop idiom (green
 * {@code HAPPY_VILLAGER} particles, matching the mod's existing "green = safe/target" visual
 * language) — there's no glow/highlight/waypoint system anywhere else in the mod to build on.
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

    /** How far ahead of the player the compass arrow floats. */
    private static final double ARROW_DISTANCE = 1.8;
    private static final double ARROW_SEGMENT_LENGTH = 0.35;

    /**
     * Floats a small green arrowhead ~{@link #ARROW_DISTANCE} blocks in front of the player,
     * oriented toward {@code target} — a compass, not a breadcrumb trail: call this **every tick**
     * (20 Hz) from whichever room is guiding the player, so it stays smoothly oriented as they move
     * and turn, unlike the old dashed-trail-from-a-fixed-point approach it replaces. Does nothing
     * if the player is already essentially at the target.
     */
    public static void spawnCompassArrow(ServerLevel level, ServerPlayer player, Vec3 target) {
        Vec3 eye = player.position().add(0, 1.2, 0);
        Vec3 diff = target.subtract(eye);
        double dist = diff.length();
        if (dist < 1.5) return;

        Vec3 dir = diff.scale(1.0 / dist);
        Vec3 center = eye.add(dir.scale(ARROW_DISTANCE));

        double angle = Math.atan2(dir.z, dir.x);
        double backLeft = angle + Math.toRadians(150);
        double backRight = angle - Math.toRadians(150);

        Vec3 tail = center.subtract(dir.scale(ARROW_SEGMENT_LENGTH));
        Vec3 tip = center.add(dir.scale(ARROW_SEGMENT_LENGTH));
        Vec3 wingLeft = tip.add(Math.cos(backLeft) * ARROW_SEGMENT_LENGTH, 0, Math.sin(backLeft) * ARROW_SEGMENT_LENGTH);
        Vec3 wingRight = tip.add(Math.cos(backRight) * ARROW_SEGMENT_LENGTH, 0, Math.sin(backRight) * ARROW_SEGMENT_LENGTH);

        spawnPoint(level, tail);
        spawnPoint(level, center);
        spawnPoint(level, tip);
        spawnPoint(level, wingLeft);
        spawnPoint(level, wingRight);
        spawnPoint(level, tip.add(wingLeft).scale(0.5));
        spawnPoint(level, tip.add(wingRight).scale(0.5));
    }

    private static void spawnPoint(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
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
