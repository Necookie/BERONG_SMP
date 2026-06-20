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
import net.minecraft.world.entity.npc.villager.Villager;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.registration.RegistrationManager;
import net.necookie.disastersim.tutorial.NpcRole;
import net.necookie.disastersim.tutorial.TutorialManager;
import net.necookie.disastersim.tutorial.TutorialSavedData;
import net.necookie.disastersim.tutorial.TutorialStage;
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
        // StructureTemplateManager loads .nbt files from
        // src/main/resources/data/<modid>/structure/<name>.nbt at runtime.
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(LOBBY_STRUCTURE_ID);

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();

            // No mirroring or rotation — the lobby is designed to face its default direction.
            // setIgnoreEntities(false) preserves any entities stored in the NBT
            // (e.g., item frames, armour stands used as props).
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            // Place the structure. The second argument (pivot) and third argument (pos)
            // are both LOBBY_POS here — the pivot is used to apply rotation/mirror offset,
            // which is irrelevant since we use NONE for both.
            // Flag 2 = UPDATE_CLIENTS | UPDATE_NOTIFY: notifies neighbouring blocks and
            // sends block change packets to connected players.
            template.placeInWorld(level, LOBBY_POS, LOBBY_POS, settings, level.getRandom(), 2);

            // Scan the bounding box of the placed structure for ButtonBlock instances
            // so we know which positions to listen for in onRightClickBlock.
            scanForButtons(level, LOBBY_POS, template.getSize());
            lobbyReady = true;
            BerongSMP.LOGGER.info("BerongSMP Lobby loaded from NBT at {}", LOBBY_POS);
        } else {
            // This means the .nbt file is missing from the resources folder.
            // Players won't see a lobby, and button clicks won't trigger simulations.
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

        // Walk every block position inside the structure's bounding box.
        // size.getX/Y/Z() returns the total block count on each axis, not the max coordinate.
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    // ButtonBlock is the common superclass for all button types
                    // (stone, oak, birch, etc.), so this catches any button variant.
                    if (level.getBlockState(pos).getBlock() instanceof ButtonBlock) {
                        // pos.immutable() is important: BlockPos.offset() can return a
                        // mutable MutableBlockPos that gets reused in the next iteration,
                        // so we must snapshot it before storing.
                        buttons.add(pos.immutable());
                    }
                }
            }
        }

        // Sort buttons by their Z coordinate (ascending).
        // Convention: the button physically closer to Z=0 (lower Z) is the fire trigger,
        // and the one further along Z is the earthquake trigger.
        // This relies on the lobby_structure NBT placing the fire button before the quake button
        // along the Z axis — if the NBT is redesigned, update this sorting key accordingly.
        buttons.sort(Comparator.comparingInt(BlockPos::getZ));

        fireButtonPos  = buttons.size() >= 1 ? buttons.get(0) : null;
        quakeButtonPos = buttons.size() >= 2 ? buttons.get(1) : null;

        if (buttons.size() < 2) {
            // Fewer than 2 buttons means the lobby NBT is missing button blocks.
            // Simulations can still be started via /sim_fire and /sim_earthquake commands.
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
     * Routes the connecting player to the tutorial lobby or main lobby based on tutorial progress.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        routePlayer(player, level);
    }

    /**
     * Routes a respawning player back to the correct lobby. Tutorial players land in the
     * BFP tutorial lobby; completed-tutorial players land in the main lobby.
     * SimulationManager handles respawn for players who died mid-simulation separately.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TutorialLobbyManager.needsTutorialLobby(player.getUUID())) return;

        ServerLevel level = (ServerLevel) player.level();
        routePlayer(player, level);
    }

    /**
     * Intercepts right-clicks on BFP tutorial NPCs (Villagers tagged with {@code bfp_role})
     * and dispatches dialogue to {@link TutorialManager#onNpcInteract}, cancelling the
     * vanilla villager trading GUI.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Villager villager)) return;

        if (!villager.getPersistentData().contains(TutorialLobbyManager.NPC_ROLE_TAG)) return;

        String roleName = villager.getPersistentData().getString(TutorialLobbyManager.NPC_ROLE_TAG).orElse("");
        if (roleName.isEmpty()) return;

        NpcRole role;
        try {
            role = NpcRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        TutorialManager.onNpcInteract(player, role);
    }

    /** Sends player to tutorial lobby or main lobby depending on tutorial completion state. */
    private static void routePlayer(ServerPlayer player, ServerLevel level) {
        if (TutorialLobbyManager.needsTutorialLobby(player.getUUID())) {
            player.teleportTo(level,
                TutorialLobbyManager.TSPAWN_X, TutorialLobbyManager.TSPAWN_Y, TutorialLobbyManager.TSPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
            // Re-give extinguisher and fires if player reconnected mid-PASS_SPRAY stage
            if (TutorialSavedData.get(level).getStage(player.getUUID()) == TutorialStage.PASS_SPRAY) {
                TutorialManager.spawnPracticeFires(level);
                TutorialManager.giveExtinguisher(player);
            }
        } else {
            player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
        }
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
        // This event fires on both sides; only run server-side logic here.
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // If createLobby hasn't completed yet (e.g., server still starting), ignore
        // the click to prevent starting a simulation against an incomplete structure.
        if (!lobbyReady) {
            player.sendSystemMessage(
                Component.literal("The lobby is still loading, please wait a moment."));
            return;
        }

        BlockPos pos = event.getPos();

        if (fireButtonPos != null && pos.equals(fireButtonPos)) {
            if (!RegistrationManager.isRegistered(player)) {
                player.sendSystemMessage(Component.literal("§cPlease /register first! Usage: /register <student_id> <section> <full_name>"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            if (!TutorialManager.isComplete(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cComplete the safety tutorial first!"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            // Player right-clicked the fire simulation button.
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.FIRE);
            // Mark the event as SUCCESS and cancel it so the button doesn't also
            // play its vanilla click animation (it has no functional meaning here).
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (quakeButtonPos != null && pos.equals(quakeButtonPos)) {
            if (!RegistrationManager.isRegistered(player)) {
                player.sendSystemMessage(Component.literal("§cPlease /register first! Usage: /register <student_id> <section> <full_name>"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            if (!TutorialManager.isComplete(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cComplete the safety tutorial first!"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            // Pick a random strong magnitude (6.0–9.5) so each button-triggered quake feels different.
            double magnitude = 6.0 + event.getLevel().getRandom().nextDouble() * 3.5;
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.EARTHQUAKE, magnitude);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
        // Any other block right-clicked in the lobby passes through normally.
    }
}
