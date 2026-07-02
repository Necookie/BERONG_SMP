package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.tutorial.TutorialManager;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TelemetryCsvWriter;

import java.util.function.Consumer;

/**
 * ABC dry-chemical extinguisher: puts out vanilla fire/soul-fire and any LIT block (candles,
 * campfires), and is the scored tool for the library FIRE scenario. Drives the safety tutorial's
 * PASS spray drill via {@link TutorialManager#onExtinguish}.
 *
 * <p>All pin/spray/durability mechanics live in {@link AbstractExtinguisherItem}; this class only
 * supplies the dry-chemical flavour, the block-suppression rules, and FIRE-scenario telemetry.
 */
public class FireExtinguisherItem extends AbstractExtinguisherItem {

    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    protected String pinPulledMessage() {
        return "§ePIN PULLED — ready to spray! Hold right-click to discharge.";
    }

    @Override
    protected void onPinPulled(ServerPlayer player) {
        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session != null) {
            session.logger.log("EXT_PIN_PULL", java.util.Map.of("pulled", true));
        }
    }

    @Override
    protected void onSprayStart(Player player) {
        BerongSMP.LOGGER.info("{} started using the fire extinguisher at {}",
                player.getName().getString(), player.blockPosition());
    }

    @Override
    protected boolean extinguishAt(ServerLevel level, BlockPos pos, Player user) {
        BlockState state = level.getBlockState(pos);
        boolean extinguished = false;

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0); // vanilla fire-extinguish hiss + smoke
            extinguished = true;
        } else if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
        } else if (isKitchenHazard(state.getBlock())) {
            if (net.necookie.disastersim.world.HazardManager.isHazardous(state) && user instanceof ServerPlayer sp) {
                warnWrongTool(sp, "§c✗ Dry chemical won't safely smother a grease fire — use the §eyellow wet chemical extinguisher§c!");
            }
        } else {
            SimulationSession hazardSession = user instanceof ServerPlayer sp ? SimulationManager.getSession(sp.getUUID()) : null;
            extinguished = net.necookie.disastersim.world.HazardManager.defuse(level, hazardSession, pos);
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

    @Override
    protected void onSprayResolved(ServerLevel level, ServerPlayer sp, ItemStack stack, boolean anyHit) {
        SimulationSession session = SimulationManager.getSession(sp.getUUID());
        if (session == null) return;

        // Periodic spray-accuracy sample for the EXT_SPRAY feature (every 20 charge units).
        if (stack.getDamageValue() % 20 == 0) {
            double nearestFire = nearestFireDist(level, sp.blockPosition());
            session.logger.log("EXT_SPRAY", java.util.Map.of(
                    "hit_fire", anyHit,
                    "distance_to_fire", Math.round(nearestFire * 10.0) / 10.0,
                    "nearby_player_count", countNearbyPlayers(level, sp, 5.0)));
        }

        // One throttled CSV row per "extinguisher hit fire" window.
        if (anyHit && session.consumeExtinguishEventPending()) {
            double elapsedS = (double) (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
            double hazDist = nearestFireDist(level, sp.blockPosition());
            session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                    session.getSessionId(), sp.getUUID().toString(),
                    session.getState().name().toLowerCase(),
                    Math.round(elapsedS * 100.0) / 100.0, "extinguisher_use",
                    sp.getX(), sp.getY(), sp.getZ(),
                    Math.round(hazDist * 100.0) / 100.0,
                    "fire_block", countNearbyPlayers(level, sp, 5.0)));
        }
    }

    @Override
    protected float sprayPitch(boolean sputtering, RandomSource random) {
        return sputtering
                ? 1.2F + random.nextFloat() * 0.3F
                : 0.9F + random.nextFloat() * 0.2F;
    }

    // Tighter scan box than SimulationManager's hazard scan — this feeds the player-facing
    // spray-accuracy metric, where only nearby flames are relevant.
    private static double nearestFireDist(ServerLevel level, BlockPos origin) {
        double minDist = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            BlockState bs = level.getBlockState(check);
            if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) {
                double d = origin.distSqr(check);
                if (d < minDist) minDist = d;
            }
        }
        return minDist == Double.MAX_VALUE ? 99.0 : Math.sqrt(minDist);
    }

    @Override
    protected void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 direction,
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
                center.x + direction.x * 0.3D, center.y + direction.y * 0.3D, center.z + direction.z * 0.3D,
                poofCount, 0.16D, 0.16D, 0.16D, 0.03D);

        level.sendParticles(ParticleTypes.SMOKE,
                center.x + direction.x * 0.4D, center.y + direction.y * 0.4D, center.z + direction.z * 0.4D,
                smokeCount, 0.12D, 0.08D, 0.12D, 0.01D);
    }

    @Override
    protected void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 direction, boolean sputtering) {
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
        appendPinStatusTooltip(stack, tooltip, "Hold right-click to spray.");
        appendChargeTooltip(stack, tooltip);
        tooltip.accept(Component.literal("§7Puts out fire, soul fire, candles, and campfires."));
        tooltip.accept(Component.literal("§c⚠ Not rated for §e[Class F/K]§c kitchen grease fires!"));
    }
}
