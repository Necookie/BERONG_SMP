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

import java.util.Optional;
import java.util.Collections;

/**
 * Manager class for the disaster simulation system.
 * Handles the state, logic, and lifecycle of fire and earthquake simulations.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public class SimulationManager {
    /** The identifier for the main simulation structure (LSPU Library). */
    private static final Identifier STRUCTURE_ID = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "lspulibrarymain");

    /** The center position where the simulation structure is loaded. */
    public static final BlockPos SIM_POS = new BlockPos(30, -34, 83);

    /** Current state of the simulation. */
    private static volatile SimulationState currentState = SimulationState.IDLE;

    /** Remaining time in ticks for the current simulation. */
    private static volatile int timer = 0;

    /** The player currently participating in the simulation. */
    private static volatile ServerPlayer activePlayer = null;

    /**
     * Enum representing the possible states of the simulation.
     */
    public enum SimulationState {
        IDLE,       // No simulation in progress
        FIRE,       // Fire disaster simulation
        EARTHQUAKE  // Earthquake disaster simulation
    }

    /**
     * Starts a new simulation for a specific player.
     * * @param player The player to participate in the simulation.
     * @param state The type of simulation to start.
     */
    public static synchronized void startSimulation(ServerPlayer player, SimulationState state) {
        // Prevent starting multiple simulations at once
        if (currentState != SimulationState.IDLE) {
            player.sendSystemMessage(Component.literal("A simulation is already in progress!"));
            return;
        }

        currentState = state;
        activePlayer = player;
        timer = 2400; // 2 minutes (2400 ticks @ 20tps)

        ServerLevel level = (ServerLevel) player.level();

        // Load the simulation area
        loadStructure(level, SIM_POS);

        // Teleport player to the start of the simulation area
        player.teleportTo(level, SIM_POS.getX() + 5.5, SIM_POS.getY() + 2.0, SIM_POS.getZ() + 5.5, Collections.emptySet(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.literal("Starting " + state.name() + " Simulation!"));
    }

    /**
     * Loads the simulation structure into the world.
     * * @param level The server level.
     * @param pos The position to place the structure.
     */
    private static void loadStructure(ServerLevel level, BlockPos pos) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(STRUCTURE_ID);

        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            // Place the structure template
            template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        } else {
            BerongSMP.LOGGER.error("Failed to load structure: {}", STRUCTURE_ID);
        }
    }

    /**
     * Main server tick listener for the simulation logic.
     * Updates the timer and triggers disaster effects.
     * * @param event The server tick event.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerPlayer player = activePlayer; // capture once to avoid null race
        if (currentState == SimulationState.IDLE || player == null) return;

        timer--;

        // Check if simulation time has expired
        if (timer <= 0) {
            endSimulation();
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Trigger fire effects every second (20 ticks)
        if (currentState == SimulationState.FIRE && timer % 20 == 0) {
            simulateFire(level);
        }
        // Trigger earthquake effects every half-second (10 ticks)
        else if (currentState == SimulationState.EARTHQUAKE && timer % 10 == 0) {
            simulateEarthquake(level);
        }

        // Send status updates to the client every 10 ticks
        if (timer % 10 == 0) {
            PacketDistributor.sendToPlayer(player, new SimulationStatusPayload(currentState.name(), (timer + 19) / 20));
        }
    }

    /**
     * Procedural fire simulation logic.
     * Randomly spawns fire blocks within the simulation area.
     * * @param level The server level.
     */
    private static void simulateFire(ServerLevel level) {
        for (int i = 0; i < 3; i++) {
            BlockPos firePos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
            // Only place fire in air blocks
            if (level.getBlockState(firePos).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    /**
     * Procedural earthquake simulation logic.
     * Randomly destroys blocks within the simulation area.
     * * @param level The server level.
     */
    private static void simulateEarthquake(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            BlockPos breakPos = SIM_POS.offset(level.getRandom().nextInt(25), level.getRandom().nextInt(10), level.getRandom().nextInt(25));
            // Destroy non-air blocks, excluding bedrock
            if (!level.getBlockState(breakPos).isAir() && level.getBlockState(breakPos).getBlock() != Blocks.BEDROCK) {
                level.destroyBlock(breakPos, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (activePlayer != null && event.getEntity().getUUID().equals(activePlayer.getUUID())) {
            endSimulation();
        }
    }

    /**
     * Ends the current simulation.
     * Returns the player to the lobby and resets the simulation area.
     */
    public static synchronized void endSimulation() {
        if (activePlayer != null) {
            // Reset client HUD
            PacketDistributor.sendToPlayer(activePlayer, new SimulationStatusPayload("", 0));

            activePlayer.sendSystemMessage(Component.literal("Simulation ended. Restoring structure..."));

            // Return player to lobby
            activePlayer.teleportTo((ServerLevel) activePlayer.level(), LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z, Collections.emptySet(), 0.0f, 0.0f, true);

            // Clean up and restore the simulation structure
            loadStructure((ServerLevel) activePlayer.level(), SIM_POS);
        }

        // Reset manager state
        currentState = SimulationState.IDLE;
        activePlayer = null;
        timer = 0;
    }
}