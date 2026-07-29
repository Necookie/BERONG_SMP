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
import net.necookie.disastersim.common.player.PlayerLifecycleRegistry;
import net.necookie.disastersim.common.scheduling.TickScheduler;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.network.AcademyStatusPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.necookie.disastersim.common.structure.AcademyBuildingManager;

/**
 * Entry point for the Academy (new tutorial building): dispatches NPC right-clicks to the
 * matching room manager and fans the shared server tick out to all 4. Mirrors
 * {@code world/LobbyManager}'s {@code onEntityInteract} pattern, but matches
 * {@code instanceof CustomNpcEntity} + {@link NpcType} instead of the old Villager+NBT-tag check
 * — no interact handler exists for {@code CustomNpcEntity} anywhere else in the mod.
 *
 * <p>Only 4 of the schematic's {@code custom_npc} entities are wired here (Officer Cruz, Sgt.
 * Reyes, Sgt. Santos, Capt. Morfe); the other 5 NPC types found in {@code academy_building.schem}
 * (student, security_tuazon, dm_orlanda, necookie, sir_bookmark) are decorative and intentionally
 * left unhandled.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public final class AcademyManager {

    private AcademyManager() {}

    /**
     * The logout hook is the primary mid-effect cleanup, but it only runs on a clean disconnect —
     * a server crash (or force-kill) mid-drill persists {@code SantosPhase.PRE_DRILL}/
     * {@code QUAKE_ACTIVE} (or a mid-demo Reyes phase) in {@link AcademySavedData} with the logout
     * rollback never having run, and the room's tick loop would re-enter the effect the moment the
     * player rejoins: a full-strength earthquake out of nowhere, with no dialogue and no context.
     * Running the same (idempotent, phase-gated) rollback on login closes that path; for players
     * whose logout hook already ran, every step is a no-op.
     */
    static {
        PlayerLifecycleRegistry.registerLogoutHook(player -> {
            cancelDialogue(player);
            clearTransientState(player);
        });
        PlayerLifecycleRegistry.registerLoginHook(AcademyManager::clearTransientState);
        TickScheduler.register(AcademyManager::tick);
    }

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

    /**
     * Blocks left-click/attack on the Academy's purely-decorative gear-display item frames (10 of
     * them, baked into {@code academy_building.schem} alongside their armor stands) — vanilla's
     * default item-frame behavior pops the held item out onto the floor on attack, and unlike
     * Sgt. Reyes's 3 Tool Selection Wall frames ({@link ReyesRoomManager#isToolWallFrame}), nothing
     * ever restocks these, so a bored player emptying one leaves a permanently empty display for
     * every future trainee. Scoped to {@link AcademyBuildingManager#ACADEMY_BOUNDS} so New Sim
     * Building 2.0's ~24 intentionally-poppable wall-mounted extinguisher frames elsewhere on the
     * map are completely unaffected.
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof net.minecraft.world.entity.decoration.ItemFrame frame)) return;
        if (!AcademyBuildingManager.ACADEMY_BOUNDS.contains(frame.position())) return;
        if (ReyesRoomManager.isToolWallFrame(frame.blockPosition())) return;
        event.setCanceled(true);
    }

    /** Called once per server tick from {@code SimulationManager.onServerTick}. */
    public static void tick(ServerLevel level) {
        tickDialogues(level);
        AcademyBuildingManager.sweepStrayCruz(level);
        AcademyGuardrails.tickRescue(level);
        CruzRoomManager.tick(level);
        ReyesRoomManager.tick(level);
        SantosRoomManager.tick(level);
        MorfeRoomManager.tick(level);
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
     * Shared by the {@link PlayerLifecycleRegistry} hooks registered above and
     * {@code /bfp tutorial reset}, which needs the same cleanup while the player stays connected.
     */
    public static void clearTransientState(ServerPlayer player) {
        UUID id = player.getUUID();
        CruzRoomManager.clearPlayer(player);
        ReyesRoomManager.clearPlayer(player);
        SantosRoomManager.clearPlayer(player);
        AcademyVisuals.clearPlayer(id);
        AcademyGuardrails.clearPlayer(id);
    }

    /**
     * Fully (re)starts the Academy for this player: wipes {@link AcademyProgress} back to a fresh
     * start, clears every room manager's transient state, resets Officer Cruz to her briefing
     * anchor, restocks Sgt. Reyes's extinguisher wall, and teleports to Room 1's default viewpoint.
     * Extracted from what {@code BfpAdminCommands}'s {@code /bfp tutorial} (bare/reset forms) did
     * inline, so the main lobby's first button can trigger the exact same "clean run" behavior —
     * pressing either always begins fresh, never resumes a stale mid-progress state.
     */
    public static void startAcademyRun(ServerPlayer player, ServerLevel level) {
        AcademySavedData.get(level).reset(player.getUUID());
        cancelDialogue(player);
        clearTransientState(player);
        CruzRoomManager.resetCruz(level);
        ReyesRoomManager.restockExtinguisherFrames(level);
        // The Academy is a scripted training tour, not a graded hazard — no damage source in it
        // (Reyes's ignite demo, Santos's earthquake drill, stray fall damage) should ever be able
        // to hurt the trainee. SimulationManager.startSimulation clears this the moment a real
        // graded simulation begins, and LobbyManager.routePlayer resets it on every login as a
        // safety net for a player who logged out mid-tutorial.
        player.setInvulnerable(true);
        AcademyBuildingManager.Viewpoint start =
                AcademyBuildingManager.VIEWPOINTS.get(AcademyBuildingManager.DEFAULT_VIEWPOINT);
        player.teleportTo(level, start.x(), start.y(), start.z(),
                java.util.Collections.emptySet(), start.yaw(), start.pitch(), true);
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
    private static final int MAX_LINE_TICKS = 200; // 10s ceiling — text-only lines (no recorded audio) only
    private static final int TICKS_PER_WORD = 5;   // ~0.25s/word reading pace — text-only lines (no recorded audio) only
    /**
     * Guardrail against rapid over-clicking an NPC: a click-driven skip-ahead (the branch in
     * {@link #startOrAdvanceDialogue} below) is only honored once a line has been showing for at
     * least this long, across every room/course that uses this shared sequencer. Without this, a
     * player mashing the interact key could blow through an entire multi-line sequence (and
     * whatever phase transition its {@code onComplete} triggers) within a fraction of a second,
     * skipping content and — for a tick-gated room like Cruz's — visibly racing ahead of what the
     * room's own state machine expected to still be teaching.
     *
     * <p>Only used as a floor for lines with no recorded voice line ({@link AcademyVoiceDurations}
     * returns -1) — a voiced line's skip-gate is its own real audio length instead (see
     * {@link #ticksFor}), so clicking ahead can never cut an instructor off mid-sentence.
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
            AcademyDialogue.DialogueLine currentLine = session.lines.get(session.index);
            long minDisplayTicks = Math.max(MIN_LINE_DISPLAY_TICKS, ticksFor(currentLine));
            if (session.lines == lines && now - session.shownAtTick >= minDisplayTicks) {
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

    /** Outcome of {@link #skipCurrentLine} — distinguishes "nothing to skip" from "won't skip past the end." */
    public enum LineSkipResult { NO_DIALOGUE, ALREADY_LAST_LINE, ADVANCED }

    /**
     * Immediately advances the player's active dialogue by exactly one line's worth of audio/
     * caption — never the sequence's completion. This is deliberately narrower than a re-click of
     * the NPC (see {@link #startOrAdvanceDialogue}): re-clicking on the last line finishes the
     * sequence and fires {@code onComplete}, which is how a phase transition, hazard ignition, or
     * room advance actually happens. A skip command used mid-demo must never trigger that — the
     * presenter is skipping a recording, not asking the room's state machine to move forward — so
     * this refuses ({@link LineSkipResult#ALREADY_LAST_LINE}) instead of completing when already on
     * the last line. Callable without being near the NPC and without the
     * {@link #MIN_LINE_DISPLAY_TICKS} anti-spam floor, since a deliberate admin command is never an
     * accidental button-mash. Stops whatever VOICE-channel sound is currently playing before
     * advancing, so the skipped line's audio never overlaps the next one.
     */
    public static LineSkipResult skipCurrentLine(ServerPlayer player) {
        DialogueSession session = activeSessions.get(player.getUUID());
        if (session == null) return LineSkipResult.NO_DIALOGUE;
        if (session.index + 1 >= session.lines.size()) return LineSkipResult.ALREADY_LAST_LINE;
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.VOICE));
        session.index++;
        playCurrentLine(player, session);
        return LineSkipResult.ADVANCED;
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
        session.nextAdvanceTick = now + ticksFor(line);
    }

    /**
     * How long to keep a line on screen/audible before auto-advancing (and the minimum a player
     * must wait before a click-skip is honored, see {@link #startOrAdvanceDialogue}). Uses the
     * real recorded voice line's length ({@link AcademyVoiceDurations}) whenever one exists, so the
     * instructor always finishes speaking before the next line's stop-sound can cut them off —
     * falls back to the old word-count reading-pace estimate only for lines with no recorded audio
     * (e.g. {@code MorfeRoomManager}'s runtime-built debrief lines).
     */
    private static int ticksFor(AcademyDialogue.DialogueLine line) {
        int voiceTicks = AcademyVoiceDurations.ticksFor(line.soundKey());
        return voiceTicks >= 0 ? voiceTicks : ticksFor(line.text());
    }

    /** Word-count reading-pace estimate — used directly by {@link #sendPrompt} (plain-text captions with no {@code DialogueLine}/soundKey at all, e.g. room managers' idle nudges and banners), and as the {@link #ticksFor(AcademyDialogue.DialogueLine)} fallback for lines with no recorded voice line. */
    private static int ticksFor(String text) {
        int words = text.trim().isEmpty() ? 1 : text.trim().split("\\s+").length;
        return Math.max(MIN_LINE_TICKS, Math.min(MAX_LINE_TICKS, words * TICKS_PER_WORD));
    }
}
