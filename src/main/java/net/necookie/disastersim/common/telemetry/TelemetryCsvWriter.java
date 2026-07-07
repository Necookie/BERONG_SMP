package net.necookie.disastersim.common.telemetry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.block.FireAlarmBlock;
import net.necookie.disastersim.common.zones.AssemblyZone;
import net.necookie.disastersim.common.zones.ExitZones;
import net.necookie.disastersim.world.SimulationManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final List<BlockPos> fireAlarmPositions = new ArrayList<>();
    private static boolean fireAlarmsScanDone = false;
    private static String cachedModVersion = null;

    // Nominal extinguisher cabinet positions — extinguishers are issued as items at session start,
    // so these are fixed reference points for map_metadata.json (not scanned from placed blocks).
    private static final BlockPos LIBRARY_EXTINGUISHER_POS = new BlockPos(33, -33, 108); // west wall, entrance hall
    private static final BlockPos CCS_EXTINGUISHER_POS     = new BlockPos(82, -32,   8); // ground floor near CCS entrance

    public static void init(Path runDir) {
        try {
            telemetryDir = runDir.resolve("telemetry");
            Files.createDirectories(telemetryDir);

            // Always rewrite so new scenario sections and extinguisher positions take effect.
            writeMapMetadata(telemetryDir.resolve("map_metadata.json"));

            openEventWriter();
            openSessionWriter();

            BerongSMP.LOGGER.info("[TelemetryCsvWriter] Initialized telemetry dir: {}", telemetryDir);
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] Failed to initialize: {}", e.getMessage());
        }
    }

    public static void closeSession(String sessionId, Map<String, Object> metadata) {
        if (telemetryDir == null) return;
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
            sb.append(',').append(csv(getModVersion()));
            sessionWriter.write(sb.toString());
            sessionWriter.newLine();
            sessionWriter.flush();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] closeSession error: {}", e.getMessage());
        }
    }

    public static String writeRow(String sessionId, String playerId, String scenarioType,
                                  double timestamp, String eventType,
                                  double x, double y, double z,
                                  double hazardDistance,
                                  String interactionTarget, Integer nearbyPlayerCount) {
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
        String row = sb.toString();
        if (telemetryDir != null) {
            try {
                ensureEventWriter();
                eventWriter.write(row);
                eventWriter.newLine();
            } catch (IOException e) {
                BerongSMP.LOGGER.error("[TelemetryCsvWriter] writeRow error: {}", e.getMessage());
            }
        }
        return row;
    }

    public static void flush() {
        try {
            if (eventWriter != null) eventWriter.flush();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] flush error: {}", e.getMessage());
        }
    }

    /**
     * Scans the simulation area for FireAlarmBlock instances and rewrites map_metadata.json
     * with their positions. Called once after the first structure placement.
     */
    public static void scanAndRegisterFireAlarms(ServerLevel level, BlockPos simPos) {
        if (fireAlarmsScanDone) return;
        fireAlarmPositions.clear();
        for (BlockPos check : BlockPos.betweenClosed(
                simPos.offset(-5, -5, -5),
                simPos.offset(55, 25, 55))) {
            if (level.getBlockState(check).getBlock() instanceof FireAlarmBlock) {
                fireAlarmPositions.add(check.immutable());
            }
        }
        fireAlarmsScanDone = true;
        if (telemetryDir != null) {
            try {
                writeMapMetadata(telemetryDir.resolve("map_metadata.json"));
            } catch (IOException e) {
                BerongSMP.LOGGER.error("[TelemetryCsvWriter] Failed to rewrite map_metadata.json: {}", e.getMessage());
            }
        }
        BerongSMP.LOGGER.info("[TelemetryCsvWriter] FireAlarmBlock scan: found {} alarm(s) in simulation area", fireAlarmPositions.size());
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
                    "contract_version,mod_version");
            sessionWriter.newLine();
        }
    }

    private static void ensureEventWriter() throws IOException {
        if (eventWriter == null) openEventWriter();
    }

    private static void ensureSessionWriter() throws IOException {
        if (sessionWriter == null) openSessionWriter();
    }

    private static String getModVersion() {
        if (cachedModVersion == null) {
            cachedModVersion = net.neoforged.fml.ModList.get()
                    .getModContainerById(BerongSMP.MODID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");
        }
        return cachedModVersion;
    }

    private static String buildExtinguisherJson(BlockPos... positions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < positions.length; i++) {
            if (i > 0) sb.append(",");
            BlockPos p = positions[i];
            sb.append(String.format(
                "{\"x\":%d,\"y\":%d,\"z\":%d,\"note\":\"nominal — extinguisher issued as item at session start\"}",
                p.getX(), p.getY(), p.getZ()));
        }
        return sb.toString();
    }

    private static String buildAlarmJson() {
        if (fireAlarmPositions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fireAlarmPositions.size(); i++) {
            if (i > 0) sb.append(",");
            BlockPos p = fireAlarmPositions.get(i);
            sb.append(String.format("{\"x\":%d,\"y\":%d,\"z\":%d}", p.getX(), p.getY(), p.getZ()));
        }
        return sb.toString();
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
        net.minecraft.world.phys.AABB az = AssemblyZone.getZone();
        StringBuilder exits = new StringBuilder();
        for (ExitZones.ExitZone z : ExitZones.ZONES) {
            net.minecraft.world.phys.AABB b = z.bounds();
            if (exits.length() > 0) exits.append(",\n        ");
            exits.append(String.format(
                "{\"label\":\"%s\",\"min\":{\"x\":%d,\"y\":%d,\"z\":%d},\"max\":{\"x\":%d,\"y\":%d,\"z\":%d}}",
                z.label(),
                (int) b.minX, (int) b.minY, (int) b.minZ,
                (int) b.maxX, (int) b.maxY, (int) b.maxZ));
        }
        String assemblyJson = String.format(
            "{\"min\":{\"x\":%d,\"y\":%d,\"z\":%d},\"max\":{\"x\":%d,\"y\":%d,\"z\":%d}}",
            (int) az.minX, (int) az.minY, (int) az.minZ,
            (int) az.maxX, (int) az.maxY, (int) az.maxZ);
        net.minecraft.core.BlockPos sp = SimulationManager.SIM_POS;
        net.minecraft.core.BlockPos ccs = SimulationManager.CCS_POS;
        String json = "{\n" +
            "  \"contract_version\": \"" + CONTRACT_VERSION + "\",\n" +
            "  \"sim_pos\": {\"x\": " + sp.getX() + ", \"y\": " + sp.getY() + ", \"z\": " + sp.getZ() + "},\n" +
            "  \"ccs_pos\": {\"x\": " + ccs.getX() + ", \"y\": " + ccs.getY() + ", \"z\": " + ccs.getZ() + "},\n" +
            "  \"scenarios\": {\n" +
            "    \"fire\": {\n" +
            "      \"exits\": [\n        " + exits + "\n      ],\n" +
            "      \"assembly_area\": " + assemblyJson + ",\n" +
            "      \"fire_alarm_positions\": [" + buildAlarmJson() + "],\n" +
            "      \"extinguisher_positions\": [" + buildExtinguisherJson(LIBRARY_EXTINGUISHER_POS) + "],\n" +
            "      \"hazard_spawn_zone\": {\"note\": \"Approximate library interior — see sim_pos\"}\n" +
            "    },\n" +
            "    \"earthquake\": {\n" +
            "      \"exits\": [],\n" +
            "      \"assembly_area\": " + assemblyJson + ",\n" +
            "      \"hazard_spawn_zone\": {\"note\": \"Epicenter varies per session — see sessions CSV magnitude column\"}\n" +
            "    },\n" +
            "    \"ccs_fire\": {\n" +
            "      \"exits\": [],\n" +
            "      \"assembly_area\": " + assemblyJson + ",\n" +
            "      \"fire_alarm_positions\": [],\n" +
            "      \"extinguisher_positions\": [" + buildExtinguisherJson(CCS_EXTINGUISHER_POS) + "],\n" +
            "      \"hazard_spawn_zone\": {\"note\": \"CCS Admin Building interior — x:80-135 z:6-69 y covers both floors\"}\n" +
            "    },\n" +
            "    \"ccs_earthquake\": {\n" +
            "      \"exits\": [],\n" +
            "      \"assembly_area\": " + assemblyJson + ",\n" +
            "      \"hazard_spawn_zone\": {\"note\": \"Epicenter varies per session inside CCS Admin Building\"}\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
        BerongSMP.LOGGER.info("[TelemetryCsvWriter] Wrote map_metadata.json (derived from AssemblyZone + ExitZones)");
    }
}
