package net.necookie.disastersim.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Shared earthquake camera shake used by both {@link SimulationHud} (live simulations) and
 * {@link TutorialHud} (the DROP-COVER-HOLD drill). Centralises the multi-layer shake math that
 * was previously copy-pasted between the two.
 */
final class CameraShake {

    private CameraShake() {}

    /**
     * Adds intensity-scaled shake to the camera angles: a slow (~1 Hz) ground roll, a mid (~3 Hz)
     * building resonance, high-frequency random jitter, and a roll tilt for disorientation.
     * No-op when {@code intensity <= 0}. Runs on the client render thread.
     */
    static void apply(ViewportEvent.ComputeCameraAngles event, float intensity) {
        if (intensity <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        long time = (mc.level != null) ? mc.level.getGameTime() : System.currentTimeMillis() / 50L;
        // Low-frequency ground roll (~1 Hz) — the earth heaving underfoot.
        float rumbleSlow = (float) Math.sin(time * 0.314) * intensity * 1.4f;
        // Mid-frequency secondary oscillation (~3 Hz) — building resonance.
        float rumbleMid  = (float) Math.sin(time * 0.942) * intensity * 0.7f;
        // High-frequency random jitter — structural noise and micro-tremors.
        float jitterYaw   = ((float) Math.random() * 2f - 1f) * intensity * 1.0f;
        float jitterPitch = ((float) Math.random() * 2f - 1f) * intensity * 0.7f;
        // Roll shake — tilts the horizon for maximum disorientation during strong shaking.
        float roll = (float) Math.sin(time * 0.628) * intensity * 0.9f
                   + ((float) Math.random() * 2f - 1f) * intensity * 0.4f;
        event.setYaw((float)   event.getYaw()   + rumbleSlow + rumbleMid + jitterYaw);
        event.setPitch((float) event.getPitch() + jitterPitch);
        event.setRoll((float)  event.getRoll()  + roll);
    }
}
