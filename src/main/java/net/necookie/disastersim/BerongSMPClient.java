package net.necookie.disastersim;

import net.necookie.disastersim.client.SimulationHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.necookie.disastersim.client.CustomNpcRenderer;
import net.necookie.disastersim.client.DropAndRollRenderModifier;

/**
 * Client-only entry point for BerongSMP.
 * Handles client-side setup, GUI registration, and event listeners.
 */
@Mod(value = BerongSMP.MODID, dist = Dist.CLIENT)
public class BerongSMPClient {

    /**
     * Constructor for the client-side mod class.
     * Registers the configuration screen factory and client-only keybindings.
     *
     * @param modEventBus The mod event bus, needed here (rather than in the common BerongSMP
     *                    constructor) so KeyMappings — which holds a real client-only KeyMapping
     *                    field — is only ever registered on the physical client.
     * @param container   The mod container for BerongSMP.
     */
    public BerongSMPClient(IEventBus modEventBus, ModContainer container) {
        // Register a factory to create the configuration screen for this mod
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.register(net.necookie.disastersim.client.KeyMappings.class);
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
        // Drive camera shake for QUAKE tutorial stages
        NeoForge.EVENT_BUS.addListener(net.necookie.disastersim.client.TutorialHud::onCameraAngles);
        // Drive camera shake for the Academy's earthquake drill (Sgt. Santos, Room 3)
        NeoForge.EVENT_BUS.addListener(net.necookie.disastersim.client.AcademyHud::onCameraAngles);
        // Supplementary whole-body rock for the drop-and-roll visual (crouch/tilt is registered
        // separately via RegisterRenderStateModifiersEvent, below)
        NeoForge.EVENT_BUS.addListener(DropAndRollRenderModifier::onRenderPre);
        NeoForge.EVENT_BUS.addListener(DropAndRollRenderModifier::onRenderPost);
    }

    /**
     * Registers custom GUI layers (like the simulation HUD) for the client.
     * 
     * @param event The GUI layer registration event.
     */
    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BerongSMP.CUSTOM_NPC.get(), CustomNpcRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        SimulationHud.registerGuiLayers(event);
        net.necookie.disastersim.client.TutorialHud.registerGuiLayers(event);
        net.necookie.disastersim.client.AcademyHud.registerGuiLayers(event);
        net.necookie.disastersim.client.AcademyCompassHud.registerGuiLayers(event);
    }

    /** Drives the purely cosmetic drop-and-roll crouch/tilt — see {@link DropAndRollRenderModifier}. */
    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new DropAndRollRenderModifier());
    }
}
