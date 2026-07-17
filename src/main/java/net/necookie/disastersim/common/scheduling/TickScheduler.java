package net.necookie.disastersim.common.scheduling;

import net.minecraft.server.level.ServerLevel;
import net.necookie.disastersim.BerongSMP;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Single registration point for the per-level tick handlers that {@code SimulationManager}'s
 * {@code onServerTick} used to call directly (TutorialManager, DropAndRollManager,
 * DuckCoverHoldManager, AcademyManager). Each of those independently calls
 * {@code level.getServer().getPlayerList().getPlayers()} and iterates it, so today's dispatch is
 * still 4 separate full player-list fetches per tick — this class only replaces the 4 hardcoded
 * call sites with a registry, preserving both the exact call order and the per-handler iteration.
 *
 * <p>Collapsing those 4 iterations into one (each handler exposing a per-player entry point
 * instead of owning its own player loop) is a real follow-up optimization, deliberately not done
 * here: it means restructuring 4 classes whose tick methods carry a lot of timing-sensitive logic
 * accumulated over many bug-fix passes (see the tutorial/Academy history in CLAUDE.md), and that
 * kind of behavioral change needs a live-server test pass this refactor didn't include.
 */
public final class TickScheduler {

    private static final List<Consumer<ServerLevel>> HANDLERS = new CopyOnWriteArrayList<>();

    /** One-shot delayed tasks queued via {@link #scheduleOnce}, decremented/fired from {@link #tick}. */
    private static final List<DelayedTask> DELAYED = new CopyOnWriteArrayList<>();

    /** Throttles repeated-failure log spam: at most one warning per handler per this many ticks. */
    private static final long ERROR_LOG_THROTTLE_TICKS = 200; // 10s
    /** Server-thread only (same contract as {@link #tick}), so plain IdentityHashMap is safe. */
    private static final Map<Object, Long> lastErrorLogTick = new IdentityHashMap<>();

    private TickScheduler() {}

    /** Registers a handler to run once per server tick, in registration order. */
    public static void register(Consumer<ServerLevel> handler) {
        HANDLERS.add(handler);
    }

    /**
     * Runs {@code task} once, {@code delayTicks} ticks from now (20 ticks = 1 second). Server-thread
     * only, same contract as {@link #tick} itself — no extra synchronisation needed. The task must
     * re-fetch any player it needs by UUID at fire time rather than capturing a live reference,
     * since the player may have logged out (or started something else) during the wait.
     */
    public static void scheduleOnce(int delayTicks, Consumer<ServerLevel> task) {
        DELAYED.add(new DelayedTask(delayTicks, task));
    }

    /**
     * Called once per server tick from {@code SimulationManager.onServerTick}. Each handler, and
     * each delayed task that fires this tick, runs inside its own try/catch — an uncaught
     * exception from any one subsystem (Academy, DropAndRoll, DuckCoverHold, SafetyDevice, a
     * scheduled Morfe hand-off, ...) used to propagate straight out of this method and take down
     * the whole server, mid-class, over a bug in a single unrelated feature. A handler that throws
     * every tick is logged at most once every {@link #ERROR_LOG_THROTTLE_TICKS} ticks rather than
     * flooding the log.
     */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Consumer<ServerLevel> handler : HANDLERS) {
            try {
                handler.accept(level);
            } catch (Exception e) {
                logThrottled(handler, now, e);
            }
        }
        if (!DELAYED.isEmpty()) {
            Iterator<DelayedTask> it = DELAYED.iterator();
            while (it.hasNext()) {
                DelayedTask delayed = it.next();
                if (--delayed.remaining <= 0) {
                    DELAYED.remove(delayed);
                    try {
                        delayed.task.accept(level);
                    } catch (Exception e) {
                        BerongSMP.LOGGER.error("[TickScheduler] Delayed task threw — skipped: {}", e.toString(), e);
                    }
                }
            }
        }
    }

    private static void logThrottled(Object handler, long now, Exception e) {
        Long last = lastErrorLogTick.get(handler);
        if (last != null && now - last < ERROR_LOG_THROTTLE_TICKS) return;
        lastErrorLogTick.put(handler, now);
        BerongSMP.LOGGER.error("[TickScheduler] Tick handler {} threw — skipped this tick: {}",
                handler.getClass().getName(), e.toString(), e);
    }

    private static final class DelayedTask {
        int remaining;
        final Consumer<ServerLevel> task;

        DelayedTask(int remaining, Consumer<ServerLevel> task) {
            this.remaining = remaining;
            this.task = task;
        }
    }
}
