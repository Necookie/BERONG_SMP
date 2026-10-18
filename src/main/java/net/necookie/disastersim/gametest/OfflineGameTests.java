package net.necookie.disastersim.gametest;

import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/**
 * GS-21: cloud database unreachable. Registered only for the "offline" game-test profile, whose
 * config sets tursoUrl to a closed local port and leaves tursoToken blank.
 */
final class OfflineGameTests {

    private OfflineGameTests() {}

    private static List<String> log() {
        try {
            return Files.readAllLines(Path.of("logs", "latest.log"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }

    private static long count(Path csv, String sessionId, String needle) {
        try (var lines = Files.lines(csv)) {
            return lines.filter(l -> l.contains(sessionId) && l.contains(needle)).count();
        } catch (IOException e) {
            return -1;
        }
    }

    static void registerAll() {
        add("GS-21-01", 400000, h -> {   // also GS-21-02 (local CSV) and GS-21-03 (warnings) for the same run
            reset(h);
            RecordingPlayer p = RecordingPlayer.join(h, "gs2101");
            cmd(p, "register offline1 \"GtPass#2026\" TEST-OFF1 TEST3A Offline Student", false);
            AtomicReference<SimulationSession> s = new AtomicReference<>();
            flow(h).when(1, () -> p.messages.stream().anyMatch(m -> m.contains("Registered") || m.contains("failed")), () -> {
                cmd(p, "sim_fire library", true);
                s.set(SimulationManager.getSession(p.getUUID()));
                h.assertTrue(s.get() != null, "simulation did not start with the database offline: " + p.messages);
                p.setInvulnerable(true);
            }).after(60 + 60, () -> tp(p, 50.5, -33, 70.5))
              .after(10, () -> {
                  String sid = s.get().getSessionId();
                  boolean a = caseCheck("GS-21-01", () -> {
                      h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "run did not finish");
                      Object reason = last(s.get(), "SIM_END").map(e -> e.data().get("end_reason")).orElse(null);
                      h.assertTrue("assembly_reached".equals(reason), "end_reason " + reason);
                      actual("GS-21-01", "With the database unreachable, registration fell back to local mode (\""
                              + p.messages.stream().filter(m -> m.contains("Registered")).findFirst().orElse("").split("\n")[0].trim()
                              + "\") and the Library run started, played and ended normally (assembly_reached); no crash.");
                  });
                  boolean b = caseCheck("GS-21-02", () -> {
                      Path csv = Path.of("telemetry", "gameplay_logs_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
                      long rows = count(csv, sid, ",");
                      long expected = s.get().buildMoveCsv().lines().skip(1).count(); // what the session itself recorded
                      h.assertTrue(rows == expected, "local CSV has " + rows + " rows for the session, the session recorded " + expected);
                      h.assertTrue(count(csv, sid, ",session_start,") == 1 && count(csv, sid, ",session_end,") == 1, "session_start/session_end missing");
                      actual("GS-21-02", "Local " + csv.getFileName() + " holds all " + rows + " telemetry rows the session recorded, from session_start to session_end.");
                  });
                  boolean c = caseCheck("GS-21-03", () -> {
                      long warnings = log().stream().filter(l -> l.contains("[TursoClient]") && (l.contains("WARN") || l.contains("ERROR"))).count();
                      h.assertTrue(warnings > 0, "no TursoClient warning logged");
                      h.assertTrue(warnings < 100, warnings + " TursoClient warnings: retry flood");
                      actual("GS-21-03", warnings + " TursoClient warning lines for the whole offline run, including registration and the session (no retry flood).");
                  });
                  finish(h, a, b, c);
              }).start();
        });
        add("GS-21-04", 40, h -> {
            String line = log().stream().filter(l -> l.contains("tursoToken is missing")).findFirst().orElse(null);
            h.assertTrue(line != null, "no missing-token warning at startup");
            actual("GS-21-04", "Startup log: " + line.substring(line.indexOf("[TursoClient]")).trim());
            h.succeed();
        });
    }
}
