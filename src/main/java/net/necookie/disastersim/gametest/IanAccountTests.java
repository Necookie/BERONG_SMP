package net.necookie.disastersim.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTestHelper;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static net.necookie.disastersim.gametest.GT.*;
import static net.necookie.disastersim.gametest.IanGT.*;

/**
 * Ian's AT-01..AT-04 (registration and login against the TEST database) and the in-game entry half of
 * ER-03 (/bfp confidence), 20 variants each. Every variant uses its own freshly generated account name.
 */
final class IanAccountTests {

    private IanAccountTests() {}

    private static final String[] SECTIONS = {"BSCS3A", "BSCS3B", "BSCS4A", "BSIT2A", "BSCS1C"};
    private static final String[] NAMES = {"Juan Dela Cruz", "Maria Clara", "Jose Rizal", "Ana Reyes Santos", "Pedro Penduko", "Liza Soberano", "Ramon Magsaysay", "Gabriela Silang", "Andres Bonifacio", "Emilio Aguinaldo"};
    private static final String[] PASSWORDS = {"Pass#2026a", "Zxcvbnm1!", "Tagalog_Pw9", "N0rth-Star", "Lspu@SantaCruz1", "Berong_SMP#7", "aaaaaaaa1A", "12345678aB", "Sym!bol$99", "Mix3d_Case-Ok", "Longer_Password_Than_Usual_2026!", "Short8aA", "Unicode_Pw1", "P@ss.w0rd", "Capital_ONLY_9", "lower_only_9z", "dots.and-dashes_1", "Equal=Sign_3", "Percent%Pw_4", "Tilde~Pw_5"};

    private static void register(RecordingPlayer p, String user, String pw, String studentId, String section, String full) {
        cmd(p, "register " + user + " \"" + pw + "\" " + studentId + " " + section + " " + full, false);
    }
    /** query() that retries when the answer is empty: a dropped HTTP call to the test database shows up as an empty result. */
    private static JsonArray q(String sql, Object... args) {
        JsonArray rows = query(sql, args);
        for (int tries = 0; rows.isEmpty() && tries < 6; tries++) {
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            rows = query(sql, args);
        }
        return rows;
    }
    private static long count(String sql, Object... args) {
        return Long.parseLong(str(q(sql, args).get(0).getAsJsonObject(), "n"));
    }
    private static boolean anyReply(RecordingPlayer p) { return p.messages.stream().anyMatch(m -> !m.contains("joined")); }
    private static String firstReply(RecordingPlayer p) { return p.messages.stream().filter(m -> !m.contains("joined")).findFirst().map(s -> s.trim().replace('\n', ' ')).orElse("no message"); }

    static void registerAll() {
        // ───────── AT-01 valid registration ─────────
        each("AT-01", 400000, (h, i) -> {
            String id = cid("AT-01", i);
            RecordingPlayer p = join(h, "at01_" + i);
            String u = uniqueName("a1"), pw = PASSWORDS[i], sid = "2026-" + String.format("%05d", 100 + i), sec = SECTIONS[i % 5], full = NAMES[i % 10];
            register(p, u, pw, sid, sec, full);
            flow(h).when(1, giveUpAfter(40, every(10, () -> AuthManager.current(p.getUUID()) != null && SessionManager.findActiveSessionRowId(p.getUUID()) > 0)), safe(h, id, () ->
                verdict(h, id, () -> {
                    need(AuthManager.current(p.getUUID()) != null, "not logged in after /register; chat: " + p.messages);
                    JsonArray rows = q("SELECT username, student_id, section, full_name, tutorial_completed FROM student_accounts WHERE username = ?", u);
                    need(rows.size() == 1, "student_accounts rows for " + u + ": " + rows.size());
                    JsonObject r = rows.get(0).getAsJsonObject();
                    need(sid.equals(str(r, "student_id")) && sec.equals(str(r, "section")), "stored " + str(r, "student_id") + "/" + str(r, "section"));
                    need(full.equals(str(r, "full_name")), "stored full name \"" + str(r, "full_name") + "\"");
                    long row = SessionManager.findActiveSessionRowId(p.getUUID());
                    need(row > 0, "no usable session identity (sessions row) for the new account");
                    return "Registered " + u + " (" + pw.length() + "-character password, student " + sid + ", " + sec + "): account row stored, player logged in, active sessions row " + row + ".";
                })
            )).start();
        });

        // ───────── AT-02 duplicate registration is rejected ─────────
        each("AT-02", 400000, (h, i) -> {
            String id = cid("AT-02", i);
            RecordingPlayer p1 = join(h, "at02a_" + i);
            String u = uniqueName("a2");
            String second = i >= 16 ? (i % 2 == 0 ? u.toUpperCase() : Character.toUpperCase(u.charAt(0)) + u.substring(1)) : u;   // 16..19: same name in different letter case
            String pw2 = i < 8 ? PW : PASSWORDS[i];                                                                              // same or different password on the duplicate attempt
            String sid2 = i % 3 == 0 ? "2026-90001" : "2026-" + String.format("%05d", 500 + i);
            register(p1, u, PW, "2026-" + String.format("%05d", 200 + i), SECTIONS[i % 5], NAMES[i % 10]);
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            flow(h).when(1, giveUpAfter(40, () -> AuthManager.current(p1.getUUID()) != null), safe(h, id, () -> {
                p2.set(RecordingPlayer.join(h, "at02b_" + i));
                register(p2.get(), second, pw2, sid2, SECTIONS[(i + 1) % 5], NAMES[(i + 3) % 10]);
            })).when(1, giveUpAfter(40, () -> p2.get() != null && anyReply(p2.get())), safe(h, id, () ->
                verdict(h, id, () -> {
                    long exact = count("SELECT COUNT(*) AS n FROM student_accounts WHERE username = ?", u);
                    long anyCase = count("SELECT COUNT(*) AS n FROM student_accounts WHERE lower(username) = lower(?)", u);
                    need(anyCase == 1, anyCase + " accounts now exist for " + u + " (exact-name rows " + exact + "); the duplicate \"" + second + "\" was accepted");
                    need(AuthManager.current(p2.get().getUUID()) == null, "the duplicate registration logged the second player in");
                    String reply = firstReply(p2.get());
                    need(reply.toLowerCase().contains("taken") || reply.toLowerCase().contains("exists") || reply.toLowerCase().contains("already"), "no rejection message: " + reply);
                    return "Second registration of " + (second.equals(u) ? "the same name" : "\"" + second + "\" (same name, different letter case)") + " rejected (\"" + reply + "\"); still one account, no login.";
                })
            )).start();
        });

        // ───────── AT-03 valid login ─────────
        each("AT-03", 400000, (h, i) -> {
            String id = cid("AT-03", i);
            RecordingPlayer p1 = join(h, "at03a_" + i);
            String u = uniqueName("a3"), pw = PASSWORDS[i];
            register(p1, u, pw, "2026-" + String.format("%05d", 300 + i), SECTIONS[i % 5], NAMES[i % 10]);
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            flow(h).when(1, giveUpAfter(40, () -> AuthManager.current(p1.getUUID()) != null), safe(h, id, () -> {
                cmd(p1, "logout", false);
                p2.set(RecordingPlayer.join(h, "at03b_" + i));
                cmd(p2.get(), "login " + u + " " + pw, false);   // /login takes the password as one bare word
            })).when(1, giveUpAfter(40, every(10, () -> AuthManager.current(p2.get().getUUID()) != null
                    && SessionManager.getActiveSession(p2.get().getUUID()) != null && lastLogin(u) != null)), safe(h, id, () ->
                verdict(h, id, () -> {
                    need(AuthManager.current(p2.get().getUUID()) != null, "not authenticated; chat: " + p2.get().messages);
                    String last = lastLogin(u);
                    need(last != null, "last_login not updated");
                    need(SessionManager.getActiveSession(p2.get().getUUID()) != null, "authenticated but no active student session");
                    need(count("SELECT COUNT(*) AS n FROM student_accounts WHERE username = ?", u) == 1, "account row count changed");
                    return "Login with a " + pw.length() + "-character password at a second station: authenticated, student session active, last_login " + last + ".";
                })
            )).start();
        });

        // ───────── AT-04 invalid login is refused and creates no session ─────────
        each("AT-04", 400000, (h, i) -> {
            String id = cid("AT-04", i);
            RecordingPlayer p1 = join(h, "at04a_" + i);
            String u = uniqueName("a4");
            String[] bad = {u + " WrongPass#1", u + " pass#2026", u + " " + PW + "x", u + " x" + PW, u + " " + PW.toLowerCase(), u + " " + PW.toUpperCase(), u + " " + PW.substring(0, PW.length() - 1),
                u + " 12345678", u + " Aa1!" + "z".repeat(190), u + " x'OR'1'='1", u + " ';DROP-TABLE-student_accounts;--", u + " Ünïcöde#1",
                "nobody_" + u + " " + PW, "NOBODY " + PW, u.toUpperCase() + " " + PW, "x'OR'1'='1 " + PW, "admin " + PW, "root " + PW, "test_owner OwnerPass#2026", "a " + PW};
            register(p1, u, PW, "2026-" + String.format("%05d", 400 + i), SECTIONS[i % 5], NAMES[i % 10]);
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            long[] before = {-1};
            flow(h).when(1, giveUpAfter(40, every(10, () -> AuthManager.current(p1.getUUID()) != null && SessionManager.findActiveSessionRowId(p1.getUUID()) > 0)), safe(h, id, () -> {
                cmd(p1, "logout", false);
                before[0] = count("SELECT COUNT(*) AS n FROM sessions WHERE username = ?", u);
                p2.set(RecordingPlayer.join(h, "at04b_" + i));
                cmd(p2.get(), "login " + bad[i], false);
            })).when(1, giveUpAfter(25, () -> p2.get() != null && anyReply(p2.get())), safe(h, id, () ->
                verdict(h, id, () -> {
                    need(AuthManager.current(p2.get().getUUID()) == null, "the bad login authenticated the player (\"login " + bad[i].substring(0, Math.min(60, bad[i].length())) + "\")");
                    need(SessionManager.getActiveSession(p2.get().getUUID()) == null, "the bad login created a student session");
                    need(count("SELECT COUNT(*) AS n FROM sessions WHERE username = ?", u) == before[0], "the bad login created a sessions row for " + u);
                    String reply = firstReply(p2.get());
                    return "Bad login #" + (i + 1) + " refused (\"" + reply + "\"): not authenticated, no session created, sessions rows for the account unchanged (" + before[0] + ").";
                })
            )).start();
        });

        // ───────── ER-03 (entry half) instructor confidence rating is stored as rating / 5, bad ratings refused ─────────
        each("ER-03", 400000, (h, i) -> {
            String id = cid("ER-03", i);
            RecordingPlayer p = join(h, "er03_" + i);
            double[] ratings = {1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 1.25, 2.75, 3.1, 4.9, 1.1, 0, 0.5, 5.5, 7, -1, 100};
            double v = ratings[i];
            boolean valid = v >= 1.0 && v <= 5.0;
            cmd(p, "bfp checkin TEST-BFP Student" + i, true);
            long[] row = {-1};
            flow(h).when(1, giveUpAfter(40, every(10, () -> (row[0] = SessionManager.findActiveSessionRowId(p.getUUID())) > 0)), safe(h, id, () -> {
                cmd(p, "bfp confidence " + v, true);
            })).when(1, giveUpAfter(valid ? 25 : 6, every(20, () -> valid && stored(row[0]) != null)), safe(h, id, () ->
                verdict(h, id, () -> {
                    String conf = stored(row[0]);
                    if (valid) {
                        need(conf != null, "no confidence stored for " + v);
                        need(Math.abs(Double.parseDouble(conf) - v / 5.0) < 1e-9, "rating " + v + " stored as " + conf + ", expected " + (v / 5.0));
                        return "/bfp confidence " + v + " stored " + conf + " (= " + v + " / 5).";
                    }
                    need(conf == null, "out-of-range rating " + v + " was stored as " + conf);
                    return "/bfp confidence " + v + " refused by the command parser (valid range 1.0 to 5.0); nothing stored.";
                })
            )).start();
        });
    }

    private static String lastLogin(String u) {
        JsonArray r = q("SELECT last_login FROM student_accounts WHERE username = ?", u);
        return r.isEmpty() ? null : str(r.get(0).getAsJsonObject(), "last_login");
    }
    private static String stored(long row) {
        JsonArray r = q("SELECT confidence FROM sessions WHERE id = ?", row);
        return r.isEmpty() ? null : str(r.get(0).getAsJsonObject(), "confidence");
    }
}
