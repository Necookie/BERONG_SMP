package net.necookie.disastersim.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.client.TutorialHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server→client packet carrying the current tutorial prompt text and optional camera-shake intensity.
 *
 * @param prompt    Instruction text shown on the tutorial HUD (empty string clears the HUD).
 * @param intensity Camera shake amplitude for QUAKE tutorial stages (0.0 when not shaking).
 */
public record TutorialStatusPayload(String prompt, float intensity) implements CustomPacketPayload {

    public static final Type<TutorialStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "tutorial_status"));

    public static final StreamCodec<FriendlyByteBuf, TutorialStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TutorialStatusPayload::prompt,
                    ByteBufCodecs.FLOAT,       TutorialStatusPayload::intensity,
                    TutorialStatusPayload::new
            );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, TutorialStatusPayload::handle);
    }

    private static void handle(TutorialStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TutorialHud.prompt    = payload.prompt();
            TutorialHud.intensity = payload.intensity();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
