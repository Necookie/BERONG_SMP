package net.necookie.disastersim.common.player;

import java.util.UUID;

/**
 * A rollback action for one player's transient state, registered with
 * {@link PlayerLifecycleRegistry}.
 *
 * <p>Named for the logout case since that's the common one, but the same functional shape is
 * reused for login-time rollback ({@link PlayerLifecycleRegistry#registerLoginHook}) — a server
 * crash/force-kill never fires the logout event, so a login-time pass is the only remaining
 * chance to undo a mid-drill state before a tick loop resumes it at full strength. Implementations
 * registered for login must be idempotent, since a clean rejoin runs them as a no-op.
 */
@FunctionalInterface
public interface ClearsOnLogout {
    void onLogout(UUID uuid);
}
