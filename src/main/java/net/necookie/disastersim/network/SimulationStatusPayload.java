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
 * Contains the current simulation state and the time remaining.
 * 
 * @param status The name of the current simulation state (e.g., "FIRE", "EARTHQUAKE").
 * @param timeLeft The time remaining in the simulation, in seconds.
 */
public record SimulationStatusPayload(String status, int timeLeft) implements CustomPacketPayload {
    
    /** The unique identifier for this packet payload. */
    public static final Type<SimulationStatusPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "sim_status"));
    
    /** The codec used to serialize and deserialize this payload over the network. */
    public static final StreamCodec<FriendlyByteBuf, SimulationStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SimulationStatusPayload::status,
            ByteBufCodecs.INT, SimulationStatusPayload::timeLeft,
            SimulationStatusPayload::new
    );

    /**
     * Registers the payload handler for this packet.
     * Called during the registration phase of network payloads.
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, SimulationStatusPayload::handle);
    }

    /**
     * Handles the receipt of this payload on the client side.
     * Updates the global simulation status and timer in the HUD.
     * 
     * @param payload The received payload.
     * @param context The context for handling the payload.
     */
    private static void handle(SimulationStatusPayload payload, IPayloadContext context) {
        // Enqueue the work to the main thread to ensure thread safety when updating UI components
        context.enqueueWork(() -> {
            SimulationHud.currentStatus = payload.status();
            SimulationHud.timeLeft = payload.timeLeft();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
