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
    private static final Gson GSON = new Gson();

    private static HttpClient httpClient;
    private static String pipelineUrl;
    private static String authHeader;
    private static boolean ready = false;

    private TursoClient() {}

    /** Initialises the client. Must be called before any other method. */
    public static void init(String dbUrl, String token) {
        if (dbUrl == null || dbUrl.isBlank() || token == null || token.isBlank()) {
            BerongSMP.LOGGER.warn("[TursoClient] TURSO_URL or TURSO_TOKEN is empty — session DB disabled.");
            ready = false;
            return;
        }
        String base = dbUrl.endsWith("/") ? dbUrl.substring(0, dbUrl.length() - 1) : dbUrl;
        pipelineUrl = base + "/v2/pipeline";
        authHeader = "Bearer " + token;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        ready = true;
        BerongSMP.LOGGER.info("[TursoClient] Ready. Endpoint: {}", pipelineUrl);
        createSchemaAsync();
    }

    public static void shutdown() {
        ready = false;
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
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = buildRequest(body);
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    BerongSMP.LOGGER.warn("[TursoClient] Write failed ({}): {}", resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                BerongSMP.LOGGER.warn("[TursoClient] Write error: {}", e.getMessage());
            }
        });
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

    private static void createSchemaAsync() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS sessions (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_name        TEXT    NOT NULL,
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
                    notes               TEXT
                )""";
        executeAsync(ddl).thenRun(() ->
                executeAsync("CREATE INDEX IF NOT EXISTS idx_sessions_student ON sessions(student_name)")
        ).thenRun(() ->
                executeAsync("CREATE INDEX IF NOT EXISTS idx_sessions_start ON sessions(start_time)")
        );
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
                    argObj.addProperty("value", l);
                } else if (arg instanceof Integer i) {
                    argObj.addProperty("type", "integer");
                    argObj.addProperty("value", i.longValue());
                } else if (arg instanceof Boolean b) {
                    argObj.addProperty("type", "integer");
                    argObj.addProperty("value", b ? 1 : 0);
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
