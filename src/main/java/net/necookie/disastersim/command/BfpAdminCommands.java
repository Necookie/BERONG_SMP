package net.necookie.disastersim.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.session.SessionManager;
import net.necookie.disastersim.session.StudentSession;
import net.necookie.disastersim.session.TursoClient;
import net.necookie.disastersim.world.TutorialLobbyManager;

public class BfpAdminCommands {

    private static final Set<UUID> bfpAuthorized = ConcurrentHashMap.newKeySet();

    // Rate-limiting: long[0] = consecutive failure count, long[1] = first-failure epoch-ms
    private static final Map<UUID, long[]> pinFailures = new ConcurrentHashMap<>();
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 5L * 60 * 1000; // 5 minutes

    public static void clearAuthorizations() {
        bfpAuthorized.clear();
        pinFailures.clear();
    }

    static boolean isBfpAuthorized(CommandSourceStack source) {
        if (!source.isPlayer()) return true;
        return bfpAuthorized.contains(source.getPlayer().getUUID())
                || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bfp")
                .requires(BfpAdminCommands::isBfpAuthorized)

                .then(Commands.literal("login")
                        .then(Commands.argument("pin", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    UUID uuid = player.getUUID();
                                    String entered = StringArgumentType.getString(ctx, "pin");
                                    String correct = Config.BFP_ADMIN_PIN.get();
                                    if (correct.isBlank()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cBFP PIN is not configured. Set 'bfpAdminPin' in berongsmp-common.toml."));
                                        return 1;
                                    }
                                    long[] rec = pinFailures.computeIfAbsent(uuid, k -> new long[]{0, 0});
                                    long now = System.currentTimeMillis();
                                    if (rec[0] >= MAX_PIN_ATTEMPTS) {
                                        long remaining = (rec[1] + LOCKOUT_MS - now) / 1000;
                                        if (remaining > 0) {
                                            ctx.getSource().sendFailure(Component.literal(
                                                    "§cToo many incorrect attempts. Try again in " + remaining + "s."));
                                            return 1;
                                        }
                                        // lockout expired — reset
                                        rec[0] = 0;
                                    }
                                    if (entered.equals(correct)) {
                                        pinFailures.remove(uuid);
                                        bfpAuthorized.add(uuid);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a✓ BFP admin access granted."), false);
                                    } else {
                                        if (rec[0] == 0) rec[1] = now; // start lockout window
                                        rec[0]++;
                                        long left = MAX_PIN_ATTEMPTS - rec[0];
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cIncorrect PIN." + (left > 0 ? " §7(" + left + " attempt(s) remaining)" : " §cLocked out for 5 minutes.")));
                                    }
                                    return 1;
                                })))

                .then(Commands.literal("logout")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            bfpAuthorized.remove(ctx.getSource().getPlayer().getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§7BFP admin access revoked."), false);
                            return 1;
                        }))

                .then(Commands.literal("checkin")
                        .then(Commands.argument("student_name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    String name = StringArgumentType.getString(ctx, "student_name");
                                    SessionManager.checkin(player.getUUID(), player.getName().getString(), name);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§a✓ Checked in: §f" + name + " §7as §f" + player.getName().getString()), true);
                                    return 1;
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("student_name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String name = StringArgumentType.getString(ctx, "student_name");
                                            SessionManager.checkin(target.getUUID(), target.getName().getString(), name);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a✓ Checked in: §f" + name + " §7as §f" + target.getName().getString()), true);
                                            return 1;
                                        }))))

                .then(Commands.literal("checkout")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            SessionManager.checkout(ctx.getSource().getPlayer().getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§aSession checked out."), true);
                            return 1;
                        }))

                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            SessionManager.reset(ctx.getSource().getPlayer().getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§eSession reset. Tutorial state wiped."), true);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    SessionManager.reset(target.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§eReset §f" + target.getName().getString()), true);
                                    return 1;
                                })))

                .then(Commands.literal("tutorial")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return tutorialReset(ctx, ctx.getSource().getPlayer());
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> tutorialReset(ctx, EntityArgument.getPlayer(ctx, "player")))))

                .then(Commands.literal("session")
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    StudentSession s = SessionManager.getActiveSession(
                                            ctx.getSource().getPlayer().getUUID());
                                    if (s == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§6--- Current Session ---\n" +
                                            "§eStudent: §f" + s.getStudentName() + "\n" +
                                            "§eAccount: §f" + s.getStationAccount() + "\n" +
                                            "§eStarted: §f" + s.getStartTime() + "\n" +
                                            "§eTutorial done: §f" + (s.getTutorialEndTime() != null) + "\n" +
                                            "§eSimulation: §f" + (s.getSimulationType() != null
                                                    ? s.getSimulationType() : "none") + "\n" +
                                            "§eScore: §f" + s.getSimulationScore() + "\n" +
                                            "§ePassed: §f" + s.isPassed()), false);
                                    return 1;
                                })))

                .then(Commands.literal("sessions")
                        .then(Commands.literal("list")
                                .executes(ctx -> listSessions(ctx, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listSessions(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("export")
                                .executes(BfpAdminCommands::exportSessions))
                        .then(Commands.literal("stats")
                                .executes(BfpAdminCommands::bfpSessionsStats))
                        .then(Commands.literal("today")
                                .executes(BfpAdminCommands::bfpSessionsToday))
                        .then(Commands.literal("search")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String q = "%" + StringArgumentType.getString(ctx, "query") + "%";
                                            String json = TursoClient.query(
                                                    "SELECT id, student_name, station_account, simulation_type, " +
                                                    "simulation_score, passed, status FROM sessions " +
                                                    "WHERE student_name LIKE ? OR station_account LIKE ? " +
                                                    "ORDER BY start_time DESC LIMIT 15",
                                                    q, q);
                                            JsonArray rows = TursoClient.parseRows(json);
                                            if (rows.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("No sessions matched."));
                                                return 0;
                                            }
                                            StringBuilder sb = new StringBuilder("§6Search results (")
                                                    .append(rows.size()).append("):\n");
                                            for (JsonElement el : rows) {
                                                JsonObject r = el.getAsJsonObject();
                                                sb.append("§7#").append(str(r, "id"))
                                                  .append(" §f").append(str(r, "student_name"))
                                                  .append("§7@").append(str(r, "station_account"))
                                                  .append(" §e").append(str(r, "simulation_type"))
                                                  .append(" §ascore=").append(str(r, "simulation_score"))
                                                  .append(" pass=").append(str(r, "passed"))
                                                  .append(" [").append(str(r, "status")).append("]\n");
                                            }
                                            String msg = sb.toString();
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return 1;
                                        }))))

                .then(Commands.literal("student")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String json = TursoClient.query(
                                            "SELECT id, start_time, simulation_type, simulation_score, passed, status " +
                                            "FROM sessions WHERE student_name=? ORDER BY start_time DESC LIMIT 10",
                                            name);
                                    JsonArray rows = TursoClient.parseRows(json);
                                    if (rows.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "No sessions found for: " + name));
                                        return 0;
                                    }
                                    StringBuilder sb = new StringBuilder("§6Sessions for §f" + name + "§6:\n");
                                    for (JsonElement el : rows) {
                                        JsonObject r = el.getAsJsonObject();
                                        sb.append("§7#").append(str(r, "id"))
                                          .append(" §f").append(str(r, "start_time"))
                                          .append(" §e").append(str(r, "simulation_type"))
                                          .append(" §ascore=").append(str(r, "simulation_score"))
                                          .append(" pass=").append(str(r, "passed"))
                                          .append(" [").append(str(r, "status")).append("]\n");
                                    }
                                    String msg = sb.toString();
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                    return 1;
                                })))

                .then(Commands.literal("note")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    String note = StringArgumentType.getString(ctx, "text");
                                    boolean ok = SessionManager.addNote(player.getUUID(), note);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aNoted: §f" + note), false);
                                    return 1;
                                })))

                .then(Commands.literal("confidence")
                        .then(Commands.argument("rating", DoubleArgumentType.doubleArg(1.0, 5.0))
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    double rating = DoubleArgumentType.getDouble(ctx, "rating");
                                    boolean ok = SessionManager.setConfidence(player.getUUID(), rating);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            String.format("§aConfidence rating set to §f%.1f/5.0", rating)), false);
                                    return 1;
                                })))

                .then(Commands.literal("prep_level")
                        .then(Commands.literal("none").executes(ctx -> setPrepLevel(ctx, "none")))
                        .then(Commands.literal("low").executes(ctx -> setPrepLevel(ctx, "low")))
                        .then(Commands.literal("moderate").executes(ctx -> setPrepLevel(ctx, "moderate")))
                        .then(Commands.literal("high").executes(ctx -> setPrepLevel(ctx, "high"))))

                .then(Commands.literal("score")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    return bfpScore(ctx, ctx.getSource().getPlayer(),
                                            IntegerArgumentType.getInteger(ctx, "value"));
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> bfpScore(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "value"))))))

                .then(Commands.literal("pass")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return bfpSetPassed(ctx, ctx.getSource().getPlayer(), true);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> bfpSetPassed(ctx, EntityArgument.getPlayer(ctx, "player"), true))))

                .then(Commands.literal("fail")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return bfpSetPassed(ctx, ctx.getSource().getPlayer(), false);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> bfpSetPassed(ctx, EntityArgument.getPlayer(ctx, "player"), false)))));
    }

    private static int tutorialReset(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getServer().overworld();
        SessionManager.resetTutorialOnly(target.getUUID());
        TutorialLobbyManager.initNpcs(level);
        target.teleportTo(level,
                TutorialLobbyManager.TSPAWN_X,
                TutorialLobbyManager.TSPAWN_Y,
                TutorialLobbyManager.TSPAWN_Z,
                java.util.Collections.emptySet(), 0f, 0f, true);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§eTutorial reset and §f" + target.getName().getString() + "§e teleported to tutorial lobby."), true);
        return 1;
    }

    private static int listSessions(CommandContext<CommandSourceStack> ctx, int page) {
        int offset = (page - 1) * 10;
        String json = TursoClient.query(
                "SELECT id, student_name, station_account, start_time, simulation_type, " +
                "simulation_score, passed, status FROM sessions ORDER BY start_time DESC LIMIT 10 OFFSET ?",
                offset);
        JsonArray rows = TursoClient.parseRows(json);
        if (rows.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No sessions found (page " + page + ")."));
            return 0;
        }
        StringBuilder sb = new StringBuilder("§6--- Sessions (page " + page + ") ---\n");
        for (JsonElement el : rows) {
            JsonObject r = el.getAsJsonObject();
            sb.append("§7#").append(str(r, "id"))
              .append(" §f").append(str(r, "student_name"))
              .append("§7@").append(str(r, "station_account"))
              .append(" §e").append(str(r, "simulation_type"))
              .append(" §ascore=").append(str(r, "simulation_score"))
              .append(" pass=").append(str(r, "passed"))
              .append(" [").append(str(r, "status")).append("]\n");
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int exportSessions(CommandContext<CommandSourceStack> ctx) {
        String json = TursoClient.query("SELECT * FROM sessions ORDER BY start_time DESC");
        JsonArray rows = TursoClient.parseRows(json);
        String path = "bfp_sessions_export.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("id,student_name,station_account,account_uuid,start_time,end_time,status," +
                       "tutorial_completed,tutorial_duration_s,simulation_type,simulation_score,passed,notes");
            for (JsonElement el : rows) {
                JsonObject r = el.getAsJsonObject();
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        str(r, "id"), csv(str(r, "student_name")), csv(str(r, "station_account")),
                        str(r, "account_uuid"), str(r, "start_time"), str(r, "end_time"),
                        str(r, "status"), str(r, "tutorial_completed"), str(r, "tutorial_duration_s"),
                        str(r, "simulation_type"), str(r, "simulation_score"), str(r, "passed"),
                        csv(str(r, "notes")));
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aExported " + rows.size() + " rows to §f" + path), true);
            return 1;
        } catch (IOException e) {
            BerongSMP.LOGGER.warn("[BfpAdminCommands] Export failed: {}", e.getMessage());
            ctx.getSource().sendFailure(Component.literal("Export failed: " + e.getMessage()));
            return 0;
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    private static String csv(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static int setPrepLevel(CommandContext<CommandSourceStack> ctx, String level) {
        if (!ctx.getSource().isPlayer()) return 0;
        ServerPlayer player = ctx.getSource().getPlayer();
        boolean ok = SessionManager.setPrepLevel(player.getUUID(), level);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal("No active session for this account."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§aPrep level set to §f" + level), false);
        return 1;
    }

    private static int bfpScore(CommandContext<CommandSourceStack> ctx, ServerPlayer target, int score) {
        boolean ok = SessionManager.setScore(target.getUUID(), score);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal(
                    "No active session for " + target.getName().getString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aScore for §f" + target.getName().getString() + " §aset to §f" + score + "/100"), true);
        return 1;
    }

    private static int bfpSetPassed(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean passed) {
        boolean ok = SessionManager.setPassedOverride(target.getUUID(), passed);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal(
                    "No active session for " + target.getName().getString()));
            return 0;
        }
        String label = passed ? "§aPASSED" : "§cFAILED";
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§f" + target.getName().getString() + " §7marked as " + label), true);
        return 1;
    }

    private static int bfpSessionsStats(CommandContext<CommandSourceStack> ctx) {
        if (!TursoClient.isReady()) {
            ctx.getSource().sendFailure(Component.literal("Turso not configured — stats unavailable."));
            return 0;
        }
        String json = TursoClient.query(
                "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN passed=1 THEN 1 ELSE 0 END) as passed_count, " +
                "AVG(simulation_score) as avg_score, " +
                "SUM(CASE WHEN simulation_type='FIRE' THEN 1 ELSE 0 END) as fire_count, " +
                "SUM(CASE WHEN simulation_type='EARTHQUAKE' THEN 1 ELSE 0 END) as quake_count " +
                "FROM sessions WHERE status='completed'");
        JsonArray rows = TursoClient.parseRows(json);
        if (rows.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No completed sessions found."));
            return 0;
        }
        JsonObject r = rows.get(0).getAsJsonObject();
        String total    = str(r, "total");
        String passed   = str(r, "passed_count");
        String avgScore = str(r, "avg_score");
        String fire     = str(r, "fire_count");
        String quake    = str(r, "quake_count");
        int    totalInt  = total.isEmpty()    ? 0 : (int) Double.parseDouble(total);
        int    passedInt = passed.isEmpty()   ? 0 : (int) Double.parseDouble(passed);
        double passRate  = totalInt > 0 ? (passedInt * 100.0 / totalInt) : 0;
        double avgScoreD = avgScore.isEmpty() ? 0 : Double.parseDouble(avgScore);
        String msg = "§6--- Session Stats (completed) ---\n" +
                "§eTotal sessions: §f" + totalInt + "\n" +
                "§ePassed: §f" + passedInt + " §7(" + String.format("%.1f%%", passRate) + ")\n" +
                "§eAvg score: §f" + String.format("%.1f", avgScoreD) + "\n" +
                "§eFire drills: §f" + fire + " §7| §eQuake drills: §f" + quake;
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int bfpSessionsToday(CommandContext<CommandSourceStack> ctx) {
        if (!TursoClient.isReady()) {
            ctx.getSource().sendFailure(Component.literal("Turso not configured — data unavailable."));
            return 0;
        }
        String today = LocalDate.now().toString();
        String json = TursoClient.query(
                "SELECT id, student_name, station_account, simulation_type, simulation_score, passed, status " +
                "FROM sessions WHERE start_time LIKE ? ORDER BY start_time DESC",
                today + "%");
        JsonArray rows = TursoClient.parseRows(json);
        if (rows.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No sessions today."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6Today's sessions (").append(rows.size()).append("):\n");
        for (JsonElement el : rows) {
            JsonObject r = el.getAsJsonObject();
            sb.append("§7#").append(str(r, "id"))
              .append(" §f").append(str(r, "student_name"))
              .append("§7@").append(str(r, "station_account"))
              .append(" §e").append(str(r, "simulation_type"))
              .append(" §ascore=").append(str(r, "simulation_score"))
              .append(" pass=").append(str(r, "passed"))
              .append(" [").append(str(r, "status")).append("]\n");
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
