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
