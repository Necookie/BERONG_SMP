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

@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {

    // Y=-33 matches the LSPU Library structure so both areas sit at the same elevation.
    public static final BlockPos LOBBY_POS = new BlockPos(0, -33, 0);

    public static final double SPAWN_X = 8.8;
    public static final double SPAWN_Y = -31.0;
    public static final double SPAWN_Z = 8.0;

    private static final Identifier LOBBY_STRUCTURE_ID =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lobby_structure");

    // Sorted by ascending Z: lower Z = fire trigger, higher Z = earthquake trigger.
    private static BlockPos fireButtonPos  = null;
    private static BlockPos quakeButtonPos = null;

    private static boolean lobbyReady = false;

    public static void createLobby(ServerLevel level) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(LOBBY_STRUCTURE_ID);

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();

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

    private static void scanForButtons(ServerLevel level, BlockPos origin, Vec3i size) {
        List<BlockPos> buttons = new ArrayList<>();

        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockState(pos).getBlock() instanceof ButtonBlock) {
                        // pos.immutable() required — offset() returns a MutableBlockPos reused each iteration.
                        buttons.add(pos.immutable());
                    }
                }
            }
        }

        // Sorted ascending Z: lower Z = fire trigger, higher Z = earthquake trigger.
        // Relies on the lobby NBT placing the fire button at a lower Z — update sort key if NBT changes.
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

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        routePlayer(player, level);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TutorialLobbyManager.needsTutorialLobby(player.getUUID())) return;

        ServerLevel level = (ServerLevel) player.level();
        routePlayer(player, level);
    }

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
            if (!gatesPassed(player, event)) return;
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.CCS_FIRE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (quakeButtonPos != null && pos.equals(quakeButtonPos)) {
            if (!gatesPassed(player, event)) return;
            // Pick a random strong magnitude (6.0–9.5) so each button-triggered quake feels different.
            double magnitude = 6.0 + event.getLevel().getRandom().nextDouble() * 3.5;
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.CCS_EARTHQUAKE, magnitude);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * Runs the lobby entry gates (registration → active session → tutorial complete), unless the
     * player has an admin test-bypass. Returns {@code true} if the player may start a simulation;
     * otherwise messages the reason, cancels the interaction with FAIL, and returns {@code false}.
     */
    private static boolean gatesPassed(ServerPlayer player, PlayerInteractEvent.RightClickBlock event) {
        if (net.necookie.disastersim.command.BfpAdminCommands.isTestBypass(player.getUUID())) return true;
        if (!RegistrationManager.isRegistered(player)) {
            return denyGate(player, event,
                "§cPlease /register first! Usage: /register <student_id> <section> <full_name>");
        }
        if (net.necookie.disastersim.session.SessionManager.getActiveSession(player.getUUID()) == null) {
            return denyGate(player, event,
                "§cYour session has expired. Type §f/register <id> <section> <name>§c again to start a new one.");
        }
        if (!TutorialManager.isComplete(player.getUUID())) {
            return denyGate(player, event, "§cComplete the safety tutorial first!");
        }
        return true;
    }

    private static boolean denyGate(ServerPlayer player, PlayerInteractEvent.RightClickBlock event, String message) {
        player.sendSystemMessage(Component.literal(message));
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        return false;
    }
}
