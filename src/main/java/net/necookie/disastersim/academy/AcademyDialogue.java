package net.necookie.disastersim.academy;

import java.util.List;
import java.util.Map;

/**
 * Dialogue lines for Officer Cruz, Sgt. Reyes, Sgt. Santos, and Capt. Morfe, keyed by the player's
 * current phase in that NPC's room. Pattern-cloned from {@code tutorial/NpcDialogue}'s
 * {@code DialogueLine}/{@code Map<Stage, List<DialogueLine>>} shape, but per-room instead of one
 * shared global stage. Full script and voice notes: {@code docs/new_tutorial_script.md}.
 *
 * <p>Voice rules (non-gamer friendly): the exact key/button is named in §e in every instruction
 * ("hold the §eW key§f", "press and hold §eShift§f"), one clear action per line, warm and
 * reassuring — many students have never played Minecraft before.
 *
 * <p>Capt. Morfe now has static entries too ({@link #MORFE_LINES}, {@link #MORFE_PASS_LINES},
 * {@link #MORFE_FAIL_LINES}) — only the score number itself is built at runtime by
 * {@code room4.MorfeRoomManager}/{@code room4.AcademyScoring}.
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
            new DialogueLine("§a[Officer Cruz] §fHi there, trainee — welcome to the Academy! I'm Officer Cruz, and I'll walk with you every step of the way. First time playing? That's perfectly fine — we'll go slow and easy.", false),
            new DialogueLine("§a[Officer Cruz] §fLet's start with your eyes. Move your §emouse§f gently to look around — left, right, up, down. Try it right now! Looking around calmly is the very first emergency skill.", false),
            new DialogueLine("§a[Officer Cruz] §fNow your feet! Hold the §eW key§f to walk forward, the §eS key§f to back up, and §eA§f and §eD§f to step left and right. See the four §abright green tiles§f on the floor? Walk onto each one — I'll come with you!", false),
            new DialogueLine("§a[Officer Cruz] §fTake all the time you need. Once you've stood on all four green tiles, we'll move on together. Off you go — I'm right behind you!", true)
        ),
        CruzPhase.BRIEFING, List.of(
            new DialogueLine("§a[Officer Cruz] §7Looking for the green tiles? Watch for the tall beam of green light — that's your next one. Hold the §eW key§f and walk toward it. You've got this!", false)
        ),
        CruzPhase.MAZE, List.of(
            new DialogueLine("§a[Officer Cruz] §7Walls in the way? That's okay! Move your §emouse§7 to look down the open path first, THEN hold §eW§7 to walk. Look first, walk second.", false)
        ),
        CruzPhase.JUMP, List.of(
            new DialogueLine("§a[Officer Cruz] §7To hop a hurdle: keep holding §eW§7 and tap the §eSpacebar§7 just before you reach it. Missed one? No problem — back up and try again!", false)
        ),
        CruzPhase.GOSTOP_STAGE, List.of(
            new DialogueLine("§a[Officer Cruz] §fLast lesson — my favorite: knowing when to §aGO§f and when to §cSTOP§f. See the low boards in the tunnel ahead? Press and hold §eShift§f to crouch, and you'll slip right under them while you walk.", false),
            new DialogueLine("§a[Officer Cruz] §fHere's how it works: when I call §a§lGO!§r§f, hold §eW§f and keep walking. When I call §c§lSTOP!§r§f, let go of every key and stand perfectly still. In a real emergency, stopping at the right moment keeps you safe. Ready? Let's go!", true)
        ),
        CruzPhase.GOSTOP_RUN, List.of(
            new DialogueLine("§a[Officer Cruz] §7Listen for my call! §aGO§7 means walk (hold §eW§7). §cSTOP§7 means let go of all the keys and freeze.", false)
        ),
        CruzPhase.DONE, List.of(
            new DialogueLine("§a[Officer Cruz] §fYou did it! You can look, walk, jump, crouch, and stop on command — that's everything you need. Now follow the glowing arrow to §6Sgt. Reyes§f for the Fire Safety Drill. She's friendly, I promise!", false)
        )
    );

    // ── Sgt. Reyes — Fire Safety Drill ──────────────────────────────────────

    public static final Map<ReyesPhase, List<DialogueLine>> REYES_LINES = Map.of(
        ReyesPhase.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fHello, trainee — great to meet you! I'm Sergeant Reyes. Today's lesson has three parts, in this order: §aprevention§f first, §eintervention§f second, and §cevacuation§f last — because §a§lpreventing a fire beats fighting one, every single time§r§f.", false),
            new DialogueLine("§6[Sgt. Reyes] §fLook around this room — a few everyday hazards, just like you'd see in a real building. A frayed cord, a stack of forgotten boxes, an unattended pan. Most fires never have to happen at all if someone catches them early.", false),
            new DialogueLine("§6[Sgt. Reyes] §fLet's practice that first — spotting a hazard §ebefore§f it becomes a fire, and fixing it with nothing but your own two hands. Follow me!", true)
        ),
        ReyesPhase.PREVENTION_DEMO, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Right-click a glowing hazard to fix it — that's prevention. Work through all three before they get away from you!", false)
        ),
        ReyesPhase.TOOL_SELECTION, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Time to gear up — follow the glowing arrow to each extinguisher and I'll walk you through grabbing it.", false)
        ),
        ReyesPhase.LIVE_FIRE_DEMO, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7To use an extinguisher: press its §enumber key (1-9)§7 to hold it, aim at the §ebase§7 of the fire, and §ehold the right mouse button§7. Match the color to what's burning — you can do it!", false)
        ),
        ReyesPhase.ALARM_CHECKPOINT, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fWell fought — but here's the one step people forget: the §cmoment§f a fire starts, before anything else, §c§lring the alarm§r§f. It warns everyone else in the building, even if you already put the fire out.", false),
            new DialogueLine("§6[Sgt. Reyes] §fFollow the glowing arrow to the fire alarm and press it. Once it's ringing, come straight back to me.", false)
        ),
        ReyesPhase.EVACUATION_BRIEF, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fPerfect — alarm's stopped, and everyone's been warned. Now for the very last step, and it's the simplest: once that alarm is ringing, §cyou evacuate§f. Don't grab your things, don't go back for anything — walk calmly to the nearest exit.", false),
            new DialogueLine("§6[Sgt. Reyes] §fPrevention, intervention, evacuation — in that order, every time. You've got all three now. I'm proud of you, trainee!", true)
        ),
        ReyesPhase.DONE, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fThree fires, three perfect matches — and you even put yourself out safely. I'm proud of you! Follow the glowing arrow to §6Sgt. Santos§f for the Earthquake Drill.", false)
        )
    );

    /**
     * Per-frame "point, then teach" lines for {@code ReyesPhase#TOOL_SELECTION} — index matches
     * {@code room2.ReyesRoomManager.EXTINGUISHER_FRAMES}'s order (0=ABC red, 1=CO2 green, 2=wet
     * chemical yellow). The first line points the player at that frame specifically (the compass
     * and beacon are already up by the time this plays); the second teaches the pop-off-the-wall
     * pickup mechanic. Played once per frame, the instant it becomes the player's current target,
     * instead of one generic line covering all three at once.
     */
    public static final List<List<DialogueLine>> REYES_TOOL_LINES = List.of(
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fFirst, the §cRED ABC extinguisher§f — see it glowing in its frame? Follow the arrow over.", false),
            new DialogueLine("§6[Sgt. Reyes] §fLeft-click the frame to pop it loose, then walk over the extinguisher on the floor to pick it up.", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fNext, the §aGREEN CO2 extinguisher§f — for electrical fires. Follow the arrow to its frame.", false),
            new DialogueLine("§6[Sgt. Reyes] §fSame as before: §eleft-click§f to pop it off the wall, then step onto it to grab it.", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fLast one — the §eYELLOW wet chemical extinguisher§f, for kitchen grease fires. Follow the arrow.", false),
            new DialogueLine("§6[Sgt. Reyes] §fLeft-click the frame, then scoop it up off the floor. That's all three — you're fully equipped!", true)
        )
    );

    /**
     * Played once, before {@code ReyesRoomManager.beginIgniteDemo} actually sets the player alight —
     * teaches the "stop, drop, and roll" mechanic and its controls up front, so the player already
     * knows what's about to happen and exactly how to respond instead of reading instructions for
     * the first time while already on fire.
     */
    public static final List<DialogueLine> REYES_IGNITE_LINES = List.of(
        new DialogueLine("§6[Sgt. Reyes] §fOne last lesson, and it's an important one: even when you do everything right, sometimes your clothes still catch fire — a stray spark, hot oil, anything. Don't panic when that happens.", false),
        new DialogueLine("§6[Sgt. Reyes] §fThe only way to put yourself out is §e§lSTOP, DROP, and ROLL§r§f: press and hold §eShift§f to drop down low, then press §eR§f to roll — keep pressing it until the flames are gone. Nothing else works, so don't waste time trying to swat it out or run for water.", false),
        new DialogueLine("§6[Sgt. Reyes] §fReady? I'm going to light a small, completely safe spark on you now, just like everything else here. The instant you catch fire — §eShift§f, then §eR§f!", true)
    );

    /**
     * Per-hazard "prevention" explanations for {@code ReyesPhase#PREVENTION_DEMO} — index 0 = Class
     * A (archive boxes), 1 = electrical, 2 = kitchen, matching {@code room2.ReyesRoomManager.HAZARDS}'s
     * fixed order. Each explains the everyday habit that stops the hazard from ever igniting; the
     * player then bare-hand right-clicks the (merely hazardous, not yet on fire) prop to fix it.
     */
    public static final List<List<DialogueLine>> REYES_PREVENTION_LINES = List.of(
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fSee that stack of boxes starting to spark and smoke? Loose paper piled near anything warm is an easy Class A fire waiting to happen.", false),
            new DialogueLine("§6[Sgt. Reyes] §fRight-click it now — pretend you're clearing it away from the heat source and stacking it properly. That's it. That's the whole fix.", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fThat computer's sparking — a damaged cord or an overloaded outlet, left plugged in and unwatched.", false),
            new DialogueLine("§6[Sgt. Reyes] §fRight-click it — you're unplugging it and setting it aside for a real repair. Never wait on a sparking cord.", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fAnd that pan — oil left heating with nobody watching it. Kitchen fires start this way more than almost anything else.", false),
            new DialogueLine("§6[Sgt. Reyes] §fRight-click it — you're pulling it off the heat before it ever catches. Never leave hot oil unattended, even for a minute.", true)
        )
    );

    /**
     * Per-hazard explanation lines for Sgt. Reyes's sequential Room 2 intervention teaching — index
     * 0 = Class A, 1 = electrical, 2 = kitchen, matching {@code room2.ReyesRoomManager.HAZARDS}'s
     * fixed order. Each explains what's on fire, which extinguisher, and exactly how to use it,
     * before that hazard ignites (the ignition is triggered by this sequence's completion, not a
     * separate click).
     */
    public static final List<List<DialogueLine>> REYES_HAZARD_LINES = List.of(
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fSometimes prevention doesn't catch it in time — like now. That stack of paper boxes just caught fire! Paper and cardboard are \"Class A\": ordinary materials.", false),
            new DialogueLine("§6[Sgt. Reyes] §fPress your §cRED ABC extinguisher§f's number key to hold it, walk close, aim at the §ebase§f of the fire, and §ehold the right mouse button§f. Sweep side to side!", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fNext — that computer is sparking and now it's alight! That's an electrical fire, \"Class C\".", false),
            new DialogueLine("§6[Sgt. Reyes] §cNever use water or foam on electrical fires§f — electricity can travel back to you! Hold your §aGREEN CO2 extinguisher§f, get close, and §ehold the right mouse button§f to put it out.", true)
        ),
        List.of(
            new DialogueLine("§6[Sgt. Reyes] §fLast one — that pan of cooking oil just caught fire! Grease fires are special: the wrong extinguisher can make them splash and spread.", false),
            new DialogueLine("§6[Sgt. Reyes] §fHold your §eYELLOW wet chemical extinguisher§f, aim at the pan, and §ehold the right mouse button§f. It cools the oil and seals it safely. Nice and steady!", true)
        )
    );

    // ── Sgt. Santos — Earthquake Drill ───────────────────────────────────────

    public static final Map<SantosPhase, List<DialogueLine>> SANTOS_LINES = Map.of(
        SantosPhase.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Santos] §fWell done getting this far! I'm Sergeant Santos. Earthquakes are different from fires — they give §cno warning at all§f. So we practice until the right move is automatic.", false),
            new DialogueLine("§6[Sgt. Santos] §fA few things to know before the ground even moves: stay §caway from windows and glass§f — they shatter. Stay clear of tall shelves or anything heavy that isn't bolted down — it can tip.", false),
            new DialogueLine("§6[Sgt. Santos] §fAnd whatever you do, §cdon't run outside mid-shake§f, and never use an elevator during or right after — stairs only, once it's safe to move at all.", false),
            new DialogueLine("§6[Sgt. Santos] §fSee that sturdy table glowing green? That's your safe spot. When the shaking starts, walk under it — hold the §eW key§f to move, then press and hold §eShift§f to crouch down low.", false),
            new DialogueLine("§6[Sgt. Santos] §fRemember three words: §e§lDROP — COVER — HOLD ON!§r§f DROP down low (hold §eShift§f), take COVER under the table, and HOLD ON — stay there until the shaking completely stops.", false),
            new DialogueLine("§6[Sgt. Santos] §fOne more thing — real quakes often have §caftershocks§f. Don't jump up the second the first shake fades; hold your position a little longer, just in case.", false),
            new DialogueLine("§6[Sgt. Santos] §fReady to try? The floor is about to shake — it's only practice, you're completely safe. Head for that glowing table the moment it starts!", true)
        ),
        SantosPhase.PRE_DRILL, List.of(
            new DialogueLine("§6[Sgt. Santos] §7Any second now — keep your eyes on that glowing green table!", false),
            new DialogueLine("§6[Sgt. Santos] §7Remember: away from windows, away from anything tall and heavy — straight to the table.", false)
        ),
        SantosPhase.QUAKE_ACTIVE, List.of(
            new DialogueLine("§6[Sgt. Santos] §7Get under the glowing table! Hold §eW§7 to walk there, then press and hold §eShift§7 to crouch — and stay put!", false)
        ),
        SantosPhase.DONE, List.of(
            new DialogueLine("§6[Sgt. Santos] §fAnd... the shaking has stopped. You dropped, covered, and held on like a pro! In a real quake you'd still watch for aftershocks and check the path out before moving.", false),
            new DialogueLine("§6[Sgt. Santos] §fOne last stop: follow the glowing arrow to §cCaptain Morfe§f for your results. Stand tall — you've earned it. Congratulations, trainee!", false)
        )
    );

    // ── Capt. Morfe — Evaluation ─────────────────────────────────────────────

    public static final Map<MorfePhase, List<DialogueLine>> MORFE_LINES = Map.of(
        MorfePhase.NOT_STARTED, List.of(
            new DialogueLine("§c[Capt. Morfe] §fWelcome, trainee — I'm Captain Morfe, and it's an honor to meet you. You've walked with Cruz, fought fires with Reyes, and held steady through Santos's earthquake.", false),
            new DialogueLine("§c[Capt. Morfe] §fEvery instructor sent me their notes, and I've put your full record together. Take a breath — let's see how you did.", false)
        ),
        MorfePhase.EVALUATED_PASS, List.of(
            new DialogueLine("§c[Capt. Morfe] §fYou're already certified, trainee — no need to prove anything twice! Head out and enjoy the simulations. Stay sharp out there!", false)
        )
    );

    /** Played after the score printout when the player passes; the certification moment. */
    public static final List<DialogueLine> MORFE_PASS_LINES = List.of(
        new DialogueLine("§c[Capt. Morfe] §fSteady feet, the right extinguisher every time, and calm under a shaking roof. That's a passing record — §a§lyou are officially certified!§r", false),
        new DialogueLine("§c[Capt. Morfe] §fBe proud of yourself — what you practiced today protects real people in real life. You're cleared for the full simulations. Congratulations, trainee!", false)
    );

    /** Played after the score printout on a fail; the retry reset fires once these finish. */
    public static final List<DialogueLine> MORFE_FAIL_LINES = List.of(
        new DialogueLine("§c[Capt. Morfe] §fYou're not quite there yet — and that is completely okay. Even our best firefighters ran these drills more than once.", false),
        new DialogueLine("§c[Capt. Morfe] §fI'm sending you back to Officer Cruz for another practice run. Take it slow, listen to each instructor, and I'll be waiting right here to certify you. You can do this!", false)
    );

    private AcademyDialogue() {}
}
