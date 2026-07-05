package net.necookie.disastersim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
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

        KeyMappings.pollDropAndRoll();
    }

    /**
     * Every HUD caption/camera-shake/compass field across {@link SimulationHud}, {@link TutorialHud},
     * {@link AcademyHud}, and {@link AcademyCompassHud} is a plain static field, which survives for
     * the life of the client JVM — a "Save and Quit to Title" only tears down the integrated server,
     * it does not reset these classes. Server-side, every one of those systems already clears its
     * own transient state on logout ({@code TutorialManager}/{@code AcademyManager}), but nothing
     * ever told the *client* to stop showing whatever caption/shake intensity it last received —
     * so a player who quits mid-earthquake (camera shake intensity nonzero) and reloads the same or
     * a different world keeps shaking indefinitely, since nothing will overwrite that stale value
     * until the next unrelated HUD packet happens to arrive. Reported as "the earthquake is still
     * going after exiting and playing back the world." Resetting on both logout and the subsequent
     * login covers every actual disconnect path (world exit, server kick, crash-recovery rejoin).
     */
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetHudState();
    }

    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        resetHudState();
    }

    private static void resetHudState() {
        SimulationHud.currentStatus = "";
        SimulationHud.timeLeft = 0;
        SimulationHud.intensity = 0f;
        TutorialHud.prompt = "";
        TutorialHud.intensity = 0f;
        AcademyHud.prompt = "";
        AcademyHud.intensity = 0f;
        AcademyHud.promptExpiresAtMillis = 0L;
        AcademyCompassHud.active = false;
    }
}
