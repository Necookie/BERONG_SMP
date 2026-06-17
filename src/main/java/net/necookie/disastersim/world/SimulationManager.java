package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    /** Namespaced path of the LSPU Library NBT structure template. */
    private static final Identifier STRUCTURE_ID =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrarymain");

    /** World origin of the simulation arena (bottom-northwest corner of the structure). */
    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

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

    /** Loads and places the LSPU Library structure when a simulation starts or ends. */
    private static final SimulationStructureLoader STRUCTURE_LOADER =
            new SimulationStructureLoader(STRUCTURE_ID);

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
     * Starts a new simulation for the given player.
     * If the player already has an active session, a message is sent and no new
     * session is created.
     *
     * <p>Thread safety: {@code synchronized} to guard against concurrent calls
     * from button-press and other server-thread events.
     *
     * @param player The player triggering the simulation.
     * @param state  The type of disaster to simulate.
     */
    public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
        UUID uuid = player.getUUID();

        // Prevent a player from starting a second session while one is already running.
        // This can happen if they click the button twice quickly or via a command.
        if (activeSessions.containsKey(uuid)) {
            player.sendSystemMessage(Component.literal("You already have a simulation in progress!"));
            return;
        }

        // Create and register the session.  SimulationSession initialises the timer
        // from Config.SIM_DURATION_TICKS so it can be tuned without recompiling.
        activeSessions.put(uuid, new SimulationSession(player, state));

        ServerLevel level = (ServerLevel) player.level();

        // (Re-)place the library NBT structure so that every session starts with a
        // clean, undamaged building — regardless of damage from the previous run.
        STRUCTURE_LOADER.placeStructure(level, SIM_POS);

        // Teleport the player just inside the front door of the structure.
        // The offset puts them 5.5 blocks east and 5.5 blocks south of the structure
        // origin (SIM_POS), landing them at ground level (Y + 2) inside the entrance.
        player.teleportTo(level,
                SIM_POS.getX() + SIM_ENTRY_OFFSET_X,
                SIM_POS.getY() + SIM_ENTRY_OFFSET_Y,
                SIM_POS.getZ() + SIM_ENTRY_OFFSET_Z,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.literal("Starting " + state.name() + " Simulation!"));
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

        // Re-place the library structure unconditionally so the arena is clean for
        // the next player, even if this session ended via death or disconnect.
        ServerLevel level = (ServerLevel) player.level();
        STRUCTURE_LOADER.placeStructure(level, SIM_POS);

        if (player.isAlive()) {
            // --- Normal end (timer expired or /sim_stop) ---
            // Clear the HUD by sending an empty status with 0 seconds left.
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0));
            player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
            // Teleport the player back to the lobby spawn point.
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
            // 'ticks % interval == 0' fires once every N ticks — e.g., with the
            // default FIRE_SPAWN_INTERVAL of 20, fire spawns once per second.
            if (session.getState() == SimulationState.FIRE
                    && ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
                EFFECTS.simulateFire(level);
            } else if (session.getState() == SimulationState.EARTHQUAKE
                    && ticks % Config.QUAKE_INTERVAL.get() == 0) {
                EFFECTS.simulateEarthquake(level);
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
                PacketDistributor.sendToPlayer(player,
                        new SimulationStatusPayload(session.getState().name(), secondsLeft));
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
        PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0));
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
