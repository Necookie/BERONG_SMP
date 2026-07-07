package net.necookie.disastersim.registry;

import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * All {@link AttachmentType} registrations (per-entity synced client-visible state). Extracted
 * from {@code BerongSMP} so the entry point stays a thin bootstrap; {@link #register(IEventBus)}
 * is called from its constructor.
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BerongSMP.MODID);

    /**
     * Ticks remaining in a player's drop-and-roll "dropped" window (0 = not dropped), mirroring
     * {@code DropAndRollManager.droppedTicksRemaining}. Auto-synced to every client tracking the
     * player (including their own client) so {@code DropAndRollRenderModifier} can drive the
     * crouch/roll animation — purely cosmetic, never touches the real entity Pose/hitbox.
     */
    public static final Supplier<AttachmentType<Integer>> DROPPED_TICKS =
            ATTACHMENT_TYPES.register("dropped_ticks",
                    () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());

    private ModAttachments() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
