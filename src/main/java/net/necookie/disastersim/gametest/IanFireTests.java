package net.necookie.disastersim.gametest;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationManager.SimulationState;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.item.AbstractExtinguisherItem;
import net.necookie.disastersim.registry.ModBlocks;
import net.necookie.disastersim.registry.ModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.necookie.disastersim.gametest.GT.*;
import static net.necookie.disastersim.gametest.IanGT.*;

/**
 * Ian's FS-01..FS-11 (New Sim Building 2.0 fire scenario), RB-04/RB-05 (extinguisher sequence) and BL-04
 * (hazard evidence), 20 variants each. Timings assume the run-gametest config written by
 * Testing/harness/game/gt_config.mjs: prevention/intervention/evacuation 200 ticks each after a 60-tick warmup.
 */
final class IanFireTests {

    private IanFireTests() {}

    private static final SimulationState NSB2 = SimulationState.NEW_SIM_BUILDING2_FIRE;
    private static Item ext(int i) {
        return switch (i % 3) { case 0 -> ModItems.FIRE_EXTINGUISHER.get(); case 1 -> ModItems.CO2_EXTINGUISHER.get(); default -> ModItems.WET_CHEMICAL_EXTINGUISHER.get(); };
    }
    private static String extName(int i) { return switch (i % 3) { case 0 -> "ABC"; case 1 -> "CO2"; default -> "wet chemical"; }; }

    private static Item correctExtinguisher(Block b) {
        if (AbstractExtinguisherItem.isKitchenHazard(b)) return ModItems.WET_CHEMICAL_EXTINGUISHER.get();
        if (AbstractExtinguisherItem.isOxidizerHazard(b)) return ModItems.CO2_EXTINGUISHER.get();
        return ModItems.FIRE_EXTINGUISHER.get();
    }
    private static String hz(Block b) { return b.getDescriptionId().replace("block.berongsmp.", ""); }

    private static BlockPos standNear(ServerLevel level, BlockPos target) {
        for (int r = 2; r <= 3; r++)
            for (int dy = -1; dy <= 1; dy++)
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos f = target.relative(d, r).above(dy);
                    if (level.getBlockState(f).isAir() && level.getBlockState(f.above()).isAir() && !level.getBlockState(f.below()).isAir()) return f;
                }
        return target.relative(net.minecraft.core.Direction.WEST, 2);
    }

    private static List<BlockPos> sorted(java.util.Collection<BlockPos> c) {
        List<BlockPos> l = new ArrayList<>(c);
        l.sort(Comparator.<BlockPos>comparingInt(b -> b.getX()).thenComparingInt(b -> b.getY()).thenComparingInt(b -> b.getZ()));
        return l;
    }
    private static Object endReason(SimulationSession s) { return last(s, "SIM_END").map(e -> e.data().get("end_reason")).orElse(null); }
    private static int endScore(SimulationSession s) { return ((Number) last(s, "SIM_END").orElseThrow().data().get("score")).intValue(); }
    private static int score(SimulationSession s, boolean alarm, boolean assembly, String end) {
        return expectedNewSim(s.getArmedHazards().size(), s.getPreventedHazards().size(), s.getEscalatedHazards().size(), s.getExtinguishedEscalated().size(), alarm, assembly, end);
    }

    static void registerAll() {
        // ───────── FS-01 prevention phase activates ─────────
        each("FS-01", 400, (h, i) -> {
            String id = cid("FS-01", i);
            RecordingPlayer p = join(h, "fs01_" + i);
            SimulationSession s = start(p, NSB2);
            int at = WARMUP + 2 + i * 8;   // sampled at 20 different moments inside the 200-tick prevention window
            h.runAfterDelay(at, safe(h, id, () -> verdict(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.PREVENTION, "phase " + s.getFirePhase() + " at tick " + h.getTick());
                int armed = s.getArmedHazards().size();
                need(armed >= 1 && armed <= 5, "armed hazards " + armed);
                long live = s.getArmedHazards().stream().filter(b -> HazardManager.isHazardous(h.getLevel().getBlockState(b))).count();
                need(live == armed, "only " + live + " of " + armed + " armed hazards are hazardous in the world");
                need(!events(s, "SIM_START").isEmpty(), "no SIM_START");
                boolean logged = events(s, "phase_transition").stream().anyMatch(e -> "prevention".equals(e.data().get("phase")));
                return "Tick " + h.getTick() + ": phase PREVENTION, " + armed + " hazards armed and live in the world; phase_transition(prevention) " + (logged ? "logged" : "not logged") + ".";
            })));
        });

        // ───────── FS-02 prevention action ─────────
        each("FS-02", 400, (h, i) -> {
            String id = cid("FS-02", i);
            RecordingPlayer p = join(h, "fs02_" + i);
            SimulationSession s = start(p, NSB2);
            List<BlockPos> armed = new ArrayList<>(s.getArmedHazards());
            int k = Math.min(armed.size(), (i % 5) + 1);   // 1..5 hazards found by hand
            for (int j = 0; j < k; j++) rightClickBlock(p, armed.get((j + i) % armed.size()));
            h.runAfterDelay(WARMUP + 14, safe(h, id, () -> verdict(h, id, () -> {
                need(s.getPreventedHazards().size() == k, "prevented " + s.getPreventedHazards().size() + ", expected " + k);
                need(events(s, "hazard_neutralize").size() == k, "hazard_neutralize events " + events(s, "hazard_neutralize").size() + ", expected " + k);
                for (BlockPos b : s.getPreventedHazards()) need(!HazardManager.isHazardous(h.getLevel().getBlockState(b)), "a prevented hazard is still hazardous");
                if (k == armed.size()) {
                    need("all_hazards_prevented".equals(endReason(s)), "end_reason " + endReason(s));
                    int exp = score(s, false, false, "all_hazards_prevented");
                    need(endScore(s) == exp, "score " + endScore(s) + ", documented " + exp);
                    return "All " + k + " hazards found by hand: run ended all_hazards_prevented, score " + endScore(s) + " (" + k + " x 12 + 25).";
                }
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                int exp = score(s, false, false, "timeout");
                need(endScore(s) == exp, "score " + endScore(s) + ", documented " + exp);
                return k + "/" + armed.size() + " hazards found: " + k + " hazard_neutralize events, score " + endScore(s) + " (" + k + " x 12).";
            })));
        });

        // ───────── FS-03 hazard escalation starts the intervention phase ─────────
        each("FS-03", 500, (h, i) -> {
            String id = cid("FS-03", i);
            RecordingPlayer p = join(h, "fs03_" + i);
            SimulationSession s = start(p, NSB2);
            List<BlockPos> armed = new ArrayList<>(s.getArmedHazards());
            int k = Math.min(armed.size() - 1, i % 5);   // 0..4 found before the timer, at least one left to escalate
            for (int j = 0; j < k; j++) rightClickBlock(p, armed.get((j + i) % armed.size()));
            h.runAfterDelay(WARMUP + 205 + (i % 4), safe(h, id, () -> verdict(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, "phase " + s.getFirePhase());
                int esc = s.getEscalatedHazards().size();
                need(esc == armed.size() - k, "escalated " + esc + ", expected " + (armed.size() - k));
                int failures = events(s, "hazard_failure").size();
                need(failures >= esc, "hazard_failure events " + failures + " for " + esc + " escalated");
                need(events(s, "phase_transition").stream().anyMatch(e -> "intervention".equals(e.data().get("phase"))), "no phase_transition(intervention)");
                return k + " hazards found, timer ran out: phase INTERVENTION, " + esc + " escalated, " + failures + " hazard_failure events, phase_transition(intervention) logged.";
            })));
        });

        // ───────── FS-04 alarm activation ─────────
        each("FS-04", 700, (h, i) -> {
            String id = cid("FS-04", i);
            RecordingPlayer p = join(h, "fs04_" + i);
            SimulationSession s = start(p, NSB2);
            BlockPos base = platform(h);
            BlockPos alarm = base.east(2);
            h.getLevel().setBlock(alarm, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3);
            int press = switch (i % 3) { case 0 -> WARMUP + 10; case 1 -> WARMUP + 210; default -> WARMUP + 410; };   // prevention / intervention / evacuation
            int presses = 1 + (i % 2);                                                                                // 1 or 2 presses
            h.runAfterDelay(press, safe(h, id, () -> { for (int n = 0; n < presses; n++) rightClickBlock(p, alarm); }));
            h.runAfterDelay(press + 6, safe(h, id, () -> verdict(h, id, () -> {
                need(events(s, "fire_alarm_activate").size() == 1, "fire_alarm_activate events " + events(s, "fire_alarm_activate").size() + " after " + presses + " press(es)");
                need(s.wasAlarmRung(), "session not marked alarm rung");
                if (presses == 2) need(p.heard("Already activated"), "no 'Already activated' answer to the second press");
                SimulationManager.endSimulation(p.getUUID(), "timeout");
                int exp = score(s, true, false, "timeout");
                need(endScore(s) == exp, "score " + endScore(s) + ", documented " + exp);
                return "Alarm pressed " + presses + "x in " + new String[]{"prevention", "intervention", "evacuation"}[i % 3] + ": one fire_alarm_activate, run marked alarm rung, score " + endScore(s) + " includes the +10.";
            })));
        });

        // ───────── FS-05 extinguisher preparation (pin pull sequence) ─────────
        each("FS-05", 300, (h, i) -> {
            String id = cid("FS-05", i);
            RecordingPlayer p = join(h, "fs05_" + i);
            SimulationSession s = start(p, NSB2);
            BlockPos base = platform(h);
            tp(p, base.getX() + 0.5 + (i % 5) * 0.4, base.getY(), base.getZ() + 0.5);
            h.runAfterDelay(WARMUP + 5 + i, safe(h, id, () -> {
                ItemStack st = arm(p, ext(i));
                verdict(h, id, () -> {
                    need(p.heard("PIN PULLED"), "no pin-pulled message: " + p.messages);
                    List<SimEvent> pins = events(s, "pin_pull");
                    need(pins.size() == 1, "pin_pull events " + pins.size());
                    need(pins.get(0).data().get("extinguisher_class") != null, "pin_pull has no extinguisher_class: " + pins.get(0).data());
                    need(events(s, "ext_spray").isEmpty() && events(s, "EXT_SPRAY").isEmpty(), "a spray was logged on the first click");
                    need(st.getDamageValue() == 0, "durability used on the first click");
                    return extName(i) + " extinguisher: first right-click logged one pin_pull (class " + pins.get(0).data().get("extinguisher_class") + "), showed \"PIN PULLED\", no spray yet.";
                });
            }));
        });

        // ───────── FS-06 correct spray on an escalated hazard ─────────
        each("FS-06", 500, (h, i) -> {
            String id = cid("FS-06", i);
            RecordingPlayer p = join(h, "fs06_" + i);
            SimulationSession s = start(p, NSB2);
            List<BlockPos> armed = new ArrayList<>(s.getArmedHazards());
            int k = Math.min(armed.size() - 2, i % 3);   // leave at least two escalated so one extinguish does not end the run
            for (int j = 0; j < Math.max(0, k); j++) rightClickBlock(p, armed.get((j + i) % armed.size()));
            h.runAfterDelay(WARMUP + 206, safe(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, "phase " + s.getFirePhase());
                List<BlockPos> esc = sorted(s.getEscalatedHazards());
                BlockPos t = esc.get(i % esc.size());
                Block b = h.getLevel().getBlockState(t).getBlock();
                BlockPos stand = standNear(h.getLevel(), t);
                tp(p, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(t));
                arm(p, correctExtinguisher(b));
                spray(p, 12);
                h.runAfterDelay(4, safe(h, id, () -> verdict(h, id, () -> {   // relative to now: this runs inside a callback
                    need(s.getExtinguishedEscalated().contains(t), "escalated " + hz(b) + " not counted as contained; state " + h.getLevel().getBlockState(t));
                    boolean hit = events(s, "ext_spray").stream().anyMatch(e -> Boolean.TRUE.equals(e.data().get("hit_fire")));
                    need(hit, "no ext_spray event with hit_fire=true");
                    return "Sprayed escalated " + hz(b) + " with the correct extinguisher: contained (" + s.getExtinguishedEscalated().size() + "/" + s.getEscalatedHazards().size() + "), ext_spray hit_fire=true logged.";
                })));
            }));
        });

        // ───────── FS-07 incorrect spray (wrong class, or aimed away) ─────────
        each("FS-07", 500, (h, i) -> {
            String id = cid("FS-07", i);
            RecordingPlayer p = join(h, "fs07_" + i);
            SimulationSession s = start(p, NSB2);
            h.runAfterDelay(WARMUP + 206, safe(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.INTERVENTION, "phase " + s.getFirePhase());
                List<BlockPos> esc = sorted(s.getEscalatedHazards());
                BlockPos t = esc.get(i % esc.size());
                Block b = h.getLevel().getBlockState(t).getBlock();
                boolean wrongClass = i % 2 == 0 && (AbstractExtinguisherItem.isKitchenHazard(b) || AbstractExtinguisherItem.isOxidizerHazard(b));
                BlockPos stand = standNear(h.getLevel(), t);
                tp(p, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                if (wrongClass) {
                    p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(t));
                    arm(p, AbstractExtinguisherItem.isKitchenHazard(b) ? ModItems.FIRE_EXTINGUISHER.get() : ModItems.WET_CHEMICAL_EXTINGUISHER.get());
                } else {
                    p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(t.above(25)));   // right unit, aimed at the ceiling
                    arm(p, correctExtinguisher(b));
                }
                spray(p, 10);
                h.runAfterDelay(4, safe(h, id, () -> verdict(h, id, () -> {   // relative to now: this runs inside a callback
                    need(!s.getExtinguishedEscalated().contains(t), "a wrong spray still counted as contained");
                    need(HazardManager.isHazardous(h.getLevel().getBlockState(t)), "hazard no longer hazardous after a wrong spray");
                    boolean anyHit = events(s, "ext_spray").stream().anyMatch(e -> Boolean.TRUE.equals(e.data().get("hit_fire")));
                    // a wrong-class spray at the hazard still reaches it (a hit that does not put it out); only a spray aimed away must log no hit
                    if (!wrongClass) need(!anyHit, "a spray aimed away from the hazard logged hit_fire=true");
                    if (wrongClass) need(p.messages.stream().anyMatch(m -> m.contains("✗")), "no wrong-extinguisher warning; chat: " + p.messages);
                    return (wrongClass ? "Wrong extinguisher class on " + hz(b) + ": warning shown, " : "Spray aimed away from " + hz(b) + ": ")
                            + "hazard still burning, not counted as contained (" + s.getExtinguishedEscalated().size() + "/" + s.getEscalatedHazards().size() + ")" + (wrongClass ? ", the wrong class does not put it out." : ", no hit_fire=true event.");
                })));
            }));
        });

        // ───────── FS-08 evacuation transition ─────────
        each("FS-08", 700, (h, i) -> {
            String id = cid("FS-08", i);
            RecordingPlayer p = join(h, "fs08_" + i);
            SimulationSession s = start(p, NSB2);
            List<BlockPos> armed = new ArrayList<>(s.getArmedHazards());
            int k = Math.min(armed.size() - 1, i % 4);
            for (int j = 0; j < k; j++) rightClickBlock(p, armed.get((j + i) % armed.size()));
            h.runAfterDelay(WARMUP + 408 + (i % 5), safe(h, id, () -> verdict(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.EVACUATION, "phase " + s.getFirePhase());
                need(events(s, "phase_transition").stream().anyMatch(e -> "evacuation".equals(e.data().get("phase"))), "no phase_transition(evacuation)");
                need(SimulationManager.getSession(p.getUUID()) == s, "run ended before evacuation");
                return k + " hazards found and none contained: intervention timer ran out, phase EVACUATION, phase_transition(evacuation) logged, run still active.";
            })));
        });

        // ───────── FS-09 emergency exit (placeholder New Sim Building 2.0 exit zones, see ExitZones) ─────────
        each("FS-09", 400, (h, i) -> {
            String id = cid("FS-09", i);
            RecordingPlayer p = join(h, "fs09_" + i);
            SimulationSession s = start(p, NSB2);
            boolean south = i % 2 == 0;
            double x = -95.5 + (i % 5) * 1.0, z = (south ? 481.4 : 497.4) + (i / 5) * 0.4;
            h.runAfterDelay(WARMUP + 5, safe(h, id, () -> tp(p, x, -32, z)));
            h.runAfterDelay(WARMUP + 12, safe(h, id, () -> verdict(h, id, () -> {
                List<SimEvent> ex = events(s, "emergency_exit");
                need(ex.size() == 1, "emergency_exit events " + ex.size() + " at (" + x + ", -32, " + z + ")");
                need(SimulationManager.getSession(p.getUUID()) == s, "run ended at the exit");
                return "Crossed the " + (south ? "south" : "north") + " lobby door zone at (" + x + ", " + z + "): one emergency_exit logged (" + ex.get(0).data().get("exit") + "), run continued. (Zone coordinates are flagged placeholder in ExitZones.)";
            })));
        });

        // ───────── FS-10 assembly completion ─────────
        each("FS-10", 700, (h, i) -> {
            String id = cid("FS-10", i);
            RecordingPlayer p = join(h, "fs10_" + i);
            SimulationSession s = start(p, NSB2);
            double x = -163.0 + (i % 5) * 3.0, z = 468.0 + (i / 5) * 11.0;
            h.runAfterDelay(WARMUP + 410, safe(h, id, () -> {
                need(s.getFirePhase() == SimulationSession.FirePhase.EVACUATION, "phase " + s.getFirePhase());
                tp(p, x, -33, z);
            }));
            h.runAfterDelay(WARMUP + 418, safe(h, id, () -> verdict(h, id, () -> {
                need(SimulationManager.getSession(p.getUUID()) == null, "run still active inside the assembly zone at (" + x + ", " + z + ")");
                need("assembly_reached".equals(endReason(s)), "end_reason " + endReason(s));
                need(!events(s, "assembly_area_reached").isEmpty(), "no assembly_area_reached event");
                need(s.hasReachedAssembly(), "session not marked assembly reached");
                int exp = score(s, false, true, "assembly_reached");
                need(endScore(s) == exp, "score " + endScore(s) + ", documented " + exp);
                return "Entered the assembly zone at (" + x + ", " + z + "): assembly_area_reached logged, end_reason assembly_reached, score " + endScore(s) + " (includes the +15).";
            })));
        });

        // ───────── FS-11 failure to assemble ends by time limit ─────────
        each("FS-11", 1500, (h, i) -> {
            String id = cid("FS-11", i);
            RecordingPlayer p = join(h, "fs11_" + i);
            SimulationSession s = start(p, NSB2);
            List<BlockPos> armed = new ArrayList<>(s.getArmedHazards());
            int k = Math.min(armed.size() - 1, i % 4);
            for (int j = 0; j < k; j++) rightClickBlock(p, armed.get((j + i) % armed.size()));
            boolean alarm = i % 2 == 1;
            BlockPos base = platform(h), al = base.east(2);
            if (alarm) { h.getLevel().setBlock(al, ModBlocks.FIRE_ALARM_BLOCK.get().defaultBlockState(), 3); h.runAfterDelay(WARMUP + 20, safe(h, id, () -> rightClickBlock(p, al))); }
            h.succeedWhen(() -> {
                h.assertTrue(SimulationManager.getSession(p.getUUID()) == null, "still running");
                try {
                    need("timeout".equals(endReason(s)), "end_reason " + endReason(s));
                    need(!s.hasReachedAssembly(), "assembly marked reached without going there");
                    int exp = score(s, alarm, false, "timeout");
                    need(endScore(s) == exp, "score " + endScore(s) + ", documented " + exp);
                    need(Boolean.FALSE.equals(last(s, "SIM_END").orElseThrow().data().get("passed")), "a timed-out run was marked passed");
                    record(id, "PASS", k + " hazards found" + (alarm ? ", alarm rung" : "") + ", never reached the assembly area: run ended timeout, passed=false, score " + endScore(s) + " with no assembly bonus.");
                } catch (Throwable t) { record(id, "FAIL", message(t)); }
            });
        });

        // ───────── RB-04 correct extinguisher sequence: pin first, then spray ─────────
        each("RB-04", 300, (h, i) -> {
            String id = cid("RB-04", i);
            RecordingPlayer p = join(h, "rb04_" + i);
            SimulationSession s = start(p, SimulationState.FIRE);
            BlockPos base = platform(h);
            int n = 1 + (i % 3);
            List<BlockPos> fires = new ArrayList<>();
            for (int j = 0; j < n; j++) fires.add(base.east(3).north(j - (n - 1) / 2));
            for (BlockPos f : fires) h.getLevel().setBlock(f, Blocks.FIRE.defaultBlockState(), 3);
            faceFrom(p, fires.get(0), 2.0 + (i % 5) * 0.4);
            arm(p, ModItems.FIRE_EXTINGUISHER.get());
            spray(p, 6 + (i % 10));
            h.runAfterDelay(4, safe(h, id, () -> verdict(h, id, () -> {
                List<String> order = s.logger.getEvents().stream().map(SimEvent::type).filter(t -> t.equals("pin_pull") || t.equals("ext_spray")).toList();
                need(order.indexOf("pin_pull") >= 0 && order.indexOf("ext_spray") > order.indexOf("pin_pull"), "event order " + order);
                long out = fires.stream().filter(f -> h.getLevel().getBlockState(f).isAir()).count();
                need(out >= 1, "no fire put out after pin then spray");
                need(s.getFiresExtinguished() >= out, "session counted " + s.getFiresExtinguished() + ", " + out + " fire blocks gone");
                int counted = s.getFiresExtinguished();
                SimulationManager.endSimulation(p.getUUID(), "assembly_reached");
                need(endScore(s) == Math.min(100, counted * 2), "score " + endScore(s) + ", documented " + Math.min(100, counted * 2));
                return "Pin then spray: " + out + "/" + n + " fires out, " + counted + " counted, score " + endScore(s) + " (2 per fire).";
            })));
        });

        // ───────── RB-05 spray without the required preparation (pin still in) ─────────
        each("RB-05", 300, (h, i) -> {
            String id = cid("RB-05", i);
            RecordingPlayer p = join(h, "rb05_" + i);
            SimulationSession s = start(p, SimulationState.FIRE);
            BlockPos base = platform(h);
            BlockPos fire = base.east(3);
            h.getLevel().setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);
            faceFrom(p, fire, 2.0 + (i % 5) * 0.4);
            Item item = ext(i);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item));
            spray(p, 3 + (i % 10));   // held spray with the pin still in: the unit must refuse
            ItemStack st = p.getMainHandItem();
            h.runAfterDelay(3, safe(h, id, () -> {
                boolean blocked = h.getLevel().getBlockState(fire).is(Blocks.FIRE) && events(s, "ext_spray").isEmpty() && events(s, "EXT_SPRAY").isEmpty() && st.getDamageValue() == 0;
                item.use(p.level(), p, net.minecraft.world.InteractionHand.MAIN_HAND);   // the click a student would actually make first
                verdict(h, id, () -> {
                    need(blocked, "an unpinned extinguisher discharged: fire " + h.getLevel().getBlockState(fire) + ", sprays " + events(s, "ext_spray").size() + ", durability " + st.getDamageValue());
                    need(s.getFiresExtinguished() == 0, "an unpinned spray scored a fire");
                    need(p.heard("PIN PULLED"), "no learner-facing message on the first click: " + p.messages);
                    need(events(s, "pin_pull").size() == 1, "pin_pull events " + events(s, "pin_pull").size());
                    return extName(i) + " unit with the pin in: held spray did nothing (fire still burning, no spray event, no durability used, no score); the first click answered \"PIN PULLED\" and logged one pin_pull.";
                });
            }));
        });

        // ───────── BL-04 hazard evidence: move_tick hazard_distance follows a fire's distance ─────────
        each("BL-04", 300, (h, i) -> {
            String id = cid("BL-04", i);
            RecordingPlayer p = join(h, "bl04_" + i);
            SimulationSession s = start(p, SimulationState.FIRE);
            BlockPos base = platform(h);
            int dx = 1 + (i % 6), dz = (i / 6) - 1 + (i % 3) - 1;   // 20 distinct offsets around the player
            dz = Math.max(-3, Math.min(3, dz));
            BlockPos fire = base.offset(dx, 0, dz);
            final int fdz = dz;
            tp(p, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            h.runAfterDelay(WARMUP + 5, safe(h, id, () -> h.getLevel().setBlock(fire, Blocks.FIRE.defaultBlockState(), 3)));
            h.runAfterDelay(WARMUP + 25, safe(h, id, () -> verdict(h, id, () -> {
                String lastRow = s.buildMoveCsv().lines().filter(l -> l.contains(",move_tick,")).reduce((a, b) -> b).orElse("");
                need(!lastRow.isEmpty(), "no move_tick row");
                double d = Double.parseDouble(lastRow.split(",", -1)[8]);
                double expect = Math.sqrt(dx * dx + fdz * fdz);
                need(Math.abs(d - expect) <= 1.0, "hazard_distance " + d + " with a fire " + String.format("%.2f", expect) + " blocks away (offset " + dx + "," + fdz + ")");
                return "Fire " + String.format("%.2f", expect) + " blocks away (offset " + dx + ", " + fdz + "): move_tick hazard_distance = " + d + ".";
            })));
        });
    }
}
