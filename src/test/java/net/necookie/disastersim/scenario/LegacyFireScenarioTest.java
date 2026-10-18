package net.necookie.disastersim.scenario;

import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationFeedback;
import net.necookie.disastersim.common.simulation.SimulationFeedback.FireMetrics;
import net.necookie.disastersim.common.simulation.SimulationFeedback.FireWarning;
import net.necookie.disastersim.common.simulation.SimulationScoring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** System-test cases GS-14 (legacy fire scoring) and GS-15 (adaptive feedback thresholds). */
class LegacyFireScenarioTest {

    private static final int DEFAULT_PASS_THRESHOLD = 5; // Config.PASS_THRESHOLD_FIRE default

    // A run on the good side of every threshold; each case changes one metric.
    private static EnumSet<FireWarning> warn(double delay, double accuracy, double panic, double path) {
        return SimulationFeedback.fireWarnings(new FireMetrics(delay, accuracy, panic, path));
    }

    @Test @DisplayName("GS-14-01")
    void noFiresScoresZero() {
        assertEquals(0, SimulationScoring.fireScore(0));
    }

    @Test @DisplayName("GS-14-02")
    void twoPointsPerFire() {
        assertEquals(20, SimulationScoring.fireScore(10));
        assertEquals(100, SimulationScoring.fireScore(50));
    }

    @Test @DisplayName("GS-14-03")
    void scoreCapsAtOneHundred() {
        assertEquals(100, SimulationScoring.fireScore(60));
    }

    @Test @DisplayName("GS-14-04")
    void passFlipsAtTheConfiguredThreshold() {
        assertFalse(SimulationScoring.firePassed(DEFAULT_PASS_THRESHOLD - 1, DEFAULT_PASS_THRESHOLD));
        assertTrue(SimulationScoring.firePassed(DEFAULT_PASS_THRESHOLD, DEFAULT_PASS_THRESHOLD));
        assertTrue(SimulationScoring.firePassed(DEFAULT_PASS_THRESHOLD + 1, DEFAULT_PASS_THRESHOLD));
    }

    @Test @DisplayName("GS-15-01")
    void slowResponseThreshold() {
        assertTrue(warn(31, 0.8, 0.5, 0.9).contains(FireWarning.SLOW_RESPONSE));
        assertFalse(warn(29, 0.8, 0.5, 0.9).contains(FireWarning.SLOW_RESPONSE));
    }

    @Test @DisplayName("GS-15-02")
    void lowAccuracyThreshold() {
        assertTrue(warn(10, 0.39, 0.5, 0.9).contains(FireWarning.LOW_ACCURACY));
        assertFalse(warn(10, 0.41, 0.5, 0.9).contains(FireWarning.LOW_ACCURACY));
    }

    @Test @DisplayName("GS-15-03")
    void erraticMovementThreshold() {
        assertTrue(warn(10, 0.8, 2.1, 0.9).contains(FireWarning.ERRATIC_MOVEMENT));
        assertFalse(warn(10, 0.8, 1.9, 0.9).contains(FireWarning.ERRATIC_MOVEMENT));
    }

    @Test @DisplayName("GS-15-04")
    void inefficientPathThreshold() {
        assertTrue(warn(10, 0.8, 0.5, 0.49).contains(FireWarning.INEFFICIENT_PATH));
        assertFalse(warn(10, 0.8, 0.5, 0.51).contains(FireWarning.INEFFICIENT_PATH));
    }

    @Test @DisplayName("GS-15-05")
    void goodRunGetsNoCorrectiveMessage() {
        assertTrue(warn(10, 0.8, 0.5, 0.9).isEmpty());
    }

    @Test @DisplayName("GS-15-06")
    void ccsRunWithCo2ExtinguisherIsMeasuredCorrectly() {
        // What a CCS_FIRE run actually logs: the CO2 extinguisher emits only the contract v1.2
        // pin_pull / ext_spray events (plus extinguisher_use), never the ABC-only EXT_* pair.
        List<SimEvent> events = List.of(
                new SimEvent("SIM_START", 0, Map.of("sim_type", "CCS_FIRE")),
                new SimEvent("pin_pull", 5000, Map.of("extinguisher_class", "CO2")),
                new SimEvent("ext_spray", 6000, Map.of("hit_fire", true, "extinguisher_class", "CO2")),
                new SimEvent("extinguisher_use", 6000, Map.of("hit_target", true)),
                new SimEvent("ext_spray", 7000, Map.of("hit_fire", true, "extinguisher_class", "CO2")),
                new SimEvent("extinguisher_use", 7000, Map.of("hit_target", true)));
        FireMetrics m = SimulationFeedback.computeFireMetrics(events);
        assertEquals(5.0, m.decisionDelay(), 1e-9, "decision delay");
        assertEquals(1.0, m.sprayAccuracy(), 1e-9, "spray accuracy");
        EnumSet<FireWarning> w = SimulationFeedback.fireWarnings(m);
        assertFalse(w.contains(FireWarning.SLOW_RESPONSE));
        assertFalse(w.contains(FireWarning.LOW_ACCURACY));
    }
}
