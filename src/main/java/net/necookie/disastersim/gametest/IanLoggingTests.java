package net.necookie.disastersim.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.registry.ModBlocks;
import net.necookie.disastersim.registry.ModItems;
import net.necookie.disastersim.session.SessionManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.necookie.disastersim.gametest.GT.*;
import static net.necookie.disastersim.gametest.IanGT.*;

/**
 * Ian's behavioral-logging IDs BL-01..03 and BL-05..10 (BL-04 lives with the fire tests), DB-01 (session save to
 * the TEST database) and the game half of IN-01, 20 variants each. The five scenarios are rotated through the variants.
 */
final class IanLoggingTests {

    private IanLoggingTests() {}

    private static final SimulationState[] STATES = {SimulationState.FIRE, SimulationState.EARTHQUAKE, SimulationState.CCS_FIRE, SimulationState.CCS_EARTHQUAKE, SimulationState.NEW_SIM_BUILDING2_FIRE};
    private static SimulationState state(int i) { return STATES[i % 5]; }

    private static List<String[]> rows(SimulationSession s) { return s.buildMoveCsv().lines().skip(1).filter(l -> !l.isBlank()).map(l -> l.split(",", -1)).toList(); }
    private static long moveRows(SimulationSession s) { return s.buildMoveCsv().lines().filter(l -> l.contains(",move_tick,")).count(); }
    private static Object endReason(SimulationSession s) { return last(s, "SIM_END").map(e -> e.data().get("end_reason")).orElse(null); }

    private static void clickBlock(RecordingPlayer p, BlockPos b) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, b, new BlockHitResult(Vec3.atCenterOf(b), Direction.UP, b, false)));
    }

    static void registerAll() {
        // ───────── BL-01 session start ─────────
        each("BL-01", 300, (h, i) -> {
            String id = cid("BL-01", i);
            RecordingPlayer p = join(h, "bl01_" + i);
            SimulationState st = state(i);
            SimulationSession s = start(p, st);
            h.runAfterDelay(WARMUP + 1 + i, safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> ev = s.logger.getEvents();
                List<SimEvent> starts = events(s, "SIM_START");
                need(starts.size() == 1, "SIM_START events " + starts.size());
                int pos = ev.indexOf(starts.get(0));
                Object type = starts.get(0).data().get("sim_type");
                need(st.name().equals(String.valueOf(type)), "SIM_START sim_type " + type + ", scenario " + st);
                need(starts.get(0).data().containsKey("x") && starts.get(0).data().containsKey("z"), "SIM_START has no start position: " + starts.get(0).data());
                String sid = s.getSessionId();
                need(sid != null && sid.length() >= 6, "session id " + sid);
                long csv = rows(s).stream().filter(r -> r[4].equals("session_start") && r[1].equals(sid) && r[0].equals(p.getUUID().toString())).count();
                need(csv == 1, "session_start rows for this session in the CSV: " + csv);
                return st + ": SIM_START recorded once with sim_type " + type + " and a start position" + (pos == 0 ? " (first event)" : " (event " + (pos + 1) + ", after " + ev.get(0).type() + ")") + "; session id " + sid + " on the matching session_start CSV row for this player.";
            })));
        });

        // ───────── BL-02 movement logging at the configured 20 Hz ─────────
        each("BL-02", 600, (h, i) -> {
            String id = cid("BL-02", i);
            RecordingPlayer p = join(h, "bl02_" + i);
            SimulationSession s = start(p, state(i));
            BlockPos base = platform(h);
            int window = 60 + i * 10;   // 60..250 ticks of walking
            AtomicLong startRows = new AtomicLong(-1);
            h.onEachTick(() -> { if (startRows.get() >= 0 && h.getTick() <= WARMUP + 5 + window) tp(p, base.getX() + 0.5 + ((h.getTick() % 40) * 0.05) * (i % 2 == 0 ? 1 : -1), base.getY(), base.getZ() + 0.5 + (i % 3) * 0.3); });
            h.runAfterDelay(WARMUP + 5, safe(h, id, () -> startRows.set(moveRows(s))));
            h.runAfterDelay(WARMUP + 5 + window + 3, safe(h, id, () -> verdict(h, id, () -> {
                long n = moveRows(s) - startRows.get();
                double hz = n / ((window + 3) / 20.0);
                need(n >= (window + 3) * 0.95 && n <= (window + 3) * 1.05, n + " move_tick rows in " + (window + 3) + " ticks (" + String.format("%.1f", hz) + " Hz), expected 20 Hz");
                String[] last = rows(s).stream().filter(r -> r[4].equals("move_tick")).reduce((a, b) -> b).orElseThrow();
                need(Math.abs(Double.parseDouble(last[5]) - p.getX()) < 0.3 && Math.abs(Double.parseDouble(last[7]) - p.getZ()) < 0.3, "last row at (" + last[5] + ", " + last[7] + "), player at (" + p.getX() + ", " + p.getZ() + ")");
                return state(i) + ": " + n + " move_tick rows over " + (window + 3) + " ticks = " + String.format("%.1f", hz) + " Hz; the last row matches the player's position.";
            })));
        });

        // ───────── BL-03 interaction event ─────────
        each("BL-03", 400, (h, i) -> {
            String id = cid("BL-03", i);
            int kind = i % 4;   // 0 alarm, 1 door, 2 pin pull, 3 spray
            SimulationState st = kind == 1 ? state(i) : (i % 8 < 4 ? SimulationState.FIRE : SimulationState.CCS_FIRE);
            RecordingPlayer p = join(h, "bl03_" + i);
            SimulationSession s = start(p, st);
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            String type = new String[]{"fire_alarm_activate", "door_open", "pin_pull", "ext_spray"}[kind];
            BlockPos fire = base.east(3), alarm = base.north(2), door = base.south(2);
            if (kind == 0) h.getLevel().setBlock(alarm, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            if (kind == 1) h.getLevel().setBlock(door, Blocks.OAK_DOOR.defaultBlockState(), 3);
            if (kind == 3) h.getLevel().setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);
            h.runAfterDelay(WARMUP + 5 + i, safe(h, id, () -> {
                switch (kind) {
                    case 0 -> rightClickBlock(p, alarm);
                    case 1 -> clickBlock(p, door);
                    case 2 -> arm(p, ModItems.FIRE_EXTINGUISHER.get());
                    default -> { faceFrom(p, fire, 2.5); arm(p, ModItems.FIRE_EXTINGUISHER.get()); spray(p, 4); }
                }
            }));
            h.runAfterDelay(WARMUP + 12 + i, safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> ev = events(s, type);
                need(ev.size() >= 1, "no " + type + " event; events: " + s.logger.getEvents().stream().map(SimEvent::type).distinct().toList());
                SimEvent e = ev.get(0);
                need(e.tOffsetMs() >= 0 && e.tOffsetMs() <= s.elapsedSeconds() * 1000 + 2000, "timestamp " + e.tOffsetMs() + " ms outside the run");
                long csv = rows(s).stream().filter(r -> r[4].equals(type)).count();
                need(csv >= 1, "no " + type + " row in the CSV");
                return st + ": " + type + " logged (" + ev.size() + " event" + (ev.size() > 1 ? "s" : "") + ", first at " + e.tOffsetMs() + " ms) with a matching CSV row.";
            })));
        });

        // ───────── BL-05 phase transition events (New Sim Building 2.0) ─────────
        each("BL-05", 700, (h, i) -> {
            String id = cid("BL-05", i);
            RecordingPlayer p = join(h, "bl05_" + i);
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            boolean toEvac = i >= 10;
            int at = toEvac ? WARMUP + 408 + (i - 10) * 3 : WARMUP + 205 + i * 3;
            h.runAfterDelay(at, safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> t = events(s, "phase_transition");
                List<String> phases = t.stream().map(e -> String.valueOf(e.data().get("phase"))).toList();
                List<String> want = toEvac ? List.of("prevention", "intervention", "evacuation") : List.of("prevention", "intervention");
                need(phases.equals(want), "phase_transition sequence " + phases + ", expected " + want);
                need(t.stream().allMatch(e -> e.data().get("t") instanceof Number), "a phase_transition has no numeric t");
                double t1 = ((Number) t.get(1).data().get("t")).doubleValue();
                need(t1 > 8.5 && t1 < 11.5, "intervention started at t=" + t1 + " s, expected about 10 s");
                if (toEvac) { double t2 = ((Number) t.get(2).data().get("t")).doubleValue(); need(t2 - t1 > 8.5 && t2 - t1 < 11.5, "evacuation began " + (t2 - t1) + " s after intervention"); }
                long csv = rows(s).stream().filter(r -> r[4].equals("phase_transition")).count();
                need(csv == t.size(), "CSV has " + csv + " phase_transition rows, event log " + t.size());
                return "Phase transitions " + phases + " logged in order with numeric times (intervention at " + t1 + " s) and matching CSV rows.";
            })));
        });

        // ───────── BL-06 outcome event: assembly area reached ─────────
        each("BL-06", 400, (h, i) -> {
            String id = cid("BL-06", i);
            RecordingPlayer p = join(h, "bl06_" + i);
            SimulationSession s = start(p, SimulationState.FIRE);
            double x = 31.0 + (i % 5) * 11.0, z = 65.0 + (i / 5) * 5.0;
            h.runAfterDelay(WARMUP + 10 + i, safe(h, id, () -> tp(p, x, -33, z)));
            h.runAfterDelay(WARMUP + 20 + i, safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> a = events(s, "assembly_area_reached");
                need(a.size() == 1, "assembly_area_reached events " + a.size() + " for a player at (" + x + ", " + z + ")");
                Object ex = a.get(0).data().get("x"), ez = a.get(0).data().get("z");
                need(ex instanceof Number && ez instanceof Number && Math.abs(((Number) ex).doubleValue() - x) < 1.0 && Math.abs(((Number) ez).doubleValue() - z) < 1.0, "event position " + ex + ", " + ez);
                need(rows(s).stream().anyMatch(r -> r[4].equals("assembly_area_reached")), "no assembly_area_reached CSV row");
                need("assembly_reached".equals(endReason(s)), "end_reason " + endReason(s));
                return "Entered the assembly zone at (" + x + ", " + z + "): one assembly_area_reached event at that position, CSV row written, run ended assembly_reached.";
            })));
        });

        // ───────── BL-07 session end: reason and summary fields ─────────
        each("BL-07", 4000, (h, i) -> {
            String id = cid("BL-07", i);
            int how = i % 4;   // 0 assembly, 1 /sim_stop, 2 disconnect, 3 time limit
            SimulationState st = how == 3 ? (i % 8 < 4 ? SimulationState.FIRE : SimulationState.EARTHQUAKE) : state(i / 4);
            RecordingPlayer p = join(h, "bl07_" + i);
            SimulationSession s = start(p, st);
            String want = new String[]{"assembly_reached", null, "disconnected", "timeout"}[how];
            Runnable check = () -> verdict(h, id, () -> {
                need(SimulationManager.getSession(p.getUUID()) == null, "run still active");
                SimEvent end = last(s, "SIM_END").orElseThrow(() -> new AssertionError("no SIM_END event"));
                Object reason = end.data().get("end_reason");
                need(reason instanceof String && !((String) reason).isBlank(), "end_reason missing: " + end.data());
                if (want != null) need(want.equals(reason), "end_reason " + reason + ", expected " + want);
                else need(!"timeout".equals(reason), "/sim_stop recorded as timeout");
                need(end.data().get("score") instanceof Number && end.data().get("passed") instanceof Boolean, "score/passed missing or mistyped: " + end.data());
                need(end.data().containsKey("fires_extinguished") && end.data().containsKey("fire_spread_count"), "summary counters missing: " + end.data());
                need(s.logger.getEvents().get(s.logger.getEvents().size() - 1).type().equals("SIM_END"), "SIM_END is not the last event");
                need(rows(s).stream().anyMatch(r -> r[4].equals("session_end")), "no session_end CSV row");
                return st + " ended by " + new String[]{"the assembly zone", "/sim_stop", "disconnect", "the time limit"}[how] + ": SIM_END end_reason=" + reason + ", score " + end.data().get("score") + ", passed " + end.data().get("passed") + ", counters present, session_end row written.";
            });
            switch (how) {
                case 0 -> h.runAfterDelay(WARMUP + 15, safe(h, id, () -> {
                    if (st == SimulationState.FIRE || st == SimulationState.EARTHQUAKE) tp(p, 50.5, -33, 70.5); else SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                    h.runAfterDelay(10, safe(h, id, check));
                }));
                case 1 -> h.runAfterDelay(WARMUP + 15, safe(h, id, () -> { cmd(p, "sim_stop", true); h.runAfterDelay(10, safe(h, id, check)); }));
                case 2 -> h.runAfterDelay(WARMUP + 15, safe(h, id, () -> { p.leave(); h.runAfterDelay(10, safe(h, id, check)); }));
                default -> flow(h).when(1, () -> SimulationManager.getSession(p.getUUID()) == null, safe(h, id, check)).start();
            }
        });

        // ───────── BL-08 every record carries the session identifier of its own run ─────────
        each("BL-08", 600, (h, i) -> {
            String id = cid("BL-08", i);
            RecordingPlayer p = join(h, "bl08_" + i);
            SimulationState st = state(i);
            SimulationSession s = start(p, st);
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            boolean fire = st.isFire();
            BlockPos alarm = base.north(2);
            if (fire) h.getLevel().setBlock(alarm, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            h.runAfterDelay(WARMUP + 20 + i * 3, safe(h, id, () -> { if (fire) rightClickBlock(p, alarm); }));
            h.runAfterDelay(WARMUP + 80 + i * 6, safe(h, id, () -> {
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                verdict(h, id, () -> {
                    String sid = s.getSessionId();
                    List<String[]> all = rows(s);
                    need(all.size() > 20, "only " + all.size() + " rows");
                    need(all.stream().allMatch(r -> r[1].equals(sid)), all.stream().filter(r -> !r[1].equals(sid)).count() + " rows carry a different session id");
                    need(all.stream().allMatch(r -> r[0].equals(p.getUUID().toString())), "a row carries a different player id");
                    need(all.stream().map(r -> r[2]).distinct().count() == 1, "mixed scenario types in one run: " + all.stream().map(r -> r[2]).distinct().toList());
                    return st + ": all " + all.size() + " CSV rows carry session " + sid.substring(0, 8) + "..., this player's id and one scenario type.";
                });
            }));
        });

        // ───────── BL-09 chronology ─────────
        each("BL-09", 600, (h, i) -> {
            String id = cid("BL-09", i);
            RecordingPlayer p = join(h, "bl09_" + i);
            SimulationState st = i % 2 == 0 ? SimulationState.FIRE : SimulationState.CCS_FIRE;
            SimulationSession s = start(p, st);
            BlockPos base = platform(h), fire = base.east(3), alarm = base.north(2);
            h.getLevel().setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);
            h.getLevel().setBlock(alarm, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            h.onEachTick(() -> { if (h.getTick() > WARMUP) tp(p, base.getX() + 0.5 - 0.5 + (h.getTick() % 30) * 0.03, base.getY(), base.getZ() + 0.5); });
            h.runAfterDelay(WARMUP + 10 + i * 2, safe(h, id, () -> rightClickBlock(p, alarm)));
            h.runAfterDelay(WARMUP + 40 + i * 2, safe(h, id, () -> { faceFrom(p, fire, 2.5); arm(p, ModItems.FIRE_EXTINGUISHER.get()); spray(p, 6); }));
            h.runAfterDelay(WARMUP + 120, safe(h, id, () -> {
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                verdict(h, id, () -> {
                    List<SimEvent> ev = s.logger.getEvents();
                    int bad = 0; for (int k = 1; k < ev.size(); k++) if (ev.get(k).tOffsetMs() < ev.get(k - 1).tOffsetMs()) bad++;
                    need(bad == 0, bad + " of " + ev.size() + " events are out of time order");
                    List<String[]> r = rows(s);
                    int badCsv = 0; for (int k = 1; k < r.size(); k++) if (Double.parseDouble(r.get(k)[3]) < Double.parseDouble(r.get(k - 1)[3])) badCsv++;
                    need(badCsv == 0, badCsv + " of " + r.size() + " CSV rows are out of time order");
                    need(ev.get(0).type().equals("SIM_START") && ev.get(ev.size() - 1).type().equals("SIM_END"), "stream does not start with SIM_START and end with SIM_END");
                    return st + ": " + ev.size() + " events and " + r.size() + " CSV rows in non-decreasing time order, SIM_START first and SIM_END last, with alarm, pin pull and spray in between.";
                });
            }));
        });

        // ───────── BL-10 required fields present and typed ─────────
        each("BL-10", 600, (h, i) -> {
            String id = cid("BL-10", i);
            RecordingPlayer p = join(h, "bl10_" + i);
            SimulationState st = state(i);
            SimulationSession s = start(p, st);
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            h.runAfterDelay(WARMUP + 40 + i * 4, safe(h, id, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                verdict(h, id, () -> {
                    for (SimEvent e : s.logger.getEvents()) {
                        need(e.type() != null && !e.type().isBlank(), "an event has no type");
                        need(e.tOffsetMs() >= 0, e.type() + " has a negative offset " + e.tOffsetMs());
                        need(e.data() != null, e.type() + " has no data map");
                    }
                    int n = 0;
                    for (String[] r : rows(s)) {
                        n++;
                        need(r.length >= 14, "CSV row " + n + " has " + r.length + " columns");
                        need(!r[0].isBlank() && !r[1].isBlank() && !r[2].isBlank() && !r[4].isBlank(), "CSV row " + n + " has an empty identifier column: " + String.join("|", r));
                        for (int c : new int[]{3, 5, 6, 7, 8}) { try { Double.parseDouble(r[c]); } catch (NumberFormatException ex) { throw new AssertionError("CSV row " + n + " column " + c + " is not numeric: \"" + r[c] + "\""); } }
                    }
                    need(n > 10, "only " + n + " CSV rows");
                    return st + ": " + s.logger.getEvents().size() + " events (type, offset, data present) and " + n + " CSV rows (14 columns, identifiers present, coordinates and timestamps numeric).";
                });
            }));
        });

        // ───────── DB-01 / IN-01 (game half): the finished session is saved to the test database ─────────
        each("DB-01", 400000, (h, i) -> saveCase(h, "DB-01", i, "db01"));
        each("IN-01", 400000, (h, i) -> saveCase(h, "IN-01", i, "in01"));
    }

    private static void saveCase(GameTestHelper h, String caseBase, int i, String tag) {
        String id = cid(caseBase, i);
        RecordingPlayer p = join(h, tag + "_" + i);
        String u = uniqueName(tag.equals("in01") ? "ianin" : "ianb");
        SimulationState st = (i % 3 == 0) ? SimulationState.FIRE : (i % 3 == 1) ? SimulationState.NEW_SIM_BUILDING2_FIRE : SimulationState.EARTHQUAKE;
        if (caseBase.equals("IN-01")) st = SimulationState.FIRE;      // end-to-end path of Ian's IN-01 is the fire session
        final SimulationState state = st;
        cmd(p, "register " + u + " \"" + PW + "\" TEST-" + u + " TEST3A Game Test " + u, false);
        AtomicReference<SimulationSession> sess = new AtomicReference<>();
        AtomicReference<JsonObject> row = new AtomicReference<>();
        int[] logged = {0};
        flow(h).when(1, giveUpAfter(40, every(10, () -> SessionManager.getActiveSession(p.getUUID()) != null && SessionManager.findActiveSessionRowId(p.getUUID()) > 0)), safe(h, id, () -> {
            sess.set(start(p, state));
        })).after(WARMUP + 20 + i, safe(h, id, () -> {
            if (state == SimulationState.FIRE || state == SimulationState.EARTHQUAKE) tp(p, 50.5, -33, 70.5);
            else SimulationManager.endSimulation(p.getUUID(), "timeout");
        })).when(1, () -> SimulationManager.getSession(p.getUUID()) == null, safe(h, id, () -> logged[0] = sess.get().logger.getEvents().size()))
          .when(1, giveUpAfter(120, every(20, () -> {
              JsonArray rows = query("SELECT * FROM sessions WHERE username = ? ORDER BY id DESC", u);
              if (rows.isEmpty()) return false;
              JsonObject r = rows.get(0).getAsJsonObject();
              if (!"completed".equals(str(r, "status")) || str(r, "move_log_csv") == null) return false;
              row.set(r);
              return true;
          })), safe(h, id, () -> verdict(h, id, () -> {
              JsonObject r = row.get();
              need(r != null, "the finished session never appeared as completed in the database within 120 s");
              need(state.name().equals(str(r, "simulation_type")), "simulation_type " + str(r, "simulation_type") + ", ran " + state);
              need(u.equals(str(r, "username")) && ("TEST-" + u).equals(str(r, "student_id")) && "TEST3A".equals(str(r, "section")), "identity columns: " + str(r, "username") + "/" + str(r, "student_id") + "/" + str(r, "section"));
              int stored = JsonParser.parseString(str(r, "event_log")).getAsJsonArray().size();
              need(stored == logged[0], "event_log holds " + stored + " events, the game logged " + logged[0]);
              String n = str(query("SELECT COUNT(*) AS n FROM sessions WHERE username = ?", u).get(0).getAsJsonObject(), "n");
              need("1".equals(n), n + " sessions rows for " + u + " (duplicate or missing)");
              need(str(r, "start_time") != null && str(r, "end_time") != null, "start/end time missing");
              return state + " run saved: sessions row " + str(r, "id") + " for " + u + ", status completed, " + stored + " events = " + logged[0] + " logged, one row only, movement log stored.";
          }))).start();
    }
}
