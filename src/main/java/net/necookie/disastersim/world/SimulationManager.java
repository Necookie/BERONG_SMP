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
        if (activeSessions.containsKey(uuid)) {
            player.sendSystemMessage(Component.literal("You already have a simulation in progress!"));
            return;
        }

        activeSessions.put(uuid, new SimulationSession(player, state));

        ServerLevel level = (ServerLevel) player.level();
        STRUCTURE_LOADER.placeStructure(level, SIM_POS);
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
        SimulationSession session = activeSessions.remove(uuid);
        if (session == null) return;

        ServerPlayer player = session.getPlayer();
        if (player == null) return;

        // Always restore the arena, even if the player died or disconnected,
        // so the structure is clean for the next participant.
        ServerLevel level = (ServerLevel) player.level();
        STRUCTURE_LOADER.placeStructure(level, SIM_POS);

        if (player.isAlive()) {
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0));
            player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
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
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            SimulationSession session = activeSessions.get(uuid);
            if (session == null) continue;

            session.tick();
            if (session.isExpired()) {
                endSimulation(uuid);
                continue;
            }

            ServerPlayer player = session.getPlayer();
            if (player == null || !player.isAlive()) {
                endSimulation(uuid);
                continue;
            }

            ServerLevel level = (ServerLevel) player.level();
            int ticks = session.getTimerTicks();

            if (session.getState() == SimulationState.FIRE
                    && ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
                EFFECTS.simulateFire(level);
            } else if (session.getState() == SimulationState.EARTHQUAKE
                    && ticks % Config.QUAKE_INTERVAL.get() == 0) {
                EFFECTS.simulateEarthquake(level);
            }

            if (ticks % HUD_SYNC_INTERVAL_TICKS == 0) {
                // Ceiling division converts ticks to whole seconds so the display
                // never shows 0 while time remains.
                int secondsLeft = (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
                PacketDistributor.sendToPlayer(player,
                        new SimulationStatusPayload(session.getState().name(), secondsLeft));
            }
        }
    }

    /**
     * Ends a player's active simulation when they disconnect.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        endSimulation(event.getEntity().getUUID());
    }
}
