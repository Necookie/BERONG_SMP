package net.necookie.disastersim.registration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Static facade over {@link RegisteredPlayerData} for easy access from commands and managers. */
public class RegistrationManager {

    private RegistrationManager() {}

    public static void register(ServerPlayer player, String studentId, String section, String name) {
        RegisteredPlayerData.get((ServerLevel) player.level())
            .register(player.getUUID(), name, studentId, section);
    }

    public static boolean isRegistered(ServerPlayer player) {
        return RegisteredPlayerData.get((ServerLevel) player.level())
            .isRegistered(player.getUUID());
    }

    public static boolean isRegistered(UUID uuid, ServerLevel level) {
        return RegisteredPlayerData.get(level).isRegistered(uuid);
    }

    public static StudentRegistration getRegistration(ServerPlayer player) {
        return RegisteredPlayerData.get((ServerLevel) player.level()).get(player.getUUID());
    }

    public static void reset(ServerPlayer player) {
        RegisteredPlayerData.get((ServerLevel) player.level()).reset(player.getUUID());
    }

    public static void reset(UUID uuid, ServerLevel level) {
        RegisteredPlayerData.get(level).reset(uuid);
    }
}
