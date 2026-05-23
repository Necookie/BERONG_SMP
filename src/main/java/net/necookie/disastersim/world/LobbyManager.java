package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages the lobby area where players can start simulations.
 * Handles lobby generation and interaction with simulation start buttons.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {
    /** Central position of the lobby. */
    public static final BlockPos LOBBY_POS = new BlockPos(0, 64, 0);
    
    /** Position of the button used to start the fire simulation. */
    public static final BlockPos FIRE_BUTTON_POS = LOBBY_POS.offset(3, 1, -1);
    
    /** Position of the button used to start the earthquake simulation. */
    public static final BlockPos QUAKE_BUTTON_POS = LOBBY_POS.offset(3, 1, 1);

    /**
     * Generates the lobby structure in the world.
     * This is typically called when the server starts.
     * 
     * @param level The level in which to create the lobby.
     */
    public static void createLobby(Level level) {
        // Create the floor (11x11 area of Polished Andesite)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                level.setBlockAndUpdate(LOBBY_POS.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }

        // Create a wall to hold the buttons (Stone Bricks)
        for (int z = -2; z <= 2; z++) {
            for (int y = 0; y <= 2; y++) {
                level.setBlockAndUpdate(LOBBY_POS.offset(4, y, z), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        // Place the interaction buttons
        level.setBlockAndUpdate(FIRE_BUTTON_POS, Blocks.OAK_BUTTON.defaultBlockState());
        level.setBlockAndUpdate(QUAKE_BUTTON_POS, Blocks.OAK_BUTTON.defaultBlockState());
        
        BerongSMP.LOGGER.info("BerongSMP Lobby created at {}", LOBBY_POS);
    }

    /**
     * Listens for players right-clicking blocks.
     * Used to detect interactions with simulation start buttons.
     * 
     * @param event The interaction event.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Only handle logic on the server side
        if (event.getLevel().isClientSide()) return;
        
        BlockPos pos = event.getPos();
        ServerPlayer player = (ServerPlayer) event.getEntity();

        // Check if the clicked block is the fire simulation button
        if (pos.equals(FIRE_BUTTON_POS)) {
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.FIRE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } 
        // Check if the clicked block is the earthquake simulation button
        else if (pos.equals(QUAKE_BUTTON_POS)) {
            SimulationManager.startSimulation(player, SimulationManager.SimulationState.EARTHQUAKE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
