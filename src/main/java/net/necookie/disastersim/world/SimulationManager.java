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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import net.minecraft.world.entity.Relative;
import java.util.Random;

@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {
    private static final Identifier STRUCTURE_ID = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrary_main");
    public static final BlockPos SIM_POS = new BlockPos(100, 64, 100);
    public static final BlockPos LOBBY_POS = new BlockPos(0, 64, 0);

    private static SimulationState currentState = SimulationState.IDLE;
    private static int timer = 0;
    private static ServerPlayer activePlayer = null;
    private static final Random random = new Random();

    public enum SimulationState {
        IDLE,
        FIRE,
        EARTHQUAKE
    }

    public static void startSimulation(ServerPlayer player, SimulationState state) {
        if (currentState != SimulationState.IDLE) {
            player.sendSystemMessage(Component.literal("A simulation is already in progress!"));
            return;
        }

        currentState = state;
        activePlayer = player;
        timer = 2400; // 2 minutes

        ServerLevel level = ((ServerLevel)player.level());
        loadStructure(level, SIM_POS);
        
        player.teleportTo(level, SIM_POS.getX() + 5.5, SIM_POS.getY() + 1.0, SIM_POS.getZ() + 5.5, Collections.emptySet(), player.getYRot(), player.getXRot(), true);
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
            
            for (int x = -5; x < 35; x++) {
                for (int y = -5; y < 25; y++) {
                    for (int z = -5; z < 35; z++) {
                        level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }

            template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        } else {
            BerongSMP.LOGGER.error("Failed to load structure: {}", STRUCTURE_ID);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (currentState == SimulationState.IDLE || activePlayer == null) return;

        timer--;

        if (timer <= 0) {
            endSimulation();
            return;
        }

        if (currentState == SimulationState.FIRE && timer % 20 == 0) {
            simulateFire(((ServerLevel)activePlayer.level()));
        } else if (currentState == SimulationState.EARTHQUAKE && timer % 10 == 0) {
            simulateEarthquake(((ServerLevel)activePlayer.level()));
        }

        if (timer % 10 == 0) {
            PacketDistributor.sendToPlayer(activePlayer, new SimulationStatusPayload(currentState.name(), timer / 20));
        }
    }

    private static void simulateFire(ServerLevel level) {
        for (int i = 0; i < 3; i++) {
            BlockPos firePos = SIM_POS.offset(random.nextInt(25), random.nextInt(10), random.nextInt(25));
            if (level.getBlockState(firePos).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private static void simulateEarthquake(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            BlockPos breakPos = SIM_POS.offset(random.nextInt(25), random.nextInt(10), random.nextInt(25));
            if (!level.getBlockState(breakPos).isAir() && level.getBlockState(breakPos).getBlock() != Blocks.BEDROCK) {
                level.destroyBlock(breakPos, false);
            }
        }
    }

    private static void endSimulation() {
        if (activePlayer != null) {
            PacketDistributor.sendToPlayer(activePlayer, new SimulationStatusPayload("", 0));
            activePlayer.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));
            activePlayer.teleportTo(((ServerLevel)activePlayer.level()), LOBBY_POS.getX() + 0.5, LOBBY_POS.getY() + 1.0, LOBBY_POS.getZ() + 0.5, Collections.emptySet(), 0.0f, 0.0f, true);
            loadStructure(((ServerLevel)activePlayer.level()), SIM_POS);
        }
        
        currentState = SimulationState.IDLE;
        activePlayer = null;
        timer = 0;
    }
}