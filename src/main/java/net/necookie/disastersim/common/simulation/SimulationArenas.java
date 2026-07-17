package net.necookie.disastersim.common.simulation;

/**
 * Pure mapping from a {@link SimulationManager.SimulationState} to the physical {@link Arena} it
 * runs in — extracted out of {@code SimulationManager} so this one piece of the arena-occupancy
 * fix (see {@code SimulationManager}'s class javadoc and {@code
 * docs/history/code-review-remediation-log-2026-07-17.md}) is unit-testable without touching
 * {@code SimulationManager} itself, which has heavy NeoForge-dependent static state (structure
 * placers, {@code Identifier}s built from {@code BerongSMP.MODID}, whose own static initializer
 * needs a live game runtime) that can't be safely loaded from a plain JUnit test.
 *
 * <p>Referencing only {@link SimulationManager.SimulationState} — a nested enum with its own
 * independent static initializer, not the enclosing {@code SimulationManager} class (same
 * precedent as {@code SimulationStateTest}) — keeps this class itself free of that problem.
 *
 * <p>Two states can legitimately share one arena: FIRE and EARTHQUAKE both run in the Library
 * building, CCS_FIRE and CCS_EARTHQUAKE both run in the CCS Admin Building. This is exactly why
 * {@code SimulationManager}'s occupancy guard is keyed by {@link Arena}, not by {@code
 * SimulationState} directly — placing the Library building for a second FIRE session would just
 * as surely wreck an in-progress EARTHQUAKE session sharing that same building.
 */
public final class SimulationArenas {

    /** The three physically-distinct simulation buildings a graded run can take place in. */
    public enum Arena { LIBRARY, CCS, NEW_SIM_BUILDING2 }

    private SimulationArenas() {}

    /** Which physical {@link Arena} a given scenario runs in. Throws for {@code IDLE}, which never reaches here. */
    public static Arena arenaFor(SimulationManager.SimulationState state) {
        return switch (state) {
            case FIRE, EARTHQUAKE -> Arena.LIBRARY;
            case CCS_FIRE, CCS_EARTHQUAKE -> Arena.CCS;
            case NEW_SIM_BUILDING2_FIRE -> Arena.NEW_SIM_BUILDING2;
            case IDLE -> throw new IllegalStateException("IDLE has no arena — should never reach here");
        };
    }
}
