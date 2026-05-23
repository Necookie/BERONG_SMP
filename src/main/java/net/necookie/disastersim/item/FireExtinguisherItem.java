package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;

import java.util.List;
import java.util.function.Consumer;

public class FireExtinguisherItem extends Item {
    private static final double SPRAY_RANGE = 5.5D;
    private static final int MAX_USE_TICKS = 72_000;

    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (user instanceof Player player) {
            boolean playSound = remainingUseTicks % 6 == 0;
            if (level instanceof ServerLevel serverLevel) {
                sprayServer(serverLevel, player, playSound);
                if (remainingUseTicks == MAX_USE_TICKS) {
                    logUsage(player);
                }
            } else {
                sprayClient(level, player, playSound);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    private void sprayServer(ServerLevel level, Player user, boolean playSound) {
        Vec3 direction = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(direction.scale(0.75D));
        Vec3 sideways = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }

        Vec3 upwards = sideways.cross(direction).normalize();

        for (int i = 1; i <= 7; i++) {
            double distance = i * (SPRAY_RANGE / 7.0D);
            Vec3 sample = origin.add(direction.scale(distance));
            extinguishAt(level, BlockPos.containing(sample));
            extinguishAt(level, BlockPos.containing(sample.add(sideways.scale(0.35D))));
            extinguishAt(level, BlockPos.containing(sample.add(sideways.scale(-0.35D))));
            extinguishAt(level, BlockPos.containing(sample.add(upwards.scale(0.25D))));
            extinguishAt(level, BlockPos.containing(sample.add(upwards.scale(-0.2D))));
        }

        // Server handles particles for other players
        spawnSprayParticlesServer(level, origin, direction, sideways, upwards);

        if (playSound) {
            level.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS,
                    0.9F,
                    0.9F + level.getRandom().nextFloat() * 0.2F
            );
        }
    }

    private void sprayClient(Level level, Player user, boolean playSound) {
        Vec3 direction = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(direction.scale(0.75D));
        Vec3 sideways = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }

        Vec3 upwards = sideways.cross(direction).normalize();

        // Client handles particles for smooth local visual feedback
        spawnSprayParticlesClient(level, origin, direction, sideways, upwards);

        if (playSound) {
            level.playLocalSound(
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS,
                    0.9F,
                    0.9F + level.getRandom().nextFloat() * 0.2F,
                    false
            );
        }
    }

    private void extinguishAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0);
            return;
        }

        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.levelEvent(null, 1009, pos, 0);
        }
    }

    private void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 direction, Vec3 sideways, Vec3 upwards) {
        Vec3 center = origin.add(direction.scale(0.5D));
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 24, 
                Math.abs(sideways.x) * 0.22D + 0.08D, Math.abs(upwards.y) * 0.16D + 0.08D, Math.abs(sideways.z) * 0.22D + 0.08D, 0.05D);
        level.sendParticles(ParticleTypes.POOF, center.x + direction.x * 0.3D, center.y + direction.y * 0.3D, center.z + direction.z * 0.3D, 12, 0.16D, 0.16D, 0.16D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE, center.x + direction.x * 0.4D, center.y + direction.y * 0.4D, center.z + direction.z * 0.4D, 5, 0.12D, 0.08D, 0.12D, 0.01D);
    }

    private void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 direction, Vec3 sideways, Vec3 upwards) {
        Vec3 center = origin.add(direction.scale(0.5D));
        for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.CLOUD, 
                    center.x + (level.getRandom().nextDouble() - 0.5) * 0.3, 
                    center.y + (level.getRandom().nextDouble() - 0.5) * 0.3, 
                    center.z + (level.getRandom().nextDouble() - 0.5) * 0.3, 
                    direction.x * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05, 
                    direction.y * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05, 
                    direction.z * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05);
        }
    }

    private void logUsage(Player user) {
        BerongSMP.LOGGER.info("{} used fire extinguisher at {}", user.getName().getString(), user.blockPosition());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Hold right click to spray a dense extinguishing foam cone."));
        tooltip.accept(Component.literal("Puts out fire, soul fire, candles, and campfires in front of you."));
    }
}
