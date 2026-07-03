package net.necookie.disastersim.academy.room2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 2 — Sgt. Reyes's Fire Safety Drill. Gated on Room 1 being complete. Hazard-prop scripting,
 * correct-tool attribution, and the scripted ignite/drop-and-roll demo are added in {@link #tick}.
 */
public final class ReyesRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    private ReyesRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        var progress = data.get(player.getUUID());

        if (progress.cruzPhase() != CruzPhase.DONE) {
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §7Finish up with Officer Cruz first — come back once you're done!");
            return;
        }

        ReyesPhase phase = progress.reyesPhase();
        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.REYES_LINES.get(phase);
        boolean advance = AcademyManager.stepDialogue(player, dialogueSteps, lines);
        if (!advance) return;

        ReyesPhase next = switch (phase) {
            case NOT_STARTED -> ReyesPhase.TOOL_SELECTION;
            default -> phase; // condition-gated, not dialogue-gated
        };
        if (next != phase) {
            data.mutate(player.getUUID(), p -> p.setReyesPhase(next));
            AcademyManager.resetDialogueStep(dialogueSteps, player);
        }
    }

    /** No-op until Room 2's hazard-prop scripting and ignite-demo are added. */
    public static void tick(ServerLevel level) {
    }
}
