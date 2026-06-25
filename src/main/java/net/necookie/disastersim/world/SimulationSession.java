package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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

    // --- Arena bounds (set by SimulationManager before first tick) ---
    private BlockPos arenaOrigin;
    private int arenaSpanX;
    private int arenaSpanZ;
    private int arenaHeight;

    // --- CCS computer positions (cached at session start for fast spread targeting) ---
    private List<BlockPos> computerPositions = new ArrayList<>();

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

    /** Called by SimulationManager right after construction to bind the arena. */
    public void setArena(BlockPos origin, int spanX, int spanZ, int height) {
        this.arenaOrigin = origin;
        this.arenaSpanX  = spanX;
        this.arenaSpanZ  = spanZ;
        this.arenaHeight = height;
    }

    public BlockPos getArenaOrigin() { return arenaOrigin != null ? arenaOrigin : SimulationManager.SIM_POS; }
    public int getArenaSpanX()  { return arenaSpanX  > 0 ? arenaSpanX  : Config.SIM_AREA_SIZE.get(); }
    public int getArenaSpanZ()  { return arenaSpanZ  > 0 ? arenaSpanZ  : Config.SIM_AREA_SIZE.get(); }
    public int getArenaHeight() { return arenaHeight > 0 ? arenaHeight : Config.SIM_AREA_HEIGHT.get(); }

    public void setComputerPositions(List<BlockPos> positions) { this.computerPositions = new ArrayList<>(positions); }
    public List<BlockPos> getComputerPositions() { return computerPositions; }

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
        int effectiveHeight = getArenaHeight();
        BlockPos base = getArenaOrigin();
        // Epicenter 3–9 blocks inside the arena from the origin corner so destruction
        // concentrates inside the structure rather than on its perimeter.
        this.epicenter = base.offset(
                3 + random.nextInt(7),
                random.nextInt(Math.max(1, effectiveHeight / 3)),
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

    // --- Warmup / cooldown phases ---
    private int warmupTicks  = 3 * 20; // countdown before effects begin (3 s)
    private int cooldownTicks = -1;    // -1 = not triggered; 0 = signal endSimulation

    public boolean isInWarmup()   { return warmupTicks > 0; }
    public int  getWarmupTicks()  { return warmupTicks; }
    public void tickWarmup()      { if (warmupTicks > 0) warmupTicks--; }

    public void startCooldown()         { if (cooldownTicks < 0) cooldownTicks = 3 * 20; }
    public boolean isInCooldown()       { return cooldownTicks > 0; }
    public void tickCooldown()          { if (cooldownTicks > 0) cooldownTicks--; }
    public boolean isCooldownExpired()  { return cooldownTicks == 0; }

    private boolean extinguishEventPending = true;
    public boolean consumeExtinguishEventPending() {
        if (!extinguishEventPending) return false;
        extinguishEventPending = false;
        return true;
    }
    public void resetExtinguishEventPending() { extinguishEventPending = true; }
}
