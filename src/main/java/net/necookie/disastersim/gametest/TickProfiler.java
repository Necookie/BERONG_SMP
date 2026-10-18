package net.necookie.disastersim.gametest;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures how long each server tick takes (start of tick to end of tick, all mod handlers
 * included). A server keeps 20 ticks per second as long as a tick stays under 50 ms. The game-test
 * server does not sleep between ticks, so this is pure processing time.
 */
final class TickProfiler {

    private static long tickStart;
    private static final List<Double> TICK_MS = new ArrayList<>();
    private static boolean installed;

    private TickProfiler() {}

    static synchronized void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> tickStart = System.nanoTime());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (tickStart != 0) TICK_MS.add((System.nanoTime() - tickStart) / 1_000_000.0);
        });
    }

    /** Index to measure from. */
    static int mark() { return TICK_MS.size(); }

    /** Tick durations (ms) recorded since {@code mark}. */
    static List<Double> since(int mark) { return new ArrayList<>(TICK_MS.subList(mark, TICK_MS.size())); }

    record Stats(int n, double median, double p95, double max, double mean) {
        String text() {
            return String.format("median %.2f ms, p95 %.2f ms, max %.2f ms over %d ticks", median, p95, max, n);
        }
    }

    static Stats stats(List<Double> xs) {
        List<Double> s = new ArrayList<>(xs);
        s.sort(Double::compare);
        if (s.isEmpty()) return new Stats(0, 0, 0, 0, 0);
        double mean = s.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new Stats(s.size(), s.get(s.size() / 2), s.get(Math.min(s.size() - 1, (int) Math.ceil(0.95 * s.size()) - 1)),
                s.get(s.size() - 1), mean);
    }
}
