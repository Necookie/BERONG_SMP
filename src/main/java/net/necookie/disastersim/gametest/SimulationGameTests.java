package net.necookie.disastersim.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.common.hazard.HazardBlock;
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.common.player.DropAndRollManager;
import net.necookie.disastersim.common.player.DuckCoverHoldManager;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.common.zones.AssemblyZone;
import net.necookie.disastersim.common.zones.ExitZones;
import net.necookie.disastersim.item.AbstractExtinguisherItem;
import net.necookie.disastersim.registry.ModBlocks;
import net.necookie.disastersim.registry.ModItems;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/**
 * In-world system tests (catalog type GT, plus the INT cases that need a live server).
 * Case IDs match Testing/harness/catalog.cjs. Game tests run in registry order, not in the order
 * they are added here, so cases that inspect one shared run are grouped into a single test that
 * records each case separately (see {@link GT#caseCheck}).
 * Timings assume the run-gametest config written by Testing/harness/game/gt_config.mjs:
 * New Sim Building 2.0 prevention/intervention/evacuation 200 ticks each, quake rumble 40 /
 * peak 200 / aftershock 40, simDurationTicks 600. Every scenario has a 60-tick warmup.
 */
final class SimulationGameTests {

    private SimulationGameTests() {}

    private static final int WARMUP = 60;
    private static final String PW = "GtPass#2026";

    private static RecordingPlayer join(GameTestHelper h, String tag) { reset(h); return RecordingPlayer.join(h, tag); }

    private static SimulationSession start(RecordingPlayer p, SimulationState state) {
        SimulationManager.startSimulation(p, state);
        SimulationSession s = SimulationManager.getSession(p.getUUID());
        if (s == null) throw new IllegalStateException("simulation did not start: " + p.messages);
        p.setInvulnerable(true); // keep the mock player alive through fire/quake damage
        return s;
    }

    private static Object endReason(SimulationSession s) {
        return last(s, "SIM_END").map(e -> e.data().get("end_reason")).orElse(null);
    }

    private static void register(RecordingPlayer p, String user) {
        cmd(p, "register " + user + " \"" + PW + "\" TEST-" + user + " TEST3A Game Test " + user, false);
    }

    private static BlockPos lobbySimButton() {
        try {
            Field f = net.necookie.disastersim.common.structure.LobbyManager.class.getDeclaredField("simulationButtonPos");
            f.setAccessible(true);
            return (BlockPos) f.get(null);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static void pressLobbySimButton(RecordingPlayer p) {
        BlockPos b = lobbySimButton();
        if (b == null) throw new IllegalStateException("lobby simulation button not found");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, b,
                new BlockHitResult(Vec3.atCenterOf(b), Direction.UP, b, false)));
    }

    /** Extinguisher class the mod treats as correct for a hazard prop. */
    private static Item correctExtinguisher(Block b) {
        if (AbstractExtinguisherItem.isKitchenHazard(b)) return ModItems.WET_CHEMICAL_EXTINGUISHER.get();
        if (AbstractExtinguisherItem.isOxidizerHazard(b)) return ModItems.CO2_EXTINGUISHER.get();
        return ModItems.FIRE_EXTINGUISHER.get();
    }

    /** Free standing spot (air at feet and head, solid floor) within 3 blocks of {@code target}. */
    private static BlockPos standNear(ServerLevel level, BlockPos target) {
        for (int r = 2; r <= 3; r++)
            for (int dy = -1; dy <= 1; dy++)
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos f = target.relative(d, r).above(dy);
                    if (level.getBlockState(f).isAir() && level.getBlockState(f.above()).isAir()
                            && !level.getBlockState(f.below()).isAir()) return f;
                }
        return target.relative(Direction.WEST, 2);
    }

    private static void crouch(RecordingPlayer p, boolean on) {
        p.setShiftKeyDown(on);
        p.setPose(on ? Pose.CROUCHING : Pose.STANDING);
    }

    /**
     * Condition that also becomes true {@code seconds} of wall-clock time after it is first polled,
     * so the step's own checks report what went wrong. Wall-clock, not ticks: the game-test server
     * ticks as fast as it can, while hashing and Turso calls take real time.
     */
    private static java.util.function.BooleanSupplier giveUpAfter(int seconds, java.util.function.BooleanSupplier cond) {
        long[] deadline = {0};
        return () -> {
            if (deadline[0] == 0) deadline[0] = System.currentTimeMillis() + seconds * 1000L;
            return System.currentTimeMillis() >= deadline[0] || cond.getAsBoolean();
        };
    }

    private static final java.util.Map<String, Integer> pollCount = new java.util.HashMap<>();

    /** True once student_accounts.last_login is set for {@code user} (checked every 20 polls). */
    private static boolean lastLoginSet(String user) {
        if (pollCount.merge(user, 1, Integer::sum) % 20 != 0) return false;
        return str(query("SELECT last_login FROM student_accounts WHERE username = ?", user).get(0).getAsJsonObject(), "last_login") != null;
    }

    private static String id(Block b) { return b.getDescriptionId().replace("block.berongsmp.", ""); }
    private static String id(Item i) { return i.getDescriptionId().replace("item.berongsmp.", ""); }

    static void registerAll() {
        accountTests();
        certificationTests();
        playerSafetyTests();
        newSimTests();
        extinguisherTests();
        legacyAndQuakeTests();
        telemetryTests();
        uploadTests();
        adminAndStateTests();
    }

    // ───────────────────────────── GS-01 / GS-02 accounts ─────────────────────────────
    private static void accountTests() {
        add("GS-01-03", 400000, h -> {   // also GS-01-04 (duplicate of this account)
            RecordingPlayer p = join(h, "gs0103");
            String u = uniqueName("gt");
            register(p, u);
            boolean[] ok = new boolean[2];
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            flow(h).when(1, () -> AuthManager.current(p.getUUID()) != null || p.messages.size() > 1, () -> {
                ok[0] = caseCheck("GS-01-03", () -> {
                    h.assertTrue(AuthManager.current(p.getUUID()) != null, "not logged in after /register; chat: " + p.messages);
                    JsonArray rows = query("SELECT username, student_id, section FROM student_accounts WHERE username = ?", u);
                    h.assertTrue(rows.size() == 1, "student_accounts rows for " + u + ": " + rows);
                    JsonObject r = rows.get(0).getAsJsonObject();
                    h.assertTrue(("TEST-" + u).equals(str(r, "student_id")) && "TEST3A".equals(str(r, "section")), "row values: " + r);
                    actual("GS-01-03", "/register " + u + " created a student_accounts row (student_id TEST-" + u + ", section TEST3A); player logged in.");
                });
                p2.set(RecordingPlayer.join(h, "gs0104"));
                register(p2.get(), u);
            }).when(1, giveUpAfter(30, () -> p2.get().messages.stream().anyMatch(m -> !m.contains("joined"))), () -> {
                ok[1] = caseCheck("GS-01-04", () -> {
                    String n = str(query("SELECT COUNT(*) AS n FROM student_accounts WHERE username = ?", u).get(0).getAsJsonObject(), "n");
                    h.assertTrue("1".equals(n), "rows after duplicate register: " + n);
                    h.assertTrue(AuthManager.current(p2.get().getUUID()) == null, "duplicate registration logged the player in");
                    String msg = p2.get().messages.stream()
                            .filter(m -> m.toLowerCase().contains("taken") || m.toLowerCase().contains("exists")).findFirst().orElse(null);
                    h.assertTrue(msg != null, "no rejection message; chat: " + p2.get().messages);
                    actual("GS-01-04", "Rejected with \"" + msg.trim() + "\"; still 1 row for " + u + ".");
                });
                finish(h, ok);
            }).start();
        });
        add("GS-01-05", 400, h -> {
            RecordingPlayer p = join(h, "gs0105");
            String u = uniqueName("gs");
            cmd(p, "register " + u + " abc TEST-X TEST3A Short Password", false);
            flow(h).when(1, () -> p.messages.stream().anyMatch(m -> m.toLowerCase().contains("password")), () -> {
                String n = str(query("SELECT COUNT(*) AS n FROM student_accounts WHERE username = ?", u).get(0).getAsJsonObject(), "n");
                h.assertTrue("0".equals(n), "row created for a 3-character password");
                h.assertTrue(AuthManager.current(p.getUUID()) == null, "player logged in with a 3-character password");
                String why = p.messages.stream().filter(m -> m.toLowerCase().contains("password")).findFirst().orElse("no message");
                actual("GS-01-05", "3-character password rejected (\"" + why.trim() + "\"); no row created.");
                h.succeed();
            }).start();
        });
        add("GS-02-03", 400000, h -> {   // also GS-02-04 (wrong password for the same account)
            RecordingPlayer p1 = join(h, "gs0203a");
            String u = uniqueName("lg");
            register(p1, u);
            boolean[] ok = new boolean[2];
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>(), p3 = new AtomicReference<>();
            flow(h).when(1, () -> AuthManager.current(p1.getUUID()) != null, () -> {
                cmd(p1, "logout", false);
                p2.set(RecordingPlayer.join(h, "gs0203b"));
                cmd(p2.get(), "login " + u + " " + PW, false);
                p3.set(RecordingPlayer.join(h, "gs0204"));
                cmd(p3.get(), "login " + u + " WrongPass#1", false);
            }).when(1, giveUpAfter(30, () -> AuthManager.current(p2.get().getUUID()) != null
                    && p3.get().messages.stream().anyMatch(m -> m.toLowerCase().contains("invalid") || m.toLowerCase().contains("incorrect"))
                    && lastLoginSet(u)), () -> {
                ok[0] = caseCheck("GS-02-03", () -> {
                    String last = str(query("SELECT last_login FROM student_accounts WHERE username = ?", u).get(0).getAsJsonObject(), "last_login");
                    h.assertTrue(last != null, "last_login not updated");
                    actual("GS-02-03", "/login " + u + " (password with a symbol, unquoted) authenticated a second station; last_login = " + last + ".");
                });
                ok[1] = caseCheck("GS-02-04", () -> {
                    h.assertTrue(AuthManager.current(p3.get().getUUID()) == null, "wrong password logged in");
                    String why = p3.get().messages.stream()
                            .filter(m -> m.toLowerCase().contains("invalid") || m.toLowerCase().contains("incorrect")).findFirst().orElse("");
                    actual("GS-02-04", "Wrong password refused (\"" + why.trim() + "\"); not authenticated.");
                });
                finish(h, ok);
            }).start();
        });
        add("GS-02-05", 100, h -> {
            RecordingPlayer p = join(h, "gs0205");
            pressLobbySimButton(p);
            h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "unregistered player started a simulation");
            h.assertTrue(p.heard("/register"), "no /register prompt; chat: " + p.messages);
            actual("GS-02-05", "Lobby simulation button refused for an unregistered player with the /register prompt; no session started.");
            h.succeed();
        });
        add("GS-02-06", 400000, h -> {
            RecordingPlayer p = join(h, "gs0206");
            String u = uniqueName("lo");
            register(p, u);
            flow(h).when(1, () -> AuthManager.current(p.getUUID()) != null, () -> cmd(p, "logout", false))
                   .after(5, () -> {
                int before = p.messages.size();
                pressLobbySimButton(p);
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "logged-out player started a simulation");
                h.assertTrue(p.messages.size() > before, "no message after pressing the button");
                String why = p.messages.get(before);
                actual("GS-02-06", "After /logout the simulation button was refused: \"" + why.trim().replace('\n', ' ') + "\"");
                h.succeed();
            }).start();
        });
    }

    // ───────────────────────────── GS-06 Academy certification persistence ─────────────────────────────
    private static void certificationTests() {
        add("GS-06-03", 400000, h -> {   // also GS-06-04 (certification restored on /login at another station)
            RecordingPlayer p = join(h, "gs0603");
            String u = uniqueName("ac");
            register(p, u);
            boolean[] ok = new boolean[2];
            AtomicReference<RecordingPlayer> p2 = new AtomicReference<>();
            Function<String, String> certified = user ->
                    str(query("SELECT tutorial_completed FROM student_accounts WHERE username = ?", user).get(0).getAsJsonObject(), "tutorial_completed");
            flow(h).when(1, () -> AuthManager.current(p.getUUID()) != null, () -> {
                // The same call MorfeRoomManager.runEvaluation makes on a PASS (score >= threshold, GS-06-02)
                AuthManager.markTutorialCompleted(p.getUUID());
            }).when(1, giveUpAfter(30, every(20, () -> "1".equals(certified.apply(u)))), () -> {
                ok[0] = caseCheck("GS-06-03", () -> {
                    String v = certified.apply(u);
                    h.assertTrue("1".equals(v), "student_accounts.tutorial_completed = " + v);
                    actual("GS-06-03", "Certifying account " + u + " set student_accounts.tutorial_completed = 1 in the test database.");
                });
                cmd(p, "logout", false);
                p2.set(RecordingPlayer.join(h, "gs0604"));
                cmd(p2.get(), "login " + u + " " + PW, false);
            }).when(1, giveUpAfter(30, () -> AuthManager.current(p2.get().getUUID()) != null
                    && SessionManager.getActiveSession(p2.get().getUUID()) != null), () -> {
                ok[1] = caseCheck("GS-06-04", () -> {
                    h.assertTrue(AuthManager.isTutorialCompleted(p2.get().getUUID()), "certification not restored on /login");
                    pressLobbySimButton(p2.get());
                    SimulationSession s = SimulationManager.getSession(p2.get().getUUID());
                    h.assertTrue(s != null && s.getState() == SimulationState.NEW_SIM_BUILDING2_FIRE,
                            "simulation button did not start New Sim Building 2.0; chat: " + p2.get().messages);
                    actual("GS-06-04", "After /login at a new station the account was still certified (\""
                            + p2.get().messages.stream().filter(m -> m.contains("certified")).findFirst().orElse("")
                                    .replaceAll("(?s).*\\n", "").trim()
                            + "\") and the simulation button started New Sim Building 2.0 without the Academy.");
                });
                reset(h);
                finish(h, ok);
            }).start();
        });
    }

    // ───────────────────────────── GS-05 player safety ─────────────────────────────
    private static void playerSafetyTests() {
        add("GS-05-03", 100, h -> {
            RecordingPlayer p = join(h, "gs0503");
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            p.igniteForTicks(100);
            seq(h).at(2, () -> DropAndRollManager.onDropAndRollRequest(p))
                  .at(3, () -> DropAndRollManager.onDropAndRollRequest(p))
                  .at(3, () -> DropAndRollManager.onDropAndRollRequest(p))
                  .at(3, () -> DropAndRollManager.onDropAndRollRequest(p))
                  .at(2, () -> {
                      h.assertTrue(DropAndRollManager.isDropped(p.getUUID()), "drop-and-roll state not recorded");
                      h.assertTrue(p.getRemainingFireTicks() <= 0, "still burning: " + p.getRemainingFireTicks() + " ticks");
                      actual("GS-05-03", "4 drop-and-roll presses: player marked dropped; fire ticks went from 100 to " + p.getRemainingFireTicks() + ".");
                  }).end();
        });
        add("GS-05-04", 200, h -> {
            RecordingPlayer p = join(h, "gs0504");
            BlockPos base = platform(h);
            h.getLevel().setBlock(base.above(2), Blocks.STONE.defaultBlockState(), 3); // cover overhead
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            h.onEachTick(() -> crouch(p, true));
            seq(h).at(110, () -> {
                h.assertTrue(DuckCoverHoldManager.isCompliant(p.getUUID()), "not marked compliant");
                int held = DuckCoverHoldManager.ticksHeld(p.getUUID());
                h.assertTrue(held >= 100, "held only " + held + " ticks");
                actual("GS-05-04", "Crouched under cover: DuckCoverHoldManager marked the player compliant (" + held + " ticks held).");
            }).end();
        });
    }

    // ───────────────────────────── GS-07..GS-09 New Sim Building 2.0 ─────────────────────────────
    private static void newSimTests() {
        add("GS-07-03", 200, h -> {
            RecordingPlayer p = join(h, "gs0703");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            List<BlockPos> armed = s.getArmedHazards();
            h.assertTrue(!armed.isEmpty(), "no hazards armed");
            BlockPos target = armed.get(0);
            String hz = id(h.getLevel().getBlockState(target).getBlock());
            rightClickBlock(p, target);
            h.assertTrue(!HazardManager.isHazardous(h.getLevel().getBlockState(target)), "hazard still hazardous after bare-hand prevention");
            h.assertTrue(s.getPreventedHazards().contains(target), "not counted as prevented");
            h.assertTrue(!events(s, "hazard_neutralize").isEmpty(), "no hazard_neutralize event");
            actual("GS-07-03", "Right-clicked armed hazard " + hz + ": disarmed, hazard_neutralize (method=prevention) logged, "
                    + s.getPreventedHazards().size() + "/" + armed.size() + " found.");
            reset(h);
            h.succeed();
        });
        add("GS-07-04", 300, h -> {
            RecordingPlayer p = join(h, "gs0704");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            for (BlockPos pos : new ArrayList<>(s.getArmedHazards())) rightClickBlock(p, pos);
            seq(h).at(WARMUP + 10, () -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "session still running after all hazards prevented");
                h.assertTrue("all_hazards_prevented".equals(endReason(s)), "end_reason " + endReason(s));
                Object score = last(s, "SIM_END").map(e -> e.data().get("score")).orElse(null);
                actual("GS-07-04", "All " + s.getArmedHazards().size() + " hazards prevented: session ended with all_hazards_prevented, score " + score + " (no alarm rung).");
            }).end();
        });
        add("GS-08-03", 400, h -> {
            RecordingPlayer p = join(h, "gs0803");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            seq(h).at(WARMUP + 205, () -> {
                h.assertTrue(s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, "phase " + s.getFirePhase());
                int failures = events(s, "hazard_failure").size();
                boolean phase = events(s, "phase_transition").stream().anyMatch(e -> "intervention".equals(e.data().get("phase")));
                h.assertTrue(failures > 0 && failures >= s.getEscalatedHazards().size(), "hazard_failure events: " + failures);
                h.assertTrue(phase, "no phase_transition to intervention");
                actual("GS-08-03", "Prevention timer ran out: " + s.getEscalatedHazards().size() + " hazards escalated, " + failures
                        + " hazard_failure events and phase_transition(intervention) logged.");
                reset(h);
            }).end();
        });
        add("GS-08-04", 500, h -> {
            RecordingPlayer p = join(h, "gs0804");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            AtomicReference<BlockPos> target = new AtomicReference<>();
            AtomicReference<String> used = new AtomicReference<>();
            seq(h).at(WARMUP + 205, () -> {
                h.assertTrue(s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, "phase " + s.getFirePhase());
                BlockPos t = s.getEscalatedHazards().iterator().next();
                target.set(t);
                Block b = h.getLevel().getBlockState(t).getBlock();
                used.set(id(b) + " with " + id(correctExtinguisher(b)));
                BlockPos stand = standNear(h.getLevel(), t);
                tp(p, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(t));
                arm(p, correctExtinguisher(b));
                spray(p, 12);
            }).at(2, () -> {
                h.assertTrue(s.getExtinguishedEscalated().contains(target.get()),
                        "escalated hazard not counted as extinguished; state " + h.getLevel().getBlockState(target.get()));
                actual("GS-08-04", "Sprayed escalated " + used.get() + ": counted as contained ("
                        + s.getExtinguishedEscalated().size() + "/" + s.getEscalatedHazards().size() + ").");
                reset(h);
            }).end();
        });
        add("GS-09-04", 700, h -> {
            RecordingPlayer p = join(h, "gs0904");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            seq(h).at(WARMUP + 410, () -> {
                h.assertTrue(s.getFirePhase() == SimulationSession.FirePhase.EVACUATION, "phase " + s.getFirePhase());
                tp(p, -155.5, -33, 480.5);
            }).at(5, () -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "session still running in the assembly zone");
                h.assertTrue("assembly_reached".equals(endReason(s)), "end_reason " + endReason(s));
                h.assertTrue(!events(s, "assembly_area_reached").isEmpty(), "no assembly_area_reached event");
                actual("GS-09-04", "Entered the assembly zone during EVACUATION: assembly_area_reached logged, session ended with assembly_reached.");
            }).end();
        });
        add("GS-09-05", 1200, h -> {
            RecordingPlayer p = join(h, "gs0905");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            h.succeedWhen(() -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "still running");
                h.assertTrue("timeout".equals(endReason(s)), "end_reason " + endReason(s));
                actual("GS-09-05", "Run left to expire: session ended with end_reason timeout after " + s.elapsedSeconds() + " s.");
            });
        });
    }

    // ───────────────────────────── GS-11..GS-13 extinguishers and alarm ─────────────────────────────
    /** Starts a Library FIRE run, then brings the player to the test platform with a fire 3 blocks east. */
    private static SimulationSession fireBench(GameTestHelper h, RecordingPlayer p, BlockPos[] fireOut) {
        SimulationSession s = start(p, SimulationState.FIRE);
        BlockPos base = platform(h);
        BlockPos fire = base.east(3);
        h.getLevel().setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);
        faceFrom(p, fire, 2.5);
        fireOut[0] = fire;
        return s;
    }

    private static void extinguisherTests() {
        add("GS-11-01", 200, h -> {
            RecordingPlayer p = join(h, "gs1101");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            spray(p, 5);
            List<String> order = s.logger.getEvents().stream().map(SimEvent::type)
                    .filter(t -> t.equals("pin_pull") || t.equals("EXT_PIN_PULL") || t.equals("ext_spray") || t.equals("EXT_SPRAY")).toList();
            h.assertTrue(order.contains("pin_pull") && order.contains("EXT_PIN_PULL"), "pin events missing: " + order);
            h.assertTrue(order.indexOf("ext_spray") > order.indexOf("pin_pull") && order.indexOf("EXT_SPRAY") > order.indexOf("EXT_PIN_PULL"), "order: " + order);
            h.assertTrue(h.getLevel().getBlockState(f[0]).isAir(), "fire not put out");
            actual("GS-11-01", "Event order: " + String.join(" → ", order) + "; fire put out.");
            reset(h);
            h.succeed();
        });
        add("GS-11-02", 200, h -> {
            RecordingPlayer p = join(h, "gs1102");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            arm(p, ModItems.FIRE_EXTINGUISHER.get()); // the single first right-click
            h.assertTrue(p.heard("PIN PULLED"), "no pin message");
            h.assertTrue(!events(s, "pin_pull").isEmpty(), "pin_pull not logged");
            h.assertTrue(events(s, "ext_spray").isEmpty() && events(s, "EXT_SPRAY").isEmpty(), "a spray was logged on the first click");
            h.assertTrue(h.getLevel().getBlockState(f[0]).is(Blocks.FIRE), "fire went out without spraying");
            actual("GS-11-02", "First right-click: \"PIN PULLED\" message and pin_pull logged; no spray event, fire still burning.");
            reset(h);
            h.succeed();
        });
        add("GS-11-03", 200, h -> {
            RecordingPlayer p = join(h, "gs1103");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            spray(p, 1);       // charge 0 → logged sample, aimed at the fire
            p.setXRot(-80f);   // look up at the open sky
            spray(p, 20);      // charge 1..20 → the 20th is the next logged sample
            List<SimEvent> sprays = events(s, "EXT_SPRAY");
            h.assertTrue(sprays.size() >= 2, "EXT_SPRAY samples: " + sprays.size());
            Object first = sprays.get(0).data().get("hit_fire"), second = sprays.get(1).data().get("hit_fire");
            h.assertTrue(Boolean.TRUE.equals(first) && Boolean.FALSE.equals(second), "hit_fire values " + first + ", " + second);
            actual("GS-11-03", "EXT_SPRAY aimed at the fire: hit_fire=true; aimed at open air: hit_fire=false.");
            reset(h);
            h.succeed();
        });
        add("GS-11-04", 200, h -> {
            RecordingPlayer p = join(h, "gs1104");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            spray(p, 5);
            h.assertTrue(h.getLevel().getBlockState(f[0]).isAir(), "fire not put out");
            String log = s.buildFireLogCsv();
            String coords = f[0].getX() + "," + f[0].getY() + "," + f[0].getZ();
            boolean found = log.lines().anyMatch(l -> l.contains(coords) && l.endsWith("extinguish"));
            h.assertTrue(found, "fire put out at " + coords + " but the fire log has no extinguish row for it (fire log: "
                    + log.lines().count() + " lines)");
            actual("GS-11-04", "Fire log has an extinguish row at " + coords + ".");
            reset(h);
            h.succeed();
        });
        // GS-12: hazard props on the test platform; the extinguisher-class rules need no session
        add("GS-12-01", 100, h -> classCase(h, "GS-12-01", ModBlocks.OVERLOADED_WALL_OUTLET.get(), ModItems.CO2_EXTINGUISHER.get(), true));
        add("GS-12-02", 100, h -> classCase(h, "GS-12-02", ModBlocks.UNATTENDED_GREASE_PAN.get(), ModItems.WET_CHEMICAL_EXTINGUISHER.get(), true));
        add("GS-12-03", 100, h -> classCase(h, "GS-12-03", ModBlocks.UNATTENDED_GREASE_PAN.get(), ModItems.FIRE_EXTINGUISHER.get(), false));
        add("GS-13-01", 200, h -> {   // also GS-13-02 (second press)
            RecordingPlayer p = join(h, "gs1301");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            BlockPos alarm = f[0].north(2);
            h.getLevel().setBlock(alarm, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            rightClickBlock(p, alarm);
            boolean a = caseCheck("GS-13-01", () -> {
                h.assertTrue(events(s, "fire_alarm_activate").size() == 1, "fire_alarm_activate events: " + events(s, "fire_alarm_activate").size());
                h.assertTrue(s.wasAlarmRung(), "session not marked alarm rung");
                actual("GS-13-01", "Pressed the fire alarm: fire_alarm_activate logged and the session marked alarm rung.");
            });
            rightClickBlock(p, alarm);
            boolean b = caseCheck("GS-13-02", () -> {
                h.assertTrue(events(s, "fire_alarm_activate").size() == 1, "fire_alarm_activate events: " + events(s, "fire_alarm_activate").size());
                h.assertTrue(p.heard("Already activated"), "no 'already activated' message");
                actual("GS-13-02", "Second press answered \"Already activated!\"; still one fire_alarm_activate event, so the bonus counts once.");
            });
            reset(h);
            finish(h, a, b);
        });
    }

    private static void classCase(GameTestHelper h, String caseId, Block hazard, Item extinguisher, boolean shouldDefuse) {
        RecordingPlayer p = join(h, caseId.replace("-", "").toLowerCase());
        BlockPos base = platform(h);
        BlockPos pos = base.east(3);
        BlockState burning = hazard.defaultBlockState().setValue(HazardBlock.HAZARDOUS, true).setValue(HazardBlock.ON_FIRE, true);
        h.getLevel().setBlock(pos, burning, 3);
        faceFrom(p, pos, 2.5);
        arm(p, extinguisher);
        spray(p, 10);
        boolean stillBurning = HazardManager.isHazardous(h.getLevel().getBlockState(pos));
        if (shouldDefuse) {
            h.assertTrue(!stillBurning, id(extinguisher) + " did not put out " + id(hazard));
            actual(caseId, id(extinguisher) + " put out the burning " + id(hazard) + ".");
        } else {
            h.assertTrue(stillBurning, id(extinguisher) + " wrongly put out " + id(hazard));
            String warn = p.messages.stream().filter(m -> m.contains("✗")).findFirst().orElse(null);
            h.assertTrue(warn != null, "no wrong-extinguisher warning; chat: " + p.messages);
            actual(caseId, id(extinguisher) + " did not put out " + id(hazard) + "; warning: \"" + warn.trim() + "\"");
        }
        reset(h);
        h.succeed();
    }

    // ───────────────────────────── GS-14, GS-16, GS-17 legacy fire and earthquake ─────────────────────────────
    private static void legacyAndQuakeTests() {
        add("GS-14-05", 100, h -> {
            RecordingPlayer p = join(h, "gs1405");
            cmd(p, "sim_fire library", true);
            SimulationSession s = SimulationManager.getSession(p.getUUID());
            h.assertTrue(s != null && s.getState() == SimulationState.FIRE, "no FIRE session: " + p.messages);
            Object type = last(s, "SIM_START").map(e -> e.data().get("sim_type")).orElse(null);
            h.assertTrue("FIRE".equals(type), "SIM_START sim_type " + type);
            actual("GS-14-05", "/sim_fire library started a FIRE session in the Library; SIM_START logged with sim_type FIRE.");
            reset(h);
            h.succeed();
        });
        add("GS-16-02", 900, h -> {
            RecordingPlayer p = join(h, "gs1602");
            start(p, SimulationState.EARTHQUAKE);
            List<String> seen = new ArrayList<>();
            h.onEachTick(() -> {
                SimulationSession cur = SimulationManager.getSession(p.getUUID());
                if (cur != null && cur.getQuakePhase() != null) {
                    String ph = cur.getQuakePhase().name();
                    if (seen.isEmpty() || !seen.get(seen.size() - 1).equals(ph)) seen.add(ph);
                }
            });
            h.succeedWhen(() -> {
                h.assertTrue(seen.contains("END"), "phases so far " + seen);
                List<String> distinct = seen.stream().distinct().toList();
                h.assertTrue(distinct.equals(List.of("RUMBLE", "PEAK", "AFTERSHOCK", "END")), "order " + seen);
                h.assertTrue(p.heard("intensifying") && p.heard("Aftershock") && p.heard("shaking has stopped"), "messages " + p.messages);
                actual("GS-16-02", "Phases observed in order: " + String.join(" → ", seen)
                        + "; player told \"intensifying\", \"Aftershock!\" and \"The shaking has stopped\".");
                reset(h);
            });
        });
        add("GS-16-03", 100, h -> {
            RecordingPlayer p = join(h, "gs1603");
            SimulationSession s = start(p, SimulationState.EARTHQUAKE);
            double before = s.getSessionMagnitude();
            cmd(p, "sim_magnitude 8.5", true);
            h.assertTrue(Math.abs(s.getSessionMagnitude() - 8.5) < 1e-9, "magnitude " + s.getSessionMagnitude());
            actual("GS-16-03", "/sim_magnitude 8.5 changed the running earthquake from " + before + " to " + s.getSessionMagnitude() + ".");
            reset(h);
            h.succeed();
        });
        add("GS-16-04", 200, h -> {
            RecordingPlayer p = join(h, "gs1604");
            SimulationSession s = start(p, SimulationState.EARTHQUAKE);
            seq(h).at(WARMUP + 20, () -> {
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                SimEvent end = last(s, "SIM_END").orElseThrow();
                h.assertTrue(Integer.valueOf(0).equals(end.data().get("score")) && Boolean.FALSE.equals(end.data().get("passed")), "SIM_END " + end.data());
                actual("GS-16-04", "Earthquake run ended with score 0 and passed=false until an instructor assigns a score (SIM_END " + end.data() + ").");
            }).end();
        });
        add("GS-17-01", 600, h -> dchCase(h, "GS-17-01", 120, true));
        add("GS-17-02", 600, h -> dchCase(h, "GS-17-02", 0, false));
        add("GS-17-03", 600, h -> dchCase(h, "GS-17-03", 40, false));
    }

    /** Earthquake run; crouches under cover for {@code holdTicks}, starting early in PEAK. */
    private static void dchCase(GameTestHelper h, String caseId, int holdTicks, boolean expectEvent) {
        RecordingPlayer p = join(h, caseId.replace("-", "").toLowerCase());
        SimulationSession s = start(p, SimulationState.EARTHQUAKE);
        BlockPos base = platform(h);
        if (holdTicks > 0) h.getLevel().setBlock(base.above(2), Blocks.STONE.defaultBlockState(), 3);
        tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        int startHold = WARMUP + 45; // RUMBLE lasts 40 ticks, so this is inside PEAK
        AtomicReference<String> phaseAtHold = new AtomicReference<>();
        h.onEachTick(() -> {
            long t = h.getTick();
            boolean on = holdTicks > 0 && t >= startHold && t < startHold + holdTicks;
            if (on && phaseAtHold.get() == null && s.getQuakePhase() != null) phaseAtHold.set(s.getQuakePhase().name());
            crouch(p, on);
        });
        seq(h).at(startHold + 150, () -> {
            int n = events(s, "duck_cover_hold").size();
            if (expectEvent) {
                h.assertTrue(n == 1, "duck_cover_hold events: " + n);
                h.assertTrue("PEAK".equals(phaseAtHold.get()), "hold began in phase " + phaseAtHold.get());
                actual(caseId, "Crouched under cover during PEAK for 6 s: duck_cover_hold logged and \"Duck, Cover, and Hold maintained\" shown.");
            } else {
                h.assertTrue(n == 0, "duck_cover_hold events: " + n);
                actual(caseId, holdTicks == 0 ? "Stood in the open through PEAK: no duck_cover_hold event."
                        : "Took cover for 2 s then stood: no duck_cover_hold event (hold under 5 s).");
            }
            reset(h);
        }).end();
    }

    // ───────────────────────────── GS-18, GS-19, GS-22 telemetry and zones ─────────────────────────────
    private static void telemetryTests() {
        add("GS-18-01", 400, h -> {   // also GS-18-02, GS-18-03, GS-19-02 (same run's CSV)
            RecordingPlayer p = join(h, "gs1801");
            SimulationSession s = start(p, SimulationState.FIRE);
            BlockPos base = platform(h);
            AtomicLong startRows = new AtomicLong(-1);
            h.onEachTick(() -> {
                if (startRows.get() >= 0) tp(p, base.getX() + 0.5 + (h.getTick() % 40) * 0.05, base.getY(), base.getZ() + 0.5);
            });
            seq(h).at(WARMUP, () -> startRows.set(s.buildMoveCsv().lines().filter(l -> l.contains(",move_tick,")).count()))
                  .at(200, () -> {
                String csv = s.buildMoveCsv();
                List<String[]> rows = csv.lines().skip(1).map(l -> l.split(",", -1)).toList();
                boolean a = caseCheck("GS-18-01", () -> {
                    long n = csv.lines().filter(l -> l.contains(",move_tick,")).count() - startRows.get();
                    h.assertTrue(n >= 190 && n <= 210, "move_tick rows in 200 ticks: " + n);
                    actual("GS-18-01", n + " move_tick rows over 10 s (200 server ticks) = " + (n / 10.0) + " Hz.");
                });
                boolean b = caseCheck("GS-18-02", () -> {
                    String header = csv.lines().findFirst().orElse("");
                    String expected = "player_id,session_id,scenario_type,timestamp,event_type,x,y,z,hazard_distance,interaction_target,nearby_player_count";
                    h.assertTrue(header.startsWith(expected), "header: " + header);
                    actual("GS-18-02", "Header: " + header);
                });
                boolean c = caseCheck("GS-18-03", () -> {
                    double prev = -1;
                    int bad = 0;
                    for (String[] r : rows) { double t = Double.parseDouble(r[3]); if (t < prev) bad++; prev = t; }
                    h.assertTrue(bad == 0, bad + " rows out of time order");
                    actual("GS-18-03", rows.size() + " rows; timestamps never decrease (0.00 to " + rows.get(rows.size() - 1)[3] + " s).");
                });
                boolean d = caseCheck("GS-19-02", () -> {
                    long ids = rows.stream().map(r -> r[1]).distinct().count();
                    h.assertTrue(ids == 1, ids + " distinct session ids");
                    actual("GS-19-02", "All " + rows.size() + " CSV rows of the run carry session_id " + rows.get(0)[1] + ".");
                });
                reset(h);
                finish(h, a, b, c, d);
            });
        });
        add("GS-18-04", 300, h -> {
            RecordingPlayer p = join(h, "gs1804");
            SimulationSession s = start(p, SimulationState.FIRE);
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            seq(h).at(WARMUP + 20, () -> h.getLevel().setBlock(base.east(3), Blocks.FIRE.defaultBlockState(), 3))
                  .at(20, () -> {
                String lastRow = s.buildMoveCsv().lines().filter(l -> l.contains(",move_tick,")).reduce((x, y) -> y).orElse("");
                double d = Double.parseDouble(lastRow.split(",", -1)[8]);
                h.assertTrue(d <= 4.0, "hazard_distance " + d + " with a fire 3 blocks away");
                actual("GS-18-04", "With a fire 3 blocks away, move_tick hazard_distance = " + d + ".");
                reset(h);
            }).end();
        });
        add("GS-19-03", 300, h -> {   // also GS-19-04, GS-19-05 (same run's event log)
            RecordingPlayer p = join(h, "gs1903");
            BlockPos[] f = new BlockPos[1];
            SimulationSession s = fireBench(h, p, f);
            seq(h).at(WARMUP + 40, () -> {
                arm(p, ModItems.FIRE_EXTINGUISHER.get());
                spray(p, 22);
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                List<SimEvent> all = s.logger.getEvents();
                boolean a = caseCheck("GS-19-03", () -> {
                    int bad = 0;
                    for (int i = 1; i < all.size(); i++) if (all.get(i).tOffsetMs() < all.get(i - 1).tOffsetMs()) bad++;
                    h.assertTrue(bad == 0, bad + " events out of order");
                    actual("GS-19-03", all.size() + " events in non-decreasing time order.");
                });
                boolean b = caseCheck("GS-19-04", () -> {
                    long legacy = all.stream().filter(e -> e.type().equals("EXT_SPRAY")).count();
                    long v12 = all.stream().filter(e -> e.type().equals("ext_spray")).count();
                    h.assertTrue(legacy > 0 && v12 > 0, "EXT_SPRAY " + legacy + ", ext_spray " + v12);
                    actual("GS-19-04", "ABC spray logged " + legacy + " EXT_SPRAY and " + v12 + " ext_spray events.");
                });
                boolean c = caseCheck("GS-19-05", () -> {
                    SimEvent first = all.get(0), lastEv = all.get(all.size() - 1);
                    h.assertTrue(first.type().equals("SIM_START") && lastEv.type().equals("SIM_END") && lastEv.data().containsKey("end_reason"),
                            "first " + first.type() + ", last " + lastEv.type());
                    actual("GS-19-05", "First event SIM_START; last event SIM_END with end_reason=" + lastEv.data().get("end_reason") + ".");
                });
                finish(h, a, b, c);
            });
        });
        add("GS-22-01", 20, h -> {
            h.assertTrue(AssemblyZone.isInside(new Vec3(50, -33, 70), false), "centre of Library zone");
            h.assertTrue(AssemblyZone.isInside(new Vec3(30, -35, 64), false), "minimum corner");
            h.assertTrue(!AssemblyZone.isInside(new Vec3(29.9, -33, 70), false), "just west");
            h.assertTrue(!AssemblyZone.isInside(new Vec3(50, -33, 83), false), "just south");
            h.assertTrue(AssemblyZone.isInsideNewSim2(new Vec3(-156, -33, 480)), "New Sim zone");
            h.assertTrue(!AssemblyZone.isInsideNewSim2(new Vec3(-170, -33, 380)), "inside New Sim building");
            actual("GS-22-01", "Library zone: centre and minimum corner inside, points 0.1 west / 1 south outside; "
                    + "New Sim zone (-156,-33,480) inside, building interior outside.");
            h.succeed();
        });
        add("GS-22-02", 20, h -> {
            ExitZones.ExitZone hit = ExitZones.find(new Vec3(52, -32, 94.5), false);
            h.assertTrue(hit != null && hit.label().equals("main_exit"), "exit at doorway: " + hit);
            h.assertTrue(ExitZones.find(new Vec3(52, -32, 100), false) == null, "point inside the hall matched an exit");
            actual("GS-22-02", "(52,-32,94.5) matched main_exit; (52,-32,100) matched no exit.");
            h.succeed();
        });
        add("GS-22-03", 200, h -> {   // also GS-22-04 (entering the assembly zone in the same run)
            RecordingPlayer p = join(h, "gs2203");
            SimulationSession s = start(p, SimulationState.FIRE);
            boolean[] ok = new boolean[2];
            seq(h).at(WARMUP + 5, () -> tp(p, 52.5, -32, 94.5))
                  .at(5, () -> ok[0] = caseCheck("GS-22-03", () -> {
                      h.assertTrue(events(s, "emergency_exit").size() == 1, "emergency_exit events " + events(s, "emergency_exit").size());
                      h.assertTrue(SimulationManager.getSession(p.getUUID()) == s, "run ended at the exit");
                      actual("GS-22-03", "Crossed main_exit: emergency_exit logged once and the run kept going.");
                  }))
                  .at(1, () -> tp(p, 50.5, -33, 70.5))
                  .at(5, () -> {
                      ok[1] = caseCheck("GS-22-04", () -> {
                          h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "run did not end in the assembly zone");
                          h.assertTrue("assembly_reached".equals(endReason(s)), "end_reason " + endReason(s));
                          actual("GS-22-04", "Entered the Library assembly zone: assembly_area_reached logged and the run ended with assembly_reached.");
                      });
                      finish(h, ok);
                  });
        });
    }

    // ───────────────────────────── GS-20 upload to the test database ─────────────────────────────
    private static void uploadTests() {
        add("GS-20-01", 400000, h -> {   // also GS-20-02..04 (same uploaded row)
            RecordingPlayer p = join(h, "gs2001");
            String u = uniqueName("up");
            register(p, u);
            AtomicReference<SimulationSession> sess = new AtomicReference<>();
            AtomicReference<JsonObject> row = new AtomicReference<>();
            flow(h).when(1, () -> SessionManager.getActiveSession(p.getUUID()) != null
                            && SessionManager.findActiveSessionRowId(p.getUUID()) > 0, () -> {
                cmd(p, "sim_fire library", true);
                sess.set(SimulationManager.getSession(p.getUUID()));
                h.assertTrue(sess.get() != null, "simulation did not start");
                p.setInvulnerable(true);
            }).after(WARMUP + 60, () -> tp(p, 50.5, -33, 70.5))   // evacuate to the Library assembly zone
              .when(1, () -> SimulationManager.getSession(p.getUUID()) == null, () -> { })
              .when(1, giveUpAfter(120, every(20, () -> {
                  JsonArray rows = query("SELECT * FROM sessions WHERE username = ? ORDER BY id DESC", u);
                  if (rows.isEmpty()) return false;
                  JsonObject r = rows.get(0).getAsJsonObject();
                  if (!"completed".equals(str(r, "status")) || str(r, "move_log_csv") == null) return false;
                  row.set(r);
                  return true;
              })), () -> {
                JsonObject r = row.get();
                h.assertTrue(r != null, "the uploaded row never appeared as completed within 120 s");
                int logged = sess.get().logger.getEvents().size();
                boolean a = caseCheck("GS-20-01", () -> actual("GS-20-01", "sessions row " + str(r, "id") + " for " + u
                        + ": status completed, simulation_type " + str(r, "simulation_type") + ", score " + str(r, "simulation_score") + "."));
                boolean b = caseCheck("GS-20-02", () -> {
                    h.assertTrue(u.equals(str(r, "username")) && ("TEST-" + u).equals(str(r, "student_id")) && "TEST3A".equals(str(r, "section")),
                            "username/student_id/section: " + str(r, "username") + "/" + str(r, "student_id") + "/" + str(r, "section"));
                    actual("GS-20-02", "Row carries username " + u + ", student_id TEST-" + u + ", section TEST3A.");
                });
                boolean c = caseCheck("GS-20-03", () -> {
                    int stored = JsonParser.parseString(str(r, "event_log")).getAsJsonArray().size();
                    h.assertTrue(stored == logged, "event_log has " + stored + " events, game logged " + logged);
                    actual("GS-20-03", "event_log parses as a JSON array of " + stored + " events = " + logged + " logged in game.");
                });
                boolean d = caseCheck("GS-20-04", () -> {
                    String move = str(r, "move_log_csv"), fire = str(r, "fire_log_csv");
                    h.assertTrue(move != null && move.lines().count() > 10, "move_log_csv " + (move == null ? "null" : move.lines().count() + " lines"));
                    h.assertTrue(fire != null && fire.lines().count() > 1, "fire_log_csv " + (fire == null ? "null" : fire.lines().count() + " lines"));
                    actual("GS-20-04", "move_log_csv " + move.lines().count() + " lines; fire_log_csv " + fire.lines().count() + " lines.");
                });
                finish(h, a, b, c, d);
            }).start();
        });
        add("GS-20-05", 400000, h -> {
            RecordingPlayer p = join(h, "gs2005");
            String u = uniqueName("ns");
            register(p, u);
            AtomicReference<JsonObject> row = new AtomicReference<>();
            flow(h).when(1, () -> SessionManager.findActiveSessionRowId(p.getUUID()) > 0, () -> start(p, SimulationState.NEW_SIM_BUILDING2_FIRE))
                   .after(WARMUP + 20, () -> SimulationManager.endSimulation(p.getUUID(), "timeout"))
                   .when(1, giveUpAfter(120, every(20, () -> {
                       JsonArray rows = query("SELECT simulation_type, prep_level, status FROM sessions WHERE username = ? ORDER BY id DESC", u);
                       if (rows.isEmpty() || !"completed".equals(str(rows.get(0).getAsJsonObject(), "status"))) return false;
                       row.set(rows.get(0).getAsJsonObject());
                       return true;
                   })), () -> {
                       h.assertTrue(row.get() != null, "the uploaded row never appeared as completed within 120 s");
                       String type = str(row.get(), "simulation_type");
                       h.assertTrue("NEW_SIM_BUILDING2_FIRE".equals(type), "simulation_type " + type);
                       actual("GS-20-05", "New Sim Building 2.0 row stored simulation_type = \"" + type + "\" (prep_level " + str(row.get(), "prep_level") + ").");
                       h.succeed();
                   }).start();
        });
    }

    // ───────────────────────────── GS-23, GS-24 admin commands and state ─────────────────────────────
    private static void adminAndStateTests() {
        add("GS-23-01", 400000, h -> {   // also GS-23-02..05 (same instructor session)
            RecordingPlayer p = join(h, "gs2301");
            cmd(p, "bfp checkin TEST-BFP Student", true);
            long[] row = {-1};
            boolean[] ok = new boolean[5];
            AtomicReference<String> c4 = new AtomicReference<>();
            AtomicReference<RecordingPlayer> student = new AtomicReference<>();
            Function<String, String> col = c -> str(query("SELECT " + c + " FROM sessions WHERE id = ?", row[0]).get(0).getAsJsonObject(), c);
            flow(h).when(1, () -> (row[0] = SessionManager.findActiveSessionRowId(p.getUUID())) > 0, () -> {
                cmd(p, "bfp prep_level high", true);
                cmd(p, "bfp confidence 4", true);
                cmd(p, "bfp note Rang alarm late", true);
                cmd(p, "bfp note Good PASS technique", true);
                cmd(p, "bfp score 85", true);
            }).when(1, every(20, () -> "85".equals(col.apply("simulation_score")) && col.apply("bfp_notes") != null
                    && col.apply("bfp_notes").contains("Good PASS")), () -> {
                ok[0] = caseCheck("GS-23-01", () -> {
                    String level = col.apply("prep_level");
                    h.assertTrue("HIGH".equals(level), "prep_level stored as \"" + level + "\"");
                    actual("GS-23-01", "/bfp prep_level high stored prep_level = \"" + level + "\".");
                });
                c4.set(col.apply("confidence"));
                cmd(p, "bfp confidence 7", true);
                ok[2] = caseCheck("GS-23-03", () -> {
                    String notes = col.apply("bfp_notes");
                    h.assertTrue(notes.contains("Rang alarm late") && notes.contains("|"), "bfp_notes: " + notes);
                    actual("GS-23-03", "bfp_notes = \"" + notes + "\"");
                });
                ok[3] = caseCheck("GS-23-04", () -> actual("GS-23-04", "/bfp score 85 stored simulation_score = 85."));
                student.set(RecordingPlayer.join(h, "gs2305"));
                cmd(student.get(), "bfp prep_level low", false);
            }).after(100, () -> {
                ok[1] = caseCheck("GS-23-02", () -> {
                    String after = col.apply("confidence");
                    h.assertTrue(after != null && after.equals(c4.get()), "/bfp confidence 7 changed the value to " + after);
                    double v = Double.parseDouble(c4.get());
                    h.assertTrue(Math.abs(v - 0.8) < 1e-9, "confidence 4 stored as " + c4.get() + " (the database and dashboard use 0.0-1.0)");
                    actual("GS-23-02", "/bfp confidence 4 stored " + c4.get() + "; /bfp confidence 7 rejected (value unchanged).");
                });
                ok[4] = caseCheck("GS-23-05", () -> {
                    String level = col.apply("prep_level");
                    h.assertTrue(level == null || !level.equalsIgnoreCase("low"), "non-operator changed prep_level to " + level);
                    actual("GS-23-05", "Non-operator /bfp was refused (" + student.get().messages.stream().findFirst().orElse("no reply").trim()
                            + "); prep_level stayed " + level + ".");
                });
                reset(h);
                finish(h, ok);
            }).start();
        });
        add("GS-24-02", 200, h -> {
            RecordingPlayer p = join(h, "gs2402");
            SimulationSession first = start(p, SimulationState.FIRE);
            SimulationManager.startSimulation(p, SimulationState.EARTHQUAKE);
            h.assertTrue(SimulationManager.getSession(p.getUUID()) == first, "second start replaced the session");
            h.assertTrue(p.heard("already have a simulation in progress"), "no refusal message; got " + p.messages);
            actual("GS-24-02", "Second start refused: \"You already have a simulation in progress!\"; first session kept.");
            reset(h);
            h.succeed();
        });
        add("GS-24-03", 200, h -> {
            RecordingPlayer p = join(h, "gs2403");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 10, () -> {
                cmd(p, "sim_stop", true);
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "/sim_stop left the session running");
                Object reason = endReason(s);
                h.assertTrue(reason != null && !"timeout".equals(reason), "stopped run recorded with end_reason " + reason);
                actual("GS-24-03", "/sim_stop ended the run; end_reason = " + reason + ".");
            }).end();
        });
        add("GS-24-04", 300, h -> {
            RecordingPlayer p = join(h, "gs2404");
            SimulationSession s = start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 10, () -> {
                p.leave();
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "session left running after disconnect");
                Object reason = endReason(s);
                RecordingPlayer next = RecordingPlayer.join(h, "gs2404b");
                SimulationManager.startSimulation(next, SimulationState.FIRE);
                h.assertTrue(SimulationManager.getSession(next.getUUID()) != null, "Library still occupied after the disconnect");
                h.assertTrue(reason != null && !"timeout".equals(reason), "disconnect recorded with end_reason " + reason);
                actual("GS-24-04", "Disconnect closed the run (end_reason " + reason + ") and released the Library for the next student.");
                reset(h);
            }).end();
        });
    }
}
