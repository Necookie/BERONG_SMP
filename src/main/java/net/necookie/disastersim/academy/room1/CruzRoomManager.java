package net.necookie.disastersim.academy.room1;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 1 — Officer Cruz's Movement School. Two {@code CustomNpcEntity} instances share
 * {@code NpcType.OFFICER_CRUZ} in the schematic (one at the briefing start, one at the Go/Stop
 * tunnel finish); both route through this same handler and are shown whatever line list matches
 * the player's *current* phase, so which physical NPC was clicked doesn't matter — a player
 * legitimately can't reach the finish-line NPC before actually finishing the phases in between.
 *
 * <p>Zone-gating (the 4 green marks, maze exit, jump obstacles) and the Go/Stop mini-game's
 * movement-during-STOP detection are added in {@link #tick}; for now dialogue-driven transitions
 * only cover the two phases where talking itself is the gate (the opening briefing and the
 * Go/Stop staging speech).
 */
public final class CruzRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    private CruzRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        CruzPhase phase = data.get(player.getUUID()).cruzPhase();

        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.CRUZ_LINES.get(phase);
        boolean advance = AcademyManager.stepDialogue(player, dialogueSteps, lines);
        if (!advance) return;

        CruzPhase next = switch (phase) {
            case NOT_STARTED -> CruzPhase.BRIEFING;
            case GOSTOP_STAGE -> CruzPhase.GOSTOP_RUN;
            default -> phase; // this phase's advancement is condition-gated, not dialogue-gated
        };
        if (next != phase) {
            data.mutate(player.getUUID(), p -> p.setCruzPhase(next));
            AcademyManager.resetDialogueStep(dialogueSteps, player);
        }
    }

    /** No-op until Room 1's zone-gating and Go/Stop mini-game are added. */
    public static void tick(ServerLevel level) {
    }
}
