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
 * {@code (-170,-33,29)} to {@code (-167,-33,29)}.
 *
 * <p><b>Detection is this room's own, not {@code DuckCoverHoldManager}'s</b> (which is still used
 * elsewhere for the live-simulation drill): its {@code isCrouching()}-based compliance check
 * actually <em>fails</em> a player who has genuinely crawled into the table's kneehole, because
 * vanilla forces {@code Pose.SWIMMING} under the sub-1-block clearance and {@code isCrouching()}
 * only returns true for {@code Pose.CROUCHING} — the exact player doing the drill correctly was
 * the one who could never complete it. Here, "under the table" means the player's feet are
 * literally inside one of {@link #TABLE_ROW}'s block cells ({@link #isUnderTable}), and "ducking"
 * means the sneak key is held (or a crouch pose, for completeness) — see {@link #isDucking}. The
 * quake never stops until both hold for {@link #HOLD_REQUIRED_TICKS} <em>consecutive</em> ticks;
 * any tick either is false resets the countdown to zero.
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
    /** Consecutive ticks the player must stay ducked under the table (5s). */
    private static final int HOLD_REQUIRED_TICKS = 100;
    /**
     * The quake only actually starts once the player is within this XZ distance of the table row —
     * a player who clicked Santos and then wandered off mid-briefing used to get hit by a
     * seemingly random earthquake wherever they happened to be standing once the fixed 3s timer
     * elapsed. Now the timer holds until they're genuinely back in Room 3 for the drill.
     */
    private static final double QUAKE_START_RANGE = 20.0;
    /** Capt. Morfe's anchor — points there until the player has actually started Room 4. */
    private static final Vec3 MORFE_ANCHOR = new Vec3(-108.5, -33.0, 77.5);
    /** Center of the table row, for the compass needle — the beacon alone had no compass pairing. */
    private static final Vec3 TABLE_ROW_CENTER = new Vec3(-168.5, -33.0, 29.5);

    private static final Map<UUID, Long> preDrillStartTick = new ConcurrentHashMap<>();
    /**
     * This room's own table-scoped hold streak (ticks), fully independent of
     * {@link DuckCoverHoldManager}'s global crouch+cover streak (whose {@code isCompliant} both
     * accumulates anywhere in the world AND fails a player crawled into the kneehole — see the
     * class javadoc). Only grows on ticks where {@link #isUnderTable} and {@link #isDucking} are
     * both true; any other tick resets it to zero, so only 5 <em>consecutive</em> seconds under
     * Santos's table ever completes the drill.
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
        // The drill only fires with the player actually present in Room 3 — the delay elapsing
        // while they wandered off used to start a seemingly random earthquake wherever they were.
        if (!player.position().closerThan(TABLE_ROW_CENTER, QUAKE_START_RANGE)) {
            return;
        }
        if (level.getGameTime() - start >= PRE_DRILL_DELAY_TICKS) {
            data.mutate(id, p -> p.setSantosPhase(SantosPhase.QUAKE_ACTIVE));
            preDrillStartTick.remove(id);
            AcademyManager.sendPrompt(player, "§c⚠ EARTHQUAKE! §fWalk to the glowing table (hold §eW§f), get "
                    + "under it, and press and hold §eShift§f — DROP, COVER, HOLD ON!");
        }
    }

    private static void tickQuakeActive(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, TABLE_ROW);
        }
        AcademyVisuals.setCompassTarget(player, TABLE_ROW_CENTER);

        UUID id = player.getUUID();
        boolean underTable = isUnderTable(player);
        boolean ducking = isDucking(player);

        if (underTable && ducking) {
            int held = tableHoldTicks.merge(id, 1, Integer::sum);
            if (held >= HOLD_REQUIRED_TICKS) {
                tableHoldTicks.remove(id);
                data.mutate(id, p -> {
                    p.setSantosPhase(SantosPhase.DONE);
                    p.setQuakeCompliant(true);
                });
                AcademyVisuals.setShake(player, 0f);
                AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §fAnd... the shaking has stopped. You dropped, "
                        + "covered, and held on like a pro! One last stop: follow the glowing arrow to "
                        + "§cCaptain Morfe§f for your results. Stand tall — you've earned it.");
                return;
            }
            // The shake intensity FADES with hold progress (mirroring the old tutorial's
            // QUAKE_HOLDON, 1.5 → 0 across the 5 seconds), re-asserted every single tick over the
            // dedicated shake channel — no caption anywhere can interrupt or stop it. Only the
            // completion branch above truly ends the shaking.
            AcademyVisuals.setShake(player, QUAKE_SHAKE_INTENSITY
                    * (1.0f - (float) held / HOLD_REQUIRED_TICKS));
            // Live countdown once genuinely under the table and holding — confirms the detection
            // actually fired and gives a clear sense of the 5-second requirement counting down.
            if (held % SECOND_TICKS == 0) {
                int secondsLeft = (HOLD_REQUIRED_TICKS - held) / SECOND_TICKS;
                AcademyManager.sendPrompt(player, "§a✔ Under cover — hold there... §f" + secondsLeft + "s");
            }
            return;
        }
        // Not holding: the quake shakes at FULL strength, asserted every tick — it never stops on
        // its own, no matter where the player goes or what captions come and go. The only way out
        // is the 5-consecutive-second hold above.
        AcademyVisuals.setShake(player, QUAKE_SHAKE_INTENSITY);

        // Cover broken: reset the hold — breaking cover mid-hold restarts the full 5 seconds,
        // exactly like the old tutorial's QUAKE_HOLDON break-cover reset.
        if (tableHoldTicks.remove(id) != null) {
            AcademyManager.sendPrompt(player, "§c✗ You left cover — the countdown reset! Get back under "
                    + "the table, hold §eShift§f, and stay put for the full 5 seconds.");
        }

        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS != 0 || AcademyManager.isDialogueActive(id)) {
            return;
        }
        if (underTable) {
            // Genuinely inside the kneehole but not holding sneak — the only missing half.
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7You're under the table — now press and "
                    + "HOLD §eShift§7, and don't let go for 5 whole seconds!");
        } else if (isNearTableRow(player)) {
            // Beside the table but not actually underneath it — being near is NOT cover.
            AcademyManager.sendPrompt(player, "§6[Sgt. Santos] §7Almost! Being beside the table isn't "
                    + "cover — hold §eShift§7 and walk INTO it to crawl underneath, then stay put!");
        } else {
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§6[Sgt. Santos] §7Get under the glowing table! Hold §eW§7 to walk there, then "
                            + "press and hold §eShift§7 to crawl underneath — and stay put!",
                    "§6[Sgt. Santos] §7The table is your safe spot — hurry back under it and hold "
                            + "§eShift§7 until the shaking stops!",
                    "§6[Sgt. Santos] §7Stay low, stay covered! Under the glowing table, hold "
                            + "§eShift§7, and don't let go!"));
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
     * True only when the player's feet are literally inside one of {@link #TABLE_ROW}'s block
     * cells — the same cell the tabletop occupies, reachable via the crawl-under assist
     * ({@code DuckCoverHoldManager.allowCrawlUnderTable}). This is the strict "actually under the
     * table" check that earns countdown progress; crouching beside the table (which the previous
     * ±1-block tolerance accepted) no longer counts.
     */
    private static boolean isUnderTable(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : TABLE_ROW) {
            if (feet.getX() == pos.getX() && feet.getZ() == pos.getZ()
                    && Math.abs(feet.getY() - pos.getY()) <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the player is deliberately staying low. {@code isCrouching()} alone is WRONG under
     * the table: the kneehole's sub-1-block clearance forces vanilla's {@code Pose.SWIMMING}
     * (crawl), and {@code isCrouching()} only returns true for {@code Pose.CROUCHING} — so the old
     * {@code DuckCoverHoldManager.isCompliant}-based gate failed exactly the player who had
     * correctly crawled underneath. The sneak key ({@code isShiftKeyDown()}) stays true while
     * crawling, so it's the primary signal; the pose check is kept for any edge case where the
     * player is in a crouch pose without the key (e.g. forced by a low ceiling elsewhere).
     */
    private static boolean isDucking(ServerPlayer player) {
        return player.isShiftKeyDown() || player.isCrouching();
    }

    /**
     * Looser 1-block-tolerance proximity to the row (matching
     * {@code DuckCoverHoldManager.hasNearbyTable}'s radius) — used only to pick the right coaching
     * nudge ("you're beside it, crawl IN"), never to award countdown progress; that requires the
     * strict {@link #isUnderTable}.
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
        // Stop the shake explicitly: on the /bfp reset path the player stays connected, and the
        // phase rollback below silently ends the per-tick shake assertion — without this, the
        // client would keep shaking with its last-received intensity forever. (On logout this is a
        // harmless no-op send; the client resets its own HUD statics via ClientEvents anyway.)
        AcademyVisuals.setShake(player, 0f);

        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        SantosPhase phase = data.get(id).santosPhase();
        if (phase == SantosPhase.PRE_DRILL || phase == SantosPhase.QUAKE_ACTIVE) {
            data.mutate(id, p -> p.setSantosPhase(SantosPhase.NOT_STARTED));
        }
    }
}
