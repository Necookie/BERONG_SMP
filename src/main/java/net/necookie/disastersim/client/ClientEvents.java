package net.necookie.disastersim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Handles general client-side tick events.
 * This class can be used to monitor player state or input every frame/tick on the client.
 */
public class ClientEvents {

    /**
     * Logic executed every client tick.
     * 
     * @param event The client tick event (Post-tick).
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        // Ensure the player is actually in a world
        if (player == null) {
            return;
        }

        // Future client-side logic (e.g., custom input handling or state monitoring) 
        // can be implemented here.
    }
}
