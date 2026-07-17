package net.necookie.disastersim;

import net.necookie.disastersim.common.simulation.SimulationScoring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the legacy fire-run scoring/pass rule extracted into {@link SimulationScoring} after
 * SimulationManager.endSimulation and the old SessionManager.onSimulationEnd were found to compute
 * pass/fail two different ways (score-based, i.e. half as many fires, vs. fires-based directly).
 * {@code passThresholdFire}'s config comment ("Fires extinguished required to pass") documents the
 * fires-based rule as the intent — these tests pin that down so it can't silently drift again.
 */
class SimulationScoringTest {

    @Test
    void scoreIsTwoPointsPerFireExtinguished() {
        assertEquals(0, SimulationScoring.fireScore(0));
        assertEquals(2, SimulationScoring.fireScore(1));
        assertEquals(10, SimulationScoring.fireScore(5));
    }

    @Test
    void scoreCapsAtOneHundred() {
        assertEquals(100, SimulationScoring.fireScore(50));
        assertEquals(100, SimulationScoring.fireScore(51));
        assertEquals(100, SimulationScoring.fireScore(1000));
    }

    @Test
    void scoreNeverGoesNegativeForNegativeInput() {
        // Defensive — firesExtinguished should never actually be negative, but a stray decrement
        // bug anywhere upstream shouldn't be able to produce a negative score.
        assertEquals(0, SimulationScoring.fireScore(-3));
    }

    @Test
    void passRequiresFiresToMeetThresholdDirectly() {
        // The documented rule: passThresholdFire fires extinguished required, NOT
        // score >= passThresholdFire (which would only require half as many fires).
        int threshold = 5;
        assertFalse(SimulationScoring.firePassed(4, threshold), "4 fires must not pass a threshold of 5");
        assertTrue(SimulationScoring.firePassed(5, threshold), "exactly meeting the threshold must pass");
        assertTrue(SimulationScoring.firePassed(6, threshold), "exceeding the threshold must pass");
    }

    @Test
    void passThresholdIsNotHalvedByScoreDoubling() {
        // Regression guard for the exact H4 bug: a session with 3 fires extinguished (score 6)
        // must NOT pass a threshold of 5, even though 6 >= 5 — passing must be judged on fires
        // extinguished directly, never on the doubled score value.
        int threshold = 5;
        int fires = 3;
        assertTrue(SimulationScoring.fireScore(fires) >= threshold,
                "sanity check: score for 3 fires (6) is indeed >= the threshold (5)");
        assertFalse(SimulationScoring.firePassed(fires, threshold),
                "3 fires must not pass a threshold of 5 fires, regardless of the doubled score");
    }

    @Test
    void zeroThresholdAlwaysPasses() {
        assertTrue(SimulationScoring.firePassed(0, 0));
    }
}
