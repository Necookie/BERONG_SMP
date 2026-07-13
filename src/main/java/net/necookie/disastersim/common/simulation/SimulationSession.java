package net.necookie.disastersim.common.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SimulationSession {

    public enum EarthquakePhase { RUMBLE, PEAK, AFTERSHOCK, END }

    /**
     * New Sim Building 2.0's fire story: PREVENTION (find/defuse the armed hazards) →
     * INTERVENTION (extinguish whichever escalated into small fires) → EVACUATION (a fire went
     * uncontained, reach the assembly zone) → END. Mirrors {@link EarthquakePhase}'s shape —
     * {@link #tickFirePhase()} is the fire-side equivalent of {@link #tickQuakePhase}. Null for
     * every scenario except {@code NEW_SIM_BUILDING2_FIRE}.
     */
    public enum FirePhase { PREVENTION, INTERVENTION, EVACUATION, END }

    private final SimulationManager.SimulationState state;
    private final ServerPlayer player;
    private int timerTicks;
    private boolean frozen = false;
    private int firesExtinguished;
    private int fireSpreadCount;
    public final SimulationEventLogger logger = new SimulationEventLogger();

    private static final String CSV_HEADER =
        "player_id,session_id,scenario_type,timestamp,event_type,x,y,z,hazard_distance,interaction_target,nearby_player_count," +
        "hit_fire,extinguisher_class,phase";
    private final StringBuilder csvBuffer = new StringBuilder(CSV_HEADER).append('\n');

    public void bufferCsvRow(String row) { csvBuffer.append(row).append('\n'); }
    public String buildMoveCsv() { return csvBuffer.toString(); }

    /**
     * Every fire block that actually ignites or gets put out during this session, buffered
     * in-memory the same way {@link #csvBuffer} buffers move ticks — never written to Turso
     * per-event, only once as a whole blob at session end (see {@code SimulationManager.endSimulation}'s
     * {@code fire_log_csv} column). Distinct from the general {@code event_log} JSON (which already
     * carries a "hazard_failure"/"hazard_neutralize" entry per hazard prop, not a row per actual
     * fire *block*) — this is what the dashboard's animated fire-spread map replays. Rows come from
     * {@code TelemetryCsvWriter.writeFireLogRow}, which also durably writes each one to
     * {@code run/telemetry/fire_logs_<date>.csv} as it happens — the same dual local-file +
     * in-memory-buffer-for-Turso shape {@link #bufferCsvRow} already uses for move ticks.
     */
    private static final String FIRE_LOG_HEADER = "session_id,timestamp,x,y,z,event";
    private final StringBuilder fireLogBuffer = new StringBuilder(FIRE_LOG_HEADER).append('\n');

    public void bufferFireLogRow(String row) { fireLogBuffer.append(row).append('\n'); }
    public String buildFireLogCsv() { return fireLogBuffer.toString(); }

    private final String sessionId = UUID.randomUUID().toString().substring(0, 8);
    private final Instant startedAt = Instant.now();

    // --- Arena bounds (set by SimulationManager before first tick) ---
    private BlockPos arenaOrigin;
    private int arenaSpanX;
    private int arenaSpanZ;
    private int arenaHeight;

    // --- CCS computer positions (cached at session start for fast spread targeting) ---
    private List<BlockPos> computerPositions = new ArrayList<>();

    // --- Hazard props (cached at session start; ticks-since-hazardous drives failure timing) ---
    private List<BlockPos> hazardPositions = new ArrayList<>();
    private final java.util.Map<BlockPos, Integer> hazardTimers = new java.util.HashMap<>();

    // --- Earthquake state ---
    private BlockPos epicenter;
    private EarthquakePhase quakePhase;
    private int quakePhaseTimer;
    private final ArrayDeque<BlockPos> pendingDestructions = new ArrayDeque<>();
    private double sessionMagnitude;
    private int aftershockCount;
    private double aftershockMagnitudeScale = 1.0;

    // --- New Sim Building 2.0 fire-phase state ---
    private FirePhase firePhase;
    private int firePhaseTimer;
    private List<BlockPos> armedHazards = new ArrayList<>();
    private final Set<BlockPos> preventedHazards = new HashSet<>();
    private final Set<BlockPos> escalatedHazards = new HashSet<>();
    private final Set<BlockPos> extinguishedEscalated = new HashSet<>();
    private boolean alarmRung = false;

    /**
     * The session's own duration anchor, captured alongside {@link #timerTicks} — needed because
     * {@link #elapsedTicks()} must work correctly for scenarios like New Sim Building 2.0 whose
     * total duration isn't {@code Config.SIM_DURATION_TICKS} (see {@link #bindDuration}). Every
     * other scenario's elapsed-time math is unaffected: this equals the config default unless
     * overridden.
     */
    private int initialTimerTicks;

    public SimulationSession(ServerPlayer player, SimulationManager.SimulationState state) {
        this.player     = player;
        this.state      = state;
        // Read the configured duration at session creation time.  Reading from Config
        // here (rather than a constant) means you can change berongsmp-common.toml and
        // the next session will use the new value without restarting the JVM.
        this.timerTicks = Config.SIM_DURATION_TICKS.get();
        this.initialTimerTicks = this.timerTicks;
    }

    /**
     * Overrides both the remaining and total session duration — used by scenarios whose length
     * isn't the config default (e.g. New Sim Building 2.0's prevention+intervention+evacuation
     * budget). Must be called before the session's first tick.
     */
    public void bindDuration(int ticks) {
        this.timerTicks = ticks;
        this.initialTimerTicks = ticks;
    }

    /** Ticks elapsed since session start — the correct anchor for telemetry {@code t}, per the contract's SIM_START rule. */
    public int elapsedTicks() { return initialTimerTicks - timerTicks; }
    public double elapsedSeconds() { return elapsedTicks() / 20.0; }

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

    public void setHazardPositions(List<BlockPos> positions) { this.hazardPositions = new ArrayList<>(positions); }
    public List<BlockPos> getHazardPositions() { return hazardPositions; }
    public java.util.Map<BlockPos, Integer> getHazardTimers() { return hazardTimers; }

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

    /**
     * Initializes a New Sim Building 2.0 run: arms {@code armed} as the 5 (or fewer, if the
     * building's hazard pool came up short) hunt targets and enters PREVENTION. Called once from
     * {@code SimulationManager.startSimulation} after the hazards have actually been set
     * {@code HAZARDOUS=true} in the world.
     */
    public void initPhasedFire(List<BlockPos> armed) {
        this.armedHazards = new ArrayList<>(armed);
        this.preventedHazards.clear();
        this.escalatedHazards.clear();
        this.extinguishedEscalated.clear();
        this.firePhase = FirePhase.PREVENTION;
        this.firePhaseTimer = 0;
    }

    /**
     * Advances the fire-phase state machine by one tick — pure bookkeeping, no world access (the
     * caller, {@code SimulationManager.tickNewSim2FireSession}, compares the phase before/after
     * and performs the actual world mutation — escalating unfound hazards, igniting escalated
     * ones further — exactly like {@link #tickQuakePhase} delegates its world effects back to
     * {@code SimulationManager}).
     */
    public void tickFirePhase() {
        if (firePhase == null || firePhase == FirePhase.END) return;
        firePhaseTimer++;
        switch (firePhase) {
            case PREVENTION -> {
                if (preventedHazards.size() >= armedHazards.size()) {
                    firePhase = FirePhase.END;
                    firePhaseTimer = 0;
                } else if (firePhaseTimer >= Config.NEW_SIM2_PREVENTION_TICKS.get()) {
                    firePhase = FirePhase.INTERVENTION;
                    firePhaseTimer = 0;
                }
            }
            case INTERVENTION -> {
                if (!escalatedHazards.isEmpty() && extinguishedEscalated.containsAll(escalatedHazards)) {
                    firePhase = FirePhase.END;
                    firePhaseTimer = 0;
                } else if (firePhaseTimer >= Config.NEW_SIM2_INTERVENTION_TICKS.get()) {
                    firePhase = FirePhase.EVACUATION;
                    firePhaseTimer = 0;
                }
            }
            case EVACUATION -> {
                // Terminal here — ends via AssemblyZone.onPlayerArrived or the overall session timer.
            }
            default -> { }
        }
    }

    public FirePhase getFirePhase() { return firePhase; }
    /** Lowercase phase name for telemetry ({@code prevention}/{@code intervention}/{@code evacuation}), or null outside New Sim Building 2.0. */
    public String firePhaseLabel() { return firePhase == null ? null : firePhase.name().toLowerCase(); }
    public List<BlockPos> getArmedHazards() { return armedHazards; }
    public Set<BlockPos> getPreventedHazards() { return preventedHazards; }
    public Set<BlockPos> getEscalatedHazards() { return escalatedHazards; }
    public Set<BlockPos> getExtinguishedEscalated() { return extinguishedEscalated; }
    public boolean isArmedHazard(BlockPos pos) { return armedHazards.contains(pos); }
    public boolean isEscalatedHazard(BlockPos pos) { return escalatedHazards.contains(pos); }
    public void recordPrevented(BlockPos pos) { preventedHazards.add(pos); }
    public void recordEscalated(BlockPos pos) { escalatedHazards.add(pos); }
    public void recordEscalatedExtinguished(BlockPos pos) { extinguishedEscalated.add(pos); }
    public void markAlarmRung() { alarmRung = true; }
    public boolean wasAlarmRung() { return alarmRung; }

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
