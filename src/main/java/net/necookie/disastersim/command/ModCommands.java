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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.session.SessionManager;
import net.necookie.disastersim.session.StudentSession;
import net.necookie.disastersim.session.TursoClient;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
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
                                    if (entered.equals(correct)) {
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
                        // Note: sendSuccess is called outside the isPlayer check so
                        // the console also gets feedback when it runs this command.
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
