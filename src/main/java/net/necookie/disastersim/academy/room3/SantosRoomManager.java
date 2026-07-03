package net.necookie.disastersim.academy.room3;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 3 — Sgt. Santos's Earthquake Drill. Gated on Room 2 being complete. The table-row green
 * highlight, the quake trigger/shake, and the {@code DuckCoverHoldManager} compliance gate are
 * added in {@link #tick}.
 */
public final class SantosRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    private SantosRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        var progress = data.get(player.getUUID());

        if (progress.reyesPhase() != ReyesPhase.DONE) {
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7Finish the fire drill with Sgt. Reyes first!");
            return;
        }

        SantosPhase phase = progress.santosPhase();
        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.SANTOS_LINES.get(phase);
        boolean advance = AcademyManager.stepDialogue(player, dialogueSteps, lines);
        if (!advance) return;

        SantosPhase next = switch (phase) {
            case NOT_STARTED -> SantosPhase.PRE_DRILL;
            default -> phase; // condition-gated, not dialogue-gated
        };
        if (next != phase) {
            data.mutate(player.getUUID(), p -> p.setSantosPhase(next));
            AcademyManager.resetDialogueStep(dialogueSteps, player);
        }
    }

    /** No-op until Room 3's quake trigger, table highlight, and compliance gate are added. */
    public static void tick(ServerLevel level) {
    }
}
