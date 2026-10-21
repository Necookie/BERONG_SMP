package net.necookie.disastersim.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static net.necookie.disastersim.gametest.GT.*;
import static net.necookie.disastersim.gametest.IanGT.*;

/**
 * Ian's EQ-01..EQ-07 (earthquake scenario), 20 variants each. Timings assume the run-gametest config:
 * 60-tick warmup, then RUMBLE 40, PEAK 200, AFTERSHOCK 40 ticks. EQ-08 (manual review) is a dashboard check.
 */
final class IanQuakeTests {

    private IanQuakeTests() {}

    private static final int RUMBLE_END = WARMUP + 40, PEAK_END = RUMBLE_END + 200, AFTER_END = PEAK_END + 40;

    private static String phase(SimulationSession s) { return s.getQuakePhase() == null ? "none" : s.getQuakePhase().name(); }
    private static void crouch(RecordingPlayer p, boolean on) { p.setShiftKeyDown(on); p.setPose(on ? Pose.CROUCHING : Pose.STANDING); }
    private static Object endReason(SimulationSession s) { return last(s, "SIM_END").map(e -> e.data().get("end_reason")).orElse(null); }
    private static SimulationState stateFor(int i) { return i % 2 == 0 ? SimulationState.EARTHQUAKE : SimulationState.CCS_EARTHQUAKE; }

    /** Quake run where the player crouches under cover for {@code holdTicks}, starting early in PEAK; returns when the hold window is over. */
    private interface HoldCheck { String apply(RecordingPlayer p, SimulationSession s, String phaseAtHold); }
    private static void holdCase(net.minecraft.gametest.framework.GameTestHelper h, String id, int i, int holdTicks, HoldCheck check) {
        RecordingPlayer p = join(h, id.toLowerCase().replace("-", ""));
        SimulationSession s = start(p, stateFor(i));
        BlockPos base = platform(h);
        if (holdTicks > 0) h.getLevel().setBlock(base.above(2), Blocks.STONE.defaultBlockState(), 3);   // cover overhead
        tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        int startHold = WARMUP + 45;                                                                      // RUMBLE lasts 40 ticks, so this is inside PEAK
        AtomicReference<String> phaseAtHold = new AtomicReference<>();
        h.onEachTick(() -> {
            long t = h.getTick();
            boolean on = holdTicks > 0 && t >= startHold && t < startHold + holdTicks;
            if (on && phaseAtHold.get() == null && s.getQuakePhase() != null) phaseAtHold.set(phase(s));
            crouch(p, on);
        });
        h.runAfterDelay(startHold + 160, safe(h, id, () -> verdict(h, id, () -> check.apply(p, s, phaseAtHold.get()))));
    }

    static void registerAll() {
        // ───────── EQ-01 rumble phase ─────────
        each("EQ-01", 400, (h, i) -> {
            String id = cid("EQ-01", i);
            RecordingPlayer p = join(h, "eq01_" + i);
            SimulationSession s = start(p, stateFor(i));
            double mag = 4.0 + (i % 11) * 0.5;
            cmd(p, "sim_magnitude " + mag, true);
            h.runAfterDelay(WARMUP + 1 + i * 2, safe(h, id, () -> verdict(h, id, () -> {
                need("RUMBLE".equals(phase(s)), "phase " + phase(s) + " at tick " + h.getTick());
                need(Math.abs(s.getSessionMagnitude() - mag) < 1e-9, "magnitude " + s.getSessionMagnitude() + ", set " + mag);
                need(SimulationManager.getSession(p.getUUID()) == s, "run ended during the rumble");
                return stateFor(i) + " tick " + h.getTick() + ": phase RUMBLE at magnitude " + mag + ".";
            })));
        });

        // ───────── EQ-02 peak shaking ─────────
        each("EQ-02", 500, (h, i) -> {
            String id = cid("EQ-02", i);
            RecordingPlayer p = join(h, "eq02_" + i);
            SimulationSession s = start(p, stateFor(i));
            h.runAfterDelay(RUMBLE_END + 5 + i * 9, safe(h, id, () -> verdict(h, id, () -> {
                need("PEAK".equals(phase(s)), "phase " + phase(s) + " at tick " + h.getTick());
                need(p.heard("intensifying"), "no 'intensifying' message: " + p.messages);
                return stateFor(i) + " tick " + h.getTick() + ": phase PEAK, player told the shaking is intensifying.";
            })));
        });

        // ───────── EQ-03 drop, cover and hold (held 5 s or more) ─────────
        each("EQ-03", 700, (h, i) -> {
            String id = cid("EQ-03", i);
            int hold = 105 + i * 4;   // 105..181 ticks (5.25 to 9 s)
            holdCase(h, id, i, hold, (p, s, ph) -> {
                int n = events(s, "duck_cover_hold").size();
                need(n == 1, "duck_cover_hold events " + n + " after holding " + hold + " ticks");
                need("PEAK".equals(ph), "hold began in phase " + ph);
                return "Held cover for " + hold + " ticks (" + (hold / 20.0) + " s) in PEAK: one duck_cover_hold event logged.";
            });
        });

        // ───────── EQ-04 missed protective action ─────────
        each("EQ-04", 700, (h, i) -> {
            String id = cid("EQ-04", i);
            int hold = i * 5;   // 0..95 ticks: never reaches the 5 s (100 tick) rule
            holdCase(h, id, i, hold, (p, s, ph) -> {
                need(events(s, "duck_cover_hold").isEmpty(), "a duck_cover_hold event was logged after only " + hold + " ticks");
                int moves = (int) s.buildMoveCsv().lines().filter(l -> l.contains(",move_tick,")).count();
                need(moves > 100, "only " + moves + " movement rows recorded for the unsafe run");
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                SimEvent end = last(s, "SIM_END").orElseThrow();
                need(Boolean.FALSE.equals(end.data().get("passed")) && Integer.valueOf(0).equals(end.data().get("score")), "SIM_END " + end.data());
                return (hold == 0 ? "No cover taken" : "Cover held only " + hold + " ticks") + ": no duck_cover_hold event; " + moves + " movement rows kept; run ended with passed=false, score 0 pending instructor review.";
            });
        });

        // ───────── EQ-05 aftershock ─────────
        each("EQ-05", 600, (h, i) -> {
            String id = cid("EQ-05", i);
            RecordingPlayer p = join(h, "eq05_" + i);
            SimulationSession s = start(p, stateFor(i));
            h.runAfterDelay(PEAK_END + 2 + (i * 9) / 5, safe(h, id, () -> verdict(h, id, () -> {
                need("AFTERSHOCK".equals(phase(s)), "phase " + phase(s) + " at tick " + h.getTick());
                need(p.heard("Aftershock"), "no 'Aftershock' message: " + p.messages);
                return stateFor(i) + " tick " + h.getTick() + ": phase AFTERSHOCK, player warned \"Aftershock!\".";
            })));
        });

        // ───────── EQ-06 evacuation after the shaking stops ─────────
        each("EQ-06", 700, (h, i) -> {
            String id = cid("EQ-06", i);
            RecordingPlayer p = join(h, "eq06_" + i);
            boolean ccs = i % 2 == 1;
            SimulationSession s = start(p, stateFor(i));
            flow(h).when(1, () -> "END".equals(phase(s)), safe(h, id, () -> {   // wait for the real END phase instead of guessing a tick
                need(p.heard("shaking has stopped"), "no 'shaking has stopped' message");
                double x = ccs ? 100.0 + (i % 5) * 4.0 : 50.5 + (i % 4) * 0.9;
                double z = ccs ? 69.0 + (i % 3) * 1.4 : 93.5 + (i % 3) * 0.9;
                tp(p, x, -32, z);
            })).after(8 + (i % 5), safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> ex = events(s, "emergency_exit");
                need(ex.size() == 1, "emergency_exit events " + ex.size());
                need(SimulationManager.getSession(p.getUUID()) == s, "run ended at the exit");
                return (ccs ? "CCS" : "Library") + ": after the shaking stopped the player crossed the exit (" + ex.get(0).data().get("exit") + "): emergency_exit logged, run continued toward assembly.";
            }))).start();
        });

        // ───────── EQ-07 assembly ─────────
        each("EQ-07", 700, (h, i) -> {
            String id = cid("EQ-07", i);
            RecordingPlayer p = join(h, "eq07_" + i);
            SimulationSession s = start(p, SimulationState.EARTHQUAKE);
            double x = 32.0 + (i % 5) * 10.0, z = 66.0 + (i / 5) * 4.5;
            h.runAfterDelay(AFTER_END + 3, safe(h, id, () -> tp(p, x, -33, z)));
            h.runAfterDelay(AFTER_END + 10, safe(h, id, () -> verdict(h, id, () -> {
                need(SimulationManager.getSession(p.getUUID()) == null, "run still active in the assembly zone at (" + x + ", " + z + ")");
                need("assembly_reached".equals(endReason(s)), "end_reason " + endReason(s));
                List<SimEvent> a = events(s, "assembly_area_reached");
                need(a.size() == 1, "assembly_area_reached events " + a.size());
                SimEvent end = last(s, "SIM_END").orElseThrow();
                need(Boolean.FALSE.equals(end.data().get("passed")) && Integer.valueOf(0).equals(end.data().get("score")), "SIM_END " + end.data());
                return "Reached the Library assembly zone at (" + x + ", " + z + "): one assembly_area_reached event, end_reason assembly_reached, score 0 / passed=false until an instructor reviews.";
            })));
        });
    }
}
