package net.necookie.disastersim.academy.room2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import net.necookie.disastersim.block.FireAlarmBlock;
import net.minecraft.sounds.SoundSource;
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
    private static final int HIGHLIGHT_INTERVAL_TICKS = 5; // matches Cruz's/Santos's own cadence

    /**
     * World position of the Academy's fire alarm, taught during {@code ALARM_CHECKPOINT}. The
     * schematic already bakes one in here (verified by parsing {@code new_tut_building1.0.schem}'s
     * raw block data: {@code berongsmp:fire_alarm[activated=false,facing=south]} at this exact
     * spot) — {@code NewTutBuildingManager} used to also place a second one one block below this,
     * which is what the "two fire alarms stacked on top of each other" report was.
     */
    public static final BlockPos ALARM_POS = new BlockPos(-143, -32, 40);
    private static final Vec3 REYES_ANCHOR = new Vec3(-172.5, -33.0, 17.5);
    /** True once a player has pressed the alarm and is expected back at Reyes; cleared on return. */
    private static final Map<UUID, Boolean> alarmRinging = new ConcurrentHashMap<>();

    /**
     * Per-player "was this position an active hazard last tick" snapshot, keyed by player UUID
     * first so two concurrent Room-2 players don't corrupt each other's hazardous→safe edge
     * detection (the hazard positions themselves are fixed world coordinates shared by everyone).
     */
    private static final Map<UUID, Map<BlockPos, Boolean>> lastActive = new ConcurrentHashMap<>();
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
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §7Hi trainee! Finish Officer Cruz's movement "
                    + "lessons first — follow the glowing arrow back to her, then come see me!");
            return;
        }

        ReyesPhase phase = progress.reyesPhase();
        UUID id = player.getUUID();

        // Returning to Reyes after the alarm is already ringing is a special transition, not a
        // plain re-click of the current phase's dialogue: it stops the alarm and moves straight
        // into the evacuation briefing.
        if (phase == ReyesPhase.ALARM_CHECKPOINT && Boolean.TRUE.equals(alarmRinging.get(id))) {
            alarmRinging.remove(id);
            stopAlarm(level);
            data.mutate(id, p -> p.setReyesPhase(ReyesPhase.EVACUATION_BRIEF));
            AcademyManager.forceStartDialogue(player, AcademyDialogue.REYES_LINES.get(ReyesPhase.EVACUATION_BRIEF),
                    () -> finishRoom(player, data));
            return;
        }

        List<AcademyDialogue.DialogueLine> lines = AcademyDialogue.REYES_LINES.get(phase);
        AcademyManager.startOrAdvanceDialogue(player, lines, () -> {
            ReyesPhase next = switch (phase) {
                case NOT_STARTED -> ReyesPhase.PREVENTION_DEMO;
                default -> phase; // condition-/tick-gated, or handled by the branch above
            };
            if (next != phase) {
                data.mutate(id, p -> p.setReyesPhase(next));
            }
        });
    }

    public static void tick(ServerLevel level) {
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            AcademyProgress progress = data.get(player.getUUID());
            if (progress.cruzPhase() != CruzPhase.DONE) continue;
            switch (progress.reyesPhase()) {
                case PREVENTION_DEMO -> tickPreventionDemo(level, player, data);
                case TOOL_SELECTION -> tickToolSelection(level, player, data);
                case LIVE_FIRE_DEMO -> tickLiveFireDemo(level, player, data);
                case ALARM_CHECKPOINT -> tickAlarmCheckpoint(level, player);
                case DONE -> tickDone(level, player, data);
                default -> { }
            }
        }
    }

    /**
     * Bare-hand right-click "prevention" drill on the same 3 hazard props LIVE_FIRE_DEMO later
     * ignites for real — here they're merely set hazardous (never actually catch fire) and the
     * player fixes each one with {@link net.necookie.disastersim.block.hazard.HazardBlock}'s
     * prevention interaction before moving to the next.
     */
    private static void tickPreventionDemo(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        int idx = currentHazard.getOrDefault(id, 0);

        if (idx >= HAZARDS.size()) {
            currentHazard.remove(id);
            explainedHazard.remove(id);
            lastActive.remove(id);
            AcademyVisuals.setCompassTarget(player, null);
            data.mutate(id, p -> p.setReyesPhase(ReyesPhase.TOOL_SELECTION));
            return;
        }

        HazardStep hazard = HAZARDS.get(idx);
        pointAtHazard(level, player, hazard);

        if (!Objects.equals(explainedHazard.get(id), idx)) {
            explainedHazard.put(id, idx);
            // Arm the prop FIRST, synchronously, then narrate it — the prop must already be
            // visible and hazardous the instant Reyes starts describing it, not several seconds
            // later once the explanation happens to finish. onComplete is a no-op here since
            // checkAndHandlePrevention already runs every tick once explainedHazard is marked,
            // independent of whether the dialogue itself has finished playing.
            armPreventionHazard(level, id, hazard);
            AcademyManager.forceStartDialogue(player, AcademyDialogue.REYES_PREVENTION_LINES.get(idx), () -> {});
            return;
        }

        if (checkAndHandlePrevention(player, hazard)) {
            currentHazard.put(id, idx + 1);
        }
    }

    private static void armPreventionHazard(ServerLevel level, UUID playerId, HazardStep hazard) {
        level.setBlock(hazard.pos(), hazard.blockState().get(), 3);
        HazardManager.activate(level, null, hazard.pos());
        lastActive.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(hazard.pos(), true);
    }

    /** Beacon + compass pointed at a hazard's position, throttled to the shared highlight cadence. */
    private static void pointAtHazard(ServerLevel level, ServerPlayer player, HazardStep hazard) {
        if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
            AcademyVisuals.highlightBlocks(level, List.of(hazard.pos()));
        }
        BlockPos pos = hazard.pos();
        AcademyVisuals.setCompassTarget(player, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** Returns true once the player's bare-hand right-click has reset the prop back to safe. */
    private static boolean checkAndHandlePrevention(ServerPlayer player, HazardStep hazard) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = hazard.pos();
        Map<BlockPos, Boolean> mine = lastActive.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        boolean activeNow = isActive(level, pos);
        boolean wasActive = mine.getOrDefault(pos, false);
        mine.put(pos, activeNow);
        if (!wasActive || activeNow) return false;

        AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                "§6[Sgt. Reyes] §aNicely done — caught before it ever became a fire!",
                "§6[Sgt. Reyes] §aThat's prevention in action. Well spotted!"));
        return true;
    }

    /** Beacon + compass at the alarm until pressed, then compass back at Reyes once it's ringing. */
    private static void tickAlarmCheckpoint(ServerLevel level, ServerPlayer player) {
        if (Boolean.TRUE.equals(alarmRinging.get(player.getUUID()))) {
            AcademyVisuals.setCompassTarget(player, REYES_ANCHOR);
        } else {
            if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
                AcademyVisuals.highlightBlocks(level, List.of(ALARM_POS));
            }
            AcademyVisuals.setCompassTarget(player,
                    new Vec3(ALARM_POS.getX() + 0.5, ALARM_POS.getY(), ALARM_POS.getZ() + 0.5));
        }
    }

    /**
     * Called from {@link FireAlarmBlock#useWithoutItem} so the Academy's own alarm checkpoint can
     * hook the shared block without touching its old-tutorial {@code SimulationSession} behavior
     * at all. Returns false (falls through to the block's normal behavior) unless this exact
     * player is at this exact alarm during {@code ALARM_CHECKPOINT}.
     */
    public static boolean tryHandleAlarmPress(ServerPlayer player, BlockPos pos) {
        if (!pos.equals(ALARM_POS)) return false;
        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        UUID id = player.getUUID();
        if (data.get(id).reyesPhase() != ReyesPhase.ALARM_CHECKPOINT) return false;
        if (Boolean.TRUE.equals(alarmRinging.get(id))) return true; // already ringing, absorb the click

        alarmRinging.put(id, true);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FireAlarmBlock && state.hasProperty(FireAlarmBlock.ACTIVATED)) {
            level.setBlock(pos, state.setValue(FireAlarmBlock.ACTIVATED, true), 3);
            level.playSound(null, pos, net.necookie.disastersim.BerongSMP.FIRE_ALARM_RING.get(), SoundSource.BLOCKS, 2.0f, 1.8f);
            level.scheduleTick(pos, state.getBlock(), 25);
        }
        AcademyVisuals.setCompassTarget(player, REYES_ANCHOR);
        AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §fGreat — the alarm's ringing! Now head back to me "
                + "so I can walk you through what happens next.");
        return true;
    }

    private static void stopAlarm(ServerLevel level) {
        BlockState state = level.getBlockState(ALARM_POS);
        if (state.getBlock() instanceof FireAlarmBlock && state.hasProperty(FireAlarmBlock.ACTIVATED)) {
            level.setBlock(ALARM_POS, state.setValue(FireAlarmBlock.ACTIVATED, false), 3);
        }
    }

    /** Sgt. Santos's anchor — points there until the player has actually started Room 3. */
    private static final Vec3 SANTOS_ANCHOR = new Vec3(-170.5, -33.0, 33.5);

    private static void tickDone(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        if (data.get(player.getUUID()).santosPhase() != SantosPhase.NOT_STARTED) {
            AcademyVisuals.setCompassTarget(player, null);
            return;
        }
        AcademyVisuals.setCompassTarget(player, SANTOS_ANCHOR);
    }

    private static void tickToolSelection(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        FrameSpec nextFrame = nextUncollectedFrame(player);
        if (nextFrame != null) {
            if (level.getGameTime() % HIGHLIGHT_INTERVAL_TICKS == 0) {
                AcademyVisuals.highlightBlocks(level, List.of(nextFrame.pos()));
            }
            BlockPos pos = nextFrame.pos();
            AcademyVisuals.setCompassTarget(player, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));

            if (level.getGameTime() % IDLE_NUDGE_INTERVAL_TICKS == 0
                    && !AcademyManager.isDialogueActive(player.getUUID())) {
                AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                        "§6[Sgt. Reyes] §7Walk right up to each extinguisher and click it with your "
                                + "§eleft mouse button§7 — it pops off the wall, then step onto it to "
                                + "pick it up. You need all three!",
                        "§6[Sgt. Reyes] §7Red, green, yellow — collect all three! §eLeft-click§7 a "
                                + "frame on the wall and walk over what drops.",
                        "§6[Sgt. Reyes] §7Each frame on the wall holds one extinguisher. Hit it with "
                                + "your §eleft mouse button§7, then scoop it up off the floor!"));
            }
            return;
        }
        AcademyVisuals.setCompassTarget(player, null);
        UUID id = player.getUUID();
        data.mutate(id, p -> p.setReyesPhase(ReyesPhase.LIVE_FIRE_DEMO));
        currentHazard.put(id, 0);
        explainedHazard.remove(id);
    }

    private static boolean hasAllExtinguishers(ServerPlayer player) {
        return nextUncollectedFrame(player) == null;
    }

    /** First extinguisher frame whose item the player doesn't have yet, or null once all 3 are collected. */
    private static FrameSpec nextUncollectedFrame(ServerPlayer player) {
        var inventory = player.getInventory();
        for (FrameSpec spec : EXTINGUISHER_FRAMES) {
            Item item = spec.item().get();
            if (!inventory.contains(stack -> stack.is(item))) {
                return spec;
            }
        }
        return null;
    }

    private static void tickLiveFireDemo(ServerLevel level, ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        int idx = currentHazard.getOrDefault(id, 0);

        if (idx >= HAZARDS.size()) {
            AcademyVisuals.setCompassTarget(player, null);
            tickIgniteDemo(player, data);
            return;
        }

        HazardStep hazard = HAZARDS.get(idx);
        pointAtHazard(level, player, hazard);

        if (!Objects.equals(explainedHazard.get(id), idx)) {
            explainedHazard.put(id, idx);
            // Ignite FIRST, synchronously, then narrate it — the fire must already be burning the
            // instant Reyes starts describing it, not several seconds later once the (still
            // forceStartDialogue'd, so a stray re-click can't clobber it) explanation happens to
            // finish. checkAndHandleDefuse already runs every tick once explainedHazard is marked,
            // independent of whether the dialogue itself has finished playing.
            igniteHazard(level, id, hazard);
            AcademyManager.forceStartDialogue(player, AcademyDialogue.REYES_HAZARD_LINES.get(idx), () -> {});
            return;
        }

        if (checkAndHandleDefuse(player, data, HAZARDS.get(idx))) {
            currentHazard.put(id, idx + 1);
        }
    }

    private static void igniteHazard(ServerLevel level, UUID playerId, HazardStep hazard) {
        level.setBlock(hazard.pos(), hazard.blockState().get(), 3);
        HazardManager.forceFailure(level, null, hazard.pos(), null);
        lastActive.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(hazard.pos(), true);
    }

    /**
     * Returns {@code true} only when this hazard was just correctly defused (advances the
     * sequence). A wrong-tool defuse re-ignites the same hazard instead of advancing.
     */
    private static boolean checkAndHandleDefuse(ServerPlayer player, AcademySavedData data, HazardStep hazard) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = hazard.pos();
        Map<BlockPos, Boolean> mine = lastActive.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        boolean activeNow = isActive(level, pos);
        boolean wasActive = mine.getOrDefault(pos, false);
        mine.put(pos, activeNow);

        if (!wasActive || activeNow) return false;
        if (player.position().distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > NEAR_RANGE_SQ) return false;

        boolean usedCorrectTool = hazard.correctTool().isInstance(player.getMainHandItem().getItem());
        if (usedCorrectTool) {
            data.mutate(player.getUUID(), AcademyProgress::addFireCorrectUse);
            AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                    "§6[Sgt. Reyes] §aPerfect match! That's exactly the right extinguisher for "
                            + "that fire. Well done!",
                    "§6[Sgt. Reyes] §aBeautiful work — right tool, right fire!",
                    "§6[Sgt. Reyes] §aThat's how it's done! Textbook extinguisher choice, trainee."));
            return true;
        }

        data.mutate(player.getUUID(), AcademyProgress::addFireWrongUse);
        AcademyManager.sendPrompt(player, AcademyManager.pick(player,
                "§6[Sgt. Reyes] §cNot that one — the fire flared back up! §7No harm done. Look at "
                        + "what's burning, press the matching extinguisher's §enumber key§7, and "
                        + "§ehold the right mouse button§7 to try again.",
                "§6[Sgt. Reyes] §cWhoops — wrong extinguisher, and the fire came right back! §7Match "
                        + "the color to what's burning and give it another go — you've got this."));
        igniteHazard(level, player.getUUID(), hazard);
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
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §cOh! A spark caught your uniform — don't panic! "
                    + "§fPress and hold §eShift§f, then press §eR§f to Drop and Roll. Roll until the flames "
                    + "are out!");
            return;
        }

        if (DropAndRollManager.isDropped(id)) {
            player.clearFire();
            data.mutate(id, p -> p.setDropAndRollPerformed(true));
            igniteWindow.remove(id);
            AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §aFlames out — beautifully done! §fYou just "
                    + "learned how to keep yourself safe. Take a breath.");
            advanceToAlarmCheckpoint(player, data);
            return;
        }

        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), FIRE_REFRESH_TICKS));

        if (remaining - 1 <= 0) {
            // Safety-cap timeout — they never rolled. Clear it and move on anyway (forgiving).
            player.clearFire();
            igniteWindow.remove(id);
            advanceToAlarmCheckpoint(player, data);
        } else {
            igniteWindow.put(id, remaining - 1);
        }
    }

    /** LIVE_FIRE_DEMO is over — clean up its props/fire and move on to the alarm checkpoint. */
    private static void advanceToAlarmCheckpoint(ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        cleanupHazardProps((ServerLevel) player.level());
        currentHazard.remove(id);
        explainedHazard.remove(id);
        lastActive.remove(id);
        data.mutate(id, p -> p.setReyesPhase(ReyesPhase.ALARM_CHECKPOINT));
    }

    /** The room's true end, run once EVACUATION_BRIEF's dialogue finishes. */
    private static void finishRoom(ServerPlayer player, AcademySavedData data) {
        UUID id = player.getUUID();
        alarmRinging.remove(id);
        data.mutate(id, p -> p.setReyesPhase(ReyesPhase.DONE));
        AcademyManager.sendPrompt(player, "§6[Sgt. Reyes] §fFollow the glowing arrow to §6Sgt. Santos§f "
                + "for the Earthquake Drill whenever you're ready.");
    }

    /** Room 2's box (with ceiling headroom) — the sweep area for leftover vanilla fire. */
    private static final BlockPos ROOM_MIN = new BlockPos(-173, -34, 10);
    private static final BlockPos ROOM_MAX = new BlockPos(-162, -28, 23);

    /** One wall slot on the Tool Selection Wall (Z=10, faces south into the room). */
    private record FrameSpec(BlockPos pos, Supplier<Item> item) {}

    /** The three extinguisher glow item frames, schematic-verified positions. */
    private static final List<FrameSpec> EXTINGUISHER_FRAMES = List.of(
            new FrameSpec(new BlockPos(-170, -32, 10), () -> BerongSMP.FIRE_EXTINGUISHER.get().asItem()),
            new FrameSpec(new BlockPos(-168, -32, 10), () -> BerongSMP.CO2_EXTINGUISHER.get().asItem()),
            new FrameSpec(new BlockPos(-166, -32, 10), () -> BerongSMP.WET_CHEMICAL_EXTINGUISHER.get().asItem())
    );

    /**
     * Restocks the Tool Selection Wall: refills each of the three glow item frames with its
     * extinguisher, respawning any frame entity that's gone entirely. The schematic only restores
     * the wall on a full server reboot ({@code SchemLoader} re-places entities at placement time) —
     * a mid-session restart via {@code /bfp new_tutorial [reset]} or a Capt. Morfe fail previously
     * left the frames empty (the previous run took the items) or missing (popped/burned), so the
     * next trainee couldn't complete TOOL_SELECTION at all. Called from both reset paths and,
     * self-healingly, right after building placement.
     */
    public static void restockExtinguisherFrames(ServerLevel level) {
        for (FrameSpec spec : EXTINGUISHER_FRAMES) {
            BlockPos pos = spec.pos();
            AABB cell = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, cell);
            ItemFrame frame;
            if (frames.isEmpty()) {
                frame = new GlowItemFrame(level, pos, Direction.SOUTH);
                level.addFreshEntity(frame);
            } else {
                frame = frames.get(0);
            }
            frame.setItem(new ItemStack(spec.item().get()));
        }
    }

    /**
     * Removes the 3 code-spawned hazard props and any leftover vanilla fire in Room 2. The props
     * only ever exist because {@link #igniteHazard} places them, and their forced failure spreads
     * real fire — without this sweep both would persist in the world forever after the drill.
     * Called on room finish, on a mid-demo logout/death, and on a Capt. Morfe fail-reset.
     */
    public static void cleanupHazardProps(ServerLevel level) {
        for (HazardStep hazard : HAZARDS) {
            level.setBlock(hazard.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
        for (BlockPos pos : BlockPos.betweenClosed(ROOM_MIN, ROOM_MAX)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Called from {@code AcademyManager}'s logout handler (and {@code /bfp new_tutorial reset}) to
     * drop this room's per-player state. If the scripted ignite demo was active, also clears the
     * player's actual fire: vanilla persists remaining fire ticks in the player's own save data, so
     * without this a player who quits mid-demo would rejoin still visibly on fire and taking damage
     * from a "lesson" that has no way to resume properly (its window countdown is gone). A player
     * who quits anywhere in LIVE_FIRE_DEMO also gets the room's spawned props/fire cleaned up and
     * the phase rolled back to TOOL_SELECTION, so a re-login re-runs the hazard sequence cleanly
     * from its explanation dialogue instead of resuming into missing props.
     */
    public static void clearPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        if (igniteWindow.remove(id) != null) {
            player.clearFire();
        }
        currentHazard.remove(id);
        explainedHazard.remove(id);
        lastActive.remove(id);

        ServerLevel level = (ServerLevel) player.level();
        AcademySavedData data = AcademySavedData.get(level);
        ReyesPhase phase = data.get(id).reyesPhase();
        if (phase == ReyesPhase.LIVE_FIRE_DEMO) {
            cleanupHazardProps(level);
            data.mutate(id, p -> p.setReyesPhase(ReyesPhase.TOOL_SELECTION));
        } else if (phase == ReyesPhase.PREVENTION_DEMO) {
            cleanupHazardProps(level);
            data.mutate(id, p -> p.setReyesPhase(ReyesPhase.NOT_STARTED));
        } else if (phase == ReyesPhase.ALARM_CHECKPOINT && alarmRinging.remove(id) != null) {
            stopAlarm(level);
        }
    }
}
