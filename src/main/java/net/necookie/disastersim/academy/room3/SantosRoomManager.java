package net.necookie.disastersim.academy.room3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyVisuals;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.player.DuckCoverHoldManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 3 — Sgt. Santos's Earthquake Drill. Gated on Room 2 ({@link ReyesPhase#DONE}).
 *
 * <p>The safe-zone table row is exactly the {@code TableBlock} run the user specified:
 * {@code (-170,-33,29)} to {@code (-167,-33,29)}. This room adds zero new duck/cover/hold logic —
 * it only triggers the quake event, keeps the row highlighted, and watches
 * {@link DuckCoverHoldManager}'s existing compliance state (plus a proximity check, so holding
 * under some *other* unrelated table elsewhere doesn't count).
 */
public final class SantosRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    private static final List<BlockPos> TABLE_ROW = List.of(
            new BlockPos(-170, -33, 29),
            new BlockPos(-169, -33, 29),
            new BlockPos(-168, -33, 29),
            new BlockPos(-167, -33, 29)
    );

    private static final int HIGHLIGHT_INTERVAL_TICKS = 5; // matches AssemblyZone's own cadence
    private static final int PRE_DRILL_DELAY_TICKS = 60;   // 3s from briefing-done to quake trigger
    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100;
    private static final float QUAKE_SHAKE_INTENSITY = 1.5f;
    private static final double NEAR_TABLE_RANGE_SQ = 3.0 * 3.0;

    private static final Map<UUID, Long> preDrillStartTick = new ConcurrentHashMap<>();

    private SantosRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        AcademyProgress progress = data.get(player.getUUID());

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
            if (next == SantosPhase.PRE_DRILL) {
                preDrillStartTick.put(player.getUUID(), level.getGameTime());
            }
        }
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            AcademyProgress progress = data.get(player.getUUID());
            if (progress.reyesPhase() != ReyesPhase.DONE) continue;
            switch (progress.santosPhase()) {
                case PRE_DRILL -> tickPreDrill(level, player, data);
                case QUAKE_ACTIVE -> tickQuakeActive(level, player, data);
                default -> { }
            }
        }
    }

    private static void tickPreDrill(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, TABLE_ROW);
        }
        UUID id = player.getUUID();
        long start = preDrillStartTick.getOrDefault(id, level.getGameTime());
        if (level.getGameTime() - start >= PRE_DRILL_DELAY_TICKS) {
            data.mutate(id, p -> p.setSantosPhase(SantosPhase.QUAKE_ACTIVE));
            preDrillStartTick.remove(id);
            AcademyManager.sendPrompt(player, "§c⚠ EARTHQUAKE! Drop, Cover, and Hold On — get under the table "
                    + "and brace yourself!", QUAKE_SHAKE_INTENSITY);
        }
    }

    private static void tickQuakeActive(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, TABLE_ROW);
        }

        UUID id = player.getUUID();
        boolean nearTable = isNearTableRow(player);

        if (nearTable && DuckCoverHoldManager.isCompliant(id)
                && DuckCoverHoldManager.ticksHeld(id) >= DuckCoverHoldManager.TARGET_TICKS) {
            data.mutate(id, p -> {
                p.setSantosPhase(SantosPhase.DONE);
                p.setQuakeCompliant(true);
            });
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §fThe shaking has stopped. Good composure out "
                    + "there — that's exactly how it's done. Proceed to Captain Morfe for your final debrief.", 0f);
            return;
        }

        if (!nearTable && level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7Get back under the table! "
                    + "The ground is still moving!", QUAKE_SHAKE_INTENSITY);
        }
    }

    private static boolean isNearTableRow(ServerPlayer player) {
        for (BlockPos pos : TABLE_ROW) {
            if (player.position().distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= NEAR_TABLE_RANGE_SQ) {
                return true;
            }
        }
        return false;
    }
}
