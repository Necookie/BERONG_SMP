package net.necookie.disastersim;

import net.necookie.disastersim.common.simulation.NewSimScoring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NewSimScoring is plain Java (no NeoForge/Minecraft dependency at all), so it's tested directly
 * against representative New Sim Building 2.0 run outcomes — the rule-based prep_level computation
 * telemetry_contract.md v1.2 requires, and the only place that logic lives.
 */
class NewSimScoringTest {

    @Test
    void perfectPreventionRunScoresHighAndPasses() {
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 5, 0, 0, true, false, "all_hazards_prevented");
        assertEquals(95, r.score()); // 5*12 + 25(all-prevented bonus) + 10(alarm) = 95
        assertEquals("HIGH", r.prepLevel());
        assertTrue(r.passed());
        assertTrue(r.weakAreas().isEmpty(), "a perfect run should have no weak areas");
    }

    @Test
    void partialInterventionRunScoresModerateAndPasses() {
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 3, 2, 1, false, false, "intervention_success");
        // 3*12=36 (no all-prevented bonus, 3<5) + 1*8=8 (no clear bonus, 1<2) + 0(no alarm) = 44
        assertEquals(44, r.score());
        assertEquals("MODERATE", r.prepLevel());
        assertTrue(r.passed());
        assertEquals(3, r.weakAreas().size(), "missing hazards, incomplete containment, and no alarm should all be flagged");
    }

    @Test
    void timeoutWithNothingDoneScoresLowAndFails() {
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 0, 5, 0, false, false, "timeout");
        assertEquals(0, r.score());
        assertEquals("LOW", r.prepLevel());
        assertFalse(r.passed(), "a timeout must never count as a pass");
        assertEquals(4, r.weakAreas().size(),
                "prevention, containment, alarm, and evacuation should all be flagged");
    }

    @Test
    void assemblyReachedGrantsPartialCreditAndPasses() {
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 2, 3, 3, true, true, "assembly_reached");
        // 2*12=24 (no bonus) + 3*8=24 + 10(clear bonus, 3>=3) + 10(alarm) + 15(assembly) = 83
        assertEquals(83, r.score());
        assertEquals("HIGH", r.prepLevel());
        assertTrue(r.passed());
        assertEquals(1, r.weakAreas().size(), "only the missed hazards should still be flagged");
    }

    @Test
    void zeroArmedHazardsNeitherRewardsNorPenalizesPrevention() {
        // Degenerate case: the building's own hazard-prop scan came up with 0 arm-able props.
        // armedCount > 0 is false, so neither the "all prevented" bonus nor the "missed some"
        // weak-area line should fire — this must not throw or double-count either branch.
        NewSimScoring.Result r = NewSimScoring.evaluate(
                0, 0, 0, 0, true, false, "all_hazards_prevented");
        assertEquals(10, r.score()); // only the alarm bonus
        assertEquals("LOW", r.prepLevel());
        assertTrue(r.passed());
        assertTrue(r.weakAreas().isEmpty());
    }

    @Test
    void injuredEndReasonNeverPassesRegardlessOfStats() {
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 5, 0, 0, true, true, "injured");
        assertFalse(r.passed(), "getting injured must never count as a pass even with a perfect stat line");
    }

    @Test
    void scoreIsCappedAtOneHundred() {
        // 5*12 + 25 + (escalated=0, no intervention points) + 10 alarm = 95, well under 100 anyway —
        // this instead checks the cap logic directly holds for a hypothetical over-full stat line.
        NewSimScoring.Result r = NewSimScoring.evaluate(
                5, 5, 5, 5, true, true, "assembly_reached");
        assertTrue(r.score() <= 100, "score must never exceed 100");
    }
}
