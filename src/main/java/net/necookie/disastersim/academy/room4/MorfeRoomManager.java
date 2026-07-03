package net.necookie.disastersim.academy.room4;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;

/**
 * Room 4 — Capt. Cesar Morfe Jr.'s Evaluation. Gated on Room 3 being complete. Unlike the other
 * three NPCs, Morfe has no static {@code AcademyDialogue} entry — his verdict is score-dependent,
 * computed by {@link AcademyScoring} (added once the other rooms' scoring inputs exist to read).
 */
public final class MorfeRoomManager {

    private MorfeRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        AcademyProgress progress = data.get(player.getUUID());

        if (progress.santosPhase() != SantosPhase.DONE) {
            AcademyManager.sendPrompt(player, "§c[Capt. Morfe] §7Complete the earthquake drill with Sgt. Santos first.");
            return;
        }

        AcademyManager.sendPrompt(player, "§c[Capt. Morfe] §fAt ease, trainee. I've reviewed the full record of your "
                + "run through the Academy — movement discipline with Officer Cruz, fire response with Sergeant "
                + "Reyes, and your composure with Sergeant Santos. Let's see how you did.");
        // Full scoring + pass/fail verdict (AcademyScoring) added once the earlier rooms populate
        // their scoring inputs on AcademyProgress.
    }

    /** No-op — Room 4 has no ongoing per-tick state, only a one-shot evaluation on interact. */
    public static void tick(ServerLevel level) {
    }
}
