package net.necookie.disastersim.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.player.DropAndRollManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server request: the player pressed the "drop and roll" key. Carries no fields —
 * the server reads the requesting player's own fire state, so there's nothing to serialize.
 *
 * <p>This is the mod's first serverbound payload (existing payloads are all clientbound status
 * syncs), so unlike them it's registered with {@code playToServer} instead of {@code playToClient}.
 */
public record DropAndRollPayload() implements CustomPacketPayload {

    public static final Type<DropAndRollPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "drop_and_roll"));

    public static final StreamCodec<FriendlyByteBuf, DropAndRollPayload> STREAM_CODEC =
            StreamCodec.unit(new DropAndRollPayload());

    /** "3" — the next free channel version; "1" and "2" are already used by the two clientbound payloads. */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToServer(TYPE, STREAM_CODEC, DropAndRollPayload::handle);
    }

    private static void handle(DropAndRollPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            context.enqueueWork(() -> DropAndRollManager.onDropAndRollRequest(serverPlayer));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
