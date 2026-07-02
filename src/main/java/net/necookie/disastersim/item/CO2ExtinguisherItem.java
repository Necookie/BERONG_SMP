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
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TelemetryCsvWriter;

import java.util.function.Consumer;

/**
 * CO2 (clean-agent) extinguisher: the correct Class-C tool for the CCS electrical-fire scenario.
 * Suppresses a burning {@link ComputerBlock} (leaving it BROKEN) and also handles ordinary fire and
 * soul fire. Cold-discharge snowflake flavour distinguishes it from the dry-chemical unit.
 *
 * <p>Shared mechanics live in {@link AbstractExtinguisherItem}; this class supplies the
 * computer-suppression rule and CCS-scenario telemetry.
 */
public class CO2ExtinguisherItem extends AbstractExtinguisherItem {

    public CO2ExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    protected String pinPulledMessage() {
        return "§ePIN PULLED — CO2 extinguisher ready! Hold right-click to discharge.";
    }

    @Override
    protected boolean extinguishAt(ServerLevel level, BlockPos pos, Player user) {
        BlockState state = level.getBlockState(pos);
        SimulationSession hazardSession = user instanceof ServerPlayer sp ? SimulationManager.getSession(sp.getUUID()) : null;
        boolean extinguished = false;

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
        } else if (state.getBlock() instanceof ComputerBlock
                && state.hasProperty(ComputerBlock.BURNING)
                && state.getValue(ComputerBlock.BURNING)) {
            // Electrical fire out, but the workstation is destroyed (BROKEN) and must be replaced.
            level.setBlock(pos, state
                    .setValue(ComputerBlock.BURNING, false)
                    .setValue(ComputerBlock.LIT, false)
                    .setValue(ComputerBlock.BROKEN, true), 3);
            level.levelEvent(null, 1009, pos, 0);
            if (hazardSession != null) hazardSession.getHazardTimers().remove(pos);
            extinguished = true;
            if (user instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        "§a✓ Electrical fire suppressed! §7(Computer destroyed — replace it)"));
            }
        } else if (isKitchenHazard(state.getBlock())) {
            if (net.necookie.disastersim.world.HazardManager.isHazardous(state) && user instanceof ServerPlayer sp) {
                warnWrongTool(sp, "§c✗ CO2 won't safely smother a grease fire — use the §eyellow wet chemical extinguisher§c!");
            }
        } else {
            extinguished = net.necookie.disastersim.world.HazardManager.defuse(level, hazardSession, pos);
        }

        if (extinguished) {
            BerongSMP.LOGGER.debug("CO2 extinguisher suppressed block at {}", pos);
        }
        return extinguished;
    }

    @Override
    protected void onSprayResolved(ServerLevel level, ServerPlayer sp, ItemStack stack, boolean anyHit) {
        SimulationSession session = SimulationManager.getSession(sp.getUUID());
        if (session == null) return;

        if (anyHit) {
            // Track suppression for CCS fire scoring (shares the FIRE extinguish counter).
            if (session.getState() == SimulationManager.SimulationState.CCS_FIRE) {
                session.recordExtinguish(1);
            }
            if (session.consumeExtinguishEventPending()) {
                double elapsedS = (double) (Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
                double hazDist = SimulationManager.nearestFireDistance(level, sp.blockPosition());
                session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                        session.getSessionId(), sp.getUUID().toString(),
                        session.getState().name().toLowerCase(),
                        Math.round(elapsedS * 100.0) / 100.0, "extinguisher_use",
                        sp.getX(), sp.getY(), sp.getZ(),
                        Math.round(hazDist * 100.0) / 100.0,
                        "co2_extinguisher", countNearbyPlayers(level, sp, 5.0)));
            }
        }

        // Periodic discharge sample (every 20 charge units), regardless of hit.
        if (stack.getDamageValue() % 20 == 0) {
            session.logger.log("extinguisher_use", java.util.Map.of(
                    "hit_target", anyHit,
                    "nearby_player_count", countNearbyPlayers(level, sp, 5.0)));
        }
    }

    @Override
    protected float sprayPitch(boolean sputtering, RandomSource random) {
        return sputtering
                ? 1.4F + random.nextFloat() * 0.3F
                : 1.1F + random.nextFloat() * 0.2F;
    }

    @Override
    protected void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 look,
                                             Vec3 side, Vec3 up, boolean sputtering) {
        Vec3 c = origin.add(look.scale(0.5));
        int snowCount  = sputtering ? 8  : 20;
        int cloudCount = sputtering ? 6  : 14;
        int poofCount  = sputtering ? 3  : 8;

        // Snowflakes sell the cold CO2 discharge.
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

    @Override
    protected void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 look, boolean sputtering) {
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
        appendPinStatusTooltip(stack, tooltip, "Hold right-click to discharge CO2.");
        appendChargeTooltip(stack, tooltip);
        tooltip.accept(Component.literal("§7Use on §c[Class C]§7 electrical fires (computers)."));
        tooltip.accept(Component.literal("§7Also suppresses regular fire and soul fire."));
        tooltip.accept(Component.literal("§c⚠ NEVER use water on electrical fires!"));
        tooltip.accept(Component.literal("§c⚠ Not rated for §e[Class F/K]§c kitchen grease fires!"));
    }
}
