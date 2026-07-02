package net.necookie.disastersim.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.necookie.disastersim.network.DropAndRollPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Manages the registration of custom key mappings (keybindings) for BerongSMP.
 *
 * <p>This class is only ever touched from {@code BerongSMPClient}'s dist-gated registration path
 * (never from the common {@code BerongSMP} constructor), since a {@link KeyMapping} field is a
 * client-only Minecraft type — loading this class on a dedicated server would crash.
 */
public class KeyMappings {

    /** "Stop, drop, and roll" while on fire. Consumed edge-triggered in {@link ClientEvents}. */
    public static final KeyMapping DROP_AND_ROLL = new KeyMapping(
            "key.berongsmp.drop_and_roll",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.MISC);

    /**
     * Registers all custom keybindings to the game.
     * Called during the client setup phase.
     *
     * @param event The key mapping registration event.
     */
    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DROP_AND_ROLL);
    }

    /** Polled every client tick from {@link ClientEvents#onClientTick}. */
    static void pollDropAndRoll() {
        if (DROP_AND_ROLL.consumeClick()) {
            ClientPacketDistributor.sendToServer(new DropAndRollPayload());
        }
    }
}
