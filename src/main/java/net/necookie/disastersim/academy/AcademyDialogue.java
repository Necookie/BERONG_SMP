package net.necookie.disastersim.academy;

import java.util.List;
import java.util.Map;

/**
 * Dialogue lines for Officer Cruz, Sgt. Reyes, and Sgt. Santos, keyed by the player's current
 * phase in that NPC's room. Pattern-cloned from {@code tutorial/NpcDialogue}'s
 * {@code DialogueLine}/{@code Map<Stage, List<DialogueLine>>} shape, but per-room instead of one
 * shared global stage. Full script and voice notes: {@code docs/new_tutorial_script.md}.
 *
 * <p>Capt. Morfe has no entry here — his evaluation line is score-dependent (built at runtime by
 * {@code room4.AcademyScoring}/{@code room4.MorfeRoomManager}), not static canned text.
 *
 * <p>{@code advancesPhase} only applies to the handful of transitions dialogue itself gates (e.g.
 * a room's opening speech ending and sending the player off to do something). Movement/condition
 * gated transitions (reaching the 4 green marks, clearing the maze, holding through the quake,
 * ...) are driven by each room's own tick logic instead, independent of whether the player has
 * talked to the NPC again.
 */
public final class AcademyDialogue {

    public record DialogueLine(String text, boolean advancesPhase, String soundKey) {
        public DialogueLine(String text, boolean advancesPhase) {
            this(text, advancesPhase, null);
        }
    }

    // ── Officer Cruz — Movement School ──────────────────────────────────────

    public static final Map<CruzPhase, List<DialogueLine>> CRUZ_LINES = Map.of(
        CruzPhase.NOT_STARTED, List.of(
            new DialogueLine("§a[Officer Cruz] §fWelcome to the Academy, trainee! I'm Officer Cruz, and before we run, jump, or duck for anything — we start with the basics that keep you in control.", false),
            new DialogueLine("§a[Officer Cruz] §fFirst, your eyes! Move your mouse to look around. Get comfortable turning your head left, right, up, and down — you'll need to spot hazards fast in a real emergency.", false),
            new DialogueLine("§a[Officer Cruz] §fNow for your feet — §eW-A-S-D§f moves you: W forward, S back, A left, D right. See those four green marks on the floor? Walk to each one. Take your time, there's no rush yet!", false),
            new DialogueLine("§a[Officer Cruz] §fOnce you've stepped on all four, come find me again and we'll move on to something trickier.", true)
        ),
        CruzPhase.BRIEFING, List.of(
            new DialogueLine("§a[Officer Cruz] §7Still looking for the marks? They're glowing green on the floor — follow the arrows if you need a hand!", false)
        ),
        CruzPhase.MAZE, List.of(
            new DialogueLine("§a[Officer Cruz] §7Bumping into walls slows you down in a real evacuation! Look at the green arrows and turn your camera before you turn your feet.", false)
        ),
        CruzPhase.JUMP, List.of(
            new DialogueLine("§a[Officer Cruz] §7Keep your momentum! Walk straight at it and tap Spacebar right before you'd hit it.", false)
        ),
        CruzPhase.GOSTOP_STAGE, List.of(
            new DialogueLine("§a[Officer Cruz] §fLast lesson, and it's the most important one — knowing when to §eGO§f and when to §cSTOP§f. Low slabs ahead mean you'll need to hold §eShift§f to crouch and slip under them.", false),
            new DialogueLine("§a[Officer Cruz] §fHere's the drill: when I call §a§lGO!§r§f, keep moving forward. The instant I call §c§lSTOP!§r§f, freeze right where you are — real emergencies have moments where rushing forward is exactly the wrong move. Ready? Here we go!", true)
        ),
        CruzPhase.GOSTOP_RUN, List.of(
            new DialogueLine("§a[Officer Cruz] §7Stay sharp — GO when I say go, STOP when I say stop!", false)
        ),
        CruzPhase.DONE, List.of(
            new DialogueLine("§a[Officer Cruz] §fOutstanding! You've got sharp eyes, steady feet, and you know how to freeze on command. Follow the green floor arrows to Sgt. Reyes for your Fire Safety Drill!", false)
        )
    );

    // ── Sgt. Reyes — Fire Safety Drill ──────────────────────────────────────

    public static final Map<ReyesPhase, List<DialogueLine>> REYES_LINES = Map.of(
        ReyesPhase.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fWelcome, trainee! I'm Sergeant Reyes, and fire doesn't care about your feelings — so we're going to learn exactly which tool beats which fire.", false),
            new DialogueLine("§6[Sgt. Reyes] §fSee those three extinguishers on the wall? §cRed§f is your ABC extinguisher, for ordinary stuff like paper and wood. §aGreen§f is CO2, for electrical fires — never water, never foam, just CO2! §eYellow§f is wet chemical, for kitchen grease and cooking oil fires.", false),
            new DialogueLine("§6[Sgt. Reyes] §fGrab all three off the wall. You'll need each one in a minute — I'm about to show you three real hazards, and only the right tool will put each one out!", true)
        ),
        ReyesPhase.TOOL_SELECTION, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Go ahead, take them off the wall! Walk up and click on each one.", false)
        ),
        ReyesPhase.LIVE_FIRE_DEMO, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fHere we go — three real hazards, right in front of you! Remember: PULL the pin, AIM low at the base, SQUEEZE the handle, SWEEP side to side. Match the right extinguisher to the right fire!", false)
        ),
        ReyesPhase.DONE, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fYou've mastered all three fire classes and how to handle catching fire yourself. Head over to Sgt. Santos for the earthquake drill — you're doing amazing!", false)
        )
    );

    // ── Sgt. Santos — Earthquake Drill ───────────────────────────────────────

    public static final Map<SantosPhase, List<DialogueLine>> SANTOS_LINES = Map.of(
        SantosPhase.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Santos] §fGreat work in there! I'm Sergeant Santos, and earthquakes strike without any warning at all — no countdown, no siren first. That's exactly why we drill for it.", false),
            new DialogueLine("§6[Sgt. Santos] §fSee that reinforced table glowing green? That's your designated safe zone. When the ground starts shaking, that's exactly where you're headed.", false),
            new DialogueLine("§6[Sgt. Santos] §fRemember the three words: §e§lDROP — COVER — HOLD ON!§r§f Drop low so you don't fall. Get cover under something solid. Hold on and stay there until the shaking stops completely.", false),
            new DialogueLine("§6[Sgt. Santos] §fReady? I'm starting the drill now — brace yourself!", true)
        ),
        SantosPhase.PRE_DRILL, List.of(
            new DialogueLine("§6[Sgt. Santos] §7Get ready — the ground won't stay still much longer!", false)
        ),
        SantosPhase.QUAKE_ACTIVE, List.of(
            new DialogueLine("§6[Sgt. Santos] §7Get back under the table! The ground is still moving!", false)
        ),
        SantosPhase.DONE, List.of(
            new DialogueLine("§6[Sgt. Santos] §fThe shaking has stopped. Good composure out there — that's exactly how it's done. Proceed to Captain Morfe for your final debrief.", false)
        )
    );

    private AcademyDialogue() {}
}
