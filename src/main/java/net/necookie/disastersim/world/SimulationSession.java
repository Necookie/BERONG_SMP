package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

import java.util.ArrayDeque;

/**
 * Holds the mutable runtime state for a single player's active simulation run.
 *
 * <p>All fields are private; callers advance the timer via {@link #tick()} and
 * query expiry via {@link #isExpired()} rather than touching the counter directly.
 * This class is not thread-safe on its own — callers must ensure mutations happen
 * on the server thread (e.g., inside {@code ServerTickEvent.Post}).
 */
public class SimulationSession {

    /** Phases of an earthquake simulation, progressing RUMBLE → PEAK → AFTERSHOCK → END. */
    public enum EarthquakePhase { RUMBLE, PEAK, AFTERSHOCK, END }

    private final SimulationManager.SimulationState state;
    private final ServerPlayer player;
    /** Remaining simulation time in server ticks. Decremented once per tick via {@link #tick()}. */
    private int timerTicks;
    /** Total fire/soul-fire blocks extinguished by the player during this session. */
    private int firesExtinguished;

    // --- Earthquake state ---
    /** Random epicenter within the simulation arena; null when not an EARTHQUAKE session. */
    private BlockPos epicenter;
    /** Current phase of the earthquake progression; null when not an EARTHQUAKE session. */
    private EarthquakePhase quakePhase;
    /** Ticks elapsed inside the current earthquake phase. */
    private int quakePhaseTimer;
    /** Pending block positions queued for cascading destruction during the PEAK phase. */
    private final ArrayDeque<BlockPos> pendingDestructions = new ArrayDeque<>();

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

    // -----------------------------------------------------------------------
    // Earthquake lifecycle
    // -----------------------------------------------------------------------

    /** Initialises epicenter and phase state for an EARTHQUAKE session. Called once by SimulationManager after session creation. */
    public void initEarthquake(RandomSource random) {
        int areaSize = Config.SIM_AREA_SIZE.get();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();
        this.epicenter = SimulationManager.SIM_POS.offset(
                random.nextInt(areaSize),
                random.nextInt(Math.max(1, areaHeight / 2)),
                random.nextInt(areaSize));
        this.quakePhase = EarthquakePhase.RUMBLE;
        this.quakePhaseTimer = 0;
    }

    /**
     * Advances the earthquake phase state machine by one tick.
     * Transitions RUMBLE → PEAK → AFTERSHOCK → END based on configured durations.
     * Must be called each tick from SimulationManager for EARTHQUAKE sessions.
     */
    public void tickQuakePhase() {
        if (quakePhase == null || quakePhase == EarthquakePhase.END) return;
        quakePhaseTimer++;
        int duration = switch (quakePhase) {
            case RUMBLE     -> Config.QUAKE_RUMBLE_DURATION.get();
            case PEAK       -> Config.QUAKE_PEAK_DURATION.get();
            case AFTERSHOCK -> Config.QUAKE_AFTERSHOCK_DURATION.get();
            default         -> Integer.MAX_VALUE;
        };
        if (quakePhaseTimer >= duration) {
            quakePhaseTimer = 0;
            quakePhase = switch (quakePhase) {
                case RUMBLE     -> EarthquakePhase.PEAK;
                case PEAK       -> { pendingDestructions.clear(); yield EarthquakePhase.AFTERSHOCK; }
                case AFTERSHOCK -> EarthquakePhase.END;
                default         -> EarthquakePhase.END;
            };
        }
    }

    /**
     * Computes the player's current earthquake intensity at the given block position.
     * Uses an exponential decay: {@code intensity = magnitude * exp(-decayRate * distance) * phaseScale}.
     */
    public double computeIntensityAt(BlockPos pos) {
        if (epicenter == null || quakePhase == null || quakePhase == EarthquakePhase.END) return 0.0;
        double magnitude  = Config.QUAKE_MAGNITUDE.get();
        double decayRate  = Config.QUAKE_DECAY_RATE.get();
        double distance   = Math.sqrt(epicenter.distSqr(pos));
        double phaseScale = switch (quakePhase) {
            case RUMBLE     -> 0.4;
            case PEAK       -> 1.0;
            case AFTERSHOCK -> 0.25;
            default         -> 0.0;
        };
        return magnitude * Math.exp(-decayRate * distance) * phaseScale;
    }

    /** Returns the random epicenter chosen at session start; {@code null} for non-EARTHQUAKE sessions. */
    public BlockPos getEpicenter() { return epicenter; }

    /** Returns the current earthquake phase; {@code null} for non-EARTHQUAKE sessions. */
    public EarthquakePhase getQuakePhase() { return quakePhase; }

    /** Returns the pending-destructions deque used for cascade block-breaking during PEAK. */
    public ArrayDeque<BlockPos> getPendingDestructions() { return pendingDestructions; }
}
