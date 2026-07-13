package net.necookie.disastersim.academy.room4;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyTelemetry;
import net.necookie.disastersim.academy.MorfePhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.academy.room1.CruzRoomManager;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.common.simulation.NewSimScoring;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.structure.LobbyManager;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.session.AuthManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
            AcademyTelemetry.record(player, "academy_certified", "score=" + result.score());
            // Persists to the logged-in account (if any) so a returning student's /login restores
            // certification without replaying the tutorial — see AuthManager/LobbyManager.
            AuthManager.markTutorialCompleted(player.getUUID());
            AcademyManager.startOrAdvanceDialogue(player, AcademyDialogue.MORFE_PASS_LINES, () -> deploySimAfterCountdown(player));
            return;
        }

        StringBuilder gaps = new StringBuilder();
        for (String area : result.weakAreas()) {
            if (!gaps.isEmpty()) gaps.append("; ");
            gaps.append(area);
        }
        player.sendSystemMessage(Component.literal("§eThings to practice: §f" + gaps));
        AcademyTelemetry.record(player, "academy_failed", "score=" + result.score() + ";weak=" + gaps);

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

    /**
     * On an Academy PASS, redirects the trainee into New Sim Building 2.0's graded fire scenario
     * (the exact path {@code /sim_fire new_sim_building2} uses) after a short countdown, instead of
     * just certifying them and stopping — the tutorial's real payoff is the graded run itself.
     * {@code SimulationManager.startSimulation} plays its own 3-2-1 warmup once this fires, so the
     * two countdowns chain rather than overlap.
     */
    private static void deploySimAfterCountdown(ServerPlayer player) {
        UUID uuid = player.getUUID();
        player.sendSystemMessage(Component.literal(
                "§e⏳ Excellent work — deploying you to New Sim Building 2.0 in 5 seconds. Good luck, trainee!"));
        TickScheduler.scheduleOnce(100, level -> {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
            if (p == null || !p.isAlive()) return;                 // logged out or died during the wait
            if (SimulationManager.getSession(uuid) != null) return; // already in a session — skip
            SimulationManager.startSimulation(p, SimulationManager.SimulationState.NEW_SIM_BUILDING2_FIRE);
        });
    }

    /**
     * Delivers an in-character debrief of a real graded {@code /sim_fire new_sim_building2} run —
     * narrates the score, never computes it (that's {@link NewSimScoring}). Fully independent of
     * {@link AcademyProgress}/{@link SantosPhase} gating: fires programmatically from
     * {@code SimulationManager.endSimulation} the instant that scenario ends, so it must work even
     * for a player who has never touched the Academy tutorial at all.
     */
    public static void startSimDebrief(ServerPlayer player, NewSimScoring.Result result, String endReason) {
        List<AcademyDialogue.DialogueLine> lines = new ArrayList<>();
        lines.add(new AcademyDialogue.DialogueLine(
                "§c[Capt. Morfe] §fWelcome back, trainee. I just got the report from New Sim Building 2.0 — let's go over it.", false));
        lines.add(new AcademyDialogue.DialogueLine(
                "§c[Capt. Morfe] §fYour score: §f" + result.score() + " / 100 §7(" + result.prepLevel() + ")", false));
        lines.add(new AcademyDialogue.DialogueLine(outcomeLine(endReason), false));
        if (!result.weakAreas().isEmpty()) {
            lines.add(new AcademyDialogue.DialogueLine(
                    "§c[Capt. Morfe] §fWork on: §f" + String.join("; ", result.weakAreas()), false));
        }
        lines.add(new AcademyDialogue.DialogueLine(
                result.passed()
                        ? "§c[Capt. Morfe] §fSolid work out there — that's the kind of response that keeps people safe. Head back and keep training!"
                        : "§c[Capt. Morfe] §fNot your best run, but every drill teaches something. Come back and try again when you're ready!",
                false));

        AcademyManager.forceStartDialogue(player, lines, () -> {
            ServerLevel level = (ServerLevel) player.level();
            player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                    Collections.emptySet(), 0.0f, 0.0f, true);
        });
    }

    private static String outcomeLine(String endReason) {
        return switch (endReason) {
            case "all_hazards_prevented" -> "§c[Capt. Morfe] §fEvery hazard caught before it ever became a fire — textbook prevention.";
            case "intervention_success" -> "§c[Capt. Morfe] §fA few hazards got past you, but you caught the fires before they spread. That's real composure under pressure.";
            case "assembly_reached" -> "§c[Capt. Morfe] §fThe fire got away from you, but you got everyone out and reached the assembly area. Evacuation always comes first.";
            default -> "§c[Capt. Morfe] §fThings got out of hand and you didn't make it to the assembly area in time. Let's talk about what happened.";
        };
    }
}
