package net.necookie.disastersim.common.structure;

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
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.registration.RegistrationManager;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.tutorial.NpcRole;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.tutorial.TutorialManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.necookie.disastersim.command.BfpAdminCommands;
import net.necookie.disastersim.session.SessionManager;

@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {

    // Y=-33 matches the LSPU Library structure so both areas sit at the same elevation.
    public static final BlockPos LOBBY_POS = new BlockPos(0, -33, 0);

    public static final double SPAWN_X = 8.8;
    public static final double SPAWN_Y = -31.0;
    public static final double SPAWN_Z = 8.0;

    private static final Identifier LOBBY_STRUCTURE_ID =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lobby_structure");

    // Sorted by ascending Z: lower Z = Academy tutorial trigger, higher Z = New Sim Building 2.0 trigger.
    private static BlockPos tutorialButtonPos   = null;
    private static BlockPos simulationButtonPos = null;

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

        // Sorted ascending Z: lower Z = Academy tutorial trigger, higher Z = simulation trigger.
        // Relies on the lobby NBT placing the tutorial button at a lower Z — update sort key if NBT changes.
        buttons.sort(Comparator.comparingInt(BlockPos::getZ));

        tutorialButtonPos   = buttons.size() >= 1 ? buttons.get(0) : null;
        simulationButtonPos = buttons.size() >= 2 ? buttons.get(1) : null;

        if (buttons.size() < 2) {
            BerongSMP.LOGGER.warn(
                    "Lobby button scan found only {} button(s); expected 2. "
                    + "Tutorial={}, Simulation={}. Check the lobby_structure NBT.",
                    buttons.size(), tutorialButtonPos, simulationButtonPos);
        } else {
            BerongSMP.LOGGER.info("Lobby buttons found: {} total. Tutorial={}, Simulation={}",
                    buttons.size(), tutorialButtonPos, simulationButtonPos);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        routePlayer(player, level);
    }

    // No custom onPlayerRespawn handler: the world respawn point is pinned to the lobby
    // (BerongSMP.onServerStarting → level.setRespawnData at BlockPos(8,-31,8)), so vanilla's own
    // respawn placement already lands players there with no bed/anchor set. A death mid-simulation
    // is handled separately by SimulationManager.onPlayerRespawn (pendingLobbyRespawn).

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

    /**
     * Every login lands in the main lobby — the old routing that diverted an old-tutorial-incomplete
     * player to the legacy tutorial lobby is gone now that the legacy tutorial no longer gates
     * anything the main lobby offers (its buttons launch the Academy / New Sim Building 2.0
     * directly). The legacy tutorial is still reachable via {@code /bfp old_tutorial} for dev use.
     */
    private static void routePlayer(ServerPlayer player, ServerLevel level) {
        player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z,
            Collections.emptySet(), 0.0f, 0.0f, true);
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

        if (tutorialButtonPos != null && pos.equals(tutorialButtonPos)) {
            if (!baseGatesPassed(player, event)) return;
            ServerLevel level = (ServerLevel) event.getLevel();
            AcademyManager.startAcademyRun(player, level);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (simulationButtonPos != null && pos.equals(simulationButtonPos)) {
            if (!baseGatesPassed(player, event)) return;
            if (!academyCertified(player, event)) return;
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * Runs the lobby entry gates common to both buttons (registration → active session), unless
     * the player has an admin test-bypass. Returns {@code true} if the player may proceed;
     * otherwise messages the reason, cancels the interaction with FAIL, and returns {@code false}.
     * The legacy tutorial-completion gate is gone — the Academy is reached via button 1 itself now,
     * and button 2's own Academy-certification gate is checked separately by
     * {@link #academyCertified}.
     */
    private static boolean baseGatesPassed(ServerPlayer player, PlayerInteractEvent.RightClickBlock event) {
        if (BfpAdminCommands.isTestBypass(player.getUUID())) return true;
        if (!RegistrationManager.isRegistered(player)) {
            return denyGate(player, event,
                "§cPlease /register first! Usage: /register <username> <password> <student_id> <section> <full_name>"
                    + "\n§7(Already have an account? §f/login <username> <password>§7 instead.)");
        }
        if (SessionManager.getActiveSession(player.getUUID()) == null) {
            return denyGate(player, event,
                "§cYour session has expired. Use §f/login <username> <password>§c (or §f/register§c again) to start a new one.");
        }
        return true;
    }

    /**
     * Button 2 (New Sim Building 2.0) additionally requires the player to have been certified by
     * the Academy — either this session (Capt. Morfe's {@code EVALUATED_PASS}) or on a previous
     * visit, restored via {@code /login} ({@link AuthManager#isTutorialCompleted}).
     */
    private static boolean academyCertified(ServerPlayer player, PlayerInteractEvent.RightClickBlock event) {
        if (BfpAdminCommands.isTestBypass(player.getUUID())) return true;
        ServerLevel level = (ServerLevel) event.getLevel();
        boolean certifiedThisSession =
                AcademySavedData.get(level).get(player.getUUID()).morfePhase() == MorfePhase.EVALUATED_PASS;
        if (certifiedThisSession || AuthManager.isTutorialCompleted(player.getUUID())) {
            return true;
        }
        return denyGate(player, event,
            "§cComplete the Academy tutorial first — press the first button."
                + "\n§7(Already certified from a previous visit? §f/login <username> <password>§7 to restore it.)");
    }

    private static boolean denyGate(ServerPlayer player, PlayerInteractEvent.RightClickBlock event, String message) {
        player.sendSystemMessage(Component.literal(message));
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        return false;
    }
}
