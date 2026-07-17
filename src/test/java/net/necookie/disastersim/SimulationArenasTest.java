package net.necookie.disastersim;

import net.necookie.disastersim.common.simulation.SimulationArenas;
import net.necookie.disastersim.common.simulation.SimulationArenas.Arena;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards {@link SimulationArenas#arenaFor} — the mapping SimulationManager's arena-occupancy guard
 * (added in the 2026-07-17 code review remediation pass, see
 * docs/history/code-review-remediation-log-2026-07-17.md) depends on. The property under test is
 * exactly what that fix exists to protect: two SimulationStates that physically share one building
 * must map to the SAME Arena (so starting one refuses a concurrent session of the other in the
 * same building), while states in different buildings must map to DIFFERENT arenas (so they don't
 * block each other unnecessarily).
 *
 * <p>Only references {@link SimulationState} — a nested enum with its own independent static
 * initializer (same precedent as {@code SimulationStateTest}) — and {@link SimulationArenas}
 * itself, which was deliberately kept free of SimulationManager's NeoForge-dependent statics
 * specifically so this stays a pure, fast unit test instead of needing a live server.
 */
class SimulationArenasTest {

    @Test
    void fireAndEarthquakeShareTheLibraryArena() {
        assertEquals(Arena.LIBRARY, SimulationArenas.arenaFor(SimulationState.FIRE));
        assertEquals(Arena.LIBRARY, SimulationArenas.arenaFor(SimulationState.EARTHQUAKE));
    }

    @Test
    void ccsFireAndCcsEarthquakeShareTheCcsArena() {
        assertEquals(Arena.CCS, SimulationArenas.arenaFor(SimulationState.CCS_FIRE));
        assertEquals(Arena.CCS, SimulationArenas.arenaFor(SimulationState.CCS_EARTHQUAKE));
    }

    @Test
    void newSimBuilding2FireHasItsOwnArena() {
        assertEquals(Arena.NEW_SIM_BUILDING2, SimulationArenas.arenaFor(SimulationState.NEW_SIM_BUILDING2_FIRE));
    }

    @Test
    void theThreeBuildingsAreMutuallyDistinctArenas() {
        // A regression guard for the exact bug class C1 fixed: if a future edit ever collapsed two
        // of these buildings onto the same Arena value by mistake, this would catch it immediately —
        // the whole point of keying occupancy by Arena is that different buildings must never
        // compete for the same occupancy slot.
        assertNotEquals(SimulationArenas.arenaFor(SimulationState.FIRE),
                SimulationArenas.arenaFor(SimulationState.CCS_FIRE));
        assertNotEquals(SimulationArenas.arenaFor(SimulationState.FIRE),
                SimulationArenas.arenaFor(SimulationState.NEW_SIM_BUILDING2_FIRE));
        assertNotEquals(SimulationArenas.arenaFor(SimulationState.CCS_FIRE),
                SimulationArenas.arenaFor(SimulationState.NEW_SIM_BUILDING2_FIRE));
    }

    @Test
    void idleHasNoArena() {
        // IDLE is a sentinel — no real SimulationSession is ever constructed with it (every
        // startSimulation caller passes a real scenario type) — so arenaFor should fail loudly
        // rather than silently mapping it to some arbitrary arena if that invariant is ever broken.
        assertThrows(IllegalStateException.class, () -> SimulationArenas.arenaFor(SimulationState.IDLE));
    }

    @Test
    void everyNonIdleStateMapsToExactlyOneArena() {
        for (SimulationState state : SimulationState.values()) {
            if (state == SimulationState.IDLE) continue;
            assertNotNull(SimulationArenas.arenaFor(state), state + " must map to a real Arena");
        }
    }
}
