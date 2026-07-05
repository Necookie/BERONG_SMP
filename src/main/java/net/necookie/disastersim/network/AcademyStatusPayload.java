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
 * Server→client packet carrying the current Academy (new tutorial building) prompt text.
 * Pattern-cloned from {@link TutorialStatusPayload} — kept as a separate payload/HUD pair rather
 * than reusing it, since the Academy is an independent system from the old tutorial and the two
 * need to be able to show different captions simultaneously without one overwriting the other's
 * last-message-wins static fields.
 *
 * <p>Camera-shake intensity used to ride along on this packet, which meant every unrelated caption
 * implicitly zeroed (or accidentally preserved) Sgt. Santos's earthquake shake — it now travels on
 * its own dedicated {@link AcademyShakePayload} and captions never touch it.
 *
 * @param prompt      Instruction/caption text shown on the Academy HUD (empty string clears it).
 * @param displayTicks How long (in ticks) this prompt should stay on screen before auto-clearing
 *                     client-side; 0 means "persist until overwritten" (used for the empty-string
 *                     clear itself, where duration is moot). Every other caption expires on its
 *                     own instead of sitting on screen forever once the player walks away from
 *                     whatever room manager sent it — see {@code AcademyHud}.
 */
public record AcademyStatusPayload(String prompt, int displayTicks) implements CustomPacketPayload {

    public static final Type<AcademyStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_status"));

    public static final StreamCodec<FriendlyByteBuf, AcademyStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AcademyStatusPayload::prompt,
                    ByteBufCodecs.VAR_INT,     AcademyStatusPayload::displayTicks,
                    AcademyStatusPayload::new
            );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("4").playToClient(TYPE, STREAM_CODEC, AcademyStatusPayload::handle);
    }

    private static void handle(AcademyStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AcademyHud.prompt = payload.prompt();
            AcademyHud.promptExpiresAtMillis = payload.displayTicks() <= 0
                    ? 0L
                    : System.currentTimeMillis() + payload.displayTicks() * 50L;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
