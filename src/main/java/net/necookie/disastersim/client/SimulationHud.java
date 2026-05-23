package net.necookie.disastersim.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
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
        // Only render if there is an active simulation status or time remaining
        if (currentStatus.isEmpty() && timeLeft <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        
        // Prepare the display strings
        String statusText = "SIMULATION STATUS: " + currentStatus;
        String timerText = "TIME REMAINING: " + timeLeft + "s";

        // Screen coordinates for the HUD element
        int xPos = 10;
        int yPos = 10;

        // Draw a semi-transparent black background box for readability
        // fill(x1, y1, x2, y2, color)
        guiGraphics.fill(xPos - 2, yPos - 2, xPos + 160, yPos + 28, 0x80000000);
        
        // Draw the status text in white
        guiGraphics.text(font, statusText, xPos, yPos, 0xFFFFFFFF, true);
        
        // Draw the timer text in red to emphasize urgency
        guiGraphics.text(font, timerText, xPos, yPos + 14, 0xFFFF0000, true);
    }
}
