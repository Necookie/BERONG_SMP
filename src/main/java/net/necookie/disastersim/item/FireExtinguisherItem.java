package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.tutorial.TutorialManager;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

import net.necookie.disastersim.world.TelemetryCsvWriter;
import net.necookie.disastersim.Config;

import java.util.function.Consumer;

public class FireExtinguisherItem extends Item {

    private static final double SPRAY_RANGE = 5.5D;
    private static final int MAX_USE_TICKS = 72_000;
    private static final String TAG_PIN_PULLED = "pin_pulled";

    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    public static boolean isPinPulled(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyTag().getBoolean(TAG_PIN_PULLED).orElse(false);
    }

    public static void pullPin(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_PIN_PULLED, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isPinPulled(stack)) {
            if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
            pullPin(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.sendSystemMessage(Component.literal("§ePIN PULLED — ready to spray! Hold right-click to discharge."));
            if (player instanceof ServerPlayer sp) {
                SimulationSession session = SimulationManager.getSession(sp.getUUID());
                if (session != null) {
                    session.logger.log("EXT_PIN_PULL", java.util.Map.of("pulled", true));
                }
            }
            return InteractionResult.SUCCESS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        if (!isPinPulled(stack)) {
            user.stopUsingItem();
            return;
        }

        boolean sputtering = stack.getDamageValue() >= stack.getMaxDamage() - 60;

        if (sputtering && remainingUseTicks % 2 != 0) return;

        boolean playSound = remainingUseTicks % 6 == 0;

        if (level instanceof ServerLevel serverLevel) {
            sprayServer(serverLevel, player, stack, playSound, sputtering);

            if (remainingUseTicks == MAX_USE_TICKS) {
                BerongSMP.LOGGER.info("{} started using the fire extinguisher at {}",
                        player.getName().getString(), player.blockPosition());
            }
        } else {
            sprayClient(level, player, playSound, sputtering);
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

    private void sprayServer(ServerLevel level, Player user, ItemStack stack,
                             boolean playSound, boolean sputtering) {
        Vec3 lookDirection = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(lookDirection.scale(0.75D));

        Vec3 sideways = lookDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }
        Vec3 upwards = sideways.cross(lookDirection).normalize();

        boolean anyHit = false;
        for (int i = 1; i <= 7; i++) {
            double distance = i * (SPRAY_RANGE / 7.0D);
            Vec3 centerPoint = origin.add(lookDirection.scale(distance));

            if (extinguishAt(level, BlockPos.containing(centerPoint), user)) anyHit = true;
            if (extinguishAt(level, BlockPos.containing(centerPoint.add(sideways.scale(0.35D))), user)) anyHit = true;
            if (extinguishAt(level, BlockPos.containing(centerPoint.add(sideways.scale(-0.35D))), user)) anyHit = true;
            if (extinguishAt(level, BlockPos.containing(centerPoint.add(upwards.scale(0.25D))), user)) anyHit = true;
            if (extinguishAt(level, BlockPos.containing(centerPoint.add(upwards.scale(-0.2D))), user)) anyHit = true;
        }

        if (user instanceof ServerPlayer sp) {
            SimulationSession session = SimulationManager.getSession(sp.getUUID());
            if (session != null && stack.getDamageValue() % 20 == 0) {
                double nearestFire = nearestFireDist(level, user.blockPosition());
                boolean finalAnyHit = anyHit;
                session.logger.log("EXT_SPRAY", java.util.Map.of(
                    "hit_fire", finalAnyHit,
                    "distance_to_fire", Math.round(nearestFire * 10.0) / 10.0
                ));
            }
        }

        if (anyHit && user instanceof ServerPlayer sp) {
            SimulationSession session = SimulationManager.getSession(sp.getUUID());
            if (session != null && session.consumeExtinguishEventPending()) {
                double elapsedS = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
                double hazDist = nearestFireDist(level, sp.blockPosition());
                int nearbyPlayers = countNearbyPlayers(level, sp, 5.0);
                TelemetryCsvWriter.writeRow(
                        session.getSessionId(), sp.getUUID().toString(),
                        session.getState().name().toLowerCase(),
                        Math.round(elapsedS * 100.0) / 100.0, "extinguisher_use",
                        sp.getX(), sp.getY(), sp.getZ(),
                        Math.round(hazDist * 100.0) / 100.0,
                        "fire_block", nearbyPlayers);
            }
        }

        spawnSprayParticlesServer(level, origin, lookDirection, sideways, upwards, sputtering);

        if (playSound) {
            float pitch = sputtering
                    ? 1.2F + level.getRandom().nextFloat() * 0.3F
                    : 0.9F + level.getRandom().nextFloat() * 0.2F;
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, pitch);
        }

        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    private void sprayClient(Level level, Player user, boolean playSound, boolean sputtering) {
        Vec3 lookDirection = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(lookDirection.scale(0.75D));

        spawnSprayParticlesClient(level, origin, lookDirection, sputtering);

        if (playSound) {
            float pitch = sputtering
                    ? 1.2F + level.getRandom().nextFloat() * 0.3F
                    : 0.9F + level.getRandom().nextFloat() * 0.2F;
            level.playLocalSound(user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, pitch, false);
        }
    }

    private boolean extinguishAt(ServerLevel level, BlockPos pos, Player user) {
        BlockState state = level.getBlockState(pos);
        boolean extinguished = false;

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
        } else if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
        }

        if (extinguished && user instanceof ServerPlayer serverPlayer) {
            SimulationSession session = SimulationManager.getSession(serverPlayer.getUUID());
            if (session != null && session.getState() == SimulationManager.SimulationState.FIRE) {
                session.recordExtinguish(1);
            }
            TutorialManager.onExtinguish(serverPlayer);
        }
        return extinguished;
    }

    private static int countNearbyPlayers(ServerLevel level, ServerPlayer self, double radius) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                self.getX() - radius, self.getY() - radius, self.getZ() - radius,
                self.getX() + radius, self.getY() + radius, self.getZ() + radius);
        return (int) level.getEntitiesOfClass(Player.class, box,
                p -> !p.getUUID().equals(self.getUUID())).size();
    }

    private static double nearestFireDist(ServerLevel level, BlockPos origin) {
        double minDist = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            if (level.getBlockState(check).is(Blocks.FIRE) || level.getBlockState(check).is(Blocks.SOUL_FIRE)) {
                double d = origin.distSqr(check);
                if (d < minDist) minDist = d;
            }
        }
        return minDist == Double.MAX_VALUE ? 99.0 : Math.sqrt(minDist);
    }

    private void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 direction,
                                           Vec3 sideways, Vec3 upwards, boolean sputtering) {
        Vec3 center = origin.add(direction.scale(0.5D));
        int cloudCount = sputtering ? 10 : 24;
        int poofCount  = sputtering ? 5  : 12;
        int smokeCount = sputtering ? 2  : 5;

        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, cloudCount,
                Math.abs(sideways.x) * 0.22D + 0.08D,
                Math.abs(upwards.y) * 0.16D + 0.08D,
                Math.abs(sideways.z) * 0.22D + 0.08D, 0.05D);

        level.sendParticles(ParticleTypes.POOF,
                center.x + direction.x * 0.3D,
                center.y + direction.y * 0.3D,
                center.z + direction.z * 0.3D,
                poofCount, 0.16D, 0.16D, 0.16D, 0.03D);

        level.sendParticles(ParticleTypes.SMOKE,
                center.x + direction.x * 0.4D,
                center.y + direction.y * 0.4D,
                center.z + direction.z * 0.4D,
                smokeCount, 0.12D, 0.08D, 0.12D, 0.01D);
    }

    private void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 direction, boolean sputtering) {
        Vec3 center = origin.add(direction.scale(0.5D));
        int count = sputtering ? 5 : 12;
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.CLOUD,
                    center.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    center.y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    center.z + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    direction.x * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05,
                    direction.y * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05,
                    direction.z * 0.2 + (level.getRandom().nextDouble() - 0.5) * 0.05);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (!isPinPulled(stack)) {
            tooltip.accept(Component.literal("§c[PIN IN] Right-click to pull the pin first."));
        } else {
            tooltip.accept(Component.literal("§a[PIN PULLED] Hold right-click to spray."));
        }

        int maxDmg = stack.getMaxDamage();
        if (maxDmg > 0) {
            int charge = (int) ((1.0 - (double) stack.getDamageValue() / maxDmg) * 100);
            if (charge < 20) {
                tooltip.accept(Component.literal("§cAlmost empty! Charge: " + charge + "%"));
            } else {
                tooltip.accept(Component.literal("§7Charge: " + charge + "%"));
            }
        }

        tooltip.accept(Component.literal("§7Puts out fire, soul fire, candles, and campfires."));
    }
}
