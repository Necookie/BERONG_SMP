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
 * change) and draws a small triangular needle at the top-center of the screen every render frame,
 * rotated purely from the local player's own exact position/view angle — smooth at full framerate
 * with zero per-frame network traffic and no particle spam.
 *
 * <p>The triangle is rasterized by hand with {@link GuiGraphicsExtractor#fill} scanlines rather
 * than a texture blit or a rotated pose-stack draw, since this MC branch's GUI pipeline
 * ({@code GuiGraphicsExtractor}, not the older {@code GuiGraphics}) routes textured blits through
 * {@code RenderPipeline}/{@code TextureSetup} objects that would need a matching sprite asset —
 * plain integer fills need none of that and are already proven elsewhere in this HUD family
 * (see {@link AcademyHud}'s caption box).
 */
public final class AcademyCompassHud {

    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_compass");

    private static final int ARROW_COLOR = 0xFF55FF6A;
    private static final int ARROW_OUTLINE_COLOR = 0x80000000;
    private static final int ARROW_HALF_HEIGHT = 8;
    private static final int ARROW_HALF_WIDTH = 6;
    private static final int SCREEN_TOP_MARGIN = 30;
    /** Below this squared XZ distance to the target, hide the arrow — nothing left to point at. */
    private static final double ARRIVED_DISTANCE_SQ = 1.5 * 1.5;

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

        // Local "points up" arrowhead (tip, back-left, back-right), rotated by `theta` around the
        // origin and translated to the fixed screen anchor.
        float[] localX = {0f, -ARROW_HALF_WIDTH, ARROW_HALF_WIDTH};
        float[] localY = {-ARROW_HALF_HEIGHT, ARROW_HALF_HEIGHT, ARROW_HALF_HEIGHT};
        float[] screenX = new float[3];
        float[] screenY = new float[3];
        for (int i = 0; i < 3; i++) {
            screenX[i] = (float) (localX[i] * cos - localY[i] * sin) + centerX;
            screenY[i] = (float) (localX[i] * sin + localY[i] * cos) + centerY;
        }

        fillTriangle(guiGraphics, scaled(screenX, screenY, 1.25f, centerX, centerY), ARROW_OUTLINE_COLOR);
        fillTriangle(guiGraphics, new float[][]{screenX, screenY}, ARROW_COLOR);
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
