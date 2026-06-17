package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.building.CCSBuildingConstructor;

/**
 * Handles the registration and execution of custom commands for BerongSMP.
 */
public class ModCommands {

    /**
     * Registers all custom commands to the command dispatcher.
     * 
     * @param dispatcher The dispatcher to register commands to.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Command to manually spawn the LSPU building (requires GM permissions)
        dispatcher.register(Commands.literal("spawn_lspu")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ModCommands::spawnLSPU));

        // Command to give the fire extinguisher to the player
        dispatcher.register(Commands.literal("get_extinguisher")
                .executes(ModCommands::getExtinguisher));

        // Command to manually trigger a fire simulation
        dispatcher.register(Commands.literal("sim_fire")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        SimulationManager.startSimulation(context.getSource().getPlayer(), SimulationManager.SimulationState.FIRE);
                        return 1;
                    }
                    return 0;
                }));

        // Command to manually trigger an earthquake simulation
        dispatcher.register(Commands.literal("sim_earthquake")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        SimulationManager.startSimulation(context.getSource().getPlayer(), SimulationManager.SimulationState.EARTHQUAKE);
                        return 1;
                    }
                    return 0;
                }));

        // Command to stop the calling player's simulation early
        dispatcher.register(Commands.literal("sim_stop")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        UUID uuid = context.getSource().getPlayer().getUUID();
                        SimulationManager.endSimulation(uuid);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Simulation stopped."), true);
                    return 1;
                }));
    }

    /**
     * Logic for spawning the LSPU building at the player's position.
     * 
     * @param context The command context.
     * @return 1 for success, 0 for failure.
     */
    private static int spawnLSPU(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            BlockPos pos = player.blockPosition();
            
            // Construct the building using the specialized constructor
            new CCSBuildingConstructor().construct(player.level(), pos);
            
            source.sendSuccess(() -> Component.literal("LSPU Building spawned at your position!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
    }

    /**
     * Logic for giving the fire extinguisher item to the player.
     * 
     * @param context The command context.
     * @return 1 for success, 0 for failure.
     */
    private static int getExtinguisher(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            
            // Add the item to the player's inventory
            player.getInventory().add(BerongSMP.FIRE_EXTINGUISHER.get().getDefaultInstance());
            
            source.sendSuccess(() -> Component.literal("Fire Extinguisher added to your inventory!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
    }
}
