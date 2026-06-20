package net.necookie.disastersim;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Defines and exposes all mod configuration entries.
 *
 * <p>Values are read lazily via {@code .get()} at runtime, so changing
 * {@code berongsmp-common.toml} and reloading the server takes effect without
 * recompilation.
 *
 * <p>Simulation tuning entries control the disaster effects applied during a
 * player session; see {@link net.necookie.disastersim.world.SimulationEffects}
 * and {@link net.necookie.disastersim.world.SimulationSession} for usage.
 */
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -----------------------------------------------------------------------
    // Template / example entries (from NeoForge mod template — not used by
    // simulation logic, kept for reference)
    // -----------------------------------------------------------------------

    /** Whether to log details about dirt blocks during common setup. */
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    /** A magic number used for demonstration purposes. */
    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    /** The introduction message for the magic number. */
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    /** A list of item identifiers logged on common setup. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // -----------------------------------------------------------------------
    // Simulation tuning entries
    // -----------------------------------------------------------------------

    /**
     * Total duration of one simulation run in server ticks (20 ticks = 1 second).
     * Default: 2400 ticks = 2 minutes.
     */
    public static final ModConfigSpec.IntValue SIM_DURATION_TICKS = BUILDER
            .comment("Total simulation duration in server ticks (20 ticks = 1 second). Default: 2400 = 2 minutes.")
            .defineInRange("simDurationTicks", 2400, 200, 72000);

    /**
     * Number of fire blocks placed per fire-spawn event.
     * Higher values make the fire spread faster.
     */
    public static final ModConfigSpec.IntValue FIRE_SPAWN_COUNT = BUILDER
            .comment("Number of fire blocks placed per fire-spawn event.")
            .defineInRange("fireSpawnCount", 3, 1, 50);

    /**
     * Ticks between consecutive fire-placement events.
     * Lower values make fire spread more frequently (20 ticks = 1 second).
     */
    public static final ModConfigSpec.IntValue FIRE_SPAWN_INTERVAL = BUILDER
            .comment("Ticks between fire-spread events (20 ticks = 1 second). Default: 20 = once per second.")
            .defineInRange("fireSpawnInterval", 20, 1, 100);

    /**
     * Number of blocks destroyed per earthquake event.
     * Higher values increase structural damage per tick.
     */
    public static final ModConfigSpec.IntValue QUAKE_BREAK_COUNT = BUILDER
            .comment("Blocks destroyed per earthquake-break event.")
            .defineInRange("quakeBreakCount", 2, 1, 50);

    /**
     * Ticks between consecutive earthquake-break events.
     * Lower values make the earthquake more intense (20 ticks = 1 second).
     */
    public static final ModConfigSpec.IntValue QUAKE_INTERVAL = BUILDER
            .comment("Ticks between earthquake-break events (20 ticks = 1 second). Default: 10 = twice per second.")
            .defineInRange("quakeInterval", 10, 1, 100);

    /**
     * Per-axis XZ range (in blocks) for random effect placement, measured
     * from the simulation arena origin. Effects land within a square column
     * of this size.
     */
    public static final ModConfigSpec.IntValue SIM_AREA_SIZE = BUILDER
            .comment("Per-axis XZ range (blocks) for random effect placement from the arena origin.")
            .defineInRange("simAreaSize", 25, 5, 256);

    /**
     * Y range (in blocks) for random effect placement above the simulation
     * arena origin.
     */
    public static final ModConfigSpec.IntValue SIM_AREA_HEIGHT = BUILDER
            .comment("Y range (blocks) for random effect placement above the arena origin.")
            .defineInRange("simAreaHeight", 10, 2, 256);

    // -----------------------------------------------------------------------
    // Earthquake phase / intensity tuning
    // -----------------------------------------------------------------------

    /** Epicenter magnitude (0.0–10.0). Intensity at the epicenter = magnitude; decays with distance. */
    public static final ModConfigSpec.DoubleValue QUAKE_MAGNITUDE = BUILDER
            .comment("Earthquake magnitude (0.0–10.0). Scales intensity at the epicenter.")
            .defineInRange("quakeMagnitude", 5.0, 0.1, 10.0);

    /** Intensity decay rate per block of distance from the epicenter. Higher = sharper falloff. */
    public static final ModConfigSpec.DoubleValue QUAKE_DECAY_RATE = BUILDER
            .comment("Intensity decay rate per block of distance from the epicenter. Higher = faster dropoff.")
            .defineInRange("quakeDecayRate", 0.05, 0.001, 1.0);

    /** Ticks for the RUMBLE phase (low-intensity pre-quake shaking). 200 ticks = 10 s. */
    public static final ModConfigSpec.IntValue QUAKE_RUMBLE_DURATION = BUILDER
            .comment("Ticks for the RUMBLE phase (pre-quake low-intensity shaking). Default: 200 = 10s.")
            .defineInRange("quakeRumbleDuration", 200, 20, 2400);

    /** Ticks for the PEAK phase (maximum-intensity structural collapse). 900 ticks = 45 s. */
    public static final ModConfigSpec.IntValue QUAKE_PEAK_DURATION = BUILDER
            .comment("Ticks for the PEAK phase (maximum-intensity block destruction). Default: 900 = 45s.")
            .defineInRange("quakePeakDuration", 900, 20, 2400);

    /** Ticks per aftershock wave (2–4 waves follow the main quake). 300 ticks = 15 s each. */
    public static final ModConfigSpec.IntValue QUAKE_AFTERSHOCK_DURATION = BUILDER
            .comment("Ticks per aftershock wave (2–4 waves follow the main quake). Default: 300 = 15s each.")
            .defineInRange("quakeAftershockDuration", 300, 20, 2400);

    // -----------------------------------------------------------------------
    // Student session / scoring
    // -----------------------------------------------------------------------

    /** Turso database HTTPS URL (e.g. https://mydb-org.turso.io). Leave blank to disable session tracking. */
    public static final ModConfigSpec.ConfigValue<String> TURSO_URL = BUILDER
            .comment("Turso database URL for student session tracking. Leave blank to disable.")
            .define("tursoUrl", "");

    /** Turso auth token (Bearer token from the Turso dashboard). */
    public static final ModConfigSpec.ConfigValue<String> TURSO_TOKEN = BUILDER
            .comment("Turso authentication token. Leave blank to disable session tracking.")
            .define("tursoToken", "");

    /** Minimum fires extinguished for a FIRE simulation to be marked as passed. */
    public static final ModConfigSpec.IntValue PASS_THRESHOLD_FIRE = BUILDER
            .comment("Fires extinguished required to pass the fire simulation.")
            .defineInRange("passThresholdFire", 5, 1, 100);

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    /** The built configuration specification registered with NeoForge. */
    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Validates that a string is a valid item registry name.
     *
     * @param obj The object to validate (expected to be a String).
     * @return {@code true} if valid, {@code false} otherwise.
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
