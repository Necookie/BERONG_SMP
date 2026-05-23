package net.necookie.disastersim.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.item.ModularExtinguisher;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record PullPinPayload() implements CustomPacketPayload {
    public static final Type<PullPinPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "pull_pin"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PullPinPayload> STREAM_CODEC = StreamCodec.unit(new PullPinPayload());

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, PullPinPayload::handle);
    }

    private static void handle(PullPinPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        pullPin(player, InteractionHand.MAIN_HAND);
        pullPin(player, InteractionHand.OFF_HAND);
    }

    private static void pullPin(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof ModularExtinguisher extinguisher) {
            extinguisher.pullPin(stack, player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}