package net.necookie.disastersim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Handles client-side input events.
 */
public class ClientEvents {
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        // Logic for pull pin removed to match migration mod behavior.
    }
}
