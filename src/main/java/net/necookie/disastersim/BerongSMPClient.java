package net.necookie.disastersim;

import net.minecraft.client.Minecraft;
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

@Mod(value = BerongSMP.MODID, dist = Dist.CLIENT)

public class BerongSMPClient {
    public BerongSMPClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BerongSMP.LOGGER.info("HELLO FROM CLIENT SETUP");
        NeoForge.EVENT_BUS.addListener(net.necookie.disastersim.client.ClientEvents::onClientTick);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        SimulationHud.registerGuiLayers(event);
    }
}