package net.necookie.disastersim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.necookie.disastersim.network.PullPinPayload;
import net.necookie.disastersim.item.ModularExtinguisher;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Handles client-side input events.
 */
public class ClientEvents {
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        while (KeyMappings.PULL_PIN.consumeClick()) {
            handlePullPin(minecraft, player);
        }
    }

    private static void handlePullPin(Minecraft minecraft, Player player) {
        ItemStack stack = player.getMainHandItem();
        ModularExtinguisher extinguisher;
        if (stack.getItem() instanceof ModularExtinguisher mainHandExtinguisher) {
            extinguisher = mainHandExtinguisher;
        } else {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof ModularExtinguisher offhandExtinguisher)) {
                return;
            }
            extinguisher = offhandExtinguisher;
        }

        minecraft.options.keySocialInteractions.setDown(false);
        if (minecraft.screen instanceof SocialInteractionsScreen) {
            minecraft.setScreen(null);
        }

        if (extinguisher.getState(stack) == ModularExtinguisher.ExtinguisherState.LOCKED) {
            FireExtinguisherHud.onPullPinStarted();
        }

        extinguisher.pullPin(stack, player);
        ClientPacketDistributor.sendToServer(new PullPinPayload());
    }
}
