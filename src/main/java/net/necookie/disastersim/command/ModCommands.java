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
import net.minecraft.core.BlockPos;
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
import net.necookie.disastersim.tutorial.TutorialManager;
import net.necookie.disastersim.tutorial.TutorialSavedData;
import net.necookie.disastersim.registration.RegistrationManager;
import net.necookie.disastersim.registration.StudentRegistration;
import net.necookie.disastersim.world.LobbyManager;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TutorialLobbyManager;
import net.necookie.disastersim.world.building.CCSBuildingConstructor;

/**
 * Handles the registration and execution of custom commands for BerongSMP.
 */
public class ModCommands {

    /** UUIDs of players who have authenticated with the BFP admin PIN this session. */
    private static final Set<UUID> bfpAuthorized = ConcurrentHashMap.newKeySet();

    /** Clears all PIN authorizations (call on server stop/start). */
    public static void clearAuthorizations() { bfpAuthorized.clear(); }

    private static boolean isBfpAuthorized(CommandSourceStack source) {
        if (!source.isPlayer()) return true; // console always allowed
        return bfpAuthorized.contains(source.getPlayer().getUUID())
                || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    /**
     * Registers all custom commands to the command dispatcher.
     * 
     * @param dispatcher The dispatcher to register commands to.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /register <student_id> <section> <full_name> — Student self-registration.
        // Syntax puts greedy full_name last so multi-word names work (e.g., "Juan dela Cruz").
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
                                            // Also create/update the Turso session row via SessionManager
                                            SessionManager.checkin(player.getUUID(), player.getName().getString(), fullName);
                                            net.necookie.disastersim.session.TursoClient.updateStudentInfo(
                                                player.getUUID().toString(), studentId, section);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a✓ Registered as §f" + fullName +
                                                " §7(§f" + studentId + "§7) — Section §f" + section +
                                                "\n§eYou may now begin the tutorial."), false);
                                            return 1;
                                        })))));

        // /spawn_lspu — Places the LSPU CCS building at the caller's feet.
        // Restricted to game masters (op level 2+) because it modifies the world
        // permanently and is intended for admin/dev use only.
        dispatcher.register(Commands.literal("spawn_lspu")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ModCommands::spawnLSPU));

        // /get_extinguisher — Adds a Fire Extinguisher item to the caller's inventory.
        // No permission restriction; any player can use this for testing purposes.
        dispatcher.register(Commands.literal("get_extinguisher")
                .executes(ModCommands::getExtinguisher));

        // /sim_fire — Starts a FIRE simulation for the calling player immediately,
        // bypassing the lobby button.  Useful for testing without needing to use the lobby.
        dispatcher.register(Commands.literal("sim_fire")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        SimulationManager.startSimulation(
                                context.getSource().getPlayer(),
                                SimulationManager.SimulationState.FIRE);
                        return 1; // Brigadier convention: return 1 for success
                    }
                    return 0; // Return 0 if not run by a player (e.g., console)
                }));

        // /sim_earthquake [magnitude] — Starts the EARTHQUAKE simulation.
        // Omitting magnitude uses the config default; supplying it (0.1–10.0) overrides it.
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

        // /sim_magnitude <value> — Changes the magnitude of the caller's active earthquake session live.
        // Restricted to game masters so players cannot exploit it to trivialise the simulation.
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

        // /bfp — Admin commands for BFP student session management.
        // Access requires either OP level 2+ OR a successful /bfp login <pin>.
        dispatcher.register(Commands.literal("bfp")
                .requires(ModCommands::isBfpAuthorized)

                // /bfp login <pin>  — authenticate with the admin PIN
                .then(Commands.literal("login")
                        .then(Commands.argument("pin", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    String entered = StringArgumentType.getString(ctx, "pin");
                                    String correct = Config.BFP_ADMIN_PIN.get();
                                    if (correct.isBlank()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cBFP PIN is not configured. Set 'bfpAdminPin' in berongsmp-common.toml."));
                                    } else if (entered.equals(correct)) {
                                        bfpAuthorized.add(player.getUUID());
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a✓ BFP admin access granted."), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cIncorrect PIN."));
                                    }
                                    return 1;
                                })))

                // /bfp logout  — revoke PIN-based access
                .then(Commands.literal("logout")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            bfpAuthorized.remove(ctx.getSource().getPlayer().getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§7BFP admin access revoked."), false);
                            return 1;
                        }))

                // /bfp checkin <student_name>  — check in the command source player
                // /bfp checkin <player> <student_name>  — check in a specific player
                .then(Commands.literal("checkin")
                        .then(Commands.argument("student_name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    String name = StringArgumentType.getString(ctx, "student_name");
                                    SessionManager.checkin(player.getUUID(), player.getName().getString(), name);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a✓ Checked in: §f" + name + " §7as §f" + player.getName().getString()), true);
                                    return 1;
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("student_name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String name = StringArgumentType.getString(ctx, "student_name");
                                            SessionManager.checkin(target.getUUID(), target.getName().getString(), name);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ Checked in: §f" + name + " §7as §f" + target.getName().getString()), true);
                                            return 1;
                                        }))))

                // /bfp checkout  — finalise the current player's session
                .then(Commands.literal("checkout")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            UUID uuid = ctx.getSource().getPlayer().getUUID();
                            SessionManager.checkout(uuid);
                            ctx.getSource().sendSuccess(() -> Component.literal("§aSession checked out."), true);
                            return 1;
                        }))

                // /bfp reset [player]  — wipe tutorial state + discard session, no DB record
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
                                    ctx.getSource().sendSuccess(() -> Component.literal("§eReset §f" + target.getName().getString()), true);
                                    return 1;
                                })))

                // /bfp tutorial [player]  — reset tutorial + teleport to tutorial lobby (no quit needed)
                .then(Commands.literal("tutorial")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return tutorialReset(ctx, ctx.getSource().getPlayer());
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    return tutorialReset(ctx, target);
                                })))

                // /bfp session info  — print current session details
                .then(Commands.literal("session")
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    StudentSession s = SessionManager.getActiveSession(ctx.getSource().getPlayer().getUUID());
                                    if (s == null) {
                                        ctx.getSource().sendFailure(Component.literal("No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§6--- Current Session ---\n" +
                                            "§eStudent: §f" + s.getStudentName() + "\n" +
                                            "§eAccount: §f" + s.getStationAccount() + "\n" +
                                            "§eStarted: §f" + s.getStartTime() + "\n" +
                                            "§eTutorial done: §f" + (s.getTutorialEndTime() != null) + "\n" +
                                            "§eSimulation: §f" + (s.getSimulationType() != null ? s.getSimulationType() : "none") + "\n" +
                                            "§eScore: §f" + s.getSimulationScore() + "\n" +
                                            "§ePassed: §f" + s.isPassed()), false);
                                    return 1;
                                })))

                // /bfp sessions list [page]  — list 10 most recent sessions from DB
                .then(Commands.literal("sessions")
                        .then(Commands.literal("list")
                                .executes(ctx -> listSessions(ctx, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listSessions(ctx, IntegerArgumentType.getInteger(ctx, "page")))))

                        // /bfp sessions export  — write CSV to run/bfp_sessions_export.csv
                        .then(Commands.literal("export")
                                .executes(ModCommands::exportSessions)))

                // /bfp student <name>  — look up all sessions for a student
                .then(Commands.literal("student")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String json = TursoClient.query(
                                            "SELECT id, start_time, simulation_type, simulation_score, passed, status FROM sessions WHERE student_name=? ORDER BY start_time DESC LIMIT 10",
                                            name);
                                    JsonArray rows = TursoClient.parseRows(json);
                                    if (rows.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("No sessions found for: " + name));
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
                                }))));

        // /sim_stop — Ends the calling player's active simulation early.
        // Works the same as the timer expiring naturally: restores the structure,
        // clears the HUD, and teleports the player back to the lobby.
        dispatcher.register(Commands.literal("sim_stop")
                .executes(context -> {
                    if (context.getSource().isPlayer()) {
                        UUID uuid = context.getSource().getPlayer().getUUID();
                        SimulationManager.endSimulation(uuid);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Simulation stopped."), true);
                    return 1;
                }));

        // /sim_status [player] — show live status of self or (op-only) another player's session.
        dispatcher.register(Commands.literal("sim_status")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simStatus(ctx, ctx.getSource().getPlayer());
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                        .executes(ctx -> simStatus(ctx, EntityArgument.getPlayer(ctx, "player")))));

        // /sim_list — op command to list all currently running simulations.
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

        // /sim_freeze [player] — pause the simulation timer for self or another player (op).
        dispatcher.register(Commands.literal("sim_freeze")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simFreeze(ctx, ctx.getSource().getPlayer(), true);
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> simFreeze(ctx, EntityArgument.getPlayer(ctx, "player"), true))));

        // /sim_unfreeze [player] — resume a previously frozen timer.
        dispatcher.register(Commands.literal("sim_unfreeze")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) return 0;
                    return simFreeze(ctx, ctx.getSource().getPlayer(), false);
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> simFreeze(ctx, EntityArgument.getPlayer(ctx, "player"), false))));

        // /sim_time set <seconds> — set remaining time; add <seconds> — add/subtract seconds.
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

        // /get_co2_extinguisher — gives the CO2 extinguisher for Class C electrical fires.
        dispatcher.register(Commands.literal("get_co2_extinguisher")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        ctx.getSource().sendFailure(Component.literal("This command can only be run by a player."));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayer();
                    player.getInventory().add(BerongSMP.CO2_EXTINGUISHER.get().getDefaultInstance());
                    ctx.getSource().sendSuccess(() -> Component.literal("§aCO2 Extinguisher added to your inventory! Use it on burning computer blocks."), true);
                    return 1;
                }));

        // /bfp note, /bfp confidence, /bfp prep_level, /bfp score, /bfp pass, /bfp fail,
        // /bfp sessions stats, /bfp sessions today, /bfp sessions search are added as
        // additional sub-commands by re-registering branches onto the existing /bfp tree.
        // NeoForge/Brigadier merges literal nodes with the same name on the same dispatcher.
        dispatcher.register(Commands.literal("bfp")
                .requires(ModCommands::isBfpAuthorized)

                // /bfp note <text> — append instructor observation to active session
                .then(Commands.literal("note")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    String note = StringArgumentType.getString(ctx, "text");
                                    boolean ok = SessionManager.addNote(player.getUUID(), note);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aNoted: §f" + note), false);
                                    return 1;
                                })))

                // /bfp confidence <1-5> — instructor confidence rating
                .then(Commands.literal("confidence")
                        .then(Commands.argument("rating", DoubleArgumentType.doubleArg(1.0, 5.0))
                                .executes(ctx -> {
                                    if (!ctx.getSource().isPlayer()) return 0;
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    double rating = DoubleArgumentType.getDouble(ctx, "rating");
                                    boolean ok = SessionManager.setConfidence(player.getUUID(), rating);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("No active session for this account."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            String.format("§aConfidence rating set to §f%.1f/5.0", rating)), false);
                                    return 1;
                                })))

                // /bfp prep_level <none|low|moderate|high> — instructor prep assessment
                .then(Commands.literal("prep_level")
                        .then(Commands.literal("none").executes(ctx -> setPrepLevel(ctx, "none")))
                        .then(Commands.literal("low").executes(ctx -> setPrepLevel(ctx, "low")))
                        .then(Commands.literal("moderate").executes(ctx -> setPrepLevel(ctx, "moderate")))
                        .then(Commands.literal("high").executes(ctx -> setPrepLevel(ctx, "high"))))

                // /bfp score <0-100> [player] — manually override simulation score
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

                // /bfp pass [player] — mark session as passed
                .then(Commands.literal("pass")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return bfpSetPassed(ctx, ctx.getSource().getPlayer(), true);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> bfpSetPassed(ctx, EntityArgument.getPlayer(ctx, "player"), true))))

                // /bfp fail [player] — mark session as failed
                .then(Commands.literal("fail")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            return bfpSetPassed(ctx, ctx.getSource().getPlayer(), false);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> bfpSetPassed(ctx, EntityArgument.getPlayer(ctx, "player"), false))))

                // /bfp sessions stats — aggregate stats from Turso
                .then(Commands.literal("sessions")
                        .then(Commands.literal("stats")
                                .executes(ModCommands::bfpSessionsStats))
                        // /bfp sessions today — list sessions started today
                        .then(Commands.literal("today")
                                .executes(ModCommands::bfpSessionsToday))
                        // /bfp sessions search <query> — search by student name or account
                        .then(Commands.literal("search")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String q = "%" + StringArgumentType.getString(ctx, "query") + "%";
                                            String json = TursoClient.query(
                                                    "SELECT id, student_name, station_account, simulation_type, simulation_score, passed, status " +
                                                    "FROM sessions WHERE student_name LIKE ? OR station_account LIKE ? ORDER BY start_time DESC LIMIT 15",
                                                    q, q);
                                            JsonArray rows = TursoClient.parseRows(json);
                                            if (rows.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("No sessions matched."));
                                                return 0;
                                            }
                                            StringBuilder sb = new StringBuilder("§6Search results (").append(rows.size()).append("):\n");
                                            for (JsonElement el : rows) {
                                                JsonObject r = el.getAsJsonObject();
                                                sb.append("§7#").append(str(r,"id"))
                                                  .append(" §f").append(str(r,"student_name"))
                                                  .append("§7@").append(str(r,"station_account"))
                                                  .append(" §e").append(str(r,"simulation_type"))
                                                  .append(" §ascore=").append(str(r,"simulation_score"))
                                                  .append(" pass=").append(str(r,"passed"))
                                                  .append(" [").append(str(r,"status")).append("]\n");
                                            }
                                            String msg = sb.toString();
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return 1;
                                        })))));
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

    private static int tutorialReset(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getServer().overworld();
        SessionManager.resetTutorialOnly(target.getUUID());
        TutorialLobbyManager.initNpcs(level); // also fixes any duplicate NPCs
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
                "SELECT id, student_name, station_account, start_time, simulation_type, simulation_score, passed, status FROM sessions ORDER BY start_time DESC LIMIT 10 OFFSET ?",
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
            pw.println("id,student_name,station_account,account_uuid,start_time,end_time,status,tutorial_completed,tutorial_duration_s,simulation_type,simulation_score,passed,notes");
            for (JsonElement el : rows) {
                JsonObject r = el.getAsJsonObject();
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        str(r,"id"), csv(str(r,"student_name")), csv(str(r,"station_account")),
                        str(r,"account_uuid"), str(r,"start_time"), str(r,"end_time"),
                        str(r,"status"), str(r,"tutorial_completed"), str(r,"tutorial_duration_s"),
                        str(r,"simulation_type"), str(r,"simulation_score"), str(r,"passed"),
                        csv(str(r,"notes")));
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§aExported " + rows.size() + " rows to §f" + path), true);
            return 1;
        } catch (IOException e) {
            BerongSMP.LOGGER.warn("[ModCommands] Export failed: {}", e.getMessage());
            ctx.getSource().sendFailure(Component.literal("Export failed: " + e.getMessage()));
            return 0;
        }
    }

    /** Safely reads a string value from a row JsonObject. */
    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    /** Wraps a CSV cell in quotes if it contains a comma or quote. */
    private static String csv(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Formats ticks into M:SS display string. */
    private static String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static int simStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        SimulationSession session = SimulationManager.getSession(target.getUUID());
        if (session == null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7" + target.getName().getString() + " has no active simulation."), false);
            return 1;
        }
        int secs = (session.getTimerTicks() + 19) / 20;
        String state = session.getState().name();
        String phase = session.getState() == SimulationManager.SimulationState.EARTHQUAKE && session.getQuakePhase() != null
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
            ctx.getSource().sendFailure(Component.literal(target.getName().getString() + " has no active simulation."));
            return 0;
        }
        session.setFrozen(freeze);
        String action = freeze ? "§bFROZEN" : "§aRESUMED";
        target.sendSystemMessage(Component.literal("§7Your simulation timer has been " + action + "§7."));
        ctx.getSource().sendSuccess(() -> Component.literal(
                action + " §7timer for §f" + target.getName().getString()), true);
        return 1;
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
            ctx.getSource().sendFailure(Component.literal("No active session for " + target.getName().getString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aScore for §f" + target.getName().getString() + " §aset to §f" + score + "/100"), true);
        return 1;
    }

    private static int bfpSetPassed(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean passed) {
        boolean ok = SessionManager.setPassedOverride(target.getUUID(), passed);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal("No active session for " + target.getName().getString()));
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
        String total = str(r, "total");
        String passed = str(r, "passed_count");
        String avgScore = str(r, "avg_score");
        String fire = str(r, "fire_count");
        String quake = str(r, "quake_count");
        int totalInt = total.isEmpty() ? 0 : (int) Double.parseDouble(total);
        int passedInt = passed.isEmpty() ? 0 : (int) Double.parseDouble(passed);
        double passRate = totalInt > 0 ? (passedInt * 100.0 / totalInt) : 0;
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
        String today = LocalDate.now().toString(); // e.g. "2026-06-22"
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
            sb.append("§7#").append(str(r,"id"))
              .append(" §f").append(str(r,"student_name"))
              .append("§7@").append(str(r,"station_account"))
              .append(" §e").append(str(r,"simulation_type"))
              .append(" §ascore=").append(str(r,"simulation_score"))
              .append(" pass=").append(str(r,"passed"))
              .append(" [").append(str(r,"status")).append("]\n");
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
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
