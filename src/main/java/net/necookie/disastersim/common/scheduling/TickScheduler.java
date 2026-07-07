package net.necookie.disastersim.common.scheduling;

import net.minecraft.server.level.ServerLevel;

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

    private TickScheduler() {}

    /** Registers a handler to run once per server tick, in registration order. */
    public static void register(Consumer<ServerLevel> handler) {
        HANDLERS.add(handler);
    }

    /** Called once per server tick from {@code SimulationManager.onServerTick}. */
    public static void tick(ServerLevel level) {
        for (Consumer<ServerLevel> handler : HANDLERS) {
            handler.accept(level);
        }
    }
}
