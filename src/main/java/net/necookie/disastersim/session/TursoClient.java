package net.necookie.disastersim.session;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.necookie.disastersim.BerongSMP;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight HTTP client for the Turso libSQL REST API.
 *
 * All writes use {@link #executeAsync} (fire-and-forget) so the server tick
 * thread is never blocked. Reads for admin commands use {@link #query}, which
 * blocks the command thread for at most {@link #TIMEOUT_SECONDS} seconds.
 *
 * Call {@link #init} on server start and {@link #shutdown} on server stop.
 */
public class TursoClient {

    private static final int TIMEOUT_SECONDS = 10;
    /** Max time {@link #shutdown()} waits for the write queue to drain before giving up. */
    private static final int SHUTDOWN_DRAIN_SECONDS = 5;
    private static final Gson GSON = new Gson();

    private static HttpClient httpClient;
    private static String pipelineUrl;
    private static String authHeader;
    private static volatile boolean ready = false;

    // Dedicated pool for the blocking HTTP writes. Keeping them off ForkJoinPool.commonPool()
    // avoids starving the shared pool (which parallel streams elsewhere also use) and bounds the
    // number of concurrent Turso connections. Daemon threads so they never block JVM shutdown.
    private static ExecutorService writeExecutor;

    private TursoClient() {}

    /** Initialises the client. Must be called before any other method. */
    public static void init(String dbUrl, String token) {
        if (dbUrl == null || dbUrl.isBlank()) {
            BerongSMP.LOGGER.info("[TursoClient] tursoUrl not configured — session DB disabled.");
            ready = false;
            return;
        }
        if (token == null || token.isBlank()) {
            BerongSMP.LOGGER.warn("[TursoClient] tursoUrl is set but tursoToken is missing — all requests will fail with 401. Configure tursoToken in berongsmp-common.toml.");
            ready = false;
            return;
        }
        String base = dbUrl.endsWith("/") ? dbUrl.substring(0, dbUrl.length() - 1) : dbUrl;
        pipelineUrl = base + "/v2/pipeline";
        authHeader = "Bearer " + token;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        writeExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread t = new Thread(runnable, "TursoClient-Writer");
            t.setDaemon(true);
            return t;
        });
        ready = true;
        BerongSMP.LOGGER.info("[TursoClient] Ready. Endpoint: {}", pipelineUrl);
        createSchemaAsync();
    }

    /**
     * Stops accepting new writes, then blocks (up to {@link #SHUTDOWN_DRAIN_SECONDS}) for the
     * write queue to actually drain before tearing down {@code httpClient}. Nulling the client
     * immediately (the previous behavior) raced with any already-queued write — e.g.
     * {@code SessionManager.shutdown()}'s "mark session aborted" calls, submitted just before this
     * runs — which would then NPE inside the executor thread and silently lose that write instead
     * of completing it.
     */
    public static void shutdown() {
        ready = false;
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(SHUTDOWN_DRAIN_SECONDS, TimeUnit.SECONDS)) {
                    BerongSMP.LOGGER.warn("[TursoClient] Write queue did not drain within {}s on shutdown — "
                            + "some pending writes may not have completed.", SHUTDOWN_DRAIN_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            writeExecutor = null;
        }
        httpClient = null;
    }

    public static boolean isReady() { return ready; }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Executes a write statement asynchronously. Errors are logged but never
     * propagate — a DB failure must not crash the game.
     *
     * @return a future that completes when the request finishes (or fails silently).
     */
    public static CompletableFuture<Void> executeAsync(String sql, Object... args) {
        if (!ready) return CompletableFuture.completedFuture(null);
        String body = buildBody(sql, args);
        try {
            return CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest req = buildRequest(body);
                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() >= 400) {
                        BerongSMP.LOGGER.warn("[TursoClient] Write failed HTTP {}: {}", resp.statusCode(), resp.body());
                    } else if (resp.body().contains("\"type\":\"error\"")) {
                        BerongSMP.LOGGER.warn("[TursoClient] Turso SQL error: {}", resp.body());
                    }
                } catch (Exception e) {
                    BerongSMP.LOGGER.warn("[TursoClient] Write error: {}", e.getMessage());
                }
            }, writeExecutor);
        } catch (RejectedExecutionException e) {
            // writeExecutor is mid-shutdown — the submitting thread lost a narrow race against
            // TursoClient.shutdown(). Nothing to recover; just don't crash the caller.
            BerongSMP.LOGGER.warn("[TursoClient] Write rejected during shutdown, dropped.");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Executes a read query synchronously and returns the JSON response body.
     * Returns {@code null} on error.
     */
    public static String query(String sql, Object... args) {
        if (!ready) return null;
        String body = buildBody(sql, args);
        try {
            HttpRequest req = buildRequest(body);
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                BerongSMP.LOGGER.warn("[TursoClient] Query failed ({}): {}", resp.statusCode(), resp.body());
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            BerongSMP.LOGGER.warn("[TursoClient] Query error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Executes an INSERT and returns the auto-incremented row ID, or -1 on failure.
     * Blocks the calling thread.
     */
    public static long insert(String sql, Object... args) {
        String json = query(sql, args);
        if (json == null) return -1;
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray results = root.getAsJsonArray("results");
            if (results == null || results.isEmpty()) return -1;
            JsonObject first = results.get(0).getAsJsonObject();
            JsonObject response = first.getAsJsonObject("response");
            if (response == null) return -1;
            JsonObject result = response.getAsJsonObject("result");
            if (result == null) return -1;
            JsonElement lastInsert = result.get("last_insert_rowid");
            return lastInsert != null ? lastInsert.getAsLong() : -1;
        } catch (Exception e) {
            BerongSMP.LOGGER.warn("[TursoClient] Failed to parse INSERT row ID: {}", e.getMessage());
            return -1;
        }
    }

    // -----------------------------------------------------------------------
    // Row parsing helpers
    // -----------------------------------------------------------------------

    /**
     * Extracts rows from a Turso pipeline response as an array of JsonObjects.
     * Each object has keys matching the column names.
     */
    public static JsonArray parseRows(String json) {
        JsonArray out = new JsonArray();
        if (json == null) return out;
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray results = root.getAsJsonArray("results");
            if (results == null || results.isEmpty()) return out;
            JsonObject first = results.get(0).getAsJsonObject();
            JsonObject response = first.getAsJsonObject("response");
            if (response == null) return out;
            JsonObject result = response.getAsJsonObject("result");
            if (result == null) return out;

            JsonArray cols = result.getAsJsonArray("cols");
            JsonArray rows = result.getAsJsonArray("rows");
            if (cols == null || rows == null) return out;

            String[] colNames = new String[cols.size()];
            for (int i = 0; i < cols.size(); i++) {
                colNames[i] = cols.get(i).getAsJsonObject().get("name").getAsString();
            }
            for (JsonElement rowEl : rows) {
                JsonArray row = rowEl.getAsJsonArray();
                JsonObject obj = new JsonObject();
                for (int i = 0; i < colNames.length && i < row.size(); i++) {
                    JsonElement cell = row.get(i);
                    if (cell.isJsonObject()) {
                        JsonObject cellObj = cell.getAsJsonObject();
                        JsonElement val = cellObj.get("value");
                        obj.add(colNames[i], val != null ? val : cell);
                    } else {
                        obj.add(colNames[i], cell);
                    }
                }
                out.add(obj);
            }
        } catch (Exception e) {
            BerongSMP.LOGGER.warn("[TursoClient] Failed to parse rows: {}", e.getMessage());
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Session telemetry write helpers
    // -----------------------------------------------------------------------

    /** Updates student identity columns on the most recent active row for this UUID. */
    public static void updateStudentInfo(String accountUuid, String studentId, String section) {
        executeAsync(
            "UPDATE sessions SET student_id=?, section=? WHERE account_uuid=? AND status='active'",
            studentId, section, accountUuid);
    }

    /** Writes the serialized event log JSON to the session row identified by its DB id. */
    public static void updateEventLog(long sessionId, String eventLogJson) {
        executeAsync("UPDATE sessions SET event_log=? WHERE id=?", eventLogJson, sessionId);
    }

    // -----------------------------------------------------------------------
    // Schema
    // -----------------------------------------------------------------------

    private static void createSchemaAsync() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS sessions (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_name        TEXT    NOT NULL,
                    student_id          TEXT,
                    section             TEXT,
                    station_account     TEXT    NOT NULL,
                    account_uuid        TEXT    NOT NULL,
                    start_time          TEXT    NOT NULL,
                    end_time            TEXT,
                    status              TEXT    DEFAULT 'active',
                    tutorial_completed  INTEGER DEFAULT 0,
                    tutorial_duration_s INTEGER,
                    simulation_type     TEXT,
                    simulation_score    INTEGER DEFAULT 0,
                    passed              INTEGER DEFAULT 0,
                    event_log           TEXT,
                    prep_level          TEXT,
                    confidence          REAL,
                    bfp_notes           TEXT,
                    notes               TEXT
                )""";
        // Run CREATE TABLE first, then add columns to existing DBs (safe to ignore errors)
        executeAsync(ddl)
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN student_id TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN section TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN event_log TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN prep_level TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN confidence REAL"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN bfp_notes TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN move_log_csv TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN fire_log_csv TEXT"))
            .thenRun(() -> silentAlter("ALTER TABLE sessions ADD COLUMN username TEXT"))
            .thenRun(() -> executeAsync("CREATE INDEX IF NOT EXISTS idx_sessions_student ON sessions(student_name)"))
            .thenRun(() -> executeAsync("CREATE INDEX IF NOT EXISTS idx_sessions_start ON sessions(start_time)"))
            .thenRun(() -> executeAsync("CREATE INDEX IF NOT EXISTS idx_sessions_username ON sessions(username)"));

        // Separate account table for the /register + /login username/password system — decoupled
        // from `sessions` (one account row can have many session rows across return visits).
        //
        // IMPORTANT: this Turso database is SHARED with the BERONG_SMP_WEB dashboard, which
        // already owns a table literally named `users` for its own admin/staff logins
        // (id, username, password_hash, created_at, role, status — no student_id/section/
        // full_name/tutorial_completed columns at all). `CREATE TABLE IF NOT EXISTS users` was a
        // silent no-op against that pre-existing table, so every mod-side INSERT referencing
        // student_id/section/full_name failed with "no such column" (surfaced to players as
        // "db_error"), and any student who happened to pick a username matching a real dashboard
        // admin account (e.g. the developer's own login) got a spurious "already taken". Named
        // `student_accounts` instead — verify against the live schema
        // (`SELECT sql FROM sqlite_master WHERE name=...`) before ever reusing a bare, generic
        // table name in this shared database again.
        String studentAccountsDdl = """
                CREATE TABLE IF NOT EXISTS student_accounts (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    username            TEXT    NOT NULL UNIQUE,
                    password_hash       TEXT    NOT NULL,
                    student_id          TEXT,
                    section             TEXT,
                    full_name           TEXT,
                    tutorial_completed  INTEGER DEFAULT 0,
                    created_at          TEXT    NOT NULL,
                    last_login          TEXT
                )""";
        executeAsync(studentAccountsDdl)
            .thenRun(() -> executeAsync("CREATE UNIQUE INDEX IF NOT EXISTS idx_student_accounts_username ON student_accounts(username)"));
    }

    /** Runs an ALTER TABLE statement, silently swallowing the error if the column already exists. */
    private static void silentAlter(String sql) {
        if (!ready) return;
        String body = buildBody(sql, new Object[0]);
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest req = buildRequest(body);
                    httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    // Status is intentionally ignored — Turso returns an error if the column already
                    // exists, which is the expected case on all subsequent server starts.
                } catch (Exception e) {
                    BerongSMP.LOGGER.debug("[TursoClient] silentAlter skipped (column may already exist): {}", e.getMessage());
                }
            }, writeExecutor);
        } catch (RejectedExecutionException e) {
            // Shutting down mid schema-creation chain — safe to drop, nothing depends on this.
        }
    }

    private static HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(pipelineUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static String buildBody(String sql, Object[] args) {
        JsonObject stmt = new JsonObject();
        stmt.addProperty("sql", sql);

        if (args != null && args.length > 0) {
            JsonArray argsArray = new JsonArray();
            for (Object arg : args) {
                JsonObject argObj = new JsonObject();
                if (arg == null) {
                    argObj.addProperty("type", "null");
                    argObj.add("value", com.google.gson.JsonNull.INSTANCE);
                } else if (arg instanceof Long l) {
                    argObj.addProperty("type", "integer");
                    argObj.addProperty("value", l.toString());
                } else if (arg instanceof Integer i) {
                    argObj.addProperty("type", "integer");
                    argObj.addProperty("value", Integer.toString(i));
                } else if (arg instanceof Boolean b) {
                    argObj.addProperty("type", "integer");
                    argObj.addProperty("value", b ? "1" : "0");
                } else {
                    argObj.addProperty("type", "text");
                    argObj.addProperty("value", arg.toString());
                }
                argsArray.add(argObj);
            }
            stmt.add("args", argsArray);
        }

        JsonObject execute = new JsonObject();
        execute.addProperty("type", "execute");
        execute.add("stmt", stmt);

        JsonArray requests = new JsonArray();
        requests.add(execute);

        JsonObject body = new JsonObject();
        body.add("requests", requests);

        return GSON.toJson(body);
    }
}
