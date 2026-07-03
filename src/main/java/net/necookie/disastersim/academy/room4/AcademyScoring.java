package net.necookie.disastersim.academy.room4;

import net.necookie.disastersim.academy.AcademyProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based rubric for Capt. Morfe's evaluation, pattern-cloned from
 * {@code world/SimulationFeedback}'s threshold-driven, purely server-side scoring approach — no
 * external calls, just arithmetic over the metrics the other 3 rooms already tracked on
 * {@link AcademyProgress}. 25 points per room (movement, fire, drop-and-roll, quake compliance).
 */
public final class AcademyScoring {

    private static final int MOVEMENT_POINTS = 25;
    private static final int FIRE_POINTS = 25;
    private static final int DROP_AND_ROLL_POINTS = 25;
    private static final int QUAKE_POINTS = 25;
    private static final int MISTAKE_PENALTY = 8;

    private AcademyScoring() {}

    public record Result(int score, List<String> weakAreas) {}

    public static Result evaluate(AcademyProgress progress) {
        List<String> weakAreas = new ArrayList<>();

        int movementScore = Math.max(0, MOVEMENT_POINTS - progress.movementMistakes() * MISTAKE_PENALTY);
        if (movementScore < MOVEMENT_POINTS) {
            weakAreas.add("you froze past the STOP window or lost your footing more than once with Officer Cruz");
        }

        int totalFireUses = progress.fireCorrectUses() + progress.fireWrongUses();
        int fireScore = totalFireUses == 0 ? 0
                : Math.round(FIRE_POINTS * (float) progress.fireCorrectUses() / totalFireUses);
        if (progress.fireWrongUses() > 0) {
            weakAreas.add("you reached for the wrong extinguisher at least once with Sgt. Reyes");
        }

        int dropScore = progress.dropAndRollPerformed() ? DROP_AND_ROLL_POINTS : 0;
        if (!progress.dropAndRollPerformed()) {
            weakAreas.add("you didn't drop and roll in time when you caught fire");
        }

        int quakeScore = progress.quakeCompliant() ? QUAKE_POINTS : 0;
        if (!progress.quakeCompliant()) {
            weakAreas.add("you broke cover before the shaking stopped with Sgt. Santos");
        }

        int total = Math.min(100, movementScore + fireScore + dropScore + quakeScore);
        return new Result(total, weakAreas);
    }
}
