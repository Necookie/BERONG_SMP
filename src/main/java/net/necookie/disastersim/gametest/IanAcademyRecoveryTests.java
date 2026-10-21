package net.necookie.disastersim.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyTelemetry;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room4.MorfeRoomManager;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.session.AuthManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.necookie.disastersim.gametest.GT.*;
import static net.necookie.disastersim.gametest.IanGT.*;

/**
 * Ian's AC-08 (repeat after a failed Academy), AC-09 (Academy logging), RC-01 (cloud interruption) and
 * RC-02 (reconnect and reconcile), 20 variants each. AC-08/AC-09 drive Capt. Morfe's real evaluation
 * routine; RC-01 must run with the offline config, RC-02 with the online config after RC-01.
 */
final class IanAcademyRecoveryTests {

    private IanAcademyRecoveryTests() {}

    private static final double RETRY_X = -145.5, RETRY_Z = 31.5;

    // Expected Academy score from the documented rubric (25 points per room), independent of AcademyScoring.
    private static int expected(int m, int c, int w, boolean drop, boolean quake) {
        int move = Math.max(0, 25 - 8 * m), fire = c + w == 0 ? 0 : (int) Math.round(25.0 * c / (c + w));
        return Math.min(100, move + fire + (drop ? 25 : 0) + (quake ? 25 : 0));
    }
    /** 20 Academy outcomes spread across the score range: {mistakes, correct, wrong, drop, quake, score}. */
    private static List<int[]> profiles(boolean pass) {
        List<int[]> all = new ArrayList<>();
        for (int m = 0; m <= 6; m++) for (int c = 0; c <= 12; c++) for (int w = 0; w <= 8; w += 2) for (int d = 0; d <= 1; d++) for (int q = 0; q <= 1; q++) {
            int s = expected(m, c, w, d == 1, q == 1);
            boolean nonTrivial = m + c + w > 0 || d + q > 0;
            if (nonTrivial && (pass ? s >= 70 : s < 70)) all.add(new int[]{m, c, w, d, q, s});
        }
        List<int[]> pick = new ArrayList<>();
        for (int k = 0; k < 20; k++) pick.add(all.get((int) ((long) k * (all.size() - 1) / 19)));
        return pick;
    }

    private static void setProgress(AcademySavedData data, UUID uuid, int[] pr) {
        data.mutate(uuid, a -> {
            a.setCruzPhase(CruzPhase.DONE); a.setReyesPhase(ReyesPhase.DONE); a.setSantosPhase(SantosPhase.DONE);
            for (int k = 0; k < pr[0]; k++) a.addMovementMistake();
            for (int k = 0; k < pr[1]; k++) a.addFireCorrectUse();
            for (int k = 0; k < pr[2]; k++) a.addFireWrongUse();
            a.setDropAndRollPerformed(pr[3] == 1);
            a.setQuakeCompliant(pr[4] == 1);
        });
    }
    private static void runEvaluation(ServerLevel level, ServerPlayer p, AcademySavedData data) throws ReflectiveOperationException {
        Method m = MorfeRoomManager.class.getDeclaredMethod("runEvaluation", ServerLevel.class, ServerPlayer.class, AcademySavedData.class);
        m.setAccessible(true);
        m.invoke(null, level, p, data);
    }
    private static Path telemetryCsv() { return Path.of("telemetry", "gameplay_logs_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv"); }
    private static List<String> csvRows(String needle) {
        TelemetryCsvWriter.flush();   // the event writer is buffered; make sure the rows are on disk before reading them
        try (var lines = Files.lines(telemetryCsv())) { return lines.filter(l -> l.contains(needle)).toList(); } catch (IOException e) { return List.of(); }
    }
    private static List<String> logLines() {
        try { return Files.readAllLines(Path.of("logs", "latest.log"), StandardCharsets.UTF_8); } catch (IOException e) { return List.of(); }
    }
    private static Path offlineFile() {
        String dir = System.getenv().getOrDefault("IAN_RC_DIR", "build/ian");
        return Path.of(dir, "rc_offline.jsonl");
    }

    static void registerAll() {
        // ───────── AC-08 repeating the Academy after failing ─────────
        List<int[]> fails = profiles(false);
        each("AC-08", 8000, (h, i) -> {
            String id = cid("AC-08", i);
            int[] pr = fails.get(i);
            RecordingPlayer p = join(h, "ac08_" + i);
            ServerLevel level = h.getLevel();
            AcademySavedData data = AcademySavedData.get(level);
            data.reset(p.getUUID());
            setProgress(data, p.getUUID(), pr);
            AcademyTelemetry.startAttempt(p);
            try { runEvaluation(level, p, data); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            h.succeedWhen(() -> {
                h.assertTrue(data.get(p.getUUID()).santosPhase() == SantosPhase.NOT_STARTED, "progress not reset yet");
                verdict0(h, id, () -> {
                    AcademyProgress a = data.get(p.getUUID());
                    need(a.cruzPhase() == CruzPhase.NOT_STARTED && a.reyesPhase() == ReyesPhase.NOT_STARTED && a.morfePhase() == MorfePhase.NOT_STARTED, "phases not reset: " + a.cruzPhase() + "/" + a.reyesPhase() + "/" + a.morfePhase());
                    need(a.movementMistakes() == 0 && a.fireCorrectUses() == 0 && a.fireWrongUses() == 0 && !a.quakeCompliant() && !a.dropAndRollPerformed(), "scoring counters not reset");
                    need(Math.abs(p.getX() - RETRY_X) < 2.0 && Math.abs(p.getZ() - RETRY_Z) < 2.0, "player at (" + p.getX() + ", " + p.getZ() + "), expected Room 1 near (" + RETRY_X + ", " + RETRY_Z + ")");
                    need(!AuthManager.isTutorialCompleted(p.getUUID()), "a failed Academy marked the account certified");
                    need(p.heard("Things to practice"), "no list of things to practice: " + p.messages);
                    boolean prompt = p.drainPayloads().stream().anyMatch(x -> x.toString().contains("Welcome back for round two"));   // the retry prompt is an Academy caption packet, not chat
                    need(prompt, "no 'Welcome back for round two' caption was sent to the player");
                    need(csvRows("academy_failed").stream().anyMatch(r -> r.contains(p.getUUID().toString()) && r.contains("score=" + pr[5])), "no academy_failed telemetry row with score=" + pr[5]);
                    need(SimulationManager.getSession(p.getUUID()) == null, "a failed Academy started the main scenario");
                    return "Score " + pr[5] + " (mistakes " + pr[0] + ", fire " + pr[1] + "/" + pr[2] + ", drop " + pr[3] + ", quake " + pr[4] + ") failed: progress fully reset, player sent back to Room 1, not certified, main scenario not started, academy_failed row logged.";
                });
            });
        });

        // ───────── AC-09 Academy events and score are stored under the attempt's session ─────────
        List<int[]> passes = profiles(true);
        each("AC-09", 6000, (h, i) -> {
            String id = cid("AC-09", i);
            boolean pass = i % 2 == 0;
            int[] pr = pass ? passes.get(i) : fails.get(i);
            RecordingPlayer p = join(h, "ac09_" + i);
            ServerLevel level = h.getLevel();
            AcademySavedData data = AcademySavedData.get(level);
            data.reset(p.getUUID());
            setProgress(data, p.getUUID(), pr);
            AcademyTelemetry.startAttempt(p);
            try { runEvaluation(level, p, data); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            h.runAfterDelay(5, safe(h, id, () -> verdict(h, id, () -> {
                String uuid = p.getUUID().toString();
                List<String> rows = csvRows(uuid).stream().filter(r -> r.contains(",ACADEMY,")).toList();
                String outcome = pass ? "academy_certified" : "academy_failed";
                need(rows.stream().anyMatch(r -> r.contains(",academy_start,")), "no academy_start row");
                String end = rows.stream().filter(r -> r.contains("," + outcome + ",")).findFirst().orElse(null);
                need(end != null, "no " + outcome + " row");
                need(end.contains("score=" + pr[5]), "outcome row does not carry score=" + pr[5] + ": " + end);
                String sid = end.split(",", -1)[1];
                need(rows.stream().filter(r -> r.contains(",academy_start,")).anyMatch(r -> r.split(",", -1)[1].equals(sid)), "the start row and the outcome row are under different sessions");
                double t0 = Double.parseDouble(rows.stream().filter(r -> r.contains(",academy_start,")).findFirst().get().split(",", -1)[3]), t1 = Double.parseDouble(end.split(",", -1)[3]);
                need(t1 >= t0, "outcome timestamp " + t1 + " before start " + t0);
                return "Academy attempt " + sid.substring(0, 8) + "...: academy_start and " + outcome + " (score=" + pr[5] + ") stored under the same session for this player, in time order.";
            })));
        });

        // ───────── RC-01 cloud interruption: the run goes on and the local record is complete (offline config) ─────────
        each("RC-01", 1200, (h, i) -> {
            String id = cid("RC-01", i);
            SimulationState st = new SimulationState[]{SimulationState.FIRE, SimulationState.EARTHQUAKE, SimulationState.CCS_FIRE, SimulationState.CCS_EARTHQUAKE, SimulationState.NEW_SIM_BUILDING2_FIRE}[i % 5];
            RecordingPlayer p = join(h, "rc01_" + i);
            long warnsBefore = logLines().stream().filter(l -> l.contains("[TursoClient]") && (l.contains("WARN") || l.contains("ERROR"))).count();
            SimulationSession s = start(p, st);
            BlockPosHolder.move(h, p, 30 + i);
            h.runAfterDelay(WARMUP + 40 + i * 3, safe(h, id, () -> {
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                verdict(h, id, () -> {
                    String sid = s.getSessionId();
                    need(SimulationManager.getSession(p.getUUID()) == null, "the run did not finish with the database offline");
                    List<String> local = csvRows(sid);
                    long expectedRows = s.buildMoveCsv().lines().skip(1).count();
                    need(local.size() == expectedRows, "local CSV has " + local.size() + " rows, the session recorded " + expectedRows);
                    need(local.stream().anyMatch(r -> r.contains(",session_start,")) && local.stream().anyMatch(r -> r.contains(",session_end,")), "session_start/session_end missing locally");
                    long warns = logLines().stream().filter(l -> l.contains("[TursoClient]") && (l.contains("WARN") || l.contains("ERROR"))).count() - warnsBefore;
                    need(warns < 60, warns + " TursoClient warnings for one run: retry flood");
                    JsonObject rec = new JsonObject();
                    rec.addProperty("variant", i); rec.addProperty("player_uuid", p.getUUID().toString()); rec.addProperty("session_id", sid); rec.addProperty("rows", local.size()); rec.addProperty("scenario", st.name());
                    try { Files.createDirectories(offlineFile().getParent()); Files.writeString(offlineFile(), rec + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
                    catch (IOException e) { throw new IllegalStateException(e); }
                    return st + " with the database unreachable: the run started and ended normally, all " + local.size() + " telemetry rows (session_start to session_end) are in the local CSV, " + warns + " TursoClient warnings (no retry flood).";
                });
            }));
        });

        // ───────── RC-02 after connectivity returns, the offline run is reconciled with the cloud exactly once (online config) ─────────
        each("RC-02", 600, (h, i) -> {
            String id = cid("RC-02", i);
            JsonObject rec = null;
            try {
                for (String line : Files.readAllLines(offlineFile(), StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    if (o.get("variant").getAsInt() == i) rec = o;
                }
            } catch (IOException ignored) { }
            if (rec == null) { record(id, "FAIL", "harness: no offline RC-01 record for variant " + (i + 1) + " (run slice RC-01 offline first)"); h.succeed(); return; }
            final JsonObject r = rec;
            h.runAfterDelay(5, safe(h, id, () -> verdict(h, id, () -> {
                String uuid = r.get("player_uuid").getAsString(), sid = r.get("session_id").getAsString();
                int rows = r.get("rows").getAsInt();
                List<String> local = csvRows(sid);
                need(local.size() == rows, "local evidence changed: " + local.size() + " rows now, " + rows + " when recorded");
                long inCloud = Long.parseLong(str(query("SELECT COUNT(*) AS n FROM sessions WHERE account_uuid = ?", uuid).get(0).getAsJsonObject(), "n"));
                need(inCloud <= 1, inCloud + " cloud rows for one offline run (duplicated)");
                need(inCloud == 1, "connectivity is back, but the " + r.get("scenario").getAsString() + " run played offline (" + rows + " rows, session " + sid.substring(0, 8) + "...) was never uploaded: 0 cloud rows. The local CSV is the only copy and there is no automatic re-upload.");
                return "Offline run reconciled: exactly one cloud row for the offline player; local evidence intact (" + rows + " rows).";
            })));
        });
    }

    /** Verdict for succeedWhen callbacks: records the result without calling reset-then-succeed twice. */
    private static void verdict0(net.minecraft.gametest.framework.GameTestHelper h, String id, java.util.function.Supplier<String> checks) {
        try { record(id, "PASS", checks.get()); } catch (Throwable t) { record(id, "FAIL", message(t)); }
        try { reset(h); } catch (Throwable ignored) { }
    }

    /** Keeps a player moving on the spot for a while, so the offline runs carry movement rows. */
    private static final class BlockPosHolder {
        static void move(net.minecraft.gametest.framework.GameTestHelper h, RecordingPlayer p, int ticks) {
            net.minecraft.core.BlockPos base = platform(h);
            h.onEachTick(() -> { if (h.getTick() > WARMUP && h.getTick() < WARMUP + ticks + 40) tp(p, base.getX() + 0.5 + (h.getTick() % 20) * 0.05, base.getY(), base.getZ() + 0.5); });
        }
    }
}
