package net.necookie.disastersim.item;

import java.util.Map;
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
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;

/**
 * First aid kit: heals 3 hearts and clears negative effects. The brief slowness it applies is the
 * "treatment time" â you stop and treat, you don't sprint while bandaging. 5 uses (durability).
 */
public class FirstAidKitItem extends Item {

    public FirstAidKitItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getHealth() >= player.getMaxHealth() && player.getActiveEffects().isEmpty()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("§7You're unhurt — save the kit for a real casualty."));
            }
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            player.heal(6.0f);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2, false, true));
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE,
                    SoundSource.PLAYERS, 1.0f, 1.1f);
            sp.sendSystemMessage(Component.literal("§a✔ Treated — wounds dressed. Move carefully for a few seconds."));

            ItemStack stack = player.getItemInHand(hand);
            int newDamage = stack.getDamageValue() + 1;
            if (newDamage >= stack.getMaxDamage()) {
                stack.shrink(1);
                sp.sendSystemMessage(Component.literal("§7The first aid kit is used up."));
            } else {
                stack.setDamageValue(newDamage);
            }

            SimulationSession session = SimulationManager.getSession(sp.getUUID());
            if (session != null) {
                session.logger.log("first_aid_use", Map.of(
                        "x", sp.getX(), "y", sp.getY(), "z", sp.getZ()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Use to heal 3 hearts and clear harmful effects."));
        tooltip.accept(Component.literal("§7Treatment takes a moment — you slow down while bandaging."));
        tooltip.accept(Component.literal("§75 uses."));
    }
}
