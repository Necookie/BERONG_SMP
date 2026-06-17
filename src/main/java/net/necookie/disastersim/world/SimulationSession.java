package net.necookie.disastersim.world;

import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

/**
 * Holds the mutable runtime state for a single player's active simulation run.
 *
 * <p>All fields are private; callers advance the timer via {@link #tick()} and
 * query expiry via {@link #isExpired()} rather than touching the counter directly.
 * This class is not thread-safe on its own — callers must ensure mutations happen
 * on the server thread (e.g., inside {@code ServerTickEvent.Post}).
 */
public class SimulationSession {

    private final SimulationManager.SimulationState state;
    private final ServerPlayer player;
    /** Remaining simulation time in server ticks. Decremented once per tick via {@link #tick()}. */
    private int timerTicks;

    /**
     * Creates a new session for the given player and disaster type.
     * The timer is initialised from {@link Config#SIM_DURATION_TICKS} so that
     * the value can be changed via the config file without recompilation.
     *
     * @param player The player participating in the simulation.
     * @param state  The type of disaster being simulated.
     */
    public SimulationSession(ServerPlayer player, SimulationManager.SimulationState state) {
        this.player     = player;
        this.state      = state;
        this.timerTicks = Config.SIM_DURATION_TICKS.get();
    }

    /**
     * Advances the simulation clock by one server tick.
     * Must only be called from the server thread.
     */
    public void tick() {
        timerTicks--;
    }

    /**
     * Returns {@code true} when the simulation timer has reached zero or below,
     * indicating the session should be ended.
     */
    public boolean isExpired() {
        return timerTicks <= 0;
    }

    /** Returns the disaster type for this session. */
    public SimulationManager.SimulationState getState() {
        return state;
    }

    /** Returns the player bound to this session. May be online or offline. */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the number of server ticks remaining in the simulation.
     * Divide by 20 to convert to seconds.
     */
    public int getTimerTicks() {
        return timerTicks;
    }
}
