package net.necookie.disastersim.scenario;

import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room4.AcademyScoring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System-test cases GS-03 to GS-06 (Academy scoring). Display names carry the catalog case IDs
 * so the JUnit report can be merged into Testing/results/test_results.csv.
 */
class AcademyScenarioTest {

    private static AcademyProgress progress(int mistakes, int correct, int wrong, boolean quake, boolean drop) {
        return new AcademyProgress(CruzPhase.NOT_STARTED, ReyesPhase.NOT_STARTED, SantosPhase.NOT_STARTED,
                MorfePhase.NOT_STARTED, mistakes, correct, wrong, quake, drop);
    }

    // Every other component at full marks (75), so total - 75 isolates the component under test.
    private static int movementComponent(int mistakes) {
        return AcademyScoring.evaluate(progress(mistakes, 1, 0, true, true)).score() - 75;
    }

    private static int fireComponent(int correct, int wrong) {
        return AcademyScoring.evaluate(progress(0, correct, wrong, true, true)).score() - 75;
    }

    @Test @DisplayName("GS-03-01")
    void noMovementMistakesGivesFullMovementScore() {
        assertEquals(25, movementComponent(0));
    }

    @Test @DisplayName("GS-03-02")
    void oneMistakeCostsEightPointsAndIsFlagged() {
        assertEquals(17, movementComponent(1));
        assertTrue(AcademyScoring.evaluate(progress(1, 1, 0, true, true)).weakAreas().stream()
                .anyMatch(w -> w.contains("Officer Cruz")));
    }

    @Test @DisplayName("GS-03-03")
    void movementPenaltyNeverGoesBelowZero() {
        assertEquals(1, movementComponent(3));
        assertEquals(0, movementComponent(4));
        assertEquals(0, movementComponent(10));
    }

    @Test @DisplayName("GS-04-01")
    void allCorrectExtinguishersGiveFullFireScore() {
        assertEquals(25, fireComponent(3, 0));
        assertTrue(AcademyScoring.evaluate(progress(0, 3, 0, true, true)).weakAreas().isEmpty());
    }

    @Test @DisplayName("GS-04-02")
    void mixedSelectionIsProportionalAndFlagged() {
        assertEquals(17, fireComponent(2, 1)); // round(25 * 2/3) = 17
        assertTrue(AcademyScoring.evaluate(progress(0, 2, 1, true, true)).weakAreas().stream()
                .anyMatch(w -> w.contains("wrong extinguisher")));
    }

    @Test @DisplayName("GS-04-03")
    void noExtinguisherUseScoresZero() {
        assertEquals(0, fireComponent(0, 0));
    }

    @Test @DisplayName("GS-04-04")
    void preventionDemoAndAlarmBriefingAreNotScored() {
        AcademyProgress before = progress(0, 2, 0, true, true);
        AcademyProgress after = new AcademyProgress(CruzPhase.NOT_STARTED, ReyesPhase.EVACUATION_BRIEF, SantosPhase.NOT_STARTED,
                MorfePhase.NOT_STARTED, 0, 2, 0, true, true);
        assertEquals(AcademyScoring.evaluate(before).score(), AcademyScoring.evaluate(after).score());
    }

    @Test @DisplayName("GS-05-01")
    void dropAndRollIsWorthTwentyFivePoints() {
        int with = AcademyScoring.evaluate(progress(0, 1, 0, true, true)).score();
        AcademyScoring.Result without = AcademyScoring.evaluate(progress(0, 1, 0, true, false));
        assertEquals(25, with - without.score());
        assertTrue(without.weakAreas().stream().anyMatch(w -> w.contains("drop and roll")));
    }

    @Test @DisplayName("GS-05-02")
    void quakeComplianceIsWorthTwentyFivePoints() {
        int with = AcademyScoring.evaluate(progress(0, 1, 0, true, true)).score();
        AcademyScoring.Result without = AcademyScoring.evaluate(progress(0, 1, 0, false, true));
        assertEquals(25, with - without.score());
        assertTrue(without.weakAreas().stream().anyMatch(w -> w.contains("broke cover")));
    }

    @Test @DisplayName("GS-06-01")
    void perfectAcademyScoresOneHundred() {
        assertEquals(100, AcademyScoring.evaluate(progress(0, 4, 0, true, true)).score());
    }

    @Test @DisplayName("GS-06-02")
    void certificationFlipsExactlyAtSeventy() {
        // fire ratios 3/4, 4/5 and 5/6 give 19, 20 and 21; with movement + drop-and-roll full and no quake credit
        int s69 = AcademyScoring.evaluate(progress(0, 3, 1, false, true)).score();
        int s70 = AcademyScoring.evaluate(progress(0, 4, 1, false, true)).score();
        int s71 = AcademyScoring.evaluate(progress(0, 5, 1, false, true)).score();
        assertEquals(69, s69);
        assertEquals(70, s70);
        assertEquals(71, s71);
        assertFalse(AcademyScoring.isCertified(s69, 70));
        assertTrue(AcademyScoring.isCertified(s70, 70));
        assertTrue(AcademyScoring.isCertified(s71, 70));
    }
}
