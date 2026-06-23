package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.world.building.CCSBuildingConstructor;

public class ItemCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn_lspu")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ItemCommands::spawnLSPU));

        dispatcher.register(Commands.literal("get_extinguisher")
                .executes(ItemCommands::getExtinguisher));

        dispatcher.register(Commands.literal("get_co2_extinguisher")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        ctx.getSource().sendFailure(Component.literal("This command can only be run by a player."));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayer();
                    player.getInventory().add(BerongSMP.CO2_EXTINGUISHER.get().getDefaultInstance());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aCO2 Extinguisher added to your inventory! Use it on burning computer blocks."), true);
                    return 1;
                }));
    }

    private static int spawnLSPU(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        BlockPos pos = player.blockPosition();
        new CCSBuildingConstructor().construct(player.level(), pos);
        source.sendSuccess(() -> Component.literal("LSPU Building spawned at your position!"), true);
        return 1;
    }

    private static int getExtinguisher(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        player.getInventory().add(BerongSMP.FIRE_EXTINGUISHER.get().getDefaultInstance());
        source.sendSuccess(() -> Component.literal("Fire Extinguisher added to your inventory!"), true);
        return 1;
    }
}
