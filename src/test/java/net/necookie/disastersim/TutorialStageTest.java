package net.necookie.disastersim;

import net.necookie.disastersim.tutorial.TutorialStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies TutorialStage ordering and completeness.
 * No Minecraft runtime is required — TutorialStage is a pure Java enum.
 */
class TutorialStageTest {

    @Test
    void completedIsLastStage() {
        TutorialStage[] stages = TutorialStage.values();
        assertEquals(TutorialStage.COMPLETED, stages[stages.length - 1],
                "COMPLETED must be the final stage so isComplete() checks work");
    }

    @Test
    void notStartedIsFirstStage() {
        assertEquals(TutorialStage.NOT_STARTED, TutorialStage.values()[0],
                "NOT_STARTED must be the first stage");
    }

    @Test
    void expectedStageCount() {
        assertEquals(11, TutorialStage.values().length,
                "Stage count changed — update gate logic and tutorial flow if intentional");
    }

    @Test
    void stagesAreInExpectedOrder() {
        TutorialStage[] expected = {
            TutorialStage.NOT_STARTED,
            TutorialStage.PASS_PULL,
            TutorialStage.PASS_SPRAY,
            TutorialStage.EXT_TYPE_A,
            TutorialStage.EXT_TYPE_B,
            TutorialStage.EXT_TYPE_C,
            TutorialStage.QUAKE_INTRO,
            TutorialStage.QUAKE_DROP,
            TutorialStage.QUAKE_COVER,
            TutorialStage.QUAKE_HOLDON,
            TutorialStage.COMPLETED
        };
        assertArrayEquals(expected, TutorialStage.values(),
                "Tutorial stage order changed — verify all progression logic is still correct");
    }

    @Test
    void completedOrdinalIsGreaterThanAllOthers() {
        int completedOrdinal = TutorialStage.COMPLETED.ordinal();
        for (TutorialStage stage : TutorialStage.values()) {
            if (stage != TutorialStage.COMPLETED) {
                assertTrue(stage.ordinal() < completedOrdinal,
                        stage + " must come before COMPLETED");
            }
        }
    }
}
