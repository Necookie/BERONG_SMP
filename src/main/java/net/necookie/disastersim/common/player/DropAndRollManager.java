package net.necookie.disastersim.common.player;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.scheduling.TickScheduler;

import java.util.Iterator;
import java.util.Map;
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

    static {
        TickScheduler.register(DropAndRollManager::tick);
    }

    private static final int FIRE_TICKS_REDUCED_PER_PRESS = 30; // 1.5s worth, vs. vanilla's 1/tick decay
    private static final int DROPPED_WINDOW_TICKS = 100;        // 5s

    private static final Map<UUID, Integer> droppedTicksRemaining = new ConcurrentHashMap<>();

    private DropAndRollManager() {}

    public static void onDropAndRollRequest(ServerPlayer player) {
        UUID id = player.getUUID();
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
            player.setData(BerongSMP.DROPPED_TICKS.get(), DROPPED_WINDOW_TICKS);
            applyCrawlEffects(player);
            spawnRollFeedback(player);
        }
    }

    /** Called once per server tick from SimulationManager.onServerTick. */
    public static void tick(ServerLevel level) {
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
                player.setData(BerongSMP.DROPPED_TICKS.get(), 0);
                it.remove();
                continue;
            }
            entry.setValue(remaining);
            player.setData(BerongSMP.DROPPED_TICKS.get(), remaining);
            applyCrawlEffects(player);
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
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                SoundSource.PLAYERS, 1.0f, 1.2f);
    }
}
