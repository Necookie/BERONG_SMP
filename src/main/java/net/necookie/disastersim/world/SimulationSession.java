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
    /** Total fire/soul-fire blocks extinguished by the player during this session. */
    private int firesExtinguished;

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
        // Read the configured duration at session creation time.  Reading from Config
        // here (rather than a constant) means you can change berongsmp-common.toml and
        // the next session will use the new value without restarting the JVM.
        this.timerTicks = Config.SIM_DURATION_TICKS.get();
    }

    /**
     * Advances the simulation clock by one server tick.
     * Called once per tick by {@link SimulationManager#onServerTick}.
     * Must only be called from the server thread to avoid race conditions.
     */
    public void tick() {
        timerTicks--;
    }

    /**
     * Returns {@code true} when the simulation timer has counted down to zero or below.
     * SimulationManager checks this after every {@link #tick()} and calls
     * {@link SimulationManager#endSimulation} if true.
     */
    public boolean isExpired() {
        return timerTicks <= 0;
    }

    /**
     * Returns the type of disaster running in this session (FIRE or EARTHQUAKE).
     * Used by SimulationManager to dispatch the correct effect method.
     */
    public SimulationManager.SimulationState getState() {
        return state;
    }

    /**
     * Returns the {@link ServerPlayer} this session belongs to.
     * The reference is stable for the lifetime of the session — the player object
     * exists as long as the player is logged in.
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the number of server ticks remaining in this simulation.
     * Divide by 20 to convert to whole seconds for display purposes.
     * SimulationManager uses ceiling division: {@code (ticks + 19) / 20} so the
     * HUD never shows 0 while time is still remaining.
     */
    public int getTimerTicks() {
        return timerTicks;
    }

    /** Increments the fires-extinguished counter by {@code count}. */
    public void recordExtinguish(int count) {
        firesExtinguished += count;
    }

    /** Returns the total number of fire blocks extinguished during this session. */
    public int getFiresExtinguished() {
        return firesExtinguished;
    }
}
