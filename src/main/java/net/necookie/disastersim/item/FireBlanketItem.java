package net.necookie.disastersim.item;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.common.hazard.HazardBlock;
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;

/**
 * Fire blanket: the one-shot smothering tool. Used while on fire it instantly clears the player's
 * fire ticks (the fast alternative to repeated drop-and-roll presses); used on a kitchen/grease
 * hazard prop it smothers it back to safe — blanket smothering is a valid Class F/K response,
 * unlike a dry-chemical blast. 3 uses (durability), not auto-issued.
 */
public class FireBlanketItem extends Item {

    public FireBlanketItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getRemainingFireTicks() <= 0) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal(
                        "§7You're not on fire — wrap the blanket when you (or a grease fire) actually burn."));
            }
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            player.clearFire();
            consumeUse(sp, player.getItemInHand(hand));
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                    16, 0.4, 0.5, 0.4, 0.02);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.PLAYERS, 1.0f, 0.9f);
            sp.sendSystemMessage(Component.literal("§a✔ Smothered! The blanket cut the fire off from air."));
            logUse(sp, "self");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(HazardBlock.HAZARDOUS)
                || !AbstractExtinguisherItem.isKitchenHazard(state.getBlock())
                || !state.getValue(HazardBlock.HAZARDOUS)) {
            return InteractionResult.PASS;
        }

        SimulationSession session = SimulationManager.getSession(sp.getUUID());
        if (HazardManager.defuse(level, session, pos)) {
            consumeUse(sp, ctx.getItemInHand());
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.8f);
            sp.sendSystemMessage(Component.literal(
                    "§a✔ Grease fire smothered under the blanket — starved of oxygen, not splashed apart."));
            logUse(sp, "kitchen_hazard");
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static void consumeUse(ServerPlayer sp, ItemStack stack) {
        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            sp.sendSystemMessage(Component.literal("§7The fire blanket is spent and falls apart."));
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    private static void logUse(ServerPlayer sp, String target) {
        SimulationSession session = SimulationManager.getSession(sp.getUUID());
        if (session != null) {
            session.logger.log("fire_blanket_use", Map.of("target", target,
                    "x", sp.getX(), "y", sp.getY(), "z", sp.getZ()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Use while ON FIRE to smother yourself out instantly."));
        tooltip.accept(Component.literal("§7Use on a §e[Class F/K]§7 kitchen hazard to smother it."));
        tooltip.accept(Component.literal("§73 uses — smothering starves fire of oxygen."));
    }
}
