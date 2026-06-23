package net.necookie.disastersim.world;

import net.necookie.disastersim.BerongSMP;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes per-tick and event rows to a local CSV file conforming to
 * telemetry_contract.md v1.1 §3.
 *
 * File layout (all under run/telemetry/):
 *   gameplay_logs_<YYYYMMDD>.csv  — one row per event / move sample
 *   sessions_<YYYYMMDD>.csv       — one row per completed session (§5 metadata)
 *   map_metadata.json             — one-time static file written on first init
 *
 * All I/O is synchronous on the server thread. The writer is buffered (8 KB)
 * and flushed on session close, so disk writes are infrequent.
 */
public class TelemetryCsvWriter {

    private static final String CONTRACT_VERSION = "1.1";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static Path telemetryDir;
    private static BufferedWriter eventWriter;
    private static BufferedWriter sessionWriter;
    private static boolean eventHeaderWritten = false;
    private static boolean sessionHeaderWritten = false;

    private static final ConcurrentHashMap<String, Boolean> openSessions = new ConcurrentHashMap<>();

    public static void init(Path runDir) {
        try {
            telemetryDir = runDir.resolve("telemetry");
            Files.createDirectories(telemetryDir);

            Path metaFile = telemetryDir.resolve("map_metadata.json");
            if (!Files.exists(metaFile)) {
                writeMapMetadata(metaFile);
            }

            openEventWriter();
            openSessionWriter();

            BerongSMP.LOGGER.info("[TelemetryCsvWriter] Initialized telemetry dir: {}", telemetryDir);
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] Failed to initialize: {}", e.getMessage());
        }
    }

    public static void openSession(String sessionId) {
        if (telemetryDir == null) return;
        openSessions.put(sessionId, true);
    }

    public static void closeSession(String sessionId, Map<String, Object> metadata) {
        if (telemetryDir == null) return;
        openSessions.remove(sessionId);
        try {
            ensureSessionWriter();
            StringBuilder sb = new StringBuilder();
            sb.append(csv(sessionId));
            sb.append(',').append(csv(metadata.getOrDefault("player_id", "")));
            sb.append(',').append(csv(metadata.getOrDefault("scenario_type", "")));
            sb.append(',').append(csv(metadata.getOrDefault("started_at", "")));
            sb.append(',').append(csv(metadata.getOrDefault("ended_at", "")));
            sb.append(',').append(metadata.getOrDefault("duration_ticks", ""));
            sb.append(',').append(csv(metadata.getOrDefault("end_reason", "")));
            sb.append(',').append(metadata.getOrDefault("fires_extinguished_count", ""));
            sb.append(',').append(metadata.getOrDefault("magnitude", ""));
            sb.append(',').append(metadata.getOrDefault("aftershock_count", ""));
            sb.append(',').append(metadata.getOrDefault("aftershock_magnitude_scale", ""));
            sb.append(',').append(csv(metadata.getOrDefault("final_earthquake_phase", "")));
            sb.append(',').append(CONTRACT_VERSION);
            sessionWriter.write(sb.toString());
            sessionWriter.newLine();
            sessionWriter.flush();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] closeSession error: {}", e.getMessage());
        }
    }

    public static void writeRow(String sessionId, String playerId, String scenarioType,
                                double timestamp, String eventType,
                                double x, double y, double z,
                                double hazardDistance,
                                String interactionTarget, Integer nearbyPlayerCount) {
        if (telemetryDir == null) return;
        try {
            ensureEventWriter();
            StringBuilder sb = new StringBuilder();
            sb.append(csv(playerId)).append(',');
            sb.append(csv(sessionId)).append(',');
            sb.append(csv(scenarioType)).append(',');
            sb.append(round2(timestamp)).append(',');
            sb.append(csv(eventType)).append(',');
            sb.append(round3(x)).append(',');
            sb.append(round3(y)).append(',');
            sb.append(round3(z)).append(',');
            sb.append(round2(hazardDistance)).append(',');
            sb.append(interactionTarget != null ? csv(interactionTarget) : "").append(',');
            sb.append(nearbyPlayerCount != null ? nearbyPlayerCount : "");
            eventWriter.write(sb.toString());
            eventWriter.newLine();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] writeRow error: {}", e.getMessage());
        }
    }

    public static void flush() {
        try {
            if (eventWriter != null) eventWriter.flush();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] flush error: {}", e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            if (eventWriter != null)   { eventWriter.flush();   eventWriter.close(); }
            if (sessionWriter != null) { sessionWriter.flush(); sessionWriter.close(); }
        } catch (IOException ignored) {}
    }

    private static void openEventWriter() throws IOException {
        String date = LocalDate.now().format(DATE_FMT);
        Path file = telemetryDir.resolve("gameplay_logs_" + date + ".csv");
        boolean isNew = !Files.exists(file);
        eventWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file.toFile(), true), StandardCharsets.UTF_8), 8192);
        if (isNew) {
            eventWriter.write("player_id,session_id,scenario_type,timestamp,event_type," +
                    "x,y,z,hazard_distance,interaction_target,nearby_player_count");
            eventWriter.newLine();
            eventHeaderWritten = true;
        }
    }

    private static void openSessionWriter() throws IOException {
        String date = LocalDate.now().format(DATE_FMT);
        Path file = telemetryDir.resolve("sessions_" + date + ".csv");
        boolean isNew = !Files.exists(file);
        sessionWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file.toFile(), true), StandardCharsets.UTF_8), 4096);
        if (isNew) {
            sessionWriter.write("session_id,player_id,scenario_type,started_at,ended_at," +
                    "duration_ticks,end_reason,fires_extinguished_count,magnitude," +
                    "aftershock_count,aftershock_magnitude_scale,final_earthquake_phase," +
                    "contract_version");
            sessionWriter.newLine();
            sessionHeaderWritten = true;
        }
    }

    private static void ensureEventWriter() throws IOException {
        if (eventWriter == null) openEventWriter();
    }

    private static void ensureSessionWriter() throws IOException {
        if (sessionWriter == null) openSessionWriter();
    }

    private static String csv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private static void writeMapMetadata(Path file) throws IOException {
        String json = "{\n" +
            "  \"contract_version\": \"" + CONTRACT_VERSION + "\",\n" +
            "  \"sim_pos\": {\"x\": 30, \"y\": -34, \"z\": 83},\n" +
            "  \"scenarios\": {\n" +
            "    \"fire\": {\n" +
            "      \"exits\": [\n" +
            "        {\"label\": \"main_exit\", \"x\": 0, \"y\": 0, \"z\": 0, \"note\": \"PLACEHOLDER — tune with F3 in-game\"}\n" +
            "      ],\n" +
            "      \"assembly_area\": {\n" +
            "        \"min\": {\"x\": 30, \"y\": -35, \"z\": 96},\n" +
            "        \"max\": {\"x\": 76, \"y\": -28, \"z\": 115},\n" +
            "        \"note\": \"PLACEHOLDER — matches AssemblyZone.ZONE (front/Z+ face), tune after runServer\"\n" +
            "      },\n" +
            "      \"fire_alarm_positions\": [\n" +
            "        {\"x\": 0, \"y\": 0, \"z\": 0, \"note\": \"PLACEHOLDER — set after placing FireAlarmBlock in-game\"}\n" +
            "      ],\n" +
            "      \"extinguisher_positions\": [\n" +
            "        {\"x\": 0, \"y\": 0, \"z\": 0, \"note\": \"PLACEHOLDER\"}\n" +
            "      ],\n" +
            "      \"hazard_spawn_zone\": {\n" +
            "        \"min\": {\"x\": 30, \"y\": -34, \"z\": 83},\n" +
            "        \"max\": {\"x\": 76, \"y\": -24, \"z\": 95},\n" +
            "        \"note\": \"Approximate library footprint — PLACEHOLDER\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"earthquake\": {\n" +
            "      \"exits\": [],\n" +
            "      \"assembly_area\": {\n" +
            "        \"min\": {\"x\": 30, \"y\": -35, \"z\": 96},\n" +
            "        \"max\": {\"x\": 76, \"y\": -28, \"z\": 115},\n" +
            "        \"note\": \"PLACEHOLDER — front/Z+ face, tune after runServer\"\n" +
            "      },\n" +
            "      \"hazard_spawn_zone\": {\n" +
            "        \"note\": \"Epicenter varies per session — see sessions CSV magnitude column\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
        BerongSMP.LOGGER.info("[TelemetryCsvWriter] Wrote map_metadata.json");
    }
}
