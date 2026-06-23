package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.UUID;

public class SimulationSession {

    public enum EarthquakePhase { RUMBLE, PEAK, AFTERSHOCK, END }

    private final SimulationManager.SimulationState state;
    private final ServerPlayer player;
    private int timerTicks;
    private boolean frozen = false;
    private int firesExtinguished;
    private int fireSpreadCount;
    public final SimulationEventLogger logger = new SimulationEventLogger();

    private final String sessionId = UUID.randomUUID().toString().substring(0, 8);
    private final Instant startedAt = Instant.now();

    // --- Earthquake state ---
    private BlockPos epicenter;
    private EarthquakePhase quakePhase;
    private int quakePhaseTimer;
    private final ArrayDeque<BlockPos> pendingDestructions = new ArrayDeque<>();
    private double sessionMagnitude;
    private int aftershockCount;
    private double aftershockMagnitudeScale = 1.0;

    public SimulationSession(ServerPlayer player, SimulationManager.SimulationState state) {
        this.player     = player;
        this.state      = state;
        // Read the configured duration at session creation time.  Reading from Config
        // here (rather than a constant) means you can change berongsmp-common.toml and
        // the next session will use the new value without restarting the JVM.
        this.timerTicks = Config.SIM_DURATION_TICKS.get();
    }

    public void tick() {
        if (!frozen) timerTicks--;
    }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    /** Directly sets remaining ticks. Clamps to [0, 72000]. */
    public void setTimerTicks(int ticks) { this.timerTicks = Math.max(0, Math.min(72000, ticks)); }

    public boolean isExpired() {
        return timerTicks <= 0;
    }

    public SimulationManager.SimulationState getState() {
        return state;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public int getTimerTicks() {
        return timerTicks;
    }

    public void recordExtinguish(int count) { firesExtinguished += count; }
    public int getFiresExtinguished() { return firesExtinguished; }
    public void incrementFireSpread() { fireSpreadCount++; }
    public int getFireSpreadCount() { return fireSpreadCount; }

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

    public String getSessionId() { return sessionId; }
    public Instant getStartedAt() { return startedAt; }
    public BlockPos getEpicenter() { return epicenter; }
    public EarthquakePhase getQuakePhase() { return quakePhase; }
    public ArrayDeque<BlockPos> getPendingDestructions() { return pendingDestructions; }
    public double getSessionMagnitude() { return sessionMagnitude; }
    public int getAftershockCount() { return aftershockCount; }
    public double getAftershockMagnitudeScale() { return aftershockMagnitudeScale; }
    public void setSessionMagnitude(double magnitude) { this.sessionMagnitude = magnitude; }

    private boolean assemblyReached = false;
    public boolean hasReachedAssembly() { return assemblyReached; }
    public void markAssemblyReached()   { assemblyReached = true; }

    private boolean passedExit = false;
    public boolean hasPassedExit() { return passedExit; }
    public void markPassedExit()   { passedExit = true; }

    private boolean extinguishEventPending = true;
    public boolean consumeExtinguishEventPending() {
        if (!extinguishEventPending) return false;
        extinguishEventPending = false;
        return true;
    }
    public void resetExtinguishEventPending() { extinguishEventPending = true; }
}
