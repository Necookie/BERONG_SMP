package net.necookie.disastersim;

import net.necookie.disastersim.common.simulation.SimulationSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the EarthquakePhase ordinal ordering.
 *
 * NOTE: This test requires NeoForge / Minecraft classes on the test classpath.
 * If compilation fails in CI, configure the test task with the NeoForge moddev
 * unit-test sourceset (see NeoForge moddev plugin docs) and re-enable this class.
 *
 * To run manually in dev: ./gradlew test
 */
class EarthquakePhaseOrderTest {

    @Test
    void phaseOrderIsRumblePeakAftershockEnd() {
        SimulationSession.EarthquakePhase[] phases = SimulationSession.EarthquakePhase.values();
        assertArrayEquals(
            new SimulationSession.EarthquakePhase[]{
                SimulationSession.EarthquakePhase.RUMBLE,
                SimulationSession.EarthquakePhase.PEAK,
                SimulationSession.EarthquakePhase.AFTERSHOCK,
                SimulationSession.EarthquakePhase.END
            },
            phases,
            "Phase order changed — verify tickQuakePhase() transitions still work correctly"
        );
    }

    @Test
    void exactlyFourPhases() {
        assertEquals(4, SimulationSession.EarthquakePhase.values().length);
    }

    @Test
    void eachPhaseOrdinalIsStrictlyIncreasing() {
        SimulationSession.EarthquakePhase[] phases = SimulationSession.EarthquakePhase.values();
        for (int i = 1; i < phases.length; i++) {
            assertTrue(phases[i - 1].ordinal() < phases[i].ordinal(),
                    phases[i - 1] + " must precede " + phases[i]);
        }
    }
}
