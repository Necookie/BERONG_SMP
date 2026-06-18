package net.necookie.disastersim;

import net.necookie.disastersim.client.SimulationHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entry point for BerongSMP.
 * Handles client-side setup, GUI registration, and event listeners.
 */
@Mod(value = BerongSMP.MODID, dist = Dist.CLIENT)
public class BerongSMPClient {

    /**
     * Constructor for the client-side mod class.
     * Registers the configuration screen factory.
     * 
     * @param container The mod container for BerongSMP.
     */
    public BerongSMPClient(ModContainer container) {
        // Register a factory to create the configuration screen for this mod
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Handles client-side initialization logic.
     * 
     * @param event The client setup event.
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BerongSMP.LOGGER.info("BerongSMP Client Setup Initialized");
        
        // Register client-specific tick listener to the NeoForge event bus
        NeoForge.EVENT_BUS.addListener(net.necookie.disastersim.client.ClientEvents::onClientTick);
        // Drive camera shake for earthquake simulations
        NeoForge.EVENT_BUS.addListener(net.necookie.disastersim.client.SimulationHud::onCameraAngles);
    }

    /**
     * Registers custom GUI layers (like the simulation HUD) for the client.
     * 
     * @param event The GUI layer registration event.
     */
    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        SimulationHud.registerGuiLayers(event);
    }
}
