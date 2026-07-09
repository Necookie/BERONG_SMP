package net.necookie.disastersim.entity;

public enum NpcType {
    // ── New Tutorial (Academy) — the 4 active room-driving instructors ────
    SGT_REYES       ("sgt_reyes",        "new_tutorial_instructors/sgt_reyes",        "§6Sgt. Reyes",             false),
    SGT_SANTOS      ("sgt_santos",       "new_tutorial_instructors/sgt_santos",       "§6Sgt. Santos",            false),
    OFFICER_CRUZ    ("officer_cruz",     "new_tutorial_instructors/officer_cruz",     "§aOfficer Cruz",           false),
    CAPT_MORFE      ("capt_morfe",       "new_tutorial_instructors/capt_morfe",       "§cCapt. Cesar Morfe Jr.",  false),

    // ── New Tutorial (Academy) — decorative background NPCs ────────────────
    SECURITY_TUAZON ("security_tuazon",  "others/security_tuazon",  "§7Security Guard Tuazon",  false),
    DM_ORLANDA      ("dm_orlanda",       "others/dm_orlanda",       "§5DM Orlanda",             true),
    NECOOKIE        ("necookie",         "others/necookie",         "§bNecookie",               true),
    SIR_BOOKMARK    ("sir_bookmark",     "others/sir_bookmark",     "§eSir BookMark",           false),
    STUDENT         ("student",          "others/student",          "§fStudent",                true),

    // ── Sim building faculty (LSPU Library / CCS Admin Building decor) ─────
    PROF_INSTRUCTOR_DAVID   ("prof_instructor_david",   "sim_building_prof_npc/instructor_david",   "§9Instructor David",   false),
    PROF_PRINCIPAL_BROWN    ("prof_principal_brown",    "sim_building_prof_npc/principal_brown",    "§9Principal Brown",    false),
    PROF_PROFESSOR_BALDWIN  ("prof_professor_baldwin",  "sim_building_prof_npc/professor_baldwin",  "§9Professor Baldwin",  false),
    PROF_PROFESSOR_KEVIN    ("prof_professor_kevin",    "sim_building_prof_npc/professor_kevin",    "§9Professor Kevin",    false),

    // ── Sim building students (LSPU Library / CCS Admin Building decor) ────
    STUDENT_GOLDY    ("student_goldy",    "sim_building_students/goldy",    "§dGoldy",    true),
    STUDENT_HARVEY   ("student_harvey",   "sim_building_students/harvey",   "§dHarvey",   true),
    STUDENT_JENNY    ("student_jenny",    "sim_building_students/jenny",    "§dJenny",    true),
    STUDENT_KAEFLA   ("student_kaefla",   "sim_building_students/kaefla",   "§dKaefla",   true),
    STUDENT_KARL     ("student_karl",     "sim_building_students/karl",     "§dKarl",     true),
    STUDENT_KATH     ("student_kath",     "sim_building_students/kath",     "§dKath",     true),
    STUDENT_KELLY    ("student_kelly",    "sim_building_students/kelly",    "§dKelly",    true),
    STUDENT_NATH     ("student_nath",     "sim_building_students/nath",     "§dNath",     true),
    STUDENT_NECOOKIE ("student_necookie", "sim_building_students/necookie", "§dNecookie", true),
    STUDENT_NELL     ("student_nell",     "sim_building_students/nell",     "§dNell",     true),
    STUDENT_PRINCESS ("student_princess", "sim_building_students/princess", "§dPrincess", true);

    /** Stable identity string persisted to entity NBT (see CustomNpcEntity) and matched by
     * {@link #fromId}. Never renamed once shipped — the academy_building.schem's baked-in NPCs
     * and any world's saved entities round-trip through this exact string. */
    public final String id;
    /** Texture path under textures/entity/npc/ (no extension) — safe to reorganize into
     * subfolders freely since it's resolved fresh every render, unlike {@link #id}. */
    public final String texturePath;
    /** Colored display name shown above the NPC's head. */
    public final String displayName;
    /** If true, the NPC occasionally takes a tiny idle step instead of standing perfectly still. */
    public final boolean minimalWander;

    NpcType(String id, String texturePath, String displayName, boolean minimalWander) {
        this.id = id;
        this.texturePath = texturePath;
        this.displayName = displayName;
        this.minimalWander = minimalWander;
    }

    public static NpcType fromId(String id) {
        for (NpcType t : values()) if (t.id.equals(id)) return t;
        return SGT_REYES;
    }
}
