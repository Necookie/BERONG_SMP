package net.necookie.disastersim.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Flashlight: 60 seconds of Night Vision — the game-mechanics stand-in for staying oriented in
 * a smoke-filled or blacked-out corridor. Real-world lesson in the tooltip: light helps you find
 * exits and helps rescuers find YOU.
 */
public class FlashlightItem extends Item {

    private static final int EFFECT_TICKS = 20 * 60;
    private static final int COOLDOWN_TICKS = 20;

    public FlashlightItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer sp) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_TICKS, 0, false, false, true));
            level.playSound(null, sp.blockPosition(), SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS, 0.7f, 1.6f);
            sp.sendSystemMessage(Component.literal("§e✔ Flashlight on — 60s of clear sight through smoke and darkness."));
            sp.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Use for 60s of night vision — stay oriented in"));
        tooltip.accept(Component.literal("§7smoke or blackout. Light also helps rescuers find you."));
    }
}
