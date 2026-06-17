package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.network.SimulationStatusPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {
    private static final Identifier STRUCTURE_ID = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrarymain");

    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

    // Tuneable simulation constants
    public static final int SIM_DURATION_TICKS  = 2400; // 2 minutes
    private static final int FIRE_SPAWN_COUNT    = 3;    // fires placed per tick interval
    private static final int FIRE_SPAWN_INTERVAL = 20;   // ticks between fire spreads
    private static final int QUAKE_BREAK_COUNT   = 2;    // blocks broken per tick interval
    private static final int QUAKE_INTERVAL      = 10;   // ticks between quake effects
    private static final int SIM_AREA_SIZE       = 25;   // XZ spread of effects
    private static final int SIM_AREA_HEIGHT     = 10;   // Y spread of effects

    // One session per player — keyed by UUID so multiple players can run simultaneously
    private static final Map<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();

    public enum SimulationState {
        IDLE,
        FIRE,
        EARTHQUAKE
    }

    public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
        UUID uuid = player.getUUID();
        if (activeSessions.containsKey(uuid)) {
            player.sendSystemMessage(Component.literal("You already have a simulation in progress!"));
            return;
        }

        activeSessions.put(uuid, new SimulationSession(player, state));

        ServerLevel level = (ServerLevel) player.level();
        loadStructure(level, SIM_POS);
        player.teleportTo(level, SIM_POS.getX() + 5.5, SIM_POS.getY() + 2.0, SIM_POS.getZ() + 5.5,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.literal("Starting " + state.name() + " Simulation!"));
    }

    private static void loadStructure(ServerLevel level, BlockPos pos) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(STRUCTURE_ID);

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        } else {
            BerongSMP.LOGGER.error("Failed to load structure: {}", STRUCTURE_ID);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            SimulationSession session = activeSessions.get(uuid);
            if (session == null) continue;

            session.timer--;
            if (session.timer <= 0) {
                endSimulation(uuid);
                continue;
            }

            ServerPlayer player = session.player;
            if (player == null || !player.isAlive()) {
                endSimulation(uuid);
                continue;
            }

            ServerLevel level = (ServerLevel) player.level();

            if (session.state == SimulationState.FIRE && session.timer % FIRE_SPAWN_INTERVAL == 0) {
                simulateFire(level);
            } else if (session.state == SimulationState.EARTHQUAKE && session.timer % QUAKE_INTERVAL == 0) {
                simulateEarthquake(level);
            }

            if (session.timer % QUAKE_INTERVAL == 0) {
                PacketDistributor.sendToPlayer(player,
                        new SimulationStatusPayload(session.state.name(), (session.timer + 19) / 20));
            }
        }
    }

    private static void simulateFire(ServerLevel level) {
        for (int i = 0; i < FIRE_SPAWN_COUNT; i++) {
            BlockPos firePos = SIM_POS.offset(
                    level.getRandom().nextInt(SIM_AREA_SIZE),
                    level.getRandom().nextInt(SIM_AREA_HEIGHT),
                    level.getRandom().nextInt(SIM_AREA_SIZE));
            if (level.getBlockState(firePos).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private static void simulateEarthquake(ServerLevel level) {
        for (int i = 0; i < QUAKE_BREAK_COUNT; i++) {
            BlockPos breakPos = SIM_POS.offset(
                    level.getRandom().nextInt(SIM_AREA_SIZE),
                    level.getRandom().nextInt(SIM_AREA_HEIGHT),
                    level.getRandom().nextInt(SIM_AREA_SIZE));
            if (!level.getBlockState(breakPos).isAir() && level.getBlockState(breakPos).getBlock() != Blocks.BEDROCK) {
                level.destroyBlock(breakPos, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        endSimulation(event.getEntity().getUUID());
    }

    public static synchronized void endSimulation(UUID uuid) {
        SimulationSession session = activeSessions.remove(uuid);
        if (session == null) return;

        ServerPlayer player = session.player;
        if (player != null && player.isAlive()) {
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload("", 0));
            player.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));

            ServerLevel level = (ServerLevel) player.level();
            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
            loadStructure(level, SIM_POS);
        }
    }
}
