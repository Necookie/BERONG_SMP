package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.necookie.disastersim.common.hazard.HazardManager;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;
import net.necookie.disastersim.world.TelemetryCsvWriter;

import java.util.function.Consumer;

/**
 * Wet chemical extinguisher: the correct Class F/K (Philippine BFP yellow-coded) tool for cooking-oil
 * and grease fires in the cafeteria/kitchen hazard props. Its potassium-acetate mist saponifies hot
 * fats into a foam blanket instead of blasting them apart — the opposite of what water does to a
 * grease fire (see {@code docs/md files/Items.md} #16: "Water triggers a 3x3 fiery explosion!").
 *
 * <p>Suppresses ordinary fire/soul fire like the other units, and defuses any hazard prop via
 * {@link HazardManager#defuse}, with distinct feedback (message + golden foam particles) when the
 * target is one of the five kitchen/grease hazard props. It is the <b>only</b> extinguisher that can
 * defuse those five — {@link FireExtinguisherItem} and {@link CO2ExtinguisherItem} skip them (see
 * {@link AbstractExtinguisherItem#isKitchenHazard}), mirroring the real Class B vs. Class F/K
 * distinction: a dry-chemical or CO2 unit can splash or re-flash a deep-fat fire instead of
 * smothering it. Shared mechanics live in {@link AbstractExtinguisherItem}.
 */
public class WetChemicalExtinguisherItem extends AbstractExtinguisherItem {

    /** Golden foam mist color (packed RGB), matching the yellow Class F/K body colour. */
    private static final int FOAM_COLOR = 0xE8C800;

    public WetChemicalExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    protected String pinPulledMessage() {
        return "§ePIN PULLED — wet chemical unit ready! Hold right-click to discharge.";
    }

    @Override
    protected boolean extinguishAt(ServerLevel level, BlockPos pos, Player user) {
        BlockState state = level.getBlockState(pos);
        boolean extinguished = false;
        boolean wasKitchenHazard = false;

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.destroyBlock(pos, false);
            level.levelEvent(null, 1009, pos, 0);
            extinguished = true;
        } else {
            wasKitchenHazard = isKitchenHazard(state.getBlock());
            SimulationSession hazardSession = user instanceof ServerPlayer sp ? SimulationManager.getSession(sp.getUUID()) : null;
            extinguished = HazardManager.defuse(level, hazardSession, pos);
        }

        if (extinguished) {
            if (wasKitchenHazard && user instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        "§a✓ Grease fire smothered — saponification complete!"));
            }
            BerongSMP.LOGGER.debug("Wet chemical extinguisher suppressed block at {}", pos);
        }
        return extinguished;
    }

    @Override
    protected void onSprayResolved(ServerLevel level, ServerPlayer sp, ItemStack stack, boolean anyHit) {
        SimulationSession session = SimulationManager.getSession(sp.getUUID());
        if (session == null) return;

        if (anyHit) {
            if (session.getState() == SimulationManager.SimulationState.FIRE
                    || session.getState() == SimulationManager.SimulationState.CCS_FIRE) {
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
                        "wet_chemical_extinguisher", countNearbyPlayers(level, sp, 5.0)));
            }
        }

        if (stack.getDamageValue() % 20 == 0) {
            session.logger.log("extinguisher_use", java.util.Map.of(
                    "hit_target", anyHit,
                    "nearby_player_count", countNearbyPlayers(level, sp, 5.0)));
        }
    }

    @Override
    protected float sprayPitch(boolean sputtering, RandomSource random) {
        return sputtering
                ? 1.0F + random.nextFloat() * 0.3F
                : 0.75F + random.nextFloat() * 0.2F;
    }

    @Override
    protected void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 look,
                                             Vec3 side, Vec3 up, boolean sputtering) {
        Vec3 c = origin.add(look.scale(0.5));
        int foamCount  = sputtering ? 8  : 18;
        int poofCount  = sputtering ? 3  : 8;
        int mistCount  = sputtering ? 2  : 5;

        // Golden foam mist sells the wet-chemical (not water, not CO2) discharge.
        level.sendParticles(new DustParticleOptions(FOAM_COLOR, 1.3F), c.x, c.y, c.z, foamCount,
                Math.abs(side.x) * 0.24 + 0.06,
                Math.abs(up.y)   * 0.16 + 0.06,
                Math.abs(side.z) * 0.24 + 0.06, 0.05);

        level.sendParticles(ParticleTypes.POOF,
                c.x + look.x * 0.3, c.y + look.y * 0.3, c.z + look.z * 0.3,
                poofCount, 0.14, 0.14, 0.14, 0.02);

        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                c.x + look.x * 0.4, c.y + look.y * 0.4, c.z + look.z * 0.4,
                mistCount, 0.1, 0.08, 0.1, 0.01);
    }

    @Override
    protected void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 look, boolean sputtering) {
        Vec3 c = origin.add(look.scale(0.5));
        int n = sputtering ? 4 : 10;
        for (int i = 0; i < n; i++) {
            level.addParticle(new DustParticleOptions(FOAM_COLOR, 1.3F),
                    c.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.z + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    look.x * 0.16 + (level.getRandom().nextDouble() - 0.5) * 0.04,
                    look.y * 0.16 + (level.getRandom().nextDouble() - 0.5) * 0.04,
                    look.z * 0.16 + (level.getRandom().nextDouble() - 0.5) * 0.04);
        }
        for (int i = 0; i < n / 3; i++) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    c.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    c.z + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    look.x * 0.12, look.y * 0.12, look.z * 0.12);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        appendPinStatusTooltip(stack, tooltip, "Hold right-click to discharge.");
        appendChargeTooltip(stack, tooltip);
        tooltip.accept(Component.literal("§7Use on §e[Class F/K]§7 grease & cooking-oil fires (kitchen hazards)."));
        tooltip.accept(Component.literal("§7Also suppresses regular fire and soul fire."));
        tooltip.accept(Component.literal("§c⚠ NEVER use water on a grease fire — it will explode!"));
    }
}
