package net.necookie.disastersim.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Consumer;

/**
 * Fire Extinguisher with P.A.S.S. (Pull, Aim, Squeeze, Sweep) logic.
 */
public class ModularExtinguisher extends Item {
    public static final String STATE_TAG = "ExtinguisherState";

    public enum ExtinguisherState {
        LOCKED,
        PULLED,
        AIMED,
        DISCHARGING
    }

    public ModularExtinguisher(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ExtinguisherState state = getState(stack);

        if (state == ExtinguisherState.PULLED || state == ExtinguisherState.AIMED) {
            setState(stack, ExtinguisherState.DISCHARGING);
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        return state == ExtinguisherState.LOCKED ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            HitResult hit = player.pick(5.0D, 0.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                setState(stack, ExtinguisherState.AIMED);
                if (!level.isClientSide()) {
                    extinguishAlongRay(level, blockHit.getBlockPos());
                }
            } else {
                setState(stack, ExtinguisherState.DISCHARGING);
            }
        }

        if (level.isClientSide()) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.CLOUD,
                    entity.getX(), entity.getEyeY() - 0.2, entity.getZ(),
                    entity.getLookAngle().x * 0.5,
                    entity.getLookAngle().y * 0.5,
                    entity.getLookAngle().z * 0.5);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (getState(stack) != ExtinguisherState.LOCKED) {
            setState(stack, ExtinguisherState.PULLED);
            return true;
        }

        return false;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return getState(stack) == ExtinguisherState.DISCHARGING ? ItemUseAnimation.BOW : ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /**
     * Called by a KeyInputEvent or similar to pull the pin.
     */
    public void pullPin(ItemStack stack, Player player) {
        if (getState(stack) == ExtinguisherState.LOCKED) {
            setState(stack, ExtinguisherState.PULLED);
            player.swing(InteractionHand.MAIN_HAND, true);
            player.level().playSound(player, player.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }

    public ExtinguisherState getState(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains(STATE_TAG)) {
            return ExtinguisherState.valueOf(customData.copyTag().getStringOr(STATE_TAG, ExtinguisherState.LOCKED.name()));
        }
        return ExtinguisherState.LOCKED;
    }

    public void setState(ItemStack stack, ExtinguisherState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString(STATE_TAG, state.name());
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return switch (getState(stack)) {
            case LOCKED -> 4;
            case PULLED -> 8;
            case AIMED -> 11;
            case DISCHARGING -> 13;
        };
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return switch (getState(stack)) {
            case LOCKED -> 0xD94A38;
            case PULLED -> 0xF0C53A;
            case AIMED -> 0x49A6FF;
            case DISCHARGING -> 0x4EE06A;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("State: " + readableState(getState(stack))).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("P: Pull pin").withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.literal("Right click: Spray extinguisher").withStyle(ChatFormatting.YELLOW));
    }

    private Component getStatusText(ItemStack stack) {
        return switch (getState(stack)) {
            case LOCKED -> Component.literal("Extinguisher locked | Press P to pull the pin");
            case PULLED -> Component.literal("Extinguisher ready | Right click to spray");
            case AIMED -> Component.literal("Extinguisher aimed | Hold right click to keep spraying");
            case DISCHARGING -> Component.literal("Extinguisher spraying | Sweep across the fire");
        };
    }

    private String readableState(ExtinguisherState state) {
        return switch (state) {
            case LOCKED -> "Pin In";
            case PULLED -> "Pin Pulled";
            case AIMED -> "Aimed";
            case DISCHARGING -> "Spraying";
        };
    }

    private void extinguishAlongRay(Level level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.FIRE)) {
                        level.removeBlock(pos, false);
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
                        continue;
                    }

                    if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
                    }
                }
            }
        }
    }
}
