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
 * Server→client packet carrying the current Academy (new tutorial building) prompt text and
 * optional camera-shake intensity. Pattern-cloned from {@link TutorialStatusPayload} — kept as a
 * separate payload/HUD pair rather than reusing it, since the Academy is an independent system
 * from the old tutorial and the two need to be able to show different captions simultaneously
 * without one overwriting the other's last-message-wins static fields.
 *
 * @param prompt    Instruction/caption text shown on the Academy HUD (empty string clears it).
 * @param intensity Camera shake amplitude for Sgt. Santos's earthquake drill (0.0 when not shaking).
 */
public record AcademyStatusPayload(String prompt, float intensity) implements CustomPacketPayload {

    public static final Type<AcademyStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_status"));

    public static final StreamCodec<FriendlyByteBuf, AcademyStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AcademyStatusPayload::prompt,
                    ByteBufCodecs.FLOAT,       AcademyStatusPayload::intensity,
                    AcademyStatusPayload::new
            );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("4").playToClient(TYPE, STREAM_CODEC, AcademyStatusPayload::handle);
    }

    private static void handle(AcademyStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AcademyHud.prompt    = payload.prompt();
            AcademyHud.intensity = payload.intensity();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
