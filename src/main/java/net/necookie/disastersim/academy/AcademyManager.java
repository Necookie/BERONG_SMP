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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        tickDialogues(level);
        CruzRoomManager.tick(level);
        ReyesRoomManager.tick(level);
        SantosRoomManager.tick(level);
        MorfeRoomManager.tick(level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            cancelDialogue(player);
            CruzRoomManager.clearPlayer(id);
            ReyesRoomManager.clearPlayer(id);
            SantosRoomManager.clearPlayer(id);
            AcademyVisuals.clearPlayer(id);
        }
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

    // -----------------------------------------------------------------------
    // Dialogue sequencer — timed auto-advance instead of one line per click
    // -----------------------------------------------------------------------

    /**
     * One player's in-progress playback of a phase's line list. {@code lines} is compared by
     * reference (each {@code AcademyDialogue} line list is a distinct {@code static final} field,
     * so this is safe and avoids needing a separate "which phase" key).
     */
    private static final class DialogueSession {
        final List<AcademyDialogue.DialogueLine> lines;
        int index;
        long nextAdvanceTick;
        final Runnable onComplete;

        DialogueSession(List<AcademyDialogue.DialogueLine> lines, Runnable onComplete) {
            this.lines = lines;
            this.onComplete = onComplete;
        }
    }

    private static final Map<UUID, DialogueSession> activeSessions = new ConcurrentHashMap<>();

    private static final int MIN_LINE_TICKS = 60;  // 3s floor
    private static final int MAX_LINE_TICKS = 200; // 10s ceiling
    private static final int TICKS_PER_WORD = 5;   // ~0.25s/word reading pace

    /**
     * Starts (or, if this exact line list is already playing for this player, immediately
     * advances) a timed dialogue sequence. Each line auto-advances after a reading-pace delay
     * (see {@link #ticksFor}); clicking the NPC again while a line is showing skips ahead
     * immediately instead of waiting. {@code onComplete} fires once, after the last line's delay
     * elapses (or is skipped past) — callers put their phase-transition logic there instead of
     * checking a returned boolean, since completion is no longer synchronous with the call that
     * started the sequence.
     */
    public static void startOrAdvanceDialogue(ServerPlayer player, List<AcademyDialogue.DialogueLine> lines,
                                               Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;
        UUID id = player.getUUID();
        DialogueSession session = activeSessions.get(id);

        if (session != null && session.lines == lines) {
            advanceSession(player, session);
            return;
        }

        session = new DialogueSession(lines, onComplete);
        activeSessions.put(id, session);
        playCurrentLine(player, session);
    }

    /** Drops a player's active session without firing its completion callback. */
    public static void cancelDialogue(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
    }

    /** Called from {@link #tick} — auto-advances any session whose delay has elapsed. */
    private static void tickDialogues(ServerLevel level) {
        if (activeSessions.isEmpty()) return;
        long now = level.getGameTime();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            DialogueSession session = activeSessions.get(player.getUUID());
            if (session != null && now >= session.nextAdvanceTick) {
                advanceSession(player, session);
            }
        }
    }

    private static void advanceSession(ServerPlayer player, DialogueSession session) {
        session.index++;
        if (session.index >= session.lines.size()) {
            activeSessions.remove(player.getUUID());
            session.onComplete.run();
            return;
        }
        playCurrentLine(player, session);
    }

    private static void playCurrentLine(ServerPlayer player, DialogueSession session) {
        AcademyDialogue.DialogueLine line = session.lines.get(session.index);
        sendPrompt(player, line.text());
        playNpcSound(player, line.soundKey());
        session.nextAdvanceTick = ((ServerLevel) player.level()).getGameTime() + ticksFor(line.text());
    }

    private static int ticksFor(String text) {
        int words = text.trim().isEmpty() ? 1 : text.trim().split("\\s+").length;
        return Math.max(MIN_LINE_TICKS, Math.min(MAX_LINE_TICKS, words * TICKS_PER_WORD));
    }
}
