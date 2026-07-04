package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.network.AcademyCompassPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared visual helpers for the Academy: a per-block green highlight (Sgt. Santos's safe-zone
 * table row, Officer Cruz's green floor marks — still particle-based, see {@link #highlightBlocks})
 * and a compass-style waypoint arrow ("follow the green floor arrows" — several dialogue lines say
 * this literally). The highlight extends {@code world/AssemblyZone.spawnBorderParticles}'s periodic
 * particle-loop idiom (green {@code HAPPY_VILLAGER} particles). The compass arrow used to be
 * particles too, but those read as messy/laggy and can't rotate faster than the tick rate they're
 * spawned at — it's now a client-rendered HUD needle ({@code client.AcademyCompassHud}) driven by
 * {@link AcademyCompassPayload}; {@link #setCompassTarget} only sends a packet when the target
 * actually changes, since the needle's rotation itself is recomputed client-side every render
 * frame from the local player's own position/view angle.
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

    /** Last target actually sent to each player; used to dedupe {@link #setCompassTarget} calls. */
    private static final Map<UUID, Vec3> lastSentTarget = new ConcurrentHashMap<>();
    /** Skip re-sending when the target hasn't moved more than this since the last packet. */
    private static final double RESEND_EPSILON_SQ = 0.25 * 0.25;

    /**
     * Shows (or updates) the client-rendered compass needle pointing toward {@code target}, or
     * hides it when {@code target} is {@code null}. Safe to call every tick from whichever room is
     * guiding the player — a network packet is only actually sent when the shown/hidden state or
     * the target position changes, since {@code AcademyCompassHud} recomputes the needle's screen
     * rotation itself every render frame and doesn't need a fresh packet to stay smooth.
     */
    public static void setCompassTarget(ServerPlayer player, Vec3 target) {
        UUID id = player.getUUID();
        Vec3 last = lastSentTarget.get(id);

        if (target == null) {
            if (last == null) return;
            lastSentTarget.remove(id);
            PacketDistributor.sendToPlayer(player, new AcademyCompassPayload(false, 0, 0, 0));
            return;
        }

        if (last != null && last.distanceToSqr(target) < RESEND_EPSILON_SQ) return;
        lastSentTarget.put(id, target);
        PacketDistributor.sendToPlayer(player, new AcademyCompassPayload(true, target.x, target.y, target.z));
    }

    /** Called from {@code AcademyManager}'s logout handler to drop this player's dedupe cache. */
    public static void clearPlayer(UUID id) {
        lastSentTarget.remove(id);
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
