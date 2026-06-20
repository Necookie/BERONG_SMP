package net.necookie.disastersim.tutorial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.network.TutorialStatusPayload;
import net.necookie.disastersim.world.LobbyManager;
import net.necookie.disastersim.world.TutorialLobbyManager;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the per-player safety tutorial that gates access to the disaster simulations.
 *
 * <p>Tutorial progression: NOT_STARTED → PASS_SPRAY (via Sgt. Reyes NPC + extinguisher drill)
 * → EXT_TYPE_A/B/C (via Officer Cruz NPC dialogue) → QUAKE_DROP/COVER/HOLDON (tick-driven drill)
 * → COMPLETED (teleports to main lobby).
 *
 * <p>Progress is persisted to disk via {@link TutorialSavedData}. Transient per-player maps
 * (hold-on timers, extinguish counts, dialogue steps) reset if the server restarts mid-stage —
 * acceptable for a tutorial flow since stages re-prompt on next login.
 */
public class TutorialManager {

    /** Center of the practice fire cluster in the BFP tutorial lobby. */
    static final BlockPos PRACTICE_FIRE = TutorialLobbyManager.TUTORIAL_LOBBY_POS.offset(12, 2, 9);

    /** Practice fire cross pattern (center + 4 cardinal neighbours). */
    private static final List<BlockPos> PRACTICE_FIRE_POSITIONS = List.of(
            PRACTICE_FIRE,
            PRACTICE_FIRE.relative(Direction.NORTH), PRACTICE_FIRE.relative(Direction.SOUTH),
            PRACTICE_FIRE.relative(Direction.EAST),  PRACTICE_FIRE.relative(Direction.WEST)
    );

    // Per-player transient state — keyed by UUID, never persisted
    private static final Map<UUID, Integer> holdOnTimers     = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> extinguishCounts = new ConcurrentHashMap<>();
    /** Current dialogue line index for each player mid-conversation. */
    private static final Map<UUID, Integer> dialogueSteps    = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // NPC dialogue
    // -----------------------------------------------------------------------

    /**
     * Handles a player right-clicking a BFP tutorial NPC villager.
     * Advances through the NPC's dialogue lines one click at a time;
     * the final line marked {@code advancesStage=true} triggers the stage transition.
     */
    public static void onNpcInteract(ServerPlayer player, NpcRole role) {
        TutorialStage stage = getStage(player);

        Map<TutorialStage, List<NpcDialogue.DialogueLine>> linesMap = switch (role) {
            case TRAINER        -> NpcDialogue.TRAINER_LINES;
            case EXT_EXPERT     -> NpcDialogue.EXT_EXPERT_LINES;
            case SAFETY_OFFICER -> NpcDialogue.SAFETY_OFFICER_LINES;
        };

        List<NpcDialogue.DialogueLine> lines = linesMap.get(stage);
        if (lines == null || lines.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7This NPC has nothing to say right now."));
            return;
        }

        int step = dialogueSteps.getOrDefault(player.getUUID(), 0);
        if (step >= lines.size()) step = 0;

        NpcDialogue.DialogueLine line = lines.get(step);
        sendPrompt(player, line.text(), 0f);

        int nextStep = step + 1;
        if (nextStep >= lines.size()) {
            dialogueSteps.remove(player.getUUID());
            if (line.advancesStage()) {
                handleStageAdvancement(player, stage);
            }
        } else {
            dialogueSteps.put(player.getUUID(), nextStep);
        }
    }

    private static void handleStageAdvancement(ServerPlayer player, TutorialStage fromStage) {
        ServerLevel level = (ServerLevel) player.level();
        switch (fromStage) {
            case NOT_STARTED, PASS_PULL -> {
                giveExtinguisher(player);
                spawnPracticeFires(level);
                advanceTo(player, TutorialStage.PASS_SPRAY);
                sendPrompt(player, "§eExtinguish all 3 practice fires to continue!", 0f);
            }
            case EXT_TYPE_A -> {
                advanceTo(player, TutorialStage.EXT_TYPE_B);
                sendPrompt(player, "§eTalk to §a[Officer Cruz]§e about Class B fires.", 0f);
            }
            case EXT_TYPE_B -> {
                advanceTo(player, TutorialStage.EXT_TYPE_C);
                sendPrompt(player, "§eTalk to §a[Officer Cruz]§e about Class C fires.", 0f);
            }
            case EXT_TYPE_C -> {
                advanceTo(player, TutorialStage.QUAKE_DROP);
                sendPrompt(player, "§c⚠ EARTHQUAKE! Press [SHIFT] to DROP!", 1.5f);
            }
            default -> {}
        }
    }

    // -----------------------------------------------------------------------
    // Extinguisher practice
    // -----------------------------------------------------------------------

    /**
     * Called when the player extinguishes a fire block during PASS_SPRAY.
     * Counts up to 3 total extinguishes then advances to EXT_TYPE_A.
     */
    public static void onExtinguish(ServerPlayer player) {
        if (getStage(player) != TutorialStage.PASS_SPRAY) return;

        int count = extinguishCounts.merge(player.getUUID(), 1, Integer::sum);
        sendPrompt(player, "§eSweep! Extinguish practice fires. (" + count + "/3)", 0f);

        if (count >= 3) {
            extinguishCounts.remove(player.getUUID());
            removePracticeFires((ServerLevel) player.level());
            player.sendSystemMessage(Component.literal(
                    "§a✓ PASS method complete! Great job.\n"
                    + "§eTalk to §a[Officer Cruz]§e to learn about extinguisher types."));
            advanceTo(player, TutorialStage.EXT_TYPE_A);
            sendPrompt(player, "§eTalk to §a[Officer Cruz]§e about fire extinguisher types.", 0f);
        }
    }

    // -----------------------------------------------------------------------
    // Earthquake drill — per-tick driver
    // -----------------------------------------------------------------------

    /**
     * Per-tick driver for QUAKE tutorial stages. Called from {@code SimulationManager.onServerTick}.
     *
     * @param level The overworld server level; used to read block states for cover detection.
     */
    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            TutorialStage stage = getStage(player);
            long gameTick = level.getGameTime();

            switch (stage) {
                case QUAKE_DROP -> {
                    if (gameTick % 10 == 0) {
                        sendPrompt(player, "§c⚠ EARTHQUAKE! Press [SHIFT] to DROP!", 1.5f);
                    }
                    if (player.isCrouching()) {
                        advanceTo(player, TutorialStage.QUAKE_COVER);
                        sendPrompt(player, "§eGood! Now move under a solid block for COVER!", 1.5f);
                    }
                }
                case QUAKE_COVER -> {
                    if (gameTick % 10 == 0) {
                        sendPrompt(player, "§eMove under a solid block while crouching!", 1.5f);
                    }
                    BlockPos above2 = player.blockPosition().above(2);
                    boolean hasCover = !level.getBlockState(above2).isAir();
                    if (player.isCrouching() && hasCover) {
                        advanceTo(player, TutorialStage.QUAKE_HOLDON);
                        holdOnTimers.put(player.getUUID(), 0);
                        sendPrompt(player, "§aHOLD ON! Stay covered... (0s / 5s)", 1.5f);
                    }
                }
                case QUAKE_HOLDON -> {
                    BlockPos above2 = player.blockPosition().above(2);
                    boolean stillCovered = player.isCrouching() && !level.getBlockState(above2).isAir();
                    int timer = holdOnTimers.getOrDefault(player.getUUID(), 0);

                    if (stillCovered) {
                        timer++;
                        holdOnTimers.put(player.getUUID(), timer);
                        float fadeIntensity = 1.5f * Math.max(0f, 1f - (timer / 100f));
                        if (gameTick % 10 == 0) {
                            int secHeld = timer / 20;
                            sendPrompt(player, "§aHOLD ON! Stay covered... (" + secHeld + "s / 5s)", fadeIntensity);
                        }
                        if (timer >= 100) {
                            holdOnTimers.remove(player.getUUID());
                            completeTutorial(player, level);
                        }
                    } else {
                        holdOnTimers.put(player.getUUID(), 0);
                        if (gameTick % 10 == 0) {
                            sendPrompt(player, "§cDon't move! Stay crouched under cover!", 1.5f);
                        }
                    }
                }
                default -> {}
            }
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** @return {@code true} if the player has completed all tutorial stages. */
    public static boolean isComplete(UUID uuid) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        return TutorialSavedData.get(server.overworld()).getStage(uuid) == TutorialStage.COMPLETED;
    }

    /** Spawns 5 practice fire blocks (cross pattern) in the tutorial lobby. */
    public static void spawnPracticeFires(ServerLevel level) {
        for (BlockPos firePos : PRACTICE_FIRE_POSITIONS) {
            BlockPos floorPos = firePos.below();
            if (level.getBlockState(floorPos).isAir()) {
                level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
            }
            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
        }
    }

    /** Gives the player a fire extinguisher in hotbar slot 0. */
    public static void giveExtinguisher(ServerPlayer player) {
        player.getInventory().setItem(0, new ItemStack(BerongSMP.FIRE_EXTINGUISHER.get()));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static TutorialStage getStage(ServerPlayer player) {
        return TutorialSavedData.get((ServerLevel) player.level()).getStage(player.getUUID());
    }

    private static void advanceTo(ServerPlayer player, TutorialStage next) {
        TutorialSavedData.get((ServerLevel) player.level()).setStage(player.getUUID(), next);
    }

    private static void sendPrompt(ServerPlayer player, String text, float intensity) {
        PacketDistributor.sendToPlayer(player, new TutorialStatusPayload(text, intensity));
    }

    private static void removePracticeFires(ServerLevel level) {
        for (BlockPos firePos : PRACTICE_FIRE_POSITIONS) {
            BlockState state = level.getBlockState(firePos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(firePos, false);
            }
        }
    }

    private static void completeTutorial(ServerPlayer player, ServerLevel level) {
        net.necookie.disastersim.session.SessionManager.onTutorialComplete(player);
        advanceTo(player, TutorialStage.COMPLETED);
        sendPrompt(player, "", 0f);
        player.sendSystemMessage(Component.literal(
                "§a✓ §lSafety Tutorial Complete!\n"
                + "§7You've mastered fire safety and earthquake drills.\n"
                + "§eTeleporting you to the main lobby..."));
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.5, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.3);
        player.teleportTo(level, LobbyManager.SPAWN_X, LobbyManager.SPAWN_Y, LobbyManager.SPAWN_Z,
                Collections.emptySet(), 0f, 0f, true);
        BerongSMP.LOGGER.info("Player {} completed the safety tutorial.", player.getName().getString());
    }
}
