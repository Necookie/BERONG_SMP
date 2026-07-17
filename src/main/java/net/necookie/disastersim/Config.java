package net.necookie.disastersim;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SIM_DURATION_TICKS = BUILDER
            .comment("Total simulation duration in server ticks (20 ticks = 1 second). Default: 2400 = 2 minutes.")
            .defineInRange("simDurationTicks", 2400, 200, 72000);

    public static final ModConfigSpec.IntValue FIRE_SPAWN_COUNT = BUILDER
            .comment("Number of fire blocks placed per fire-spawn event. Bumped 3 -> 5 on 2026-07-14 "
                   + "to make fire spread faster/feel more dangerous.")
            .defineInRange("fireSpawnCount", 5, 1, 50);

    public static final ModConfigSpec.IntValue FIRE_SPAWN_INTERVAL = BUILDER
            .comment("Ticks between fire-spread events (20 ticks = 1 second). Default: 10 = twice per "
                   + "second (halved from 20 on 2026-07-14 to make fire spread faster).")
            .defineInRange("fireSpawnInterval", 10, 1, 100);

    public static final ModConfigSpec.IntValue QUAKE_BREAK_COUNT = BUILDER
            .comment("Blocks destroyed per earthquake-break event.")
            .defineInRange("quakeBreakCount", 2, 1, 50);

    public static final ModConfigSpec.IntValue QUAKE_INTERVAL = BUILDER
            .comment("Ticks between earthquake-break events (20 ticks = 1 second). Default: 10 = twice per second.")
            .defineInRange("quakeInterval", 10, 1, 100);

    public static final ModConfigSpec.IntValue SIM_AREA_SIZE = BUILDER
            .comment("Per-axis XZ range (blocks) for random effect placement from the arena origin.")
            .defineInRange("simAreaSize", 25, 5, 256);

    public static final ModConfigSpec.IntValue SIM_AREA_HEIGHT = BUILDER
            .comment("Y range (blocks) for random effect placement above the arena origin.")
            .defineInRange("simAreaHeight", 10, 2, 256);

    public static final ModConfigSpec.DoubleValue QUAKE_MAGNITUDE = BUILDER
            .comment("Earthquake magnitude (0.0–10.0). Scales intensity at the epicenter.")
            .defineInRange("quakeMagnitude", 5.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue QUAKE_DECAY_RATE = BUILDER
            .comment("Intensity decay rate per block of distance from the epicenter. Higher = faster dropoff.")
            .defineInRange("quakeDecayRate", 0.05, 0.001, 1.0);

    public static final ModConfigSpec.IntValue QUAKE_RUMBLE_DURATION = BUILDER
            .comment("Ticks for the RUMBLE phase (pre-quake low-intensity shaking). Default: 200 = 10s.")
            .defineInRange("quakeRumbleDuration", 200, 20, 2400);

    public static final ModConfigSpec.IntValue QUAKE_PEAK_DURATION = BUILDER
            .comment("Ticks for the PEAK phase (maximum-intensity block destruction). Default: 900 = 45s.")
            .defineInRange("quakePeakDuration", 900, 20, 2400);

    public static final ModConfigSpec.IntValue QUAKE_AFTERSHOCK_DURATION = BUILDER
            .comment("Ticks per aftershock wave (2–4 waves follow the main quake). Default: 300 = 15s each.")
            .defineInRange("quakeAftershockDuration", 300, 20, 2400);

    public static final ModConfigSpec.ConfigValue<String> BFP_ADMIN_PIN = BUILDER
            .comment("PIN required to use /bfp admin commands via '/bfp login <pin>'. OPs bypass this. "
                   + "Leave blank to disable PIN-based login (OP access still works).")
            .define("bfpAdminPin", "");

    public static final ModConfigSpec.ConfigValue<String> TURSO_URL = BUILDER
            .comment("Turso database URL for student session tracking. Leave blank to disable.")
            .define("tursoUrl", "");

    public static final ModConfigSpec.ConfigValue<String> TURSO_TOKEN = BUILDER
            .comment("Turso authentication token. Leave blank to disable session tracking.")
            .define("tursoToken", "");

    public static final ModConfigSpec.IntValue PASS_THRESHOLD_FIRE = BUILDER
            .comment("Fires extinguished required to pass the fire simulation.")
            .defineInRange("passThresholdFire", 5, 1, 100);

    public static final ModConfigSpec.IntValue ACADEMY_IGNITE_DEMO_TICKS = BUILDER
            .comment("Safety-cap timeout (ticks) for Sgt. Reyes's scripted drop-and-roll demo -- the "
                   + "fire itself is kept alight until the player actually rolls, so this only forces "
                   + "it out if they never do. Default: 200 = 10s (20 ticks = 1s).")
            .defineInRange("academyIgniteDemoTicks", 200, 20, 400);

    public static final ModConfigSpec.IntValue ACADEMY_GOSTOP_GRACE_TICKS = BUILDER
            .comment("Reaction window (ticks) after Officer Cruz calls STOP: movement during this "
                   + "window is never punished (the reference position keeps re-anchoring while the "
                   + "player slides to a halt); only movement after it counts as a violation. "
                   + "Default: 30 = 1.5s (20 ticks = 1s).")
            .defineInRange("academyGoStopGraceTicks", 30, 0, 100);

    public static final ModConfigSpec.IntValue ACADEMY_PASS_THRESHOLD = BUILDER
            .comment("Minimum score (0-100) Capt. Morfe requires to certify a player after the Academy.")
            .defineInRange("academyPassThreshold", 70, 0, 100);

    public static final ModConfigSpec.IntValue NEW_SIM2_PREVENTION_TICKS = BUILDER
            .comment("New Sim Building 2.0: ticks to find and bare-hand-prevent all 5 armed hazards "
                   + "before unfound ones ignite. Default: 2400 = 2 minutes.")
            .defineInRange("newSimBuilding2PreventionTicks", 2400, 200, 12000);

    public static final ModConfigSpec.IntValue NEW_SIM2_INTERVENTION_TICKS = BUILDER
            .comment("New Sim Building 2.0: ticks to extinguish every escalated hazard with its "
                   + "class-correct extinguisher before it goes big and forces evacuation. Default: 1800 = 90s.")
            .defineInRange("newSimBuilding2InterventionTicks", 1800, 200, 12000);

    public static final ModConfigSpec.IntValue NEW_SIM2_EVACUATION_TICKS = BUILDER
            .comment("New Sim Building 2.0: additional ticks budgeted for the evacuation phase once "
                   + "triggered (added to the prevention+intervention budgets for the session's total "
                   + "duration). Default: 1800 = 90s.")
            .defineInRange("newSimBuilding2EvacuationTicks", 1800, 200, 12000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
