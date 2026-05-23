package net.necookie.disastersim.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.necookie.disastersim.BerongSMP;
import org.lwjgl.glfw.GLFW;

/**
 * Handles registration of custom key mappings for the mod.
 */
public class KeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "main"));

    public static final KeyMapping PULL_PIN = new KeyMapping(
            "key." + BerongSMP.MODID + ".pull_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PULL_PIN);
    }
}
