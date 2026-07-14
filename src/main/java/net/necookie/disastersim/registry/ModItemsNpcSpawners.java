package net.necookie.disastersim.registry;

import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.item.NpcSpawnerItem;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * NPC spawner item registrations (all 24 {@link NpcType} characters) — split out of
 * {@link ModItems} (see its class javadoc). Package-private: {@link ModItems} re-exports every
 * field below in the same declared order.
 */
final class ModItemsNpcSpawners {

    private ModItemsNpcSpawners() {}

    static final DeferredItem<NpcSpawnerItem> NPC_SGT_REYES =
            ModItems.ITEMS.registerItem("npc_sgt_reyes",
                    p -> new NpcSpawnerItem(NpcType.SGT_REYES, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_SGT_SANTOS =
            ModItems.ITEMS.registerItem("npc_sgt_santos",
                    p -> new NpcSpawnerItem(NpcType.SGT_SANTOS, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_OFFICER_CRUZ =
            ModItems.ITEMS.registerItem("npc_officer_cruz",
                    p -> new NpcSpawnerItem(NpcType.OFFICER_CRUZ, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_CAPT_MORFE =
            ModItems.ITEMS.registerItem("npc_capt_morfe",
                    p -> new NpcSpawnerItem(NpcType.CAPT_MORFE, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_SECURITY_TUAZON =
            ModItems.ITEMS.registerItem("npc_security_tuazon",
                    p -> new NpcSpawnerItem(NpcType.SECURITY_TUAZON, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_DM_ORLANDA =
            ModItems.ITEMS.registerItem("npc_dm_orlanda",
                    p -> new NpcSpawnerItem(NpcType.DM_ORLANDA, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_NECOOKIE =
            ModItems.ITEMS.registerItem("npc_necookie",
                    p -> new NpcSpawnerItem(NpcType.NECOOKIE, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_SIR_BOOKMARK =
            ModItems.ITEMS.registerItem("npc_sir_bookmark",
                    p -> new NpcSpawnerItem(NpcType.SIR_BOOKMARK, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT =
            ModItems.ITEMS.registerItem("npc_student",
                    p -> new NpcSpawnerItem(NpcType.STUDENT, p.stacksTo(16)));

    // ── Sim building faculty spawners (npc_prof_* — tab-complete groups them) ──

    static final DeferredItem<NpcSpawnerItem> NPC_PROF_INSTRUCTOR_DAVID =
            ModItems.ITEMS.registerItem("npc_prof_instructor_david",
                    p -> new NpcSpawnerItem(NpcType.PROF_INSTRUCTOR_DAVID, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_PROF_PRINCIPAL_BROWN =
            ModItems.ITEMS.registerItem("npc_prof_principal_brown",
                    p -> new NpcSpawnerItem(NpcType.PROF_PRINCIPAL_BROWN, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_PROF_PROFESSOR_BALDWIN =
            ModItems.ITEMS.registerItem("npc_prof_professor_baldwin",
                    p -> new NpcSpawnerItem(NpcType.PROF_PROFESSOR_BALDWIN, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_PROF_PROFESSOR_KEVIN =
            ModItems.ITEMS.registerItem("npc_prof_professor_kevin",
                    p -> new NpcSpawnerItem(NpcType.PROF_PROFESSOR_KEVIN, p.stacksTo(16)));

    // ── Sim building student spawners (npc_student_* — tab-complete groups them) ──

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_GOLDY =
            ModItems.ITEMS.registerItem("npc_student_goldy",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_GOLDY, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_HARVEY =
            ModItems.ITEMS.registerItem("npc_student_harvey",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_HARVEY, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_JENNY =
            ModItems.ITEMS.registerItem("npc_student_jenny",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_JENNY, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KAEFLA =
            ModItems.ITEMS.registerItem("npc_student_kaefla",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KAEFLA, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KARL =
            ModItems.ITEMS.registerItem("npc_student_karl",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KARL, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KATH =
            ModItems.ITEMS.registerItem("npc_student_kath",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KATH, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KELLY =
            ModItems.ITEMS.registerItem("npc_student_kelly",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KELLY, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NATH =
            ModItems.ITEMS.registerItem("npc_student_nath",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NATH, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NECOOKIE =
            ModItems.ITEMS.registerItem("npc_student_necookie",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NECOOKIE, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NELL =
            ModItems.ITEMS.registerItem("npc_student_nell",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NELL, p.stacksTo(16)));

    static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_PRINCESS =
            ModItems.ITEMS.registerItem("npc_student_princess",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_PRINCESS, p.stacksTo(16)));
}
