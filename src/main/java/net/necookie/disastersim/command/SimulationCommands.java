package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

public class SimulationCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sim_fire")
                .executes(context -> {
                    if (!context.getSource().isPlayer()) return 0;
                    SimulationManager.startSimulation(
                            context.getSource().getPlayer(),
                            SimulationManager.SimulationState.FIRE);
                    return 1;
                }));

        dispatcher.register(Commands.literal("sim_earthquake")
                .executes(context -> {
                    if (!context.getSource().isPlayer()) return 0;
                    SimulationManager.startSimulation(
                            context.getSource().getPlayer(),
                            SimulationManager.SimulationState.EARTHQUAKE);
                    return 1;
                })
                .then(Commands.argument("magnitude", DoubleArgumentType.doubleArg(0.1, 10.0))
                        .executes(context -> {
                            if (!context.getSource().isPlayer()) return 0;
                            double mag = DoubleArgumentType.getDouble(context, "magnitude");
                            SimulationManager.startSimulation(
                                    context.getSource().getPlayer(),
                                    SimulationManager.SimulationState.EARTHQUAKE,
                                    mag);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("sim_magnitude")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
                        .executes(context -> {
                            if (!context.getSource().isPlayer()) return 0;
                            ServerPlayer player = context.getSource().getPlayer();
                            SimulationSession session = SimulationManager.getSession(player.getUUID());
                            if (session == null
                                    || session.getState() != SimulationManager.SimulationState.EARTHQUAKE) {
                                context.getSource().sendFailure(
                                        Component.literal("No active earthquake simulation to adjust."));
                                return 0;
                            }
                            double val = DoubleArgumentType.getDouble(context, "value");
                            session.setSessionMagnitude(val);
                            context.getSource().sendSuccess(
                                    () -> Component.literal(String.format(
                                            "§eMagnitude updated to %.1f — takes effect next tick.", val)),
                                    true);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("sim_stop")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        UUID uuid = context.getSource().getPlayer().getUUID();
                        SimulationManager.endSimulation(uuid);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Simulation stopped."), true);
                    return 1;
                }));

        dispatcher.register(Commands.literal("sim_status")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simStatus(ctx, ctx.getSource().getPlayer());
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .executes(ctx -> simStatus(ctx, EntityArgument.getPlayer(ctx, "player")))));

        dispatcher.register(Commands.literal("sim_list")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    Map<UUID, SimulationSession> sessions = SimulationManager.getActiveSessions();
                    if (sessions.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§7No active simulations."), false);
                        return 1;
                    }
                    StringBuilder sb = new StringBuilder("§6Active simulations (").append(sessions.size()).append("):\n");
                    for (SimulationSession s : sessions.values()) {
                        int secs = (s.getTimerTicks() + 19) / 20;
                        sb.append("§f").append(s.getPlayer().getName().getString())
                          .append(" §7— §e").append(s.getState().name())
                          .append(" §7| ⏱ ").append(formatTime(secs))
                          .append(s.isFrozen() ? " §b[FROZEN]" : "")
                          .append("\n");
                    }
                    String msg = sb.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }));

        dispatcher.register(Commands.literal("sim_freeze")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simFreeze(ctx, ctx.getSource().getPlayer(), true);
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> simFreeze(ctx, EntityArgument.getPlayer(ctx, "player"), true))));

        dispatcher.register(Commands.literal("sim_unfreeze")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simFreeze(ctx, ctx.getSource().getPlayer(), false);
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> simFreeze(ctx, EntityArgument.getPlayer(ctx, "player"), false))));

        dispatcher.register(Commands.literal("sim_time")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.literal("set")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    SimulationSession session = SimulationManager.getSession(player.getUUID());
                                    if (session == null) {
                                        ctx.getSource().sendFailure(Component.literal("No active simulation."));
                                        return 0;
                                    }
                                    int secs = IntegerArgumentType.getInteger(ctx, "seconds");
                                    session.setTimerTicks(secs * 20);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§eTimer set to §f" + formatTime(secs) + "§e."), true);
                                    return 1;
                                })))
                .then(Commands.literal("add")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(-3600, 3600))
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    SimulationSession session = SimulationManager.getSession(player.getUUID());
                                    if (session == null) {
                                        ctx.getSource().sendFailure(Component.literal("No active simulation."));
                                        return 0;
                                    }
                                    int delta = IntegerArgumentType.getInteger(ctx, "seconds");
                                    int newTicks = Math.max(0, session.getTimerTicks() + delta * 20);
                                    session.setTimerTicks(newTicks);
                                    int newSecs = (newTicks + 19) / 20;
                                    String sign = delta >= 0 ? "+" : "";
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§eTimer adjusted by §f" + sign + delta + "s §e→ §f" + formatTime(newSecs)), true);
                                    return 1;
                                }))));
    }

    private static int simStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        SimulationSession session = SimulationManager.getSession(target.getUUID());
        if (session == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7" + target.getName().getString() + " has no active simulation."), false);
            return 1;
        }
        int secs = (session.getTimerTicks() + 19) / 20;
        String state = session.getState().name();
        String phase = session.getState() == SimulationManager.SimulationState.EARTHQUAKE
                && session.getQuakePhase() != null
                ? " §7(Phase: §e" + session.getQuakePhase().name() + "§7)" : "";
        String fireInfo = session.getState() == SimulationManager.SimulationState.FIRE
                ? "\n§eFires extinguished: §f" + session.getFiresExtinguished() : "";
        String frozen = session.isFrozen() ? " §b[FROZEN]" : "";
        String msg = "§6--- Simulation Status: " + target.getName().getString() + " ---\n" +
                "§eType: §f" + state + phase + frozen + "\n" +
                "§eTime remaining: §f" + formatTime(secs) + fireInfo;
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int simFreeze(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean freeze) {
        SimulationSession session = SimulationManager.getSession(target.getUUID());
        if (session == null) {
            ctx.getSource().sendFailure(Component.literal(
                    target.getName().getString() + " has no active simulation."));
            return 0;
        }
        session.setFrozen(freeze);
        String action = freeze ? "§bFROZEN" : "§aRESUMED";
        target.sendSystemMessage(Component.literal("§7Your simulation timer has been " + action + "§7."));
        ctx.getSource().sendSuccess(() -> Component.literal(
                action + " §7timer for §f" + target.getName().getString()), true);
        return 1;
    }

    static String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
