package net.necookie.disastersim.common.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.necookie.disastersim.block.TableBlock;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live "duck, cover, and hold": every online player is checked every tick, independent of any
 * active {@code SimulationSession} — this is deliberate, so the drill can be tested by simply
 * crouching under a block anywhere, without needing to run a full earthquake simulation.
 *
 * <p>Cover is recognized under either 1 or 2 blocks of clearance (a short desk or a taller
 * shelter both count), matching the same {@code isCrouching()} + solid-block-above idiom already
 * proven in {@code TutorialManager}'s scripted drill — plus, separately, crouching within 1 block
 * of a {@link TableBlock}, since a real table is too short to ever occupy the "block above the
 * player's feet" cell on its own. When a real quake-type session is active for
 * the player, reaching the target also logs the {@code duck_cover_hold} telemetry event; outside a
 * session, the buff and message still fire (for testing) but nothing is logged, matching how
 * {@link DropAndRollManager} already treats the no-session case.
 *
 * <p>Also drives the "go under a table" assist: crouching while facing or beside a
 * {@link TableBlock} pre-emptively shrinks the player to vanilla's crawl hitbox
 * ({@code Pose.SWIMMING}) so normal movement can actually carry them into its kneehole — see
 * {@link #allowCrawlUnderTable}. This doesn't touch sneak/shift itself, only what fits under the
 * player once they're already crouching near a table.
 *
 * <p>Transient per-player state (see {@link DropAndRollManager}'s own javadoc for why this idiom
 * doesn't need to survive a server restart).
 *
 * <p>{@link #isCompliant}/{@link #ticksHeld}/{@link #TARGET_TICKS} are read-only accessors used by
 * {@code academy.room3.SantosRoomManager} to gate the new tutorial building's earthquake drill on
 * this exact same compliance state, without duplicating any duck/cover/hold logic.
 */
public final class DuckCoverHoldManager {

    static {
        TickScheduler.register(DuckCoverHoldManager::tick);
    }

    /**
     * No-op. Nothing else in the codebase calls real (non-Javadoc) code on this class — Java only
     * runs the {@code static} block above on first active use, so without a genuine caller
     * somewhere, this class silently never loads and its tick handler never registers. Call this
     * once at startup to force that load; see {@code BerongSMP.commonSetup}.
     */
    public static void bootstrap() {}

    /** Ticks of continuous compliance required to count the drill as performed correctly (5s). */
    public static final int TARGET_TICKS = 100;

    private static final Map<UUID, Integer> ticksHeld = new ConcurrentHashMap<>();
    private static final Set<UUID> achievedThisHold = ConcurrentHashMap.newKeySet();

    private DuckCoverHoldManager() {}

    /** Called once per server tick from SimulationManager.onServerTick. */
    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();

            if (player.isCrouching() && facingOrBesideTable(level, player)) {
                allowCrawlUnderTable(player);
            }

            boolean compliant = player.isCrouching() && hasCoverAbove(level, player);

            if (!compliant) {
                ticksHeld.remove(id);
                achievedThisHold.remove(id);
                continue;
            }

            int elapsed = ticksHeld.merge(id, 1, Integer::sum);
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 1, false, false));

            if (elapsed >= TARGET_TICKS && achievedThisHold.add(id)) {
                onHoldAchieved(level, player);
            }
        }
    }

    /**
     * Solid block 1 OR 2 above the player's feet both count — a tall shelter overhead is real
     * cover. A real table is under a metre tall though, so a single-block {@link TableBlock}
     * can never occupy the player's own feet-relative cell above them; instead, being crouched
     * within 1 block of one (any direction, same or one block up) counts as sheltering under it.
     */
    private static boolean hasCoverAbove(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (!level.getBlockState(feet.above(1)).isAir() || !level.getBlockState(feet.above(2)).isAir()) {
            return true;
        }
        return hasNearbyTable(level, feet);
    }

    private static boolean hasNearbyTable(ServerLevel level, BlockPos feet) {
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 1, 1))) {
            if (level.getBlockState(pos).getBlock() instanceof TableBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the player is directly beside a {@link TableBlock} (any of the 4 horizontal
     * neighbours, or already standing in its footprint) or looking at one a short step ahead.
     * Deliberately stricter than {@link #hasNearbyTable} (no diagonals, no "somewhere behind
     * you") — this gates the crawl-under assist below, so it should only fire when the player is
     * clearly walking towards/into a specific table, not merely near one.
     */
    private static boolean facingOrBesideTable(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (isTable(level, feet)) {
            return true;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isTable(level, feet.relative(dir))) {
                return true;
            }
        }
        BlockPos ahead = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(1.5)));
        return isTable(level, ahead);
    }

    private static boolean isTable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof TableBlock;
    }

    /**
     * A single-block {@link TableBlock} is under a metre tall, well short of even the vanilla
     * {@code Pose.CROUCHING} hitbox (1.5 blocks) — normal collision would just stop the player at
     * its edge before {@code Player.updatePlayerPose()} ever gets a chance to shrink them further.
     * Pre-emptively forcing {@code Pose.SWIMMING} (the same shrunk hitbox vanilla uses for
     * crawling through tight gaps) while they're crouching towards/beside a table lets ordinary
     * movement carry them into its kneehole; vanilla's own per-tick pose logic takes back over
     * seamlessly once they're actually under it (or once they walk back out), so no cleanup is
     * needed here.
     */
    private static void allowCrawlUnderTable(ServerPlayer player) {
        if (player.getPose() != Pose.SWIMMING) {
            player.setPose(Pose.SWIMMING);
        }
    }

    /** Whether {@code id} currently has any continuous compliance streak going (>0 ticks held). */
    public static boolean isCompliant(UUID id) {
        return ticksHeld.containsKey(id);
    }

    /** Current continuous compliance streak length in ticks, or 0 if not currently complying. */
    public static int ticksHeld(UUID id) {
        return ticksHeld.getOrDefault(id, 0);
    }

    /**
     * The chat message and telemetry are only relevant to a real earthquake simulation session --
     * this method runs unconditionally for every crouching-with-cover player in the world (see the
     * class javadoc), so without this guard it used to fire the "Duck, Cover, and Hold maintained"
     * message during completely unrelated activity, e.g. crouching under Officer Cruz's Go/Stop
     * tunnel slabs mid-tutorial, or under Sgt. Santos's own table (which already sends its own
     * tailored completion message via SantosRoomManager -- this would have doubled up there too).
     */
    private static void onHoldAchieved(ServerLevel level, ServerPlayer player) {
        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || !session.getState().isQuake()) return;

        player.sendSystemMessage(Component.literal("§a✓ Duck, Cover, and Hold maintained — well done!"));

        session.logger.log("duck_cover_hold", Map.of(
                "x", player.getX(), "y", player.getY(), "z", player.getZ()));
        // session.elapsedSeconds() (not Config.SIM_DURATION_TICKS - timerTicks) is the correct
        // anchor — it stays right even if a GM ran /sim_time on this session, and for any scenario
        // whose duration isn't the config default.
        double elapsedS = session.elapsedSeconds();
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                Math.round(elapsedS * 100.0) / 100.0, "duck_cover_hold",
                player.getX(), player.getY(), player.getZ(), 0.0, null, null));
    }
}
