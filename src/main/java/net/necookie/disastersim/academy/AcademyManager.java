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
        net.necookie.disastersim.common.structure.NewTutBuildingManager.sweepStrayCruz(level);
        AcademyGuardrails.tickRescue(level);
        CruzRoomManager.tick(level);
        ReyesRoomManager.tick(level);
        SantosRoomManager.tick(level);
        MorfeRoomManager.tick(level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelDialogue(player);
            clearTransientState(player);
        }
    }

    /**
     * The logout hook above is the primary mid-effect cleanup, but it only runs on a clean
     * disconnect — a server crash (or force-kill) mid-drill persists {@code SantosPhase.PRE_DRILL}/
     * {@code QUAKE_ACTIVE} (or a mid-demo Reyes phase) in {@link AcademySavedData} with the logout
     * rollback never having run, and the room's tick loop would re-enter the effect the moment the
     * player rejoins: a full-strength earthquake out of nowhere, with no dialogue and no context.
     * Running the same (idempotent, phase-gated) rollback on login closes that path; for players
     * whose logout hook already ran, every step is a no-op.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearTransientState(player);
        }
    }

    /**
     * Drops every room manager's leak-prone per-player transient state (marks hit, Go/Stop
     * timers, ignite windows, the compass dedupe cache, ...) without touching persisted
     * {@link AcademyProgress} phases in general — except the two specific "mid-effect" phases
     * ({@link ReyesRoomManager}'s scripted ignite demo, {@link SantosRoomManager}'s earthquake
     * drill) that would otherwise silently resume the instant the player reconnects: their driving
     * transient timer is gone (cleared right here), but the *persisted* phase alone is enough for
     * the room's tick loop to re-enter that phase's effect on the very next tick, with no dialogue
     * re-triggered and no clear sign to the player of why it's happening again — reported as "the
     * earthquake is still going after exiting and reloading the world." Both room managers roll
     * their own phase back to a safe re-enterable point internally; see their {@code clearPlayer}.
     * Shared by the logout hook above and {@code /bfp new_tutorial reset}, which needs the same
     * cleanup while the player stays connected.
     */
    public static void clearTransientState(ServerPlayer player) {
        UUID id = player.getUUID();
        CruzRoomManager.clearPlayer(id);
        ReyesRoomManager.clearPlayer(player);
        SantosRoomManager.clearPlayer(player);
        AcademyVisuals.clearPlayer(id);
        AcademyGuardrails.clearPlayer(id);
    }

    // -----------------------------------------------------------------------
    // Shared helpers — used by all 4 room managers
    // -----------------------------------------------------------------------

    /**
     * Sends a caption to the Academy HUD. Deliberately does NOT touch camera-shake intensity —
     * shake travels on its own {@code AcademyShakePayload} channel (see
     * {@link AcademyVisuals#setShake}), so an unrelated caption can never silently stop (or
     * accidentally preserve) Sgt. Santos's earthquake the way the old combined packet did.
     */
    public static void sendPrompt(ServerPlayer player, String text) {
        int displayTicks = text.isEmpty() ? 0 : ticksFor(text) + PROMPT_EXPIRY_GRACE_TICKS;
        PacketDistributor.sendToPlayer(player, new AcademyStatusPayload(text, displayTicks));
    }

    /**
     * Picks one of several phrasings at random — used for every message a player hears more than
     * once per run (idle nudges, GO/STOP calls, correct/wrong feedback), so repeated coaching
     * doesn't sound like a broken record. All variants of a message must teach the same thing
     * with the same key names; only the wording varies.
     */
    public static String pick(ServerPlayer player, String... variants) {
        return variants[player.level().getRandom().nextInt(variants.length)];
    }

    /** Extra ticks beyond a caption's own reading pace before it auto-clears, in case a player lingers on it. */
    private static final int PROMPT_EXPIRY_GRACE_TICKS = 40; // 2s

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
        long shownAtTick;
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
     * Guardrail against rapid over-clicking an NPC: a click-driven skip-ahead (the branch in
     * {@link #startOrAdvanceDialogue} below) is only honored once a line has been showing for at
     * least this long, across every room/course that uses this shared sequencer. Without this, a
     * player mashing the interact key could blow through an entire multi-line sequence (and
     * whatever phase transition its {@code onComplete} triggers) within a fraction of a second,
     * skipping content and — for a tick-gated room like Cruz's — visibly racing ahead of what the
     * room's own state machine expected to still be teaching.
     */
    private static final int MIN_LINE_DISPLAY_TICKS = 20; // 1s

    /**
     * Starts (or, if this exact line list is already playing for this player, immediately
     * advances) a timed dialogue sequence. Each line auto-advances after a reading-pace delay
     * (see {@link #ticksFor}); clicking the NPC again while a line is showing skips ahead
     * immediately instead of waiting. {@code onComplete} fires once, after the last line's delay
     * elapses (or is skipped past) — callers put their phase-transition logic there instead of
     * checking a returned boolean, since completion is no longer synchronous with the call that
     * started the sequence.
     *
     * <p>If a <em>different</em> sequence is already playing for this player (most commonly: they
     * re-clicked an NPC while a tick-driven sequence like Sgt. Reyes's per-hazard explanation was
     * still on screen), this call is ignored instead of overwriting it — overwriting silently
     * discarded the in-flight session's {@code onComplete}, which is how a re-click used to
     * permanently strand Room 2 with the fire never igniting. Callers whose sequence MUST start
     * regardless of what else is playing (state that has to progress on a fixed tick schedule, not
     * on request) should use {@link #forceStartDialogue} instead.
     */
    public static void startOrAdvanceDialogue(ServerPlayer player, List<AcademyDialogue.DialogueLine> lines,
                                               Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;
        UUID id = player.getUUID();
        DialogueSession session = activeSessions.get(id);

        if (session != null) {
            long now = ((ServerLevel) player.level()).getGameTime();
            if (session.lines == lines && now - session.shownAtTick >= MIN_LINE_DISPLAY_TICKS) {
                advanceSession(player, session);
            }
            // else: already mid-conversation about something else (ignore rather than clobber), or
            // the current line hasn't been up long enough yet for a click-skip to be honored.
            return;
        }

        session = new DialogueSession(lines, onComplete);
        activeSessions.put(id, session);
        playCurrentLine(player, session);
    }

    /**
     * Like {@link #startOrAdvanceDialogue}, but always starts fresh even if a different sequence
     * is currently playing for this player — for tick-driven sequences that must begin exactly
     * when game state says they should (e.g. a hazard's explanation the instant its turn comes
     * up), where silently ignoring the request because an unrelated line happened to still be on
     * screen would strand that state forever. Safe to use here specifically because every session
     * that could still be active at that moment is a re-click reminder whose own onComplete is a
     * no-op (condition-gated phases, not dialogue-gated) — discarding it loses nothing.
     */
    public static void forceStartDialogue(ServerPlayer player, List<AcademyDialogue.DialogueLine> lines,
                                           Runnable onComplete) {
        if (lines == null || lines.isEmpty()) return;
        DialogueSession session = new DialogueSession(lines, onComplete);
        activeSessions.put(player.getUUID(), session);
        playCurrentLine(player, session);
    }

    /**
     * True while a timed dialogue sequence is actively playing for this player. Room managers'
     * periodic idle-nudge reminders should check this before calling {@link #sendPrompt} directly —
     * otherwise an unrelated nudge fired from a room's own tick loop can stomp the caption of a
     * dialogue line that's still "on screen" (its voice line may still be playing), which reads as
     * the caption and voice falling out of sync with each other.
     */
    public static boolean isDialogueActive(UUID id) {
        return activeSessions.containsKey(id);
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
            // Nothing else clears the caption once a sequence naturally finishes -- without this,
            // the last line stays glued to the screen forever until some unrelated prompt happens
            // to overwrite it. If onComplete has something new to say, it sends it right after.
            sendPrompt(player, "");
            session.onComplete.run();
            return;
        }
        playCurrentLine(player, session);
    }

    private static void playCurrentLine(ServerPlayer player, DialogueSession session) {
        AcademyDialogue.DialogueLine line = session.lines.get(session.index);
        sendPrompt(player, line.text());
        playNpcSound(player, line.soundKey());
        long now = ((ServerLevel) player.level()).getGameTime();
        session.shownAtTick = now;
        session.nextAdvanceTick = now + ticksFor(line.text());
    }

    private static int ticksFor(String text) {
        int words = text.trim().isEmpty() ? 1 : text.trim().split("\\s+").length;
        return Math.max(MIN_LINE_TICKS, Math.min(MAX_LINE_TICKS, words * TICKS_PER_WORD));
    }
}
