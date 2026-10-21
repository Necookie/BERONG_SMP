package net.necookie.disastersim.ian;

import com.google.gson.JsonObject;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room4.AcademyScoring;
import net.necookie.disastersim.common.simulation.NewSimScoring;
import net.necookie.disastersim.common.simulation.SimulationFeedback;
import net.necookie.disastersim.common.simulation.SimulationFeedback.FireMetrics;
import net.necookie.disastersim.common.simulation.SimulationFeedback.FireWarning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ian's testing-instrument IDs that are decided by pure scoring rules (AC-01..07, RB-01..03, RB-06,
 * RB-07, FS-12..15), 20 variants per ID. Every variant writes one result line to
 * $IAN_RESULTS_DIR/ian_junit.jsonl in the same shape as the web suites; the JUnit methods themselves
 * only fail on a harness error, so a failing variant never hides the other 19.
 * Expected values come from the documented formulas (thesis section 10), written out here
 * independently of the classes under test.
 */
class IanScoringTest {

    // ───────────── result recording ─────────────
    private static final Path OUT = Path.of(System.getenv().getOrDefault("IAN_RESULTS_DIR", "build/ian"), "ian_junit.jsonl");
    private static final Path EVIDENCE = OUT.resolveSibling("evidence_ian_junit.jsonl");

    private static void run(String id, int variant, Supplier<String> body) {
        long t0 = System.currentTimeMillis();
        String status = "PASS", actual;
        try { actual = body.get(); }
        catch (Throwable t) { status = "FAIL"; actual = t.getMessage() == null ? t.toString() : t.getMessage(); }
        JsonObject o = new JsonObject();
        o.addProperty("case_id", id + "-R" + String.format("%02d", variant + 1));
        o.addProperty("status", status);
        o.addProperty("actual", actual);
        o.addProperty("duration_ms", System.currentTimeMillis() - t0);
        o.addProperty("executed_at", Instant.now().toString());
        o.addProperty("attempt", Integer.parseInt(System.getenv().getOrDefault("IAN_ATTEMPT", "1")));
        o.addProperty("suite", "ian_junit");
        try {
            Files.createDirectories(OUT.getParent());
            Files.writeString(OUT, o + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }

    private static void check(boolean cond, String msg) { if (!cond) throw new AssertionError(msg); }

    // ───────────── Academy (AC-01..07) ─────────────
    private static AcademyProgress progress(int mistakes, int correct, int wrong, boolean quake, boolean drop) {
        return new AcademyProgress(CruzPhase.NOT_STARTED, ReyesPhase.NOT_STARTED, SantosPhase.NOT_STARTED,
                MorfePhase.NOT_STARTED, mistakes, correct, wrong, quake, drop);
    }
    private static int moveExpected(int m) { return Math.max(0, 25 - 8 * m); }
    private static int fireExpected(int c, int w) { return c + w == 0 ? 0 : (int) Math.round(25.0 * c / (c + w)); }
    private static boolean weak(AcademyScoring.Result r, String fragment) { return r.weakAreas().stream().anyMatch(s -> s.contains(fragment)); }

    @Test @DisplayName("AC-01")
    void ac01Movement() {
        for (int i = 0; i < 20; i++) {
            int m = i % 10, c = i < 10 ? 1 : 4, w = i < 10 ? 0 : 0;
            run("AC-01", i, () -> {
                AcademyScoring.Result r = AcademyScoring.evaluate(progress(m, c, w, true, true));
                int expect = moveExpected(m) + 25 + 25 + 25;
                check(r.score() == expect, m + " movement mistakes: score " + r.score() + ", expected " + expect);
                check(weak(r, "froze past the STOP window") == (m > 0), "movement weak-area flag wrong for " + m + " mistakes");
                return m + " movement mistakes -> movement component " + moveExpected(m) + "/25, total " + r.score() + (m > 0 ? ", weak area flagged" : ", no weak area") + ".";
            });
        }
    }

    private static final int[][] FIRE = {{1, 0}, {5, 0}, {10, 0}, {0, 1}, {0, 5}, {1, 1}, {3, 1}, {4, 1}, {1, 4}, {2, 3}, {3, 2}, {7, 3}, {9, 1}, {1, 9}, {5, 5}, {12, 8}, {19, 6}, {11, 14}, {30, 1}, {0, 0}};
    @Test @DisplayName("AC-02")
    void ac02Extinguisher() {
        for (int i = 0; i < 20; i++) {
            int c = FIRE[i][0], w = FIRE[i][1], m = i % 3;
            run("AC-02", i, () -> {
                AcademyScoring.Result r = AcademyScoring.evaluate(progress(m, c, w, true, true));
                int expect = moveExpected(m) + fireExpected(c, w) + 50;
                check(r.score() == expect, c + " correct / " + w + " wrong: score " + r.score() + ", expected " + expect);
                check(weak(r, "wrong extinguisher") == (w > 0), "wrong-extinguisher flag wrong for " + w + " wrong uses");
                return c + " correct / " + w + " wrong extinguisher uses -> fire component " + fireExpected(c, w) + "/25, total " + r.score() + ".";
            });
        }
    }

    @Test @DisplayName("AC-03")
    void ac03DropAndRoll() {
        for (int i = 0; i < 20; i++) {
            boolean drop = i % 2 == 0; int m = i % 5, c = 1 + i % 4, w = i % 3;
            run("AC-03", i, () -> {
                AcademyScoring.Result r = AcademyScoring.evaluate(progress(m, c, w, true, drop));
                int expect = moveExpected(m) + fireExpected(c, w) + (drop ? 25 : 0) + 25;
                check(r.score() == expect, "drop-and-roll " + drop + ": score " + r.score() + ", expected " + expect);
                check(weak(r, "drop and roll") == !drop, "drop-and-roll flag wrong when performed=" + drop);
                return "Drop-and-roll " + (drop ? "performed" : "not performed") + " -> component " + (drop ? 25 : 0) + "/25, total " + r.score() + ".";
            });
        }
    }

    @Test @DisplayName("AC-04")
    void ac04QuakeProtectiveAction() {
        for (int i = 0; i < 20; i++) {
            boolean quake = i % 2 == 1; int m = i % 4, c = 1 + i % 5, w = i % 2;
            run("AC-04", i, () -> {
                AcademyScoring.Result r = AcademyScoring.evaluate(progress(m, c, w, quake, true));
                int expect = moveExpected(m) + fireExpected(c, w) + 25 + (quake ? 25 : 0);
                check(r.score() == expect, "quake compliant " + quake + ": score " + r.score() + ", expected " + expect);
                check(weak(r, "broke cover") == !quake, "quake flag wrong when compliant=" + quake);
                return "Duck-Cover-Hold " + (quake ? "held" : "broken") + " -> component " + (quake ? 25 : 0) + "/25, total " + r.score() + ".";
            });
        }
    }

    /** Every combination of Academy inputs, grouped by the score it produces (computed by the documented rubric). */
    private static List<int[]> academyCombos(int lo, int hi) {
        List<int[]> out = new ArrayList<>();
        for (int m = 0; m <= 6; m++) for (int c = 0; c <= 20; c += 1) for (int w = 0; w <= 12; w += 2)
            for (int d = 0; d <= 1; d++) for (int q = 0; q <= 1; q++) {
                int s = Math.min(100, moveExpected(m) + fireExpected(c, w) + 25 * d + 25 * q);
                if (s >= lo && s <= hi) out.add(new int[]{m, c, w, d, q, s});
            }
        return out;
    }
    private static List<int[]> spread(List<int[]> all, int n) {
        List<int[]> pick = new ArrayList<>();
        for (int k = 0; k < n; k++) pick.add(all.get((int) ((long) k * (all.size() - 1) / Math.max(1, n - 1))));
        return pick;
    }
    private static void academyThreshold(String id, int lo, int hi, boolean expectCertified) {
        List<int[]> picks = spread(academyCombos(lo, hi), 20);
        for (int i = 0; i < 20; i++) {
            int[] p = picks.get(i);
            run(id, i, () -> {
                AcademyScoring.Result r = AcademyScoring.evaluate(progress(p[0], p[1], p[2], p[4] == 1, p[3] == 1));
                check(r.score() == p[5], "rubric gave " + r.score() + ", documented formula " + p[5]);
                boolean cert = AcademyScoring.isCertified(r.score(), 70);
                check(cert == expectCertified, "score " + r.score() + " certified=" + cert + ", expected " + expectCertified);
                return "Score " + r.score() + " (mistakes " + p[0] + ", fire " + p[1] + "/" + p[2] + ", drop " + p[3] + ", quake " + p[4] + "): "
                        + (cert ? "certified, main scenario unlocks" : "not certified, main scenario stays locked") + " at threshold 70.";
            });
        }
    }
    @Test @DisplayName("AC-05") void ac05BelowPass() { academyThreshold("AC-05", 0, 69, false); }
    @Test @DisplayName("AC-06") void ac06AtPass() { academyThreshold("AC-06", 70, 70, true); }
    @Test @DisplayName("AC-07") void ac07AbovePass() { academyThreshold("AC-07", 71, 100, true); }

    // ───────────── New Sim Building 2.0 rule (RB-01..03, RB-07, FS-12..15) ─────────────
    private record Run(int armed, int prevented, int extinguished, boolean alarm, boolean assembly, String end) {
        int escalated() { return armed - prevented; }
        int raw() {   // documented formula, written independently of NewSimScoring
            int s = prevented * 12 + (prevented >= armed && armed > 0 ? 25 : 0);
            if (escalated() > 0) s += extinguished * 8 + (extinguished >= escalated() ? 10 : 0);
            if (alarm) s += 10;
            if (("assembly_reached".equals(end) || "timeout".equals(end)) && assembly) s += 15;
            return s;
        }
        int score() { return Math.min(100, raw()); }
        String tier() { return score() >= 75 ? "HIGH" : score() >= 40 ? "MODERATE" : "LOW"; }
        NewSimScoring.Result actual() { return NewSimScoring.evaluate(armed, prevented, escalated(), extinguished, alarm, assembly, end); }
        @Override public String toString() { return "armed " + armed + ", prevented " + prevented + ", extinguished " + extinguished + "/" + escalated() + ", alarm " + alarm + ", assembly " + assembly + ", end " + end; }
    }
    private static final String[] ENDS = {"all_hazards_prevented", "intervention_success", "assembly_reached", "timeout", "injured"};
    private static List<Run> allRuns(int maxArmed) {
        List<Run> out = new ArrayList<>();
        for (int a = 1; a <= maxArmed; a++) for (int p = 0; p <= a; p++) for (int e = 0; e <= a - p; e++)
            for (int al = 0; al <= 1; al++) for (int as = 0; as <= 1; as++) for (String end : ENDS) out.add(new Run(a, p, e, al == 1, as == 1, end));
        return out;
    }
    /** Picks n distinct configurations spread across the matches. The normal hazard count is 5; when fewer than n distinct
     *  configurations exist there (a raw total above 100 needs all five found), larger counts are allowed because the scorer takes any count. */
    private static List<Run> pickRuns(java.util.function.Predicate<Run> f, int n) {
        for (int maxArmed : new int[]{5, 8, 12}) {
            List<Run> all = allRuns(maxArmed).stream().filter(f).toList();
            if (all.size() < n) continue;
            List<Run> out = new ArrayList<>();
            for (int k = 0; k < n; k++) out.add(all.get((int) ((long) k * (all.size() - 1) / Math.max(1, n - 1))));
            return out;
        }
        throw new AssertionError("fewer than " + n + " distinct configurations match even with up to 12 hazards");
    }
    private static void scoreCase(String id, List<Run> runs, java.util.function.Function<NewSimScoring.Result, String> extra) {
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            run(id, i, () -> {
                NewSimScoring.Result a = r.actual();
                check(a.score() == r.score(), "(" + r + ") scorer gave " + a.score() + ", documented formula " + r.score());
                check(a.prepLevel().equals(r.tier()), "(" + r + ") tier " + a.prepLevel() + ", expected " + r.tier());
                return "(" + r + ") -> score " + a.score() + ", " + a.prepLevel() + extra.apply(a) + ".";
            });
        }
    }

    @Test @DisplayName("RB-01")
    void rb01CorrectPrevention() {   // hazards found before ignition earn 12 each (+25 when all were found)
        List<Run> runs = pickRuns(r -> r.prevented() >= 1 && !r.alarm() && !r.assembly() && r.extinguished() == 0, 20);
        for (int i = 0; i < 20; i++) {
            Run r = runs.get(i);
            run("RB-01", i, () -> {
                NewSimScoring.Result a = r.actual();
                int expect = r.prevented() * 12 + (r.prevented() == r.armed() ? 25 : 0);
                check(a.score() == expect, "(" + r + ") prevention score " + a.score() + ", expected " + expect);
                check(a.weakAreas().stream().anyMatch(s -> s.contains("spotting hazards")) == (r.prevented() < r.armed()), "weak-area flag wrong");
                return r.prevented() + "/" + r.armed() + " hazards prevented -> " + a.score() + " points (" + r.prevented() + " x 12" + (r.prevented() == r.armed() ? " + 25 all-found bonus" : "") + ").";
            });
        }
    }

    @Test @DisplayName("RB-02")
    void rb02IncorrectPrevention() {   // nothing found before ignition: no prevention credit, weak area named
        List<Run> runs = pickRuns(r -> r.prevented() == 0 && r.extinguished() == 0 && !r.assembly(), 20);
        for (int i = 0; i < 20; i++) {
            Run r = runs.get(i);
            run("RB-02", i, () -> {
                NewSimScoring.Result a = r.actual();
                check(a.score() == (r.alarm() ? 10 : 0), "(" + r + ") score " + a.score() + ", expected " + (r.alarm() ? 10 : 0) + " (alarm only)");
                check(a.weakAreas().stream().anyMatch(s -> s.contains("spotting hazards before they ignite (0/" + r.armed() + " found)")), "weak area text missing: " + a.weakAreas());
                return "0/" + r.armed() + " prevented: no prevention credit, score " + a.score() + ", weak area \"spotting hazards before they ignite (0/" + r.armed() + " found)\".";
            });
        }
    }

    @Test @DisplayName("RB-03")
    void rb03CorrectAlarmResponse() {   // ringing the alarm adds exactly 10 and clears its weak area
        List<Run> base = pickRuns(r -> r.alarm() && r.raw() <= 90, 20);
        for (int i = 0; i < 20; i++) {
            Run on = base.get(i), off = new Run(on.armed(), on.prevented(), on.extinguished(), false, on.assembly(), on.end());
            run("RB-03", i, () -> {
                NewSimScoring.Result a = on.actual(), b = off.actual();
                check(a.score() - b.score() == 10, "(" + on + ") alarm changed the score by " + (a.score() - b.score()));
                check(a.weakAreas().stream().noneMatch(s -> s.contains("ringing the fire alarm")) && b.weakAreas().stream().anyMatch(s -> s.contains("ringing the fire alarm")), "alarm weak-area flag wrong");
                return "Alarm rung: " + a.score() + " vs " + b.score() + " without (+10); \"ringing the fire alarm\" flagged only without it.";
            });
        }
    }

    @Test @DisplayName("RB-07")
    void rb07TierBoundaries() {   // tier changes exactly at 39/40 and 74/75
        List<Run> runs = new ArrayList<>();
        for (int t : new int[]{39, 40, 74, 75}) runs.addAll(pickRuns(r -> r.raw() == t, 5));
        scoreCase("RB-07", runs, a -> {
            return "";
        });
    }

    @Test @DisplayName("FS-12")
    void fs12ScoreCap() {   // raw totals above 100 are capped at exactly 100
        scoreCase("FS-12", pickRuns(r -> r.raw() > 100, 20), a -> " (capped at 100)");
    }
    @Test @DisplayName("FS-13")
    void fs13HighBoundary() { scoreCase("FS-13", pickRuns(r -> r.score() >= 75 && r.score() <= 80 || r.score() == 100, 20), a -> ""); }
    @Test @DisplayName("FS-14")
    void fs14ModerateBoundaries() {
        List<Run> runs = new ArrayList<>(pickRuns(r -> r.raw() == 40, 5));
        runs.addAll(pickRuns(r -> r.raw() == 74, 5)); runs.addAll(pickRuns(r -> r.raw() > 40 && r.raw() < 74, 10));
        scoreCase("FS-14", runs, a -> "");
    }
    @Test @DisplayName("FS-15")
    void fs15LowRange() {
        List<Run> runs = new ArrayList<>(pickRuns(r -> r.raw() == 0, 3));
        runs.addAll(pickRuns(r -> r.raw() == 39, 5)); runs.addAll(pickRuns(r -> r.raw() > 0 && r.raw() < 39, 12));
        scoreCase("FS-15", runs, a -> "");
    }

    // ───────────── learner feedback thresholds (RB-06) ─────────────
    private static final double[][] METRICS = {   // decisionDelay, sprayAccuracy, panicProxy, pathEfficiency
        {29.9, 0.41, 1.9, 0.51}, {30.0, 0.40, 2.0, 0.50}, {30.1, 0.39, 2.1, 0.49}, {45, 0.8, 0.5, 0.9}, {999, 0.0, 0.0, 0.0},
        {5, 0.39, 0.5, 0.9}, {5, 0.40, 0.5, 0.9}, {5, 1.0, 2.01, 0.9}, {5, 1.0, 2.0, 0.9}, {5, 1.0, 0.5, 0.49},
        {5, 1.0, 0.5, 0.50}, {5, 1.0, 0.5, 0.01}, {31, 0.1, 3.0, 0.2}, {12, 0.9, 0.2, 0.8}, {60, 1.0, 0.0, 1.0},
        {0, 0.0, 0.0, 0.0}, {30.0001, 0.4001, 1.9999, 0.5001}, {29.9999, 0.3999, 2.0001, 0.4999}, {10, 0.55, 1.2, 0.6}, {100, 0.05, 9, 0.05}};
    @Test @DisplayName("RB-06")
    void rb06FeedbackThresholds() {
        for (int i = 0; i < 20; i++) {
            double[] m = METRICS[i];
            run("RB-06", i, () -> {
                EnumSet<FireWarning> expect = EnumSet.noneOf(FireWarning.class);
                if (m[0] > 30.0) expect.add(FireWarning.SLOW_RESPONSE);
                if (m[1] < 0.40) expect.add(FireWarning.LOW_ACCURACY);
                if (m[2] > 2.0) expect.add(FireWarning.ERRATIC_MOVEMENT);
                if (m[3] < 0.50 && m[3] > 0) expect.add(FireWarning.INEFFICIENT_PATH);
                EnumSet<FireWarning> got = SimulationFeedback.fireWarnings(new FireMetrics(m[0], m[1], m[2], m[3]));
                check(got.equals(expect), "delay " + m[0] + ", accuracy " + m[1] + ", panic " + m[2] + ", path " + m[3] + ": warnings " + got + ", documented " + expect);
                return "delay " + m[0] + " s, accuracy " + m[1] + ", panic " + m[2] + ", path " + m[3] + " -> " + (got.isEmpty() ? "no warning" : got.toString()) + ".";
            });
        }
    }
}
