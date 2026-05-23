package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = BerongSMP.MODID)
public class LobbyManager {
    public static final BlockPos LOBBY_POS = new BlockPos(0, 64, 0);
    public static final BlockPos FIRE_BUTTON_POS = LOBBY_POS.offset(3, 1, -1);
    public static final BlockPos QUAKE_BUTTON_POS = LOBBY_POS.offset(3, 1, 1);

    public static void createLobby(Level level) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                level.setBlockAndUpdate(LOBBY_POS.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }

        for (int z = -2; z <= 2; z++) {
            for (int y = 0; y <= 2; y++) {
                level.setBlockAndUpdate(LOBBY_POS.offset(4, y, z), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        level.setBlockAndUpdate(FIRE_BUTTON_POS, Blocks.OAK_BUTTON.defaultBlockState());
        level.setBlockAndUpdate(QUAKE_BUTTON_POS, Blocks.OAK_BUTTON.defaultBlockState());
        
        BerongSMP.LOGGER.info("Lobby created at {}", LOBBY_POS);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        
        BlockPos pos = event.getPos();
        if (pos.equals(FIRE_BUTTON_POS)) {
            SimulationManager.startSimulation((ServerPlayer) event.getEntity(), SimulationManager.SimulationState.FIRE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (pos.equals(QUAKE_BUTTON_POS)) {
            SimulationManager.startSimulation((ServerPlayer) event.getEntity(), SimulationManager.SimulationState.EARTHQUAKE);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}