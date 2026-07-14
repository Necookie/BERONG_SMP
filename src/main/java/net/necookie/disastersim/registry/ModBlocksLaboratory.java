package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BalanceScaleTableBlock;
import net.necookie.disastersim.block.ComponentDrawerCabinetBlock;
import net.necookie.disastersim.block.EquipmentRackBlock;
import net.necookie.disastersim.block.EyeWashStationBlock;
import net.necookie.disastersim.block.LabStoolBlock;
import net.necookie.disastersim.block.LaboratoryFumeHoodBlock;
import net.necookie.disastersim.block.MicroscopeStationBlock;
import net.necookie.disastersim.block.OscilloscopeCartBlock;
import net.necookie.disastersim.block.SampleStorageRackBlock;
import net.necookie.disastersim.block.SecuredCylinderRackBlock;
import net.necookie.disastersim.common.hazard.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Research & Instrumentation Laboratory furniture + hazard prop registrations (props 76-85) —
 * split out of {@link ModBlocks} (see its class javadoc). Package-private: {@link ModBlocks}
 * re-exports every field below in the same declared order. Furniture and hazards are interleaved
 * in a single file because that's how the original section was ordered.
 */
final class ModBlocksLaboratory {

    private ModBlocksLaboratory() {}

    // ── Laboratory furniture ─────────────────────────────────────────────

    /** Full-height fume hood with glass sash — see {@link LaboratoryFumeHoodBlock}. */
    static final DeferredBlock<LaboratoryFumeHoodBlock> LABORATORY_FUME_HOOD = ModBlocks.BLOCKS.registerBlock(
            "laboratory_fume_hood", LaboratoryFumeHoodBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Floor-standing 19-inch instrument rack — see {@link EquipmentRackBlock}. */
    static final DeferredBlock<EquipmentRackBlock> EQUIPMENT_RACK = ModBlocks.BLOCKS.registerBlock(
            "equipment_rack", EquipmentRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Adjustable lab stool on a steel column — see {@link LabStoolBlock}. */
    static final DeferredBlock<LabStoolBlock> LAB_STOOL = ModBlocks.BLOCKS.registerBlock(
            "lab_stool", LabStoolBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Rolling oscilloscope instrument cart — see {@link OscilloscopeCartBlock}. */
    static final DeferredBlock<OscilloscopeCartBlock> OSCILLOSCOPE_CART = ModBlocks.BLOCKS.registerBlock(
            "oscilloscope_cart", OscilloscopeCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Bench microscope on a small stand — see {@link MicroscopeStationBlock}. */
    static final DeferredBlock<MicroscopeStationBlock> MICROSCOPE_STATION = ModBlocks.BLOCKS.registerBlock(
            "microscope_station", MicroscopeStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Green wall eyewash fountain — see {@link EyeWashStationBlock}. */
    static final DeferredBlock<EyeWashStationBlock> EYE_WASH_STATION = ModBlocks.BLOCKS.registerBlock(
            "eye_wash_station", EyeWashStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall cabinet of labeled electronics-parts drawers — see {@link ComponentDrawerCabinetBlock}. */
    static final DeferredBlock<ComponentDrawerCabinetBlock> COMPONENT_DRAWER_CABINET = ModBlocks.BLOCKS.registerBlock(
            "component_drawer_cabinet", ComponentDrawerCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall rack of properly chained gas cylinders — see {@link SecuredCylinderRackBlock}. */
    static final DeferredBlock<SecuredCylinderRackBlock> SECURED_CYLINDER_RACK = ModBlocks.BLOCKS.registerBlock(
            "secured_cylinder_rack", SecuredCylinderRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Shelf of labeled sample boxes and vial trays — see {@link SampleStorageRackBlock}. */
    static final DeferredBlock<SampleStorageRackBlock> SAMPLE_STORAGE_RACK = ModBlocks.BLOCKS.registerBlock(
            "sample_storage_rack", SampleStorageRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Anti-vibration granite balance table — see {@link BalanceScaleTableBlock}. */
    static final DeferredBlock<BalanceScaleTableBlock> BALANCE_SCALE_TABLE = ModBlocks.BLOCKS.registerBlock(
            "balance_scale_table", BalanceScaleTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.STONE).noOcclusion());

    // ── Laboratory hazards (props 76-85) ────────────────────────────────────

    /** Unbalanced centrifuge rotor walking the bench — see {@link UnbalancedCentrifugeBlock}. */
    static final DeferredBlock<UnbalancedCentrifugeBlock> UNBALANCED_CENTRIFUGE = ModBlocks.BLOCKS.registerBlock(
            "unbalanced_centrifuge", UnbalancedCentrifugeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** 3D printer thermal-runaway on a dislodged heater cartridge — see {@link Runaway3dPrinterBlock}. */
    static final DeferredBlock<Runaway3dPrinterBlock> RUNAWAY_3D_PRINTER = ModBlocks.BLOCKS.registerBlock(
            "runaway_3d_printer", Runaway3dPrinterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Soldering iron left powered at full temperature — see {@link UnattendedSolderingIronBlock}. */
    static final DeferredBlock<UnattendedSolderingIronBlock> UNATTENDED_SOLDERING_IRON = ModBlocks.BLOCKS.registerBlock(
            "unattended_soldering_iron", UnattendedSolderingIronBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Class-4 test laser running with its beam dump missing — see {@link UnshieldedTestLaserBlock}. */
    static final DeferredBlock<UnshieldedTestLaserBlock> UNSHIELDED_TEST_LASER = ModBlocks.BLOCKS.registerBlock(
            "unshielded_test_laser", UnshieldedTestLaserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Leaking oxygen cylinder — oxidizer enrichment, CO2-only — see {@link LeakingOxygenCylinderBlock}. */
    static final DeferredBlock<LeakingOxygenCylinderBlock> LEAKING_OXYGEN_CYLINDER = ModBlocks.BLOCKS.registerBlock(
            "leaking_oxygen_cylinder", LeakingOxygenCylinderBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Shorted bench DC power supply — see {@link ShortedBenchSupplyBlock}. */
    static final DeferredBlock<ShortedBenchSupplyBlock> SHORTED_BENCH_SUPPLY = ModBlocks.BLOCKS.registerBlock(
            "shorted_bench_supply", ShortedBenchSupplyBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Vacuum pump running on degraded oil, belt slipping — see {@link OverheatedVacuumPumpBlock}. */
    static final DeferredBlock<OverheatedVacuumPumpBlock> OVERHEATED_VACUUM_PUMP = ModBlocks.BLOCKS.registerBlock(
            "overheated_vacuum_pump", OverheatedVacuumPumpBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Environmental chamber with a welded heater relay — see {@link StuckEnvironmentChamberBlock}. */
    static final DeferredBlock<StuckEnvironmentChamberBlock> STUCK_ENVIRONMENT_CHAMBER = ModBlocks.BLOCKS.registerBlock(
            "stuck_environment_chamber", StuckEnvironmentChamberBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Drying oven with flammable solvent vapor accumulating — see {@link SolventDryingOvenBlock}. */
    static final DeferredBlock<SolventDryingOvenBlock> SOLVENT_DRYING_OVEN = ModBlocks.BLOCKS.registerBlock(
            "solvent_drying_oven", SolventDryingOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Dehumidifier with an iced coil, compressor short-cycling — see {@link FaultyDehumidifierBlock}. */
    static final DeferredBlock<FaultyDehumidifierBlock> FAULTY_DEHUMIDIFIER = ModBlocks.BLOCKS.registerBlock(
            "faulty_dehumidifier", FaultyDehumidifierBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());
}
