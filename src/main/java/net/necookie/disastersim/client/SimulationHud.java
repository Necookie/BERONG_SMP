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
 * Handles the rendering of the simulation-specific HUD (Heads-Up Display).
 * Displays information like current simulation status and time remaining.
 */
public class SimulationHud {
    /** Unique identifier for the HUD GUI layer. */
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "simulation_hud");
    
    /** The current status message to display (synced from server). */
    public static String currentStatus = "";
    
    /** The time remaining in the simulation, in seconds (synced from server). */
    public static int timeLeft = 0;

    /** Per-player earthquake intensity (0–magnitude); drives camera shake amplitude. */
    public static float intensity = 0f;

    /**
     * Registers the custom HUD layer to the NeoForge GUI system.
     * Positions the layer above the vanilla hotbar.
     * 
     * @param event The GUI layer registration event.
     */
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, SimulationHud::render);
    }

    /**
     * Renders the simulation HUD on the screen.
     * 
     * @param guiGraphics The graphics object used for drawing.
     * @param deltaTracker Tracker for frame delta time.
     */
    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        // When the simulation ends, SimulationStatusPayload sends status="" and timeLeft=0.
        // Both conditions being true means no simulation is active, so we skip rendering.
        if (currentStatus.isEmpty() && timeLeft <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        // Build the two lines to display.  currentStatus contains the enum name
        // ("FIRE" or "EARTHQUAKE"), set by SimulationStatusPayload.handle().
        String statusText = "SIMULATION STATUS: " + currentStatus;
        String timerText  = "TIME REMAINING: " + timeLeft + "s";

        // Top-left corner of the HUD panel (10 pixels from each edge).
        int xPos = 10;
        int yPos = 10;

        // Draw a semi-transparent black rectangle behind the text for contrast.
        // Colour format is ARGB: 0x80000000 = 50% opacity black.
        // Dimensions: 162 px wide, 30 px tall — enough for two lines of default font text.
        guiGraphics.fill(xPos - 2, yPos - 2, xPos + 160, yPos + 28, 0x80000000);

        // First line: status name in white (0xFFFFFFFF = fully opaque white).
        // The final 'true' enables drop shadow for readability over bright backgrounds.
        guiGraphics.text(font, statusText, xPos, yPos, 0xFFFFFFFF, true);

        // Second line: timer in red (0xFFFF0000) to create visual urgency.
        // Offset by 14 pixels down (approximate line height of the default Minecraft font).
        guiGraphics.text(font, timerText, xPos, yPos + 14, 0xFFFF0000, true);
    }

    /**
     * Applies camera shake proportional to the current earthquake intensity.
     * Two layers: a slow sinusoidal rumble and a fast random jitter, both scaled by intensity.
     * Registered to the NeoForge event bus from BerongSMPClient so it only runs client-side.
     */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (intensity <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        long time = (mc.level != null) ? mc.level.getGameTime() : System.currentTimeMillis() / 50L;
        // Low-frequency ground roll (~1 Hz) — feels like the earth heaving underfoot.
        float rumbleSlow = (float) Math.sin(time * 0.314) * intensity * 1.4f;
        // Mid-frequency secondary oscillation (~3 Hz) — building resonance.
        float rumbleMid  = (float) Math.sin(time * 0.942) * intensity * 0.7f;
        // High-frequency random jitter — structural noise and micro-tremors.
        float jitterYaw   = ((float) Math.random() * 2f - 1f) * intensity * 1.0f;
        float jitterPitch = ((float) Math.random() * 2f - 1f) * intensity * 0.7f;
        // Roll shake — tilts the horizon for maximum disorientation during strong shaking.
        float roll = (float) Math.sin(time * 0.628) * intensity * 0.9f
                   + ((float) Math.random() * 2f - 1f) * intensity * 0.4f;
        event.setYaw((float) event.getYaw() + rumbleSlow + rumbleMid + jitterYaw);
        event.setPitch((float) event.getPitch() + jitterPitch);
        event.setRoll((float) event.getRoll() + roll);
    }
}
