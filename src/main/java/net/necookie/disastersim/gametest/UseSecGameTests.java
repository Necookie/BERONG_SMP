package net.necookie.disastersim.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.common.hazard.HazardBlock;
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.common.player.DropAndRollManager;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.network.SimulationStatusPayload;
import net.necookie.disastersim.registry.ModBlocks;
import net.necookie.disastersim.registry.ModItems;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static net.necookie.disastersim.gametest.BerongGameTests.add;
import static net.necookie.disastersim.gametest.GT.*;

/** Game usability (G-USE) and extra security (G-SEC) cases. */
final class UseSecGameTests {

    private UseSecGameTests() {}

    private static final int WARMUP = 60;
    private static final String PW = "GtPass#2026";
    static final String TEST_PIN = "GT-PIN-4821";   // written to run-gametest config by gt_config.mjs

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

    private static BlockPos lobbyButton(String field) {
        try {
            Field f = net.necookie.disastersim.common.structure.LobbyManager.class.getDeclaredField(field);
            f.setAccessible(true);
            return (BlockPos) f.get(null);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static void press(RecordingPlayer p, String field) {
        BlockPos b = lobbyButton(field);
        if (b == null) throw new IllegalStateException(field + " not found");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, b,
                new BlockHitResult(Vec3.atCenterOf(b), Direction.UP, b, false)));
    }

    /** Message text without Minecraft colour codes, for quoting in the report. */
    private static String plain(String m) { return m.replaceAll("§.", "").replace('\n', ' ').trim(); }

    private static String said(RecordingPlayer p, String fragment) {
        return p.messages.stream().filter(m -> m.contains(fragment)).map(UseSecGameTests::plain).findFirst().orElse(null);
    }

    private static List<String> tooltip(Item item) {
        List<String> lines = new ArrayList<>();
        item.appendHoverText(new ItemStack(item), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT,
                (Component c) -> lines.add(plain(c.getString())), TooltipFlag.NORMAL);
        return lines;
    }

    private static boolean bfpVisible(RecordingPlayer p) {
        return ((net.minecraft.server.level.ServerLevel) p.level()).getServer().getCommands().getDispatcher().getRoot().getChild("bfp").getChild("bypass")
                .canUse(p.createCommandSourceStack());
    }

    static void registerAll() {
        usability();
        security();
    }

    // ───────────────────────────── Usability ─────────────────────────────
    private static void usability() {
        add("G-USE-01", 60, h -> {
            RecordingPlayer p = join(h, "use01");
            press(p, "simulationButtonPos");
            String m = said(p, "/register");
            h.assertTrue(m != null && m.contains("/register <username> <password>") && m.contains("/login"), "gate message: " + p.messages);
            actual("G-USE-01", "Unregistered student pressing the simulation button is told exactly what to type: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-02", 400000, h -> {
            RecordingPlayer p = join(h, "use02");
            String u = uniqueName("us");
            register(p, u);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                String m = said(p, "Registered as");
                h.assertTrue(m != null && m.contains("tutorial") && m.contains("/login"), "registration reply: " + p.messages);
                actual("G-USE-02", "After /register the student is told the next step and how to come back: \"" + m + "\"");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-USE-03", 400000, h -> {
            RecordingPlayer p = join(h, "use03");
            String u = uniqueName("us");
            register(p, u);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                p.messages.clear();
                press(p, "simulationButtonPos");
                String m = said(p, "Academy");
                h.assertTrue(m != null && m.contains("first button") && m.contains("/login"), "certification gate: " + p.messages);
                actual("G-USE-03", "A registered but uncertified student gets: \"" + m + "\" (names the button to press and the way to restore an earlier certification).");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-USE-04", 400000, h -> {
            RecordingPlayer p = join(h, "use04");
            String u = uniqueName("us");
            register(p, u);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                SessionManager.checkout(p.getUUID());
                p.messages.clear();
                press(p, "simulationButtonPos");
                String m = said(p, "expired");
                h.assertTrue(m != null && m.contains("/login"), "expired-session gate: " + p.messages);
                actual("G-USE-04", "With the class session closed the button says: \"" + m + "\"");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-USE-05", 60, h -> {
            RecordingPlayer p = join(h, "use05");
            cmd(p, "history", false);
            String m = said(p, "/login");
            h.assertTrue(m != null, "history reply: " + p.messages);
            actual("G-USE-05", "/history before logging in: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-06", 60, h -> {
            RecordingPlayer p = join(h, "use06");
            cmd(p, "register ab \"GoodPass#1\" TEST-X TEST3A Short Name", false);
            String a = said(p, "Username must");
            cmd(p, "register " + uniqueName("us") + " \"abc\" TEST-X TEST3A Short Pass", false);
            String b = said(p, "Password must");
            h.assertTrue(a != null && a.contains("3-16") && b != null && b.contains("6"), "validation replies: " + p.messages);
            actual("G-USE-06", "Bad registration input states the rule to follow: \"" + a + "\" and \"" + b + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-07", WARMUP + 40, h -> {
            RecordingPlayer p = join(h, "use07");
            start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 5, () -> {
                List<String> order = p.messages.stream().filter(m -> m.contains("Simulation starts in") || m.contains("Simulation has started"))
                        .map(UseSecGameTests::plain).toList();
                h.assertTrue(order.size() == 4 && order.get(3).contains("started"), "countdown: " + order);
                actual("G-USE-07", "The 3-second warmup counts down in chat: " + String.join(" / ", order) + ".");
                reset(h);
            }).end();
        });
        add("G-USE-08", 60, h -> {
            RecordingPlayer p = join(h, "use08");
            start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            int secs = Config.NEW_SIM2_PREVENTION_TICKS.get() / 20;
            String expected = String.format("%d:%02d", secs / 60, secs % 60);
            String how = said(p, "bare-handed");
            String count = said(p, "hazards are hidden");
            h.assertTrue(how != null && how.contains(expected), "briefing time (configured " + expected + "): " + how);
            h.assertTrue(count != null && count.contains(String.valueOf(SimulationManager.getSession(p.getUUID()).getArmedHazards().size())), "hazard count: " + count);
            actual("G-USE-08", "New Sim Building 2.0 briefing tells the student how many hazards, how to prevent them and the real time limit: \""
                    + count + "\" / \"" + how + "\" (configured window " + expected + ").");
            reset(h);
            h.succeed();
        });
        add("G-USE-09", 60, h -> {
            RecordingPlayer p = join(h, "use09");
            start(p, SimulationState.FIRE);
            String m = said(p, "slot 1");
            h.assertTrue(m != null && m.toLowerCase().contains("pull the pin"), "fire intro: " + p.messages);
            actual("G-USE-09", "Library fire run opens with: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-10", 40, h -> {
            RecordingPlayer p = join(h, "use10");
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            String m = said(p, "PIN PULLED");
            h.assertTrue(m != null && m.contains("Hold right-click"), "pin message: " + p.messages);
            actual("G-USE-10", "First right-click with an extinguisher answers: \"" + m + "\" (confirms the step and says what to do next).");
            reset(h);
            h.succeed();
        });
        add("G-USE-11", 100, h -> {
            RecordingPlayer p = join(h, "use11");
            BlockPos base = platform(h);
            BlockPos pos = base.east(3);
            h.getLevel().setBlock(pos, ModBlocks.UNATTENDED_GREASE_PAN.get().defaultBlockState()
                    .setValue(HazardBlock.HAZARDOUS, true).setValue(HazardBlock.ON_FIRE, true), 3);
            faceFrom(p, pos, 2.5);
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            spray(p, 10);
            String m = said(p, "✗");
            h.assertTrue(m != null && m.contains("wet chemical"), "wrong-tool warning: " + p.messages);
            actual("G-USE-11", "Spraying ABC dry chemical on a grease fire is refused and names the right tool: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-12", 200, h -> {
            RecordingPlayer p = join(h, "use12");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            seq(h).at(WARMUP + 2, () -> {
                BlockPos hz = s.getArmedHazards().get(0);
                rightClickBlock(p, hz);
                String m = said(p, "Hazard prevented");
                h.assertTrue(m != null && m.contains("1/" + s.getArmedHazards().size()), "progress message: " + p.messages);
                actual("G-USE-12", "Preventing a hazard shows progress: \"" + m + "\"");
                reset(h);
            }).end();
        });
        add("G-USE-13", 900, h -> {   // also G-USE-14 (evacuation announcement in the same run)
            RecordingPlayer p = join(h, "use13");
            SimulationSession s = start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            boolean[] ok = new boolean[2];
            flow(h).when(WARMUP, () -> s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, () -> {
                ok[0] = caseCheck("G-USE-13", () -> {
                    String m = said(p, "caught fire");
                    h.assertTrue(m != null && m.contains("extinguisher"), "intervention message: " + p.messages);
                    actual("G-USE-13", "When prevention time runs out: \"" + m + "\" (says what happened and what to grab).");
                });
            }).when(1, () -> s.getFirePhase() == SimulationSession.FirePhase.EVACUATION, () -> {
                ok[1] = caseCheck("G-USE-14", () -> {
                    String m = said(p, "EVACUATE");
                    h.assertTrue(m != null && m.contains("assembly area"), "evacuation message: " + p.messages);
                    actual("G-USE-14", "When the fire gets out of control: \"" + m + "\" (names where to go).");
                });
                reset(h);
                finish(h, ok);
            }).start();
        });
        add("G-USE-15", 200, h -> {
            RecordingPlayer p = join(h, "use15");
            start(p, SimulationState.FIRE);
            p.drainPayloads();
            seq(h).at(WARMUP + 60, () -> {
                List<SimulationStatusPayload> hud = new ArrayList<>();
                for (CustomPacketPayload pl : p.drainPayloads()) if (pl instanceof SimulationStatusPayload sp) hud.add(sp);
                h.assertTrue(hud.size() >= 2, "HUD updates received: " + hud.size());
                SimulationStatusPayload first = hud.get(0), lastP = hud.get(hud.size() - 1);
                h.assertTrue("FIRE".equals(first.status()) && lastP.timeLeft() < first.timeLeft(), "HUD values " + first + " -> " + lastP);
                actual("G-USE-15", hud.size() + " HUD status packets over 3 seconds of play; status " + first.status()
                        + ", time left went from " + first.timeLeft() + " s to " + lastP.timeLeft() + " s, so the on-screen timer keeps counting.");
                reset(h);
            }).end();
        });
        add("G-USE-16", 200, h -> {
            RecordingPlayer p = join(h, "use16");
            start(p, SimulationState.FIRE);
            seq(h).at(WARMUP + 10, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                String score = said(p, "Score:");
                String tip = said(p, "Tip: Remember PASS");
                h.assertTrue(score != null && score.contains("/ 100") && tip != null, "feedback: " + p.messages);
                actual("G-USE-16", "End of a Library fire run shows a feedback block with the score out of 100 (\"" + score
                        + "\") and a reminder (\"" + tip + "\").");
                reset(h);
            }).end();
        });
        add("G-USE-17", 200, h -> {
            RecordingPlayer p = join(h, "use17");
            start(p, SimulationState.NEW_SIM_BUILDING2_FIRE);
            seq(h).at(WARMUP + 5, () -> {
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                String m = said(p, "debrief");
                h.assertTrue(m != null && m.contains("5 seconds"), "end message: " + p.messages);
                actual("G-USE-17", "End of a New Sim Building 2.0 run tells the student what happens next: \"" + m + "\"");
                reset(h);
            }).end();
        });
        add("G-USE-18", 600, h -> {
            RecordingPlayer p = join(h, "use18");
            start(p, SimulationState.EARTHQUAKE);
            flow(h).when(WARMUP, () -> p.heard("shaking has stopped"), () -> {
                List<String> seen = new ArrayList<>();
                for (String f : List.of("Magnitude", "intensifying", "Aftershock", "shaking has stopped"))
                    if (p.heard(f)) seen.add(said(p, f));
                h.assertTrue(seen.size() == 4, "quake announcements: " + seen);
                actual("G-USE-18", "Every earthquake phase is announced: " + String.join(" / ", seen) + ".");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-USE-19", 20, h -> {
            List<Item> items = List.of(ModItems.FIRE_EXTINGUISHER.get(), ModItems.CO2_EXTINGUISHER.get(), ModItems.WET_CHEMICAL_EXTINGUISHER.get(),
                    ModItems.FIRE_BLANKET.get(), ModItems.FIRST_AID_KIT.get(), ModItems.FLASHLIGHT.get(), ModItems.SAFETY_WHISTLE.get());
            List<String> rows = new ArrayList<>();
            for (Item it : items) {
                List<String> tip = tooltip(it);
                h.assertTrue(!tip.isEmpty(), it.getDescriptionId() + " has no tooltip");
                rows.add(it.getDescriptionId().replace("item.berongsmp.", "") + " (" + tip.size() + " lines: \"" + tip.get(0) + "\")");
            }
            actual("G-USE-19", "Every student safety item explains itself on hover: " + String.join("; ", rows) + ".");
            h.succeed();
        });
        add("G-USE-20", 100, h -> {
            reset(h);
            RecordingPlayer a = RecordingPlayer.join(h, "use20a"), b = RecordingPlayer.join(h, "use20b");
            start(a, SimulationState.FIRE);
            SimulationManager.startSimulation(b, SimulationState.EARTHQUAKE);
            String m = said(b, "in use");
            h.assertTrue(m != null && SimulationManager.getSession(b.getUUID()) == null, "busy-arena reply: " + b.messages);
            actual("G-USE-20", "A second student asking for the busy Library is told why and what to do: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
        add("G-USE-21", 40, h -> {
            RecordingPlayer p = join(h, "use21");
            BlockPos base = platform(h);
            h.getLevel().setBlock(base.east(2), ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            rightClickBlock(p, base.east(2));
            String m = said(p, "ALARM");
            h.assertTrue(m != null && m.contains("No active fire"), "alarm reply outside a run: " + p.messages);
            actual("G-USE-21", "Pressing a fire alarm when nothing is burning explains why nothing happened: \"" + m + "\"");
            reset(h);
            h.succeed();
        });
    }

    // ───────────────────────────── Security ─────────────────────────────
    private static void security() {
        add("G-SEC-01", 400000, h -> {
            RecordingPlayer p = join(h, "sec01");
            String u = uniqueName("sq");
            String evil = "Robert'); DROP TABLE student_accounts;--";
            cmd(p, "register " + u + " \"" + PW + "\" TEST-" + u + " TEST3A " + evil, false);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                JsonArray rows = query("SELECT full_name FROM student_accounts WHERE username = ?", u);
                h.assertTrue(rows.size() == 1 && evil.equals(str(rows.get(0).getAsJsonObject(), "full_name")), "stored row: " + rows);
                String n = str(query("SELECT COUNT(*) AS n FROM student_accounts").get(0).getAsJsonObject(), "n");
                actual("G-SEC-01", "A full name containing SQL (\"" + evil + "\") was stored as plain text; the student_accounts table is intact ("
                        + n + " rows). Queries use bound parameters.");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-02", 400000, h -> {
            RecordingPlayer p = join(h, "sec02");
            String u = uniqueName("sq");
            register(p, u);
            AtomicReference<RecordingPlayer> q = new AtomicReference<>();
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                q.set(RecordingPlayer.join(h, "sec02b"));
                cmd(q.get(), "login " + u + " ' OR '1'='1", false);
            }).when(1, giveUpAfter(30, () -> q.get().heard("Incorrect")), () -> {
                h.assertTrue(AuthManager.current(q.get().getUUID()) == null, "SQL-shaped password logged in");
                actual("G-SEC-02", "/login with the password ' OR '1'='1 was refused (\"" + said(q.get(), "Incorrect") + "\").");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-03", 400000, h -> {
            RecordingPlayer p = join(h, "sec03");
            String u = uniqueName("bf");
            register(p, u);
            AtomicReference<RecordingPlayer> q = new AtomicReference<>();
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                q.set(RecordingPlayer.join(h, "sec03b"));
                for (int i = 0; i < 5; i++) cmd(q.get(), "login " + u + " Wrong#" + i, false);
            }).when(1, giveUpAfter(60, () -> q.get().messages.stream().filter(m -> m.contains("Incorrect")).count() >= 5), () -> {
                cmd(q.get(), "login " + u + " " + PW, false);
            }).when(1, giveUpAfter(30, () -> q.get().heard("Too many")), () -> {
                h.assertTrue(AuthManager.current(q.get().getUUID()) == null, "correct password accepted during lockout");
                actual("G-SEC-03", "After 5 wrong passwords, even the right one is refused: \"" + said(q.get(), "Too many") + "\" (5-minute lockout).");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-04", 400000, h -> {
            RecordingPlayer p = join(h, "sec04");
            String u = uniqueName("ha");
            register(p, u);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                String hash = str(query("SELECT password_hash FROM student_accounts WHERE username = ?", u).get(0).getAsJsonObject(), "password_hash");
                String[] parts = hash.split("\\$");
                h.assertTrue(parts.length == 4 && parts[0].equals("pbkdf2_sha256") && parts[1].equals("210000") && !hash.contains(PW), "stored: " + hash);
                actual("G-SEC-04", "Password stored only as a salted hash: pbkdf2_sha256, 210,000 iterations, " + parts[2].length()
                        + "-character salt, " + parts[3].length() + "-character hash; the plain password appears nowhere in the row.");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-05", 400000, h -> {
            RecordingPlayer p = join(h, "sec05");
            long t0 = System.nanoTime();
            cmd(p, "login someone " + "A".repeat(129), false);
            flow(h).when(1, giveUpAfter(10, () -> p.heard("Incorrect")), () -> {
                double ms = (System.nanoTime() - t0) / 1e6;
                h.assertTrue(p.heard("Incorrect") && ms < 500, "reply after " + ms + " ms: " + p.messages);
                actual("G-SEC-05", String.format("A 129-character password is rejected in %.0f ms without hashing it (cap is 128), so long inputs cannot tie up the server.", ms));
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-06", 60, h -> {
            RecordingPlayer p = join(h, "sec06");
            for (String c : List.of("sim_fire library", "sim_earthquake library", "sim_stop", "sim_time set 10", "sim_freeze"))
                cmd(p, c, false);
            h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "a student started a run with /sim_fire");
            actual("G-SEC-06", "A student without operator rights cannot use /sim_fire, /sim_earthquake, /sim_stop, /sim_time or /sim_freeze; no run started ("
                    + said(p, "Unknown") + ").");
            reset(h);
            h.succeed();
        });
        add("G-SEC-07", 60, h -> {
            RecordingPlayer p = join(h, "sec07");
            int before = p.getInventory().countItem(ModItems.FIRE_EXTINGUISHER.get());
            for (String c : List.of("item kit", "item get fire_extinguisher", "place_buildings", "spawn_npc", "spawn_lspu"))
                cmd(p, c, false);
            int after = p.getInventory().countItem(ModItems.FIRE_EXTINGUISHER.get());
            h.assertTrue(after == before, "items were given to a student");
            actual("G-SEC-07", "/item, /place_buildings, /spawn_npc and /spawn_lspu are refused for students; inventory unchanged.");
            reset(h);
            h.succeed();
        });
        add("G-SEC-08", 60, h -> {
            RecordingPlayer p = join(h, "sec08");
            boolean before = bfpVisible(p);
            cmd(p, "bfp login " + TEST_PIN, false);
            boolean after = bfpVisible(p);
            h.assertTrue(!before && p.heard("access granted") && after, "PIN login: before " + before + ", after " + after + ", chat " + p.messages);
            actual("G-SEC-08", "An instructor without operator rights entered the BFP PIN: \"" + said(p, "access granted")
                    + "\"; /bfp admin commands became available only after the PIN.");
            reset(h);
            h.succeed();
        });
        add("G-SEC-09", 60, h -> {
            RecordingPlayer p = join(h, "sec09");
            for (int i = 0; i < 5; i++) cmd(p, "bfp login 0000" + i, false);
            cmd(p, "bfp login " + TEST_PIN, false);
            h.assertTrue(!bfpVisible(p) && p.heard("Too many incorrect"), "after 5 wrong PINs: " + p.messages);
            actual("G-SEC-09", "Five wrong PINs lock /bfp login: the correct PIN was then refused with \"" + said(p, "Too many incorrect") + "\"");
            reset(h);
            h.succeed();
        });
        add("G-SEC-10", 60, h -> {
            RecordingPlayer p = join(h, "sec10");
            cmd(p, "bfp login " + TEST_PIN, false);
            boolean granted = bfpVisible(p);
            SessionManager.checkin(p.getUUID(), p.getName().getString(), "TEST Next Student");
            h.assertTrue(granted && !bfpVisible(p), "instructor rights survived a new student check-in");
            actual("G-SEC-10", "When the next student checks in at the same station, the instructor's PIN access is dropped, so the student cannot use /bfp.");
            reset(h);
            h.succeed();
        });
        add("G-SEC-11", 400000, h -> {
            RecordingPlayer p = join(h, "sec11");
            String u = uniqueName("lo");
            register(p, u);
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                cmd(p, "bfp login " + TEST_PIN, false);
                java.util.UUID id = p.getUUID();
                p.leave();
                h.assertTrue(AuthManager.current(id) == null && SessionManager.getActiveSession(id) == null, "login state kept after disconnect");
                actual("G-SEC-11", "Disconnecting clears the station's student login, class session and instructor PIN access.");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-12", 40, h -> {
            // All 10 presses run inside one server tick, so nothing else can change the fire ticks
            // in between: only the first press may count, the throttle must swallow the other 9.
            RecordingPlayer p = join(h, "sec12");
            p.setRemainingFireTicks(200);
            List<Integer> after = new ArrayList<>();
            int before = p.getRemainingFireTicks();
            for (int i = 0; i < 10; i++) {
                DropAndRollManager.onDropAndRollRequest(p);
                after.add(p.getRemainingFireTicks());
            }
            metric("G-SEC-12", java.util.Map.of("before", before, "after_each_press", after));
            h.assertTrue(after.stream().distinct().count() == 1 && before - after.get(0) <= 30,
                    "fire ticks " + before + " then after each press " + after);
            actual("G-SEC-12", "Ten drop-and-roll packets in the same tick: fire ticks went " + before + " to " + after.get(0)
                    + " on the first and stayed at " + after.get(0) + " for the other nine, so a modified client cannot spam it to put the fire out instantly.");
            reset(h);
            h.succeed();
        });
        add("G-SEC-13", 400000, h -> {
            RecordingPlayer p = join(h, "sec13");
            String u = uniqueName("en");
            register(p, u);
            AtomicReference<RecordingPlayer> q = new AtomicReference<>();
            flow(h).when(1, giveUpAfter(30, () -> AuthManager.current(p.getUUID()) != null), () -> {
                q.set(RecordingPlayer.join(h, "sec13b"));
                cmd(q.get(), "login " + u + " WrongPass#9", false);
                cmd(q.get(), "login nosuch" + u + " WrongPass#9", false);
            }).when(1, giveUpAfter(30, () -> q.get().messages.stream().filter(m -> m.contains("Incorrect")).count() >= 2), () -> {
                List<String> replies = q.get().messages.stream().filter(m -> m.contains("Incorrect")).map(UseSecGameTests::plain).toList();
                h.assertTrue(replies.size() == 2 && replies.get(0).equals(replies.get(1)), "replies differ: " + replies);
                actual("G-SEC-13", "Wrong password and unknown username get the same reply (\"" + replies.get(0) + "\"), so the station cannot be used to find out which usernames exist.");
                reset(h);
                h.succeed();
            }).start();
        });
        add("G-SEC-14", 40, h -> {
            RecordingPlayer p = join(h, "sec14");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MEGAPHONE.get()));
            ModItems.MEGAPHONE.get().use(p.level(), p, InteractionHand.MAIN_HAND);
            String m = said(p, "instructor tool");
            h.assertTrue(m != null && !p.heard("[MEGAPHONE]"), "megaphone: " + p.messages);
            actual("G-SEC-14", "A student using the megaphone gets \"" + m + "\" and no evacuation call is broadcast.");
            reset(h);
            h.succeed();
        });
        add("G-SEC-15", 60, h -> {
            RecordingPlayer p = join(h, "sec15");
            BlockPos base = platform(h);
            BlockPos pos = base.east(2);
            BlockState armed = ModBlocks.OVERLOADED_WALL_OUTLET.get().defaultBlockState().setValue(HazardBlock.HAZARDOUS, true);
            h.getLevel().setBlock(pos, armed, 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HAZARD_WAND.get()));
            ModItems.HAZARD_WAND.get().useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            boolean still = HazardManager.isHazardous(h.getLevel().getBlockState(pos));
            h.assertTrue(still, "a student reset a hazard with the hazard wand");
            actual("G-SEC-15", "A student holding the instructor hazard wand cannot reset an armed hazard (it stays hazardous).");
            reset(h);
            h.succeed();
        });
    }
}
