package net.necookie.disastersim.academy;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.command.BfpAdminCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-safety guardrails for the Academy tutorial building:
 * <ul>
 *   <li><b>Block protection</b> — students can't mine the maze walls, the earthquake cover table,
 *       the lime WASD marks, or the spawned hazard props mid-drill, and can't wall themselves in
 *       with placed blocks. Admins (OP level 2+ or an active {@code /bfp bypass}) are exempt so
 *       WorldEdit/manual fixups keep working.</li>
 *   <li><b>Out-of-bounds rescue</b> ({@link #tickRescue}) — a player mid-tutorial who ends up
 *       outside the building (or falls into the void below it) is teleported back to the anchor of
 *       the room they're currently working on.</li>
 *   <li><b>Death/respawn recovery</b> ({@link #onPlayerRespawn}) — dying mid-tutorial applies the
 *       same mid-effect rollback the logout hook does (Santos's quake, Reyes's ignite demo) and
 *       returns the player to their current room instead of world spawn.</li>
 * </ul>
 *
 * <p>Deliberate consequence of the block guard: punching vanilla fire out by hand inside the
 * building is also a block break, so it's cancelled too — extinguishers ({@code setBlock}, no
 * break event fired) become the only way to put fires out, which is exactly the behavior Room 2
 * teaches.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public final class AcademyGuardrails {

    /**
     * Academy building bounds with margin: the schematic occupies X -177..-96, Y -34..-27,
     * Z 8..84; one extra block horizontally and generous Y headroom so roof/edge cases still
     * count as "inside".
     */
    public static final AABB BUILDING_BOUNDS = new AABB(-178, -40, 7, -94, -20, 86);

    private static final int DENY_MSG_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> lastDenyMsgTick = new ConcurrentHashMap<>();

    private AcademyGuardrails() {}

    /** OP level 2+ or an active /bfp test bypass — the same people every other admin gate trusts. */
    public static boolean isAdmin(ServerPlayer player) {
        return Commands.LEVEL_GAMEMASTERS.check(player.permissions())
                || BfpAdminCommands.isTestBypass(player.getUUID());
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (isAdmin(player)) return;
        if (!BUILDING_BOUNDS.contains(Vec3.atCenterOf(event.getPos()))) return;
        event.setCanceled(true);
        sendDenyMessage(level, player);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isAdmin(player)) return;
        if (!BUILDING_BOUNDS.contains(Vec3.atCenterOf(event.getPos()))) return;
        event.setCanceled(true);
        sendDenyMessage(level, player);
    }

    private static void sendDenyMessage(ServerLevel level, ServerPlayer player) {
        long now = level.getGameTime();
        Long last = lastDenyMsgTick.get(player.getUUID());
        if (last != null && now - last < DENY_MSG_COOLDOWN_TICKS) return;
        lastDenyMsgTick.put(player.getUUID(), now);
        AcademyManager.sendPrompt(player, "§e[Academy] §7This building is part of your training — "
                + "it can't be changed. No blocks needed here, just follow your instructor's steps!");
    }

    /** Called from {@code AcademyManager.clearTransientState} to drop this player's cooldown entry. */
    public static void clearPlayer(UUID id) {
        lastDenyMsgTick.remove(id);
    }

    // -----------------------------------------------------------------------
    // Out-of-bounds rescue + death/respawn recovery
    // -----------------------------------------------------------------------

    private static final int RESCUE_CHECK_INTERVAL_TICKS = 20;
    /** Below this Y the player has fallen out of the building's underside. */
    private static final double RESCUE_MIN_Y = -36.0;

    /** Called every tick from {@code AcademyManager.tick}; only actually scans every 20 ticks. */
    public static void tickRescue(ServerLevel level) {
        if (level.getGameTime() % RESCUE_CHECK_INTERVAL_TICKS != 0) return;
        AcademySavedData data = AcademySavedData.get(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (isAdmin(player)) continue;
            AcademyProgress progress = data.get(player.getUUID());
            if (!isInProgress(progress)) continue;
            boolean out = !BUILDING_BOUNDS.contains(player.position()) || player.getY() < RESCUE_MIN_Y;
            if (!out) continue;
            Vec3 anchor = currentRoomAnchor(progress);
            player.teleportTo(level, anchor.x, anchor.y, anchor.z,
                    Collections.emptySet(), player.getYRot(), player.getXRot(), true);
            AcademyManager.sendPrompt(player, "§e[Academy] §7Whoops — you wandered off the training "
                    + "floor! Here you are, right back with your instructor.");
        }
    }

    /** Started Room 1 but not yet certified — the window where the tutorial "owns" the player. */
    private static boolean isInProgress(AcademyProgress p) {
        return p.cruzPhase() != CruzPhase.NOT_STARTED && p.morfePhase() != MorfePhase.EVALUATED_PASS;
    }

    /** The anchor of the furthest room the player has reached — matches the NPC anchor positions. */
    private static Vec3 currentRoomAnchor(AcademyProgress p) {
        if (p.santosPhase() == SantosPhase.DONE) return new Vec3(-108.5, -33.0, 77.5); // Capt. Morfe
        if (p.reyesPhase()  == ReyesPhase.DONE)  return new Vec3(-170.5, -33.0, 33.5); // Sgt. Santos
        if (p.cruzPhase()   == CruzPhase.DONE)   return new Vec3(-172.5, -33.0, 17.5); // Sgt. Reyes
        return new Vec3(-145.5, -33.0, 31.5);                                          // Room 1 briefing centre
    }

    /**
     * Dying mid-tutorial gets the same treatment as logging out mid-tutorial: cancel any playing
     * dialogue, roll back mid-effect phases (Santos's quake, Reyes's ignite demo — via the shared
     * {@code clearTransientState}), and return the player to their current room rather than
     * leaving them at world spawn with drill nudges firing at a table they can't reach.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        AcademyProgress progress = AcademySavedData.get(level).get(player.getUUID());
        if (!isInProgress(progress)) return;
        AcademyManager.cancelDialogue(player);
        AcademyManager.clearTransientState(player);
        Vec3 anchor = currentRoomAnchor(progress);
        player.teleportTo(level, anchor.x, anchor.y, anchor.z,
                Collections.emptySet(), player.getYRot(), player.getXRot(), true);
        AcademyManager.sendPrompt(player, "§e[Academy] §7Welcome back, trainee! No worries at all — "
                + "let's pick up right where your training left off.");
    }
}
