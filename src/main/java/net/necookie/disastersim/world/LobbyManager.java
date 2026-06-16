package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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

@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {
    // Y matches the library structure so both areas sit at the same elevation.
    public static final BlockPos LOBBY_POS = new BlockPos(0, -33, 0);

    // Exact player spawn point inside the lobby (used on login and simulation return).
    public static final double SPAWN_X = 8.8;
    public static final double SPAWN_Y = -31.0;
    public static final double SPAWN_Z = 8.0;

    private static final Identifier LOBBY_STRUCTURE_ID =
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lobby_structure");

    // Discovered at load time by scanning the placed structure for buttons.
    // Sorted by ascending Z: lower Z = fire, higher Z = earthquake.
    private static BlockPos fireButtonPos = null;
    private static BlockPos quakeButtonPos = null;

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
            BerongSMP.LOGGER.info("BerongSMP Lobby loaded from NBT at {}", LOBBY_POS);
        } else {
            BerongSMP.LOGGER.error("Failed to load lobby structure: {}", LOBBY_STRUCTURE_ID);
        }
    }

    // Scans the placed structure bounds for button blocks and assigns fire/quake positions.
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

        // Sort ascending by Z so the button with lower Z = fire, higher Z = earthquake
        buttons.sort(Comparator.comparingInt(BlockPos::getZ));

        fireButtonPos  = buttons.size() >= 1 ? buttons.get(0) : null;
        quakeButtonPos = buttons.size() >= 2 ? buttons.get(1) : null;

        BerongSMP.LOGGER.info("Lobby buttons found: {} total. Fire={}, Quake={}",
                buttons.size(), fireButtonPos, quakeButtonPos);
    }

    // Teleports every player to the lobby the moment they log in.
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z,
                Collections.emptySet(), 0.0f, 0.0f, true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        ServerPlayer player = (ServerPlayer) event.getEntity();

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
