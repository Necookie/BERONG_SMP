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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-safety guardrails for the Academy tutorial building. First piece: block protection —
 * students can't mine the maze walls, the earthquake cover table, the lime WASD marks, or the
 * spawned hazard props mid-drill, and can't wall themselves in with placed blocks. Admins (OP
 * level 2+ or an active {@code /bfp bypass}) are exempt so WorldEdit/manual fixups keep working.
 *
 * <p>Deliberate consequence: punching vanilla fire out by hand inside the building is also a block
 * break, so it's cancelled too — extinguishers ({@code setBlock}, no break event fired) become the
 * only way to put fires out, which is exactly the behavior Room 2 teaches.
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
}
