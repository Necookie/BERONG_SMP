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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;

import java.util.List;
import java.util.function.Consumer;

/**
 * A functional Fire Extinguisher tool.
 * When used, it sprays a dense cone of extinguishing foam that puts out fires
 * and unlights blocks like candles and campfires within a specific range.
 */
public class FireExtinguisherItem extends Item {
    /** The effective range of the spray in blocks. */
    private static final double SPRAY_RANGE = 5.5D;
    
    /** The maximum duration the item can be held in use (effectively infinite). */
    private static final int MAX_USE_TICKS = 72_000;

    /**
     * Constructor for the Fire Extinguisher.
     * 
     * @param properties The item properties.
     */
    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    /**
     * Triggered when the player right-clicks with the item.
     * Starts the 'using' state for continuous spraying.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /**
     * Logic executed every tick while the item is being used (right-click held).
     * Handles spray calculation, sound effects, and fire extinguishing.
     */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (user instanceof Player player) {
            // Play the extinguish sound every 6 ticks (approx. 3 times per second) for a looping effect
            boolean playSound = remainingUseTicks % 6 == 0;
            
            if (level instanceof ServerLevel serverLevel) {
                // Server-side: Handle fire extinguishing and multi-player particle sync
                sprayServer(serverLevel, player, playSound);
                
                // Log usage once when the spray starts
                if (remainingUseTicks == MAX_USE_TICKS) {
                    logUsage(player);
                }
            } else {
                // Client-side: Handle local smooth particle feedback and local sounds
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
        // No vanilla animation (like eating or bow drawing)
        return ItemUseAnimation.NONE;
    }

    /**
     * Server-side spray logic.
     * Calculates the spray cone, extinguishes fires, and sends particles to all nearby players.
     * 
     * @param level The server level.
     * @param user The player using the extinguisher.
     * @param playSound Whether to play the sound effect this tick.
     */
    private void sprayServer(ServerLevel level, Player user, boolean playSound) {
        // --- Vector Math for Spray Cone ---
        // 1. Get the direction the player is looking
        Vec3 lookDirection = user.getViewVector(1.0F).normalize();
        
        // 2. Determine the starting point (slightly in front of eyes)
        Vec3 origin = user.getEyePosition().add(lookDirection.scale(0.75D));
        
        // 3. Calculate orthogonal vectors to define the width and height of the cone
        Vec3 sideways = lookDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D); // Edge case for looking straight up/down
        } else {
            sideways = sideways.normalize();
        }
        Vec3 upwards = sideways.cross(lookDirection).normalize();

        // 4. Sample points along the cone and extinguish fires
        for (int i = 1; i <= 7; i++) {
            double distance = i * (SPRAY_RANGE / 7.0D);
            Vec3 centerPoint = origin.add(lookDirection.scale(distance));
            
            // Extinguish at the center, left, right, top, and bottom of the cone cross-section
            extinguishAt(level, BlockPos.containing(centerPoint));
            extinguishAt(level, BlockPos.containing(centerPoint.add(sideways.scale(0.35D))));
            extinguishAt(level, BlockPos.containing(centerPoint.add(sideways.scale(-0.35D))));
            extinguishAt(level, BlockPos.containing(centerPoint.add(upwards.scale(0.25D))));
            extinguishAt(level, BlockPos.containing(centerPoint.add(upwards.scale(-0.2D))));
        }

        // 5. Sync spray particles to all clients in range
        spawnSprayParticlesServer(level, origin, lookDirection, sideways, upwards);

        // 6. Play the sound globally
        if (playSound) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), 
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 
                    0.9F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    /**
     * Client-side spray logic.
     * Handles local-only visual feedback for the player using the extinguisher.
     */
    private void sprayClient(Level level, Player user, boolean playSound) {
        Vec3 lookDirection = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(lookDirection.scale(0.75D));
        
        Vec3 sideways = lookDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }
        Vec3 upwards = sideways.cross(lookDirection).normalize();

        // Spawn high-frequency local particles for smoothness
        spawnSprayParticlesClient(level, origin, lookDirection);

        // Play local-only sound for the user
        if (playSound) {
            level.playLocalSound(user.getX(), user.getY(), user.getZ(), 
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 
                    0.9F + level.getRandom().nextFloat() * 0.2F, false);
        }
    }

    /**
     * Logic to extinguish fire or unlight a block at a specific position.
     * 
     * @param level The server level.
     * @param pos The block position to check.
     */
    private void extinguishAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Handle Fire and Soul Fire blocks
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0); // Play fire extinguish event (particles + sound)
            return;
        }

        // Handle blocks with the 'LIT' property (Candles, Campfires, etc.)
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.levelEvent(null, 1009, pos, 0);
        }
    }

    /**
     * Spawns networked particles for the spray.
     * Uses Cloud, Poof, and Smoke particles to simulate dense foam.
     */
    private void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 direction, Vec3 sideways, Vec3 upwards) {
        Vec3 center = origin.add(direction.scale(0.5D));
        
        // Dense Cloud particles form the main body of the foam
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 24, 
                Math.abs(sideways.x) * 0.22D + 0.08D, Math.abs(upwards.y) * 0.16D + 0.08D, Math.abs(sideways.z) * 0.22D + 0.08D, 0.05D);
        
        // Poof particles add "fluffiness" to the spray
        level.sendParticles(ParticleTypes.POOF, center.x + direction.x * 0.3D, center.y + direction.y * 0.3D, center.z + direction.z * 0.3D, 12, 0.16D, 0.16D, 0.16D, 0.03D);
        
        // Minimal Smoke particles for a lingering effect
        level.sendParticles(ParticleTypes.SMOKE, center.x + direction.x * 0.4D, center.y + direction.y * 0.4D, center.z + direction.z * 0.4D, 5, 0.12D, 0.08D, 0.12D, 0.01D);
    }

    /**
     * Spawns local-only particles for the player using the tool.
     * Provides immediate visual feedback without waiting for server sync.
     */
    private void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 direction) {
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

    /**
     * Logs the usage of the fire extinguisher for administrative/debugging purposes.
     */
    private void logUsage(Player user) {
        BerongSMP.LOGGER.info("{} started using the fire extinguisher at {}", user.getName().getString(), user.blockPosition());
    }

    /**
     * Adds instructional text to the item's tooltip.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Hold right click to spray a dense extinguishing foam cone."));
        tooltip.accept(Component.literal("§7Puts out fire, soul fire, candles, and campfires in front of you."));
    }
}
