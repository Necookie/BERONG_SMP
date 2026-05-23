package net.necookie.disastersim.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class SimulationHud {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "simulation_hud");
    public static String currentStatus = "";
    public static int timeLeft = 0;

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, SimulationHud::render);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (currentStatus.isEmpty() && timeLeft <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        
        String text = "SIMULATION STATUS: " + currentStatus;
        String timeText = "TIME REMAINING: " + timeLeft + "s";

        int x = 10;
        int y = 10;

        guiGraphics.fill(x - 2, y - 2, x + 150, y + 25, 0x80000000);
        guiGraphics.text(font, text, x, y, 0xFFFFFFFF, true);
        guiGraphics.text(font, timeText, x, y + 12, 0xFFFF0000, true);
    }
}