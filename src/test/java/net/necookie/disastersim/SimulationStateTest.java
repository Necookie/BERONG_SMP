package net.necookie.disastersim;

import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the {@link SimulationState} category predicates. A large amount of branching across
 * SimulationManager, SimulationFeedback, telemetry, and the dashboard keys off isFire()/isQuake()/
 * isCCS(); if a new scenario type is added (e.g. SIM_3PHASE), these assertions force the author to
 * decide which categories it belongs to rather than letting it silently fall through a switch.
 *
 * <p>Referencing the nested enum initialises only {@code SimulationManager$SimulationState}, not the
 * enclosing SimulationManager (whose static init needs a live Minecraft runtime), so this stays a
 * pure unit test.
 */
class SimulationStateTest {

    @Test
    void fireStatesAreFireAndNotQuake() {
        for (SimulationState s : new SimulationState[]{SimulationState.FIRE, SimulationState.CCS_FIRE}) {
            assertTrue(s.isFire(), s + " should be a fire scenario");
            assertFalse(s.isQuake(), s + " must not be a quake scenario");
        }
    }

    @Test
    void quakeStatesAreQuakeAndNotFire() {
        for (SimulationState s : new SimulationState[]{SimulationState.EARTHQUAKE, SimulationState.CCS_EARTHQUAKE}) {
            assertTrue(s.isQuake(), s + " should be a quake scenario");
            assertFalse(s.isFire(), s + " must not be a fire scenario");
        }
    }

    @Test
    void ccsStatesAreFlaggedCcs() {
        assertTrue(SimulationState.CCS_FIRE.isCCS());
        assertTrue(SimulationState.CCS_EARTHQUAKE.isCCS());
        assertFalse(SimulationState.FIRE.isCCS());
        assertFalse(SimulationState.EARTHQUAKE.isCCS());
    }

    @Test
    void idleIsNeitherFireNorQuakeNorCcs() {
        assertFalse(SimulationState.IDLE.isFire());
        assertFalse(SimulationState.IDLE.isQuake());
        assertFalse(SimulationState.IDLE.isCCS());
    }

    @Test
    void everyNonIdleStateIsExactlyFireXorQuake() {
        for (SimulationState s : SimulationState.values()) {
            if (s == SimulationState.IDLE) continue;
            assertNotEquals(s.isFire(), s.isQuake(),
                    s + " must be exactly one of fire/quake, never both or neither");
        }
    }
}
