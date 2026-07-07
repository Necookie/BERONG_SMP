package net.necookie.disastersim.common.player;

import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central dispatch point for "roll back this player's transient state" hooks that would otherwise
 * each need their own ad-hoc {@code PlayerLoggedOutEvent}/{@code PlayerLoggedInEvent} subscriber.
 *
 * <p>Exists because the same bug shape recurred three separate times in this codebase —
 * {@code TutorialManager}'s QUAKE_DROP/COVER/HOLDON rollback, {@code SantosRoomManager}'s
 * PRE_DRILL/QUAKE_ACTIVE rollback, and the Academy shake channel — all a static or persisted
 * per-player field with no compiler-enforced link to a cleanup hook. Registering here doesn't
 * prevent forgetting the rollback *logic* itself, but it removes the need for every new subsystem
 * to also remember to write and wire its own event subscriber from scratch.
 */
@EventBusSubscriber(modid = BerongSMP.MODID)
public final class PlayerLifecycleRegistry {

    private static final List<ClearsOnLogout> LOGOUT_HOOKS = new CopyOnWriteArrayList<>();
    private static final List<ClearsOnLogout> LOGIN_HOOKS = new CopyOnWriteArrayList<>();

    private PlayerLifecycleRegistry() {}

    /** Registers a hook run once per player on logout — e.g. rolling a mid-drill phase back to a safe stage. */
    public static void registerLogoutHook(ClearsOnLogout hook) {
        LOGOUT_HOOKS.add(hook);
    }

    /**
     * Registers a hook run once per player on login. Needed alongside the logout hook because a
     * server crash/force-kill never runs logout handlers, leaving persisted state stuck mid-drill —
     * the login pass is a second, independent chance to roll it back before a tick loop resumes it
     * at full strength. Implementations must be idempotent so a clean rejoin is a no-op.
     */
    public static void registerLoginHook(ClearsOnLogout hook) {
        LOGIN_HOOKS.add(hook);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        for (ClearsOnLogout hook : LOGOUT_HOOKS) hook.onLogout(uuid);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        for (ClearsOnLogout hook : LOGIN_HOOKS) hook.onLogout(uuid);
    }
}
