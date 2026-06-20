package net.necookie.disastersim.session;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.tutorial.TutorialSavedData;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-student sessions across shared station accounts.
 *
 * <p>Each station account (shared Minecraft login) can have at most one active
 * {@link StudentSession}. On check-in a new session starts, tutorial state is
 * wiped, and a row is inserted into Turso. Hooks in {@link TutorialSavedData}
 * completion and {@link SimulationManager#endSimulation} record progress and
 * scores back to the same row.
 */
public class SessionManager {

    private static final Map<UUID, StudentSession> activeSessions = new ConcurrentHashMap<>();
    private static MinecraftServer server;

    private SessionManager() {}

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public static void init(MinecraftServer srv) {
        server = srv;
        TursoClient.init(Config.TURSO_URL.get(), Config.TURSO_TOKEN.get());
    }

    public static void shutdown() {
        // Mark all still-active sessions as aborted
        for (Map.Entry<UUID, StudentSession> entry : activeSessions.entrySet()) {
            StudentSession s = entry.getValue();
            if (s.getDbRowId() > 0) {
                TursoClient.executeAsync(
                        "UPDATE sessions SET end_time=?, status='aborted' WHERE id=?",
                        Instant.now().toString(), s.getDbRowId());
            }
        }
        activeSessions.clear();
        TursoClient.shutdown();
    }

    // -----------------------------------------------------------------------
    // Session lifecycle
    // -----------------------------------------------------------------------

    /**
     * Starts a new student session for the given account.
     * Closes and saves any existing session for the same UUID first.
     *
     * @param accountUuid  UUID of the shared Minecraft account
     * @param accountName  Minecraft username of the account
     * @param studentName  Real name/ID of the student checking in
     */
    public static void checkin(UUID accountUuid, String accountName, String studentName) {
        // Close any in-progress session first
        checkout(accountUuid, "replaced");

        // Wipe tutorial state so the new student starts fresh
        if (server != null) {
            TutorialSavedData.get(server.overworld()).reset(accountUuid);
        }

        StudentSession session = new StudentSession(studentName, accountName, accountUuid);
        activeSessions.put(accountUuid, session);

        // Insert row into Turso; store the returned row ID for later updates
        long rowId = TursoClient.insert(
                "INSERT INTO sessions (student_name, station_account, account_uuid, start_time, status) VALUES (?,?,?,?,?)",
                studentName, accountName, accountUuid.toString(),
                session.getStartTime().toString(), "active");
        session.setDbRowId(rowId);

        BerongSMP.LOGGER.info("[SessionManager] Checked in: student='{}' account='{}' rowId={}", studentName, accountName, rowId);
    }

    /**
     * Finalises and closes the active session for the given UUID.
     * Writes {@code end_time} and {@code status='completed'} to Turso.
     */
    public static void checkout(UUID accountUuid) {
        checkout(accountUuid, "completed");
    }

    private static void checkout(UUID accountUuid, String status) {
        StudentSession session = activeSessions.remove(accountUuid);
        if (session == null) return;
        if (session.getDbRowId() > 0) {
            TursoClient.executeAsync(
                    "UPDATE sessions SET end_time=?, status=? WHERE id=?",
                    Instant.now().toString(), status, session.getDbRowId());
        }
        BerongSMP.LOGGER.info("[SessionManager] Checked out: student='{}' status='{}'", session.getStudentName(), status);
    }

    /**
     * Resets tutorial state and removes any active session without writing a DB record.
     * Used by {@code /bfp reset}.
     */
    public static void reset(UUID accountUuid) {
        StudentSession removed = activeSessions.remove(accountUuid);
        if (removed != null && removed.getDbRowId() > 0) {
            TursoClient.executeAsync("DELETE FROM sessions WHERE id=?", removed.getDbRowId());
        }
        if (server != null) {
            TutorialSavedData.get(server.overworld()).reset(accountUuid);
        }
    }

    // -----------------------------------------------------------------------
    // Integration hooks
    // -----------------------------------------------------------------------

    /** Called by TutorialManager when the player completes the tutorial. */
    public static void onTutorialComplete(ServerPlayer player) {
        StudentSession session = activeSessions.get(player.getUUID());
        if (session == null) return;
        session.setTutorialEndTime(Instant.now());
        if (session.getDbRowId() > 0) {
            TursoClient.executeAsync(
                    "UPDATE sessions SET tutorial_completed=1, tutorial_duration_s=? WHERE id=?",
                    session.getTutorialDurationSeconds(), session.getDbRowId());
        }
    }

    /** Called by SimulationManager when the simulation ends. */
    public static void onSimulationEnd(ServerPlayer player, SimulationSession sim) {
        StudentSession session = activeSessions.get(player.getUUID());
        if (session == null) return;

        String type = sim.getState() == SimulationManager.SimulationState.FIRE ? "FIRE" : "EARTHQUAKE";
        int score = sim.getState() == SimulationManager.SimulationState.FIRE
                ? Math.min(100, sim.getFiresExtinguished() * 2)
                : 0; // earthquake score TBD
        boolean passed = sim.getState() == SimulationManager.SimulationState.FIRE
                ? sim.getFiresExtinguished() >= Config.PASS_THRESHOLD_FIRE.get()
                : false;

        session.setSimulationType(type);
        session.setSimulationScore(score);
        session.setPassed(passed);

        if (session.getDbRowId() > 0) {
            TursoClient.executeAsync(
                    "UPDATE sessions SET simulation_type=?, simulation_score=?, passed=?, end_time=?, status='completed' WHERE id=?",
                    type, score, passed, Instant.now().toString(), session.getDbRowId());
        }
        activeSessions.remove(player.getUUID());
    }

    // -----------------------------------------------------------------------
    // Query helpers
    // -----------------------------------------------------------------------

    public static StudentSession getActiveSession(UUID uuid) {
        return activeSessions.get(uuid);
    }
}
