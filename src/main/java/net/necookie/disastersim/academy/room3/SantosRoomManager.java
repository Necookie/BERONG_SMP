package net.necookie.disastersim.academy.room3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyVisuals;
import net.necookie.disastersim.academy.MorfePhase;
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

    private static final List<BlockPos> TABLE_ROW = List.of(
            new BlockPos(-170, -33, 29),
            new BlockPos(-169, -33, 29),
            new BlockPos(-168, -33, 29),
            new BlockPos(-167, -33, 29)
    );

    private static final int HIGHLIGHT_INTERVAL_TICKS = 5; // matches AssemblyZone's own cadence
    private static final int SECOND_TICKS = 20;
    private static final int PRE_DRILL_DELAY_TICKS = 60;   // 3s from briefing-done to quake trigger
    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100;
    private static final float QUAKE_SHAKE_INTENSITY = 1.5f;
    /** Capt. Morfe's anchor — points there until the player has actually started Room 4. */
    private static final Vec3 MORFE_ANCHOR = new Vec3(-108.5, -33.0, 77.5);
    /** Center of the table row, for the compass needle — the beacon alone had no compass pairing. */
    private static final Vec3 TABLE_ROW_CENTER = new Vec3(-168.5, -33.0, 29.5);

    private static final Map<UUID, Long> preDrillStartTick = new ConcurrentHashMap<>();
    /**
     * This room's own table-scoped compliance streak (ticks), separate from
     * {@link DuckCoverHoldManager}'s global crouch+cover streak. The global streak accumulates
     * anywhere in the world regardless of location, so reading it directly here let a player build
     * the whole streak elsewhere and merely step within {@link #isNearTableRow}'s range for a single
     * tick to pass instantly. This counter only ever grows on ticks where the player is genuinely
     * near {@link #TABLE_ROW} *and* {@link DuckCoverHoldManager#isCompliant} is true that tick; any
     * other tick resets it to zero.
     */
    private static final Map<UUID, Integer> tableHoldTicks = new ConcurrentHashMap<>();

    private SantosRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        AcademyProgress progress = data.get(player.getUUID());

        if (progress.reyesPhase() != ReyesPhase.DONE) {
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7Hello trainee! Finish the fire drill with "
                    + "Sgt. Reyes first — she's just next door. Come right back after!");
            return;
        }

        SantosPhase phase = progress.santosPhase();
        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.SANTOS_LINES.get(phase);
        AcademyManager.startOrAdvanceDialogue(player, lines, () -> {
            SantosPhase next = switch (phase) {
                case NOT_STARTED -> SantosPhase.PRE_DRILL;
                default -> phase; // condition-gated, not dialogue-gated
            };
            if (next != phase) {
                data.mutate(player.getUUID(), p -> p.setSantosPhase(next));
                if (next == SantosPhase.PRE_DRILL) {
                    preDrillStartTick.put(player.getUUID(), level.getGameTime());
                }
            }
        });
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            AcademyProgress progress = data.get(player.getUUID());
            if (progress.reyesPhase() != ReyesPhase.DONE) continue;
            switch (progress.santosPhase()) {
                case PRE_DRILL -> tickPreDrill(level, player, data);
                case QUAKE_ACTIVE -> tickQuakeActive(level, player, data);
                case DONE -> tickDone(level, player, data);
                default -> { }
            }
        }
    }

    private static void tickPreDrill(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, TABLE_ROW);
        }
        AcademyVisuals.setCompassTarget(player, TABLE_ROW_CENTER);
        UUID id = player.getUUID();
        long start = preDrillStartTick.computeIfAbsent(id, k -> level.getGameTime());
        if (level.getGameTime() - start >= PRE_DRILL_DELAY_TICKS) {
            data.mutate(id, p -> p.setSantosPhase(SantosPhase.QUAKE_ACTIVE));
            preDrillStartTick.remove(id);
            AcademyManager.sendPrompt(player, "§c⚠ EARTHQUAKE! §fWalk to the glowing table (hold §eW§f), get "
                    + "under it, and press and hold §eShift§f — DROP, COVER, HOLD ON!", QUAKE_SHAKE_INTENSITY);
        }
    }

    private static void tickQuakeActive(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, TABLE_ROW);
        }
        AcademyVisuals.setCompassTarget(player, TABLE_ROW_CENTER);

        UUID id = player.getUUID();
        boolean nearTable = isNearTableRow(player);

        if (nearTable && DuckCoverHoldManager.isCompliant(id)) {
            int held = tableHoldTicks.merge(id, 1, Integer::sum);
            if (held >= DuckCoverHoldManager.TARGET_TICKS) {
                tableHoldTicks.remove(id);
                data.mutate(id, p -> {
                    p.setSantosPhase(SantosPhase.DONE);
                    p.setQuakeCompliant(true);
                });
                AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §fAnd... the shaking has stopped. You dropped, "
                        + "covered, and held on like a pro! One last stop: follow the glowing arrow to "
                        + "§cCaptain Morfe§f for your results. Stand tall — you've earned it.", 0f);
                return;
            }
            // Live countdown once genuinely under the table and holding — confirms the detection
            // actually fired and gives a clear sense of the 5-second requirement counting down.
            // The shake intensity FADES with progress (mirroring the old tutorial's QUAKE_HOLDON,
            // 1.5 → 0 across the hold) instead of being zeroed outright: sending 0f here was the
            // bug that made the earthquake visually stop the instant the player started holding,
            // seconds before the drill was actually complete. Only the completion message above
            // truly ends the shaking.
            if (held % SECOND_TICKS == 0) {
                int secondsLeft = (DuckCoverHoldManager.TARGET_TICKS - held) / SECOND_TICKS;
                float fading = QUAKE_SHAKE_INTENSITY
                        * (1.0f - (float) held / DuckCoverHoldManager.TARGET_TICKS);
                AcademyManager.sendPrompt(player, "§a✔ Under cover — hold there... §f" + secondsLeft + "s", fading);
            }
            return;
        }
        // Cover broken (or never established): reset the hold and put the shaking back at full
        // strength — breaking cover mid-hold restarts the 5 seconds, exactly like the old
        // tutorial's QUAKE_HOLDON break-cover reset.
        if (tableHoldTicks.remove(id) != null) {
            AcademyManager.sendPrompt(player, "§c✗ You left cover — get back under the table and hold on!",
                    QUAKE_SHAKE_INTENSITY);
        }

        if (nearTable && level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0
                && !AcademyManager.isDialogueActive(id)) {
            // At the table but not crouched/covered — remind them of the missing half, keeping the
            // quake shaking at full strength the whole time.
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7Almost! Now press and hold §eShift§7 to "
                    + "crouch under the table — and stay put!", QUAKE_SHAKE_INTENSITY);
            return;
        }

        if (!nearTable && level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0
                && !AcademyManager.isDialogueActive(id)) {
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§6[Sgt. Santos] §7Get under the glowing table! Hold §eW§7 to walk there, then "
                            + "press and hold §eShift§7 to crouch — and stay put!",
                    "§6[Sgt. Santos] §7The table is your safe spot — hurry back under it and hold "
                            + "§eShift§7 until the shaking stops!",
                    "§6[Sgt. Santos] §7Stay low, stay covered! Under the glowing table, hold "
                            + "§eShift§7, and don't let go!"), QUAKE_SHAKE_INTENSITY);
        }
    }

    /** Points toward Capt. Morfe until the player has actually started Room 4. */
    private static void tickDone(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (data.get(player.getUUID()).morfePhase() != MorfePhase.NOT_STARTED) {
            AcademyVisuals.setCompassTarget(player, null);
            return;
        }
        AcademyVisuals.setCompassTarget(player, MORFE_ANCHOR);
    }

    /**
     * True only when the player is actually at/under one of {@link #TABLE_ROW}'s cells — a
     * 1-block XZ tolerance (matching {@code DuckCoverHoldManager.hasNearbyTable}'s own radius
     * exactly), not the previous 3-block sphere, which let a player pass the drill just by
     * standing somewhere nearby in the open room without ever actually taking cover under the
     * table.
     */
    private static boolean isNearTableRow(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : TABLE_ROW) {
            if (Math.abs(feet.getX() - pos.getX()) <= 1
                    && Math.abs(feet.getZ() - pos.getZ()) <= 1
                    && Math.abs(feet.getY() - pos.getY()) <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called from {@code AcademyManager}'s logout handler (and {@code /bfp new_tutorial reset}) to
     * drop this room's per-player state. If the player quit mid-drill (PRE_DRILL or QUAKE_ACTIVE),
     * also rolls their phase back to NOT_STARTED: {@code preDrillStartTick} is gone the instant it's
     * removed below, but the persisted phase alone is enough for {@link #tick} to re-enter
     * {@link #tickQuakeActive}/{@link #tickPreDrill} on the very next tick after they reconnect —
     * with no dialogue re-triggered, no clear reason given, and shake prompts firing again as if the
     * earthquake had simply kept running while they were offline.
     */
    public static void clearPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        preDrillStartTick.remove(id);
        tableHoldTicks.remove(id);

        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        SantosPhase phase = data.get(id).santosPhase();
        if (phase == SantosPhase.PRE_DRILL || phase == SantosPhase.QUAKE_ACTIVE) {
            data.mutate(id, p -> p.setSantosPhase(SantosPhase.NOT_STARTED));
        }
    }
}
