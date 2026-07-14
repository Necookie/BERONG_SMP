package net.necookie.disastersim.common.simulation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.necookie.disastersim.Config;

/**
 * Drives the earthquake session's per-tick world mutation and phase-transition chat lines — split
 * out of {@link SimulationManager} (see its class javadoc). Own branch from
 * {@link LegacyFireTicker}/{@link NewSim2FireTicker}.
 */
final class EarthquakeTicker {

    private EarthquakeTicker() {}

    static void tick(SimulationSession session, ServerLevel level,
                      ServerPlayer player, int ticks) {
        SimulationSession.EarthquakePhase phaseBefore = session.getQuakePhase();
        session.tickQuakePhase(level.getRandom());
        SimulationSession.EarthquakePhase phaseAfter = session.getQuakePhase();
        if (phaseBefore != phaseAfter) {
            if (phaseAfter == SimulationSession.EarthquakePhase.PEAK) {
                player.sendSystemMessage(Component.literal("§c⚠ Earthquake is intensifying!"));
            } else if (phaseAfter == SimulationSession.EarthquakePhase.AFTERSHOCK) {
                player.sendSystemMessage(Component.literal("§e⚠ Aftershock!"));
            } else if (phaseAfter == SimulationSession.EarthquakePhase.END) {
                player.sendSystemMessage(Component.literal("§a✓ The shaking has stopped."));
            }
        }
        if (ticks % 60 == 0 && session.getQuakePhase() != SimulationSession.EarthquakePhase.END) {
            int nauseaAmp = switch (session.getQuakePhase()) {
                case PEAK       -> (int) Math.min(3, session.getSessionMagnitude() / 2.5);
                case AFTERSHOCK -> (int) Math.min(2,
                        session.getSessionMagnitude() * session.getAftershockMagnitudeScale() / 3.0);
                default         -> 0;
            };
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, nauseaAmp, false, true));
        }
        if (ticks % Config.QUAKE_INTERVAL.get() == 0) {
            SimulationManager.EARTHQUAKE_EFFECTS.simulateEarthquake(level, session);
        }
        SimulationManager.EARTHQUAKE_EFFECTS.drainEarthquakePending(level, session);
        if (ticks % 20 == 0) {
            SimulationManager.FIRE_EFFECTS.clearFireInArena(level, session);
        }
    }
}
