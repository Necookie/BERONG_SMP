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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {

    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

    private static final BlockPos SSC_POS = new BlockPos(11,  -33, 90);
    private static final BlockPos CCS_POS = new BlockPos(76, -34, 4);

    // Separate from QUAKE_INTERVAL so tuning earthquake rate doesn't silently change HUD update frequency.
    private static final int HUD_SYNC_INTERVAL_TICKS = 10;

    private static final int TICKS_PER_SECOND = 20;

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
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "structure/ccs_admin_building2.schem"), 1), CCS_POS)
    );

    private static final SimulationEffects EFFECTS = new SimulationEffects();

    public enum SimulationState {
        IDLE,
        FIRE,
        EARTHQUAKE
    }

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

        ServerLevel level = (ServerLevel) player.level();
        if (state == SimulationState.EARTHQUAKE) {
            session.initEarthquake(level.getRandom(), magnitude);
        }

        session.logger.log("SIM_START", java.util.Map.of(
            "sim_type", state.name(),
            "magnitude", state == SimulationState.EARTHQUAKE ? magnitude : 0.0
        ));

        // Place all buildings so every session starts with clean, undamaged structures.
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());
        TelemetryCsvWriter.scanAndRegisterFireAlarms(level, SIM_POS);

        BlockPos spawnPos = findRandomSpawnInLibrary(level);
        player.teleportTo(level,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);

        if (state == SimulationState.FIRE) {
            ItemStack extinguisher = new ItemStack(BerongSMP.FIRE_EXTINGUISHER.get());
            player.getInventory().setItem(0, extinguisher);
            player.sendSystemMessage(Component.literal("§eYou have been given a Fire Extinguisher in slot 1. Remember: Pull the pin first before spraying! (PASS)"));
            player.sendSystemMessage(Component.literal("§6Starting FIRE Simulation!"));
        } else if (state == SimulationState.EARTHQUAKE) {
            player.sendSystemMessage(Component.literal(
                    String.format("§c⚠ Magnitude %.1f Earthquake has begun! Brace for impact!", magnitude)));
        }

        // --- Telemetry: session_start (t=0 anchor) ---
        double startHazDist = (state == SimulationState.FIRE)
                ? 99.0
                : (session.getEpicenter() != null
                        ? player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(session.getEpicenter()))
                        : 99.0);
        TelemetryCsvWriter.openSession(session.getSessionId());
        TelemetryCsvWriter.writeRow(
                session.getSessionId(), uuid.toString(), state.name().toLowerCase(),
                0.0, "session_start",
                player.getX(), player.getY(), player.getZ(),
                startHazDist, null, null);
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
        if (session.getState() == SimulationState.FIRE && event.getLevel() instanceof ServerLevel sl) {
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
        TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                tRounded, "door_open",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, targetName, null);
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
        if (session.getState() == SimulationState.FIRE) {
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
            TelemetryCsvWriter.writeRow(
                    session.getSessionId(), uuid.toString(),
                    session.getState().name().toLowerCase(),
                    Math.round(elapsedT * 100.0) / 100.0, "session_end",
                    playerForCsv.getX(), playerForCsv.getY(), playerForCsv.getZ(),
                    Math.round(endHazDist * 100.0) / 100.0, null, null);
        }
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("player_id",                    uuid.toString());
        meta.put("scenario_type",                session.getState().name().toLowerCase());
        meta.put("started_at",                   session.getStartedAt().toString());
        meta.put("ended_at",                     java.time.Instant.now().toString());
        meta.put("duration_ticks",               Config.SIM_DURATION_TICKS.get() - session.getTimerTicks());
        meta.put("end_reason",                   endReason);
        meta.put("fires_extinguished_count",     session.getFiresExtinguished());
        meta.put("magnitude",                    session.getState() == SimulationState.EARTHQUAKE
                                                     ? session.getSessionMagnitude() : "");
        meta.put("aftershock_count",             session.getState() == SimulationState.EARTHQUAKE
                                                     ? session.getAftershockCount() : "");
        meta.put("aftershock_magnitude_scale",   session.getState() == SimulationState.EARTHQUAKE
                                                     ? session.getAftershockMagnitudeScale() : "");
        meta.put("final_earthquake_phase",       session.getQuakePhase() != null
                                                     ? session.getQuakePhase().name() : "");
        TelemetryCsvWriter.closeSession(session.getSessionId(), meta);
        TelemetryCsvWriter.flush();

        if (TursoClient.isReady()) {
            String simType = session.getState() == SimulationState.FIRE ? "FIRE" : "EARTHQUAKE";
            boolean passed = session.getState() == SimulationState.FIRE
                    && finalScore >= net.necookie.disastersim.Config.PASS_THRESHOLD_FIRE.get();
            net.necookie.disastersim.BerongSMP.LOGGER.info(
                    "[SimulationManager] endSimulation uuid={} simType={} score={}", uuid, simType, finalScore);
            TursoClient.executeAsync(
                    "UPDATE sessions SET simulation_type=?, simulation_score=?, passed=?," +
                    " end_time=?, status='completed', event_log=?" +
                    " WHERE id=(SELECT id FROM sessions WHERE account_uuid=? AND status='active'" +
                    " ORDER BY id DESC LIMIT 1)",
                    simType, finalScore, passed,
                    java.time.Instant.now().toString(),
                    session.logger.toJson(),
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
            if (session.getState() == SimulationState.FIRE) {
                player.sendSystemMessage(Component.literal(
                    "§eFires extinguished: " + session.getFiresExtinguished()));
                SimulationFeedback.sendFire(player, session.logger, finalScore);
            } else if (session.getState() == SimulationState.EARTHQUAKE) {
                SimulationFeedback.sendQuake(player, finalScore);
            }
            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
        } else {
            pendingLobbyRespawn.add(uuid);
        }
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

            session.tick();

            if (session.isExpired()) { endSimulation(uuid); continue; }

            ServerPlayer player = session.getPlayer();
            if (player == null || !player.isAlive()) { endSimulation(uuid, "injured"); continue; }

            ServerLevel level = (ServerLevel) player.level();
            int ticks = session.getTimerTicks();

            tickTelemetry(session, uuid, level, player, ticks);

            if (session.getState() == SimulationState.FIRE) {
                tickFireSession(session, level, player, ticks);
            } else if (session.getState() == SimulationState.EARTHQUAKE) {
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
            SimRoom room = SimRoom.fromPos(pos, SIM_POS);
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
            TelemetryCsvWriter.writeRow(
                    session.getSessionId(), uuid.toString(),
                    session.getState().name().toLowerCase(),
                    Math.round(elapsedS * 100.0) / 100.0, "move",
                    player.getX(), player.getY(), player.getZ(),
                    Math.round(hazDist * 100.0) / 100.0, null, null);
        }
    }

    private static void tickFireSession(SimulationSession session, ServerLevel level,
                                        ServerPlayer player, int ticks) {
        if (ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
            EFFECTS.simulateFire(level, session);
        }
        if (ticks % 20 == 0) {
            EFFECTS.applyFireProximityEffects(level, player);
        }
        if (ticks % 40 == 0) {
            EFFECTS.cleanupFireOutsideBounds(level);
        }
        if (ticks % 20 == 0) {
            session.resetExtinguishEventPending();
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
            EFFECTS.clearFireInArena(level);
        }
    }

    private static void tickExitZone(SimulationSession session, UUID uuid,
                                     ServerLevel level, ServerPlayer player, int ticks) {
        if (session.hasPassedExit()) return;
        ExitZones.ExitZone exit = ExitZones.find(player.position());
        if (exit == null) return;
        session.markPassedExit();
        double elapsedS   = (double)(Config.SIM_DURATION_TICKS.get() - ticks) / 20.0;
        double hazDist    = hazardDistance(session, level, player);
        double tRounded   = Math.round(elapsedS * 100.0) / 100.0;
        double hazRounded = Math.round(hazDist  * 100.0) / 100.0;
        session.logger.log("emergency_exit", java.util.Map.of(
                "t", tRounded, "x", player.getX(), "y", player.getY(), "z", player.getZ(),
                "exit", exit.label(), "hazard_distance", hazRounded));
        TelemetryCsvWriter.writeRow(
                session.getSessionId(), uuid.toString(),
                session.getState().name().toLowerCase(),
                tRounded, "emergency_exit",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, exit.label(), null);
    }

    /** Returns true if simulation ended (caller should continue outer loop). */
    private static boolean tickAssemblyZone(SimulationSession session, UUID uuid,
                                            ServerLevel level, ServerPlayer player, int ticks) {
        if (ticks % 5 == 0) {
            AssemblyZone.spawnBorderParticles(level);
        }
        if (!session.hasReachedAssembly() && AssemblyZone.isInside(player.position())) {
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
        float intensity = (session.getState() == SimulationState.EARTHQUAKE)
                ? (float) session.computeIntensityAt(player.blockPosition())
                : 0f;
        PacketDistributor.sendToPlayer(player,
                new SimulationStatusPayload(session.getState().name(), secondsLeft, intensity));
    }

    private static double hazardDistance(SimulationSession session, ServerLevel level, ServerPlayer player) {
        if (session.getState() == SimulationState.FIRE) {
            return nearestFireDistance(level, player.blockPosition());
        }
        return session.getEpicenter() != null
                ? player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(session.getEpicenter()))
                : 99.0;
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

    public static double nearestFireDistance(ServerLevel level, BlockPos origin) {
        double minSq = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(origin.offset(-10, -5, -10), origin.offset(10, 5, 10))) {
            net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(check);
            if (bs.is(net.minecraft.world.level.block.Blocks.FIRE) ||
                bs.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                double d = origin.distSqr(check);
                if (d < minSq) minSq = d;
            }
        }
        return minSq == Double.MAX_VALUE ? 99.0 : Math.sqrt(minSq);
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

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();

        // Remove from pending-respawn before calling endSimulation: if the player was dead,
        // endSimulation would add them back to pendingLobbyRespawn, defeating this remove.
        pendingLobbyRespawn.remove(uuid);
        endSimulation(uuid);
    }
}
