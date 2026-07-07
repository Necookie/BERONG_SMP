package net.necookie.disastersim.item;

import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Safety whistle: a loud, audible-from-far ping plus a particle burst above the blower's head.
 * Used in group drills to mark a position ("on me!") or call attention. 2-second cooldown.
 */
public class SafetyWhistleItem extends Item {

    private static final int COOLDOWN_TICKS = 40;

    public SafetyWhistleItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            serverLevel.playSound(null, sp.blockPosition(), SoundEvents.ARROW_HIT_PLAYER,
                    SoundSource.PLAYERS, 3.0f, 1.8f);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    sp.getX(), sp.getY() + 2.2, sp.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
            sp.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Blow to mark your position with a loud ping"));
        tooltip.accept(Component.literal("§7and a burst of sparks overhead."));
    }
}
