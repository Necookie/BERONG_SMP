package net.necookie.disastersim.common.simulation;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Buffers per-tick and action events during a simulation; serialized to JSON on session end. */
public class SimulationEventLogger {

    public record SimEvent(String type, long tOffsetMs, Map<String, Object> data) {}

    private static final Gson GSON = new Gson();

    private final List<SimEvent> events = new ArrayList<>();
    private final long startMs = System.currentTimeMillis();

    public void log(String type, Map<String, Object> data) {
        events.add(new SimEvent(type, System.currentTimeMillis() - startMs, data));
    }

    public List<SimEvent> getEvents() { return events; }

    public String toJson() { return GSON.toJson(events); }
}
