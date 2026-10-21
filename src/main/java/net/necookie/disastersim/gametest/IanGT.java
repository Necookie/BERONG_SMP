package net.necookie.disastersim.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/**
 * Shared plumbing for the Ian testing-instrument cases (Testing/ian). Every ID gets 20 numbered
 * variants (AT-01-R01 .. AT-01-R20); each variant ends by recording exactly one PASS or FAIL line in
 * run-gametest/gt_actuals.jsonl, and always ends the game test normally, so a failed variant never
 * hides the other 19 and never aborts the slice.
 */
final class IanGT {

    private IanGT() {}

    static final int WARMUP = 60;
    static final String PW = "GtPass#2026";

    static String cid(String id, int variant) { return id + "-R" + String.format("%02d", variant + 1); }

    /** Registers 20 variants of one ID. {@code body} receives the test helper and the variant index 0..19. */
    static void each(String id, int maxTicks, BiConsumer<GameTestHelper, Integer> body) {
        for (int i = 0; i < 20; i++) {
            final int v = i;
            add(cid(id, v), maxTicks, h -> {
                try { body.accept(h, v); }
                catch (Throwable t) { conclude(h, cid(id, v), t); }
            });
        }
    }

    static RecordingPlayer join(GameTestHelper h, String tag) { reset(h); return RecordingPlayer.join(h, tag); }

    static SimulationSession start(RecordingPlayer p, SimulationState state) {
        SimulationManager.startSimulation(p, state);
        SimulationSession s = SimulationManager.getSession(p.getUUID());
        if (s == null) throw new IllegalStateException("simulation did not start: " + p.messages);
        p.setInvulnerable(true);   // keep the mock player alive through fire/quake damage
        return s;
    }

    static void need(boolean cond, String msg) { if (!cond) throw new AssertionError(msg); }

    /** Runs the variant's checks, records PASS (with the returned sentence) or FAIL (with the reason), then ends the test. */
    static void verdict(GameTestHelper h, String caseId, Supplier<String> checks) {
        try {
            String text = checks.get();
            record(caseId, "PASS", text);
        } catch (Throwable t) {
            record(caseId, "FAIL", message(t));
        }
        try { reset(h); } catch (Throwable ignored) { }
        h.succeed();
    }

    /** Called when something unexpected threw outside {@link #verdict}: records FAIL and ends the test normally. */
    static void conclude(GameTestHelper h, String caseId, Throwable t) {
        record(caseId, "FAIL", "harness error: " + message(t));
        try { reset(h); } catch (Throwable ignored) { }
        h.succeed();
    }

    /** A scheduled step that records FAIL instead of letting an exception escape into the game-test framework. */
    static Runnable safe(GameTestHelper h, String caseId, Runnable r) {
        return () -> {
            try { r.run(); }
            catch (Throwable t) { conclude(h, caseId, t); }
        };
    }

    /** True once {@code seconds} of wall-clock time have passed since the first poll, or when {@code cond} holds (network work cannot be timed in ticks). */
    static java.util.function.BooleanSupplier giveUpAfter(int seconds, java.util.function.BooleanSupplier cond) {
        long[] deadline = {0};
        return () -> {
            if (deadline[0] == 0) deadline[0] = System.currentTimeMillis() + seconds * 1000L;
            return System.currentTimeMillis() >= deadline[0] || cond.getAsBoolean();
        };
    }

    static String message(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.toString() : m;
    }

    /** The documented New Sim Building 2.0 score (thesis section 10), written out independently of NewSimScoring. */
    static int expectedNewSim(int armed, int prevented, int escalated, int extinguished, boolean alarm, boolean assembly, String end) {
        int s = prevented * 12;
        if (armed > 0 && prevented >= armed) s += 25;
        if (escalated > 0) { s += extinguished * 8; if (extinguished >= escalated) s += 10; }
        if (alarm) s += 10;
        if (("assembly_reached".equals(end) || "timeout".equals(end)) && assembly) s += 15;
        return Math.min(100, s);
    }
}
