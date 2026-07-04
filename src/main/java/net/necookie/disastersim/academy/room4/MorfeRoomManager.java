package net.necookie.disastersim.academy.room4;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room1.CruzRoomManager;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.Collections;

/**
 * Room 4 — Capt. Cesar Morfe Jr.'s Evaluation. Gated on Room 3 ({@link SantosPhase#DONE}).
 *
 * <p>Morfe's greeting, verdict, and retry send-off are static {@link AcademyDialogue} lines
 * ({@code MORFE_LINES}/{@code MORFE_PASS_LINES}/{@code MORFE_FAIL_LINES}) played through the
 * shared timed sequencer, just like the other three instructors — only the score number and the
 * weak-areas list are built at runtime by {@link AcademyScoring}.
 *
 * <p>On fail, the reset (full {@link AcademyProgress} wipe, Room 2 hazard-prop cleanup, teleport
 * back to Room 1's briefing zone) fires from the fail sequence's {@code onComplete} — i.e. only
 * once the player has actually read Morfe's send-off where they stand, not mid-sentence.
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
            AcademyManager.sendPrompt(player, "§c[Capt. Morfe] §7Almost there, trainee! Complete Sgt. Santos's "
                    + "earthquake drill first — then come see me for your results.");
            return;
        }

        if (progress.morfePhase() == MorfePhase.EVALUATED_PASS) {
            AcademyManager.startOrAdvanceDialogue(player,
                    AcademyDialogue.MORFE_LINES.get(MorfePhase.EVALUATED_PASS), () -> { });
            return;
        }

        AcademyManager.startOrAdvanceDialogue(player,
                AcademyDialogue.MORFE_LINES.get(MorfePhase.NOT_STARTED),
                () -> runEvaluation(level, player, data));
    }

    private static void runEvaluation(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        AcademyProgress progress = data.get(player.getUUID());
        AcademyScoring.Result result = AcademyScoring.evaluate(progress);
        boolean passed = result.score() >= Config.ACADEMY_PASS_THRESHOLD.get();
        player.sendSystemMessage(Component.literal("§eYour Academy score: §f" + result.score() + " / 100"));

        if (passed) {
            data.mutate(player.getUUID(), p -> p.setMorfePhase(MorfePhase.EVALUATED_PASS));
            AcademyManager.startOrAdvanceDialogue(player, AcademyDialogue.MORFE_PASS_LINES, () -> { });
            return;
        }

        StringBuilder gaps = new StringBuilder();
        for (String area : result.weakAreas()) {
            if (!gaps.isEmpty()) gaps.append("; ");
            gaps.append(area);
        }
        player.sendSystemMessage(Component.literal("§eThings to practice: §f" + gaps));

        AcademyManager.startOrAdvanceDialogue(player, AcademyDialogue.MORFE_FAIL_LINES, () -> {
            data.mutate(player.getUUID(), AcademyProgress::resetAll);
            ReyesRoomManager.cleanupHazardProps(level);
            ReyesRoomManager.restockExtinguisherFrames(level);
            CruzRoomManager.resetCruz(level);
            player.teleportTo(level, RETRY_X, RETRY_Y, RETRY_Z,
                    Collections.emptySet(), player.getYRot(), player.getXRot(), true);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fWelcome back for round two, trainee! "
                    + "Same as before — walk onto the four green tiles. You'll fly through it this time!");
        });
    }

    /** No-op — Room 4 has no ongoing per-tick state, only the interact-driven evaluation flow. */
    public static void tick(ServerLevel level) {
    }
}
