package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.UUID;

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
    /** When true, the timer does not count down. Set/cleared by /sim_freeze and /sim_unfreeze. */
    private boolean frozen = false;
    /** Total fire/soul-fire blocks extinguished by the player during this session. */
    private int firesExtinguished;
    /** Total fire blocks spread/placed by the simulation during this session. */
    private int fireSpreadCount;
    /** Buffers behavioral events; serialized to JSON and flushed to Turso on session end. */
    public final SimulationEventLogger logger = new SimulationEventLogger();

    /** Unique identifier for this run — used as the CSV join key. */
    private final String sessionId = UUID.randomUUID().toString().substring(0, 8);
    /** Wall-clock instant when this session was created (used in CSV §5 metadata). */
    private final Instant startedAt = Instant.now();

    // --- Earthquake state ---
    /** Random epicenter within the simulation arena; null when not an EARTHQUAKE session. */
    private BlockPos epicenter;
    /** Current phase of the earthquake progression; null when not an EARTHQUAKE session. */
    private EarthquakePhase quakePhase;
    /** Ticks elapsed inside the current earthquake phase. */
    private int quakePhaseTimer;
    /** Pending block positions queued for cascading destruction during the PEAK phase. */
    private final ArrayDeque<BlockPos> pendingDestructions = new ArrayDeque<>();
    /** Per-session magnitude (0.1–10.0); set at session start and optionally changed live. */
    private double sessionMagnitude;
    /** Remaining aftershocks after the initial AFTERSHOCK phase. */
    private int aftershockCount;
    /** Magnitude scale for the current aftershock relative to sessionMagnitude. */
    private double aftershockMagnitudeScale = 1.0;

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
     * No-ops when frozen.
     */
    public void tick() {
        if (!frozen) timerTicks--;
    }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    /** Directly sets remaining ticks. Clamps to [0, 72000]. */
    public void setTimerTicks(int ticks) { this.timerTicks = Math.max(0, Math.min(72000, ticks)); }

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

    /** Increments the count of fire blocks spread by the simulation. */
    public void incrementFireSpread() { fireSpreadCount++; }

    /** Returns the total fire blocks spread by the simulation during this session. */
    public int getFireSpreadCount() { return fireSpreadCount; }

    // -----------------------------------------------------------------------
    // Earthquake lifecycle
    // -----------------------------------------------------------------------

    /** Initialises epicenter, phase state, and magnitude for an EARTHQUAKE session. Called once by SimulationManager after session creation. */
    public void initEarthquake(RandomSource random, double magnitude) {
        int areaHeight = Config.SIM_AREA_HEIGHT.get();
        // Epicenter fixed inside the library interior (3–9 blocks from SIM_POS in XZ)
        // so destruction concentrates inside the structure, not scattered across the arena.
        this.epicenter = SimulationManager.SIM_POS.offset(
                3 + random.nextInt(7),
                random.nextInt(Math.max(1, areaHeight / 3)),
                3 + random.nextInt(7));
        this.sessionMagnitude        = magnitude;
        this.aftershockCount         = 2 + random.nextInt(3); // 2–4 aftershocks
        this.aftershockMagnitudeScale = 1.0;
        this.quakePhase     = EarthquakePhase.RUMBLE;
        this.quakePhaseTimer = 0;
    }

    /**
     * Advances the earthquake phase state machine by one tick.
     * Transitions RUMBLE → PEAK → AFTERSHOCK → END based on configured durations.
     * Must be called each tick from SimulationManager for EARTHQUAKE sessions.
     */
    public void tickQuakePhase(RandomSource random) {
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
                case RUMBLE -> EarthquakePhase.PEAK;
                case PEAK   -> { pendingDestructions.clear(); yield EarthquakePhase.AFTERSHOCK; }
                case AFTERSHOCK -> {
                    if (aftershockCount > 0) {
                        aftershockCount--;
                        // 25% chance of a stronger aftershock (0.6–1.1×), otherwise weaker (0.2–0.55×)
                        aftershockMagnitudeScale = (random.nextFloat() < 0.25f)
                                ? 0.6 + random.nextFloat() * 0.5
                                : 0.2 + random.nextFloat() * 0.35;
                        yield EarthquakePhase.AFTERSHOCK;
                    }
                    yield EarthquakePhase.END;
                }
                default -> EarthquakePhase.END;
            };
        }
    }

    /**
     * Computes the player's current earthquake intensity at the given block position.
     * Uses an exponential decay: {@code intensity = magnitude * exp(-decayRate * distance) * phaseScale}.
     */
    public double computeIntensityAt(BlockPos pos) {
        if (epicenter == null || quakePhase == null || quakePhase == EarthquakePhase.END) return 0.0;
        double magnitude  = sessionMagnitude;
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

    /** Returns the unique session identifier used as the CSV join key. */
    public String getSessionId() { return sessionId; }

    /** Returns the wall-clock instant when this session was created. */
    public Instant getStartedAt() { return startedAt; }

    /** Returns the random epicenter chosen at session start; {@code null} for non-EARTHQUAKE sessions. */
    public BlockPos getEpicenter() { return epicenter; }

    /** Returns the current earthquake phase; {@code null} for non-EARTHQUAKE sessions. */
    public EarthquakePhase getQuakePhase() { return quakePhase; }

    /** Returns the pending-destructions deque used for cascade block-breaking during PEAK. */
    public ArrayDeque<BlockPos> getPendingDestructions() { return pendingDestructions; }

    /** Returns the per-session magnitude used for intensity and radius calculations. */
    public double getSessionMagnitude() { return sessionMagnitude; }

    /** Returns the total aftershock wave count initialised for this session (2–4). */
    public int getAftershockCount() { return aftershockCount; }

    /** Returns the magnitude scale for the current aftershock phase (relative to sessionMagnitude). */
    public double getAftershockMagnitudeScale() { return aftershockMagnitudeScale; }

    /** Updates the per-session magnitude live; takes effect on the next tick. */
    public void setSessionMagnitude(double magnitude) { this.sessionMagnitude = magnitude; }

    // --- Telemetry zone flags (Phase 5) ---

    /** True once the player has entered the assembly zone during this session. */
    private boolean assemblyReached = false;
    public boolean hasReachedAssembly() { return assemblyReached; }
    public void markAssemblyReached()   { assemblyReached = true; }

    /** True once the player has passed through any designated emergency exit zone. */
    private boolean passedExit = false;
    public boolean hasPassedExit() { return passedExit; }
    public void markPassedExit()   { passedExit = true; }

    /** True once the extinguisher_use contract event has been emitted for the current spray gesture. */
    private boolean extinguishEventPending = true;
    public boolean consumeExtinguishEventPending() {
        if (!extinguishEventPending) return false;
        extinguishEventPending = false;
        return true;
    }
    public void resetExtinguishEventPending() { extinguishEventPending = true; }
}
