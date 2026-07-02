package net.necookie.disastersim.entity;

public enum NpcType {
    SGT_REYES       ("sgt_reyes",        "§6Sgt. Reyes",             false),
    SGT_SANTOS      ("sgt_santos",       "§6Sgt. Santos",            false),
    OFFICER_CRUZ    ("officer_cruz",     "§aOfficer Cruz",           false),
    CAPT_MORFE      ("capt_morfe",       "§cCapt. Cesar Morfe Jr.",  false),
    SECURITY_TUAZON ("security_tuazon",  "§7Security Guard Tuazon",  false),
    DM_ORLANDA      ("dm_orlanda",       "§5DM Orlanda",             true),
    NECOOKIE        ("necookie",         "§bNecookie",               true),
    SIR_BOOKMARK    ("sir_bookmark",     "§eSir BookMark",           false),
    STUDENT         ("student",          "§fStudent",                true);

    /** Matches the texture filename under entity/npc/ and item/npc/. */
    public final String id;
    /** Colored display name shown above the NPC's head. */
    public final String displayName;
    /** If true, the NPC occasionally takes a tiny idle step instead of standing perfectly still. */
    public final boolean minimalWander;

    NpcType(String id, String displayName, boolean minimalWander) {
        this.id = id;
        this.displayName = displayName;
        this.minimalWander = minimalWander;
    }

    public static NpcType fromId(String id) {
        for (NpcType t : values()) if (t.id.equals(id)) return t;
        return SGT_REYES;
    }
}
