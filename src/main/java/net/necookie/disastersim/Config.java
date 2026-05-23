package net.necookie.disastersim;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration class for BerongSMP.
 * Defines and manages mod-specific configuration options.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Whether to log details about dirt blocks during common setup (Example). */
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

    /** A list of item identifiers that are logged on common setup. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    /** The built configuration specification. */
    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Validates that a string is a valid item registry name.
     * 
     * @param obj The object to validate (expected to be a String).
     * @return True if valid, false otherwise.
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
