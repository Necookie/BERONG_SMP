package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
 * table row, Officer Cruz's green floor marks — see {@link #highlightBlocks}) and a compass-style
 * waypoint arrow ("follow the green floor arrows" — several dialogue lines say this literally, see
 * {@code client.AcademyCompassHud}). Both used to be traced with {@code ParticleTypes.HAPPY_VILLAGER}
 * (the same idiom {@code world/AssemblyZone.spawnBorderParticles} still uses), which reads as a
 * chaotic swirl of little green notes rather than a clean highlight — {@link #highlightBlocks} now
 * uses {@link DustParticleOptions} instead (the same jitter-free, custom-RGB particle
 * {@code WetChemicalExtinguisherItem}'s foam mist already relies on), which holds still and renders
 * as a crisp glowing dot, plus a soft top-face glow so a highlighted block reads as a lit-up marker
 * tile rather than just an empty wireframe box. The compass needle moved further, off particles
 * entirely, onto a client-rendered HUD ({@code client.AcademyCompassHud}) driven by
 * {@link AcademyCompassPayload}; {@link #setCompassTarget} only sends a packet when the target
 * actually changes, since the needle's rotation itself is recomputed client-side every render
 * frame from the local player's own position/view angle.
 */
public final class AcademyVisuals {

    /** Default highlight color — a clean, saturated "safe/target" green, chosen for contrast against most floors/walls. */
    public static final int DEFAULT_HIGHLIGHT_COLOR = 0x3CFF6E;
    private static final float HIGHLIGHT_EDGE_SCALE = 1.15f;
    private static final float HIGHLIGHT_GLOW_SCALE = 0.9f;

    private AcademyVisuals() {}

    /**
     * Traces a clean green wireframe + soft top-face glow around each block in {@code positions}
     * using the default color. Call periodically (e.g. every 5 ticks).
     */
    public static void highlightBlocks(ServerLevel level, Collection<BlockPos> positions) {
        highlightBlocks(level, positions, DEFAULT_HIGHLIGHT_COLOR);
    }

    /**
     * Same as {@link #highlightBlocks(ServerLevel, Collection)} but with a custom RGB color —
     * kept as a separate overload so future features (a red danger marker, a yellow caution zone,
     * ...) can reuse this exact drawing routine without copying it.
     */
    public static void highlightBlocks(ServerLevel level, Collection<BlockPos> positions, int color) {
        for (BlockPos pos : positions) {
            highlightBlock(level, pos, color);
        }
    }

    private static void highlightBlock(ServerLevel level, BlockPos pos, int color) {
        double x0 = pos.getX(), x1 = pos.getX() + 1;
        double y0 = pos.getY(), y1 = pos.getY() + 1;
        double z0 = pos.getZ(), z1 = pos.getZ() + 1;

        // Bottom perimeter
        edge(level, x0, y0, z0, x1, y0, z0, color);
        edge(level, x1, y0, z0, x1, y0, z1, color);
        edge(level, x1, y0, z1, x0, y0, z1, color);
        edge(level, x0, y0, z1, x0, y0, z0, color);
        // Top perimeter
        edge(level, x0, y1, z0, x1, y1, z0, color);
        edge(level, x1, y1, z0, x1, y1, z1, color);
        edge(level, x1, y1, z1, x0, y1, z1, color);
        edge(level, x0, y1, z1, x0, y1, z0, color);
        // Vertical corners
        edge(level, x0, y0, z0, x0, y1, z0, color);
        edge(level, x1, y0, z0, x1, y1, z0, color);
        edge(level, x1, y0, z1, x1, y1, z1, color);
        edge(level, x0, y0, z1, x0, y1, z1, color);
        // Soft glow sprinkled across the top face — reads as a lit-up marker tile, not an empty box.
        glowTop(level, pos, color);
    }

    private static void glowTop(ServerLevel level, BlockPos pos, int color) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.02, cz = pos.getZ() + 0.5;
        level.sendParticles(new DustParticleOptions(color, HIGHLIGHT_GLOW_SCALE),
                cx, cy, cz, 3, 0.32, 0.02, 0.32, 0.0);
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

    private static void edge(ServerLevel level, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(new DustParticleOptions(color, HIGHLIGHT_EDGE_SCALE),
                    x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
