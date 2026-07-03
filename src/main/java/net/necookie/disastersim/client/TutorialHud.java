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

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side HUD for tutorial prompts and QUAKE-stage camera shake.
 * Hidden whenever the simulation HUD is active, so the two overlays never stack.
 */
public class TutorialHud {

    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "tutorial_hud");

    /** Current instruction text synced from the server; empty string hides the HUD. */
    public static String prompt = "";

    /** Camera shake amplitude for QUAKE tutorial stages; 0 disables shake. */
    public static float intensity = 0f;

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, TutorialHud::render);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (prompt.isEmpty()) return;
        // Don't overlap with the active simulation HUD
        if (!SimulationHud.currentStatus.isEmpty() || SimulationHud.timeLeft > 0) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Word-wrap to at most 70% of screen width (max 460 px) so text never overflows
        int maxWidth = Math.min((int)(screenW * 0.70f), 460);
        List<String> lines = wrapText(font, prompt, maxWidth);

        int lineH   = font.lineHeight + 2;
        int totalH  = lines.size() * lineH - 2;

        // Measure the widest rendered line for the background box
        int boxW = 0;
        for (String line : lines) boxW = Math.max(boxW, font.width(line));

        // Anchor: bottom of text block sits at screenH - 72; grows upward for multi-line
        int yStart = screenH - 72 - totalH;
        int xBox   = (screenW - boxW) / 2;

        guiGraphics.fill(xBox - 4, yStart - 4, xBox + boxW + 4, yStart + totalH + 4, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int xLine = (screenW - font.width(line)) / 2;
            guiGraphics.text(font, line, xLine, yStart + i * lineH, 0xFFFFFFFF, true);
        }
    }

    /**
     * Splits {@code text} (which may contain §-codes) into lines that fit within {@code maxWidth}.
     * Package-visible so {@link AcademyHud} can reuse it instead of duplicating the wrap logic.
     */
    static List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (font.width(candidate) <= maxWidth) {
                if (current.length() > 0) current.append(' ');
                current.append(word);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines.isEmpty() ? List.of(text) : lines;
    }

    /** Applies camera shake for QUAKE tutorial stages — shared with SimulationHud via CameraShake. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShake.apply(event, intensity);
    }
}
