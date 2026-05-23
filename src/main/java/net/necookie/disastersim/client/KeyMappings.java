package net.necookie.disastersim.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Manages the registration of custom key mappings (keybindings) for BerongSMP.
 */
public class KeyMappings {

    /**
     * Registers all custom keybindings to the game.
     * Called during the client setup phase.
     * 
     * @param event The key mapping registration event.
     */
    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        // Currently, no custom keybindings are required for the mod.
        // If a "pull pin" or "reset simulation" key is needed, it would be registered here.
    }
}
