package net.necookie.disastersim;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Template entries from NeoForge mod template — not used by simulation logic.
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    public static final ModConfigSpec.IntValue SIM_DURATION_TICKS = BUILDER
            .comment("Total simulation duration in server ticks (20 ticks = 1 second). Default: 2400 = 2 minutes.")
            .defineInRange("simDurationTicks", 2400, 200, 72000);

    public static final ModConfigSpec.IntValue FIRE_SPAWN_COUNT = BUILDER
            .comment("Number of fire blocks placed per fire-spawn event.")
            .defineInRange("fireSpawnCount", 3, 1, 50);

    public static final ModConfigSpec.IntValue FIRE_SPAWN_INTERVAL = BUILDER
            .comment("Ticks between fire-spread events (20 ticks = 1 second). Default: 20 = once per second.")
            .defineInRange("fireSpawnInterval", 20, 1, 100);

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
            .comment("PIN required to use /bfp admin commands via '/bfp login <pin>'. OPs bypass this.")
            .define("bfpAdminPin", "1234");

    public static final ModConfigSpec.ConfigValue<String> TURSO_URL = BUILDER
            .comment("Turso database URL for student session tracking. Leave blank to disable.")
            .define("tursoUrl", "");

    public static final ModConfigSpec.ConfigValue<String> TURSO_TOKEN = BUILDER
            .comment("Turso authentication token. Leave blank to disable session tracking.")
            .define("tursoToken", "");

    public static final ModConfigSpec.IntValue PASS_THRESHOLD_FIRE = BUILDER
            .comment("Fires extinguished required to pass the fire simulation.")
            .defineInRange("passThresholdFire", 5, 1, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
