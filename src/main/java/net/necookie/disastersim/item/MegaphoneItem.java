package net.necookie.disastersim.item;

import java.util.function.Consumer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.necookie.disastersim.registry.ModSounds;

/**
 * Instructor megaphone (OP level 2): broadcasts an evacuation call — chat line + klaxon — to every
 * player within 30 blocks. 5-second cooldown so it can't be spammed into noise.
 */
public class MegaphoneItem extends Item {

    private static final double RANGE = 30.0;
    private static final int COOLDOWN_TICKS = 100;

    public MegaphoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (!Commands.LEVEL_GAMEMASTERS.check(sp.createCommandSourceStack().permissions())) {
            sp.sendSystemMessage(Component.literal("§7The megaphone is an instructor tool (OP required)."));
            return InteractionResult.PASS;
        }
        for (ServerPlayer nearby : serverLevel.players()) {
            if (nearby.distanceTo(sp) <= RANGE) {
                nearby.sendSystemMessage(Component.literal(
                        "§c§l[MEGAPHONE] §f" + sp.getName().getString()
                        + ": EVACUATE NOW — proceed calmly to the nearest exit and assembly area!"));
            }
        }
        serverLevel.playSound(null, sp.blockPosition(), ModSounds.FIRE_ALARM_RING.get(),
                SoundSource.PLAYERS, 2.0f, 1.0f);
        sp.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Instructor tool: broadcast an evacuation call"));
        tooltip.accept(Component.literal("§7to all players within 30 blocks. §cOP only."));
    }
}
