package net.necookie.disastersim.session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.common.player.PlayerLifecycleRegistry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Per-station "who is logged in right now" state for the {@code /register}/{@code /login} account
 * system backed by the Turso {@code users} table (see {@link TursoClient#createSchemaAsync}).
 *
 * <p>Transient, keyed by the shared station's Minecraft account UUID — same idiom as
 * {@code DropAndRollManager.droppedTicksRemaining} / {@code TutorialManager.holdOnTimers}. A
 * returning student's {@code tutorial_completed} flag (persisted in {@code users}, distinct from
 * the in-world {@code AcademyProgress}/{@code AcademySavedData}) is what lets a re-logged-in
 * player satisfy the lobby's "Academy certified" gate without replaying the tutorial in this
 * session — see {@code LobbyManager}.
 */
public final class AuthManager {

    static {
        // A station's login state is per sit-down; the next student must /login or /register
        // again rather than silently inheriting whoever was here before.
        PlayerLifecycleRegistry.registerLogoutHook(player -> logout(player.getUUID()));
    }

    /** Forces this class to load so the static block above actually registers — see DuckCoverHoldManager. */
    public static void bootstrap() {}

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]{3,16}");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 5L * 60 * 1000; // 5 minutes

    private static final Map<UUID, AuthAccount> active = new ConcurrentHashMap<>();
    // long[0] = consecutive failure count, long[1] = first-failure epoch-ms — mirrors BfpAdminCommands.pinFailures.
    private static final Map<UUID, long[]> loginFailures = new ConcurrentHashMap<>();

    // PBKDF2 (~100-200ms) + the Turso HTTP round-trip must never run on the server/command thread.
    private static final ExecutorService AUTH_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread t = new Thread(runnable, "AuthManager-Worker");
        t.setDaemon(true);
        return t;
    });

    private AuthManager() {}

    public record AuthAccount(long userId, String username, String studentId, String section,
                               String fullName, boolean tutorialCompleted) {}

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public static AuthAccount current(UUID uuid) {
        return active.get(uuid);
    }

    public static boolean isTutorialCompleted(UUID uuid) {
        AuthAccount account = active.get(uuid);
        return account != null && account.tutorialCompleted();
    }

    /** Flips the logged-in account's certified flag, in-memory and in Turso. No-op if nobody is logged in. */
    public static void markTutorialCompleted(UUID uuid) {
        AuthAccount account = active.get(uuid);
        if (account == null || account.tutorialCompleted()) return;
        active.put(uuid, new AuthAccount(account.userId(), account.username(), account.studentId(),
                account.section(), account.fullName(), true));
        TursoClient.executeAsync("UPDATE users SET tutorial_completed=1 WHERE id=?", account.userId());
    }

    public static void logout(UUID uuid) {
        active.remove(uuid);
    }

    /**
     * Creates a new account and logs the station into it. Runs the Turso lookup/hash/insert off
     * the calling thread; {@code onSuccess}/{@code onError} are invoked back on the server thread
     * via {@link MinecraftServer#execute}. {@code onSuccess} receives {@code true} when Turso is
     * unavailable — registration still succeeds locally (via the caller's own
     * {@code RegistrationManager} call) but without a persistent account, so login/history won't
     * work later; the caller is expected to warn the player about that.
     */
    public static void registerAsync(ServerPlayer player, String username, String password,
                                      String studentId, String section, String fullName,
                                      Consumer<Boolean> onSuccess, Consumer<String> onError) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        UUID uuid = player.getUUID();
        if (!TursoClient.isReady()) {
            server.execute(() -> onSuccess.accept(true));
            return;
        }
        CompletableFuture.runAsync(() -> {
            String json = TursoClient.query("SELECT id FROM users WHERE username=?", username);
            JsonArray rows = TursoClient.parseRows(json);
            if (!rows.isEmpty()) {
                server.execute(() -> onError.accept("taken"));
                return;
            }
            String hash = PasswordHasher.hash(password);
            long id = TursoClient.insert(
                    "INSERT INTO users (username, password_hash, student_id, section, full_name, created_at) VALUES (?,?,?,?,?,?)",
                    username, hash, studentId, section, fullName, Instant.now().toString());
            if (id < 0) {
                server.execute(() -> onError.accept("db_error"));
                return;
            }
            AuthAccount account = new AuthAccount(id, username, studentId, section, fullName, false);
            server.execute(() -> {
                active.put(uuid, account);
                onSuccess.accept(false);
            });
        }, AUTH_EXECUTOR);
    }

    /**
     * Logs the station into an existing account. Rate-limited the same way {@code /bfp login}
     * rate-limits PIN attempts (5 tries, 5-minute lockout) to slow down password guessing.
     */
    public static void loginAsync(ServerPlayer player, String username, String password,
                                   Consumer<AuthAccount> onSuccess, Consumer<String> onError) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        UUID uuid = player.getUUID();
        if (!TursoClient.isReady()) {
            server.execute(() -> onError.accept("offline"));
            return;
        }
        long[] rec = loginFailures.computeIfAbsent(uuid, k -> new long[]{0, 0});
        long now = System.currentTimeMillis();
        if (rec[0] >= MAX_LOGIN_ATTEMPTS) {
            long remaining = (rec[1] + LOCKOUT_MS - now) / 1000;
            if (remaining > 0) {
                server.execute(() -> onError.accept("locked:" + remaining));
                return;
            }
            rec[0] = 0;
        }
        CompletableFuture.runAsync(() -> {
            String json = TursoClient.query(
                    "SELECT id, username, password_hash, student_id, section, full_name, tutorial_completed " +
                    "FROM users WHERE username=?", username);
            JsonArray rows = TursoClient.parseRows(json);
            if (rows.isEmpty()) {
                recordFailure(rec, now);
                server.execute(() -> onError.accept("not_found"));
                return;
            }
            JsonObject row = rows.get(0).getAsJsonObject();
            if (!PasswordHasher.verify(password, str(row, "password_hash"))) {
                recordFailure(rec, now);
                server.execute(() -> onError.accept("bad_password"));
                return;
            }
            loginFailures.remove(uuid);
            long id = row.get("id").getAsLong();
            boolean tutorialDone = row.has("tutorial_completed") && !row.get("tutorial_completed").isJsonNull()
                    && row.get("tutorial_completed").getAsInt() == 1;
            AuthAccount account = new AuthAccount(id, username, str(row, "student_id"),
                    str(row, "section"), str(row, "full_name"), tutorialDone);
            TursoClient.executeAsync("UPDATE users SET last_login=? WHERE id=?", Instant.now().toString(), id);
            server.execute(() -> {
                active.put(uuid, account);
                onSuccess.accept(account);
            });
        }, AUTH_EXECUTOR);
    }

    private static void recordFailure(long[] rec, long now) {
        if (rec[0] == 0) rec[1] = now; // start lockout window
        rec[0]++;
    }

    private static String str(JsonObject obj, String key) {
        var el = obj.get(key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }
}
