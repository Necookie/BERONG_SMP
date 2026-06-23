package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.registration.RegistrationManager;
import net.necookie.disastersim.session.SessionManager;
import net.necookie.disastersim.session.TursoClient;

public class RegistrationCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("register")
                .then(Commands.argument("student_id", StringArgumentType.word())
                        .then(Commands.argument("section", StringArgumentType.word())
                                .then(Commands.argument("full_name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (!ctx.getSource().isPlayer()) return 0;
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            String studentId = StringArgumentType.getString(ctx, "student_id");
                                            String section   = StringArgumentType.getString(ctx, "section");
                                            String fullName  = StringArgumentType.getString(ctx, "full_name");
                                            RegistrationManager.register(player, studentId, section, fullName);
                                            SessionManager.checkin(player.getUUID(), player.getName().getString(), fullName);
                                            TursoClient.updateStudentInfo(
                                                player.getUUID().toString(), studentId, section);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a✓ Registered as §f" + fullName +
                                                " §7(§f" + studentId + "§7) — Section §f" + section +
                                                "\n§eYou may now begin the tutorial."), false);
                                            return 1;
                                        })))));
    }
}
