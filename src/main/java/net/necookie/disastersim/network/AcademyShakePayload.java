package net.necookie.disastersim.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.client.AcademyHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server→client packet carrying ONLY the Academy's earthquake camera-shake amplitude, split out of
 * {@link AcademyStatusPayload} on purpose: while shake rode along on the caption packet, every
 * caption in the Academy — dialogue lines, the sequencer's end-of-sequence clear, Cruz's GO/STOP
 * banners, guardrail nudges — implicitly sent {@code intensity = 0} and silently killed Sgt.
 * Santos's earthquake, while a stray nonzero value from a Santos nudge could just as easily stick
 * around with nothing left to overwrite it. The visible symptom was the quake randomly stopping
 * and starting depending on whichever caption happened to arrive last.
 *
 * <p>Now the server (only {@code SantosRoomManager}) asserts the desired shake state continuously
 * through this dedicated channel, deduped server-side in {@code AcademyVisuals.setShake}, and
 * captions never touch it.
 *
 * @param intensity Camera shake amplitude; 0.0 stops the shaking.
 */
public record AcademyShakePayload(float intensity) implements CustomPacketPayload {

    public static final Type<AcademyShakePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_shake"));

    public static final StreamCodec<FriendlyByteBuf, AcademyShakePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, AcademyShakePayload::intensity,
                    AcademyShakePayload::new
            );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("6").playToClient(TYPE, STREAM_CODEC, AcademyShakePayload::handle);
    }

    private static void handle(AcademyShakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AcademyHud.intensity = payload.intensity());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
