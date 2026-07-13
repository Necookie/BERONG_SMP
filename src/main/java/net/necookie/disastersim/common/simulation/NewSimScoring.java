package net.necookie.disastersim.common.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based scorer for New Sim Building 2.0's prevention/intervention/evacuation run — the mod's
 * first implementation of telemetry_contract.md v1.2's "game-computed rule-based prep_level"
 * requirement. Pattern-cloned from {@code academy.room4.AcademyScoring}'s {@code Result(score,
 * weakAreas)} shape, but a fully independent class: this scores a real graded
 * {@code SimulationSession}, not an {@code AcademyProgress}.
 */
public final class NewSimScoring {

    public record Result(int score, String prepLevel, boolean passed, List<String> weakAreas) {}

    private static final int PREVENTION_POINTS_PER_HAZARD = 12; // up to 60 for 5/5
    private static final int ALL_PREVENTED_BONUS = 25;
    private static final int INTERVENTION_POINTS_PER_HAZARD = 8;
    private static final int INTERVENTION_CLEAR_BONUS = 10;
    private static final int ALARM_BONUS = 10;
    private static final int ASSEMBLY_BONUS = 15;

    private NewSimScoring() {}

    /**
     * @param armedCount        how many hazards were armed for this attempt (normally 5, fewer if
     *                          the building's hazard pool came up short)
     * @param preventedCount    hazards bare-hand-defused before ever igniting
     * @param escalatedCount    hazards that ignited (armedCount - preventedCount)
     * @param extinguishedCount escalated hazards successfully put out with the correct extinguisher
     * @param alarmRung         whether the fire alarm was pressed at any point this run
     * @param assemblyReached   whether the player reached the assembly zone (only meaningful once
     *                          evacuation was actually triggered)
     * @param endReason         {@code all_hazards_prevented} / {@code intervention_success} /
     *                          {@code assembly_reached} / {@code timeout} / {@code injured}
     */
    public static Result evaluate(int armedCount, int preventedCount, int escalatedCount,
                                   int extinguishedCount, boolean alarmRung,
                                   boolean assemblyReached, String endReason) {
        List<String> weak = new ArrayList<>();
        int score = 0;

        score += preventedCount * PREVENTION_POINTS_PER_HAZARD;
        if (armedCount > 0 && preventedCount >= armedCount) {
            score += ALL_PREVENTED_BONUS;
        } else if (preventedCount < armedCount) {
            weak.add("spotting hazards before they ignite (" + preventedCount + "/" + armedCount + " found)");
        }

        if (escalatedCount > 0) {
            score += extinguishedCount * INTERVENTION_POINTS_PER_HAZARD;
            if (extinguishedCount >= escalatedCount) {
                score += INTERVENTION_CLEAR_BONUS;
            } else {
                weak.add("containing fires with the correct extinguisher ("
                        + extinguishedCount + "/" + escalatedCount + " contained)");
            }
        }

        if (alarmRung) {
            score += ALARM_BONUS;
        } else {
            weak.add("ringing the fire alarm to alert others");
        }

        boolean neededEvacuation = "assembly_reached".equals(endReason) || "timeout".equals(endReason);
        if (neededEvacuation) {
            if (assemblyReached) {
                score += ASSEMBLY_BONUS;
            } else {
                weak.add("evacuating to the assembly area in time");
            }
        }

        score = Math.min(100, score);
        String prepLevel = score >= 75 ? "HIGH" : score >= 40 ? "MODERATE" : "LOW";
        boolean passed = switch (endReason) {
            case "all_hazards_prevented", "intervention_success", "assembly_reached" -> true;
            default -> false;
        };

        return new Result(score, prepLevel, passed, weak);
    }
}
