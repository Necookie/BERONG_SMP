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

    /**
     * Resets ONLY the in-game tutorial progress for this account, without touching
     * the active StudentSession or the DB row. Used by {@code /bfp tutorial} so that
     * a student can redo the tutorial without losing their session.
     */
    public static void resetTutorialOnly(UUID accountUuid) {
        if (server != null) {
            TutorialSavedData.get(server.overworld()).reset(accountUuid);
        }
        StudentSession session = activeSessions.get(accountUuid);
        if (session != null) {
            session.setTutorialEndTime(null);
        }
    }

    // -----------------------------------------------------------------------
    // Integration hooks
    // -----------------------------------------------------------------------

    /** Called by TutorialManager when the player completes the tutorial. */
    public static void onTutorialComplete(ServerPlayer player) {
        StudentSession session = activeSessions.get(player.getUUID());
        if (session != null) {
            session.setTutorialEndTime(Instant.now());
        }
        // Always write by UUID subquery — no rowId dependency.
        if (TursoClient.isReady()) {
            long duration = (session != null) ? session.getTutorialDurationSeconds() : -1L;
            TursoClient.executeAsync(
                    "UPDATE sessions SET tutorial_completed=1, tutorial_duration_s=?" +
                    " WHERE id=(SELECT id FROM sessions WHERE account_uuid=? AND status='active'" +
                    " ORDER BY id DESC LIMIT 1)",
                    duration > 0 ? duration : null,
                    player.getStringUUID());
        }
    }

    /**
     * Called by SimulationManager when the simulation ends.
     * Only updates in-memory StudentSession state and removes from the map.
     * All Turso writes (simulation_type, score, event_log, status) are handled
     * directly by SimulationManager.endSimulation using the row ID it already holds.
     */
    public static void onSimulationEnd(ServerPlayer player, SimulationSession sim) {
        String type = sim.getState().isFire() ? "FIRE" : "EARTHQUAKE";
        int score = sim.getState().isFire()
                ? Math.min(100, sim.getFiresExtinguished() * 2)
                : 0;
        boolean passed = sim.getState().isFire()
                && sim.getFiresExtinguished() >= Config.PASS_THRESHOLD_FIRE.get();

        StudentSession session = activeSessions.get(player.getUUID());
        if (session == null) return;

        session.setSimulationType(type);
        session.setSimulationScore(score);
        session.setPassed(passed);
        activeSessions.remove(player.getUUID());
    }

    /**
     * Synchronously queries the DB for the row ID of the most recent active session
     * for the given account UUID. Returns -1 when not found or DB is unavailable.
     * Used as a fallback in endSimulation when no in-memory StudentSession exists.
     */
    public static long findActiveSessionRowId(UUID accountUuid) {
        if (!TursoClient.isReady()) return -1L;
        String json = TursoClient.query(
                "SELECT id FROM sessions WHERE account_uuid=? AND status='active' ORDER BY id DESC LIMIT 1",
                accountUuid.toString());
        if (json == null) return -1L;
        com.google.gson.JsonArray rows = TursoClient.parseRows(json);
        if (rows.isEmpty()) return -1L;
        com.google.gson.JsonElement idEl = rows.get(0).getAsJsonObject().get("id");
        return (idEl != null && !idEl.isJsonNull()) ? idEl.getAsLong() : -1L;
    }

    // -----------------------------------------------------------------------
    // Query helpers
    // -----------------------------------------------------------------------

    public static StudentSession getActiveSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    /** Appends a note to the in-memory session and persists it asynchronously. */
    public static boolean addNote(UUID uuid, String note) {
        StudentSession session = activeSessions.get(uuid);
        if (session == null) return false;
        String existing = session.getBfpNotes();
        String updated = (existing == null || existing.isBlank()) ? note : existing + " | " + note;
        session.setBfpNotes(updated);
        if (TursoClient.isReady() && session.getDbRowId() > 0) {
            TursoClient.executeAsync("UPDATE sessions SET bfp_notes=? WHERE id=?", updated, session.getDbRowId());
        }
        return true;
    }

    /** Sets the instructor confidence rating (1.0–5.0) for the active session. */
    public static boolean setConfidence(UUID uuid, double confidence) {
        StudentSession session = activeSessions.get(uuid);
        if (session == null) return false;
        session.setConfidence(confidence);
        if (TursoClient.isReady() && session.getDbRowId() > 0) {
            TursoClient.executeAsync("UPDATE sessions SET confidence=? WHERE id=?", confidence, session.getDbRowId());
        }
        return true;
    }

    /** Sets the prep_level field for the active session. */
    public static boolean setPrepLevel(UUID uuid, String level) {
        StudentSession session = activeSessions.get(uuid);
        if (session == null) return false;
        session.setPrepLevel(level);
        if (TursoClient.isReady() && session.getDbRowId() > 0) {
            TursoClient.executeAsync("UPDATE sessions SET prep_level=? WHERE id=?", level, session.getDbRowId());
        }
        return true;
    }

    /** Overrides the simulation score for the active session. */
    public static boolean setScore(UUID uuid, int score) {
        StudentSession session = activeSessions.get(uuid);
        if (session == null) return false;
        session.setSimulationScore(Math.max(0, Math.min(100, score)));
        if (TursoClient.isReady() && session.getDbRowId() > 0) {
            TursoClient.executeAsync("UPDATE sessions SET simulation_score=? WHERE id=?",
                    session.getSimulationScore(), session.getDbRowId());
        }
        return true;
    }

    /** Overrides the passed flag for the active session. */
    public static boolean setPassedOverride(UUID uuid, boolean passed) {
        StudentSession session = activeSessions.get(uuid);
        if (session == null) return false;
        session.setPassed(passed);
        if (TursoClient.isReady() && session.getDbRowId() > 0) {
            TursoClient.executeAsync("UPDATE sessions SET passed=? WHERE id=?", passed, session.getDbRowId());
        }
        return true;
    }
}
