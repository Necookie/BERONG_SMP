package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.building.CCSBuildingConstructor;

public class ModCommands {
    public static void register(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn_lspu")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ModCommands::spawnLSPU));

        dispatcher.register(Commands.literal("get_extinguisher")
                .executes(ModCommands::getExtinguisher));

        dispatcher.register(Commands.literal("sim_fire")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        SimulationManager.startSimulation(context.getSource().getPlayer(), SimulationManager.SimulationState.FIRE);
                        return 1;
                    }
                    return 0;
                }));

        dispatcher.register(Commands.literal("sim_earthquake")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        SimulationManager.startSimulation(context.getSource().getPlayer(), SimulationManager.SimulationState.EARTHQUAKE);
                        return 1;
                    }
                    return 0;
                }));
    }

    private static int spawnLSPU(CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            BlockPos pos = player.blockPosition();
            new CCSBuildingConstructor().construct(player.level(), pos);
            source.sendSuccess(() -> Component.literal("LSPU Building spawned at your position!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
    }

    private static int getExtinguisher(CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            player.getInventory().add(BerongSMP.FIRE_EXTINGUISHER.get().getDefaultInstance());
            source.sendSuccess(() -> Component.literal("Fire Extinguisher added to your inventory!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
    }
}