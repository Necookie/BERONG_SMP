package net.necookie.disastersim.academy.room1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademySavedData;
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
 * {@code world.NewTutBuildingManager}) physically escorts the player through Briefing/Maze/Jump
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

    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100; // 5s, matches TutorialManager's idiom
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
     * Consecutive no-progress re-issue cycles before poof-recovery. Slightly more generous than a
     * single briefing/maze/jump leg (≈3s) since the door hand-off leg below can be a longer walk
     * across the open corridor — a normal walk there shouldn't spuriously trip a poof, which would
     * look exactly like the "instant banishing" this system is meant to avoid.
     */
    private static final int ESCORT_STUCK_CYCLES_MAX = 6;
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
     * <p>Halved from the original 14 blocks (2026-07-05) — she reacts to a much smaller wander
     * now, since 14 blocks felt unresponsive in practice (by the time she reacted, the player was
     * already most of the way across a zone).
     */
    private static final double PLAYER_TOO_FAR_DISTANCE_SQ = 7.0 * 7.0;

    private static CustomNpcEntity cachedCruz;
    private static UUID cachedCruzId;
    private static long nextEscortMoveTick;
    private static Vec3 lastCruzPos = Vec3.ZERO;
    private static int stuckCycles;

    private CruzRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        CruzPhase phase = data.get(player.getUUID()).cruzPhase();

        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.CRUZ_LINES.get(phase);
        AcademyManager.startOrAdvanceDialogue(player, lines, () -> {
            CruzPhase next = switch (phase) {
                case NOT_STARTED -> CruzPhase.BRIEFING;
                case GOSTOP_STAGE -> CruzPhase.GOSTOP_RUN;
                default -> phase; // this phase's advancement is condition-gated, not dialogue-gated
            };
            if (next == phase) return;

            data.mutate(player.getUUID(), p -> p.setCruzPhase(next));
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
                case GOSTOP_STAGE -> AcademyVisuals.setCompassTarget(player, STAGING_POS);
                default -> AcademyVisuals.setCompassTarget(player, null); // NOT_STARTED
            }
            // DONE keeps counting as escortable, but only while the player is still physically
            // inside Room 1's (now door-inclusive) footprint — she walks them right up to the
            // doorway into Reyes's room instead of cutting the escort the instant the Go/Stop
            // finish line is crossed, which used to happen well before the player was anywhere
            // near actually leaving. The moment they step through the door and out of
            // ROOM1_BOUNDS, this stops matching and updateCruzEscort's null branch sends her
            // walking gradually back home instead.
            boolean stillEscortable = isEscortablePhase(phase)
                    || (phase == CruzPhase.DONE && ROOM1_BOUNDS.contains(player.position()));
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

        if (escortTarget == null) {
            // Nobody to escort (nobody online in Room 1, everyone reset/finished): walk back to
            // the briefing anchor instead of freezing wherever the last escort leg ended — she
            // must always be waiting beside her spot at (-153.5,-33,32.5) for the next trainee.
            tickReturnHome(level, cruz);
            return;
        }
        cruz.setEscorting(true);

        long now = level.getGameTime();
        if (now < nextEscortMoveTick) return;
        nextEscortMoveTick = now + ESCORT_MOVE_INTERVAL_TICKS;

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
            target = clampToRoom1Bounds(escortTarget.position());
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
                // barrier by barrier instead of being left behind at the entrance.
                case GOSTOP_RUN -> escortTarget.position();
                // The lesson itself is done — walk the player straight to the doorway instead of
                // a fixed waypoint. `tick`'s stillEscortable check already stops treating them as
                // an escort target the instant they physically step outside ROOM1_BOUNDS, so this
                // case only ever runs while they're still inside (heading for the door).
                case DONE -> escortTarget.position();
                default -> null;
            };
        }
        if (target == null) return;
        boolean issued = cruz.getNavigation().moveTo(target.x, target.y, target.z, 1.0);

        // Stuck detection: count consecutive re-issue cycles where the path couldn't even be
        // created, the navigator reports stuck, or Cruz made no net progress — unless she's
        // already essentially at the target.
        double distToTargetSq = cruz.position().distanceToSqr(target);
        boolean movedThisCycle = cruz.position().distanceToSqr(lastCruzPos) >= ESCORT_PROGRESS_EPSILON_SQ;
        lastCruzPos = cruz.position();

        if (distToTargetSq <= ESCORT_ARRIVED_RANGE_SQ) {
            stuckCycles = 0;
        } else if (!issued || cruz.getNavigation().isStuck() || !movedThisCycle) {
            stuckCycles++;
        } else {
            stuckCycles = 0;
        }

        if (stuckCycles >= ESCORT_STUCK_CYCLES_MAX) {
            stuckCycles = 0;
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

    /**
     * With no one to escort, Cruz walks back to {@link #BRIEFING_ANCHOR} (same cadence and stuck
     * handling as an escort leg; a return route blocked by the crouch-only tunnel just times out
     * into a poof-teleport home after ~3s) and drops escort mode once she's arrived.
     */
    private static void tickReturnHome(ServerLevel level, CustomNpcEntity cruz) {
        if (cruz.position().distanceToSqr(BRIEFING_ANCHOR) <= ESCORT_ARRIVED_RANGE_SQ) {
            cruz.setEscorting(false);
            return;
        }
        cruz.setEscorting(true);

        long now = level.getGameTime();
        if (now < nextEscortMoveTick) return;
        nextEscortMoveTick = now + ESCORT_MOVE_INTERVAL_TICKS;

        boolean issued = cruz.getNavigation().moveTo(BRIEFING_ANCHOR.x, BRIEFING_ANCHOR.y, BRIEFING_ANCHOR.z, 1.0);
        boolean movedThisCycle = cruz.position().distanceToSqr(lastCruzPos) >= ESCORT_PROGRESS_EPSILON_SQ;
        lastCruzPos = cruz.position();

        if (!issued || cruz.getNavigation().isStuck() || !movedThisCycle) {
            stuckCycles++;
        } else {
            stuckCycles = 0;
        }
        if (stuckCycles >= ESCORT_STUCK_CYCLES_MAX) {
            stuckCycles = 0;
            teleportCruzTo(level, cruz, BRIEFING_ANCHOR);
            cruz.setEscorting(false);
        }
    }

    /**
     * Instantly returns Cruz to her briefing anchor. Called by {@code /bfp new_tutorial [reset]}
     * and Capt. Morfe's fail-reset so a restarting trainee always finds her waiting beside them at
     * (-153.5,-33,32.5) — never stranded across the building where the last run left her.
     */
    public static void resetCruz(ServerLevel level) {
        CustomNpcEntity cruz = findCruz(level);
        if (cruz == null) return;
        stuckCycles = 0;
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
        // (NewTutBuildingManager.placeGreenMarks) keep all four permanently visible.
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
    }

    private static void startGoStop(ServerLevel level, ServerPlayer player) {
        GoStopState state = new GoStopState();
        state.nextFlipTick = level.getGameTime() + randomInterval(level);
        goStopStates.put(player.getUUID(), state);
        AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                "§a▶ GO! Hold the W key and keep walking!",
                "§a▶ GO! Move move move — keep that W key down!",
                "§a▶ GO! Forward, trainee — hold W and go!"));
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

        if (gameTime >= state.nextFlipTick) {
            state.isGo = !state.isGo;
            state.nextFlipTick = gameTime + randomInterval(level);
            if (state.isGo) {
                state.stopStartTick = -1;
                AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                "§a▶ GO! Hold the W key and keep walking!",
                "§a▶ GO! Move move move — keep that W key down!",
                "§a▶ GO! Forward, trainee — hold W and go!"));
            } else {
                state.stopStartTick = gameTime;
                state.posAtStop = player.position();
                AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                        "§c■ STOP! Let go of every key and freeze!",
                        "§c■ STOP! Hands off the keys — statue still!",
                        "§c■ STOP! Freeze right where you are!"));
            }
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
                goStopStates.remove(id);
                AcademyManager.cancelDialogue(player);
                AcademyManager.sendPrompt(player, "§c[Officer Cruz] §7Oops — you moved after STOP! That's okay, "
                        + "everyone does it once. Back to the start line: walk on §aGO§7, and when you hear "
                        + "§cSTOP§7, let go of all the keys right away.");
                return;
            }
        }

        if (player.getX() <= GOSTOP_FINISH_X) {
            data.mutate(id, p -> p.setCruzPhase(CruzPhase.DONE));
            goStopStates.remove(id);
            // Make sure she's actually standing right beside the player for this beat, rather than
            // wherever the last 15-tick escort re-issue happened to leave her — a snap-to-side poof
            // if she isn't already close (the same idiom recoverCruz already uses elsewhere), then
            // the "you did it, go find Reyes" line delivered as a real spoken Cruz line through the
            // dialogue sequencer instead of a bare caption.
            CustomNpcEntity cruz = findCruz(level);
            if (cruz != null && cruz.position().distanceToSqr(player.position()) > ESCORT_ARRIVED_RANGE_SQ) {
                recoverCruz(level, cruz, player);
            }
            AcademyManager.forceStartDialogue(player, AcademyDialogue.CRUZ_LINES.get(CruzPhase.DONE), () -> {});
        }
    }

    /** Called from {@code AcademyManager}'s logout handler to drop this room's per-player state. */
    public static void clearPlayer(UUID id) {
        marksHit.remove(id);
        goStopStates.remove(id);
        waypointIdx.remove(id);
    }
}
