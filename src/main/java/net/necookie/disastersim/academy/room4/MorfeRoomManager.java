package net.necookie.disastersim.academy.room4;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.Collections;

/**
 * Room 4 — Capt. Cesar Morfe Jr.'s Evaluation. Gated on Room 3 ({@link SantosPhase#DONE}). Unlike
 * the other three NPCs, Morfe has no static {@code AcademyDialogue} entry — his verdict is
 * score-dependent, computed fresh by {@link AcademyScoring} on every interaction.
 *
 * <p>On fail, resets the player's entire {@link AcademyProgress} (phases and scoring counters
 * alike, for a clean retry — no lifetime/historical tracking in this pass) and teleports them back
 * to Room 1's briefing zone.
 */
public final class MorfeRoomManager {

    /** Centre of Room 1's briefing zone — where a failed player is warped back to for a retry. */
    private static final double RETRY_X = -145.5;
    private static final double RETRY_Y = -33.0;
    private static final double RETRY_Z = 31.5;

    private MorfeRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        AcademyProgress progress = data.get(player.getUUID());

        if (progress.santosPhase() != SantosPhase.DONE) {
            AcademyManager.sendPrompt(player, "§c[Capt. Morfe] §7Complete the earthquake drill with Sgt. Santos first.");
            return;
        }

        AcademyManager.sendPrompt(player, "§c[Capt. Morfe] §fAt ease, trainee. I've reviewed the full record of "
                + "your run through the Academy — movement discipline with Officer Cruz, fire response with "
                + "Sergeant Reyes, and your composure with Sergeant Santos. Let's see how you did.");

        AcademyScoring.Result result = AcademyScoring.evaluate(progress);
        boolean passed = result.score() >= Config.ACADEMY_PASS_THRESHOLD.get();

        if (passed) {
            player.sendSystemMessage(Component.literal("§c[Capt. Morfe] §fYour movement control was sharp, your "
                    + "fire suppression was accurate, and you held your position through the earthquake drill "
                    + "without breaking. Congratulations, trainee — you are certified."));
            player.sendSystemMessage(Component.literal("§eScore: §f" + result.score() + " / 100"));
            player.sendSystemMessage(Component.literal("§c[Capt. Morfe] §fYou're cleared for the real "
                    + "simulations. Stay just as sharp out there."));
            data.mutate(player.getUUID(), p -> p.setMorfePhase(MorfePhase.EVALUATED_PASS));
            return;
        }

        StringBuilder gaps = new StringBuilder();
        for (String area : result.weakAreas()) {
            if (!gaps.isEmpty()) gaps.append("; ");
            gaps.append(area);
        }
        player.sendSystemMessage(Component.literal(
                "§c[Capt. Morfe] §cYour record shows some critical gaps under pressure: " + gaps + "."));
        player.sendSystemMessage(Component.literal("§eScore: §f" + result.score() + " / 100"));
        player.sendSystemMessage(Component.literal("§c[Capt. Morfe] §fThat's not a passing mark yet. Return to "
                + "Room 1 and run the drills again — everyone needs more than one pass sometimes. Dismissed."));

        data.mutate(player.getUUID(), AcademyProgress::resetAll);
        ReyesRoomManager.cleanupHazardProps(level);
        AcademyManager.cancelDialogue(player);
        player.teleportTo(level, RETRY_X, RETRY_Y, RETRY_Z,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);
    }

    /** No-op — Room 4 has no ongoing per-tick state, only a one-shot evaluation on interact. */
    public static void tick(ServerLevel level) {
    }
}
