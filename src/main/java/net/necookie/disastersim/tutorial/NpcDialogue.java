package net.necookie.disastersim.tutorial;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

/** Dialogue lines for each BFP NPC role, keyed by the player's current tutorial stage. */
public final class NpcDialogue {

    public record DialogueLine(String text, boolean advancesStage, String soundKey) {
        /** Convenience constructor for lines without audio (sound played separately). */
        public DialogueLine(String text, boolean advancesStage) {
            this(text, advancesStage, null);
        }
    }

    // ── Sgt. Reyes — Entry Hall ──────────────────────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> TRAINER_LINES = Map.ofEntries(
        entry(TutorialStage.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fHi there! Welcome to the BFP Fire Training Center! My name is Sergeant Reyes, and I am going to teach you how to stay safe!", false, "npc.sgt_reyes.sgt_reyes_0"),
            new DialogueLine("§6[Sgt. Reyes] §fDid you know that fire can spread really, really fast — even faster than you can run? That is why we need to know what to do BEFORE a fire happens!", false, "npc.sgt_reyes.sgt_reyes_1"),
            new DialogueLine("§6[Sgt. Reyes] §fToday we are going to learn how to use a fire extinguisher! It is a special tool that sprays something to put fires out.", false, "npc.sgt_reyes.sgt_reyes_2"),
            new DialogueLine("§6[Sgt. Reyes] §fWe follow 4 easy steps called §eP-A-S-S§f! §eP§f is for PULL the pin — like pulling a ring. §eA§f is for AIM — always point it LOW at the bottom of the fire, not the top!", false, "npc.sgt_reyes.sgt_reyes_3"),
            new DialogueLine("§6[Sgt. Reyes] §e§lS§r§f is for SQUEEZE the handles together — that makes it spray! And the last §e§lS§r§f is for SWEEP — move it slowly side to side, like you are wagging a dog's tail!", false, "npc.sgt_reyes.sgt_reyes_4"),
            new DialogueLine("§6[Sgt. Reyes] §fI am going to give you a real extinguisher now! Go to the practice area and put out all 3 fires using PASS. You can do it — I believe in you!", true, "npc.sgt_reyes.sgt_reyes_5")
        )),
        entry(TutorialStage.PASS_PULL, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fHey, welcome back! Quick reminder — we use P-A-S-S to fight fire!", false, "npc.sgt_reyes.sgt_reyes_refreshener_0"),
            new DialogueLine("§6[Sgt. Reyes] §fPULL the pin, AIM low at the base of the fire, SQUEEZE the handles, and SWEEP side to side. Easy peasy!", false, "npc.sgt_reyes.sgt_reyes_refreshener_1"),
            new DialogueLine("§6[Sgt. Reyes] §fHere is your extinguisher! Go put out those practice fires — you have got this!", true, "npc.sgt_reyes.sgt_reyes_refreshener_2")
        )),
        entry(TutorialStage.PASS_SPRAY, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7You are doing great! Remember — aim LOW at the bottom of the fire, not at the flames on top. Keep sweeping left and right!", false, "npc.sgt_reyes.sgt_reyes_spray_0")
        )),
        entry(TutorialStage.EXT_TYPE_A, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Great work on the fires! Go talk to §aOfficer Cruz§7 — she will teach you about the different types of fire extinguishers!", false, "npc.sgt_reyes.sgt_reyes_redirect_a")
        )),
        entry(TutorialStage.EXT_TYPE_B, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Keep going! §aOfficer Cruz§7 has more important things to teach you — go talk to her!", false, "npc.sgt_reyes.sgt_reyes_redirect_b")
        )),
        entry(TutorialStage.EXT_TYPE_C, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Almost done with the fire lessons! Go finish up with §aOfficer Cruz§7 for the last class.", false, "npc.sgt_reyes.sgt_reyes_redirect_c")
        )),
        entry(TutorialStage.QUAKE_INTRO, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Now head over to §cCaptain Santos§7 in the earthquake zone — she will teach you what to do when the ground shakes!", false, "npc.sgt_reyes.sgt_reyes_redirect_quake")
        )),
        entry(TutorialStage.QUAKE_DROP, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Stay in the drill! Press [SHIFT] to DROP!", false, "npc.sgt_reyes.sgt_reyes_redirect_quake")
        )),
        entry(TutorialStage.QUAKE_COVER, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Stay in the drill! Get under something solid and keep crouching!", false, "npc.sgt_reyes.sgt_reyes_redirect_cover")
        )),
        entry(TutorialStage.QUAKE_HOLDON, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Hold tight! Stay crouched under that cover until the shaking stops!", false, "npc.sgt_reyes.sgt_reyes_redirect_holdon")
        )),
        entry(TutorialStage.COMPLETED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fYou finished all the training! Go to the main lobby to start the real disaster simulation. I am so proud of you!", false, "npc.sgt_reyes.sgt_reyes_done")
        ))
    );

    // ── Officer Cruz — Extinguisher Types Room ───────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> EXT_EXPERT_LINES = Map.of(
        TutorialStage.EXT_TYPE_A, List.of(
            new DialogueLine("§a[Officer Cruz] §fAmazing job putting out those fires! I am Officer Cruz, and I will teach you about the three types of fires and which extinguisher to use for each one!", false, "npc.sgt_cruz.cruz_a_0"),
            new DialogueLine("§a[Officer Cruz] §fHere is a very important secret — §lnot all fires are the same!§r§f Using the WRONG extinguisher can actually make the fire BIGGER and more dangerous. So listen carefully!", false, "npc.sgt_cruz.cruz_a_1"),
            new DialogueLine("§a[Officer Cruz] §e§lClass A §r§a— Ordinary Things That Burn! §fClass A fires happen when everyday things catch fire — like paper, wood, cloth, cardboard, and plastic.", false, "npc.sgt_cruz.cruz_a_2"),
            new DialogueLine("§a[Officer Cruz] §fThink about it — if your notebook, a wooden chair, or your school bag caught fire, that would be a Class A fire! It is the most common type of fire in homes and schools.", false, "npc.sgt_cruz.cruz_a_3"),
            new DialogueLine("§a[Officer Cruz] §fFor Class A fires, you can use a water extinguisher, a foam extinguisher, or a special one called the ABC extinguisher. The ABC one is the big red one you usually see hanging on walls!", false, "npc.sgt_cruz.cruz_a_4"),
            new DialogueLine("§a[Officer Cruz] §fThe ABC extinguisher is like the superhero of extinguishers — it can fight Class A, B, AND C fires all by itself! Do you understand Class A now?", true, "npc.sgt_cruz.cruz_a_5")
        ),
        TutorialStage.EXT_TYPE_B, List.of(
            new DialogueLine("§a[Officer Cruz] §e§lClass B §r§a— Fires From Liquids and Gases! §fClass B fires happen when liquids or gases that can burn catch fire — like gasoline, cooking oil, diesel, or gas from a tank.", false, "npc.sgt_cruz.cruz_b_0"),
            new DialogueLine("§a[Officer Cruz] §fThink about the fuel in a car, or the cooking oil your parents use in the kitchen — if those caught fire, that would be a Class B fire! These fires spread very, very fast!", false, "npc.sgt_cruz.cruz_b_1"),
            new DialogueLine("§a[Officer Cruz] §c§lVERY IMPORTANT: NEVER use water on a Class B fire! §r§fIf you throw water on burning oil, it will EXPLODE and the fire will jump and spread everywhere — making it much worse!", false, "npc.sgt_cruz.cruz_b_2"),
            new DialogueLine("§a[Officer Cruz] §fInstead, use a CO2 extinguisher or a dry chemical ABC extinguisher. These work by removing the oxygen that the fire needs to keep burning — like putting a blanket over it!", false, "npc.sgt_cruz.cruz_b_3"),
            new DialogueLine("§a[Officer Cruz] §fAim at the near edge of the fire closest to you and sweep slowly towards the far side. Always keep a safe distance! Remember — NO water on Class B fires. Got it?", true, "npc.sgt_cruz.cruz_b_4")
        ),
        TutorialStage.EXT_TYPE_C, List.of(
            new DialogueLine("§a[Officer Cruz] §e§lClass C §r§a— Electrical Fires! §fClass C fires happen when electricity is involved — like a broken wire, an overloaded plug, a computer, a TV, or an electrical board.", false, "npc.sgt_cruz.cruz_c_0"),
            new DialogueLine("§a[Officer Cruz] §fThink about the wires inside your walls, your phone charger, or the electric panel in your house — if any of those sparked and caught fire, that is a Class C fire!", false, "npc.sgt_cruz.cruz_c_1"),
            new DialogueLine("§a[Officer Cruz] §c§l⚠ THIS IS THE MOST DANGEROUS CLASS! NEVER EVER use water on a Class C fire! §r§fWater carries electricity — if you pour water on an electrical fire, the electricity can travel through the water and give you a deadly electric shock!", false, "npc.sgt_cruz.cruz_c_2"),
            new DialogueLine("§a[Officer Cruz] §fEven a tiny bit of water near an electrical fire can seriously hurt you or someone nearby. So this is the most important rule — §lNO WATER on Class C fires. EVER. No exceptions!", false, "npc.sgt_cruz.cruz_c_3"),
            new DialogueLine("§a[Officer Cruz] §fOnly use a CO2 extinguisher or a dry chemical extinguisher on Class C fires. These are completely safe because they do not carry electricity at all!", false, "npc.sgt_cruz.cruz_c_4"),
            new DialogueLine("§a[Officer Cruz] §fHere is one more tip — if it is safe to do so, try to turn off the electricity first by flipping the switch or unplugging the device. That makes the fire much easier to fight!", false, "npc.sgt_cruz.cruz_c_5"),
            new DialogueLine("§a[Officer Cruz] §fYou are a star! You learned all three classes — §eClass A§a for ordinary things, §eClass B§a for liquids, §eClass C§a for electricity. Now go see §cCaptain Santos§a for the earthquake drill — you are almost done!", true, "npc.sgt_cruz.cruz_c_6")
        )
    );

    // ── Capt. Santos — Earthquake Drill Zone ─────────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> SAFETY_OFFICER_LINES = Map.of(
        TutorialStage.QUAKE_INTRO, List.of(
            new DialogueLine("§c[Capt. Santos] §fGreat work on all the fire training! I am Captain Santos, and I am here to teach you what to do when the ground starts shaking — an earthquake!", false, "npc.sgt_santos.santos_0"),
            new DialogueLine("§c[Capt. Santos] §fThe Philippines is located in a very special — but also very dangerous — place on Earth where big pieces of the ground called tectonic plates push against each other and cause the ground to shake!", false, "npc.sgt_santos.santos_1"),
            new DialogueLine("§c[Capt. Santos] §fEvery year, the Philippines has more than 2,000 earthquakes! Most are too small to feel, but some are very strong and can knock down buildings, break furniture, and hurt people.", false, "npc.sgt_santos.santos_2"),
            new DialogueLine("§c[Capt. Santos] §fThe most important thing to remember during an earthquake is three words: §e§lDROP — COVER — HOLD ON! §r§fFirst you DROP low to the ground so you do not fall over. Then you get COVER under something strong like a table.", false, "npc.sgt_santos.santos_3"),
            new DialogueLine("§c[Capt. Santos] §fI am starting the drill right now! When you feel the shaking — SHIFT to crouch and DROP immediately! Then move under a solid block for COVER and HOLD ON until it stops. Ready? Here we go!", true, "npc.sgt_santos.santos_4")
        ),
        TutorialStage.COMPLETED, List.of(
            new DialogueLine("§c[Capt. Santos] §f§lWow! Outstanding work, trainee! You are amazing!", false, "npc.sgt_santos.santos_done_0"),
            new DialogueLine("§c[Capt. Santos] §fToday you learned how to use a fire extinguisher the right way, you learned which extinguisher to use for Class A, B, and C fires, AND you practiced DROP, COVER, HOLD ON for earthquakes!", false, "npc.sgt_santos.santos_done_1"),
            new DialogueLine("§c[Capt. Santos] §fAlways remember — in a real emergency, do NOT panic. Take a deep breath, think about what you practiced today, and do your steps. Even kids like you can save lives!", false, "npc.sgt_santos.santos_done_2"),
            new DialogueLine("§c[Capt. Santos] §fThe BFP is very, very proud of you! You are now a certified BFP Junior Trainee! Head back to the main lobby and show everyone what you have learned. Stay safe out there — dismissed!", true, "npc.sgt_santos.santos_done_3")
        )
    );

    private NpcDialogue() {}
}
