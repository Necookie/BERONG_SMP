package net.necookie.disastersim.academy.room1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 1 — Officer Cruz's Movement School. Two {@code CustomNpcEntity} instances share
 * {@code NpcType.OFFICER_CRUZ} in the schematic (one at the briefing start, one at the Go/Stop
 * tunnel finish); both route through {@link #onInteract} and are shown whatever line list matches
 * the player's *current* phase, so which physical NPC was clicked doesn't matter — a player can't
 * legitimately reach the finish-line NPC before finishing the phases in between.
 *
 * <p>Zone boxes below are the exact boxes from the user's blueprint. The 4 green floor marks
 * (Phase 1) are placeholder positions spread inside that box — the user described them without
 * exact F3 readings; see {@code docs/f3_tuning_todo.md}. The maze/jump zones only gate on reaching
 * the next zone's box, without per-obstacle jump enforcement, for the same reason (exact wall/
 * obstacle placement isn't F3-verified yet) — matches the old tutorial's generally forgiving style
 * of not punishing imperfect technique on non-critical steps.
 */
public final class CruzRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    private static final AABB BRIEFING_ZONE = new AABB(-154, -34, 38, -137, -30, 25);
    private static final AABB MAZE_ZONE     = new AABB(-135, -34, 38, -122, -30, 25);
    private static final AABB JUMP_ZONE     = new AABB(-122, -34, 38, -106, -30, 25);

    /** PLACEHOLDER — needs in-game F3 verification, see docs/f3_tuning_todo.md. */
    private static final List<BlockPos> GREEN_MARKS = List.of(
            new BlockPos(-150, -33, 34),
            new BlockPos(-146, -33, 30),
            new BlockPos(-142, -33, 34),
            new BlockPos(-139, -33, 30)
    );
    private static final double MARK_RADIUS_SQ = 1.5 * 1.5;

    /** Warp-back target for a Go/Stop violation — the staging line's midpoint. */
    private static final Vec3 STAGING_POS = new Vec3(-103.5, -33.0, 51.0);
    /** Far end of the Go/Stop corridor — reaching this while compliant finishes Room 1. */
    private static final double GOSTOP_FINISH_X = -118.0;

    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100; // 5s, matches TutorialManager's idiom
    private static final int GOSTOP_MIN_INTERVAL_TICKS = 60;  // 3s
    private static final int GOSTOP_MAX_INTERVAL_TICKS = 120; // 6s
    private static final double GOSTOP_MOVE_EPSILON_SQ = 0.35 * 0.35;

    private static final Map<UUID, Set<Integer>> marksHit = new ConcurrentHashMap<>();
    private static final Map<UUID, GoStopState> goStopStates = new ConcurrentHashMap<>();

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
        if (next == phase) return;

        data.mutate(player.getUUID(), p -> p.setCruzPhase(next));
        AcademyManager.resetDialogueStep(dialogueSteps, player);
        if (next == CruzPhase.GOSTOP_RUN) {
            startGoStop(level, player);
        }
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            CruzPhase phase = data.get(player.getUUID()).cruzPhase();
            switch (phase) {
                case BRIEFING -> tickBriefing(level, player, data);
                case MAZE -> tickMaze(level, player, data);
                case JUMP -> tickJump(level, player, data);
                case GOSTOP_RUN -> tickGoStopRun(level, player, data);
                default -> { }
            }
        }
    }

    private static void tickBriefing(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        Set<Integer> hits = marksHit.computeIfAbsent(id, k -> new HashSet<>());
        Vec3 pos = player.position();
        for (int i = 0; i < GREEN_MARKS.size(); i++) {
            if (hits.contains(i)) continue;
            BlockPos mark = GREEN_MARKS.get(i);
            if (pos.distanceToSqr(mark.getX() + 0.5, mark.getY(), mark.getZ() + 0.5) <= MARK_RADIUS_SQ) {
                hits.add(i);
            }
        }
        if (hits.size() >= GREEN_MARKS.size()) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.MAZE));
            marksHit.remove(id);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fNice work! Now let's put those turns to use. "
                    + "Follow the green arrows through this maze — steer your camera, not just forward!");
            return;
        }
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §7Still looking for the marks? "
                    + "They're glowing green on the floor!");
        }
    }

    private static void tickMaze(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        if (JUMP_ZONE.contains(player.position())) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.JUMP));
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fObstacles ahead! Keep walking forward and "
                    + "hit Spacebar to hop right over them. Don't stop moving!");
            return;
        }
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §7Bumping into walls slows you down! "
                    + "Look at the green arrows and turn your camera before your feet.");
        }
    }

    private static void tickJump(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        if (!JUMP_ZONE.contains(player.position())) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.GOSTOP_STAGE));
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fGreat job clearing the obstacles! "
                    + "Come find me at the tunnel entrance for the last lesson.");
            return;
        }
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §7Keep your momentum! "
                    + "Walk straight at it and tap Spacebar right before you'd hit it.");
        }
    }

    // -----------------------------------------------------------------------
    // Go / Stop mini-game
    // -----------------------------------------------------------------------

    private static final class GoStopState {
        boolean isGo = true;
        long nextFlipTick;
        long stopStartTick = -1;
        Vec3 posAtStop = Vec3.ZERO;
    }

    private static void startGoStop(ServerLevel level, ServerPlayer player) {
        GoStopState state = new GoStopState();
        state.nextFlipTick = level.getGameTime() + randomInterval(level);
        goStopStates.put(player.getUUID(), state);
        AcademyManager.sendPrompt(player, "§a▶ GO! Keep moving forward!");
    }

    private static long randomInterval(ServerLevel level) {
        int span = GOSTOP_MAX_INTERVAL_TICKS - GOSTOP_MIN_INTERVAL_TICKS + 1;
        return GOSTOP_MIN_INTERVAL_TICKS + level.getRandom().nextInt(span);
    }

    private static void tickGoStopRun(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        GoStopState state = goStopStates.get(id);
        if (state == null) {
            startGoStop(level, player);
            return;
        }

        long gameTime = level.getGameTime();

        if (gameTime >= state.nextFlipTick) {
            state.isGo = !state.isGo;
            state.nextFlipTick = gameTime + randomInterval(level);
            if (state.isGo) {
                state.stopStartTick = -1;
                AcademyManager.sendPrompt(player, "§a▶ GO! Keep moving forward!");
            } else {
                state.stopStartTick = gameTime;
                state.posAtStop = player.position();
                AcademyManager.sendPrompt(player, "§c■ STOP! Freeze right there!");
            }
        }

        if (!state.isGo && state.stopStartTick >= 0) {
            long grace = Config.ACADEMY_GOSTOP_GRACE_TICKS.get();
            if (gameTime - state.stopStartTick > grace
                    && player.position().distanceToSqr(state.posAtStop) > GOSTOP_MOVE_EPSILON_SQ) {
                player.teleportTo(level, STAGING_POS.x, STAGING_POS.y, STAGING_POS.z,
                        Collections.emptySet(), player.getYRot(), player.getXRot(), true);
                data.mutate(id, p -> { p.setCruzPhase(CruzPhase.GOSTOP_STAGE); p.addMovementMistake(); });
                goStopStates.remove(id);
                AcademyManager.resetDialogueStep(dialogueSteps, player);
                AcademyManager.sendPrompt(player, "§c[Officer Cruz] §7Whoa — you moved after STOP! Back to the "
                        + "staging line. Watch for my call and freeze the instant you hear it.");
                return;
            }
        }

        if (player.getX() <= GOSTOP_FINISH_X) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.DONE));
            goStopStates.remove(id);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fOutstanding! You've got sharp eyes, steady feet, "
                    + "and you know how to freeze on command. Follow the green floor arrows to Sgt. Reyes "
                    + "for your Fire Safety Drill!");
        }
    }
}
