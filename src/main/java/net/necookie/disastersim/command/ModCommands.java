package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Command registration entry point. Delegates to focused subclasses:
 * - RegistrationCommands  — /register
 * - ItemCommands          — /spawn_lspu, /get_extinguisher, /get_co2_extinguisher
 * - SimulationCommands    — /sim_fire, /sim_earthquake, /sim_stop, /sim_status, /sim_list, etc.
 * - BfpAdminCommands      — /bfp (login, checkin, checkout, sessions, student, ...)
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
    }
}
