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


public record SimulationStatusPayload(String status, int timeLeft) implements CustomPacketPayload {
    public static final Type<SimulationStatusPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "sim_status"));
    
    public static final StreamCodec<FriendlyByteBuf, SimulationStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SimulationStatusPayload::status,
            ByteBufCodecs.INT, SimulationStatusPayload::timeLeft,
            SimulationStatusPayload::new
    );

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, SimulationStatusPayload::handle);
    }

    private static void handle(SimulationStatusPayload payload, IPayloadContext context) {
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