package net.necookie.disastersim.tutorial;

/** Ordered stages of the safety tutorial. Players progress linearly; COMPLETED unlocks simulations. */
public enum TutorialStage {
    NOT_STARTED,
    PASS_PULL,
    PASS_SPRAY,
    EXT_TYPE_A,
    EXT_TYPE_B,
    EXT_TYPE_C,
    QUAKE_INTRO,
    QUAKE_DROP,
    QUAKE_COVER,
    QUAKE_HOLDON,
    COMPLETED
}
