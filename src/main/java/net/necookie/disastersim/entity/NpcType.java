package net.necookie.disastersim.entity;

public enum NpcType {
    SGT_REYES       ("sgt_reyes",        "§6Sgt. Reyes"),
    SGT_SANTOS      ("sgt_santos",       "§6Sgt. Santos"),
    OFFICER_CRUZ    ("officer_cruz",     "§aOfficer Cruz"),
    CAPT_MORFE      ("capt_morfe",       "§cCapt. Cesar Morfe Jr."),
    SECURITY_TUAZON ("security_tuazon",  "§7Security Guard Tuazon");

    /** Matches the texture filename under entity/npc/ and item/npc/. */
    public final String id;
    /** Colored display name shown above the NPC's head. */
    public final String displayName;

    NpcType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static NpcType fromId(String id) {
        for (NpcType t : values()) if (t.id.equals(id)) return t;
        return SGT_REYES;
    }
}
