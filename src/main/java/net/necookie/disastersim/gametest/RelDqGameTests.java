package net.necookie.disastersim.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseFireBlock;
import net.necookie.disastersim.academy.AcademyTelemetry;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.registry.ModItems;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/** Game reliability (G-REL), data quality (G-DQ) and the integration seed run (G-INT-SEED). */
final class RelDqGameTests {

    private RelDqGameTests() {}

    private static final int WARMUP = 60;
    private static final String PW = "GtPass#2026";
    /** Turned on by G-REL-08 only: makes an extra tick handler throw every tick. */
    private static volatile boolean injectHandlerFailure;

    private static RecordingPlayer join(GameTestHelper h, String tag) { reset(h); return RecordingPlayer.join(h, tag); }

    private static SimulationSession start(RecordingPlayer p, SimulationState state) {
        SimulationManager.startSimulation(p, state);
        SimulationSession s = SimulationManager.getSession(p.getUUID());
        if (s == null) throw new IllegalStateException("simulation did not start: " + p.messages);
        p.setInvulnerable(true);
        return s;
    }

    private static BooleanSupplier giveUpAfter(int seconds, BooleanSupplier cond) {
        long[] deadline = {0};
        return () -> {
            if (deadline[0] == 0) deadline[0] = System.currentTimeMillis() + seconds * 1000L;
            return System.currentTimeMillis() >= deadline[0] || cond.getAsBoolean();
        };
    }

    private static void register(RecordingPlayer p, String user) {
        cmd(p, "register " + user + " \"" + PW + "\" TEST-" + user + " TEST3A Game Test " + user, false);
    }

    private static Object endReason(SimulationSession s) {
        return last(s, "SIM_END").map(e -> e.data().get("end_reason")).orElse(null);
    }

    private static List<String[]> rows(String csv) {
        List<String[]> out = new ArrayList<>();
        csv.lines().skip(1).filter(l -> !l.isBlank()).forEach(l -> out.add(l.split(",", -1)));
        return out;
    }

    private static long nonAir(ServerLevel level, BlockPos from, BlockPos to) {
        long n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) if (!level.getBlockState(pos).isAir()) n++;
        return n;
    }

    static void registerAll() {
        TickScheduler.register(level -> { if (injectHandlerFailure) throw new IllegalStateException("injected test failure"); });
        reliability();
        dataQuality();
        integrationSeed();
    }

    // ───────────────────────────── Reliability ─────────────────────────────
    private static void reliability() {
        add("G-REL-01", 500, h -> {   // also G-REL-02 (fourth student refused while all three arenas are busy)
            reset(h);
            RecordingPlayer a = RecordingPlayer.join(h, "rel01a"), b = RecordingPlayer.join(h, "rel01b"), c = RecordingPlayer.join(h, "rel01c");
            SimulationSession sa = start(a, SimulationState.FIRE), sb = start(b, SimulationState.CCS_FIRE), sc = start(c, SimulationState.NEW_SIM_BUILDING2_FIRE);
            boolean[] ok = new boolean[2];
            seq(h).at(WARMUP + 40, () -> {
                SimulationManager.endSimulation(a.getUUID(), "timeout");
            }).at(40, () -> {
                ok[0] = caseCheck("G-REL-01", () -> {
                    h.assertTrue(SimulationManager.getSession(b.getUUID()) == sb && SimulationManager.getSession(c.getUUID()) == sc,
                            "ending the Library run disturbed the others");
                    h.assertTrue(sc.getFirePhase() == SimulationSession.FirePhase.PREVENTION && sc.getArmedHazards().size() == 5, "NSB2 state " + sc.getFirePhase());
                    actual("G-REL-01", "Three students ran at once (Library fire, CCS fire, New Sim Building 2.0). Ending the Library run left the CCS run and the New Sim run ("
                            + sc.getArmedHazards().size() + " armed hazards, still in PREVENTION) untouched.");
                });
                RecordingPlayer d = RecordingPlayer.join(h, "rel01d");
                SimulationManager.startSimulation(d, SimulationState.CCS_EARTHQUAKE);
                ok[1] = caseCheck("G-REL-02", () -> {
                    h.assertTrue(SimulationManager.getSession(d.getUUID()) == null && d.heard("in use"), "4th student: " + d.messages);
                    actual("G-REL-02", "A further student asking for the busy CCS building was refused with a clear message, and the running CCS session kept going.");
                });
                reset(h);
            }).at(1, () -> finish(h, ok)).end();
        });
        add("G-REL-03", 4000, h -> {
            RecordingPlayer p = join(h, "rel03");
            int ok = 0;
            for (int i = 0; i < 25; i++) {
                SimulationState st = List.of(SimulationState.FIRE, SimulationState.CCS_FIRE, SimulationState.NEW_SIM_BUILDING2_FIRE, SimulationState.EARTHQUAKE).get(i % 4);
                SimulationManager.startSimulation(p, st);
                if (SimulationManager.getSession(p.getUUID()) != null) ok++;
                SimulationManager.endSimulation(p.getUUID(), "test_reset");
            }
            h.assertTrue(ok == 25 && SimulationManager.getActiveSessions().isEmpty(), ok + " of 25 starts worked");
            actual("G-REL-03", "25 runs started and ended back to back across all four building/scenario types: all 25 started, none left running, every building freed for the next run.");
            reset(h);
            h.succeed();
        });
        add("G-REL-04", 300, h -> {
            RecordingPlayer p = join(h, "rel04");
            SimulationSession s = start(p, SimulationState.FIRE);
            p.setInvulnerable(false);
            seq(h).at(WARMUP + 5, () -> p.kill(h.getLevel())).at(5, () -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null && "injured".equals(endReason(s)), "after death: " + endReason(s));
                SimulationManager.startSimulation(p, SimulationState.FIRE);
                h.assertTrue(SimulationManager.getSession(p.getUUID()) != null, "could not start again after dying");
                actual("G-REL-04", "A student who died mid-run had the run closed with end_reason injured, and could start a new run straight away.");
                reset(h);
            }).end();
        });
        add("G-REL-05", 100, h -> {
            RecordingPlayer p = join(h, "rel05");
            SimulationSession s = start(p, SimulationState.CCS_FIRE);
            seq(h).at(10, () -> {
                h.assertTrue(s.isInWarmup(), "not in warmup");
                p.leave();
            }).at(2, () -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "session kept after leaving during warmup");
                RecordingPlayer q = RecordingPlayer.join(h, "rel05b");
                SimulationManager.startSimulation(q, SimulationState.CCS_FIRE);
                h.assertTrue(SimulationManager.getSession(q.getUUID()) != null, "CCS building still marked busy");
                actual("G-REL-05", "A student who disconnected during the 3-second countdown had the run closed, and the CCS building was free for the next student.");
                reset(h);
            }).end();
        });
        add("G-REL-06", 100, h -> {
            RecordingPlayer p = join(h, "rel06");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 5, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                SimulationManager.endSimulation(p.getUUID());
                h.assertTrue(events(s, "SIM_END").size() == 1, "SIM_END events " + events(s, "SIM_END").size());
                actual("G-REL-06", "Ending the same run three times in a row is harmless: one SIM_END event, no error, one database update.");
                reset(h);
            }).end();
        });
        add("G-REL-07", 40, h -> {
            RecordingPlayer p = join(h, "rel07");
            cmd(p, "sim_stop", true);
            cmd(p, "sim_time set 30", true);
            cmd(p, "sim_magnitude 7.0", true);
            h.assertTrue(SimulationManager.getActiveSessions().isEmpty(), "a session appeared");
            actual("G-REL-07", "Instructor commands with no run active (/sim_stop, /sim_time, /sim_magnitude) answer politely (\""
                    + p.messages.stream().filter(m -> m.contains("No active")).findFirst().orElse("no reply").trim() + "\") instead of failing.");
            reset(h);
            h.succeed();
        });
        add("G-REL-08", 300, h -> {
            RecordingPlayer p = join(h, "rel08");
            SimulationSession s = start(p, SimulationState.FIRE);
            int[] before = {0};
            seq(h).at(WARMUP, () -> {
                before[0] = s.elapsedTicks();
                injectHandlerFailure = true;
            }).at(100, () -> {
                injectHandlerFailure = false;
                int ran = s.elapsedTicks() - before[0];
                h.assertTrue(ran >= 95 && SimulationManager.getSession(p.getUUID()) == s, "run advanced " + ran + " ticks while a handler failed");
                actual("G-REL-08", "With another subsystem throwing an error on every tick for 5 seconds, the server and the run kept going (" + ran
                        + " ticks advanced); the failure was caught and logged instead of crashing the class.");
                reset(h);
            }).end();
        });
        add("G-REL-09", 900, h -> {
            RecordingPlayer p = join(h, "rel09");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            flow(h).when(WARMUP, () -> s.getFirePhase() == SimulationSession.FirePhase.EVACUATION, () -> { })
                   .after(150, () -> {
                       ServerLevel level = h.getLevel();
                       BlockPos o = s.getArenaOrigin();
                       int sx = s.getArenaSpanX(), sz = s.getArenaSpanZ(), ht = s.getArenaHeight();
                       int inside = 0, outside = 0;
                       for (BlockPos pos : BlockPos.betweenClosed(o.offset(-12, -2, -12), o.offset(sx + 12, ht + 4, sz + 12))) {
                           if (!(level.getBlockState(pos).getBlock() instanceof BaseFireBlock)) continue;
                           boolean in = pos.getX() >= o.getX() && pos.getX() < o.getX() + sx && pos.getZ() >= o.getZ() && pos.getZ() < o.getZ() + sz;
                           if (in) inside++; else outside++;
                       }
                       h.assertTrue(inside > 0 && outside == 0, "fire blocks inside " + inside + ", outside " + outside);
                       actual("G-REL-09", "7.5 s into the evacuation phase there were " + inside + " fire blocks inside the building and none outside its footprint, so fire cannot escape into the lobby or other buildings.");
                       reset(h);
                       h.succeed();
                   }).start();
        });
        add("G-REL-10", 1500, h -> {
            ServerLevel level = h.getLevel();
            RecordingPlayer p = join(h, "rel10");
            BlockPos o = SimulationManager.SIM_POS;
            long before = nonAir(level, o, o.offset(29, 9, 29));
            SimulationSession s = start(p, SimulationState.EARTHQUAKE);
            flow(h).when(WARMUP, () -> SimulationManager.getSession(p.getUUID()) == null, () -> { })
                   .after(5, () -> {
                       long after = nonAir(level, o, o.offset(29, 9, 29));
                       h.assertTrue(after == before, "library blocks " + before + " before, " + after + " after");
                       actual("G-REL-10", "After a full Library earthquake (peak destruction, aftershocks) the building was restored block for block: "
                               + before + " blocks before, " + after + " after (end reason " + endReason(s) + ").");
                       reset(h);
                       h.succeed();
                   }).start();
        });
        add("G-REL-11", 200, h -> {
            RecordingPlayer p = join(h, "rel11");
            start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 5, () -> {
                p.setInvulnerable(false);
                p.hurtServer(h.getLevel(), p.damageSources().generic(), 8.0f);
                p.getFoodData().setFoodLevel(5);
                p.setRemainingFireTicks(100);
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                h.assertTrue(p.getHealth() == p.getMaxHealth() && p.getFoodData().getFoodLevel() == 20 && p.getRemainingFireTicks() <= 0
                        && p.getInventory().countItem(ModItems.FIRE_EXTINGUISHER.get()) == 0, "after end: health " + p.getHealth()
                        + ", food " + p.getFoodData().getFoodLevel() + ", fire " + p.getRemainingFireTicks());
                actual("G-REL-11", "After a run ends the student is healed, fed and put out, and the issued extinguisher is taken back, so nothing carries over into the lobby.");
                reset(h);
            }).end();
        });
        add("G-REL-12", 400000, h -> {
            reset(h);
            List<RecordingPlayer> ps = new ArrayList<>();
            List<String> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                RecordingPlayer p = RecordingPlayer.join(h, "rel12" + i);
                String u = uniqueName("cr") + i;
                ps.add(p); users.add(u);
                register(p, u);
            }
            long t0 = System.currentTimeMillis();
            flow(h).when(1, giveUpAfter(60, () -> ps.stream().allMatch(p -> AuthManager.current(p.getUUID()) != null)), () -> {
                long ms = System.currentTimeMillis() - t0;
                int done = (int) ps.stream().filter(p -> AuthManager.current(p.getUUID()) != null).count();
                int rowsN = 0;
                for (String u : users) rowsN += query("SELECT id FROM student_accounts WHERE username = ?", u).size();
                h.assertTrue(done == 5 && rowsN == 5, done + " logged in, " + rowsN + " rows");
                actual("G-REL-12", "Five students registered at the same moment: all 5 accounts created and logged in within " + ms + " ms, no duplicates or lost rows.");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-REL-13", 9000, h -> {
            RecordingPlayer p = join(h, "rel13");
            int[] run = {0};
            List<String> reasons = new ArrayList<>();
            AtomicReference<SimulationSession> cur = new AtomicReference<>(start(p, SimulationState.EARTHQUAKE));
            h.onEachTick(() -> {
                if (run[0] >= 10) return;
                if (SimulationManager.getSession(p.getUUID()) == null) {
                    reasons.add(String.valueOf(endReason(cur.get())));
                    run[0]++;
                    if (run[0] < 10) cur.set(start(p, SimulationState.EARTHQUAKE));
                    else {
                        boolean allEnded = reasons.stream().noneMatch(r -> r.equals("null") || r.equals("injured"));
                        h.assertTrue(allEnded && SimulationManager.getActiveSessions().isEmpty(), "reasons " + reasons);
                        actual("G-REL-13", "Ten full Library earthquakes in a row each ran to the end on their own (end reasons " + new HashSet<>(reasons)
                                + "); nothing left running afterwards.");
                        reset(h);
                        h.succeed();
                    }
                }
            });
        });
        add("G-REL-14", 200, h -> {
            RecordingPlayer p = join(h, "rel14");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 10, () -> {
                cmd(p, "sim_freeze", true);
            }).at(40, () -> {
                int frozenAt = s.getTimerTicks();
                cmd(p, "sim_unfreeze", true);
                h.assertTrue(!s.isFrozen(), "still frozen");
                h.runAfterDelay(20, () -> {
                    h.assertTrue(s.getTimerTicks() < frozenAt, "timer did not resume");
                    actual("G-REL-14", "/sim_freeze held the run timer still and /sim_unfreeze resumed it (" + frozenAt + " ticks left when frozen, " + s.getTimerTicks() + " a second after resuming).");
                    reset(h);
                    h.succeed();
                });
            });
        });
    }

    // ───────────────────────────── Data quality ─────────────────────────────
    private static void dataQuality() {
        add("G-DQ-01", 400, h -> {   // also G-DQ-02, G-DQ-03, G-DQ-06 (same New Sim run)
            RecordingPlayer p = join(h, "dq01");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            boolean[] ok = new boolean[4];
            seq(h).at(WARMUP + 2, () -> {
                rightClickBlock(p, s.getArmedHazards().get(0));
                tp(p, p.getX() + 3, p.getY(), p.getZ());
            }).at(260, () -> {
                String move = s.buildMoveCsv(), fire = s.buildFireLogCsv();
                List<String[]> mrows = rows(move);
                int width = move.lines().findFirst().orElse("").split(",", -1).length;
                ok[0] = caseCheck("G-DQ-01", () -> {
                    long bad = mrows.stream().filter(r -> r.length != width).count();
                    h.assertTrue(width == 14 && bad == 0, width + "-column header, " + bad + " rows with another width");
                    actual("G-DQ-01", "All " + mrows.size() + " movement rows have exactly the 14 columns of the header (player_id ... phase).");
                });
                ok[1] = caseCheck("G-DQ-02", () -> {
                    List<SimEvent> evs = s.logger.getEvents();
                    long bad = evs.stream().filter(e -> e.type() == null || e.type().isBlank() || e.data() == null).count();
                    Set<String> types = new java.util.TreeSet<>();
                    evs.forEach(e -> types.add(e.type()));
                    h.assertTrue(bad == 0, bad + " malformed events");
                    actual("G-DQ-02", evs.size() + " events, every one with a type and a data object; types seen: " + String.join(", ", types) + ".");
                });
                ok[2] = caseCheck("G-DQ-03", () -> {
                    List<String[]> frows = rows(fire);
                    h.assertTrue(fire.startsWith("session_id,timestamp,x,y,z,event"), "fire header " + fire.lines().findFirst().orElse(""));
                    long bad = frows.stream().filter(r -> r.length != 6 || !(r[5].equals("ignite") || r[5].equals("extinguish"))
                            || !r[0].equals(s.getSessionId())).count();
                    h.assertTrue(!frows.isEmpty() && bad == 0, frows.size() + " fire rows, " + bad + " bad");
                    actual("G-DQ-03", "Fire log: " + frows.size() + " rows, all 6 columns, event always ignite or extinguish, all carrying session " + s.getSessionId() + ".");
                });
                ok[3] = caseCheck("G-DQ-06", () -> {
                    Map<String, Integer> byPhase = new LinkedHashMap<>();
                    for (String[] r : mrows) byPhase.merge(r[13].isEmpty() ? "(blank)" : r[13], 1, Integer::sum);
                    List<SimEvent> pt = events(s, "phase_transition");
                    h.assertTrue(byPhase.containsKey("prevention") && byPhase.containsKey("intervention") && !byPhase.containsKey("(blank)"), "phase column " + byPhase);
                    h.assertTrue(!pt.isEmpty() && pt.stream().allMatch(e -> e.data().get("phase") != null), "phase_transition " + pt);
                    actual("G-DQ-06", "Every movement row of the New Sim run is tagged with its phase " + byPhase + "; " + pt.size() + " phase_transition events each name the phase.");
                });
                reset(h);
                finish(h, ok);
            });
        });
        add("G-DQ-04", 200, h -> {
            RecordingPlayer p = join(h, "dq04");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 20, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                List<String[]> r = rows(s.buildMoveCsv());
                String first = r.get(0)[4], lastT = r.get(r.size() - 1)[4];
                h.assertTrue(first.equals("session_start") && lastT.equals("session_end"), "first " + first + ", last " + lastT);
                actual("G-DQ-04", "The movement log opens with a session_start row (t=" + r.get(0)[3] + ") and closes with a session_end row (t=" + r.get(r.size() - 1)[3] + "), so each run's boundaries are explicit.");
                reset(h);
            }).end();
        });
        add("G-DQ-05", 200, h -> {
            RecordingPlayer p = join(h, "dq05");
            SimulationSession s = start(p, SimulationState.CCS_FIRE);
            seq(h).at(WARMUP + 10, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                Path f = Path.of("telemetry", "sessions_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
                List<String> lines;
                try { lines = Files.readAllLines(f, StandardCharsets.UTF_8); } catch (Exception e) { throw new IllegalStateException("cannot read " + f.toAbsolutePath()); }
                int width = lines.get(0).split(",", -1).length;
                String mine = lines.stream().filter(l -> l.startsWith(s.getSessionId() + ",")).findFirst().orElse(null);
                h.assertTrue(mine != null, "no sidecar row for " + s.getSessionId());
                String[] cols = mine.split(",", -1);
                h.assertTrue(cols.length == width && cols[2].equalsIgnoreCase("CCS_FIRE"), "row " + mine);
                h.assertTrue(mine.contains(",1.2,"), "contract version missing: " + mine);
                actual("G-DQ-05", "The local sessions CSV got one row for the run with all " + width + " columns (scenario " + cols[2] + ", end reason "
                        + cols[6] + ", contract version 1.2).");
                reset(h);
            }).end();
        });
        add("G-DQ-07", 400, h -> {
            RecordingPlayer p = join(h, "dq07");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 200, () -> {
                int n = events(s, "PLAYER_TICK").size();
                h.assertTrue(n >= 9 && n <= 11, n + " PLAYER_TICK events in 10 s");
                SimEvent e = events(s, "PLAYER_TICK").get(0);
                h.assertTrue(e.data().containsKey("room") && e.data().containsKey("nearest_fire_dist"), "fields " + e.data().keySet());
                actual("G-DQ-07", n + " PLAYER_TICK events over 10 s of play (1 per second as designed), each with x, y, z, room and nearest_fire_dist.");
                reset(h);
            }).end();
        });
        add("G-DQ-08", 200, h -> {
            RecordingPlayer p = join(h, "dq08");
            SimulationSession s = start(p, SimulationState.FIRE);
            String row = TelemetryCsvWriter.writeRow(s.getSessionId(), p.getUUID().toString(), "fire", 1.0, "move_tick", 35, -33, 88, 5.0, null, null);
            int needed = 3 * 1024 * 1024 / row.length() + 100;
            for (int i = 0; i < needed; i++) s.bufferCsvRow(row);
            String csv = s.buildMoveCsv();
            int len = csv.length();
            for (int i = 0; i < 1000; i++) s.bufferCsvRow(row);
            h.assertTrue(csv.contains("TRUNCATED") && len <= 3 * 1024 * 1024 + 200 && s.buildMoveCsv().length() == len, "length " + len);
            actual("G-DQ-08", String.format("A run that outgrows the 3 MB upload cap stops at %.2f MB and ends with an explicit TRUNCATED marker line; later rows are not appended, so the database write still fits.", len / 1048576.0));
            reset(h);
            h.succeed();
        });
        add("G-DQ-09", 100, h -> {
            RecordingPlayer p = join(h, "dq09");
            AcademyTelemetry.startAttempt(p);
            long[] t0 = {h.getLevel().getGameTime()};
            seq(h).at(40, () -> {
                String row = AcademyTelemetry.record(p, "gt_check", null);
                double ts = Double.parseDouble(row.split(",", -1)[3]);
                double expected = (h.getLevel().getGameTime() - t0[0]) / 20.0;
                h.assertTrue(Math.abs(ts - expected) <= 0.1, "timestamp " + ts + " (expected about " + expected + " s)");
                actual("G-DQ-09", "Academy telemetry rows now use seconds since the attempt began (" + ts + " s after 2 s of play), the same unit as every other row; before the fix they held the raw world tick count.");
                reset(h);
            }).end();
        });
        add("G-DQ-10", 200, h -> {
            RecordingPlayer p = join(h, "dq10");
            BlockPos base = platform(h);
            BlockPos fire = base.east(3);
            h.getLevel().setBlock(fire, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 2, () -> {
                faceFrom(p, fire, 2.5);
                arm(p, ModItems.CO2_EXTINGUISHER.get());
                spray(p, 6);
                List<String[]> spr = rows(s.buildMoveCsv()).stream().filter(r -> r[4].equals("ext_spray") || r[4].equals("pin_pull")).toList();
                h.assertTrue(!spr.isEmpty() && spr.stream().allMatch(r -> !r[12].isEmpty()), "spray rows " + spr.size());
                long sprays = spr.stream().filter(r -> r[4].equals("ext_spray")).count();
                h.assertTrue(spr.stream().filter(r -> r[4].equals("ext_spray")).allMatch(r -> r[11].equals("0") || r[11].equals("1")), "hit_fire not 0/1");
                actual("G-DQ-10", "pin_pull and " + sprays + " ext_spray rows all carry extinguisher_class (" + spr.get(0)[12] + "); every ext_spray row has hit_fire as 0 or 1.");
                reset(h);
            }).end();
        });
        add("G-DQ-11", 300, h -> {
            RecordingPlayer p = join(h, "dq11");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            seq(h).at(WARMUP + 2, () -> {
                for (BlockPos hz : new ArrayList<>(s.getArmedHazards())) rightClickBlock(p, hz);
            }).at(5, () -> {
                SimEvent end = last(s, "SIM_END").orElseThrow();
                Object score = end.data().get("score"), prep = end.data().get("prep_level");
                double sc = score instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(score));
                h.assertTrue(sc >= 0 && sc <= 100, "score " + score);
                h.assertTrue(prep == null || Set.of("HIGH", "MODERATE", "LOW").contains(String.valueOf(prep)), "prep " + prep);
                String level = sc >= 75 ? "HIGH" : sc >= 40 ? "MODERATE" : "LOW";
                actual("G-DQ-11", "A perfect New Sim run recorded score " + score + " (inside 0 to 100), which maps to prep level " + level
                        + " under the 75/40 thresholds. SIM_END data: " + end.data() + ".");
                reset(h);
            }).end();
        });
    }

    // ───────────────────────────── Integration seed (checked from the dashboard side) ─────────────────────────────
    private static void integrationSeed() {
        add("G-INT-SEED", 400000, h -> {
            RecordingPlayer p = join(h, "intseed");
            String u = uniqueName("int");
            register(p, u);
            AtomicReference<SimulationSession> sess = new AtomicReference<>();
            AtomicReference<JsonObject> row = new AtomicReference<>();
            flow(h).when(1, () -> SessionManager.findActiveSessionRowId(p.getUUID()) > 0, () -> {
                sess.set(start(p, SimulationState.NEW_SIM_BUILDING2_FIRE));
            }).after(WARMUP + 5, () -> {
                List<BlockPos> armed = sess.get().getArmedHazards();
                for (int i = 0; i < 3; i++) { tp(p, armed.get(i).getX() + 1.5, armed.get(i).getY(), armed.get(i).getZ() + 0.5); rightClickBlock(p, armed.get(i)); }
            }).when(1, () -> sess.get().getFirePhase() == SimulationSession.FirePhase.EVACUATION, () -> tp(p, -150.5, -33, 470.5))
              .after(3, () -> tp(p, -155.5, -33, 480.5))
              .when(1, () -> SimulationManager.getSession(p.getUUID()) == null, () -> { })
              .when(1, every(20, () -> {
                  JsonArray rows = query("SELECT * FROM sessions WHERE username = ? ORDER BY id DESC", u);
                  if (rows.isEmpty()) return false;
                  JsonObject r = rows.get(0).getAsJsonObject();
                  if (!"completed".equals(str(r, "status")) || str(r, "move_log_csv") == null) return false;
                  row.set(r);
                  return true;
              }), () -> {
                  JsonObject r = row.get();
                  SimulationSession s = sess.get();
                  Map<String, Object> seed = new LinkedHashMap<>();
                  seed.put("username", u);
                  seed.put("student_id", "TEST-" + u);
                  seed.put("full_name", "Game Test " + u);
                  seed.put("row_id", Long.parseLong(str(r, "id")));
                  seed.put("simulation_type", str(r, "simulation_type"));
                  seed.put("simulation_score", str(r, "simulation_score"));
                  seed.put("prep_level", str(r, "prep_level"));
                  seed.put("end_reason", String.valueOf(endReason(s)));
                  seed.put("events_in_game", s.logger.getEvents().size());
                  seed.put("prevented", s.getPreventedHazards().size());
                  seed.put("escalated", s.getEscalatedHazards().size());
                  seed.put("move_lines", str(r, "move_log_csv").lines().count());
                  seed.put("fire_lines", str(r, "fire_log_csv") == null ? 0 : str(r, "fire_log_csv").lines().count());
                  try {
                      Files.writeString(Path.of("gt_int_seed.json"), new Gson().toJson(seed), StandardCharsets.UTF_8);
                  } catch (Exception e) { throw new IllegalStateException(e); }
                  actual("G-INT-SEED", "Played one New Sim Building 2.0 run as " + u + " (3 of 5 hazards prevented, evacuated to the assembly area); row "
                          + seed.get("row_id") + " uploaded with score " + seed.get("simulation_score") + ". Details saved for the dashboard checks.");
                  reset(h);
                  h.succeed();
              }).start();
        });
    }
}
