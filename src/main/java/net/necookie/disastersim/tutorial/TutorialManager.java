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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.network.TutorialStatusPayload;
import net.necookie.disastersim.world.LobbyManager;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the per-player safety tutorial that gates access to the disaster simulations.
 *
 * <p>Tutorial progression is linear: NOT_STARTED → PASS stages → EXT_TYPE stages
 * → QUAKE stages → COMPLETED. Progress is persisted to disk via {@link TutorialSavedData}.
 *
 * <p>Station positions are placed programmatically in the lobby at known offsets.
 * These constants may need tuning once the lobby NBT layout is known.
 */
public class TutorialManager {

    // -----------------------------------------------------------------------
    // Station positions (offsets from LobbyManager.LOBBY_POS = (0, -33, 0))
    // Adjust these based on the actual lobby interior layout.
    // -----------------------------------------------------------------------

    /** Right-click to begin the PASS tutorial and receive the fire extinguisher. */
    static final BlockPos STATION_PULL  = LobbyManager.LOBBY_POS.offset(5,  2, 14);
    /** Right-click to learn about Class A extinguishers. */
    static final BlockPos STATION_EXT_A = LobbyManager.LOBBY_POS.offset(9,  2, 14);
    /** Right-click to learn about Class B extinguishers. */
    static final BlockPos STATION_EXT_B = LobbyManager.LOBBY_POS.offset(13, 2, 14);
    /** Right-click to learn about Class C extinguishers. */
    static final BlockPos STATION_EXT_C = LobbyManager.LOBBY_POS.offset(17, 2, 14);

    /**
     * Center of the practice fire cluster. Five fire blocks placed in a cross pattern.
     * Must be on a non-flammable floor (we place stone slabs beneath the fire).
     */
    static final BlockPos PRACTICE_FIRE = LobbyManager.LOBBY_POS.offset(11, 2, 18);

    private static final Set<BlockPos> ALL_STATIONS = Set.of(
            STATION_PULL, STATION_EXT_A, STATION_EXT_B, STATION_EXT_C);

    /** Practice fire cross pattern (center + 4 cardinal neighbours). */
    private static final List<BlockPos> PRACTICE_FIRE_POSITIONS = List.of(
            PRACTICE_FIRE,
            PRACTICE_FIRE.relative(Direction.NORTH), PRACTICE_FIRE.relative(Direction.SOUTH),
            PRACTICE_FIRE.relative(Direction.EAST),  PRACTICE_FIRE.relative(Direction.WEST)
    );

    // -----------------------------------------------------------------------
    // Per-player transient state (not persisted; resets if server restarts
    // while a player is mid-QUAKE — they re-enter from QUAKE_DROP on next
    // login, which is acceptable for a tutorial flow)
    // -----------------------------------------------------------------------

    private static final Map<UUID, Integer> holdOnTimers      = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> extinguishCounts  = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Places station blocks in the lobby. Call from {@code LobbyManager.createLobby} after NBT load. */
    public static void placeStations(ServerLevel level) {
        placeStation(level, STATION_PULL,  "§6[PULL STATION]\n§ePASS: Pull the pin here!");
        placeStation(level, STATION_EXT_A, "§a[Class A]\n§7Water/foam — ordinary combustibles");
        placeStation(level, STATION_EXT_B, "§e[Class B]\n§7CO2/dry chem — flammable liquids/gas");
        placeStation(level, STATION_EXT_C, "§b[Class C]\n§7CO2/dry chem — electrical fires");
    }

    /** @return {@code true} if the given block position is a tutorial station. */
    public static boolean isStationPos(BlockPos pos) {
        return ALL_STATIONS.contains(pos);
    }

    /**
     * Handles a player right-clicking a tutorial station block.
     * Advances the stage when the click matches the expected next station.
     */
    public static void onInteract(ServerPlayer player, BlockPos pos) {
        TutorialStage stage = getStage(player);

        if (pos.equals(STATION_PULL)) {
            if (stage == TutorialStage.NOT_STARTED) {
                giveExtinguisher(player);
                spawnPracticeFires((ServerLevel) player.level());
                advanceTo(player, TutorialStage.PASS_SPRAY);
                sendPrompt(player, "§ePull the pin (right-click extinguisher), then aim and spray the fire!", 0f);
                player.sendSystemMessage(Component.literal(
                        "§6=== PASS Method ===\n"
                        + "§eP§7 - Pull the pin (right-click)\n"
                        + "§eA§7 - Aim at the base of the fire\n"
                        + "§eS§7 - Squeeze the handle (hold right-click)\n"
                        + "§eS§7 - Sweep side to side\n"
                        + "§ePut out 3 fire blocks to complete this stage!"));
            } else {
                player.sendSystemMessage(Component.literal("§7You've already completed this step."));
            }
            return;
        }

        if (pos.equals(STATION_EXT_A)) {
            if (stage == TutorialStage.EXT_TYPE_A) {
                player.sendSystemMessage(Component.literal(
                        "§a=== Class A Fire Extinguisher ===\n"
                        + "§7For ordinary combustibles: wood, paper, cloth, plastics.\n"
                        + "§7Common types: water, foam, multi-purpose dry chemical."));
                advanceTo(player, TutorialStage.EXT_TYPE_B);
                sendPrompt(player, "§eGood! Now check the Class B station.", 0f);
            } else {
                wrongOrderMessage(player, stage, TutorialStage.EXT_TYPE_A);
            }
            return;
        }

        if (pos.equals(STATION_EXT_B)) {
            if (stage == TutorialStage.EXT_TYPE_B) {
                player.sendSystemMessage(Component.literal(
                        "§e=== Class B Fire Extinguisher ===\n"
                        + "§7For flammable liquids and gases: gasoline, oil, propane.\n"
                        + "§7Common types: CO2, dry chemical, foam."));
                advanceTo(player, TutorialStage.EXT_TYPE_C);
                sendPrompt(player, "§eNow check the Class C station.", 0f);
            } else {
                wrongOrderMessage(player, stage, TutorialStage.EXT_TYPE_B);
            }
            return;
        }

        if (pos.equals(STATION_EXT_C)) {
            if (stage == TutorialStage.EXT_TYPE_C) {
                player.sendSystemMessage(Component.literal(
                        "§b=== Class C Fire Extinguisher ===\n"
                        + "§7For energized electrical equipment.\n"
                        + "§7Common types: CO2, dry chemical. NEVER use water on Class C!\n"
                        + "§c⚠ Earthquake safety drill starting — brace yourself!"));
                advanceTo(player, TutorialStage.QUAKE_DROP);
                // Shake starts immediately; tick() will maintain it
                sendPrompt(player, "§c⚠ EARTHQUAKE! Press [SHIFT] to DROP!", 1.5f);
            } else {
                wrongOrderMessage(player, stage, TutorialStage.EXT_TYPE_C);
            }
        }
    }

    /**
     * Called when the player extinguishes a fire block during the PASS_SPRAY stage.
     * Counts up to 3 total extinguishes, then advances to EXT_TYPE_A.
     */
    public static void onExtinguish(ServerPlayer player) {
        if (getStage(player) != TutorialStage.PASS_SPRAY) return;

        int count = extinguishCounts.merge(player.getUUID(), 1, Integer::sum);
        sendPrompt(player, "§eSweep! Extinguish practice fires. (" + count + "/3)", 0f);

        if (count >= 3) {
            extinguishCounts.remove(player.getUUID());
            removePracticeFires((ServerLevel) player.level());
            player.sendSystemMessage(Component.literal(
                    "§a✓ PASS method complete! Great job extinguishing the fire.\n"
                    + "§eNow learn about extinguisher types. Right-click the Class A station."));
            advanceTo(player, TutorialStage.EXT_TYPE_A);
            sendPrompt(player, "§eRight-click the §a[Class A]§e station.", 0f);
        }
    }

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
                    // Refresh shake every 10 ticks; advance when player crouches
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
                        // Fade intensity from 1.5 → 0 over the 100-tick hold period
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
                        // Broke cover — reset timer, keep shaking
                        holdOnTimers.put(player.getUUID(), 0);
                        if (gameTick % 10 == 0) {
                            sendPrompt(player, "§cDon't move! Stay crouched under cover!", 1.5f);
                        }
                    }
                }
                default -> { /* other stages handled by events, not tick */ }
            }
        }
    }

    /** @return {@code true} if the player has completed all tutorial stages. */
    public static boolean isComplete(UUID uuid) {
        // We need a server level to load saved data; use the current server's overworld.
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        return TutorialSavedData.get(server.overworld()).getStage(uuid) == TutorialStage.COMPLETED;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
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

    private static void giveExtinguisher(ServerPlayer player) {
        ItemStack stack = new ItemStack(BerongSMP.FIRE_EXTINGUISHER.get());
        // Pin is NOT pre-pulled — player must right-click once to pull it, then hold to spray.
        player.getInventory().setItem(0, stack);
    }

    private static void spawnPracticeFires(ServerLevel level) {
        for (BlockPos firePos : PRACTICE_FIRE_POSITIONS) {
            // Ensure the block below is non-flammable (place stone if needed)
            BlockPos floorPos = firePos.below();
            if (level.getBlockState(floorPos).isAir()) {
                level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
            }
            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
        }
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
        advanceTo(player, TutorialStage.COMPLETED);
        sendPrompt(player, "", 0f);
        player.sendSystemMessage(Component.literal(
                "§a✓ §lSafety Tutorial Complete!\n"
                + "§7You've learned fire extinguisher techniques and earthquake safety.\n"
                + "§eYou can now start the disaster simulations in the lobby!"));
        // Confetti burst
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.5, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.3);
        BerongSMP.LOGGER.info("Player {} completed the safety tutorial.", player.getName().getString());
    }

    private static void wrongOrderMessage(ServerPlayer player, TutorialStage current, TutorialStage required) {
        if (current.ordinal() < required.ordinal()) {
            player.sendSystemMessage(Component.literal("§cComplete earlier tutorial steps first!"));
        } else {
            player.sendSystemMessage(Component.literal("§7You've already completed this step."));
        }
    }

    /**
     * Places a stone pedestal with an oak button and a decorative stone brick wall behind it.
     * Station labels are communicated via chat messages and HUD prompts rather than sign text.
     * The button faces north so it is clickable from the expected approach direction.
     */
    private static void placeStation(ServerLevel level, BlockPos pos, String ignoredSignText) {
        // Stone pedestal beneath the button
        level.setBlock(pos.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);

        // Oak button on top of the pedestal, facing north (floor-placed)
        BlockState buttonState = Blocks.OAK_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlock(pos, buttonState, 3);

        // Stone brick backdrop one block south, plus one block above, for visual framing
        BlockPos wallBase = pos.relative(Direction.SOUTH);
        level.setBlock(wallBase, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(wallBase.above(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
    }
}
