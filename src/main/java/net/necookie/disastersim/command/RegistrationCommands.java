package net.necookie.disastersim.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.registration.RegistrationManager;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;
import net.necookie.disastersim.session.TursoClient;

/**
 * Player-facing account commands: {@code /register} (create an account + start a session),
 * {@code /login}/{@code /logout} (return to an existing account across sessions/days), and
 * {@code /history} (a logged-in student's own past runs).
 *
 * <p>Registration/login are async — see {@link AuthManager#registerAsync}/{@link AuthManager#loginAsync}
 * — since password hashing plus the Turso round-trip is too slow for the command/server thread.
 * Every callback re-enters on the server thread before touching game state.
 */
public class RegistrationCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("register")
                .then(Commands.argument("username", StringArgumentType.word())
                        .then(Commands.argument("password", StringArgumentType.word())
                                .then(Commands.argument("student_id", StringArgumentType.word())
                                        .then(Commands.argument("section", StringArgumentType.word())
                                                .then(Commands.argument("full_name", StringArgumentType.greedyString())
                                                        .executes(RegistrationCommands::doRegister)))))));

        dispatcher.register(Commands.literal("login")
                .then(Commands.argument("username", StringArgumentType.word())
                        .then(Commands.argument("password", StringArgumentType.word())
                                .executes(RegistrationCommands::doLogin))));

        dispatcher.register(Commands.literal("logout")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    ServerPlayer player = ctx.getSource().getPlayer();
                    AuthManager.logout(player.getUUID());
                    SessionManager.checkout(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Logged out."), false);
                    return 1;
                }));

        dispatcher.register(Commands.literal("history")
                .executes(RegistrationCommands::doHistory));
    }

    private static int doRegister(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!ctx.getSource().isPlayer()) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        String username = StringArgumentType.getString(ctx, "username");
        String password = StringArgumentType.getString(ctx, "password");
        String studentId = StringArgumentType.getString(ctx, "student_id");
        String section   = StringArgumentType.getString(ctx, "section");
        String fullName  = StringArgumentType.getString(ctx, "full_name");

        if (!AuthManager.isValidUsername(username)) {
            ctx.getSource().sendFailure(Component.literal(
                    "§cUsername must be 3-16 letters, numbers, or underscores."));
            return 0;
        }
        if (!AuthManager.isValidPassword(password)) {
            ctx.getSource().sendFailure(Component.literal("§cPassword must be at least 6 characters."));
            return 0;
        }

        AuthManager.registerAsync(player, username, password, studentId, section, fullName,
                offline -> {
                    RegistrationManager.register(player, studentId, section, fullName);
                    SessionManager.checkin(player.getUUID(), player.getName().getString(), fullName, username);
                    TursoClient.updateStudentInfo(player.getUUID().toString(), studentId, section);
                    player.sendSystemMessage(Component.literal(
                            "§a✓ Registered as §f" + fullName + " §7(§f" + studentId + "§7) — Section §f" + section
                                    + "\n§eYou may now begin the tutorial."
                                    + (offline
                                    ? "\n§7(Account system offline — /login won't work next time; ask an instructor.)"
                                    : "\n§7Remember your username/password — §f/login §7next time skips you straight back in.")));
                },
                error -> player.sendSystemMessage(Component.literal(switch (error) {
                    case "taken" -> "§cUsername '" + username + "' is already taken. Try another with /register.";
                    default -> "§cRegistration failed — please tell an instructor. (" + error + ")";
                })));
        return 1;
    }

    private static int doLogin(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!ctx.getSource().isPlayer()) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        String username = StringArgumentType.getString(ctx, "username");
        String password = StringArgumentType.getString(ctx, "password");

        AuthManager.loginAsync(player, username, password,
                account -> {
                    RegistrationManager.register(player, account.studentId(), account.section(), account.fullName());
                    SessionManager.checkin(player.getUUID(), player.getName().getString(), account.fullName(), username);
                    TursoClient.updateStudentInfo(player.getUUID().toString(), account.studentId(), account.section());
                    player.sendSystemMessage(Component.literal(
                            "§a✓ Logged in as §f" + account.fullName() + " §7(@" + username + ")"
                                    + (account.tutorialCompleted()
                                    ? "\n§eAcademy already certified — the simulation button is unlocked."
                                    : "\n§eComplete the Academy tutorial (first lobby button) to unlock the simulation.")));
                },
                error -> player.sendSystemMessage(Component.literal(
                        error.startsWith("locked:")
                                ? "§cToo many failed attempts. Try again in " + error.substring(7) + "s."
                                : switch (error) {
                            // Deliberately identical wording for an unknown username and a wrong
                            // password (AuthManager.loginAsync) — distinguishing them lets a
                            // station enumerate which usernames are actually registered.
                            case "invalid_credentials" -> "§cIncorrect username or password.";
                            case "offline" -> "§cAccount system is offline — ask an instructor to /register you locally.";
                            default -> "§cLogin failed. (" + error + ")";
                        })));
        return 1;
    }

    /**
     * Runs the run-history lookup asynchronously ({@link TursoClient#queryAsync}) so a slow or
     * stalled Turso endpoint can't freeze the server thread for the whole 10s HTTP timeout — the
     * command returns immediately and the reply is sent once the query resolves, re-entering the
     * server thread via {@code server.execute(...)} the same way {@code AuthManager.registerAsync}/
     * {@code loginAsync} already do for async login/registration.
     */
    private static int doHistory(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!ctx.getSource().isPlayer()) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        AuthManager.AuthAccount account = AuthManager.current(player.getUUID());
        if (account == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "§cLog in first with §f/login <username> <password>§c to see your run history."));
            return 0;
        }
        var server = ctx.getSource().getServer();
        TursoClient.queryAsync(
                "SELECT id, start_time, simulation_type, simulation_score, passed, status, prep_level " +
                "FROM sessions WHERE username=? ORDER BY start_time DESC LIMIT 10",
                account.username())
            .thenAccept(rows -> server.execute(() -> {
                if (rows.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§7No past runs yet."), false);
                    return;
                }
                StringBuilder sb = new StringBuilder("§6--- Your run history (@" + account.username() + ") ---\n");
                for (JsonElement el : rows) {
                    JsonObject r = el.getAsJsonObject();
                    sb.append("§7#").append(str(r, "id"))
                      .append(" §f").append(str(r, "start_time"))
                      .append(" §e").append(str(r, "simulation_type"))
                      .append(" §ascore=").append(str(r, "simulation_score"))
                      .append(" pass=").append(str(r, "passed"))
                      .append(" §b").append(str(r, "prep_level"))
                      .append(" [").append(str(r, "status")).append("]\n");
                }
                String msg = sb.toString();
                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
            }));
        return 1;
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }
}
