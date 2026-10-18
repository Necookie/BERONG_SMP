package net.necookie.disastersim.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.common.simulation.SimulationEventLogger.SimEvent;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.item.AbstractExtinguisherItem;
import net.necookie.disastersim.session.TursoClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Shared helpers for the in-world system tests. */
final class GT {

    private GT() {}

    /** Ends every running simulation and removes leftover mock players so each test starts clean. */
    static void reset(GameTestHelper h) {
        for (UUID id : new ArrayList<>(SimulationManager.getActiveSessions().keySet())) {
            SimulationManager.endSimulation(id, "test_reset");
        }
        for (ServerPlayer p : new ArrayList<>(h.getLevel().getServer().getPlayerList().getPlayers())) {
            if (p instanceof RecordingPlayer rp) rp.leave();
        }
    }

    /** Sequential steps on top of runAfterDelay: each {@code at} delay is relative to the previous step. */
    static final class Seq {
        private final GameTestHelper h;
        private long t;
        Seq(GameTestHelper h) { this.h = h; }
        Seq at(long dt, Runnable r) { t += dt; h.runAfterDelay(t, r); return this; }
        /** Polls {@code cond} every tick from now on; runs {@code then} once it holds, fails after {@code timeout} ticks. */
        void end() { t += 1; h.runAfterDelay(t, h::succeed); }
    }

    static Seq seq(GameTestHelper h) { return new Seq(h); }

    /**
     * Steps that wait on conditions rather than fixed delays. The game-test server ticks as fast as
     * it can, so work that finishes on other threads in wall-clock time (password hashing, Turso
     * HTTP calls) cannot be waited for with a tick count. Polls once per tick via onEachTick,
     * which must be registered when the test starts, not from inside a scheduled step.
     */
    static final class Flow {
        private record Step(int minTicks, java.util.function.BooleanSupplier ready, Runnable action) {}
        private final GameTestHelper h;
        private final List<Step> steps = new ArrayList<>();
        private int index;
        private int ticksInStep;

        Flow(GameTestHelper h) { this.h = h; }

        Flow after(int ticks, Runnable action) { steps.add(new Step(ticks, () -> true, action)); return this; }

        /** After {@code minTicks}, waits until {@code ready} holds, then runs {@code action}. */
        Flow when(int minTicks, java.util.function.BooleanSupplier ready, Runnable action) {
            steps.add(new Step(minTicks, ready, action));
            return this;
        }

        void start() {
            h.onEachTick(() -> {
                if (index >= steps.size()) return;
                Step s = steps.get(index);
                if (++ticksInStep < s.minTicks() || !s.ready().getAsBoolean()) return;
                index++;
                ticksInStep = 0;
                s.action().run();   // the last step ends the test (succeed/finish)
            });
        }
    }

    static Flow flow(GameTestHelper h) { return new Flow(h); }

    /** Condition that only evaluates every {@code everyTicks} calls (for checks that hit the database). */
    static java.util.function.BooleanSupplier every(int everyTicks, java.util.function.BooleanSupplier check) {
        int[] n = {0};
        return () -> ++n[0] % everyTicks == 0 && check.getAsBoolean();
    }

    static void cmd(ServerPlayer p, String command, boolean operator) {
        CommandSourceStack src = p.createCommandSourceStack();
        if (operator) src = src.withPermission(LevelBasedPermissionSet.OWNER);
        ((ServerLevel) p.level()).getServer().getCommands().performPrefixedCommand(src, command);
    }

    static List<SimEvent> events(SimulationSession s, String type) {
        return s.logger.getEvents().stream().filter(e -> e.type().equals(type)).toList();
    }

    static Optional<SimEvent> last(SimulationSession s, String type) {
        List<SimEvent> list = events(s, type);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    static String uniqueName(String prefix) {
        return prefix + Long.toString(System.nanoTime() % 2_176_782_336L, 36);
    }

    static JsonArray query(String sql, Object... args) {
        return TursoClient.parseRows(TursoClient.query(sql, args));
    }

    static String str(JsonObject row, String col) {
        JsonElement e = row.get(col);
        return e == null || e.isJsonNull() || e.isJsonObject() ? null : e.getAsString();
    }

    /** Flat stone platform at the test origin; returns the block position players stand on top of. */
    static BlockPos platform(GameTestHelper h) {
        BlockPos base = h.absolutePos(new BlockPos(0, 1, 0));
        ServerLevel level = h.getLevel();
        for (int dx = -4; dx <= 6; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(base.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
                for (int dy = 0; dy <= 3; dy++) level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        return base;
    }

    static void tp(ServerPlayer p, double x, double y, double z) {
        p.teleportTo((ServerLevel) p.level(), x, y, z, Collections.emptySet(), p.getYRot(), p.getXRot(), true);
    }

    /** Stands the player {@code dist} blocks west of {@code target} (same floor) facing it. */
    static void faceFrom(ServerPlayer p, BlockPos target, double dist) {
        tp(p, target.getX() + 0.5 - dist, target.getY(), target.getZ() + 0.5);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target));
    }

    /** Puts a fresh extinguisher in hand and pulls its pin (the first right-click). */
    static ItemStack arm(ServerPlayer p, Item extinguisher) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(extinguisher));
        extinguisher.use(p.level(), p, InteractionHand.MAIN_HAND);
        return p.getMainHandItem();
    }

    /** Holds right-click (spray) for {@code ticks} server ticks with whatever is in the main hand. */
    static void spray(ServerPlayer p, int ticks) {
        ItemStack stack = p.getMainHandItem();
        AbstractExtinguisherItem item = (AbstractExtinguisherItem) stack.getItem();
        for (int i = 0; i < ticks; i++) item.onUseTick(p.level(), p, p.getMainHandItem(), 72_000 - i);
    }

    static void rightClickBlock(ServerPlayer p, BlockPos pos) {
        ServerLevel level = (ServerLevel) p.level();
        level.getBlockState(pos).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    /** Records a passing case and its observed result in run-gametest/gt_actuals.jsonl (merged into results later). */
    static void actual(String caseId, String text) { record(caseId, "PASS", text); }

    static void record(String caseId, String status, String text) {
        JsonObject o = new JsonObject();
        o.addProperty("case_id", caseId);
        o.addProperty("status", status);
        o.addProperty("actual", text);
        try {
            Files.writeString(Path.of("gt_actuals.jsonl"), o + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    /**
     * Runs one case's checks inside a test that covers several cases; a failed check is recorded as
     * that case's FAIL instead of aborting the remaining cases. Returns false if it failed.
     */
    static boolean caseCheck(String caseId, Runnable checks) {
        try {
            checks.run();
            return true;
        } catch (Throwable t) {
            record(caseId, "FAIL", t.getMessage() == null ? t.toString() : t.getMessage());
            return false;
        }
    }

    /** Ends a multi-case test: passes only if every case passed, so the report still flags the scenario. */
    static void finish(GameTestHelper h, boolean... results) {
        int failed = 0;
        for (boolean r : results) if (!r) failed++;
        if (failed == 0) h.succeed(); else h.fail(failed + " case(s) failed; see gt_actuals.jsonl");
    }
}
