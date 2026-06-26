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
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TelemetryCsvWriter;

import java.util.function.Consumer;

public class CO2ExtinguisherItem extends Item {

    private static final double SPRAY_RANGE = 5.5D;
    private static final int MAX_USE_TICKS = 72_000;
    private static final String TAG_PIN_PULLED = "pin_pulled";

    public CO2ExtinguisherItem(Properties properties) {
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
            player.sendSystemMessage(Component.literal(
                    "§ePIN PULLED — CO2 extinguisher ready! Hold right-click to discharge."));
            return InteractionResult.SUCCESS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        if (!isPinPulled(stack)) { user.stopUsingItem(); return; }

        boolean sputtering = stack.getDamageValue() >= stack.getMaxDamage() - 60;
        if (sputtering && remainingUseTicks % 2 != 0) return;

        boolean playSound = remainingUseTicks % 6 == 0;

        if (level instanceof ServerLevel serverLevel) {
            sprayServer(serverLevel, player, stack, playSound, sputtering);
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
        Vec3 look = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(look.scale(0.75D));

        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        else side = side.normalize();
        Vec3 up = side.cross(look).normalize();

        boolean anyHit = false;
        for (int i = 1; i <= 7; i++) {
            double d = i * (SPRAY_RANGE / 7.0);
            Vec3 c = origin.add(look.scale(d));
            anyHit |= extinguishAt(level, BlockPos.containing(c), user);
            anyHit |= extinguishAt(level, BlockPos.containing(c.add(side.scale( 0.35))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(c.add(side.scale(-0.35))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(c.add(up.scale( 0.25))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(c.add(up.scale(-0.20))), user);
        }

        if (anyHit && user instanceof ServerPlayer sp) {
            SimulationSession session = SimulationManager.getSession(sp.getUUID());
            if (session != null) {
                // Track extinguish count for CCS fire scoring (same counter as regular FIRE)
                if (session.getState() == SimulationManager.SimulationState.CCS_FIRE) {
                    session.recordExtinguish(1);
                }
                if (session.consumeExtinguishEventPending()) {
                    double elapsedS = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
                    double hazDist = SimulationManager.nearestFireDistance(level, sp.blockPosition());
                    int nearbyPlayers = countNearbyPlayers(level, sp, 5.0);
                    TelemetryCsvWriter.writeRow(
                            session.getSessionId(), sp.getUUID().toString(),
                            session.getState().name().toLowerCase(),
                            Math.round(elapsedS * 100.0) / 100.0, "extinguisher_use",
                            sp.getX(), sp.getY(), sp.getZ(),
                            Math.round(hazDist * 100.0) / 100.0,
                            "co2_extinguisher", nearbyPlayers);
                }
            }
        }

        if (user instanceof ServerPlayer sp) {
            SimulationSession session = SimulationManager.getSession(sp.getUUID());
            if (session != null && stack.getDamageValue() % 20 == 0) {
                session.logger.log("extinguisher_use", java.util.Map.of(
                        "hit_target", anyHit,
                        "nearby_player_count", countNearbyPlayers(level, sp, 5.0)));
            }
        }

        spawnParticlesServer(level, origin, look, side, up, sputtering);

        if (playSound) {
            float pitch = sputtering
                    ? 1.4F + level.getRandom().nextFloat() * 0.3F
                    : 1.1F + level.getRandom().nextFloat() * 0.2F;
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
        Vec3 look = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(look.scale(0.75D));
        spawnParticlesClient(level, origin, look, sputtering);

        if (playSound) {
            float pitch = sputtering
                    ? 1.4F + level.getRandom().nextFloat() * 0.3F
                    : 1.1F + level.getRandom().nextFloat() * 0.2F;
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
        } else if (state.getBlock() instanceof ComputerBlock
                && state.hasProperty(ComputerBlock.BURNING)
                && state.getValue(ComputerBlock.BURNING)) {
            level.setBlock(pos, state
                    .setValue(ComputerBlock.BURNING, false)
                    .setValue(ComputerBlock.LIT, false)
                    .setValue(ComputerBlock.BROKEN, true), 3);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
            if (user instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        "§a✓ Electrical fire suppressed! §7(Computer destroyed — replace it)"));
            }
        }

        if (extinguished) {
            BerongSMP.LOGGER.debug("CO2 extinguisher suppressed block at {}", pos);
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

    private void spawnParticlesServer(ServerLevel level, Vec3 origin, Vec3 look,
                                      Vec3 side, Vec3 up, boolean sputtering) {
        Vec3 c = origin.add(look.scale(0.5));
        int snowCount  = sputtering ? 8  : 20;
        int cloudCount = sputtering ? 6  : 14;
        int poofCount  = sputtering ? 3  : 8;

        // Snowflake particles give the cold CO2 discharge look
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y, c.z, snowCount,
                Math.abs(side.x) * 0.25 + 0.06,
                Math.abs(up.y)   * 0.18 + 0.06,
                Math.abs(side.z) * 0.25 + 0.06, 0.06);

        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, cloudCount,
                Math.abs(side.x) * 0.20 + 0.05,
                Math.abs(up.y)   * 0.14 + 0.05,
                Math.abs(side.z) * 0.20 + 0.05, 0.04);

        level.sendParticles(ParticleTypes.POOF,
                c.x + look.x * 0.3, c.y + look.y * 0.3, c.z + look.z * 0.3,
                poofCount, 0.14, 0.14, 0.14, 0.02);
    }

    private void spawnParticlesClient(Level level, Vec3 origin, Vec3 look, boolean sputtering) {
        Vec3 c = origin.add(look.scale(0.5));
        int n = sputtering ? 4 : 10;
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    c.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.z + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    look.x * 0.18 + (level.getRandom().nextDouble() - 0.5) * 0.04,
                    look.y * 0.18 + (level.getRandom().nextDouble() - 0.5) * 0.04,
                    look.z * 0.18 + (level.getRandom().nextDouble() - 0.5) * 0.04);
        }
        for (int i = 0; i < n / 2; i++) {
            level.addParticle(ParticleTypes.CLOUD,
                    c.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.z + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    look.x * 0.15, look.y * 0.15, look.z * 0.15);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (!isPinPulled(stack)) {
            tooltip.accept(Component.literal("§c[PIN IN] Right-click to pull the pin first."));
        } else {
            tooltip.accept(Component.literal("§a[PIN PULLED] Hold right-click to discharge CO2."));
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

        tooltip.accept(Component.literal("§7Use on §c[Class C]§7 electrical fires (computers)."));
        tooltip.accept(Component.literal("§7Also suppresses regular fire and soul fire."));
        tooltip.accept(Component.literal("§c⚠ NEVER use water on electrical fires!"));
    }
}
