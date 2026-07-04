package net.necookie.disastersim.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client-rendered compass-style HUD arrow for the Academy's waypoint navigation ("follow the
 * green floor arrows", said literally in several dialogue lines). Replaces the earlier server-side
 * particle arrow ({@code AcademyVisuals.spawnCompassArrow}) — particles read as messy/laggy at a
 * distance and can't rotate smoother than the tick rate they're spawned at. This instead syncs only
 * the target world position from the server (via {@code AcademyCompassPayload}, sent just once per
 * change) and draws a concave 4-point dart needle over a fixed circular backdrop plate at the
 * top-center of the screen every render frame, rotated purely from the local player's own exact
 * position/view angle — smooth at full framerate with zero per-frame network traffic and no
 * particle spam. The backdrop plate keeps the needle legible over any world background; the dart
 * shape (vs. a plain triangle) reads as a distinct, purpose-built compass needle rather than an
 * arbitrary colored wedge.
 *
 * <p>Both the backdrop and the needle are rasterized by hand with {@link GuiGraphicsExtractor#fill}
 * scanlines rather than a texture blit or a rotated pose-stack draw, since this MC branch's GUI
 * pipeline ({@code GuiGraphicsExtractor}, not the older {@code GuiGraphics}) routes textured blits
 * through {@code RenderPipeline}/{@code TextureSetup} objects that would need a matching sprite
 * asset — plain integer fills need none of that and are already proven elsewhere in this HUD
 * family (see {@link AcademyHud}'s caption box).
 */
public final class AcademyCompassHud {

    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_compass");

    private static final int NEEDLE_COLOR = 0xFF4CFF7A;
    private static final int NEEDLE_OUTLINE_COLOR = 0xE00B2412;
    private static final int BACKDROP_COLOR = 0x8C101010;
    private static final int BACKDROP_RING_COLOR = 0x704CFF7A;
    private static final int BACKDROP_RADIUS = 13;
    private static final int SCREEN_TOP_MARGIN = 32;
    /** Below this squared XZ distance to the target, hide the arrow — nothing left to point at. */
    private static final double ARRIVED_DISTANCE_SQ = 1.5 * 1.5;

    // Local-space "dart" outline (tip, back-left, inner notch, back-right), pointing straight up
    // at zero rotation — a concave 4-point dart reads as a much more distinct, purpose-built arrow
    // than a plain triangle, closer to a real compass needle/waypoint marker.
    private static final float[] DART_X = {0f, -7f, 0f, 7f};
    private static final float[] DART_Y = {-9f, 8f, 3f, 8f};

    /** Synced from the server via {@code AcademyCompassPayload}; false hides the compass entirely. */
    public static boolean active = false;
    public static double targetX, targetY, targetZ;

    private AcademyCompassHud() {}

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, AcademyCompassHud::render);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        if (dx * dx + dz * dz < ARRIVED_DISTANCE_SQ) return;

        float bearingYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float playerYaw = player.getViewYRot(deltaTracker.getGameTimeDeltaPartialTick(true));
        double theta = Math.toRadians(wrapDegrees(bearingYaw - playerYaw));
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = SCREEN_TOP_MARGIN;

        // Fixed backdrop plate (does not rotate) — keeps the needle legible against any background,
        // world scenery included, instead of a bare colored shape floating in open air.
        fillCircle(guiGraphics, centerX, centerY, BACKDROP_RADIUS, BACKDROP_COLOR);
        fillRing(guiGraphics, centerX, centerY, BACKDROP_RADIUS, BACKDROP_RING_COLOR);

        float[] screenX = new float[DART_X.length];
        float[] screenY = new float[DART_Y.length];
        for (int i = 0; i < DART_X.length; i++) {
            screenX[i] = (float) (DART_X[i] * cos - DART_Y[i] * sin) + centerX;
            screenY[i] = (float) (DART_X[i] * sin + DART_Y[i] * cos) + centerY;
        }

        // Dark outline first (slightly larger copy), then the bright fill on top — gives the dart a
        // crisp edge instead of blending into the backdrop plate.
        float[][] outline = scaled(screenX, screenY, 1.2f, centerX, centerY);
        fillDart(guiGraphics, outline[0], outline[1], NEEDLE_OUTLINE_COLOR);
        fillDart(guiGraphics, screenX, screenY, NEEDLE_COLOR);
    }

    /** Draws the concave 4-point dart as two triangles sharing the tip and inner notch. */
    private static void fillDart(GuiGraphicsExtractor guiGraphics, float[] xs, float[] ys, int color) {
        fillTriangle(guiGraphics, new float[][]{
                {xs[0], xs[1], xs[2]}, {ys[0], ys[1], ys[2]}}, color); // tip, back-left, notch
        fillTriangle(guiGraphics, new float[][]{
                {xs[0], xs[2], xs[3]}, {ys[0], ys[2], ys[3]}}, color); // tip, notch, back-right
    }

    private static float[][] scaled(float[] xs, float[] ys, float factor, int centerX, int centerY) {
        float[] ox = new float[xs.length];
        float[] oy = new float[ys.length];
        for (int i = 0; i < xs.length; i++) {
            ox[i] = centerX + (xs[i] - centerX) * factor;
            oy[i] = centerY + (ys[i] - centerY) * factor;
        }
        return new float[][]{ox, oy};
    }

    private static void fillCircle(GuiGraphicsExtractor guiGraphics, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.round(Math.sqrt((double) (radius * radius - y * y)));
            guiGraphics.fill(cx - halfWidth, cy + y, cx + halfWidth + 1, cy + y + 1, color);
        }
    }

    /** A thin 1px bright ring traced around the backdrop circle's edge for a "compass rose" feel. */
    private static void fillRing(GuiGraphicsExtractor guiGraphics, int cx, int cy, int radius, int color) {
        int steps = 48;
        for (int i = 0; i < steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static float wrapDegrees(float deg) {
        float d = deg % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    /** Simple scanline rasterizer — avoids depending on any texture/blit or pose-rotation API. */
    private static void fillTriangle(GuiGraphicsExtractor guiGraphics, float[][] triangle, int color) {
        float[] xs = triangle[0];
        float[] ys = triangle[1];
        int minY = Math.round(Math.min(ys[0], Math.min(ys[1], ys[2])));
        int maxY = Math.round(Math.max(ys[0], Math.max(ys[1], ys[2])));
        for (int y = minY; y <= maxY; y++) {
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            for (int e = 0; e < 3; e++) {
                int n = (e + 1) % 3;
                float y0 = ys[e], y1 = ys[n];
                if (y0 == y1) continue;
                if ((y >= y0 && y < y1) || (y >= y1 && y < y0)) {
                    float t = (y - y0) / (y1 - y0);
                    float x = xs[e] + t * (xs[n] - xs[e]);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
            if (minX <= maxX) {
                guiGraphics.fill(Math.round(minX), y, Math.round(maxX) + 1, y + 1, color);
            }
        }
    }
}
