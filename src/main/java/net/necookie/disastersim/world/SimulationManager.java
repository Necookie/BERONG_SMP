package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.tutorial.TutorialManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.network.SimulationStatusPayload;
import net.necookie.disastersim.session.TursoClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry and tick driver for all active disaster simulations.
 *
 * <p>Holds one {@link SimulationSession} per player UUID in a {@link ConcurrentHashMap} and advances
 * them from {@link #onServerTick}. Each tick a session runs, in order: telemetry sampling, the
 * scenario effect (fire or earthquake), exit-zone and assembly-zone checks, and a HUD sync packet.
 *
 * <p>{@link #startSimulation}/{@link #endSimulation} are {@code synchronized}; everything else runs
 * single-threaded on the server tick thread. Buildings are (re)placed from {@link #BUILDINGS} on both
 * start and end so every session begins with clean, undamaged structures.
 *
 * <p><b>Hot path:</b> the fire-proximity scan ({@link #nearestFireDistance}) is the most frequent
 * heavy world read. It is memoised per (game-tick, position) so the several callers that need it for
 * the same player on the same tick — PLAYER_TICK, the move_tick CSV row, and {@link #hazardDistance}
 * — share a single scan instead of repeating it.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {

    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

    private static final BlockPos SSC_POS = new BlockPos(11,  -33, 90);
    public static final BlockPos CCS_POS = new BlockPos(76, -34, 4);

    // Separate from QUAKE_INTERVAL so tuning earthquake rate doesn't silently change HUD update frequency.
    private static final int HUD_SYNC_INTERVAL_TICKS = 10;

    private static final int TICKS_PER_SECOND = 20;

    // Half-extents of the proximity scan boxes (XZ radius, Y radius), and the sentinel
    // distance returned when no hazard is found in range.
    private static final int FIRE_SCAN_RADIUS_XZ = 10;
    private static final int FIRE_SCAN_RADIUS_Y  = 5;
    private static final int CCS_HAZARD_RADIUS_XZ = 15;
    private static final int CCS_HAZARD_RADIUS_Y  = 5;
    private static final double NO_HAZARD_DISTANCE = 99.0;

    // Single-slot per-tick memo for the fire scan — the heaviest per-tick world read.
    // Repeated calls for the same player on the same tick reuse the result instead of
    // re-scanning ~4,800 blocks. Server-thread only, so no synchronisation is needed.
    private static long   fireScanTick = Long.MIN_VALUE;
    private static long   fireScanPos  = Long.MIN_VALUE;
    private static double fireScanVal  = NO_HAZARD_DISTANCE;

    private static final double SIM_ENTRY_OFFSET_X = 5.5;
    private static final double SIM_ENTRY_OFFSET_Y = 2.0;
    private static final double SIM_ENTRY_OFFSET_Z = 5.5;

    private static final Map<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();

    private static final Set<UUID> pendingLobbyRespawn = ConcurrentHashMap.newKeySet();

    private static final List<Map.Entry<StructurePlacer, BlockPos>> BUILDINGS = List.of(
            Map.entry(new SimulationStructureLoader(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrarymain")), SIM_POS),
            Map.entry(new SchemLoader(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "structure/ssc_building.schem"), 1), SSC_POS),
            Map.entry(new SchemLoader(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "structure/ccs_admin_building2.4.schem"), 0), CCS_POS)
    );

    private static final SimulationEffects EFFECTS = new SimulationEffects();

    public enum SimulationState {
        IDLE, FIRE, EARTHQUAKE, CCS_FIRE, CCS_EARTHQUAKE;

        /** True for any fire-type simulation (library or CCS building). */
        public boolean isFire()  { return this == FIRE || this == CCS_FIRE; }
        /** True for any earthquake-type simulation (library or CCS building). */
        public boolean isQuake() { return this == EARTHQUAKE || this == CCS_EARTHQUAKE; }
        /** True when the simulation is set inside the CCS admin building. */
        public boolean isCCS()   { return this == CCS_FIRE || this == CCS_EARTHQUAKE; }
    }

    // CCS building fire spawn area (world space after 3-CCW placement at CCS_POS).
    // Spans the building interior; X: 80–135, Z: 6–69, Y covers both floors.
    private static final BlockPos CCS_FIRE_BASE  = new BlockPos(80, -32, 6);
    private static final int CCS_AREA_SPAN_X = 55;
    private static final int CCS_AREA_SPAN_Z = 63;
    private static final int CCS_AREA_HEIGHT = 12;

    public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
        startSimulation(player, state, Config.QUAKE_MAGNITUDE.get());
    }

    public static synchronized void startSimulation(ServerPlayer player, SimulationState state, double magnitude) {
        UUID uuid = player.getUUID();

        if (activeSessions.containsKey(uuid)) {
            player.sendSystemMessage(Component.literal("You already have a simulation in progress!"));
            return;
        }

        SimulationSession session = new SimulationSession(player, state);
        activeSessions.put(uuid, session);

        // Bind arena bounds so effects and epicenter calculations target the right building.
        if (state.isCCS()) {
            session.setArena(CCS_FIRE_BASE, CCS_AREA_SPAN_X, CCS_AREA_SPAN_Z, CCS_AREA_HEIGHT);
        } else {
            session.setArena(SIM_POS, Config.SIM_AREA_SIZE.get(), Config.SIM_AREA_SIZE.get(), Config.SIM_AREA_HEIGHT.get());
        }

        ServerLevel level = (ServerLevel) player.level();
        if (state.isQuake()) {
            session.initEarthquake(level.getRandom(), magnitude);
        }

        // Place all buildings so every session starts with clean, undamaged structures.
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());
        TelemetryCsvWriter.scanAndRegisterFireAlarms(level, SIM_POS);

        if (state.isFire()) {
            session.setHazardPositions(HazardManager.scanHazardProps(level,
                    session.getArenaOrigin(), session.getArenaSpanX(), session.getArenaSpanZ(), session.getArenaHeight()));
        }

        BlockPos spawnPos;
        if (state == SimulationState.CCS_FIRE) {
            List<BlockPos> computers = findComputersInCCS(level);
            session.setComputerPositions(computers);
            // Ignite 1 random computer to start the electrical fire
            igniteRandomComputers(level, computers, 1, level.getRandom());
            spawnPos = findSpawnNearComputer(level, computers);
        } else if (state.isCCS()) {
            spawnPos = findRandomSpawnInCCS(level);
        } else {
            spawnPos = findRandomSpawnInLibrary(level);
        }
        player.teleportTo(level,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);

        session.logger.log("SIM_START", java.util.Map.of(
            "sim_type", state.name(),
            "magnitude", state.isQuake() ? magnitude : 0.0,
            "x", spawnPos.getX(), "y", spawnPos.getY(), "z", spawnPos.getZ()
        ));

        if (state == SimulationState.FIRE) {
            ItemStack extinguisher = new ItemStack(BerongSMP.FIRE_EXTINGUISHER.get());
            player.getInventory().setItem(0, extinguisher);
            player.sendSystemMessage(Component.literal("§eYou have been given a Fire Extinguisher in slot 1. Remember: Pull the pin first before spraying! (PASS)"));
            player.sendSystemMessage(Component.literal("§6Starting FIRE Simulation!"));
        } else if (state == SimulationState.CCS_FIRE) {
            ItemStack extinguisher = new ItemStack(BerongSMP.CO2_EXTINGUISHER.get());
            player.getInventory().setItem(0, extinguisher);
            player.sendSystemMessage(Component.literal("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendSystemMessage(Component.literal("§c§l🔥  FIRE EMERGENCY — CCS Admin Building"));
            player.sendSystemMessage(Component.literal("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendSystemMessage(Component.literal("§eA fire has started from a computer workstation!"));
            player.sendSystemMessage(Component.literal("§fLocate the burning computer and suppress it immediately."));
            player.sendSystemMessage(Component.literal("§7CO2 extinguisher issued — §ePull pin → Aim → Sweep"));
        } else if (state.isQuake()) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§c⚠ Magnitude %.1f Earthquake has begun! Brace for impact!%s",
                    magnitude, state.isCCS() ? " (CCS Building)" : "")));
        }

        // --- Telemetry: session_start (t=0 anchor) ---
        double startHazDist = (state == SimulationState.FIRE)
                ? 99.0
                : (session.getEpicenter() != null
                        ? player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(session.getEpicenter()))
                        : 99.0);
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), uuid.toString(), state.name().toLowerCase(),
                0.0, "session_start",
                player.getX(), player.getY(), player.getZ(),
                startHazDist, null, null));
    }

    @SubscribeEvent
    public static void onPlayerInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SimulationSession session = activeSessions.get(player.getUUID());
        if (session == null) return;

        net.minecraft.world.level.block.state.BlockState state =
                event.getLevel().getBlockState(event.getPos());
        net.minecraft.world.level.block.Block block = state.getBlock();
        boolean isDoor = block instanceof net.minecraft.world.level.block.DoorBlock
                || block instanceof net.minecraft.world.level.block.TrapDoorBlock
                || block instanceof net.minecraft.world.level.block.FenceGateBlock;
        if (!isDoor) return;

        String targetName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
        double elapsedS = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        double hazDist;
        if (session.getState().isFire() && event.getLevel() instanceof ServerLevel sl) {
            hazDist = nearestFireDistance(sl, player.blockPosition());
        } else {
            hazDist = session.getEpicenter() != null
                    ? player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(session.getEpicenter()))
                    : 99.0;
        }
        double tRounded   = Math.round(elapsedS * 100.0) / 100.0;
        double hazRounded = Math.round(hazDist  * 100.0) / 100.0;
        session.logger.log("door_open", java.util.Map.of(
                "t", tRounded, "x", player.getX(), "y", player.getY(), "z", player.getZ(),
                "target", targetName, "hazard_distance", hazRounded));
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                tRounded, "door_open",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, targetName, null));
    }

    public static synchronized void endSimulation(UUID uuid) {
        endSimulation(uuid, "timeout");
    }

    public static synchronized void endSimulation(UUID uuid, String endReason) {
        // Remove the session atomically.  If no session existed (e.g., called twice),
        // bail out immediately — nothing to clean up.
        SimulationSession session = activeSessions.remove(uuid);
        if (session == null) return;

        int finalScore = 0;
        if (session.getState().isFire()) {
            finalScore = Math.min(100, session.getFiresExtinguished() * 2);
        }
        double elapsedT = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        session.logger.log("SIM_END", java.util.Map.of(
            "fires_extinguished", session.getFiresExtinguished(),
            "fire_spread_count", session.getFireSpreadCount(),
            "score", finalScore,
            "passed", finalScore >= net.necookie.disastersim.Config.PASS_THRESHOLD_FIRE.get(),
            "end_reason", endReason
        ));

        // --- Telemetry: session_end row + sessions CSV ---
        ServerPlayer playerForCsv = session.getPlayer();
        if (playerForCsv != null) {
            double endHazDist = hazardDistance(session, (ServerLevel) playerForCsv.level(), playerForCsv);
            session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                    session.getSessionId(), uuid.toString(),
                    session.getState().name().toLowerCase(),
                    Math.round(elapsedT * 100.0) / 100.0, "session_end",
                    playerForCsv.getX(), playerForCsv.getY(), playerForCsv.getZ(),
                    Math.round(endHazDist * 100.0) / 100.0, null, null));
        }
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("player_id",                    uuid.toString());
        meta.put("scenario_type",                session.getState().name().toLowerCase());
        meta.put("started_at",                   session.getStartedAt().toString());
        meta.put("ended_at",                     java.time.Instant.now().toString());
        meta.put("duration_ticks",               Config.SIM_DURATION_TICKS.get() - session.getTimerTicks());
        meta.put("end_reason",                   endReason);
        meta.put("fires_extinguished_count",     session.getFiresExtinguished());
        meta.put("magnitude",                    session.getState().isQuake()
                                                     ? session.getSessionMagnitude() : "");
        meta.put("aftershock_count",             session.getState().isQuake()
                                                     ? session.getAftershockCount() : "");
        meta.put("aftershock_magnitude_scale",   session.getState().isQuake()
                                                     ? session.getAftershockMagnitudeScale() : "");
        meta.put("final_earthquake_phase",       session.getQuakePhase() != null
                                                     ? session.getQuakePhase().name() : "");
        TelemetryCsvWriter.closeSession(session.getSessionId(), meta);
        TelemetryCsvWriter.flush();

        if (TursoClient.isReady()) {
            String simType = session.getState().name(); // FIRE, EARTHQUAKE, CCS_FIRE, or CCS_EARTHQUAKE
            boolean passed = session.getState().isFire()
                    && finalScore >= net.necookie.disastersim.Config.PASS_THRESHOLD_FIRE.get();
            net.necookie.disastersim.BerongSMP.LOGGER.info(
                    "[SimulationManager] endSimulation uuid={} simType={} score={}", uuid, simType, finalScore);
            TursoClient.executeAsync(
                    "UPDATE sessions SET simulation_type=?, simulation_score=?, passed=?," +
                    " end_time=?, status='completed', event_log=?, move_log_csv=?" +
                    " WHERE id=(SELECT id FROM sessions WHERE account_uuid=? AND status='active'" +
                    " ORDER BY id DESC LIMIT 1)",
                    simType, finalScore, passed,
                    java.time.Instant.now().toString(),
                    session.logger.toJson(),
                    session.buildMoveCsv(),
                    uuid.toString());
        } else {
            net.necookie.disastersim.BerongSMP.LOGGER.warn(
                    "[SimulationManager] TursoClient not ready — session data NOT saved for {}", uuid);
        }

        ServerPlayer player = session.getPlayer();
        if (player != null) {
            net.necookie.disastersim.session.SessionManager.onSimulationEnd(player, session);
        }
        if (player == null) return;

        ServerLevel level = (ServerLevel) player.level();
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());

        if (player.isAlive()) {
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0, 0f));
            player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
            if (session.getState().isFire()) {
                player.sendSystemMessage(Component.literal(
                    "§eFires extinguished: " + session.getFiresExtinguished()));
                SimulationFeedback.sendFire(player, session.logger, finalScore);
            } else if (session.getState().isQuake()) {
                SimulationFeedback.sendQuake(player, finalScore);
            }
            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
        } else {
            pendingLobbyRespawn.add(uuid);
        }
    }

    public static void placeAllBuildings(ServerLevel level) {
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());
    }

    public static SimulationSession getSession(java.util.UUID uuid) {
        return activeSessions.get(uuid);
    }

    public static Map<UUID, SimulationSession> getActiveSessions() {
        return java.util.Collections.unmodifiableMap(activeSessions);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            TutorialManager.tick(server.overworld());
        }

        // Snapshot key set before iterating — endSimulation removes entries and would ConcurrentModify.
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            SimulationSession session = activeSessions.get(uuid);
            if (session == null) continue;

            ServerPlayer player = session.getPlayer();
            if (player == null) continue;

            // Pre-sim countdown — show 3/2/1 before effects begin
            if (session.isInWarmup()) {
                if (!player.isAlive()) { endSimulation(uuid, "injured"); continue; }
                tickWarmupCountdown(session, player);
                session.tickWarmup();
                tickHudSync(session, player, session.getTimerTicks());
                continue;
            }

            // Post-sim cooldown — brief pause before lobby teleport
            if (session.isInCooldown()) {
                session.tickCooldown();
                if (session.isCooldownExpired()) { endSimulation(uuid); }
                continue;
            }

            if (!player.isAlive()) { endSimulation(uuid, "injured"); continue; }

            session.tick();

            if (session.isExpired()) {
                player.sendSystemMessage(Component.literal("§e⏱ Time's up! Returning to lobby..."));
                session.startCooldown();
                continue;
            }

            ServerLevel level = (ServerLevel) player.level();
            int ticks = session.getTimerTicks();

            tickTelemetry(session, uuid, level, player, ticks);

            if (session.getState().isFire()) {
                tickFireSession(session, level, player, ticks);
            } else if (session.getState().isQuake()) {
                tickEarthquakeSession(session, level, player, ticks);
            }

            tickExitZone(session, uuid, level, player, ticks);

            if (tickAssemblyZone(session, uuid, level, player, ticks)) continue;

            tickHudSync(session, player, ticks);
        }
    }

    private static void tickTelemetry(SimulationSession session, UUID uuid,
                                      ServerLevel level, ServerPlayer player, int ticks) {
        if (ticks % 20 == 0) {
            BlockPos pos = player.blockPosition();
            SimRoom room = session.getState().isCCS()
                    ? SimRoom.fromCCSPos(pos)
                    : SimRoom.fromPos(pos, SIM_POS);
            double nearestFire = nearestFireDistance(level, pos);
            session.logger.log("PLAYER_TICK", java.util.Map.of(
                "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                "room", room.name(),
                "nearest_fire_dist", Math.round(nearestFire * 10.0) / 10.0
            ));
        }
        if (ticks % 2 == 0) {
            double elapsedS = (double)(Config.SIM_DURATION_TICKS.get() - ticks) / 20.0;
            double hazDist = hazardDistance(session, level, player);
            session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                    session.getSessionId(), uuid.toString(),
                    session.getState().name().toLowerCase(),
                    Math.round(elapsedS * 100.0) / 100.0, "move_tick",
                    player.getX(), player.getY(), player.getZ(),
                    Math.round(hazDist * 100.0) / 100.0, null, null));
        }
    }

    private static void tickWarmupCountdown(SimulationSession session, ServerPlayer player) {
        int w = session.getWarmupTicks();
        if (w == 3 * 20) {
            player.sendSystemMessage(Component.literal("§e⏳ Simulation starts in §63..."));
        } else if (w == 2 * 20) {
            player.sendSystemMessage(Component.literal("§e⏳ Simulation starts in §62..."));
        } else if (w == 1 * 20) {
            player.sendSystemMessage(Component.literal("§c⏳ Simulation starts in §c1..."));
        } else if (w == 1) {
            player.sendSystemMessage(Component.literal("§a§l▶ Simulation has started!"));
        }
    }

    private static void tickFireSession(SimulationSession session, ServerLevel level,
                                        ServerPlayer player, int ticks) {
        if (ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
            if (session.getState().isCCS()) {
                int spreadBefore = session.getFireSpreadCount();
                EFFECTS.spreadComputerFire(level, session);
                int spreadAfter = session.getFireSpreadCount();
                if (spreadAfter > spreadBefore) {
                    sendCcsFireSpreadAlert(player, spreadBefore, spreadAfter);
                }
            } else {
                EFFECTS.simulateFire(level, session);
            }
        }
        if (ticks % 20 == 0) {
            EFFECTS.applyFireProximityEffects(level, player);
        }
        if (ticks % 40 == 0) {
            EFFECTS.cleanupFireOutsideBounds(level, session);
        }
        if (ticks % 20 == 0) {
            session.resetExtinguishEventPending();
        }
        HazardManager.tick(level, session, ticks);
        if (session.getState().isCCS()) {
            tickCcsFireNarrative(player, ticks);
        }
    }

    /** Sends a one-time alert when the total spread count crosses a dramatic threshold. */
    private static void sendCcsFireSpreadAlert(ServerPlayer player, int before, int after) {
        if (before == 0) {
            player.sendSystemMessage(Component.literal("§c⚠ The electrical fire is spreading to nearby workstations!"));
        } else if (before < 4 && after >= 4) {
            player.sendSystemMessage(Component.literal("§4⚠ More computers are catching fire! Suppress them before it's too late!"));
        } else if (before < 8 && after >= 8) {
            player.sendSystemMessage(Component.literal("§4§l⚠ CRITICAL — The lab is ablaze! Use your CO2 extinguisher immediately!"));
        }
    }

    /** Time-based narrative escalation for CCS fire, keyed to exact elapsed ticks. */
    private static void tickCcsFireNarrative(ServerPlayer player, int ticks) {
        int elapsed = Config.SIM_DURATION_TICKS.get() - ticks;
        if (elapsed == 20 * 15) {
            player.sendSystemMessage(Component.literal("§e[15s] The fire is still active — locate the burning computer!"));
        } else if (elapsed == 20 * 30) {
            player.sendSystemMessage(Component.literal("§c[30s] Electrical fires spread fast — check all workstations in the lab!"));
        } else if (elapsed == 20 * 55) {
            player.sendSystemMessage(Component.literal("§4[55s] ⚠ The fire has been burning for nearly a minute. Multiple stations may be at risk!"));
        } else if (elapsed == 20 * 90) {
            player.sendSystemMessage(Component.literal("§4§l[90s] DANGER — If you cannot control the fire, evacuate to the assembly area!"));
        }
    }

    private static void tickEarthquakeSession(SimulationSession session, ServerLevel level,
                                              ServerPlayer player, int ticks) {
        SimulationSession.EarthquakePhase phaseBefore = session.getQuakePhase();
        session.tickQuakePhase(level.getRandom());
        SimulationSession.EarthquakePhase phaseAfter = session.getQuakePhase();
        if (phaseBefore != phaseAfter) {
            if (phaseAfter == SimulationSession.EarthquakePhase.PEAK) {
                player.sendSystemMessage(Component.literal("§c⚠ Earthquake is intensifying!"));
            } else if (phaseAfter == SimulationSession.EarthquakePhase.AFTERSHOCK) {
                player.sendSystemMessage(Component.literal("§e⚠ Aftershock!"));
            } else if (phaseAfter == SimulationSession.EarthquakePhase.END) {
                player.sendSystemMessage(Component.literal("§a✓ The shaking has stopped."));
            }
        }
        if (ticks % 60 == 0 && session.getQuakePhase() != SimulationSession.EarthquakePhase.END) {
            int nauseaAmp = switch (session.getQuakePhase()) {
                case PEAK       -> (int) Math.min(3, session.getSessionMagnitude() / 2.5);
                case AFTERSHOCK -> (int) Math.min(2,
                        session.getSessionMagnitude() * session.getAftershockMagnitudeScale() / 3.0);
                default         -> 0;
            };
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, nauseaAmp, false, true));
        }
        if (ticks % Config.QUAKE_INTERVAL.get() == 0) {
            EFFECTS.simulateEarthquake(level, session);
        }
        EFFECTS.drainEarthquakePending(level, session);
        if (ticks % 20 == 0) {
            EFFECTS.clearFireInArena(level, session);
        }
    }

    private static void tickExitZone(SimulationSession session, UUID uuid,
                                     ServerLevel level, ServerPlayer player, int ticks) {
        if (session.hasPassedExit()) return;
        ExitZones.ExitZone exit = ExitZones.find(player.position(), session.getState().isCCS());
        if (exit == null) return;
        session.markPassedExit();
        double elapsedS   = (double)(Config.SIM_DURATION_TICKS.get() - ticks) / 20.0;
        double hazDist    = hazardDistance(session, level, player);
        double tRounded   = Math.round(elapsedS * 100.0) / 100.0;
        double hazRounded = Math.round(hazDist  * 100.0) / 100.0;
        session.logger.log("emergency_exit", java.util.Map.of(
                "t", tRounded, "x", player.getX(), "y", player.getY(), "z", player.getZ(),
                "exit", exit.label(), "hazard_distance", hazRounded));
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), uuid.toString(),
                session.getState().name().toLowerCase(),
                tRounded, "emergency_exit",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, exit.label(), null));
    }

    /** Returns true if simulation ended (caller should continue outer loop). */
    private static boolean tickAssemblyZone(SimulationSession session, UUID uuid,
                                            ServerLevel level, ServerPlayer player, int ticks) {
        if (ticks % 5 == 0) {
            AssemblyZone.spawnBorderParticles(level, session.getState().isCCS());
        }
        if (!session.hasReachedAssembly() && AssemblyZone.isInside(player.position(), session.getState().isCCS())) {
            session.markAssemblyReached();
            double hazDist = hazardDistance(session, level, player);
            AssemblyZone.onPlayerArrived(player, session, level, hazDist);
            endSimulation(uuid, "assembly_reached");
            return true;
        }
        return false;
    }

    private static void tickHudSync(SimulationSession session, ServerPlayer player, int ticks) {
        if (ticks % HUD_SYNC_INTERVAL_TICKS != 0) return;
        // Ceiling division so the HUD shows "1" on the last tick rather than jumping to "0".
        int secondsLeft = (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        float intensity = session.getState().isQuake()
                ? (float) session.computeIntensityAt(player.blockPosition())
                : 0f;
        PacketDistributor.sendToPlayer(player,
                new SimulationStatusPayload(session.getState().name(), secondsLeft, intensity));
    }

    private static double hazardDistance(SimulationSession session, ServerLevel level, ServerPlayer player) {
        if (session.getState() == SimulationState.CCS_FIRE) {
            return nearestCCSHazardDistance(level, player.blockPosition());
        }
        if (session.getState().isFire()) {
            return nearestFireDistance(level, player.blockPosition());
        }
        return session.getEpicenter() != null
                ? player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(session.getEpicenter()))
                : 99.0;
    }

    private static double nearestCCSHazardDistance(ServerLevel level, BlockPos origin) {
        double minSq = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(
                origin.offset(-CCS_HAZARD_RADIUS_XZ, -CCS_HAZARD_RADIUS_Y, -CCS_HAZARD_RADIUS_XZ),
                origin.offset( CCS_HAZARD_RADIUS_XZ,  CCS_HAZARD_RADIUS_Y,  CCS_HAZARD_RADIUS_XZ))) {
            net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(check);
            boolean isHazard = bs.is(net.minecraft.world.level.block.Blocks.FIRE)
                    || bs.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)
                    || (bs.getBlock() instanceof net.necookie.disastersim.block.ComputerBlock
                        && bs.getValue(net.necookie.disastersim.block.ComputerBlock.BURNING));
            if (isHazard) {
                double d = origin.distSqr(check);
                if (d < minSq) minSq = d;
            }
        }
        return minSq == Double.MAX_VALUE ? NO_HAZARD_DISTANCE : Math.sqrt(minSq);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!pendingLobbyRespawn.remove(player.getUUID())) return;

        ServerLevel level = (ServerLevel) player.level();
        PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0, 0f));
        player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
        player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
    }

    /**
     * Straight-line distance from {@code origin} to the nearest fire/soul-fire block within the
     * scan box, or {@link #NO_HAZARD_DISTANCE} if none. Memoised per (game-tick, position): the
     * common case of several callers asking for the same player on the same tick costs one scan.
     */
    public static double nearestFireDistance(ServerLevel level, BlockPos origin) {
        long gameTime  = level.getGameTime();
        long packedPos = origin.asLong();
        if (gameTime == fireScanTick && packedPos == fireScanPos) return fireScanVal;
        double result = computeNearestFireDistance(level, origin);
        fireScanTick = gameTime;
        fireScanPos  = packedPos;
        fireScanVal  = result;
        return result;
    }

    private static double computeNearestFireDistance(ServerLevel level, BlockPos origin) {
        double minSq = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(
                origin.offset(-FIRE_SCAN_RADIUS_XZ, -FIRE_SCAN_RADIUS_Y, -FIRE_SCAN_RADIUS_XZ),
                origin.offset( FIRE_SCAN_RADIUS_XZ,  FIRE_SCAN_RADIUS_Y,  FIRE_SCAN_RADIUS_XZ))) {
            net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(check);
            if (bs.is(net.minecraft.world.level.block.Blocks.FIRE) ||
                bs.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                double d = origin.distSqr(check);
                if (d < minSq) minSq = d;
            }
        }
        return minSq == Double.MAX_VALUE ? NO_HAZARD_DISTANCE : Math.sqrt(minSq);
    }

    private static BlockPos findRandomSpawnInLibrary(ServerLevel level) {
        int areaSize   = Config.SIM_AREA_SIZE.get();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();
        List<BlockPos> candidates = new ArrayList<>();
        // margin of 2 on XZ avoids spawning inside perimeter walls
        for (int dx = 2; dx < areaSize - 2; dx++) {
            for (int dz = 2; dz < areaSize - 2; dz++) {
                // dy starts at 1 so pos.below() is never below SIM_POS;
                // stops 2 below the ceiling so the head block check stays in-bounds
                for (int dy = 1; dy < areaHeight - 2; dy++) {
                    BlockPos pos = SIM_POS.offset(dx, dy, dz);
                    if (!level.getBlockState(pos.below()).isAir()
                            && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) {
                        candidates.add(pos);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return SIM_POS.offset((int) SIM_ENTRY_OFFSET_X, (int) SIM_ENTRY_OFFSET_Y, (int) SIM_ENTRY_OFFSET_Z);
        }
        return candidates.get(level.getRandom().nextInt(candidates.size()));
    }

    /**
     * Picks a random named room from either CCS floor and finds a valid spawn inside it.
     * Returns empty if no room on either floor has a solid-floor + 2-air-above position.
     */
    private static Optional<BlockPos> findSpawnInCcsNamedRoom(ServerLevel level) {
        net.minecraft.util.RandomSource random = level.getRandom();
        List<SimRoom.CcsRoom> all = new ArrayList<>(SimRoom.CCS_UPPER_ROOMS.size() + SimRoom.CCS_GROUND_ROOMS.size());
        all.addAll(SimRoom.CCS_UPPER_ROOMS);
        all.addAll(SimRoom.CCS_GROUND_ROOMS);
        List<SimRoom.CcsRoom> shuffled = all;
        Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
        for (SimRoom.CcsRoom room : shuffled) {
            List<BlockPos> candidates = new ArrayList<>();
            int xMin = (int) room.bounds().minX, xMax = (int) room.bounds().maxX;
            int yMin = (int) room.bounds().minY, yMax = (int) room.bounds().maxY;
            int zMin = (int) room.bounds().minZ, zMax = (int) room.bounds().maxZ;
            for (int x = xMin + 1; x < xMax; x++) {
                for (int z = zMin + 1; z < zMax; z++) {
                    for (int y = yMin; y < yMax; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!level.getBlockState(pos.below()).isAir()
                                && level.getBlockState(pos).isAir()
                                && level.getBlockState(pos.above()).isAir()) {
                            candidates.add(pos.immutable());
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                return Optional.of(candidates.get(random.nextInt(candidates.size())));
            }
        }
        return Optional.empty();
    }

    private static BlockPos findRandomSpawnInCCS(ServerLevel level) {
        // Only spawn inside a known named room — no blind arena scan.
        return findSpawnInCcsNamedRoom(level)
                .orElseGet(() -> new BlockPos(133, -24, 27)); // Computer Lab centre, absolute last resort
    }

    /** Scans the CCS arena for every computer and returns their positions. */
    static List<BlockPos> findComputersInCCS(ServerLevel level) {
        List<BlockPos> computers = new ArrayList<>();
        for (int dx = 0; dx < CCS_AREA_SPAN_X; dx++) {
            for (int dz = 0; dz < CCS_AREA_SPAN_Z; dz++) {
                for (int dy = 0; dy < CCS_AREA_HEIGHT; dy++) {
                    BlockPos pos = CCS_FIRE_BASE.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == BerongSMP.COMPUTER.get()) {
                        computers.add(pos.immutable());
                    }
                }
            }
        }
        return computers;
    }

    /** Sets `count` randomly chosen computers to BURNING=true. */
    private static void igniteRandomComputers(ServerLevel level,
                                              List<BlockPos> computers, int count,
                                              net.minecraft.util.RandomSource random) {
        if (computers.isEmpty()) return;
        List<BlockPos> pool = new ArrayList<>(computers);
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            int idx = random.nextInt(pool.size());
            BlockPos pos = pool.remove(idx);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ComputerBlock
                    && !state.getValue(ComputerBlock.BURNING)
                    && !state.getValue(ComputerBlock.BROKEN)) {
                level.setBlock(pos, state.setValue(ComputerBlock.BURNING, true), 3);
            }
        }
    }

    /** Returns true if pos is inside any named CCS room on either floor. */
    private static boolean isInCcsNamedRoom(BlockPos pos) {
        net.minecraft.world.phys.Vec3 v = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        for (SimRoom.CcsRoom room : SimRoom.CCS_UPPER_ROOMS) {
            if (room.bounds().contains(v)) return true;
        }
        for (SimRoom.CcsRoom room : SimRoom.CCS_GROUND_ROOMS) {
            if (room.bounds().contains(v)) return true;
        }
        return false;
    }

    /**
     * Finds a safe floor position near a burning computer that is also inside a
     * named 2nd floor room. Falls back to any named room if no computer is
     * adjacent to one.
     */
    private static BlockPos findSpawnNearComputer(ServerLevel level, List<BlockPos> computers) {
        if (computers.isEmpty()) return findRandomSpawnInCCS(level);
        net.minecraft.util.RandomSource random = level.getRandom();
        List<BlockPos> shuffled = new ArrayList<>(computers);
        Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
        for (BlockPos comp : shuffled) {
            for (int ddx = -3; ddx <= 3; ddx++) {
                for (int ddz = -3; ddz <= 3; ddz++) {
                    BlockPos candidate = comp.offset(ddx, 0, ddz);
                    if (isInCcsNamedRoom(candidate)
                            && !level.getBlockState(candidate.below()).isAir()
                            && level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()) {
                        return candidate;
                    }
                }
            }
        }
        return findRandomSpawnInCCS(level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();

        // Remove from pending-respawn before calling endSimulation: if the player was dead,
        // endSimulation would add them back to pendingLobbyRespawn, defeating this remove.
        pendingLobbyRespawn.remove(uuid);
        endSimulation(uuid);
    }
}
