package com.necookie.fireextinguisherprototype.items;

import com.necookie.fireextinguisherprototype.FireExtinguisherPrototype;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.List;

public class FireExtinguisherItem extends Item {
    private static final double SPRAY_RANGE = 5.5D;
    private static final int MAX_USE_TICKS = 72_000;

    public FireExtinguisherItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);

        if (!world.isClient) {
            spray((ServerWorld) world, user, true);
            logUsage(user);
        }

        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            spray((ServerWorld) world, player, remainingUseTicks % 6 == 0);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    private void spray(ServerWorld world, PlayerEntity user, boolean playSound) {
        Vec3d direction = user.getRotationVec(1.0F).normalize();
        Vec3d origin = user.getEyePos().add(direction.multiply(0.75D));
        Vec3d sideways = direction.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));

        if (sideways.lengthSquared() < 1.0E-4D) {
            sideways = new Vec3d(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }

        Vec3d upwards = sideways.crossProduct(direction).normalize();

        for (int i = 1; i <= 7; i++) {
            double distance = i * (SPRAY_RANGE / 7.0D);
            Vec3d sample = origin.add(direction.multiply(distance));
            extinguishAt(world, BlockPos.ofFloored(sample));
            extinguishAt(world, BlockPos.ofFloored(sample.add(sideways.multiply(0.35D))));
            extinguishAt(world, BlockPos.ofFloored(sample.add(sideways.multiply(-0.35D))));
            extinguishAt(world, BlockPos.ofFloored(sample.add(upwards.multiply(0.25D))));
            extinguishAt(world, BlockPos.ofFloored(sample.add(upwards.multiply(-0.2D))));
        }

        spawnSprayParticles(world, origin, direction, sideways, upwards);

        if (playSound) {
            world.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS,
                    0.9F,
                    0.9F + world.random.nextFloat() * 0.2F
            );
        }
    }

    private void extinguishAt(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
            world.breakBlock(pos, false);
            world.syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, pos, 0);
            return;
        }

        if (state.contains(Properties.LIT) && state.get(Properties.LIT)) {
            world.setBlockState(pos, state.with(Properties.LIT, false));
            world.syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, pos, 0);
        }
    }

    private void spawnSprayParticles(ServerWorld world, Vec3d origin, Vec3d direction, Vec3d sideways, Vec3d upwards) {
        Vec3d center = origin.add(direction.multiply(0.5D));
        Vec3d velocity = direction.multiply(0.4D);

        world.spawnParticles(
                ParticleTypes.CLOUD,
                center.x,
                center.y,
                center.z,
                24,
                Math.abs(sideways.x) * 0.22D + 0.08D,
                Math.abs(upwards.y) * 0.16D + 0.08D,
                Math.abs(sideways.z) * 0.22D + 0.08D,
                0.05D
        );
        world.spawnParticles(
                ParticleTypes.POOF,
                center.x + direction.x * 0.3D,
                center.y + direction.y * 0.3D,
                center.z + direction.z * 0.3D,
                12,
                0.16D,
                0.16D,
                0.16D,
                0.03D
        );
        world.spawnParticles(
                ParticleTypes.SMOKE,
                center.x + velocity.x,
                center.y + velocity.y,
                center.z + velocity.z,
                5,
                0.12D,
                0.08D,
                0.12D,
                0.01D
        );
    }

    private void logUsage(PlayerEntity user) {
        FireExtinguisherPrototype.LOGGER.info("{} used fire extinguisher at {}", user.getName().getString(), user.getBlockPos());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Hold right click to spray a dense extinguishing foam cone."));
        tooltip.add(Text.literal("Puts out fire, soul fire, candles, and campfires in front of you."));
    }
}
