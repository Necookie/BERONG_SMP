package net.necookie.disastersim.player;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.block.TableBlock;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TelemetryCsvWriter;

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
 * <p>Transient per-player state (see {@link DropAndRollManager}'s own javadoc for why this idiom
 * doesn't need to survive a server restart).
 */
public final class DuckCoverHoldManager {

    /** Ticks of continuous compliance required to count the drill as performed correctly (5s). */
    private static final int TARGET_TICKS = 100;

    private static final Map<UUID, Integer> ticksHeld = new ConcurrentHashMap<>();
    private static final Set<UUID> achievedThisHold = ConcurrentHashMap.newKeySet();

    private DuckCoverHoldManager() {}

    /** Called once per server tick from SimulationManager.onServerTick. */
    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
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

    private static void onHoldAchieved(ServerLevel level, ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§a✓ Duck, Cover, and Hold maintained — well done!"));

        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || !session.getState().isQuake()) return;

        session.logger.log("duck_cover_hold", Map.of(
                "x", player.getX(), "y", player.getY(), "z", player.getZ()));
        double elapsedS = (double) (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                Math.round(elapsedS * 100.0) / 100.0, "duck_cover_hold",
                player.getX(), player.getY(), player.getZ(), 0.0, null, null));
    }
}
