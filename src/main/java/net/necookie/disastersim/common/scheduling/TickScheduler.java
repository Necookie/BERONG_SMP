package net.necookie.disastersim.common.scheduling;

import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.List;
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

    /** Called once per server tick from {@code SimulationManager.onServerTick}. */
    public static void tick(ServerLevel level) {
        for (Consumer<ServerLevel> handler : HANDLERS) {
            handler.accept(level);
        }
        if (!DELAYED.isEmpty()) {
            Iterator<DelayedTask> it = DELAYED.iterator();
            while (it.hasNext()) {
                DelayedTask delayed = it.next();
                if (--delayed.remaining <= 0) {
                    DELAYED.remove(delayed);
                    delayed.task.accept(level);
                }
            }
        }
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
