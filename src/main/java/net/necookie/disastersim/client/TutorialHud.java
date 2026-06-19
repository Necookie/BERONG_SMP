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

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int textWidth    = font.width(prompt);
        int xPos         = (screenWidth - textWidth) / 2;
        int yPos         = mc.getWindow().getGuiScaledHeight() - 80;

        guiGraphics.fill(xPos - 4, yPos - 4, xPos + textWidth + 4, yPos + 14, 0xB0000000);
        guiGraphics.text(font, prompt, xPos, yPos, 0xFFFFFF00, true);
    }

    /** Applies camera shake for QUAKE tutorial stages — same multi-layer approach as SimulationHud. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (intensity <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        long time = (mc.level != null) ? mc.level.getGameTime() : System.currentTimeMillis() / 50L;
        float rumbleSlow = (float) Math.sin(time * 0.314) * intensity * 1.4f;
        float rumbleMid  = (float) Math.sin(time * 0.942) * intensity * 0.7f;
        float jitterYaw   = ((float) Math.random() * 2f - 1f) * intensity * 1.0f;
        float jitterPitch = ((float) Math.random() * 2f - 1f) * intensity * 0.7f;
        float roll = (float) Math.sin(time * 0.628) * intensity * 0.9f
                   + ((float) Math.random() * 2f - 1f) * intensity * 0.4f;
        event.setYaw((float)   event.getYaw()   + rumbleSlow + rumbleMid + jitterYaw);
        event.setPitch((float) event.getPitch() + jitterPitch);
        event.setRoll((float)  event.getRoll()  + roll);
    }
}
