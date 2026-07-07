package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.common.structure.building.CCSBuildingConstructor;

import java.util.Arrays;
import java.util.List;

public class ItemCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> hazardIds = List.copyOf(BerongSMP.HAZARD_ITEM_MAP.keySet());
        List<String> allIds = List.copyOf(BerongSMP.ALL_ITEM_MAP.keySet());
        dispatcher.register(Commands.literal("item")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.literal("hazard")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(hazardIds, builder))
                                .executes(ItemCommands::giveHazardItem)))
                .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(allIds, builder))
                                .executes(ItemCommands::giveAnyItem)))
                .then(Commands.literal("kit")
                        .executes(ItemCommands::giveKit)));
        dispatcher.register(Commands.literal("spawn_lspu")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ItemCommands::spawnLSPU));

        dispatcher.register(Commands.literal("get_extinguisher")
                .executes(ItemCommands::getExtinguisher));

        dispatcher.register(Commands.literal("place_buildings")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    SimulationManager.placeAllBuildings(level);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aAll buildings placed — check logs for entity output."), true);
                    return 1;
                }));

        dispatcher.register(Commands.literal("spawn_npc")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(NpcType.values()).map(t -> t.id), builder))
                        .executes(ItemCommands::spawnNpc)));

        dispatcher.register(Commands.literal("get_co2_extinguisher")
                .executes(ctx -> {
                    ServerPlayer player = requirePlayer(ctx.getSource());
                    if (player == null) return 0;
                    player.getInventory().add(BerongSMP.CO2_EXTINGUISHER.get().getDefaultInstance());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aCO2 Extinguisher added to your inventory! Use it on burning computer blocks."), true);
                    return 1;
                }));

        dispatcher.register(Commands.literal("get_wet_chemical_extinguisher")
                .executes(ctx -> {
                    ServerPlayer player = requirePlayer(ctx.getSource());
                    if (player == null) return 0;
                    player.getInventory().add(BerongSMP.WET_CHEMICAL_EXTINGUISHER.get().getDefaultInstance());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aWet Chemical Extinguisher added to your inventory! Use it on kitchen grease fires."), true);
                    return 1;
                }));
    }

    private static int spawnNpc(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) return 0;
        String typeId = StringArgumentType.getString(ctx, "type");
        NpcType npcType = NpcType.fromId(typeId);

        ServerLevel level = source.getLevel();
        BlockPos pos = player.blockPosition();

        CustomNpcEntity npc = new CustomNpcEntity(BerongSMP.CUSTOM_NPC.get(), level);
        npc.setNpcType(npcType);
        npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        npc.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        npc.yRotO = npc.getYRot();
        level.addFreshEntity(npc);

        source.sendSuccess(() -> Component.literal(
                "§aSpawned §r" + npcType.displayName + "§a at your position."), true);
        return 1;
    }

    private static int spawnLSPU(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) return 0;
        BlockPos pos = player.blockPosition();
        new CCSBuildingConstructor().construct(player.level(), pos);
        source.sendSuccess(() -> Component.literal("LSPU Building spawned at your position!"), true);
        return 1;
    }

    private static int getExtinguisher(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(source);
        if (player == null) return 0;
        player.getInventory().add(BerongSMP.FIRE_EXTINGUISHER.get().getDefaultInstance());
        source.sendSuccess(() -> Component.literal("Fire Extinguisher added to your inventory!"), true);
        return 1;
    }

    private static int giveAnyItem(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx.getSource());
        if (player == null) return 0;
        String name = StringArgumentType.getString(ctx, "name");
        net.neoforged.neoforge.registries.DeferredItem<? extends net.minecraft.world.item.Item> item =
                BerongSMP.ALL_ITEM_MAP.get(name);
        if (item == null) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown item: §r" + name));
            return 0;
        }
        player.getInventory().add(item.get().getDefaultInstance());
        ctx.getSource().sendSuccess(() -> Component.literal("§aGave item: §r" + name), true);
        return 1;
    }

    private static int giveKit(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx.getSource());
        if (player == null) return 0;
        BerongSMP.ALL_ITEM_MAP.values().forEach(item -> player.getInventory().add(item.get().getDefaultInstance()));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aGave one of every BerongSMP item (§r" + BerongSMP.ALL_ITEM_MAP.size() + "§a)."), true);
        return 1;
    }

    private static int giveHazardItem(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx.getSource());
        if (player == null) return 0;
        String name = StringArgumentType.getString(ctx, "name");
        net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> item =
                BerongSMP.HAZARD_ITEM_MAP.get(name);
        if (item == null) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown hazard prop: §r" + name));
            return 0;
        }
        player.getInventory().add(item.get().getDefaultInstance());
        ctx.getSource().sendSuccess(() -> Component.literal("§aGave hazard prop: §r" + name), true);
        return 1;
    }

    /** Returns the source's player, or {@code null} after sending a failure if the source isn't a player. */
    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return null;
        }
        return source.getPlayer();
    }
}
