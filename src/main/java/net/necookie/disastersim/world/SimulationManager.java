package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.network.SimulationStatusPayload;
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

/**
 * Central coordinator for the disaster simulation system.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintains the per-player session registry ({@link SimulationSession}).</li>
 *   <li>Drives the simulation clock via {@link #onServerTick}.</li>
 *   <li>Dispatches disaster effects each tick to {@link SimulationEffects}.</li>
 *   <li>Delegates structure load/restore to {@link SimulationStructureLoader}.</li>
 *   <li>Sends HUD sync packets to the client via {@link SimulationStatusPayload}.</li>
 * </ul>
 *
 * <p>Thread safety: {@code activeSessions} is a {@link ConcurrentHashMap} for
 * safe iteration and atomic key operations. {@code startSimulation} and
 * {@code endSimulation} are {@code synchronized} to prevent a race between a
 * button press and a concurrent logout event. All tick-driven mutations (e.g.,
 * {@link SimulationSession#tick()}) are confined to the server thread via
 * {@code ServerTickEvent.Post} and require no additional locking.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** World origin of the simulation arena (bottom-northwest corner of the LSPU Library). */
    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

    /** World origin of the SSC building placed alongside the simulation arena. */
    private static final BlockPos SSC_POS = new BlockPos(11, -33, 90);

    /**
     * How often the HUD sync packet is sent to the client (in ticks).
     * Kept separate from {@link Config#QUAKE_INTERVAL} so tuning the earthquake
     * rate does not silently change the HUD update frequency.
     */
    private static final int HUD_SYNC_INTERVAL_TICKS = 10;

    /** Divisor for converting server ticks to whole seconds. */
    private static final int TICKS_PER_SECOND = 20;

    /**
     * Player entry offsets relative to {@link #SIM_POS}, placing the player
     * just inside the structure's front door at ground level.
     */
    private static final double SIM_ENTRY_OFFSET_X = 5.5;
    private static final double SIM_ENTRY_OFFSET_Y = 2.0;
    private static final double SIM_ENTRY_OFFSET_Z = 5.5;

    // -----------------------------------------------------------------------
    // Collaborators and state
    // -----------------------------------------------------------------------

    /** One active session per player UUID; allows multiple players to run concurrently. */
    private static final Map<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Players who died during a simulation and still need to be sent to the lobby
     * on their next respawn. Populated by {@link #endSimulation} when the player
     * is dead; consumed and cleared by {@link #onPlayerRespawn}.
     */
    private static final Set<UUID> pendingLobbyRespawn = ConcurrentHashMap.newKeySet();

    /**
     * All buildings that are placed/restored together each simulation session.
     * Each entry pairs a loader with the world position it targets.
     */
    private static final List<Map.Entry<StructurePlacer, BlockPos>> BUILDINGS = List.of(
            Map.entry(new SimulationStructureLoader(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrarymain")), SIM_POS),
            Map.entry(new SchemLoader(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "structure/ssc_building.schem"), 1), SSC_POS)
    );

    /** Applies per-tick fire and earthquake world effects. */
    private static final SimulationEffects EFFECTS = new SimulationEffects();

    // -----------------------------------------------------------------------
    // Simulation state enum
    // -----------------------------------------------------------------------

    /** The type of disaster a {@link SimulationSession} is running. */
    public enum SimulationState {
        IDLE,
        FIRE,
        EARTHQUAKE
    }

    // -----------------------------------------------------------------------
    // Session lifecycle
    // -----------------------------------------------------------------------

    /**
     * Convenience overload that starts a simulation using the config-default magnitude.
     * Use the two-argument overload to pass an explicit magnitude (e.g., from button or command).
     */
    public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
        startSimulation(player, state, Config.QUAKE_MAGNITUDE.get());
    }

    /**
     * Starts a new simulation for the given player with an explicit earthquake magnitude.
     * For FIRE sessions the magnitude parameter is ignored.
     * If the player already has an active session, a message is sent and no new session is created.
     *
     * @param player    The player triggering the simulation.
     * @param state     The type of disaster to simulate.
     * @param magnitude Earthquake magnitude (0.1–10.0); only used for EARTHQUAKE sessions.
     */
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

        // Place all buildings so every session starts with clean, undamaged structures.
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());

        player.teleportTo(level,
                SIM_POS.getX() + SIM_ENTRY_OFFSET_X,
                SIM_POS.getY() + SIM_ENTRY_OFFSET_Y,
                SIM_POS.getZ() + SIM_ENTRY_OFFSET_Z,
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
    }

    /**
     * Ends and removes the active session for the given player UUID.
     * Always restores the simulation arena structure. Teleports and notifies the
     * player only if they are still alive on the server.
     *
     * <p>Thread safety: {@code synchronized} to guard against concurrent calls
     * from the tick event and logout events.
     *
     * @param uuid The UUID of the player whose session should be terminated.
     */
    public static synchronized void endSimulation(UUID uuid) {
        // Remove the session atomically.  If no session existed (e.g., called twice),
        // bail out immediately — nothing to clean up.
        SimulationSession session = activeSessions.remove(uuid);
        if (session == null) return;

        ServerPlayer player = session.getPlayer();
        // player should always be non-null because sessions hold a direct reference,
        // but guard anyway to avoid NPEs during edge-case shutdown sequences.
        if (player == null) return;

        // Restore all buildings so the arena is clean for the next player.
        ServerLevel level = (ServerLevel) player.level();
        for (var entry : BUILDINGS) entry.getKey().place(level, entry.getValue());

        if (player.isAlive()) {
            // --- Normal end (timer expired or /sim_stop) ---
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0, 0f));
            player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));

            // Send PASS score report for FIRE simulations.
            if (session.getState() == SimulationState.FIRE) {
                int fires = session.getFiresExtinguished();
                int score = Math.min(100, fires * 2);
                player.sendSystemMessage(Component.literal("§6--- Fire Drill Results ---"));
                player.sendSystemMessage(Component.literal("§eFires extinguished: " + fires));
                player.sendSystemMessage(Component.literal("§aScore: " + score + " / 100"));
                player.sendSystemMessage(Component.literal("§7Tip: Remember PASS — Pull, Aim at the base, Squeeze, Sweep side to side."));
            }

            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
        } else {
            // --- Death end ---
            // The player is in the death screen right now; teleportTo would be ignored.
            // Mark this UUID so that onPlayerRespawn can intercept the next respawn
            // event and redirect them to the lobby instead of the world spawn.
            pendingLobbyRespawn.add(uuid);
        }
    }

    // -----------------------------------------------------------------------
    // Session query
    // -----------------------------------------------------------------------

    /**
     * Returns the active {@link SimulationSession} for the given player UUID, or
     * {@code null} if no session is currently running for that player.
     * Used by {@code FireExtinguisherItem} to record extinguishes without coupling
     * to session internals directly.
     */
    public static SimulationSession getSession(java.util.UUID uuid) {
        return activeSessions.get(uuid);
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Advances all active simulation sessions once per server tick.
     * Runs on the server thread; no additional locking is required for
     * per-session mutations inside this method.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Snapshot the key set before iterating so that endSimulation (which removes
        // entries from activeSessions) does not cause a ConcurrentModificationException.
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            SimulationSession session = activeSessions.get(uuid);
            // The session may have been removed by a concurrent endSimulation call
            // (e.g., a logout event) between the snapshot and this iteration step.
            if (session == null) continue;

            // Decrement the session's internal tick counter by 1.
            session.tick();

            // If the timer hit zero, the simulation has run its full duration.
            // End it and move on to the next session.
            if (session.isExpired()) {
                endSimulation(uuid);
                continue;
            }

            ServerPlayer player = session.getPlayer();
            // Safety guard: if the player reference is gone or they are dead (the death
            // event hasn't fired yet), end the session now so we don't waste ticks on
            // a ghost session.  The dead-player case also triggers pendingLobbyRespawn
            // inside endSimulation so the respawn redirect still works.
            if (player == null || !player.isAlive()) {
                endSimulation(uuid);
                continue;
            }

            ServerLevel level = (ServerLevel) player.level();
            int ticks = session.getTimerTicks();

            // Dispatch the correct disaster effect at its configured interval.
            if (session.getState() == SimulationState.FIRE) {
                if (ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
                    EFFECTS.simulateFire(level);
                }
                // Smoke suffocation and proximity damage once per second
                if (ticks % 20 == 0) {
                    EFFECTS.applyFireProximityEffects(level, player);
                }
            } else if (session.getState() == SimulationState.EARTHQUAKE) {
                // Advance the phase state machine every tick and notify on transitions.
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
                // Nausea/dizziness from seismic vibrations — stronger during PEAK
                if (ticks % 60 == 0 && session.getQuakePhase() != SimulationSession.EarthquakePhase.END) {
                    int nauseaAmp = switch (session.getQuakePhase()) {
                        case PEAK       -> (int) Math.min(3, session.getSessionMagnitude() / 2.5);
                        case AFTERSHOCK -> (int) Math.min(2,
                                session.getSessionMagnitude() * session.getAftershockMagnitudeScale() / 3.0);
                        default         -> 0;
                    };
                    player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, nauseaAmp, false, true));
                }
                // Periodic effect (fills cascade queue or breaks blocks directly).
                if (ticks % Config.QUAKE_INTERVAL.get() == 0) {
                    EFFECTS.simulateEarthquake(level, session);
                }
                // Drain cascade queue every tick for gradual PEAK-phase collapse.
                EFFECTS.drainEarthquakePending(level, session);
                // Suppress vanilla fire spreading triggered by block destruction.
                if (ticks % 20 == 0) {
                    EFFECTS.clearFireInArena(level);
                }
            }

            // Vanilla fire spreading (FireBlock#tick) can move fire outside the
            // simulation arena without triggering any hookable NeoForge event.
            // Every 40 ticks (2 seconds) we scan the border region and remove
            // any fire that escaped the structure's bounding box.
            if (session.getState() == SimulationState.FIRE && ticks % 40 == 0) {
                EFFECTS.cleanupFireOutsideBounds(level);
            }

            // Send an updated HUD packet at HUD_SYNC_INTERVAL_TICKS (every 10 ticks = 0.5s).
            // Ceiling division converts ticks to whole seconds so the HUD timer
            // shows "1" on the last tick rather than jumping straight to "0".
            if (ticks % HUD_SYNC_INTERVAL_TICKS == 0) {
                int secondsLeft = (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
                float intensity = (session.getState() == SimulationState.EARTHQUAKE)
                        ? (float) session.computeIntensityAt(player.blockPosition())
                        : 0f;
                PacketDistributor.sendToPlayer(player,
                        new SimulationStatusPayload(session.getState().name(), secondsLeft, intensity));
            }
        }
    }

    /**
     * Sends players who died mid-simulation to the lobby when they respawn.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Filter: we only care about server-side players.
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // pendingLobbyRespawn.remove returns true only if the UUID was present.
        // If this player didn't die during a simulation, the set won't contain their
        // UUID and we fall through without interfering with a normal respawn.
        if (!pendingLobbyRespawn.remove(player.getUUID())) return;

        // At this point the player has clicked "Respawn" and is alive again.
        // Redirect them to the lobby instead of the world spawn.
        ServerLevel level = (ServerLevel) player.level();

        // Clear the HUD (empty status, 0 seconds) so the simulation overlay disappears.
        PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0, 0f));
        player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));

        // Teleport to the lobby spawn point.
        player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
    }

    /**
     * Ends a player's active simulation when they disconnect.
     * Also clears any pending respawn redirect so it does not fire on next login.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();

        // Remove from the pending-respawn set first.  If we called endSimulation first
        // and the player was dead, endSimulation would add them to pendingLobbyRespawn —
        // and then this remove would still work, but ordering is cleaner this way.
        pendingLobbyRespawn.remove(uuid);

        // End (and clean up) the session if one exists.  Safe to call even if the
        // player had no active session — endSimulation is a no-op in that case.
        endSimulation(uuid);
    }
}
