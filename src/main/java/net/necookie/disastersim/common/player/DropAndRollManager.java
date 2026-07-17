package net.necookie.disastersim.common.player;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.necookie.disastersim.registry.ModAttachments;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Stop, drop, and roll": pressing the drop-and-roll key while on fire knocks a chunk off the
 * player's remaining fire ticks and drops them into a brief "dropped" window during which they're
 * slowed (a stand-in for crawling low instead of standing and walking normally). Repeated presses
 * both keep extinguishing and extend the window, so continuing to "roll" reads as one continuous
 * action rather than a single fixed animation.
 *
 * <p>Transient per-player state, matching the rest of the mod's idiom for state that doesn't need
 * to survive a server restart (see {@code TutorialManager.holdOnTimers}).
 */
public final class DropAndRollManager {

    private static final int FIRE_TICKS_REDUCED_PER_PRESS = 30; // 1.5s worth, vs. vanilla's 1/tick decay
    private static final int DROPPED_WINDOW_TICKS = 100;        // 5s

    // "The fire must always stick to the player unless they drop and roll" — once a player catches
    // fire during an active fire-type simulation, membership here persists (independent of
    // Player.getRemainingFireTicks(), which vanilla can zero out on its own via water contact)
    // until they either start a drop-and-roll or leave the simulation. Stronger-than-vanilla damage
    // ticks alongside it. See #tick's sticky-fire loop.
    private static final Set<UUID> stickyBurning = ConcurrentHashMap.newKeySet();
    private static final int SIM_FIRE_TICK_FLOOR = 60;            // 3s — re-topped every tick, so this is really "never runs out"
    private static final int SIM_FIRE_DAMAGE_INTERVAL_TICKS = 20; // 1s, matches vanilla's own on-fire damage cadence
    private static final float SIM_FIRE_DAMAGE = 3.0f;            // vanilla's own on-fire tick is 1.0 — this is meant to hurt

    private static final Map<UUID, Integer> droppedTicksRemaining = new ConcurrentHashMap<>();

    /**
     * {@link net.necookie.disastersim.network.DropAndRollPayload} is client-triggered and carries
     * no server-side rate limit of its own — the client only ever sends it from
     * {@code KeyMapping.consumeClick()} (edge-triggered, so a real keyboard can't fire it faster
     * than the OS key-repeat rate), but nothing stops a modified client from calling {@code
     * ClientPacketDistributor.sendToServer} directly in a loop. Every other player-triggered action
     * in this codebase throttles itself (e.g. {@code AbstractExtinguisherItem}'s wrong-tool
     * warning, {@code CruzRoomManager}'s starting-line nudge); this one didn't. The actual per-call
     * cost is small (a map write, an attachment sync, one MobEffectInstance, a fixed 12-particle
     * burst) so this isn't a severe DoS, but an unthrottled flood would still mean unnecessary
     * repeated particle broadcasts to every nearby player every single tick.
     */
    private static final int MIN_REQUEST_INTERVAL_TICKS = 2;
    private static final Map<UUID, Long> lastRequestTick = new ConcurrentHashMap<>();

    static {
        TickScheduler.register(DropAndRollManager::tick);
        PlayerLifecycleRegistry.registerLogoutHook(player -> {
            stickyBurning.remove(player.getUUID());
            lastRequestTick.remove(player.getUUID());
        });
    }

    private DropAndRollManager() {}

    public static void onDropAndRollRequest(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long last = lastRequestTick.get(id);
        if (last != null && now - last < MIN_REQUEST_INTERVAL_TICKS) return;
        lastRequestTick.put(id, now);

        boolean wasOnFire = player.getRemainingFireTicks() > 0;

        if (wasOnFire) {
            int reduced = Math.max(0, player.getRemainingFireTicks() - FIRE_TICKS_REDUCED_PER_PRESS);
            player.setRemainingFireTicks(reduced);
            if (reduced == 0) player.clearFire();
        }

        // Only ever enters the dropped state from an on-fire press; once in it, further presses
        // (even after the fire ticks reach 0 mid-roll) extend the window.
        if (wasOnFire || droppedTicksRemaining.containsKey(id)) {
            droppedTicksRemaining.put(id, DROPPED_WINDOW_TICKS);
            player.setData(ModAttachments.DROPPED_TICKS.get(), DROPPED_WINDOW_TICKS);
            applyCrawlEffects(player);
            spawnRollFeedback(player);
        }
    }

    /** Called once per server tick from SimulationManager.onServerTick. */
    public static void tick(ServerLevel level) {
        tickStickyFire(level);

        if (droppedTicksRemaining.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> it = droppedTicksRemaining.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                player.setData(ModAttachments.DROPPED_TICKS.get(), 0);
                it.remove();
                continue;
            }
            entry.setValue(remaining);
            player.setData(ModAttachments.DROPPED_TICKS.get(), remaining);
            applyCrawlEffects(player);
        }
    }

    /**
     * Drives the "fire sticks until you actually drop and roll" rule: while a player is inside an
     * active fire-type {@code SimulationSession} and not currently in the dropped/rolling window,
     * catching fire (from touching a real fire block, a hazard failure, etc.) keeps them burning —
     * re-topped every tick regardless of what vanilla's own water-contact/decay logic just did to
     * {@code remainingFireTicks} — with a heavier damage tick than vanilla's default. Starting a
     * drop-and-roll ({@link #isDropped}) immediately exempts the player for as long as the window
     * lasts; leaving the fire-type session (or logging out, via the class-load static hook) drops
     * them from tracking entirely.
     */
    private static void tickStickyFire(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            SimulationSession session = SimulationManager.getSession(id);
            boolean inFireSim = session != null && session.getState().isFire();
            if (!inFireSim || isDropped(id)) {
                stickyBurning.remove(id);
                continue;
            }
            if (player.getRemainingFireTicks() > 0) {
                stickyBurning.add(id);
            }
            if (!stickyBurning.contains(id)) continue;

            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), SIM_FIRE_TICK_FLOOR));
            if (level.getGameTime() % SIM_FIRE_DAMAGE_INTERVAL_TICKS == 0) {
                player.hurt(player.damageSources().onFire(), SIM_FIRE_DAMAGE);
            }
        }
    }

    public static boolean isDropped(UUID id) {
        return droppedTicksRemaining.containsKey(id);
    }

    private static void applyCrawlEffects(ServerPlayer player) {
        // Refreshed every tick while dropped so it never expires mid-window.
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
    }

    private static void spawnRollFeedback(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.1, player.getZ(),
                12, 0.4, 0.05, 0.4, 0.02);
        // No sound here anymore — GENERIC_EXTINGUISH_FIRE at pitch 1.2 fired on every single press
        // (mashing R = a rapid, jarring pop repeated many times); the particles alone are enough
        // feedback for the roll.
    }
}
