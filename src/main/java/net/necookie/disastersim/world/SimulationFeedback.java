package net.necookie.disastersim.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Computes and delivers adaptive, rule-based feedback to a player at the end
 * of a simulation.  All analysis is performed server-side using the buffered
 * {@link SimulationEventLogger} — no external API call is needed.
 *
 * <p>Currently supports FIRE sessions (extinguisher mechanics).  EARTHQUAKE
 * sessions receive a simpler summary via {@link #sendQuake}.
 */
public class SimulationFeedback {

    // Decision-delay threshold (seconds) above which a "slow response" tip fires.
    private static final double SLOW_DECISION_THRESHOLD_S = 30.0;
    private static final double POOR_ACCURACY_THRESHOLD   = 0.40;
    private static final double HIGH_PANIC_THRESHOLD      = 2.0;
    private static final double LOW_EFFICIENCY_THRESHOLD  = 0.50;

    private SimulationFeedback() {}

    // -----------------------------------------------------------------------
    // Public entry points
    // -----------------------------------------------------------------------

    public static void sendFire(ServerPlayer player, SimulationEventLogger logger, int score) {
        List<SimulationEventLogger.SimEvent> events = logger.getEvents();

        double delay    = computeDecisionDelay(events);
        double accuracy = computeSprayAccuracy(events);
        double panic    = computePanicProxy(events);
        double path     = computePathEfficiency(events);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6--- Fire Drill Feedback ---"));

        // Decision delay
        if (delay > SLOW_DECISION_THRESHOLD_S) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Slow response: §fyou took " + (int) delay +
                "s to pull the pin. In a real fire, act within 10 seconds."));
        } else if (delay > 0) {
            player.sendSystemMessage(Component.literal(
                "§a✓ Good response time: §fpulled the pin in " + (int) delay + "s."));
        }

        // Spray accuracy
        if (accuracy < POOR_ACCURACY_THRESHOLD) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Low accuracy (" + Math.round(accuracy * 100) +
                "%): §faim at the §lbase§r§f of the flame, not the smoke above it."));
        } else {
            player.sendSystemMessage(Component.literal(
                "§a✓ Spray accuracy: §f" + Math.round(accuracy * 100) + "%"));
        }

        // Panic proxy (erratic movement)
        if (panic > HIGH_PANIC_THRESHOLD) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Erratic movement detected: §fstay calm and move deliberately toward the exit."));
        }

        // Path efficiency
        if (path < LOW_EFFICIENCY_THRESHOLD && path > 0) {
            player.sendSystemMessage(Component.literal(
                "§c⚠ Inefficient path: §favoid backtracking in smoke — " +
                "know your exit before the fire starts."));
        }

        player.sendSystemMessage(Component.literal("§7Tip: Remember PASS — Pull, Aim at the base, Squeeze, Sweep."));
        player.sendSystemMessage(Component.literal("§eScore: §f" + score + " / 100"));
        player.sendSystemMessage(Component.literal("§6----------------------------"));
    }

    public static void sendQuake(ServerPlayer player, int score) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6--- Earthquake Drill Feedback ---"));
        player.sendSystemMessage(Component.literal("§7Tip: DROP → COVER → HOLD ON until shaking stops."));
        player.sendSystemMessage(Component.literal("§7Avoid windows, shelves, and unsupported structures."));
        player.sendSystemMessage(Component.literal("§eScore: §f" + score + " / 100"));
        player.sendSystemMessage(Component.literal("§6----------------------------------"));
    }

    // -----------------------------------------------------------------------
    // Feature extractors
    // -----------------------------------------------------------------------

    private static double computeDecisionDelay(List<SimulationEventLogger.SimEvent> events) {
        long simStart = events.stream()
            .filter(e -> "SIM_START".equals(e.type()))
            .mapToLong(SimulationEventLogger.SimEvent::tOffsetMs)
            .findFirst().orElse(0L);
        long pinPull = events.stream()
            .filter(e -> "EXT_PIN_PULL".equals(e.type()))
            .mapToLong(SimulationEventLogger.SimEvent::tOffsetMs)
            .findFirst().orElse(-1L);
        return pinPull < 0 ? 999.0 : (pinPull - simStart) / 1000.0;
    }

    private static double computeSprayAccuracy(List<SimulationEventLogger.SimEvent> events) {
        long hits  = events.stream()
            .filter(e -> "EXT_SPRAY".equals(e.type()))
            .filter(e -> Boolean.TRUE.equals(e.data().get("hit_fire")))
            .count();
        long total = events.stream()
            .filter(e -> "EXT_SPRAY".equals(e.type()))
            .count();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    private static double computePanicProxy(List<SimulationEventLogger.SimEvent> events) {
        List<SimulationEventLogger.SimEvent> ticks = events.stream()
            .filter(e -> "PLAYER_TICK".equals(e.type()))
            .toList();
        if (ticks.size() < 2) return 0.0;
        double[] speeds = new double[ticks.size() - 1];
        for (int i = 1; i < ticks.size(); i++) {
            double dx = getDouble(ticks.get(i), "x")     - getDouble(ticks.get(i - 1), "x");
            double dz = getDouble(ticks.get(i), "z")     - getDouble(ticks.get(i - 1), "z");
            speeds[i - 1] = dx * dx + dz * dz;
        }
        double mean = Arrays.stream(speeds).average().orElse(0.0);
        return Math.sqrt(Arrays.stream(speeds)
            .map(s -> (s - mean) * (s - mean))
            .average().orElse(0.0));
    }

    private static double computePathEfficiency(List<SimulationEventLogger.SimEvent> events) {
        List<SimulationEventLogger.SimEvent> ticks = events.stream()
            .filter(e -> "PLAYER_TICK".equals(e.type()))
            .toList();
        if (ticks.size() < 2) return 1.0;
        SimulationEventLogger.SimEvent first = ticks.get(0);
        SimulationEventLogger.SimEvent last  = ticks.get(ticks.size() - 1);
        double straightDist = Math.sqrt(
            Math.pow(getDouble(last, "x") - getDouble(first, "x"), 2) +
            Math.pow(getDouble(last, "z") - getDouble(first, "z"), 2));
        double cumDist = 0;
        for (int i = 1; i < ticks.size(); i++) {
            double dx = getDouble(ticks.get(i), "x") - getDouble(ticks.get(i - 1), "x");
            double dz = getDouble(ticks.get(i), "z") - getDouble(ticks.get(i - 1), "z");
            cumDist += Math.sqrt(dx * dx + dz * dz);
        }
        return cumDist == 0 ? 1.0 : straightDist / cumDist;
    }

    private static double getDouble(SimulationEventLogger.SimEvent e, String key) {
        Object v = e.data().get(key);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
}
