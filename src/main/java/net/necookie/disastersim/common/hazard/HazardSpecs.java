package net.necookie.disastersim.common.hazard;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.Block.box;

/**
 * {@link HazardSpec} table for the 8 hazard props migrated off dedicated subclasses as a
 * proof-of-concept (2026-07-14) — see {@link HazardSpec}'s javadoc for scope. Every value here is
 * transcribed verbatim from the deleted per-prop class it replaces (shape boxes, particle bodies,
 * failure delay, both message strings, failure action + arguments) — checked one field at a time
 * against the original before that class was deleted, not rewritten.
 */
public final class HazardSpecs {

    private HazardSpecs() {}

    /** Was {@code CorrodedGasLineJointBlock}. Fixed gas-line elbow joint, corrosion pinholes weeping gas. */
    public static final HazardSpec CORRODED_GAS_LINE_JOINT = new HazardSpec(
            HazardSpec.shape4Way(
                    box(4, 4, 12, 12, 10, 16),
                    box(4, 4, 0, 12, 10, 4),
                    box(0, 4, 4, 4, 10, 12),
                    box(12, 4, 4, 16, 10, 12)),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
                double y = pos.getY() + 0.35 + rand.nextDouble() * 0.15;
                double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.01, 0);
            },
            300,
            "§a✔ Prevented! You shut the upstream line and clamped the joint.",
            "§c⚠ Gas from the corroded pipe joint finds a spark and flashes over!",
            (level, pos, state) -> HazardFacingBlock.igniteRadius(level, pos, 2, 2));

    /** Was {@code EbikeChargingStationBlock}. E-bike pack + charger warming and hissing overnight. */
    public static final HazardSpec EBIKE_CHARGING_STATION = new HazardSpec(
            HazardSpec.shapeNsEw(box(1, 0, 4, 15, 12, 12), box(4, 0, 1, 12, 12, 15)),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
                double y = pos.getY() + 0.4;
                double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
            },
            300,
            "§a✔ Prevented! You unplugged the cheap charger before the pack ran away.",
            "§c⚠ The e-bike battery goes into thermal runaway and bursts into flame!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 2));

    /** Was {@code DryAquariumHeaterBlock}. Office fish tank whose dropped water level exposes the heater. */
    public static final VoxelShape DRY_AQUARIUM_HEATER_SHAPE = box(1, 0, 1, 15, 10, 15);
    public static final HazardSpec DRY_AQUARIUM_HEATER = new HazardSpec(
            HazardSpec.fixedShape(DRY_AQUARIUM_HEATER_SHAPE),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
                double y = pos.getY() + 0.6;
                double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
                level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.2, z, 0, -0.02, 0);
            },
            400,
            "§a✔ Prevented! You topped up the tank and unplugged the exposed heater.",
            "§c⚠ The exposed heater scorches through the acrylic lid and ignites it!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 1));

    /** Was {@code RodentChewedWiringBlock}. Baseboard cable run gnawed to bare copper, arcing hidden from view. */
    public static final HazardSpec RODENT_CHEWED_WIRING = new HazardSpec(
            HazardSpec.shape4Way(
                    box(2, 0, 15, 14, 2, 16),
                    box(2, 0, 0, 14, 2, 1),
                    box(0, 0, 2, 1, 2, 14),
                    box(15, 0, 2, 16, 2, 14)),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.4 + rand.nextDouble() * 0.2;
                double y = pos.getY() + 0.15;
                double z = pos.getZ() + 0.4 + rand.nextDouble() * 0.2;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.008, 0);
            },
            350,
            "§a✔ Prevented! You spliced and re-insulated the chewed section.",
            "§c⚠ The gnawed wiring arcs through the baseboard and ignites it!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 1));

    /** Was {@code DustyCrtMonitorBlock}. Old CRT monitor whose flyback transformer arcs through years of dust. */
    public static final VoxelShape DUSTY_CRT_MONITOR_SHAPE = box(2, 0, 2, 14, 11, 14);
    public static final HazardSpec DUSTY_CRT_MONITOR = new HazardSpec(
            HazardSpec.fixedShape(DUSTY_CRT_MONITOR_SHAPE),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
            },
            450,
            "§a✔ Prevented! You unplugged the old CRT and cleared the dust from its vents.",
            "§c⚠ The flyback transformer arcs through the dust and ignites the casing!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 1));

    /** Was {@code ShortedBenchSupplyBlock}. Benchtop DC power supply shorting under a paper schematic. */
    public static final VoxelShape SHORTED_BENCH_SUPPLY_SHAPE = box(2, 0, 2, 14, 8, 14);
    public static final HazardSpec SHORTED_BENCH_SUPPLY = new HazardSpec(
            HazardSpec.fixedShape(SHORTED_BENCH_SUPPLY_SHAPE),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
                double y = pos.getY() + 0.4;
                double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
            },
            400,
            "§a✔ Prevented! You cleared the shorted leads and reset the current limit.",
            "§c⚠ The shorted leads arc and ignite the schematic paper!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 1));

    /** Was {@code OverheatedVacuumPumpBlock} (no facing). Rotary-vane pump on degraded oil, belt slipping. */
    public static final VoxelShape OVERHEATED_VACUUM_PUMP_SHAPE = box(3, 0, 3, 13, 8, 13);
    public static final HazardSpec OVERHEATED_VACUUM_PUMP = new HazardSpec(
            HazardSpec.fixedShape(OVERHEATED_VACUUM_PUMP_SHAPE),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.35 + rand.nextDouble() * 0.3;
                double y = pos.getY() + 0.4;
                double z = pos.getZ() + 0.35 + rand.nextDouble() * 0.3;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.01, 0);
            },
            450,
            "§a✔ Prevented! You shut off the pump and scheduled an oil change.",
            "§c⚠ The degraded oil and slipping belt finally ignite!",
            (level, pos, state) -> HazardBlock.igniteAdjacent(level, pos, 1));

    /** Was {@code FaultyDehumidifierBlock}. Lab dehumidifier with an iced coil, compressor short-cycling. */
    public static final VoxelShape FAULTY_DEHUMIDIFIER_SHAPE = box(2, 0, 2, 14, 12, 14);
    public static final HazardSpec FAULTY_DEHUMIDIFIER = new HazardSpec(
            HazardSpec.fixedShape(FAULTY_DEHUMIDIFIER_SHAPE),
            (level, pos, state, rand) -> {
                if (rand.nextInt(3) != 0) return;
                double x = pos.getX() + 0.3 + rand.nextDouble() * 0.4;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.3 + rand.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
                level.addParticle(ParticleTypes.DRIPPING_WATER, x, y + 0.2, z, 0, -0.02, 0);
            },
            400,
            "§a✔ Prevented! You unplugged the unit and let the iced coil thaw.",
            "§c⚠ The short-cycling compressor overheats and ignites!",
            (level, pos, state) -> HazardFacingBlock.igniteAdjacent(level, pos, 1));
}
