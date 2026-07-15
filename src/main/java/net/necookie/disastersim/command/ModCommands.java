package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModList;

/**
 * Command registration entry point. Delegates to focused subclasses:
 * - RegistrationCommands  — /register
 * - ItemCommands          — /spawn_lspu, /get_extinguisher, /get_co2_extinguisher, /get_wet_chemical_extinguisher
 * - SimulationCommands    — /sim_fire, /sim_earthquake, /sim_stop, /sim_status, /sim_list, etc.
 * - BfpAdminCommands      — /bfp (login, checkin, checkout, sessions, student, ...)
 * - CopyRoomCommand       — //copyroom (WorldEdit selection → clipboard room metrics, optional dep)
 */
public class ModCommands {

    /** Clears all PIN authorizations (call on server start). */
    public static void clearAuthorizations() {
        BfpAdminCommands.clearAuthorizations();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RegistrationCommands.register(dispatcher);
        ItemCommands.register(dispatcher);
        SimulationCommands.register(dispatcher);
        BfpAdminCommands.register(dispatcher);

        // CopyRoomCommand touches WorldEdit types (e.g. a catch clause for
        // IncompleteRegionException) that don't resolve without WorldEdit installed. The class
        // must not even be loaded on a server that lacks it, or the JVM verifier throws
        // NoClassDefFoundError before CopyRoomCommand's own ModList guard ever runs.
        if (ModList.get().isLoaded("worldedit")) {
            CopyRoomCommand.register(dispatcher);
        }
    }
}
