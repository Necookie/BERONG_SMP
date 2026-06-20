package net.necookie.disastersim.tutorial;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

/** Dialogue lines for each BFP NPC role, keyed by the player's current tutorial stage. */
public final class NpcDialogue {

    public record DialogueLine(String text, boolean advancesStage) {}

    // ── Sgt. Reyes — Entry Hall ──────────────────────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> TRAINER_LINES = Map.ofEntries(
        entry(TutorialStage.NOT_STARTED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fWelcome to the BFP National Fire Training Center!", false),
            new DialogueLine("§6[Sgt. Reyes] §fI'm Sergeant Reyes, your lead fire safety instructor.", false),
            new DialogueLine("§6[Sgt. Reyes] §fThe Philippines is one of the most fire-prone countries in Asia.", false),
            new DialogueLine("§6[Sgt. Reyes] §fKnowing how to respond in the first 3 minutes saves lives.", false),
            new DialogueLine("§6[Sgt. Reyes] §fWe'll start with fire extinguishers using the §ePASS §ftechnique.", false),
            new DialogueLine("§6[Sgt. Reyes] §e§lP§r§f — Pull the pin.  §e§lA§r§f — Aim at the base.  §e§lS§r§f — Squeeze the handle.  §e§lS§r§f — Sweep side to side.", false),
            new DialogueLine("§6[Sgt. Reyes] §fHere's a live extinguisher. Head to the practice area — put out those 3 fires!", true)
        )),
        // Vestigial stage from old system — treat same as NOT_STARTED so old save data works
        entry(TutorialStage.PASS_PULL, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fWelcome back! Let's start fresh — here's a refresher on the §ePASS §ftechnique.", false),
            new DialogueLine("§6[Sgt. Reyes] §e§lP§r§f — Pull.  §e§lA§r§f — Aim.  §e§lS§r§f — Squeeze.  §e§lS§r§f — Sweep.", false),
            new DialogueLine("§6[Sgt. Reyes] §fHere's your extinguisher. Head to the practice fires and put them all out!", true)
        )),
        entry(TutorialStage.PASS_SPRAY, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Remember: aim at the §obase §7of the flame, not the top. Keep sweeping!", false)
        )),
        entry(TutorialStage.EXT_TYPE_A, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Good work on the fires! §a[Officer Cruz]§7 is covering extinguisher classes — go talk to her.", false)
        )),
        entry(TutorialStage.EXT_TYPE_B, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Keep going — §a[Officer Cruz]§7 has more to teach you about extinguisher types.", false)
        )),
        entry(TutorialStage.EXT_TYPE_C, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Almost there! Finish up with §a[Officer Cruz]§7 for the last extinguisher class.", false)
        )),
        entry(TutorialStage.QUAKE_DROP, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7The earthquake drill is with §c[Capt. Santos]§7. Head to the drill zone!", false)
        )),
        entry(TutorialStage.QUAKE_COVER, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Stay in the drill! Get under a solid block and keep crouching.", false)
        )),
        entry(TutorialStage.QUAKE_HOLDON, List.of(
            new DialogueLine("§6[Sgt. Reyes] §7Hold your position! Stay crouched under that cover until the shaking stops.", false)
        )),
        entry(TutorialStage.COMPLETED, List.of(
            new DialogueLine("§6[Sgt. Reyes] §fYou've completed all training. The disaster simulation is ready for you in the main lobby!", false)
        ))
    );

    // ── Officer Cruz — Extinguisher Types Room ───────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> EXT_EXPERT_LINES = Map.of(
        TutorialStage.EXT_TYPE_A, List.of(
            new DialogueLine("§a[Officer Cruz] §fWell done on PASS! I'm Officer Cruz — extinguisher classification specialist.", false),
            new DialogueLine("§a[Officer Cruz] §fNot all fires are the same. Using the wrong extinguisher can make things worse.", false),
            new DialogueLine("§a[Officer Cruz] §e§lClass A §r§a— Ordinary Combustibles.", false),
            new DialogueLine("§a[Officer Cruz] §fThis covers wood, paper, cloth, rubber, cardboard, and most plastics.", false),
            new DialogueLine("§a[Officer Cruz] §fUse water-type, foam, or multipurpose §e(ABC) §fdry chemical extinguishers.", false),
            new DialogueLine("§a[Officer Cruz] §fYou'll see Class A fires most often in homes and offices. Understood?", true)
        ),
        TutorialStage.EXT_TYPE_B, List.of(
            new DialogueLine("§a[Officer Cruz] §e§lClass B §r§a— Flammable Liquids and Gases.", false),
            new DialogueLine("§a[Officer Cruz] §fGasoline, diesel, cooking oil, propane, paint solvents — all Class B.", false),
            new DialogueLine("§a[Officer Cruz] §c§l⚠ DO NOT use water! §r§aIt spreads flaming liquid and worsens the fire.", false),
            new DialogueLine("§a[Officer Cruz] §fUse CO2 or dry chemical. Aim at the near edge and sweep across.", false),
            new DialogueLine("§a[Officer Cruz] §fClass B fires spread fast. Act within seconds. Got it?", true)
        ),
        TutorialStage.EXT_TYPE_C, List.of(
            new DialogueLine("§a[Officer Cruz] §e§lClass C §r§a— Energized Electrical Equipment.", false),
            new DialogueLine("§a[Officer Cruz] §fComputers, circuit breakers, switchboards, motors — all Class C.", false),
            new DialogueLine("§a[Officer Cruz] §c§l⚠ CRITICAL: NEVER use water on Class C. §r§aWater conducts electricity.", false),
            new DialogueLine("§a[Officer Cruz] §fUsing water on an electrical fire can electrocute you instantly.", false),
            new DialogueLine("§a[Officer Cruz] §fOnly CO2 or dry chemical — they smother the fire without conducting current.", false),
            new DialogueLine("§a[Officer Cruz] §fIf possible, cut the power first, then apply the extinguisher. Stay safe!", false),
            new DialogueLine("§a[Officer Cruz] §fExcellent! You now know all three classes. Report to §cCapt. Santos§a for earthquake drills.", true)
        )
    );

    // ── Capt. Santos — Earthquake Drill Zone ─────────────────────────────────

    public static final Map<TutorialStage, List<DialogueLine>> SAFETY_OFFICER_LINES = Map.of(
        TutorialStage.QUAKE_DROP, List.of(
            new DialogueLine("§c[Capt. Santos] §fFire training complete. I'm Captain Santos — earthquake preparedness.", false),
            new DialogueLine("§c[Capt. Santos] §fThe Philippines sits directly on the Pacific Ring of Fire.", false),
            new DialogueLine("§c[Capt. Santos] §fWe experience over §c2,000 earthquakes §eper year, many without warning.", false),
            new DialogueLine("§c[Capt. Santos] §fThe correct response is: §e§lDROP §r§c— §e§lCOVER §r§c— §e§lHOLD ON.", false),
            new DialogueLine("§c[Capt. Santos] §fI'm activating the drill now. When shaking begins — §eDROP immediately, SHIFT to crouch!", false)
        ),
        TutorialStage.COMPLETED, List.of(
            new DialogueLine("§c[Capt. Santos] §f§lOutstanding work, trainee.", false),
            new DialogueLine("§c[Capt. Santos] §fYou've demonstrated fire extinguisher skill and proper earthquake response.", false),
            new DialogueLine("§c[Capt. Santos] §fRemember: in a real emergency, stay calm and apply what you learned here.", false),
            new DialogueLine("§c[Capt. Santos] §fThe BFP is proud to have prepared you. Dismissed — and stay safe out there.", false)
        )
    );

    private NpcDialogue() {}
}
