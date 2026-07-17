package net.necookie.disastersim.common.simulation;

/**
 * Single source of truth for scoring/passing a legacy fire-type run (Library, CCS — New Sim
 * Building 2.0 has its own richer scorer, see {@link NewSimScoring}).
 *
 * <p>Extracted because {@code SimulationManager.endSimulation} and the old
 * {@code SessionManager.onSimulationEnd} used to compute this independently and had drifted apart:
 * the former checked {@code score >= passThreshold} (i.e. {@code fires >= passThreshold / 2}, since
 * score is {@code fires * 2}) while the latter checked {@code fires >= passThreshold} directly —
 * the config comment on {@code passThresholdFire} ("Fires extinguished required to pass") documents
 * the second as the intent, so the first was quietly certifying runs on half the required fires.
 * {@code SessionManager.onSimulationEnd} no longer recomputes anything; it takes the already-scored
 * result from {@code endSimulation} instead — see its own javadoc.
 *
 * <p>Deliberately takes {@code passThreshold} as a parameter rather than reading
 * {@code Config.PASS_THRESHOLD_FIRE} itself, so this class stays plain and unit-testable
 * (NeoForge's {@code ModConfigSpec.ConfigValue.get()} throws outside a fully loaded mod
 * environment) — callers pass {@code Config.PASS_THRESHOLD_FIRE.get()} at the call site.
 */
public final class SimulationScoring {

    private SimulationScoring() {}

    /** Score for a legacy fire-type run: 2 points per fire extinguished, capped at 100. */
    public static int fireScore(int firesExtinguished) {
        return Math.min(100, Math.max(0, firesExtinguished) * 2);
    }

    /** Whether {@code firesExtinguished} meets {@code passThreshold} — the documented pass rule. */
    public static boolean firePassed(int firesExtinguished, int passThreshold) {
        return firesExtinguished >= passThreshold;
    }
}
