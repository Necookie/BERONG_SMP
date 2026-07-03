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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room 2 — Sgt. Reyes's Fire Safety Drill. Gated on Room 1 ({@link CruzPhase#DONE}).
 *
 * <p>The 3 hazard props aren't part of the schematic — {@link #placeAndIgniteHazards} places them
 * itself once the player has all 3 extinguishers, at placeholder positions inside Room 2's box
 * (see {@code docs/f3_tuning_todo.md}), then forces each straight to its failure/burning state via
 * {@link HazardManager#forceFailure} — the same deterministic, session-nullable entry point
 * {@code HazardWandItem} already uses for manual testing, so no randomness is involved.
 *
 * <p>Correct-tool attribution is done by watching each position's active-hazard flag for a
 * true→false transition while this player is nearby holding an item — not by any new signal from
 * {@code HazardManager}/the extinguisher items themselves, since today only the 5 kitchen props
 * get an automatic "wrong tool" warning (the other two hazard classes silently succeed with either
 * extinguisher). This keeps the scoring accurate without touching that shared system.
 */
public final class ReyesRoomManager {

    private static final Map<UUID, Integer> dialogueSteps = new ConcurrentHashMap<>();

    /** PLACEHOLDER positions inside Room 2's box; see docs/f3_tuning_todo.md. */
    private static final BlockPos CLASS_A_POS = new BlockPos(-165, -33, 20);      // ArchiveBoxStack -> ABC
    private static final BlockPos ELECTRICAL_POS = new BlockPos(-169, -33, 16);  // ComputerBlock -> CO2
    private static final BlockPos KITCHEN_POS = new BlockPos(-172, -33, 12);     // UnattendedGreasePan -> wet chemical

    private static final double NEAR_RANGE_SQ = 6.0 * 6.0;
    private static final int IDLE_NUDGE_INTERVAL_TICKS = 100;

    /** World-shared "was this position an active hazard last tick" snapshot (not per-player — see class doc). */
    private static final Map<BlockPos, Boolean> lastActive = new ConcurrentHashMap<>();
    /** Per-player countdown for the scripted ignite-demo window; absent when not currently on fire from it. */
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
        boolean advance = AcademyManager.stepDialogue(player, dialogueSteps, lines);
        if (!advance) return;

        ReyesPhase next = switch (phase) {
            case NOT_STARTED -> ReyesPhase.TOOL_SELECTION;
            default -> phase; // condition-gated, not dialogue-gated
        };
        if (next != phase) {
            data.mutate(player.getUUID(), p -> p.setReyesPhase(next));
            AcademyManager.resetDialogueStep(dialogueSteps, player);
        }
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
    private static final int WAYPOINT_INTERVAL_TICKS = 10;

    private static void tickDone(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (data.get(player.getUUID()).santosPhase() != SantosPhase.NOT_STARTED) return;
        if (level.getGameTime() % WAYPOINT_INTERVAL_TICKS == 0) {
            AcademyVisuals.spawnWaypointArrow(level, player.position(), SANTOS_ANCHOR);
        }
    }

    private static void tickToolSelection(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (!hasAllExtinguishers(player)) {
            if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0) {
                AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §7Go ahead, take them off the wall! "
                        + "Walk up and click on each one.");
            }
            return;
        }
        data.mutate(player.getUUID(), p -> p.setReyesPhase(ReyesPhase.LIVE_FIRE_DEMO));
        AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §fHere we go — three real hazards, right in front of "
                + "you! Remember: PULL the pin, AIM low, SQUEEZE the handle, SWEEP side to side. "
                + "Match the right extinguisher to the right fire!");
        placeAndIgniteHazards(level);
    }

    private static boolean hasAllExtinguishers(ServerPlayer player) {
        var inventory = player.getInventory();
        return inventory.contains(stack -> stack.is(BerongSMP.FIRE_EXTINGUISHER.get()))
                && inventory.contains(stack -> stack.is(BerongSMP.CO2_EXTINGUISHER.get()))
                && inventory.contains(stack -> stack.is(BerongSMP.WET_CHEMICAL_EXTINGUISHER.get()));
    }

    private static void placeAndIgniteHazards(ServerLevel level) {
        level.setBlock(CLASS_A_POS, BerongSMP.ARCHIVE_BOX_STACK.get().defaultBlockState(), 3);
        level.setBlock(ELECTRICAL_POS, BerongSMP.COMPUTER.get().defaultBlockState(), 3);
        level.setBlock(KITCHEN_POS, BerongSMP.UNATTENDED_GREASE_PAN.get().defaultBlockState(), 3);

        HazardManager.forceFailure(level, null, CLASS_A_POS, null);
        HazardManager.forceFailure(level, null, ELECTRICAL_POS, null);
        HazardManager.forceFailure(level, null, KITCHEN_POS, null);

        lastActive.put(CLASS_A_POS, true);
        lastActive.put(ELECTRICAL_POS, true);
        lastActive.put(KITCHEN_POS, true);
    }

    private static void tickLiveFireDemo(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        checkDefuse(player, data, CLASS_A_POS, FireExtinguisherItem.class);
        checkDefuse(player, data, ELECTRICAL_POS, CO2ExtinguisherItem.class);
        boolean kitchenJustDefused = checkDefuse(player, data, KITCHEN_POS, WetChemicalExtinguisherItem.class);

        if (kitchenJustDefused && !igniteWindow.containsKey(id)) {
            int ticks = Config.ACADEMY_IGNITE_DEMO_TICKS.get();
            player.setRemainingFireTicks(ticks);
            igniteWindow.put(id, ticks);
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §cWatch out — you caught a spark! Hit Shift, then "
                    + "press R to Drop and Roll it out — just like we practice!");
        }

        Integer remaining = igniteWindow.get(id);
        if (remaining != null) {
            if (DropAndRollManager.isDropped(id)) {
                data.mutate(id, p -> p.setDropAndRollPerformed(true));
                igniteWindow.remove(id);
                AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §aThat's it — smothered! You just put "
                        + "yourself out safely. Great reflexes!");
            } else if (remaining - 1 <= 0) {
                igniteWindow.remove(id);
            } else {
                igniteWindow.put(id, remaining - 1);
            }
        }

        boolean allHandled = !isActive(level, CLASS_A_POS) && !isActive(level, ELECTRICAL_POS) && !isActive(level, KITCHEN_POS);
        if (allHandled && !igniteWindow.containsKey(id)) {
            data.mutate(id, p -> p.setReyesPhase(ReyesPhase.DONE));
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §fYou've mastered all three fire classes and how "
                    + "to handle catching fire yourself. Head over to Sgt. Santos for the earthquake drill — "
                    + "you're doing amazing!");
        }
    }

    /**
     * Returns true only on the exact tick {@code pos} transitions from active-hazard to safe while
     * this player is nearby — awards/penalizes scoring based on whether they were holding
     * {@code correctTool} at that moment.
     */
    private static boolean checkDefuse(ServerPlayer player, AcademySavedData data, BlockPos pos,
                                        Class<? extends AbstractExtinguisherItem> correctTool) {
        ServerLevel level = (ServerLevel) player.level();
        boolean activeNow = isActive(level, pos);
        boolean wasActive = lastActive.getOrDefault(pos, false);
        lastActive.put(pos, activeNow);

        if (!wasActive || activeNow) return false;
        if (player.position().distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > NEAR_RANGE_SQ) return false;

        boolean usedCorrectTool = correctTool.isInstance(player.getMainHandItem().getItem());
        data.mutate(player.getUUID(), p -> {
            if (usedCorrectTool) p.addFireCorrectUse(); else p.addFireWrongUse();
        });
        AcademyManager.sendPrompt(player, usedCorrectTool
                ? "§6[Sgt. Reyes] §aPerfect! That's exactly the right tool for that fire."
                : "§6[Sgt. Reyes] §cHold on — that's not the right extinguisher for this one! "
                  + "Think about what's burning before you spray.");
        return true;
    }

    private static boolean isActive(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ComputerBlock) {
            return state.hasProperty(ComputerBlock.BURNING) && state.getValue(ComputerBlock.BURNING);
        }
        return HazardManager.isHazardous(state);
    }
}
