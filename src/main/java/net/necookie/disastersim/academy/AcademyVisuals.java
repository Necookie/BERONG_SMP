package net.necookie.disastersim.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
 * Shared visual helpers for the Academy.
 *
 * <p><b>Block marker</b> ({@link #highlightBlocks}): a beginner-friendly "go here" beacon —
 * four corner posts framing the tile, a pulsing ring breathing above it, a beacon-beam column of
 * fading dust rising ~3 blocks (spottable from across a room), and a bright END_ROD sparkle at the
 * beam top. Replaced two earlier designs: a {@code HAPPY_VILLAGER} swirl (chaotic green notes) and
 * a plain 12-edge dust wireframe (clean but flat — invisible past a few blocks and easy to read as
 * decor rather than an objective). All colored elements are {@link DustParticleOptions} tinted by
 * the {@code color} parameter, so future markers (red danger, yellow caution, ...) reuse this
 * exact routine.
 *
 * <p><b>Compass needle</b> ({@link #setCompassTarget}): a client-rendered HUD needle
 * ({@code client.AcademyCompassHud}) driven by {@link AcademyCompassPayload}; only sends a packet
 * when the target actually changes, since the needle's rotation is recomputed client-side every
 * render frame from the local player's own position/view angle.
 */
public final class AcademyVisuals {

    /** Default highlight color — a clean, saturated "safe/target" green, chosen for contrast against most floors/walls. */
    public static final int DEFAULT_HIGHLIGHT_COLOR = 0x3CFF6E;

    // ── Marker geometry — corner posts + pulsing ring + beacon beam + END_ROD cap ──
    /** Corner posts sit this far inside the block edge. */
    private static final double POST_INSET = 0.1;
    private static final double POST_HEIGHT = 1.2;
    /** Dust points per corner post (16 total across the 4 posts). */
    private static final int POST_POINTS = 4;
    private static final float POST_SCALE = 1.3f;
    private static final int RING_POINTS = 8;
    private static final double RING_BASE_RADIUS = 0.45;
    private static final double RING_PULSE_AMPLITUDE = 0.15;
    /** Radians per game tick — one full breath roughly every 1.25 s. */
    private static final double RING_PULSE_SPEED = 0.25;
    private static final double RING_Y_OFFSET = 1.05;
    private static final float RING_SCALE = 1.0f;
    private static final int BEAM_POINTS = 6;
    private static final double BEAM_START_Y = 1.2;
    /** Beam top lands at y0 + 3.2 — tall enough to spot over furniture from across a room. */
    private static final double BEAM_HEIGHT = 2.0;
    private static final float BEAM_SCALE_MAX = 1.5f;
    private static final float BEAM_SCALE_MIN = 0.4f;
    private static final double CAP_Y_OFFSET = 3.3;

    private AcademyVisuals() {}

    /**
     * Draws the full marker (posts + pulsing ring + beam + cap) on each block in {@code positions}
     * using the default green. Call periodically (e.g. every 5 ticks).
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
        double x0 = pos.getX(), y0 = pos.getY(), z0 = pos.getZ();
        double cx = x0 + 0.5, cz = z0 + 0.5;
        DustParticleOptions dustPost = new DustParticleOptions(color, POST_SCALE);
        DustParticleOptions dustRing = new DustParticleOptions(color, RING_SCALE);

        // 1. Four corner posts — a readable "claimed tile" footprint even up close.
        double[][] corners = {
                {x0 + POST_INSET, z0 + POST_INSET}, {x0 + 1 - POST_INSET, z0 + POST_INSET},
                {x0 + 1 - POST_INSET, z0 + 1 - POST_INSET}, {x0 + POST_INSET, z0 + 1 - POST_INSET}};
        for (double[] c : corners) {
            for (int i = 0; i < POST_POINTS; i++) {
                double y = y0 + POST_HEIGHT * i / (POST_POINTS - 1);
                level.sendParticles(dustPost, c[0], y, c[1], 1, 0, 0, 0, 0);
            }
        }
        // 2. Pulsing top ring — the "this exact spot" cue; radius breathes with game time.
        double radius = RING_BASE_RADIUS + RING_PULSE_AMPLITUDE * Math.sin(level.getGameTime() * RING_PULSE_SPEED);
        for (int k = 0; k < RING_POINTS; k++) {
            double angle = (Math.PI * 2 / RING_POINTS) * k;
            level.sendParticles(dustRing, cx + radius * Math.cos(angle), y0 + RING_Y_OFFSET,
                    cz + radius * Math.sin(angle), 1, 0, 0, 0, 0);
        }
        // 3. Beacon-beam column — fading dust rising 2 blocks, spottable from across a room.
        for (int i = 0; i < BEAM_POINTS; i++) {
            float t = (float) i / (BEAM_POINTS - 1);
            float scale = BEAM_SCALE_MAX - (BEAM_SCALE_MAX - BEAM_SCALE_MIN) * t;
            level.sendParticles(new DustParticleOptions(color, scale),
                    cx, y0 + BEAM_START_Y + BEAM_HEIGHT * t, cz, 1, 0.02, 0, 0.02, 0);
        }
        // 4. Bright white END_ROD cap — a color-independent sparkle marking the beam top.
        level.sendParticles(ParticleTypes.END_ROD, cx, y0 + CAP_Y_OFFSET, cz, 1, 0, 0, 0, 0);
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
}
