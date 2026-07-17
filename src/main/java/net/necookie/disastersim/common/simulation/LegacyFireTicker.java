package net.necookie.disastersim.common.simulation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.common.hazard.HazardManager;

/**
 * Drives the legacy Library/CCS FIRE session's per-tick world mutation — split out of
 * {@link SimulationManager} (see its class javadoc / the Master Plan's key-classes table) so that
 * class isn't three unrelated scenario state machines in one file. Own branch from
 * {@link NewSim2FireTicker} (New Sim Building 2.0 doesn't use this) and {@link EarthquakeTicker}.
 */
final class LegacyFireTicker {

    private LegacyFireTicker() {}

    static void tick(SimulationSession session, ServerLevel level,
                      ServerPlayer player, int ticks) {
        if (ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
            if (session.getState().isCCS()) {
                int spreadBefore = session.getFireSpreadCount();
                SimulationManager.FIRE_EFFECTS.spreadComputerFire(level, session);
                int spreadAfter = session.getFireSpreadCount();
                if (spreadAfter > spreadBefore) {
                    sendCcsFireSpreadAlert(player, spreadBefore, spreadAfter);
                }
            } else {
                SimulationManager.FIRE_EFFECTS.simulateFire(level, session);
            }
        }
        if (ticks % 20 == 0) {
            SimulationManager.FIRE_EFFECTS.applyFireProximityEffects(level, player);
        }
        if (ticks % 40 == 0) {
            SimulationManager.FIRE_EFFECTS.cleanupFireOutsideBounds(level, session);
        }
        if (ticks % 20 == 0) {
            session.resetExtinguishEventPending();
        }
        HazardManager.tick(level, session, ticks);
        if (session.getState().isCCS()) {
            tickCcsFireNarrative(session, player);
        }
    }

    /** Sends a one-time alert when the total spread count crosses a dramatic threshold. */
    private static void sendCcsFireSpreadAlert(ServerPlayer player, int before, int after) {
        if (before == 0) {
            player.sendSystemMessage(Component.literal("§c⚠ The electrical fire is spreading to nearby workstations!"));
        } else if (before < 4 && after >= 4) {
            player.sendSystemMessage(Component.literal("§4⚠ More computers are catching fire! Suppress them before it's too late!"));
        } else if (before < 8 && after >= 8) {
            player.sendSystemMessage(Component.literal("§4§l⚠ CRITICAL — The lab is ablaze! Use your CO2 extinguisher immediately!"));
        }
    }

    /**
     * Time-based narrative escalation for CCS fire, keyed to exact elapsed ticks.
     *
     * <p>Uses {@link SimulationSession#elapsedTicks()} rather than {@code Config.SIM_DURATION_TICKS
     * - ticks} — the latter silently drifts wrong the moment a GM runs {@code /sim_time} on the
     * session, since it assumes the session's total duration always equals the config default.
     */
    private static void tickCcsFireNarrative(SimulationSession session, ServerPlayer player) {
        int elapsed = session.elapsedTicks();
        if (elapsed == 20 * 15) {
            player.sendSystemMessage(Component.literal("§e[15s] The fire is still active — locate the burning computer!"));
        } else if (elapsed == 20 * 30) {
            player.sendSystemMessage(Component.literal("§c[30s] Electrical fires spread fast — check all workstations in the lab!"));
        } else if (elapsed == 20 * 55) {
            player.sendSystemMessage(Component.literal("§4[55s] ⚠ The fire has been burning for nearly a minute. Multiple stations may be at risk!"));
        } else if (elapsed == 20 * 90) {
            player.sendSystemMessage(Component.literal("§4§l[90s] DANGER — If you cannot control the fire, evacuate to the assembly area!"));
        }
    }
}
