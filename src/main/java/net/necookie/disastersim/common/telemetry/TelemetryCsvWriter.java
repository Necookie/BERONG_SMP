package net.necookie.disastersim.common.telemetry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.block.FireAlarmBlock;
import net.necookie.disastersim.common.zones.AssemblyZone;
import net.necookie.disastersim.common.zones.ExitZones;
import net.necookie.disastersim.common.simulation.SimulationManager;

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

    private static final String CONTRACT_VERSION = "1.2";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static Path telemetryDir;
    private static BufferedWriter eventWriter;
    private static BufferedWriter sessionWriter;
    private static final List<BlockPos> fireAlarmPositions = new ArrayList<>();
    private static boolean fireAlarmsScanDone = false;
    private static final List<BlockPos> nsb2FireAlarmPositions = new ArrayList<>();
    private static boolean nsb2FireAlarmsScanDone = false;
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
            sb.append(',').append(csv(metadata.getOrDefault("final_fire_phase", "")));
            sb.append(',').append(CONTRACT_VERSION);
            sb.append(',').append(csv(getModVersion()));
            sessionWriter.write(sb.toString());
            sessionWriter.newLine();
            sessionWriter.flush();
        } catch (IOException e) {
            BerongSMP.LOGGER.error("[TelemetryCsvWriter] closeSession error: {}", e.getMessage());
        }
    }

    /** Legacy 11-column call shape — delegates with the v1.2 extra fields left blank. */
    public static String writeRow(String sessionId, String playerId, String scenarioType,
                                  double timestamp, String eventType,
                                  double x, double y, double z,
                                  double hazardDistance,
                                  String interactionTarget, Integer nearbyPlayerCount) {
        return writeRow(sessionId, playerId, scenarioType, timestamp, eventType, x, y, z,
                hazardDistance, interactionTarget, nearbyPlayerCount, null, null, null);
    }

    /**
     * v1.2 call shape carrying the 3-phase fire state machine's extra per-event fields
     * ({@code hit_fire}/{@code extinguisher_class} on {@code pin_pull}/{@code ext_spray},
     * {@code phase} on {@code phase_transition} and any event fired during a phased run).
     * Every row this class writes goes through here — the legacy 11-arg overload just passes
     * nulls for these three, keeping every row in the CSV the same width.
     */
    public static String writeRow(String sessionId, String playerId, String scenarioType,
                                  double timestamp, String eventType,
                                  double x, double y, double z,
                                  double hazardDistance,
                                  String interactionTarget, Integer nearbyPlayerCount,
                                  Boolean hitFire, String extinguisherClass, String phase) {
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
        sb.append(nearbyPlayerCount != null ? nearbyPlayerCount : "").append(',');
        sb.append(hitFire != null ? (hitFire ? 1 : 0) : "").append(',');
        sb.append(extinguisherClass != null ? csv(extinguisherClass) : "").append(',');
        sb.append(phase != null ? csv(phase) : "");
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

    /**
     * Same idea as {@link #scanAndRegisterFireAlarms}, but for New Sim Building 2.0's much larger
     * footprint (both floors) — a separate list/flag/box since the two buildings' alarm counts feed
     * different {@code map_metadata.json} sections (§8 of telemetry_contract.md) and this scenario's
     * {@code NewSimScoring} bonus depends on the building actually containing a reachable alarm.
     */
    public static void scanAndRegisterNewSim2FireAlarms(ServerLevel level, BlockPos base, int spanX, int spanZ, int height) {
        if (nsb2FireAlarmsScanDone) return;
        nsb2FireAlarmPositions.clear();
        for (BlockPos check : BlockPos.betweenClosed(base, base.offset(spanX, height, spanZ))) {
            if (level.getBlockState(check).getBlock() instanceof FireAlarmBlock) {
                nsb2FireAlarmPositions.add(check.immutable());
            }
        }
        nsb2FireAlarmsScanDone = true;
        if (telemetryDir != null) {
            try {
                writeMapMetadata(telemetryDir.resolve("map_metadata.json"));
            } catch (IOException e) {
                BerongSMP.LOGGER.error("[TelemetryCsvWriter] Failed to rewrite map_metadata.json: {}", e.getMessage());
            }
        }
        BerongSMP.LOGGER.info("[TelemetryCsvWriter] New Sim Building 2.0 FireAlarmBlock scan: found {} alarm(s)", nsb2FireAlarmPositions.size());
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
                    "x,y,z,hazard_distance,interaction_target,nearby_player_count," +
                    "hit_fire,extinguisher_class,phase");
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
                    "final_fire_phase,contract_version,mod_version");
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
        return buildAlarmJson(fireAlarmPositions);
    }

    private static String buildAlarmJson(List<BlockPos> positions) {
        if (positions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) sb.append(",");
            BlockPos p = positions.get(i);
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
        net.minecraft.world.phys.AABB nsb2Zone = AssemblyZone.getNewSim2Zone();
        String nsb2AssemblyJson = String.format(
            "{\"min\":{\"x\":%d,\"y\":%d,\"z\":%d},\"max\":{\"x\":%d,\"y\":%d,\"z\":%d}}",
            (int) nsb2Zone.minX, (int) nsb2Zone.minY, (int) nsb2Zone.minZ,
            (int) nsb2Zone.maxX, (int) nsb2Zone.maxY, (int) nsb2Zone.maxZ);
        StringBuilder nsb2Exits = new StringBuilder();
        for (ExitZones.ExitZone z : ExitZones.NEW_SIM2_ZONES) {
            net.minecraft.world.phys.AABB b = z.bounds();
            if (nsb2Exits.length() > 0) nsb2Exits.append(",\n        ");
            nsb2Exits.append(String.format(
                "{\"label\":\"%s\",\"min\":{\"x\":%d,\"y\":%d,\"z\":%d},\"max\":{\"x\":%d,\"y\":%d,\"z\":%d}}",
                z.label(),
                (int) b.minX, (int) b.minY, (int) b.minZ,
                (int) b.maxX, (int) b.maxY, (int) b.maxZ));
        }
        net.minecraft.core.BlockPos sp = SimulationManager.SIM_POS;
        net.minecraft.core.BlockPos ccs = SimulationManager.CCS_POS;
        net.minecraft.core.BlockPos nsb2 = SimulationManager.NEW_SIM_BUILDING2_POS;
        String json = "{\n" +
            "  \"contract_version\": \"" + CONTRACT_VERSION + "\",\n" +
            "  \"sim_pos\": {\"x\": " + sp.getX() + ", \"y\": " + sp.getY() + ", \"z\": " + sp.getZ() + "},\n" +
            "  \"ccs_pos\": {\"x\": " + ccs.getX() + ", \"y\": " + ccs.getY() + ", \"z\": " + ccs.getZ() + "},\n" +
            "  \"new_sim_building2_pos\": {\"x\": " + nsb2.getX() + ", \"y\": " + nsb2.getY() + ", \"z\": " + nsb2.getZ() + "},\n" +
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
            "    },\n" +
            "    \"new_sim_building2_fire\": {\n" +
            "      \"exits\": [\n        " + nsb2Exits + "\n      ],\n" +
            "      \"assembly_area\": " + nsb2AssemblyJson + ",\n" +
            "      \"fire_alarm_positions\": [" + buildAlarmJson(nsb2FireAlarmPositions) + "],\n" +
            "      \"extinguisher_positions\": [{\"note\": \"all 3 classes issued as items at session start\"}],\n" +
            "      \"hazard_spawn_zone\": {\"note\": \"5 random hazards armed each run from the building's own hazard-prop scan — see new_sim_building2_pos and /sim_scan_hazards\"},\n" +
            "      \"phases\": [\"prevention\", \"intervention\", \"evacuation\"],\n" +
            "      \"survey_status\": \"assembly_area F3-verified via //copyroom (2026-07-14); exits are STALE placeholders derived from an earlier (now-wrong) assembly location, need a fresh F3 walk-through\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
        BerongSMP.LOGGER.info("[TelemetryCsvWriter] Wrote map_metadata.json (derived from AssemblyZone + ExitZones)");
    }
}
