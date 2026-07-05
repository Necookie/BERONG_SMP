package net.necookie.disastersim.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Client-side HUD for the new tutorial building ("the Academy")'s NPC dialogue, phase captions,
 * and Sgt. Santos's earthquake-drill camera shake. Pattern-cloned from {@link TutorialHud} (own
 * static fields, own GUI layer) rather than reused, since the Academy is an independent system —
 * hidden whenever {@link SimulationHud} or {@link TutorialHud} is active, so at most one caption
 * overlay ever shows at once.
 */
public class AcademyHud {

    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_hud");

    /** Current instruction/dialogue text synced from the server; empty string hides the HUD. */
    public static String prompt = "";

    /** Camera shake amplitude for the earthquake drill; 0 disables shake. */
    public static float intensity = 0f;

    /**
     * Wall-clock millis (System.currentTimeMillis()) at which {@link #prompt} should auto-clear;
     * 0 means "no expiry, persists until overwritten" — see {@code AcademyStatusPayload}. Without
     * this, every non-dialogue-sequence caption (idle nudges, phase banners, GO/STOP calls) used
     * to sit on screen forever once the player walked away, since the only place that ever sent an
     * explicit clear was a timed dialogue sequence naturally finishing.
     */
    public static long promptExpiresAtMillis = 0L;

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, AcademyHud::render);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (promptExpiresAtMillis > 0 && System.currentTimeMillis() >= promptExpiresAtMillis) {
            prompt = "";
            promptExpiresAtMillis = 0L;
        }
        if (prompt.isEmpty()) return;
        if (!SimulationHud.currentStatus.isEmpty() || SimulationHud.timeLeft > 0) return;
        if (!TutorialHud.prompt.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int maxWidth = Math.min((int) (screenW * 0.70f), 460);
        List<String> lines = TutorialHud.wrapText(font, prompt, maxWidth);

        int lineH = font.lineHeight + 2;
        int totalH = lines.size() * lineH - 2;

        int boxW = 0;
        for (String line : lines) boxW = Math.max(boxW, font.width(line));

        int yStart = screenH - 72 - totalH;
        int xBox = (screenW - boxW) / 2;

        guiGraphics.fill(xBox - 4, yStart - 4, xBox + boxW + 4, yStart + totalH + 4, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int xLine = (screenW - font.width(line)) / 2;
            guiGraphics.text(font, line, xLine, yStart + i * lineH, 0xFFFFFFFF, true);
        }
    }

    /** Applies camera shake for the earthquake drill — shared with SimulationHud/TutorialHud via CameraShake. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShake.apply(event, intensity);
    }
}
