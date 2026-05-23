package net.necookie.disastersim.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Global key mappings for the mod.
 */
public class KeyMappings {
    // Removed PULL_PIN mapping as it is no longer used by the new fire extinguisher.

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        // No custom key mappings to register currently.
    }
}
