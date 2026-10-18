package net.necookie.disastersim.scenario;

import net.necookie.disastersim.common.simulation.NewSimScoring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** System-test cases GS-07 to GS-10 and GS-13-03 (New Sim Building 2.0 scoring). */
class NewSimScoringScenarioTest {

    private static final List<String> END_REASONS =
            List.of("all_hazards_prevented", "intervention_success", "assembly_reached", "timeout", "injured");

    @Test @DisplayName("GS-07-01")
    void fullPreventionWithAlarm() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 5, 0, 0, true, false, "all_hazards_prevented");
        assertEquals(95, r.score());
        assertEquals("HIGH", r.prepLevel());
        assertTrue(r.passed());
    }

    @Test @DisplayName("GS-07-02")
    void fullPreventionWithoutAlarm() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 5, 0, 0, false, false, "all_hazards_prevented");
        assertEquals(85, r.score());
        assertEquals("HIGH", r.prepLevel());
        assertTrue(r.passed());
        assertTrue(r.weakAreas().stream().anyMatch(w -> w.contains("fire alarm")));
    }

    @Test @DisplayName("GS-08-01")
    void interventionSuccess() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 3, 2, 2, true, false, "intervention_success");
        assertEquals(72, r.score()); // 36 + 16 + 10 + 10
        assertEquals("MODERATE", r.prepLevel());
        assertTrue(r.passed());
    }

    @Test @DisplayName("GS-08-02")
    void partialInterventionThenTimeout() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 3, 2, 1, false, false, "timeout");
        assertEquals(44, r.score()); // 36 + 8
        assertEquals("MODERATE", r.prepLevel());
        assertFalse(r.passed());
        assertEquals(4, r.weakAreas().size(), "hazards, containment, alarm and evacuation should all be flagged");
    }

    @Test @DisplayName("GS-09-01")
    void evacuatedWithAlarm() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 0, 5, 0, true, true, "assembly_reached");
        assertEquals(25, r.score());
        assertEquals("LOW", r.prepLevel());
        assertTrue(r.passed());
    }

    @Test @DisplayName("GS-09-02")
    void timeoutWithNothingDone() {
        NewSimScoring.Result r = NewSimScoring.evaluate(5, 0, 5, 0, false, false, "timeout");
        assertEquals(0, r.score());
        assertEquals("LOW", r.prepLevel());
        assertFalse(r.passed());
    }

    @Test @DisplayName("GS-09-03")
    void injuredNeverPasses() {
        assertFalse(NewSimScoring.evaluate(5, 4, 1, 1, true, false, "injured").passed());
    }

    @Test @DisplayName("GS-10-01")
    void everyCombinationStaysWithinZeroToOneHundred() {
        int combos = sweep((r) -> assertTrue(r.score() >= 0 && r.score() <= 100, "score " + r.score()));
        assertTrue(combos > 1000, "sweep covered " + combos + " combinations");
    }

    @Test @DisplayName("GS-10-02")
    void tierRuleHoldsForEveryCombination() {
        sweep((r) -> {
            String expected = r.score() >= 75 ? "HIGH" : r.score() >= 40 ? "MODERATE" : "LOW";
            assertEquals(expected, r.prepLevel(), "score " + r.score());
        });
    }

    @Test @DisplayName("GS-10-03")
    void rawOneHundredTenIsCapped() {
        assertEquals(100, NewSimScoring.evaluate(5, 5, 0, 0, true, true, "assembly_reached").score());
    }

    @Test @DisplayName("GS-10-04")
    void allPreventedBonusWithFewerHazards() {
        NewSimScoring.Result r = NewSimScoring.evaluate(3, 3, 0, 0, false, false, "all_hazards_prevented");
        assertEquals(61, r.score());
        assertEquals("MODERATE", r.prepLevel());
    }

    @Test @DisplayName("GS-13-03")
    void alarmIsWorthExactlyTenPoints() {
        int with = NewSimScoring.evaluate(5, 2, 3, 2, true, false, "intervention_success").score();
        int without = NewSimScoring.evaluate(5, 2, 3, 2, false, false, "intervention_success").score();
        assertEquals(10, with - without);
    }

    private static int sweep(java.util.function.Consumer<NewSimScoring.Result> check) {
        int n = 0;
        for (int armed = 1; armed <= 5; armed++)
            for (int prevented = 0; prevented <= armed; prevented++) {
                int escalated = armed - prevented;
                for (int ext = 0; ext <= escalated; ext++)
                    for (boolean alarm : new boolean[]{false, true})
                        for (boolean assembly : new boolean[]{false, true})
                            for (String end : END_REASONS) {
                                check.accept(NewSimScoring.evaluate(armed, prevented, escalated, ext, alarm, assembly, end));
                                n++;
                            }
            }
        return n;
    }
}
