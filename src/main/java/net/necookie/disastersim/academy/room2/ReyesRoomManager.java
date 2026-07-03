package net.necookie.disastersim.academy.room2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.academy.AcademyDialogue;
import net.necookie.disastersim.academy.AcademyManager;
import net.necookie.disastersim.academy.AcademyProgress;
import net.necookie.disastersim.academy.AcademySavedData;
import net.necookie.disastersim.academy.AcademyVisuals;
import net.necookie.disastersim.academy.CruzPhase;
import net.necookie.disastersim.academy.ReyesPhase;
import net.necookie.disastersim.academy.SantosPhase;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.item.AbstractExtinguisherItem;
import net.necookie.disastersim.item.CO2ExtinguisherItem;
import net.necookie.disastersim.item.FireExtinguisherItem;
import net.necookie.disastersim.item.WetChemicalExtinguisherItem;
import net.necookie.disastersim.player.DropAndRollManager;
import net.necookie.disastersim.world.HazardManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Room 2 — Sgt. Reyes's Fire Safety Drill. Gated on Room 1 ({@link CruzPhase#DONE}).
 *
 * <p>Teaches one hazard at a time, in a fixed order (Class A → electrical → kitchen): Reyes
 * explains what's on fire and why that specific extinguisher before it ignites (the explanation's
 * completion is what triggers {@link #igniteHazard}, not the player clicking again), then the
 * player must defuse it with the *correct* tool before moving on — a wrong-tool defuse
 * re-ignites the same hazard via {@link HazardManager#forceFailure} (the same deterministic,
 * session-nullable entry point {@code HazardWandItem} uses for manual testing) rather than just a
 * warning, so getting it right is actually required, without touching the shared
 * {@code HazardManager}/extinguisher-item code (those still let any tool mechanically succeed on
 * non-kitchen props — this room's own logic is what enforces "the right one" here).
 *
 * <p>Once all 3 are correctly handled, Reyes's scripted "you caught fire" demo ignites the player;
 * the fire is kept topped up every tick (never left to burn down on its own) until
 * {@link DropAndRollManager#isDropped} is observed true, so the player actually has to drop and
 * roll rather than the fire just expiring on a timer — {@code Config.ACADEMY_IGNITE_DEMO_TICKS} is
 * a safety-cap timeout, not the real duration.
 */
public final class ReyesRoomManager {

    /** PLACEHOLDER positions inside Room 2's box; see docs/f3_tuning_todo.md. */
    private static final BlockPos CLASS_A_POS = new BlockPos(-165, -33, 20);
    private static final BlockPos ELECTRICAL_POS = new BlockPos(-169, -33, 16);
    private static final BlockPos KITCHEN_POS = new BlockPos(-172, -33, 12);

    private record HazardStep(BlockPos pos, Supplier<BlockState> blockState,
                               Class<? extends AbstractExtinguisherItem> correctTool) {}

    /** Fixed teaching order: Class A -> electrical -> kitchen. */
    private static final List<HazardStep> HAZARDS = List.of(
            new HazardStep(CLASS_A_POS, () -> BerongSMP.ARCHIVE_BOX_STACK.get().defaultBlockState(), FireExtinguisherItem.class),
            new HazardStep(ELECTRICAL_POS, () -> BerongSMP.COMPUTER.get().defaultBlockState(), CO2ExtinguisherItem.class),
            new HazardStep(KITCHEN_POS, () -> BerongSMP.UNATTENDED_GREASE_PAN.get().defaultBlockState(), WetChemicalExtinguisherItem.class)
    );

    private static final double NEAR_RANGE_SQ = 6.0 * 6.0;
    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100;
    private static final int FIRE_REFRESH_TICKS = 20; // 1s buffer kept topped up until they roll

    /** World-shared "was this position an active hazard last tick" snapshot (not per-player — see class doc). */
    private static final Map<BlockPos, Boolean> lastActive = new ConcurrentHashMap<>();
    /** Which hazard index (0-2) each player is currently working on; absent until LIVE_FIRE_DEMO starts. */
    private static final Map<UUID, Integer> currentHazard = new ConcurrentHashMap<>();
    /** The hazard index whose explanation dialogue has already been kicked off, so it's only started once. */
    private static final Map<UUID, Integer> explainedHazard = new ConcurrentHashMap<>();
    /** Per-player safety-cap countdown for the scripted ignite demo; absent when not currently active. */
    private static final Map<UUID, Integer> igniteWindow = new ConcurrentHashMap<>();

    private ReyesRoomManager() {}

    public static void onInteract(ServerPlayer player, CustomNpcEntity npc) {
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        AcademyProgress progress = data.get(player.getUUID());

        if (progress.cruzPhase() != CruzPhase.DONE) {
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §7Finish up with Officer Cruz first — come back once you're done!");
            return;
        }

        ReyesPhase phase = progress.reyesPhase();
        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.REYES_LINES.get(phase);
        AcademyManager.startOrAdvanceDialogue(player, lines, () -> {
            ReyesPhase next = switch (phase) {
                case NOT_STARTED -> ReyesPhase.TOOL_SELECTION;
                default -> phase; // condition-gated, not dialogue-gated
            };
            if (next != phase) {
                data.mutate(player.getUUID(), p -> p.setReyesPhase(next));
            }
        });
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            AcademyProgress progress = data.get(player.getUUID());
            if (progress.cruzPhase() != CruzPhase.DONE) continue;
            switch (progress.reyesPhase()) {
                case TOOL_SELECTION -> tickToolSelection(level, player, data);
                case LIVE_FIRE_DEMO -> tickLiveFireDemo(level, player, data);
                case DONE -> tickDone(level, player, data);
                default -> { }
            }
        }
    }

    /** Sgt. Santos's anchor — points there until the player has actually started Room 3. */
    private static final Vec3 SANTOS_ANCHOR = new Vec3(-170.5, -33.0, 33.5);

    private static void tickDone(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (data.get(player.getUUID()).santosPhase() != SantosPhase.NOT_STARTED) return;
        AcademyVisuals.spawnCompassArrow(level, player, SANTOS_ANCHOR);
    }

    private static void tickToolSelection(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (!hasAllExtinguishers(player)) {
            if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
                AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §7Go ahead, take them off the wall! "
                        + "Walk up and click on each one.");
            }
            return;
        }
        UUID id = player.getUUID();
        data.mutate(id, p -> p.setReyesPhase(ReyesPhase.LIVE_FIRE_DEMO));
        currentHazard.put(id, 0);
        explainedHazard.remove(id);
    }

    private static boolean hasAllExtinguishers(ServerPlayer player) {
        var inventory = player.getInventory();
        return inventory.contains(stack -> stack.is(BerongSMP.FIRE_EXTINGUISHER.get()))
                && inventory.contains(stack -> stack.is(BerongSMP.CO2_EXTINGUISHER.get()))
                && inventory.contains(stack -> stack.is(BerongSMP.WET_CHEMICAL_EXTINGUISHER.get()));
    }

    private static void tickLiveFireDemo(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        int idx = currentHazard.getOrDefault(id, 0);

        if (idx >= HAZARDS.size()) {
            tickIgniteDemo(player, data);
            return;
        }

        if (!Objects.equals(explainedHazard.get(id), idx)) {
            explainedHazard.put(id, idx);
            HazardStep hazard = HAZARDS.get(idx);
            AcademyManager.startOrAdvanceDialogue(player, AcademyDialogue.REYES_HAZARD_LINES.get(idx),
                    () -> igniteHazard(level, hazard));
            return; // wait for the explanation to finish before this hazard even ignites
        }

        if (checkAndHandleDefuse(player, data, HAZARDS.get(idx))) {
            currentHazard.put(id, idx + 1);
        }
    }

    private static void igniteHazard(ServerLevel level, HazardStep hazard) {
        level.setBlock(hazard.pos(), hazard.blockState().get(), 3);
        HazardManager.forceFailure(level, null, hazard.pos(), null);
        lastActive.put(hazard.pos(), true);
    }

    /**
     * Returns {@code true} only when this hazard was just correctly defused (advances the
     * sequence). A wrong-tool defuse re-ignites the same hazard instead of advancing.
     */
    private static boolean checkAndHandleDefuse(ServerPlayer player, AcademySavedData data, HazardStep hazard) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = hazard.pos();
        boolean activeNow = isActive(level, pos);
        boolean wasActive = lastActive.getOrDefault(pos, false);
        lastActive.put(pos, activeNow);

        if (!wasActive || activeNow) return false;
        if (player.position().distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > NEAR_RANGE_SQ) return false;

        boolean usedCorrectTool = hazard.correctTool().isInstance(player.getMainHandItem().getItem());
        if (usedCorrectTool) {
            data.mutate(player.getUUID(), AcademyProgress::addFireCorrectUse);
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §aPerfect! That's exactly the right tool for that fire.");
            return true;
        }

        data.mutate(player.getUUID(), AcademyProgress::addFireWrongUse);
        AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §cThat's the wrong extinguisher — the fire flares "
                + "right back up! Think about what's burning and try again.");
        igniteHazard(level, hazard);
        return false;
    }

    private static boolean isActive(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ComputerBlock) {
            return state.hasProperty(ComputerBlock.BURNING) && state.getValue(ComputerBlock.BURNING);
        }
        return HazardManager.isHazardous(state);
    }

    /**
     * The scripted "you caught fire" demo. The fire is re-topped-up every tick (never left to
     * naturally count down to 0) until {@link DropAndRollManager#isDropped} is observed true, so
     * the lesson always requires an actual drop-and-roll instead of the fire just expiring on a
     * timer; {@code Config.ACADEMY_IGNITE_DEMO_TICKS} is only a safety-cap fallback in case the
     * player never rolls at all.
     */
    private static void tickIgniteDemo(ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        Integer remaining = igniteWindow.get(id);

        if (remaining == null) {
            int cap = Config.ACADEMY_IGNITE_DEMO_TICKS.get();
            igniteWindow.put(id, cap);
            player.setRemainingFireTicks(cap);
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §cWatch out — you caught a spark! Hit Shift, then "
                    + "press R to Drop and Roll it out — just like we practice!");
            return;
        }

        if (DropAndRollManager.isDropped(id)) {
            player.clearFire();
            data.mutate(id, p -> p.setDropAndRollPerformed(true));
            igniteWindow.remove(id);
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §aThat's it — smothered! You just put "
                    + "yourself out safely. Great reflexes!");
            finishRoom(player, data);
            return;
        }

        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), FIRE_REFRESH_TICKS));

        if (remaining - 1 <= 0) {
            // Safety-cap timeout — they never rolled. Clear it and move on anyway (forgiving).
            player.clearFire();
            igniteWindow.remove(id);
            finishRoom(player, data);
        } else {
            igniteWindow.put(id, remaining - 1);
        }
    }

    private static void finishRoom(ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        data.mutate(id, p -> p.setReyesPhase(ReyesPhase.DONE));
        currentHazard.remove(id);
        explainedHazard.remove(id);
        AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §fYou've mastered all three fire classes and how to "
                + "handle catching fire yourself. Head over to Sgt. Santos for the earthquake drill — "
                + "you're doing amazing!");
    }

    /** Called from {@code AcademyManager}'s logout handler to drop this room's per-player state. */
    public static void clearPlayer(UUID id) {
        igniteWindow.remove(id);
        currentHazard.remove(id);
        explainedHazard.remove(id);
    }
}
