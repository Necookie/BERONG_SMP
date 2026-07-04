package net.necookie.disastersim.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.client.AcademyCompassHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server→client packet carrying the Academy's current waypoint target for the client-rendered
 * compass arrow ({@link AcademyCompassHud}). Replaces the old server-side particle waypoint
 * ({@code AcademyVisuals.spawnCompassArrow}) — the arrow is now drawn entirely client-side every
 * render frame from the local player's own exact position/view angle, so this packet only needs
 * to say *where* the target is, not redraw anything; {@code AcademyVisuals.setCompassTarget}
 * dedupes so it's only actually sent when the target changes. {@code active=false} hides the
 * compass (e.g. once the player has already reached the room the compass was pointing at).
 *
 * @param active whether the compass should be shown at all
 * @param x      target world X (ignored when {@code active} is false)
 * @param y      target world Y (ignored when {@code active} is false)
 * @param z      target world Z (ignored when {@code active} is false)
 */
public record AcademyCompassPayload(boolean active, double x, double y, double z) implements CustomPacketPayload {

    public static final Type<AcademyCompassPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy_compass"));

    public static final StreamCodec<FriendlyByteBuf, AcademyCompassPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,   AcademyCompassPayload::active,
                    ByteBufCodecs.DOUBLE, AcademyCompassPayload::x,
                    ByteBufCodecs.DOUBLE, AcademyCompassPayload::y,
                    ByteBufCodecs.DOUBLE, AcademyCompassPayload::z,
                    AcademyCompassPayload::new
            );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("5").playToClient(TYPE, STREAM_CODEC, AcademyCompassPayload::handle);
    }

    private static void handle(AcademyCompassPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AcademyCompassHud.active = payload.active();
            AcademyCompassHud.targetX = payload.x();
            AcademyCompassHud.targetY = payload.y();
            AcademyCompassHud.targetZ = payload.z();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
