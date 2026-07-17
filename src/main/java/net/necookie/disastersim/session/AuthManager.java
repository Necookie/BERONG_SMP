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
import java.util.concurrent.atomic.AtomicInteger;
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
    /** Rejects absurdly long submitted passwords before they ever reach PBKDF2 or Turso. */
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 5L * 60 * 1000; // 5 minutes

    private static final Map<UUID, AuthAccount> active = new ConcurrentHashMap<>();
    /**
     * Per-station rate-limit state. The attempt counter is incremented synchronously on the
     * calling (server) thread at dispatch time — before the async lookup even starts — rather than
     * only on a confirmed failure once the async result comes back. The old failure-only counting
     * had a real TOCTOU window: several rapid /login submissions could all pass the
     * "already locked out?" check before any one of their async results returned and recorded a
     * failure, letting a burst exceed the intended 5-attempt budget.
     */
    private static final class LoginAttempts {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lockoutStartMs = 0L;
    }
    private static final Map<UUID, LoginAttempts> loginFailures = new ConcurrentHashMap<>();

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
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.length() <= MAX_PASSWORD_LENGTH;
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
        TursoClient.executeAsync("UPDATE student_accounts SET tutorial_completed=1 WHERE id=?", account.userId());
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
            String json = TursoClient.query("SELECT id FROM student_accounts WHERE username=?", username);
            JsonArray rows = TursoClient.parseRows(json);
            if (!rows.isEmpty()) {
                server.execute(() -> onError.accept("taken"));
                return;
            }
            String hash = PasswordHasher.hash(password);
            long id = TursoClient.insert(
                    "INSERT INTO student_accounts (username, password_hash, student_id, section, full_name, created_at) VALUES (?,?,?,?,?,?)",
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
     * rate-limits PIN attempts (5 tries, 5-minute lockout) to slow down password guessing — every
     * dispatched attempt counts toward the budget (incremented synchronously here, before the
     * async lookup even starts), not just confirmed failures, so a burst of concurrent submissions
     * can't slip past the lockout check before any one of them resolves.
     *
     * <p>{@code onError} receives {@code "invalid_credentials"} for both an unknown username and a
     * wrong password — kept indistinguishable so a station can't be used to enumerate which
     * usernames are registered.
     */
    public static void loginAsync(ServerPlayer player, String username, String password,
                                   Consumer<AuthAccount> onSuccess, Consumer<String> onError) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        UUID uuid = player.getUUID();
        if (!TursoClient.isReady()) {
            server.execute(() -> onError.accept("offline"));
            return;
        }
        if (password != null && password.length() > MAX_PASSWORD_LENGTH) {
            // Reject before ever touching PBKDF2/Turso — no point spending 210k iterations
            // hashing input that can't possibly match a stored (bounded-length) password anyway.
            server.execute(() -> onError.accept("invalid_credentials"));
            return;
        }

        LoginAttempts rec = loginFailures.computeIfAbsent(uuid, k -> new LoginAttempts());
        long now = System.currentTimeMillis();
        synchronized (rec) {
            if (rec.count.get() >= MAX_LOGIN_ATTEMPTS) {
                long remaining = (rec.lockoutStartMs + LOCKOUT_MS - now) / 1000;
                if (remaining > 0) {
                    server.execute(() -> onError.accept("locked:" + remaining));
                    return;
                }
                rec.count.set(0);
            }
            if (rec.count.incrementAndGet() == 1) {
                rec.lockoutStartMs = now;
            }
        }

        CompletableFuture.runAsync(() -> {
            String json = TursoClient.query(
                    "SELECT id, username, password_hash, student_id, section, full_name, tutorial_completed " +
                    "FROM student_accounts WHERE username=?", username);
            JsonArray rows = TursoClient.parseRows(json);
            if (rows.isEmpty()) {
                server.execute(() -> onError.accept("invalid_credentials"));
                return;
            }
            JsonObject row = rows.get(0).getAsJsonObject();
            if (!PasswordHasher.verify(password, str(row, "password_hash"))) {
                server.execute(() -> onError.accept("invalid_credentials"));
                return;
            }
            loginFailures.remove(uuid);
            long id = row.get("id").getAsLong();
            boolean tutorialDone = row.has("tutorial_completed") && !row.get("tutorial_completed").isJsonNull()
                    && row.get("tutorial_completed").getAsInt() == 1;
            AuthAccount account = new AuthAccount(id, username, str(row, "student_id"),
                    str(row, "section"), str(row, "full_name"), tutorialDone);
            TursoClient.executeAsync("UPDATE student_accounts SET last_login=? WHERE id=?", Instant.now().toString(), id);
            server.execute(() -> {
                active.put(uuid, account);
                onSuccess.accept(account);
            });
        }, AUTH_EXECUTOR);
    }

    private static String str(JsonObject obj, String key) {
        var el = obj.get(key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }
}
