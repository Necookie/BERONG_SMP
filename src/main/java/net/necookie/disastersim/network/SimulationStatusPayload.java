package net.necookie.disastersim.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.client.SimulationHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network payload for synchronizing simulation status from server to client.
 * Contains the current simulation state, time remaining, and earthquake intensity.
 *
 * @param status    The name of the current simulation state (e.g., "FIRE", "EARTHQUAKE").
 * @param timeLeft  The time remaining in the simulation, in seconds.
 * @param intensity Per-player earthquake intensity (0.0 when not an earthquake or at END phase).
 */
public record SimulationStatusPayload(String status, int timeLeft, float intensity) implements CustomPacketPayload {

    /** The unique identifier for this packet payload. */
    public static final Type<SimulationStatusPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "sim_status"));

    /** The codec used to serialize and deserialize this payload over the network. */
    public static final StreamCodec<FriendlyByteBuf, SimulationStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SimulationStatusPayload::status,
            ByteBufCodecs.INT,         SimulationStatusPayload::timeLeft,
            ByteBufCodecs.FLOAT,       SimulationStatusPayload::intensity,
            SimulationStatusPayload::new
    );

    /**
     * Registers the payload handler for this packet.
     * Called during the registration phase of network payloads.
     */
    /**
     * Registers this payload type with NeoForge's network system.
     * "1" is the channel version string — if you change the packet format,
     * bump this number so clients with the old version are disconnected gracefully
     * instead of crashing on a malformed packet.
     * playToClient means this packet only travels server → client (never the reverse).
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, STREAM_CODEC, SimulationStatusPayload::handle);
    }

    /**
     * Handles the receipt of this payload on the client side.
     * Updates the global simulation status and timer in the HUD.
     *
     * @param payload The received payload containing status name and seconds left.
     * @param context Provides utilities for safe cross-thread work scheduling.
     */
    private static void handle(SimulationStatusPayload payload, IPayloadContext context) {
        // Network packets arrive on the Netty IO thread, but Minecraft's render and
        // game logic run on the main client thread.  enqueueWork schedules the field
        // updates to run on the main thread so there's no race condition with the HUD
        // renderer reading SimulationHud.currentStatus / timeLeft at the same time.
        context.enqueueWork(() -> {
            SimulationHud.currentStatus = payload.status();   // e.g., "FIRE" or "" when ended
            SimulationHud.timeLeft      = payload.timeLeft(); // seconds remaining; 0 hides the HUD
            SimulationHud.intensity     = payload.intensity(); // earthquake intensity for camera shake
        });
    }

    /**
     * Returns the unique type identifier for this payload.
     * Required by the {@link CustomPacketPayload} contract so NeoForge can route
     * incoming packets to the correct handler.
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
