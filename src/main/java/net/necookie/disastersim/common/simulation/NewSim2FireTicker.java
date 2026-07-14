package net.necookie.disastersim.common.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.common.hazard.HazardBlock;
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;

import java.util.UUID;

/**
 * Drives New Sim Building 2.0's prevention→intervention→evacuation state machine — split out of
 * {@link SimulationManager} (see its class javadoc). Own branch from {@link LegacyFireTicker}
 * (the organic random-escalation {@code HazardManager.tick} drives is deliberately not used here)
 * and {@link EarthquakeTicker}. Returns true once the run has ended (caller should {@code continue}
 * the outer loop, matching {@code SimulationManager#tickAssemblyZone}'s convention) —
 * {@code tickFirePhase()} is pure bookkeeping, so all the actual world mutation (igniting unfound
 * hazards, letting contained fires "go big", ending the session) happens here on the phase-change
 * edge, exactly like {@link EarthquakeTicker} does for quake phases.
 */
final class NewSim2FireTicker {

    private NewSim2FireTicker() {}

    static boolean tick(SimulationSession session, UUID uuid,
                         ServerLevel level, ServerPlayer player, int ticks) {
        SimulationSession.FirePhase before = session.getFirePhase();
        session.tickFirePhase();
        SimulationSession.FirePhase after = session.getFirePhase();

        if (before != after) {
            if (after == SimulationSession.FirePhase.INTERVENTION) {
                for (BlockPos pos : session.getArmedHazards()) {
                    if (!session.getPreventedHazards().contains(pos)) {
                        HazardManager.forceFailure(level, session, pos, player);
                        session.recordEscalated(pos);
                        // Catch the 2 nearest wood-plank blocks (not just an adjacent air cell) so
                        // the fire has real fuel and actually spreads through the room, instead of
                        // risking a lone fire block fizzling out on a stone/tile floor.
                        for (BlockPos lit : HazardBlock.igniteNearestFlammable(level, pos, 2, 6)) {
                            session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                                    session.elapsedSeconds(), lit.getX(), lit.getY(), lit.getZ(), "ignite"));
                            session.addFireSource(lit);
                        }
                    }
                }
                player.sendSystemMessage(Component.literal(String.format(
                        "§c⚠ Time's up! %d hazard(s) you missed have caught fire — grab the correct extinguisher!",
                        session.getEscalatedHazards().size())));
                SimulationManager.emitPhaseTransition(session, uuid, player, "intervention");
            } else if (after == SimulationSession.FirePhase.EVACUATION) {
                for (BlockPos pos : session.getEscalatedHazards()) {
                    if (!session.getExtinguishedEscalated().contains(pos)) {
                        for (BlockPos lit : HazardBlock.igniteRadius(level, pos, 3, 10)) {
                            session.bufferFireLogRow(TelemetryCsvWriter.writeFireLogRow(session.getSessionId(),
                                    session.elapsedSeconds(), lit.getX(), lit.getY(), lit.getZ(), "ignite"));
                            session.addFireSource(lit);
                        }
                    }
                }
                player.sendSystemMessage(Component.literal(
                        "§4§l⚠ The fire is out of control — EVACUATE to the assembly area now!"));
                SimulationManager.emitPhaseTransition(session, uuid, player, "evacuation");
            } else if (after == SimulationSession.FirePhase.END) {
                String endReason = before == SimulationSession.FirePhase.PREVENTION
                        ? "all_hazards_prevented" : "intervention_success";
                player.sendSystemMessage(Component.literal(
                        "all_hazards_prevented".equals(endReason)
                                ? "§a✓ Every hazard prevented before it ever caught fire!"
                                : "§a✓ Every fire contained — nice work!"));
                SimulationManager.endSimulation(uuid, endReason);
                return true;
            }
        }

        // Real arena-wide fire spread only kicks in once evacuation has actually begun.
        if (session.getFirePhase() == SimulationSession.FirePhase.EVACUATION
                && ticks % Config.FIRE_SPAWN_INTERVAL.get() == 0) {
            SimulationManager.FIRE_EFFECTS.simulateFire(level, session);
        }
        if (ticks % 20 == 0) {
            SimulationManager.FIRE_EFFECTS.applyFireProximityEffects(level, player);
            session.resetExtinguishEventPending();
        }
        if (ticks % 40 == 0) {
            SimulationManager.FIRE_EFFECTS.cleanupFireOutsideBounds(level, session);
        }
        return false;
    }
}
