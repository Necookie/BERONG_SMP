package net.necookie.disastersim.academy;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.academy.room1.CruzRoomManager;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;
import net.necookie.disastersim.academy.room3.SantosRoomManager;
import net.necookie.disastersim.academy.room4.MorfeRoomManager;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.network.AcademyStatusPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point for the Academy (new tutorial building): dispatches NPC right-clicks to the
 * matching room manager and fans the shared server tick out to all 4. Mirrors
 * {@code world/LobbyManager}'s {@code onEntityInteract} pattern, but matches
 * {@code instanceof CustomNpcEntity} + {@link NpcType} instead of the old Villager+NBT-tag check
 * — no interact handler exists for {@code CustomNpcEntity} anywhere else in the mod.
 *
 * <p>Only 4 of the schematic's {@code custom_npc} entities are wired here (Officer Cruz, Sgt.
 * Reyes, Sgt. Santos, Capt. Morfe); the other 5 NPC types found in {@code new_tut_building1.0.schem}
 * (student, security_tuazon, dm_orlanda, necookie, sir_bookmark) are decorative and intentionally
 * left unhandled.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public final class AcademyManager {

    private AcademyManager() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof CustomNpcEntity npc)) return;

        boolean handled = switch (npc.getNpcType()) {
            case OFFICER_CRUZ -> { CruzRoomManager.onInteract(player, npc); yield true; }
            case SGT_REYES -> { ReyesRoomManager.onInteract(player, npc); yield true; }
            case SGT_SANTOS -> { SantosRoomManager.onInteract(player, npc); yield true; }
            case CAPT_MORFE -> { MorfeRoomManager.onInteract(player, npc); yield true; }
            default -> false; // decorative NPC — not part of the scripted Academy
        };

        if (handled) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Called once per server tick from {@code SimulationManager.onServerTick}. */
    public static void tick(ServerLevel level) {
        CruzRoomManager.tick(level);
        ReyesRoomManager.tick(level);
        SantosRoomManager.tick(level);
        MorfeRoomManager.tick(level);
    }

    // -----------------------------------------------------------------------
    // Shared helpers — used by all 4 room managers
    // -----------------------------------------------------------------------

    public static void sendPrompt(ServerPlayer player, String text) {
        sendPrompt(player, text, 0f);
    }

    public static void sendPrompt(ServerPlayer player, String text, float intensity) {
        PacketDistributor.sendToPlayer(player, new AcademyStatusPayload(text, intensity));
    }

    /** Plays an NPC voice line for a single player only, stopping any previous VOICE sound first. */
    public static void playNpcSound(ServerPlayer player, String soundKey) {
        if (soundKey == null) return;
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.VOICE));
        Identifier rl = Identifier.fromNamespaceAndPath(BerongSMP.MODID, soundKey);
        SoundEvent event = SoundEvent.createVariableRangeEvent(rl);
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(event),
                SoundSource.VOICE,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f,
                player.level().getRandom().nextLong()
        ));
    }

    /**
     * Generic "print the next line in this list for this player" stepper, shared by all 4 rooms
     * (each owns its own transient {@code stepMap}, matching the {@code ConcurrentHashMap<UUID,?>}
     * idiom already used by {@code TutorialManager.holdOnTimers}/{@code DropAndRollManager}).
     * Returns {@code true} only when the line just delivered was both the last in the list AND
     * flagged {@code advancesPhase} — callers use that to trigger their own phase transition.
     * Re-clicking after the list is exhausted repeats the final line rather than erroring.
     */
    public static boolean stepDialogue(ServerPlayer player, Map<UUID, Integer> stepMap,
                                        List<AcademyDialogue.DialogueLine> lines) {
        if (lines == null || lines.isEmpty()) return false;
        UUID id = player.getUUID();
        int step = Math.min(stepMap.getOrDefault(id, 0), lines.size() - 1);
        AcademyDialogue.DialogueLine line = lines.get(step);

        sendPrompt(player, line.text());
        playNpcSound(player, line.soundKey());

        boolean isLast = step == lines.size() - 1;
        stepMap.put(id, isLast ? step : step + 1);
        return isLast && line.advancesPhase();
    }

    /** Resets a player's dialogue cursor for a room — call whenever that room's phase changes. */
    public static void resetDialogueStep(Map<UUID, Integer> stepMap, ServerPlayer player) {
        stepMap.remove(player.getUUID());
    }
}
