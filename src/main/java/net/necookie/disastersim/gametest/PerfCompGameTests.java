package net.necookie.disastersim.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.common.structure.AcademyBuildingManager;
import net.necookie.disastersim.common.structure.LobbyManager;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.PasswordHasher;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/** Game performance (G-PERF) and compatibility (G-COMP) cases. */
final class PerfCompGameTests {

    private PerfCompGameTests() {}

    private static final int WARMUP = 60;
    private static final double TICK_BUDGET_MS = 50.0; // 20 ticks per second

    private static RecordingPlayer join(GameTestHelper h, String tag) { reset(h); return RecordingPlayer.join(h, tag); }

    private static SimulationSession start(RecordingPlayer p, SimulationState state) {
        SimulationManager.startSimulation(p, state);
        SimulationSession s = SimulationManager.getSession(p.getUUID());
        if (s == null) throw new IllegalStateException("simulation did not start: " + p.messages);
        p.setInvulnerable(true);
        return s;
    }

    /** Tick-time case: starts {@code state}, measures {@code window} ticks beginning {@code from} ticks after start. */
    private static void tickCase(String id, String label, SimulationState state, int from, int window) {
        add(id, from + window + 200, h -> {
            RecordingPlayer p = join(h, id.toLowerCase().replace("-", ""));
            SimulationSession s = state == null ? null : start(p, state);
            int[] mark = {0};
            String[] phaseAtStart = {""};
            seq(h).at(from, () -> {
                mark[0] = TickProfiler.mark();
                phaseAtStart[0] = s == null ? "idle" : phaseOf(s);
            }).at(window, () -> {
                List<Double> xs = TickProfiler.since(mark[0]);
                TickProfiler.Stats st = TickProfiler.stats(xs);
                metric(id, Map.of("samples_ms", xs, "median", st.median(), "p95", st.p95(), "max", st.max()));
                h.assertTrue(st.p95() < TICK_BUDGET_MS, label + ": " + st.text() + " (a tick must stay under 50 ms for 20 TPS)");
                actual(id, label + " (" + phaseAtStart[0] + "): " + st.text() + ". Budget 50 ms per tick for a steady 20 TPS.");
                reset(h);
            }).end();
        });
    }

    private static String phaseOf(SimulationSession s) {
        if (s.getFirePhase() != null) return "phase " + s.getFirePhase().name();
        if (s.getQuakePhase() != null) return "phase " + s.getQuakePhase().name();
        return s.getState().name();
    }

    /** Start-time case: median wall time of startSimulation over several runs. */
    private static void startTimeCase(String id, String label, SimulationState state) {
        add(id, 400, h -> {
            RecordingPlayer p = join(h, id.toLowerCase().replace("-", ""));
            List<Double> xs = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                long t0 = System.nanoTime();
                SimulationManager.startSimulation(p, state);
                xs.add((System.nanoTime() - t0) / 1e6);
                SimulationManager.endSimulation(p.getUUID(), "test_reset");
            }
            TickProfiler.Stats st = TickProfiler.stats(xs);
            metric(id, Map.of("samples_ms", xs));
            h.assertTrue(st.median() < 3000, label + " took " + st.text());
            actual(id, String.format("%s: median %.0f ms, slowest %.0f ms over %d starts (placing the building, scanning hazards, spawning the player). Target under 3,000 ms.",
                    label, st.median(), st.max(), st.n()));
            reset(h);
            h.succeed();
        });
    }

    private static long countNonAir(ServerLevel level, BlockPos from, BlockPos to) {
        long n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) if (!level.getBlockState(pos).isAir()) n++;
        return n;
    }

    private static List<Identifier> modIds(net.minecraft.core.Registry<?> registry) {
        return registry.keySet().stream().filter(k -> k.getNamespace().equals("berongsmp")).sorted().toList();
    }

    private static boolean resource(String path) {
        return PerfCompGameTests.class.getResource("/" + path) != null;
    }

    static void registerAll() {
        TickProfiler.install();

        // ───────────── Performance ─────────────
        tickCase("G-PERF-01", "Idle server with the mod loaded", null, 20, 200);
        tickCase("G-PERF-02", "Library fire run", SimulationState.FIRE, WARMUP + 5, 200);
        tickCase("G-PERF-03", "CCS Admin Building fire run", SimulationState.CCS_FIRE, WARMUP + 5, 200);
        tickCase("G-PERF-04", "New Sim Building 2.0 prevention phase", SimulationState.NEW_SIM_BUILDING2_FIRE, WARMUP + 5, 180);
        tickCase("G-PERF-05", "New Sim Building 2.0 intervention phase (hazards burning)", SimulationState.NEW_SIM_BUILDING2_FIRE, WARMUP + 205, 180);
        tickCase("G-PERF-06", "New Sim Building 2.0 evacuation phase (fire spreading)", SimulationState.NEW_SIM_BUILDING2_FIRE, WARMUP + 410, 180);
        tickCase("G-PERF-07", "Library earthquake at peak shaking (blocks breaking)", SimulationState.EARTHQUAKE, WARMUP + 45, 180);
        tickCase("G-PERF-08", "Library earthquake during aftershocks", SimulationState.EARTHQUAKE, WARMUP + 245, 60);

        add("G-PERF-09", 700, h -> {
            reset(h);
            RecordingPlayer a = RecordingPlayer.join(h, "perf09a"), b = RecordingPlayer.join(h, "perf09b"), c = RecordingPlayer.join(h, "perf09c");
            start(a, SimulationState.FIRE); start(b, SimulationState.CCS_FIRE); start(c, SimulationState.NEW_SIM_BUILDING2_FIRE);
            int[] mark = {0};
            seq(h).at(WARMUP + 5, () -> mark[0] = TickProfiler.mark()).at(200, () -> {
                TickProfiler.Stats st = TickProfiler.stats(TickProfiler.since(mark[0]));
                metric("G-PERF-09", Map.of("samples_ms", TickProfiler.since(mark[0])));
                h.assertTrue(SimulationManager.getActiveSessions().size() == 3, "sessions running: " + SimulationManager.getActiveSessions().size());
                h.assertTrue(st.p95() < TICK_BUDGET_MS, "three runs at once: " + st.text());
                actual("G-PERF-09", "Three runs at once (Library fire, CCS fire, New Sim Building 2.0): " + st.text() + ". Budget 50 ms.");
                reset(h);
            }).end();
        });
        startTimeCase("G-PERF-10", "Starting a Library fire run", SimulationState.FIRE);
        startTimeCase("G-PERF-11", "Starting a CCS fire run", SimulationState.CCS_FIRE);
        startTimeCase("G-PERF-12", "Starting a New Sim Building 2.0 run", SimulationState.NEW_SIM_BUILDING2_FIRE);
        add("G-PERF-13", 400, h -> {
            RecordingPlayer p = join(h, "perf13");
            List<Double> xs = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                SimulationManager.startSimulation(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
                long t0 = System.nanoTime();
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                xs.add((System.nanoTime() - t0) / 1e6);
            }
            TickProfiler.Stats st = TickProfiler.stats(xs);
            metric("G-PERF-13", Map.of("samples_ms", xs));
            h.assertTrue(st.median() < 3000, "ending a run took " + st.text());
            actual("G-PERF-13", String.format("Ending a New Sim Building 2.0 run (scoring, telemetry flush, queuing the upload, restoring the building): median %.0f ms, slowest %.0f ms. Target under 3,000 ms.", st.median(), st.max()));
            reset(h);
            h.succeed();
        });
        add("G-PERF-14", 400000, h -> {   // also G-PERF-15 (login latency for the same account)
            RecordingPlayer p = join(h, "perf14");
            String u = uniqueName("pf");
            AtomicLong t0 = new AtomicLong(System.nanoTime());
            cmd(p, "register " + u + " \"PerfPass#1\" TEST-" + u + " TEST3A Perf Tester", false);
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            double[] reg = {0};
            boolean[] ok = new boolean[2];
            flow(h).when(1, () -> AuthManager.current(p.getUUID()) != null, () -> {
                reg[0] = (System.nanoTime() - t0.get()) / 1e6;
                ok[0] = caseCheck("G-PERF-14", () -> {
                    h.assertTrue(reg[0] < 3000, "registration took " + reg[0] + " ms");
                    actual("G-PERF-14", String.format("/register finished in %.0f ms (password hashing with 210,000 PBKDF2 iterations plus two Turso calls). Target under 3,000 ms.", reg[0]));
                });
                cmd(p, "logout", false);
                p2.set(RecordingPlayer.join(h, "perf15"));
                t0.set(System.nanoTime());
                cmd(p2.get(), "login " + u + " PerfPass#1", false);
            }).when(1, () -> AuthManager.current(p2.get().getUUID()) != null, () -> {
                double ms = (System.nanoTime() - t0.get()) / 1e6;
                ok[1] = caseCheck("G-PERF-15", () -> {
                    h.assertTrue(ms < 3000, "login took " + ms + " ms");
                    actual("G-PERF-15", String.format("/login finished in %.0f ms (account lookup plus password check). Target under 3,000 ms.", ms));
                });
                metric("G-PERF-14", Map.of("register_ms", reg[0], "login_ms", ms));
                finish(h, ok);
            }).start();
        });
        add("G-PERF-16", 100, h -> {
            List<Double> xs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long t0 = System.nanoTime();
                PasswordHasher.hash("Timing#2026");
                xs.add((System.nanoTime() - t0) / 1e6);
            }
            TickProfiler.Stats st = TickProfiler.stats(xs);
            metric("G-PERF-16", Map.of("samples_ms", xs));
            h.assertTrue(st.median() < 1000, "hash " + st.text());
            actual("G-PERF-16", String.format("One password hash (PBKDF2, 210,000 iterations) takes a median of %.0f ms off the server thread. Target under 1,000 ms.", st.median()));
            h.succeed();
        });
        add("G-PERF-17", 200, h -> {
            RecordingPlayer p = join(h, "perf17");
            SimulationSession s = start(p, SimulationState.FIRE);
            for (int i = 0; i < 12_000; i++) {  // a 10-minute run at 20 Hz
                s.bufferCsvRow(TelemetryCsvWriter.writeRow(s.getSessionId(), p.getUUID().toString(), "fire",
                        i / 20.0, "move_tick", 35 + i % 10, -33, 88, 5.0, null, null));
            }
            long t0 = System.nanoTime();
            String csv = s.buildMoveCsv();
            double ms = (System.nanoTime() - t0) / 1e6;
            metric("G-PERF-17", Map.of("build_ms", ms, "chars", csv.length()));
            h.assertTrue(ms < 200, "building the movement log took " + ms + " ms");
            actual("G-PERF-17", String.format("Assembling the movement log of a 10-minute run (12,000 rows, %.1f MB) for upload took %.1f ms. Target under 200 ms.", csv.length() / 1e6, ms));
            reset(h);
            h.succeed();
        });
        add("G-PERF-18", 300, h -> {
            RecordingPlayer p = join(h, "perf18");
            start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 100, () -> {
                long t0 = System.nanoTime();
                TelemetryCsvWriter.flush();
                double ms = (System.nanoTime() - t0) / 1e6;
                metric("G-PERF-18", Map.of("flush_ms", ms));
                h.assertTrue(ms < 100, "flush took " + ms + " ms");
                actual("G-PERF-18", String.format("Flushing the local telemetry CSV during a run took %.2f ms. Target under 100 ms.", ms));
                reset(h);
            }).end();
        });
        add("G-PERF-19", 300, h -> {
            RecordingPlayer p = join(h, "perf19");
            start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 20, () -> {
                ServerLevel level = h.getLevel();
                BlockPos origin = SimulationManager.SIM_POS.offset(10, 2, 10);
                long t0 = System.nanoTime();
                for (int i = 0; i < 200; i++) SimulationManager.nearestFireDistance(level, origin.offset(i % 3, 0, 0));
                double perCall = (System.nanoTime() - t0) / 1e6 / 200;
                metric("G-PERF-19", Map.of("ms_per_call", perCall));
                h.assertTrue(perCall < 2.0, "fire distance scan " + perCall + " ms per call");
                actual("G-PERF-19", String.format("The nearest-fire scan (about 4,800 blocks, used for hazard distance) costs %.3f ms per call. Target under 2 ms.", perCall));
                reset(h);
            }).end();
        });
        add("G-PERF-20", 2000, h -> {
            RecordingPlayer p = join(h, "perf20");
            Runtime rt = Runtime.getRuntime();
            System.gc();
            long before = rt.totalMemory() - rt.freeMemory();
            for (int i = 0; i < 20; i++) {
                SimulationManager.startSimulation(p, SimulationState.FIRE);
                SimulationManager.endSimulation(p.getUUID(), "test_reset");
            }
            System.gc();
            long after = rt.totalMemory() - rt.freeMemory();
            double growthMb = (after - before) / 1e6;
            metric("G-PERF-20", Map.of("before_mb", before / 1e6, "after_mb", after / 1e6));
            h.assertTrue(growthMb < 64, "heap grew by " + growthMb + " MB");
            h.assertTrue(SimulationManager.getActiveSessions().isEmpty(), "sessions left behind");
            actual("G-PERF-20", String.format("Memory in use after 20 back-to-back runs changed by %.1f MB (%.0f MB to %.0f MB after garbage collection); no sessions left behind. Target under 64 MB growth.", growthMb, before / 1e6, after / 1e6));
            reset(h);
            h.succeed();
        });

        // ───────────── Compatibility ─────────────
        add("G-COMP-01", 20, h -> {
            int v = Runtime.version().feature();
            h.assertTrue(v == 25, "running on Java " + Runtime.version());
            actual("G-COMP-01", "Running on Java " + Runtime.version() + " (" + System.getProperty("java.vendor") + "), the version the mod targets.");
            h.succeed();
        });
        add("G-COMP-02", 20, h -> {
            String mc = ModList.get().getModContainerById("minecraft").orElseThrow().getModInfo().getVersion().toString();
            h.assertTrue(mc.startsWith("26.2"), "Minecraft " + mc);
            actual("G-COMP-02", "Minecraft version " + mc + " (the mod is built for 26.2.0).");
            h.succeed();
        });
        add("G-COMP-03", 20, h -> {
            String nf = ModList.get().getModContainerById("neoforge").orElseThrow().getModInfo().getVersion().toString();
            String mod = ModList.get().getModContainerById("berongsmp").orElseThrow().getModInfo().getVersion().toString();
            h.assertTrue(nf.startsWith("26.2.0"), "NeoForge " + nf);
            actual("G-COMP-03", "BerongSMP " + mod + " loaded on NeoForge " + nf + ", the server build used on MCServerHost.");
            h.succeed();
        });
        add("G-COMP-04", 20, h -> {
            String toml;
            try (InputStream in = PerfCompGameTests.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
                toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) { throw new IllegalStateException("cannot read neoforge.mods.toml", e); }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("modId\\s*=\\s*\"neoforge\"[\\s\\S]*?versionRange\\s*=\\s*\"([^\"]+)\"").matcher(toml);
            h.assertTrue(m.find(), "no neoforge dependency range in mods.toml");
            String range = m.group(1);
            try {
                var vr = org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec(range);
                boolean server = vr.containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion("26.2.0.0-beta"));
                boolean client = vr.containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion("26.2.0.7-beta"));
                h.assertTrue(server && client, "range " + range + " accepts 26.2.0.0: " + server + ", 26.2.0.7: " + client);
            } catch (org.apache.maven.artifact.versioning.InvalidVersionSpecificationException e) { throw new IllegalStateException(e); }
            actual("G-COMP-04", "The mod's NeoForge dependency range " + range + " accepts both the server build 26.2.0.0-beta and the client build 26.2.0.7-beta that the installer bundles.");
            h.succeed();
        });
        add("G-COMP-05", 20, h -> {
            boolean we = ModList.get().isLoaded("worldedit");
            boolean copyroom = h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("/copyroom") != null;
            h.assertTrue(!we && !copyroom, "worldedit loaded " + we + ", //copyroom registered " + copyroom);
            actual("G-COMP-05", "Server runs without WorldEdit installed: the optional //copyroom tool is simply not registered and nothing fails to load.");
            h.succeed();
        });
        add("G-COMP-06", 200, h -> {
            ServerLevel level = h.getLevel();
            long n = countNonAir(level, LobbyManager.LOBBY_POS, LobbyManager.LOBBY_POS.offset(20, 6, 20));
            h.assertTrue(n > 200, "lobby blocks " + n);
            actual("G-COMP-06", "Lobby structure (lobby_structure.nbt) is in place: " + n + " blocks in its footprint.");
            h.succeed();
        });
        add("G-COMP-07", 600, h -> {
            ServerLevel level = h.getLevel();
            SimulationManager.placeAllBuildings(level);
            BlockPos lib = SimulationManager.SIM_POS;
            long library = countNonAir(level, lib, lib.offset(29, 9, 29));
            BlockPos ssc = new BlockPos(11, -33, 90); // SimulationManager.SSC_POS (private)
            long sscN = countNonAir(level, ssc.offset(-12, 0, 0), ssc.offset(12, 8, 12));
            boolean[] ok = new boolean[3];
            ok[0] = caseCheck("G-COMP-07", () -> {
                h.assertTrue(library > 1000, "library blocks " + library);
                actual("G-COMP-07", "LSPU Library structure (lspu_library_main.nbt) placed: " + library + " blocks in a 30 x 10 x 30 area from its origin.");
            });
            ok[1] = caseCheck("G-COMP-08", () -> {
                h.assertTrue(sscN > 300, "SSC blocks " + sscN);
                actual("G-COMP-08", "SSC Building (ssc_building.schem, Sponge schematic, rotated once) placed: " + sscN + " blocks around its origin.");
            });
            BlockPos ccs = SimulationManager.CCS_POS;
            long ccsN = countNonAir(level, ccs, ccs.offset(59, 9, 67));
            ok[2] = caseCheck("G-COMP-09", () -> {
                h.assertTrue(ccsN > 3000, "CCS blocks " + ccsN);
                actual("G-COMP-09", "CCS Admin Building (ccs_admin_building.schem) placed: " + ccsN + " blocks in its 60 x 68 footprint.");
            });
            finish(h, ok);
        });
        add("G-COMP-10", 400, h -> {
            // Schematic NPCs are stored with their chunks and only attach to the world once those
            // chunks are loaded for entities (normally because a student walks in). Mock players do
            // not drive chunk loading, so force-load the Academy's chunks for the check, then release them.
            ServerLevel level = h.getLevel();
            BlockPos a = AcademyBuildingManager.POS;
            long n = countNonAir(level, a, a.offset(81, 7, 76));
            List<int[]> chunks = new ArrayList<>();
            for (int cx = (a.getX() - 8) >> 4; cx <= (a.getX() + 90) >> 4; cx++)
                for (int cz = (a.getZ() - 8) >> 4; cz <= (a.getZ() + 85) >> 4; cz++) {
                    level.setChunkForced(cx, cz, true);
                    chunks.add(new int[]{cx, cz});
                }
            seq(h).at(100, () -> {
                java.util.Map<net.necookie.disastersim.entity.NpcType, Integer> found = new java.util.TreeMap<>();
                for (CustomNpcEntity e : level.getEntitiesOfClass(CustomNpcEntity.class,
                        new AABB(a.getX() - 8, a.getY() - 5, a.getZ() - 8, a.getX() + 90, a.getY() + 20, a.getZ() + 85)))
                    found.merge(e.getNpcType(), 1, Integer::sum);
                for (int[] c : chunks) level.setChunkForced(c[0], c[1], false);
                var need = java.util.EnumSet.of(net.necookie.disastersim.entity.NpcType.OFFICER_CRUZ, net.necookie.disastersim.entity.NpcType.SGT_REYES,
                        net.necookie.disastersim.entity.NpcType.SGT_SANTOS, net.necookie.disastersim.entity.NpcType.CAPT_MORFE);
                h.assertTrue(n > 5000 && found.keySet().containsAll(need), "academy blocks " + n + ", NPCs found " + found);
                actual("G-COMP-10", "Academy building (academy_building.schem) placed with " + n + " blocks; once its chunks load, all four instructors are present ("
                        + found.values().stream().mapToInt(Integer::intValue).sum() + " NPCs in total: " + found + ").");
            }).end();
        });
        add("G-COMP-11", 400, h -> {
            ServerLevel level = h.getLevel();
            SimulationManager.placeAllBuildings(level);
            int hazards = SimulationManager.scanNewSimBuilding2Hazards(level).size();
            h.assertTrue(hazards >= 150, "hazard props found " + hazards);
            actual("G-COMP-11", "New Sim Building 2.0 (new_sim_building2.0.schem) placed; the building-wide scan finds " + hazards + " hazard-capable props.");
            h.succeed();
        });
        add("G-COMP-12", 20, h -> {
            List<Identifier> blocks = modIds(BuiltInRegistries.BLOCK);
            List<String> missing = blocks.stream().filter(id -> !resource("assets/berongsmp/blockstates/" + id.getPath() + ".json")).map(Identifier::getPath).toList();
            h.assertTrue(missing.isEmpty(), missing.size() + " blocks without a blockstate: " + missing);
            actual("G-COMP-12", "All " + blocks.size() + " BerongSMP blocks have a blockstate definition, so none render as missing-texture blocks.");
            h.succeed();
        });
        add("G-COMP-13", 20, h -> {
            JsonObject lang;
            try (InputStream in = PerfCompGameTests.class.getResourceAsStream("/assets/berongsmp/lang/en_us.json")) {
                lang = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) { throw new IllegalStateException(e); }
            List<String> missing = new ArrayList<>();
            for (Identifier id : modIds(BuiltInRegistries.ITEM)) {
                String key = BuiltInRegistries.ITEM.getValue(id).getDescriptionId();
                if (!lang.has(key)) missing.add(id.getPath());
            }
            h.assertTrue(missing.isEmpty(), missing.size() + " items without an English name: " + missing);
            actual("G-COMP-13", "All " + modIds(BuiltInRegistries.ITEM).size() + " BerongSMP items have an English display name in en_us.json.");
            h.succeed();
        });
        add("G-COMP-14", 20, h -> {
            List<Identifier> items = modIds(BuiltInRegistries.ITEM);
            List<String> missing = items.stream().filter(id -> !resource("assets/berongsmp/items/" + id.getPath() + ".json")).map(Identifier::getPath).toList();
            h.assertTrue(missing.isEmpty(), missing.size() + " items without an item model: " + missing);
            actual("G-COMP-14", "All " + items.size() + " BerongSMP items have an item model definition (assets/berongsmp/items).");
            h.succeed();
        });
        add("G-COMP-15", 40, h -> {
            RecordingPlayer p = join(h, "comp15");
            List<String> ids = List.of("sim_status", "drop_and_roll", "tutorial_status", "academy_status", "academy_shake", "academy_compass");
            List<String> missing = ids.stream().filter(i -> !NetworkRegistry.hasChannel(p.connection, Identifier.fromNamespaceAndPath("berongsmp", i))).toList();
            h.assertTrue(missing.isEmpty(), "unregistered payload channels: " + missing);
            actual("G-COMP-15", "All 6 BerongSMP network channels (" + String.join(", ", ids) + ") are registered, so a NeoForge client with the mod can receive HUD, shake and compass updates.");
            reset(h);
            h.succeed();
        });
        add("G-COMP-16", 20, h -> {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("simDurationTicks", Config.SIM_DURATION_TICKS.getDefault());
            defaults.put("passThresholdFire", Config.PASS_THRESHOLD_FIRE.getDefault());
            defaults.put("academyPassThreshold", Config.ACADEMY_PASS_THRESHOLD.getDefault());
            defaults.put("quakeMagnitude", Config.QUAKE_MAGNITUDE.getDefault());
            defaults.put("newSimBuilding2PreventionTicks", Config.NEW_SIM2_PREVENTION_TICKS.getDefault());
            h.assertTrue(defaults.get("simDurationTicks").equals(2400) && defaults.get("passThresholdFire").equals(5)
                    && defaults.get("academyPassThreshold").equals(70) && defaults.get("newSimBuilding2PreventionTicks").equals(2400), "defaults " + defaults);
            actual("G-COMP-16", "A fresh berongsmp-common.toml gets the documented defaults: " + defaults + ".");
            h.succeed();
        });
        add("G-COMP-17", 400, h -> {
            RecordingPlayer p = join(h, "comp17");
            SimulationSession s = start(p, SimulationState.CCS_EARTHQUAKE);
            seq(h).at(WARMUP + 60, () -> {
                h.assertTrue(s.getQuakePhase() == SimulationSession.EarthquakePhase.PEAK, "phase " + s.getQuakePhase());
                actual("G-COMP-17", "Legacy CCS earthquake (admin path) still runs on this build: reached PEAK shaking after the 2-second rumble.");
                reset(h);
            }).end();
        });
        add("G-COMP-18", 300, h -> {
            RecordingPlayer p = join(h, "comp18");
            SimulationSession s = start(p, SimulationState.CCS_FIRE);
            int computers = s.getComputerPositions().size();
            long burning = s.getComputerPositions().stream()
                    .filter(pos -> h.getLevel().getBlockState(pos).getBlock() instanceof net.necookie.disastersim.block.ComputerBlock
                            && h.getLevel().getBlockState(pos).getValue(net.necookie.disastersim.block.ComputerBlock.BURNING)).count();
            h.assertTrue(computers > 0 && burning >= 1, "computers " + computers + ", burning " + burning);
            actual("G-COMP-18", "Legacy CCS fire (admin path) still runs: " + computers + " computers found in the building and " + burning + " set burning to start the fire.");
            reset(h);
            h.succeed();
        });
        add("G-COMP-19", 20, h -> {
            Path meta = Path.of("telemetry", "map_metadata.json");
            String text;
            try { text = Files.readString(meta); } catch (Exception e) { throw new IllegalStateException("no " + meta.toAbsolutePath()); }
            JsonObject j = JsonParser.parseString(text).getAsJsonObject();
            h.assertTrue(text.contains("new_sim_building2_fire"), "map_metadata.json has no New Sim Building 2.0 section");
            actual("G-COMP-19", "Telemetry map_metadata.json is valid JSON with " + j.keySet().size() + " top-level sections, including new_sim_building2_fire, for the ML side to read.");
            h.succeed();
        });
        add("G-COMP-20", 20, h -> {
            var dist = net.neoforged.fml.loading.FMLEnvironment.getDist();
            h.assertTrue(dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER, "dist " + dist);
            actual("G-COMP-20", "The mod loads and runs on the server distribution (" + dist + ", no window or graphics), so its client-only HUD code is kept off the server as on MCServerHost.");
            h.succeed();
        });
    }
}
