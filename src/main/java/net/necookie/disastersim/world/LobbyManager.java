package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the simulation lobby: structure placement, player spawn, and button detection.
 *
 * <p>On server start, {@link #createLobby} places the lobby NBT structure and scans
 * it for {@link ButtonBlock} instances. The two buttons found are assigned as the
 * fire-simulation trigger (lower Z) and earthquake-simulation trigger (higher Z).
 *
 * <p>Event handlers on the NeoForge bus:
 * <ul>
 *   <li>{@link #onPlayerLogin} — teleports every connecting player to the lobby spawn.</li>
 *   <li>{@link #onRightClickBlock} — detects button presses and starts the corresponding
 *       simulation via {@link SimulationManager}.</li>
 * </ul>
 *
 * <p>Thread safety: {@code lobbyReady}, {@code fireButtonPos}, and {@code quakeButtonPos}
 * are written once during server startup (single-threaded) and thereafter only read from
 * the server thread via event handlers — no synchronisation is required.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {

    /** World origin of the lobby structure (bottom-northwest corner). */
    // Y matches the LSPU Library structure so both areas sit at the same elevation.
    public static final BlockPos LOBBY_POS = new BlockPos(0, -33, 0);

    /**
     * Exact player spawn coordinates inside the lobby.
     * Used on login and when returning from a completed simulation.
     */
    public static final double SPAWN_X = 8.8;
    public static final double SPAWN_Y = -31.0;
    public static final double SPAWN_Z = 8.0;

    /** Resource path of the lobby NBT structure file. */
    private static final Identifier LOBBY_STRUCTURE_ID =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lobby_structure");

    // Discovered at load time by scanning the placed structure for buttons.
    // Sorted by ascending Z: lower Z = fire trigger, higher Z = earthquake trigger.
    private static BlockPos fireButtonPos  = null;
    private static BlockPos quakeButtonPos = null;

    /** Set to {@code true} once the structure is placed and buttons are discovered. */
    private static boolean lobbyReady = false;

    // -----------------------------------------------------------------------
    // Lobby initialisation
    // -----------------------------------------------------------------------

    /**
     * Places the lobby NBT structure in the world and discovers the trigger buttons.
     * Should be called once during {@code ServerStartingEvent}.
     *
     * @param level The overworld server level to place the lobby in.
     */
    public static void createLobby(ServerLevel level) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(LOBBY_STRUCTURE_ID);

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            // Flags: 2 = UPDATE_CLIENTS | UPDATE_NOTIFY
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            template.placeInWorld(level, LOBBY_POS, LOBBY_POS, settings, level.getRandom(), 2);
            scanForButtons(level, LOBBY_POS, template.getSize());
            lobbyReady = true;
            BerongSMP.LOGGER.info("BerongSMP Lobby loaded from NBT at {}", LOBBY_POS);
        } else {
            BerongSMP.LOGGER.error("Failed to load lobby structure: {}", LOBBY_STRUCTURE_ID);
        }
    }

    /**
     * Scans the placed structure's bounding box for {@link ButtonBlock} instances
     * and assigns the lowest-Z button as the fire trigger and the next as the
     * earthquake trigger.
     *
     * <p>If fewer than two buttons are found, the missing trigger is left as
     * {@code null} and a warning is logged so the issue is visible in the console.
     *
     * @param level  The level to scan.
     * @param origin The placement origin of the structure.
     * @param size   The size of the structure template in blocks.
     */
    private static void scanForButtons(ServerLevel level, BlockPos origin, Vec3i size) {
        List<BlockPos> buttons = new ArrayList<>();

        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockState(pos).getBlock() instanceof ButtonBlock) {
                        buttons.add(pos.immutable());
                    }
                }
            }
        }

        // Sort ascending by Z so the button with the lower Z coordinate is the fire trigger.
        buttons.sort(Comparator.comparingInt(BlockPos::getZ));

        fireButtonPos  = buttons.size() >= 1 ? buttons.get(0) : null;
        quakeButtonPos = buttons.size() >= 2 ? buttons.get(1) : null;

        if (buttons.size() < 2) {
            BerongSMP.LOGGER.warn(
                    "Lobby button scan found only {} button(s); expected 2. "
                    + "Fire={}, Quake={}. Check the lobby_structure NBT.",
                    buttons.size(), fireButtonPos, quakeButtonPos);
        } else {
            BerongSMP.LOGGER.info("Lobby buttons found: {} total. Fire={}, Quake={}",
                    buttons.size(), fireButtonPos, quakeButtonPos);
        }
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Teleports every player to the lobby spawn point the moment they log in.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
    }

    /**
     * Detects right-clicks on the lobby trigger buttons and starts the corresponding
     * simulation for the interacting player.
     *
     * <p>If the lobby structure has not finished loading, a message is sent to the
     * player and the click is ignored.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!lobbyReady) {
            player.sendSystemMessage(
                Component.literal("The lobby is still loading, please wait a moment."));
            return;
        }

        BlockPos pos = event.getPos();

        if (fireButtonPos != null && pos.equals(fireButtonPos)) {
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.FIRE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (quakeButtonPos != null && pos.equals(quakeButtonPos)) {
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.EARTHQUAKE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
