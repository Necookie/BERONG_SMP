package net.necookie.disastersim.academy.room1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyTelemetry;
import net.necookie.disastersim.academy.AcademyVisuals;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 1 — Officer Cruz's Movement School. A single {@code CustomNpcEntity} (the schematic's
 * duplicate near the old Go/Stop tunnel finish is discarded by
 * {@code world.AcademyBuildingManager}) physically escorts the player through Briefing/Maze/Jump
 * via real vanilla pathfinding (see {@link #updateCruzEscort}), then waits at the Go/Stop staging
 * line and calls out GO/STOP and the finish line from there — her fixed hitbox can't crouch
 * through the tunnel's low slabs the way a player can, so she supervises the last phase rather
 * than walking it (see the plan notes on this decision).
 *
 * <p>Zone boxes below are the exact boxes from the user's blueprint. The 4 green floor marks
 * (Phase 1) are placeholder positions spread inside that box — the user described them without
 * exact F3 readings; see {@code docs/f3_tuning_todo.md}. The maze/jump zones only gate on reaching
 * the next zone's box, without per-obstacle jump enforcement, for the same reason (exact wall/
 * obstacle placement isn't F3-verified yet) — matches the old tutorial's generally forgiving style
 * of not punishing imperfect technique on non-critical steps.
 */
public final class CruzRoomManager {

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
    /**
     * The staging pen the player must physically enter before Cruz will even start the Go/Stop
     * briefing — schem-verified: this is the pen's actual open interior (X -102..-98, Z 47..55),
     * one block inside the entrance gap at X=-103.
     */
    private static final AABB GOSTOP_ZONE = new AABB(-102, -34, 47, -97, -30, 55);
    /**
     * The starting line at the pen's entrance gap (X=-103, schem-verified against the flanking wall
     * blocks at Z=46/56 — the actual opening is Z 47..55). Nothing stops the player from crossing
     * this before Cruz calls the first GO; {@link #tickGoStopStage} pushes them back east if they try.
     */
    private static final double GOSTOP_STARTING_LINE_X = -103.0;
    private static final double GOSTOP_STARTING_LINE_MIN_Z = 46.0;
    private static final double GOSTOP_STARTING_LINE_MAX_Z = 56.0;
    /**
     * The exact block just inside the pen, right beside Cruz's {@link #STAGING_POS} waiting spot,
     * where the player should stand to talk to her. Highlighted the same way as
     * {@link #GREEN_MARKS} so the {@link #GOSTOP_STARTING_LINE_X} barrier reads as "stand on the
     * green tile" instead of an unexplained invisible wall.
     */
    private static final BlockPos GOSTOP_STAGE_MARK = new BlockPos(-102, -33, 51);
    /**
     * The crouch-tunnel corridor, schem-verified: the three low-ceiling slab lanes sit at X=-106/
     * -107, -110, and -114/-115 (each spanning the full Z 40..62 corridor width), so this covers a
     * margin around all of them plus the open runs between. Cruz shrinks to
     * {@code CustomNpcEntity.CROUCH_DIMENSIONS} whenever she's inside it.
     */
    private static final AABB GOSTOP_TUNNEL_ZONE = new AABB(-117, -34, 39, -103, -30, 63);
    /** Throttle for the "wait for GO" barrier nudge, same idiom as the too-far-chase nudge. */
    private static final long STARTING_LINE_NUDGE_COOLDOWN_TICKS = 60; // 3s

    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100; // 5s, matches TutorialManager's idiom
    private static final int SECOND_TICKS = 20;
    private static final int GOSTOP_MIN_INTERVAL_TICKS = 60;  // 3s
    private static final int GOSTOP_MAX_INTERVAL_TICKS = 120; // 6s
    private static final double GOSTOP_MOVE_EPSILON_SQ = 0.35 * 0.35;
    private static final int HIGHLIGHT_INTERVAL_TICKS = 5; // matches AssemblyZone's/Santos's cadence

    private static final Vec3 MAZE_EXIT_POINT = new Vec3(-121.5, -33.0, 31.5);
    private static final Vec3 JUMP_EXIT_POINT = new Vec3(-105.5, -33.0, 31.5);
    /** East edge of the jump zone — the hurdles' finish line (last hurdle row is at X=-111). */
    private static final double JUMP_FINISH_X = -106.0;

    /**
     * Schem-verified route through the maze's serpentine (walls at X=-131 open only at Z=25,
     * X=-127 open only at Z=38, X=-124 open only at Z=25). The compass and Cruz's escort both
     * target the next unpassed waypoint rather than the far exit, so neither points/paths
     * straight into a wall.
     */
    private static final List<Vec3> MAZE_WAYPOINTS = List.of(
            new Vec3(-131.5, -33.0, 25.5),
            new Vec3(-127.5, -33.0, 38.5),
            new Vec3(-124.5, -33.0, 25.5),
            MAZE_EXIT_POINT
    );

    /** One waypoint just past each 1-block hurdle row (X=-117, -114, -111), then the exit. */
    private static final List<Vec3> JUMP_WAYPOINTS = List.of(
            new Vec3(-116.5, -33.0, 31.5),
            new Vec3(-113.5, -33.0, 31.5),
            new Vec3(-110.5, -33.0, 31.5),
            JUMP_EXIT_POINT
    );

    private static final double WAYPOINT_ADVANCE_RANGE_SQ = 2.0 * 2.0;

    /** Per-player progress along the current phase's waypoint chain; reset on phase transitions. */
    private static final Map<UUID, Integer> waypointIdx = new ConcurrentHashMap<>();
    /** Sgt. Reyes's anchor — where Room 1 points once it's done and Room 2 hasn't started yet. */
    private static final Vec3 REYES_ANCHOR = new Vec3(-172.5, -33.0, 17.5);

    private static final Map<UUID, Set<Integer>> marksHit = new ConcurrentHashMap<>();
    private static final Map<UUID, GoStopState> goStopStates = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Cruz escort — cached entity reference + periodic pathfinding re-issue
    // -----------------------------------------------------------------------

    private static final Vec3 BRIEFING_ANCHOR = new Vec3(-153.5, -33.0, 32.5);
    /**
     * Room 1's full footprint (briefing + maze + jump + Go/Stop tunnel, with margin). Cruz must
     * never end up outside this — it's a beginner tutorial and she has to stay reachable. If she
     * somehow escapes (bad path, shove, glitch), the next escort cycle poof-recovers her.
     *
     * <p>Widened 6 blocks past the original briefing-room wall (X -156 → -162), verified directly
     * against the schematic's block data: the only real wall gap between Room 1's open floor and
     * Sgt. Reyes's walled hazard room sits at relative X=15 (world X=-162), spanning the doorway
     * Z-range the briefing anchor already lines up with. This lets Cruz's "still escorting" check
     * (see {@link #isEscortablePhase}/{@code CruzPhase#DONE} handling in {@link #tick}) cover the
     * open corridor right up to that doorway threshold — she escorts the player that far, then
     * stops the instant they actually step through into Reyes's room, instead of cutting off deep
     * inside Room 1 the moment the Go/Stop finish line is crossed (the old boundary, which was
     * ~17 blocks short of Reyes's room and the actual cause of her "instantly banishing" herself
     * mid-room instead of walking the player to the door first).
     */
    private static final AABB ROOM1_BOUNDS = new AABB(-162, -36, 23, -95, -28, 64);
    private static final int ESCORT_MOVE_INTERVAL_TICKS = 15;
    /**
     * Consecutive no-progress re-issue cycles before poof-recovery (6 × {@link
     * #ESCORT_MOVE_INTERVAL_TICKS} = 90 ticks ≈ 4.5s) — generous enough that the door hand-off
     * leg's longer walk across the open corridor shouldn't spuriously trip a poof, which would look
     * exactly like the "instant banishing" this system is meant to avoid. A provably unreachable
     * target (see {@link #UNREACHABLE_STUCK_STEP}) recovers much faster than this full budget.
     */
    private static final int ESCORT_STUCK_CYCLES_MAX = 6;
    /**
     * Purely diagnostic threshold for {@link #tickReturnHome}'s walk back to
     * {@link #BRIEFING_ANCHOR} (~30s at the 15-tick cadence) — logs a warning if she's made no
     * progress for this long, but never teleports her: the walk home is a straightforward,
     * verified-open route, and the user specifically wants a realistic walk-back with no poof,
     * ever, for this leg.
     */
    private static final int RETURN_HOME_STALL_WARNING_CYCLES = 40;
    /** "Made progress" = Cruz moved more than this per re-issue cycle. */
    private static final double ESCORT_PROGRESS_EPSILON_SQ = 0.25 * 0.25;
    /** Within this range of her target Cruz is considered arrived — never counted as stuck. */
    private static final double ESCORT_ARRIVED_RANGE_SQ = 2.0 * 2.0;
    /**
     * If the escorted player strays this far from Cruz's current position, she abandons the
     * phase's waypoint for a tick and walks straight toward the player instead — nudging them back
     * toward the lesson instead of leaving them to wander off unsupervised. Resumes the normal
     * waypoint target on the next re-issue once they're close again. Always bounds-clamped to
     * {@link #ROOM1_BOUNDS} so she never leaves the building chasing someone through a wall gap.
     *
     * <p>Tuning history (2026-07-05): started at 14, halved to 7 for quicker reaction, then widened
     * to 20 per explicit follow-up feedback asking for a larger chasing radius — she should be
     * willing to let the player roam a bit before reeling them in, not react to every small step.
     */
    private static final double PLAYER_TOO_FAR_DISTANCE_SQ = 20.0 * 20.0;
    /** Throttle for the "come back" nudge below — spoken once per wander, not every re-issue tick. */
    private static final long TOO_FAR_NUDGE_COOLDOWN_TICKS = 200; // 10s

    /** Distance a newly-computed target must differ by to count as a real target change. */
    private static final double TARGET_CHANGE_EPSILON_SQ = 0.5 * 0.5;
    /**
     * How much a single cycle counts against {@link #ESCORT_STUCK_CYCLES_MAX} when the path is
     * provably unreachable (null, or {@code !Path.canReach()}) rather than merely slow — recovers
     * after ~1-2 cycles instead of waiting out the full budget, since there's nothing to wait for.
     * This is what actually fixes her lagging a full 4.5s behind at every Go/Stop tunnel barrier
     * (a genuinely unreachable target every time, since she can't crouch under the slabs).
     */
    private static final int UNREACHABLE_STUCK_STEP = 3;

    private static CustomNpcEntity cachedCruz;
    private static UUID cachedCruzId;
    private static long lastStartingLineNudgeTick = Long.MIN_VALUE;

    private CruzRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        CruzPhase phase = data.get(player.getUUID()).cruzPhase();

        // The Go/Stop briefing (and the crouch teaching in it) only starts once the player is
        // actually standing in the staging pen — not the instant the jump course hands them off.
        if (phase == CruzPhase.GOSTOP_STAGE && !GOSTOP_ZONE.contains(player.position())) {
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §7Come stand with me on the glowing green "
                    + "tile first — follow the arrow!");
            return;
        }

        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.CRUZ_LINES.get(phase);
        AcademyManager.startOrAdvanceDialogue(player, lines, () -> {
            CruzPhase next = switch (phase) {
                case NOT_STARTED -> CruzPhase.BRIEFING;
                case GOSTOP_STAGE -> CruzPhase.GOSTOP_RUN;
                default -> phase; // this phase's advancement is condition-gated, not dialogue-gated
            };
            if (next == phase) return;

            data.mutate(player.getUUID(), p -> p.setCruzPhase(next));
            if (next == CruzPhase.BRIEFING) {
                AcademyTelemetry.startAttempt(player);
            }
            if (next == CruzPhase.GOSTOP_RUN) {
                startGoStop(level, player);
            }
        });
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        ServerPlayer escortTarget = null;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            CruzPhase phase = data.get(player.getUUID()).cruzPhase();
            switch (phase) {
                case BRIEFING -> tickBriefing(level, player, data);
                case MAZE -> tickMaze(level, player, data);
                case JUMP -> tickJump(level, player, data);
                case GOSTOP_RUN -> tickGoStopRun(level, player, data);
                case DONE -> tickDone(level, player, data);
                case GOSTOP_STAGE -> tickGoStopStage(level, player);
                default -> AcademyVisuals.setCompassTarget(player, null); // NOT_STARTED
            }
            // DONE keeps counting as escortable, but only while the player is still physically
            // inside Room 1's (now door-inclusive) footprint AND Sgt. Reyes hasn't actually started
            // instructing them yet — she walks them right up to the doorway into Reyes's room
            // instead of cutting the escort the instant the Go/Stop finish line is crossed, which
            // used to happen well before the player was anywhere near actually leaving. The moment
            // they step through the door (out of ROOM1_BOUNDS) OR Reyes's dialogue actually begins
            // (reyesPhase advances past NOT_STARTED, whichever happens first), this stops matching
            // and updateCruzEscort's null branch sends her walking gradually back home instead —
            // she only ever follows during her own turn of the tutorial, never once Reyes is
            // teaching, regardless of whether the player is still technically inside the bounds.
            boolean stillEscortable = isEscortablePhase(phase)
                    || (phase == CruzPhase.DONE
                        && ROOM1_BOUNDS.contains(player.position())
                        && data.get(player.getUUID()).reyesPhase() == ReyesPhase.NOT_STARTED);
            if (escortTarget == null && stillEscortable) {
                escortTarget = player;
            }
        }

        updateCruzEscort(level, escortTarget);
    }

    private static boolean isEscortablePhase(CruzPhase phase) {
        return phase == CruzPhase.BRIEFING || phase == CruzPhase.MAZE || phase == CruzPhase.JUMP
                || phase == CruzPhase.GOSTOP_STAGE || phase == CruzPhase.GOSTOP_RUN;
    }

    /**
     * Toggles Cruz's escort mode on/off and, while escorting, periodically re-issues a path toward
     * whichever player is being escorted (first found in the online-players iteration if more than
     * one is active — matches this system's already-accepted single-station-at-a-time limitation).
     * Re-issued every {@link #ESCORT_MOVE_INTERVAL_TICKS} ticks rather than every tick, since
     * short-hop pathfinding doesn't need to be recomputed that often and it keeps server cost down.
     */
    private static void updateCruzEscort(ServerLevel level, ServerPlayer escortTarget) {
        CustomNpcEntity cruz = findCruz(level);
        if (cruz == null) return;

        // Shrink her hitbox to fit under the crouch-tunnel's slab lanes whenever she's actually in
        // there (either direction) -- checked every tick regardless of escort state, since she can
        // pass through the tunnel escorting a player OR walking home. This is what actually lets
        // her walk it at all instead of every lane being a provably unreachable path that the
        // stuck-recovery had no choice but to poof through.
        cruz.setCrouchingForObstacle(GOSTOP_TUNNEL_ZONE.contains(cruz.position()));

        if (escortTarget == null) {
            // Nobody to escort (nobody online in Room 1, everyone reset/finished): walk back to
            // the briefing anchor instead of freezing wherever the last escort leg ended — she
            // must always be waiting beside her spot at (-153.5,-33,32.5) for the next trainee.
            tickReturnHome(level, cruz);
            return;
        }
        cruz.setEscorting(true);

        long now = level.getGameTime();
        if (now < cruz.getNextEscortMoveTick()) return;
        cruz.setNextEscortMoveTick(now + ESCORT_MOVE_INTERVAL_TICKS);

        // Safety net: if she somehow ended up outside Room 1 entirely, don't bother pathing —
        // poof her straight back to the escorted player.
        if (!ROOM1_BOUNDS.contains(cruz.position())) {
            recoverCruz(level, cruz, escortTarget);
            return;
        }

        AcademySavedData data = AcademySavedData.get(level);
        CruzPhase phase = data.get(escortTarget.getUUID()).cruzPhase();
        Vec3 target;
        if (cruz.position().distanceToSqr(escortTarget.position()) > PLAYER_TOO_FAR_DISTANCE_SQ) {
            // Stop about 2 blocks short of the player rather than pathing exactly onto their
            // position — reads as "catching up to" them rather than walking through them.
            target = clampToRoom1Bounds(nearPlayerTarget(cruz.position(), escortTarget.position()));
            long gameTime = level.getGameTime();
            if (gameTime - cruz.getLastTooFarNudgeTick() >= TOO_FAR_NUDGE_COOLDOWN_TICKS
                    && !AcademyManager.isDialogueActive(escortTarget.getUUID())) {
                cruz.setLastTooFarNudgeTick(gameTime);
                AcademyManager.sendPrompt(escortTarget, AcademyManager.pick(escortTarget,
                        "§a[Officer Cruz] §7Hey, don't wander off! Come back this way, trainee!",
                        "§a[Officer Cruz] §7Whoa there — come on back, we're not done yet!",
                        "§a[Officer Cruz] §7Stay close, trainee! Follow me back over here."));
            }
        } else {
            target = switch (phase) {
                case BRIEFING -> nextUnhitMarkFor(escortTarget);
                case MAZE -> currentWaypoint(escortTarget, MAZE_WAYPOINTS);
                case JUMP -> currentWaypoint(escortTarget, JUMP_WAYPOINTS);
                // Waits at the tunnel entrance while she briefs...
                case GOSTOP_STAGE -> STAGING_POS;
                // ...then follows the player THROUGH the slab tunnel during the run. She can't crouch
                // under the 1.5-block slab lanes, so each lane blocks her path — the stuck-recovery
                // below then poofs her to the player's side, reading as the instructor keeping pace
                // barrier by barrier instead of being left behind at the entrance. Stops short via
                // nearPlayerTarget rather than pathing exactly onto the player (same as the too-far
                // chase above), so she never shoulders into them at the barrier.
                case GOSTOP_RUN -> nearPlayerTarget(cruz.position(), escortTarget.position());
                // The lesson itself is done — walk the player straight to the doorway instead of
                // a fixed waypoint. `tick`'s stillEscortable check already stops treating them as
                // an escort target the instant they physically step outside ROOM1_BOUNDS or Reyes
                // starts instructing, so this case only ever runs while they're still inside,
                // heading for the door. Same near-target stop-short as GOSTOP_RUN.
                case DONE -> nearPlayerTarget(cruz.position(), escortTarget.position());
                default -> null;
            };
        }
        if (target == null) return;

        // A genuine target change (new phase, new waypoint, or the too-far chase toggling on/off)
        // resets stall history -- otherwise stuck-cycle bookkeeping accumulated chasing one target
        // could trigger a spurious poof seconds into pursuing a completely different one.
        Vec3 lastTarget = cruz.getLastEscortTarget();
        if (lastTarget == null || lastTarget.distanceToSqr(target) > TARGET_CHANGE_EPSILON_SQ) {
            cruz.setEscortStuckCycles(0);
            cruz.setLastEscortPos(cruz.position());
        }
        cruz.setLastEscortTarget(target);

        boolean issued = cruz.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        net.minecraft.world.level.pathfinder.Path path = cruz.getNavigation().getPath();
        boolean unreachable = !issued || path == null || !path.canReach();

        // Stuck detection: a provably unreachable path (fast-fail) counts for more than a single
        // cycle so recovery doesn't wait out the whole budget; otherwise it's genuine per-cycle
        // progress (navigator-reported stuck, or no net displacement) — unless she's already
        // essentially at the target.
        double distToTargetSq = cruz.position().distanceToSqr(target);
        boolean movedThisCycle = cruz.position().distanceToSqr(cruz.getLastEscortPos()) >= ESCORT_PROGRESS_EPSILON_SQ;
        cruz.setLastEscortPos(cruz.position());

        int stuckCycles = cruz.getEscortStuckCycles();
        if (distToTargetSq <= ESCORT_ARRIVED_RANGE_SQ) {
            stuckCycles = 0;
        } else if (unreachable) {
            stuckCycles += UNREACHABLE_STUCK_STEP;
        } else if (cruz.getNavigation().isStuck() || !movedThisCycle) {
            stuckCycles++;
        } else {
            stuckCycles = 0;
        }
        cruz.setEscortStuckCycles(stuckCycles);

        if (stuckCycles >= ESCORT_STUCK_CYCLES_MAX) {
            cruz.setEscortStuckCycles(0);
            recoverCruz(level, cruz, escortTarget);
        }
    }

    /** Clamps a position into {@link #ROOM1_BOUNDS} so a "come back here" chase target never leaves the building. */
    private static Vec3 clampToRoom1Bounds(Vec3 pos) {
        return new Vec3(
                Mth.clamp(pos.x, ROOM1_BOUNDS.minX, ROOM1_BOUNDS.maxX),
                Mth.clamp(pos.y, ROOM1_BOUNDS.minY, ROOM1_BOUNDS.maxY),
                Mth.clamp(pos.z, ROOM1_BOUNDS.minZ, ROOM1_BOUNDS.maxZ));
    }

    /** How short of the player's exact position the too-far chase target stops. */
    private static final double CHASE_STOP_DISTANCE = 2.0;

    /**
     * A point {@link #CHASE_STOP_DISTANCE} blocks short of {@code playerPos}, on the side
     * {@code fromPos} is already approaching from — same offset idiom as {@link #recoverCruz}, so
     * the too-far chase reads as catching up to the player rather than walking through them.
     */
    private static Vec3 nearPlayerTarget(Vec3 fromPos, Vec3 playerPos) {
        Vec3 toward = fromPos.subtract(playerPos);
        if (toward.horizontalDistanceSqr() < 0.01) return playerPos;
        Vec3 offset = new Vec3(toward.x, 0, toward.z).normalize().scale(CHASE_STOP_DISTANCE);
        return playerPos.add(offset);
    }

    /**
     * With no one to escort, Cruz walks back to {@link #BRIEFING_ANCHOR} (same re-issue cadence as
     * an escort leg) and drops escort mode once she's arrived — this leg never teleports, see
     * {@link #RETURN_HOME_STALL_WARNING_CYCLES}.
     */
    private static void tickReturnHome(ServerLevel level, CustomNpcEntity cruz) {
        if (cruz.position().distanceToSqr(BRIEFING_ANCHOR) <= ESCORT_ARRIVED_RANGE_SQ) {
            cruz.setEscorting(false);
            cruz.setLastEscortTarget(null);
            return;
        }
        cruz.setEscorting(true);

        // Coming straight off an escort leg (lastEscortTarget still set to wherever that left
        // off): don't let its stall history count against this completely different walk.
        Vec3 lastTarget = cruz.getLastEscortTarget();
        if (lastTarget == null || lastTarget.distanceToSqr(BRIEFING_ANCHOR) > TARGET_CHANGE_EPSILON_SQ) {
            cruz.setEscortStuckCycles(0);
            cruz.setLastEscortPos(cruz.position());
        }
        cruz.setLastEscortTarget(BRIEFING_ANCHOR);

        long now = level.getGameTime();
        if (now < cruz.getNextEscortMoveTick()) return;
        cruz.setNextEscortMoveTick(now + ESCORT_MOVE_INTERVAL_TICKS);

        cruz.getNavigation().moveTo(BRIEFING_ANCHOR.x, BRIEFING_ANCHOR.y, BRIEFING_ANCHOR.z, 1.0);
        boolean movedThisCycle = cruz.position().distanceToSqr(cruz.getLastEscortPos()) >= ESCORT_PROGRESS_EPSILON_SQ;
        cruz.setLastEscortPos(cruz.position());

        // Deliberately never teleports on this leg: it's a straightforward, verified-open walk
        // back to a fixed point, so she should always just walk it for realism. Simply re-issuing
        // moveTo every cycle (rather than giving up after N stalls) means a temporary blip just
        // resolves itself on the next attempt instead of triggering a poof. A one-time log line
        // flags the rare case where she's made no progress for an unusually long stretch, purely
        // for debugging -- it takes no action and never falls back to a teleport.
        if (movedThisCycle) {
            cruz.setEscortStuckCycles(0);
        } else {
            int cycles = cruz.getEscortStuckCycles() + 1;
            cruz.setEscortStuckCycles(cycles);
            if (cycles == RETURN_HOME_STALL_WARNING_CYCLES) {
                BerongSMP.LOGGER.warn("Officer Cruz has made no progress walking back to her anchor for {} cycles",
                        cycles);
            }
        }
    }

    /**
     * Instantly returns Cruz to her briefing anchor. Called by {@code /bfp tutorial [reset]}
     * and Capt. Morfe's fail-reset so a restarting trainee always finds her waiting beside them at
     * (-153.5,-33,32.5) — never stranded across the building where the last run left her.
     */
    public static void resetCruz(ServerLevel level) {
        CustomNpcEntity cruz = findCruz(level);
        if (cruz == null) return;
        cruz.setEscortStuckCycles(0);
        teleportCruzTo(level, cruz, BRIEFING_ANCHOR);
        cruz.setEscorting(false);
    }

    /**
     * Teleports a lost/stuck Cruz to just beside the escorted player, wrapped in POOF particles at
     * both ends so it reads as an intentional instructor catch-up rather than a glitch. The offset
     * keeps her on the side she was already on (or on top of the player if degenerate).
     */
    private static void recoverCruz(ServerLevel level, CustomNpcEntity cruz, ServerPlayer player) {
        Vec3 from = cruz.position();
        Vec3 toward = from.subtract(player.position());
        Vec3 offset = toward.horizontalDistanceSqr() < 0.01
                ? Vec3.ZERO
                : new Vec3(toward.x, 0, toward.z).normalize().scale(1.5);
        teleportCruzTo(level, cruz, player.position().add(offset));
    }

    /** POOF at both ends + navigation stop + teleport — the shared "Cruz jumps somewhere" visual. */
    private static void teleportCruzTo(ServerLevel level, CustomNpcEntity cruz, Vec3 dest) {
        Vec3 from = cruz.position();
        level.sendParticles(ParticleTypes.POOF, from.x, from.y + 0.8, from.z, 10, 0.3, 0.4, 0.3, 0.01);
        cruz.getNavigation().stop();
        cruz.teleportTo(dest.x, dest.y, dest.z);
        level.sendParticles(ParticleTypes.POOF, dest.x, dest.y + 0.8, dest.z, 10, 0.3, 0.4, 0.3, 0.01);
    }

    private static Vec3 nextUnhitMarkFor(ServerPlayer player) {
        Set<Integer> hits = marksHit.getOrDefault(player.getUUID(), Set.of());
        for (int i = 0; i < GREEN_MARKS.size(); i++) {
            if (!hits.contains(i)) return Vec3.atCenterOf(GREEN_MARKS.get(i));
        }
        return Vec3.atCenterOf(GREEN_MARKS.get(GREEN_MARKS.size() - 1));
    }

    /**
     * The player's current waypoint along {@code chain}, advancing the stored index while they're
     * within {@link #WAYPOINT_ADVANCE_RANGE_SQ} of it (never past the last entry).
     */
    private static Vec3 currentWaypoint(ServerPlayer player, List<Vec3> chain) {
        UUID id = player.getUUID();
        int idx = waypointIdx.getOrDefault(id, 0);
        while (idx < chain.size() - 1
                && player.position().distanceToSqr(chain.get(idx)) <= WAYPOINT_ADVANCE_RANGE_SQ) {
            idx++;
        }
        waypointIdx.put(id, idx);
        return chain.get(idx);
    }

    /**
     * Resolves the single Officer Cruz instance, caching a direct object reference (schematic
     * entities are discarded and respawned as fresh Java objects on every server start, so a
     * cached reference only needs to survive within one running session). Falls back to a UUID
     * lookup, then a bounded area scan, tie-broken by proximity to her known briefing anchor in
     * case a duplicate is ever manually spawned (e.g. via {@code NpcSpawnerItem}).
     */
    private static CustomNpcEntity findCruz(ServerLevel level) {
        if (cachedCruz != null && !cachedCruz.isRemoved() && cachedCruz.isAlive()) {
            return cachedCruz;
        }
        if (cachedCruzId != null) {
            Entity entity = level.getEntity(cachedCruzId);
            if (entity instanceof CustomNpcEntity npc && npc.getNpcType() == NpcType.OFFICER_CRUZ && !npc.isRemoved()) {
                cachedCruz = npc;
                return npc;
            }
        }

        AABB searchBounds = new AABB(-180, -40, 0, -95, -25, 65);
        List<CustomNpcEntity> found = level.getEntitiesOfClass(CustomNpcEntity.class, searchBounds,
                e -> e.getNpcType() == NpcType.OFFICER_CRUZ);
        if (found.isEmpty()) return null;

        CustomNpcEntity best = found.get(0);
        double bestDistSq = best.position().distanceToSqr(BRIEFING_ANCHOR);
        for (CustomNpcEntity npc : found) {
            double distSq = npc.position().distanceToSqr(BRIEFING_ANCHOR);
            if (distSq < bestDistSq) {
                best = npc;
                bestDistSq = distSq;
            }
        }
        cachedCruz = best;
        cachedCruzId = best.getUUID();
        return best;
    }

    private static void tickBriefing(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        Set<Integer> hits = marksHit.computeIfAbsent(id, k -> new HashSet<>());
        Vec3 pos = player.position();
        int nextMark = -1;
        for (int i = 0; i < GREEN_MARKS.size(); i++) {
            if (hits.contains(i)) continue;
            BlockPos mark = GREEN_MARKS.get(i);
            if (pos.distanceToSqr(mark.getX() + 0.5, mark.getY(), mark.getZ() + 0.5) <= MARK_RADIUS_SQ) {
                hits.add(i);
            } else if (nextMark < 0) {
                nextMark = i;
            }
        }
        if (hits.size() >= GREEN_MARKS.size()) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.MAZE));
            marksHit.remove(id);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fThat's all four — wonderful! Now for the maze: "
                    + "move your §emouse§f to look down the open path, then hold §eW§f to walk it. "
                    + "Follow the glowing arrow — I'm right beside you!");
            return;
        }
        // Only the NEXT mark gets the particle marker — the physical lime concrete tiles
        // (AcademyBuildingManager.placeGreenMarks) keep all four permanently visible.
        if (nextMark >= 0) {
            if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
                AcademyVisuals.highlightBlocks(level, List.of(GREEN_MARKS.get(nextMark)));
            }
            AcademyVisuals.setCompassTarget(player, Vec3.atCenterOf(GREEN_MARKS.get(nextMark)));
        }
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0 && !AcademyManager.isDialogueActive(id)) {
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§a[Officer Cruz] §7See the tall beam of green light? That's your next tile — "
                            + "hold the §eW key§7 and walk to it!",
                    "§a[Officer Cruz] §7You're doing great — one green tile at a time! The glowing "
                            + "arrow at the top of your screen points right at the next one.",
                    "§a[Officer Cruz] §7No rush at all! Spot the green glow, then hold §eW§7 and "
                            + "stroll right onto it."));
        }
    }

    private static void tickMaze(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        if (JUMP_ZONE.contains(player.position())) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.JUMP));
            waypointIdx.remove(id);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fYou found the way through — great navigating! "
                    + "Now some low hurdles: keep holding §eW§f and tap the §eSpacebar§f just before each one "
                    + "to hop over. Ready? Jump!");
            return;
        }
        AcademyVisuals.setCompassTarget(player, currentWaypoint(player, MAZE_WAYPOINTS));
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0 && !AcademyManager.isDialogueActive(id)) {
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§a[Officer Cruz] §7Hit a wall? No worries! Turn with your §emouse§7 first, "
                            + "spot the opening, then walk with §eW§7. The glowing arrow shows the way.",
                    "§a[Officer Cruz] §7Take it slow — look around with your §emouse§7 until you "
                            + "see the gap, then walk on through with §eW§7.",
                    "§a[Officer Cruz] §7Trust the arrow up top! It always points at your next "
                            + "opening in the maze. Look first, walk second."));
        }
    }

    private static void tickJump(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        // Directional finish line: the hurdles are only "cleared" by exiting EAST past the last
        // one (X=-111). The old any-direction check (!JUMP_ZONE.contains) also fired when the
        // player stepped backward into the maze, silently skipping them ahead to Go/Stop without
        // ever jumping a single hurdle.
        if (player.getX() >= JUMP_FINISH_X) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.GOSTOP_STAGE));
            waypointIdx.remove(id);
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §fAll hurdles cleared — you're a natural! "
                    + "Now come meet me at the tunnel entrance — just follow the glowing arrow — for our "
                    + "very last lesson.");
            return;
        }
        AcademyVisuals.setCompassTarget(player, currentWaypoint(player, JUMP_WAYPOINTS));
        if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0 && !AcademyManager.isDialogueActive(id)) {
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§a[Officer Cruz] §7Hold §eW§7 to keep walking and tap the §eSpacebar§7 just "
                            + "before each hurdle. Bumped one? Back up a step and try again!",
                    "§a[Officer Cruz] §7Timing is everything — walk straight at the hurdle and tap "
                            + "§eSpacebar§7 right at its edge!",
                    "§a[Officer Cruz] §7Almost there! Keep moving with §eW§7 and hop each hurdle "
                            + "with a quick §eSpacebar§7 tap."));
        }
    }

    /**
     * Before the Go/Stop game actually begins: point the compass at the staging pen, and push the
     * player back east if they try to cross the starting line before Cruz has called GO. Doesn't
     * count as a movement mistake — the game hasn't started yet, this is just an invisible wall.
     */
    private static void tickGoStopStage(ServerLevel level, ServerPlayer player) {
        AcademyVisuals.setCompassTarget(player, STAGING_POS);
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, List.of(GOSTOP_STAGE_MARK));
        }

        if (player.getX() > GOSTOP_STARTING_LINE_X
                || player.getZ() < GOSTOP_STARTING_LINE_MIN_Z
                || player.getZ() > GOSTOP_STARTING_LINE_MAX_Z) {
            return;
        }

        player.teleportTo(level, GOSTOP_STARTING_LINE_X + 0.5, player.getY(), player.getZ(),
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);

        long gameTime = level.getGameTime();
        if (gameTime - lastStartingLineNudgeTick >= STARTING_LINE_NUDGE_COOLDOWN_TICKS
                && !AcademyManager.isDialogueActive(player.getUUID())) {
            lastStartingLineNudgeTick = gameTime;
            AcademyManager.sendPrompt(player, "§a[Officer Cruz] §7Not yet! Stand on the glowing green "
                    + "tile and wait for me to call §a§lGO§r§7 first.");
            AcademyManager.playNpcSound(player, "academy.officer_cruz.012");
        }
    }

    /** Points toward Sgt. Reyes until the player has actually started Room 2. */
    private static void tickDone(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (data.get(player.getUUID()).reyesPhase() != ReyesPhase.NOT_STARTED) {
            AcademyVisuals.setCompassTarget(player, null);
            return;
        }
        AcademyVisuals.setCompassTarget(player, REYES_ANCHOR);
    }

    // -----------------------------------------------------------------------
    // Go / Stop mini-game
    // -----------------------------------------------------------------------

    private static final class GoStopState {
        boolean isGo = true;
        long nextFlipTick;
        long stopStartTick = -1;
        Vec3 posAtStop = Vec3.ZERO;
        /** The exact banner text last sent, so the fun countdown below can re-append it without re-randomizing. */
        String currentBanner = "";
    }

    /** GO/STOP callout banners and their matching recorded voice lines — index-paired, not picked independently. */
    private static final String[] GO_BANNERS = {
            "§a▶ GO! Hold the W key and keep walking!",
            "§a▶ GO! Move move move — keep that W key down!",
            "§a▶ GO! Forward, trainee — hold W and go!"
    };
    private static final String[] GO_SOUND_KEYS = {
            "academy.officer_cruz.014", "academy.officer_cruz.015", "academy.officer_cruz.016"
    };
    private static final String[] STOP_BANNERS = {
            "§c■ STOP! Let go of every key and freeze!",
            "§c■ STOP! Hands off the keys — statue still!",
            "§c■ STOP! Freeze right where you are!"
    };
    private static final String[] STOP_SOUND_KEYS = {
            "academy.officer_cruz.017", "academy.officer_cruz.018", "academy.officer_cruz.019"
    };

    private static void startGoStop(ServerLevel level, ServerPlayer player) {
        GoStopState state = new GoStopState();
        state.nextFlipTick = level.getGameTime() + randomInterval(level);
        int idx = level.getRandom().nextInt(GO_BANNERS.length);
        state.currentBanner = GO_BANNERS[idx];
        goStopStates.put(player.getUUID(), state);
        AcademyManager.sendPrompt(player, state.currentBanner);
        AcademyManager.playNpcSound(player, GO_SOUND_KEYS[idx]);
    }

    private static long randomInterval(ServerLevel level) {
        int span = GOSTOP_MAX_INTERVAL_TICKS - GOSTOP_MIN_INTERVAL_TICKS + 1;
        return GOSTOP_MIN_INTERVAL_TICKS + level.getRandom().nextInt(span);
    }

    private static void tickGoStopRun(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        AcademyVisuals.setCompassTarget(player, null); // Cruz shouts GO/STOP from the staging line, no arrow needed
        UUID id = player.getUUID();
        GoStopState state = goStopStates.get(id);
        if (state == null) {
            startGoStop(level, player);
            return;
        }

        long gameTime = level.getGameTime();
        boolean justFlipped = false;

        if (gameTime >= state.nextFlipTick) {
            justFlipped = true;
            state.isGo = !state.isGo;
            state.nextFlipTick = gameTime + randomInterval(level);
            int idx = level.getRandom().nextInt(GO_BANNERS.length);
            String soundKey;
            if (state.isGo) {
                state.stopStartTick = -1;
                state.currentBanner = GO_BANNERS[idx];
                soundKey = GO_SOUND_KEYS[idx];
            } else {
                state.stopStartTick = gameTime;
                state.posAtStop = player.position();
                state.currentBanner = STOP_BANNERS[idx];
                soundKey = STOP_SOUND_KEYS[idx];
            }
            AcademyManager.sendPrompt(player, state.currentBanner);
            AcademyManager.playNpcSound(player, soundKey);
        }

        // Fun green countdown to the next call, refreshed once a second — purely cosmetic, doesn't
        // change any timing or detection, just re-appends the remaining seconds to whatever banner
        // is already showing so the wait doesn't feel like a black box.
        if (!justFlipped && gameTime % SECOND_TICKS == 0) {
            long secondsLeft = Math.max(0, (state.nextFlipTick - gameTime) / SECOND_TICKS);
            AcademyManager.sendPrompt(player, state.currentBanner + " §a⏱" + secondsLeft + "s");
        }

        if (!state.isGo && state.stopStartTick >= 0) {
            long grace = Config.ACADEMY_GOSTOP_GRACE_TICKS.get();
            if (gameTime - state.stopStartTick <= grace) {
                // Reaction window (default 1.5s): keep re-anchoring the reference position, so
                // sliding to a halt during the window is never punished — only movement AFTER the
                // window (measured from wherever they actually came to rest) counts.
                state.posAtStop = player.position();
            } else if (player.position().distanceToSqr(state.posAtStop) > GOSTOP_MOVE_EPSILON_SQ) {
                player.teleportTo(level, STAGING_POS.x, STAGING_POS.y, STAGING_POS.z,
                        Collections.emptySet(), player.getYRot(), player.getXRot(), true);
                data.mutate(id, p -> { p.setCruzPhase(CruzPhase.GOSTOP_STAGE); p.addMovementMistake(); });
                AcademyTelemetry.record(player, "academy_movement_mistake", "gostop_violation");
                goStopStates.remove(id);
                AcademyManager.cancelDialogue(player);
                AcademyManager.sendPrompt(player, "§c[Officer Cruz] §7Oops — you moved after STOP! That's okay, "
                        + "everyone does it once. Back to the start line: walk on §aGO§7, and when you hear "
                        + "§cSTOP§7, let go of all the keys right away.");
                AcademyManager.playNpcSound(player, "academy.officer_cruz.020");
                return;
            }
        }

        if (player.getX() <= GOSTOP_FINISH_X) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.DONE));
            AcademyTelemetry.record(player, "academy_room1_complete", null);
            goStopStates.remove(id);
            // No teleport here on purpose: DONE keeps counting as escortable (see tick()/
            // updateCruzEscort), targeting the player's own live position every re-issue, exactly
            // like GOSTOP_RUN already did throughout the run -- so she just naturally walks up and
            // ends up in front of the player over the next couple of seconds instead of snapping
            // to their side. The "you did it, go find Reyes" line is delivered as a real spoken
            // Cruz line through the dialogue sequencer instead of a bare caption; she keeps
            // escorting/standing with the player for its whole duration (and beyond, until they
            // actually leave through the door), so she's never rushing off mid-sentence.
            AcademyManager.forceStartDialogue(player, AcademyDialogue.CRUZ_LINES.get(CruzPhase.DONE), () -> {});
        }
    }

    /**
     * Called from {@code AcademyManager}'s logout/login handlers to drop this room's per-player
     * transient state. Also rolls a mid-run {@link CruzPhase} (anything past NOT_STARTED but not
     * yet DONE) back to NOT_STARTED — same idiom as {@code ReyesRoomManager}/{@code SantosRoomManager}
     * rolling back their own "mid-effect" phases on logout, just simpler here since none of Room 1's
     * phases place blocks/fire that need cleanup. Without this, closing the game mid-Room-1 and
     * reconnecting left {@code CruzPhase} at whatever it was; since login always routes to the main
     * lobby (far outside {@link #ROOM1_BOUNDS}), the player would still get Cruz's idle-nudge chat
     * prompts and her escort/teleport logic firing at them there, even though the room's own tick
     * dispatch and {@link #isEscortablePhase} never checked physical location — reported as "Cruz
     * follows me into the main lobby after reconnecting."
     */
    public static void clearPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        marksHit.remove(id);
        goStopStates.remove(id);
        waypointIdx.remove(id);

        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        CruzPhase phase = data.get(id).cruzPhase();
        if (phase != CruzPhase.NOT_STARTED && phase != CruzPhase.DONE) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.NOT_STARTED));
        }
    }
}
