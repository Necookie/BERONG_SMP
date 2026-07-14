package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.common.hazard.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Hazard prop registrations for the school/classroom + kitchen zones (the original 55 props) —
 * split out of {@link ModBlocks} (see its class javadoc). Package-private: {@link ModBlocks}
 * re-exports every field below in the same declared order.
 */
final class ModBlocksHazardsSchoolKitchen {

    private ModBlocksHazardsSchoolKitchen() {}

    /** Plastic waste bin — emits smoke when has_vape=true (hazardous). */
    static final DeferredBlock<PlasticTrashBinBlock> PLASTIC_TRASH_BIN = ModBlocks.BLOCKS.registerBlock(
            "plastic_trash_bin", PlasticTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(0.5f, 1.0f).sound(SoundType.STONE).noOcclusion());

    /** Overloaded daisy-chain extension cord — electric sparks when overloaded=true. */
    static final DeferredBlock<DaisyChainExtensionBlock> DAISY_CHAIN_EXTENSION = ModBlocks.BLOCKS.registerBlock(
            "daisy_chain_extension", DaisyChainExtensionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f, 0.5f).sound(SoundType.STONE).noOcclusion());

    /** Floor sawdust accumulation layer — emits ash particles at accumulation >= 3. */
    static final DeferredBlock<WoodshopSawdustLayerBlock> WOODSHOP_SAWDUST_LAYER = ModBlocks.BLOCKS.registerBlock(
            "woodshop_sawdust_layer", WoodshopSawdustLayerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.1f, 0.1f).sound(SoundType.SAND).noOcclusion());

    /** Stage/theatre spotlight — overheating housing emits flame and smoke when hazardous. */
    static final DeferredBlock<StageSpotlightBlock> STAGE_SPOTLIGHT = ModBlocks.BLOCKS.registerBlock(
            "stage_spotlight", StageSpotlightBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Stack of flammable archive document boxes — fire proximity raises hazard. */
    static final DeferredBlock<ArchiveBoxStackBlock> ARCHIVE_BOX_STACK = ModBlocks.BLOCKS.registerBlock(
            "archive_box_stack", ArchiveBoxStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0f, 1.0f).sound(SoundType.WOOL).noOcclusion());

    /** Desktop PC tower with dust-clogged vents — overheats and emits smoke when hazardous. */
    static final DeferredBlock<DustChokedPcBlock> DUST_CHOKED_PC = ModBlocks.BLOCKS.registerBlock(
            "dust_choked_pc", DustChokedPcBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Rolling Chromebook/laptop charging cart — overloaded outlets spark when hazardous. */
    static final DeferredBlock<ChargingCartBlock> CHARGING_CART = ModBlocks.BLOCKS.registerBlock(
            "charging_cart", ChargingCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 5 : 0));

    /** Frayed AV/console wire on the floor — exposed copper arcs blue sparks when hazardous. */
    static final DeferredBlock<FrayedConsoleWireBlock> FRAYED_CONSOLE_WIRE = ModBlocks.BLOCKS.registerBlock(
            "frayed_console_wire", FrayedConsoleWireBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Vending machine with shorted compressor — smokes from back vents when hazardous. */
    static final DeferredBlock<MalfunctioningVendingBlock> MALFUNCTIONING_VENDING = ModBlocks.BLOCKS.registerBlock(
            "malfunctioning_vending", MalfunctioningVendingBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** Ceiling-mounted projector with failed cooling fan — overheats and smokes. */
    static final DeferredBlock<CeilingProjectorBlock> CEILING_PROJECTOR = ModBlocks.BLOCKS.registerBlock(
            "ceiling_projector", CeilingProjectorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 0));

    /** Swollen Li-ion phone battery left on desk — thermal runaway risk; cyan soul-flame gas when hazardous. */
    static final DeferredBlock<SwollenPhoneBatteryBlock> SWOLLEN_PHONE_BATTERY = ModBlocks.BLOCKS.registerBlock(
            "swollen_phone_battery", SwollenPhoneBatteryBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.METAL).noOcclusion());

    /** Damaged LiPo battery pack (drone/RC) — punctured cells off-gas smoke when hazardous. */
    static final DeferredBlock<DamagedLipoPackBlock> DAMAGED_LIPO_PACK = ModBlocks.BLOCKS.registerBlock(
            "damaged_lipo_pack", DamagedLipoPackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Iron locker with a vape device inside — sparks and smoke leak from vent slot when hazardous. */
    static final DeferredBlock<VapeInIronLockerBlock> VAPE_IN_IRON_LOCKER = ModBlocks.BLOCKS.registerBlock(
            "vape_in_iron_locker", VapeInIronLockerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** PA/public-address backup amp rack — faulty capacitors spark and glow when hazardous. */
    static final DeferredBlock<PaSystemBackupBlock> PA_SYSTEM_BACKUP = ModBlocks.BLOCKS.registerBlock(
            "pa_system_backup", PaSystemBackupBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Smartboard power inverter — roof leak drips on live electronics when hazardous. */
    static final DeferredBlock<SmartboardInverterBlock> SMARTBOARD_INVERTER = ModBlocks.BLOCKS.registerBlock(
            "smartboard_inverter", SmartboardInverterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Stove with a grease pan left unattended — grease fire erupts when hazardous. */
    static final DeferredBlock<UnattendedGreasePanBlock> UNATTENDED_GREASE_PAN = ModBlocks.BLOCKS.registerBlock(
            "unattended_grease_pan", UnattendedGreasePanBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Kitchen range hood with clogged grease filters — backflow smoke when hazardous. */
    static final DeferredBlock<GreaseCloggedHoodBlock> GREASE_CLOGGED_HOOD = ModBlocks.BLOCKS.registerBlock(
            "grease_clogged_hood", GreaseCloggedHoodBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Kitchen waste bin with oil-soaked contaminated rags — self-heating rags emit steam when hazardous. */
    static final DeferredBlock<ContaminatedKitchenBinBlock> CONTAMINATED_KITCHEN_BIN = ModBlocks.BLOCKS.registerBlock(
            "contaminated_kitchen_bin", ContaminatedKitchenBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Panini press with jammed lid and burning food — smoking when hazardous. */
    static final DeferredBlock<JammedPaniniPressBlock> JAMMED_PANINI_PRESS = ModBlocks.BLOCKS.registerBlock(
            "jammed_panini_press", JammedPaniniPressBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Commercial deep fryer — overheated oil ignites and erupts smoke/flame when hazardous. */
    static final DeferredBlock<CommercialDeepFryerBlock> COMMERCIAL_DEEP_FRYER = ModBlocks.BLOCKS.registerBlock(
            "commercial_deep_fryer", CommercialDeepFryerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 12 : 0));

    /** Overloaded Microwave (Electrical Hazard) — see {@link OverloadedMicrowaveBlock}. */
    static final DeferredBlock<OverloadedMicrowaveBlock> OVERLOADED_MICROWAVE = ModBlocks.BLOCKS.registerBlock(
            "overloaded_microwave", OverloadedMicrowaveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Bunsen Burner Station (Open Flame Hazard) — see {@link BunsenBurnerStationBlock}. */
    static final DeferredBlock<BunsenBurnerStationBlock> BUNSEN_BURNER_STATION = ModBlocks.BLOCKS.registerBlock(
            "bunsen_burner_station", BunsenBurnerStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 9 : 0));

    /** Reagent Storage Shelf (Chemical Hazard) — see {@link ReagentStorageShelfBlock}. */
    static final DeferredBlock<ReagentStorageShelfBlock> REAGENT_STORAGE_SHELF = ModBlocks.BLOCKS.registerBlock(
            "reagent_storage_shelf", ReagentStorageShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Overloaded Breaker Panel (Electrical Hazard) — see {@link OverloadedBreakerPanelBlock}. */
    static final DeferredBlock<OverloadedBreakerPanelBlock> OVERLOADED_BREAKER_PANEL = ModBlocks.BLOCKS.registerBlock(
            "overloaded_breaker_panel", OverloadedBreakerPanelBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Overheating Wall Aircon (Electrical Hazard) — see {@link OverheatingWallAirconBlock}. */
    static final DeferredBlock<OverheatingWallAirconBlock> OVERHEATING_WALL_AIRCON = ModBlocks.BLOCKS.registerBlock(
            "overheating_wall_aircon", OverheatingWallAirconBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Jammed Laser Printer (Overheat Hazard) — see {@link JammedLaserPrinterBlock}. */
    static final DeferredBlock<JammedLaserPrinterBlock> JAMMED_LASER_PRINTER = ModBlocks.BLOCKS.registerBlock(
            "jammed_laser_printer", JammedLaserPrinterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Unattended Shrine Candle (Open Flame Hazard) — see {@link UnattendedShrineCandleBlock}. */
    static final DeferredBlock<UnattendedShrineCandleBlock> UNATTENDED_SHRINE_CANDLE = ModBlocks.BLOCKS.registerBlock(
            "unattended_shrine_candle", UnattendedShrineCandleBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Leaking Gas Valve (Kitchen Gas Hazard) — see {@link LeakingGasValveBlock}. */
    static final DeferredBlock<LeakingGasValveBlock> LEAKING_GAS_VALVE = ModBlocks.BLOCKS.registerBlock(
            "leaking_gas_valve", LeakingGasValveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** Alcohol Dispenser Station (Flammable Liquid Hazard) — see {@link AlcoholDispenserStationBlock}. */
    static final DeferredBlock<AlcoholDispenserStationBlock> ALCOHOL_DISPENSER_STATION = ModBlocks.BLOCKS.registerBlock(
            "alcohol_dispenser_station", AlcoholDispenserStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 2 : 0));

    /** Clogged Exhaust Fan (Dust Overheat Hazard) — see {@link CloggedExhaustFanBlock}. */
    static final DeferredBlock<CloggedExhaustFanBlock> CLOGGED_EXHAUST_FAN = ModBlocks.BLOCKS.registerBlock(
            "clogged_exhaust_fan", CloggedExhaustFanBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Overloaded Wall Outlet — see {@link OverloadedWallOutletBlock}. */
    static final DeferredBlock<OverloadedWallOutletBlock> OVERLOADED_WALL_OUTLET = ModBlocks.BLOCKS.registerBlock(
            "overloaded_wall_outlet", OverloadedWallOutletBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Jammed Circuit Breaker Box — see {@link JammedCircuitBreakerBlock}. */
    static final DeferredBlock<JammedCircuitBreakerBlock> JAMMED_CIRCUIT_BREAKER = ModBlocks.BLOCKS.registerBlock(
            "jammed_circuit_breaker", JammedCircuitBreakerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 5 : 0));

    /** Unsealed Solvent Shelf — see {@link UnsealedSolventShelfBlock}. */
    static final DeferredBlock<UnsealedSolventShelfBlock> UNSEALED_SOLVENT_SHELF = ModBlocks.BLOCKS.registerBlock(
            "unsealed_solvent_shelf", UnsealedSolventShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Unattended Welding Station — see {@link UnattendedWeldingStationBlock}. */
    static final DeferredBlock<UnattendedWeldingStationBlock> UNATTENDED_WELDING_STATION = ModBlocks.BLOCKS.registerBlock(
            "unattended_welding_station", UnattendedWeldingStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Leaking Butane Canister Stove — see {@link LeakingButaneCanisterStoveBlock}. */
    static final DeferredBlock<LeakingButaneCanisterStoveBlock> LEAKING_BUTANE_CANISTER_STOVE = ModBlocks.BLOCKS.registerBlock(
            "leaking_butane_canister_stove", LeakingButaneCanisterStoveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Chef's Prep Table — see {@link ChefsPrepDrawersBlock}. */
    static final DeferredBlock<ChefsPrepDrawersBlock> CHEFS_PREP_DRAWERS = ModBlocks.BLOCKS.registerBlock(
            "chefs_prep_drawers", ChefsPrepDrawersBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Culinary Lab Refrigerator — see {@link CulinaryFridgeBlock}. */
    static final DeferredBlock<CulinaryFridgeBlock> CULINARY_FRIDGE = ModBlocks.BLOCKS.registerBlock(
            "culinary_fridge", CulinaryFridgeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Student Lab Microwave — see {@link StudentLabMicrowaveBlock}. */
    static final DeferredBlock<StudentLabMicrowaveBlock> STUDENT_LAB_MICROWAVE = ModBlocks.BLOCKS.registerBlock(
            "student_lab_microwave", StudentLabMicrowaveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Corroded Gas Line Joint — data-driven via {@link HazardSpecs#CORRODED_GAS_LINE_JOINT} (no dedicated class, see {@link SimpleHazardFacingBlock}). */
    static final DeferredBlock<SimpleHazardFacingBlock> CORRODED_GAS_LINE_JOINT = ModBlocks.BLOCKS.registerBlock(
            "corroded_gas_line_joint", p -> new SimpleHazardFacingBlock(p, HazardSpecs.CORRODED_GAS_LINE_JOINT),
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Gas Range, Stuck Burner — see {@link GasRangeStuckBurnerBlock}. */
    static final DeferredBlock<GasRangeStuckBurnerBlock> GAS_RANGE_STUCK_BURNER = ModBlocks.BLOCKS.registerBlock(
            "gas_range_stuck_burner", GasRangeStuckBurnerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 11 : 0));

    /** Commercial Stand Mixer — see {@link CommercialStandMixerBlock}. */
    static final DeferredBlock<CommercialStandMixerBlock> COMMERCIAL_STAND_MIXER = ModBlocks.BLOCKS.registerBlock(
            "commercial_stand_mixer", CommercialStandMixerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Gas Deck Oven — see {@link GasDeckOvenBlock}. */
    static final DeferredBlock<GasDeckOvenBlock> GAS_DECK_OVEN = ModBlocks.BLOCKS.registerBlock(
            "gas_deck_oven", GasDeckOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Induction Cooktop — see {@link InductionCooktopStationBlock}. */
    static final DeferredBlock<InductionCooktopStationBlock> INDUCTION_COOKTOP_STATION = ModBlocks.BLOCKS.registerBlock(
            "induction_cooktop_station", InductionCooktopStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Rice Cooker Bank — see {@link RiceCookerBankBlock}. */
    static final DeferredBlock<RiceCookerBankBlock> RICE_COOKER_BANK = ModBlocks.BLOCKS.registerBlock(
            "rice_cooker_bank", RiceCookerBankBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Espresso Machine — see {@link EspressoMachineBlock}. */
    static final DeferredBlock<EspressoMachineBlock> ESPRESSO_MACHINE = ModBlocks.BLOCKS.registerBlock(
            "espresso_machine", EspressoMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Hot Water Urn — see {@link HotWaterUrnBlock}. */
    static final DeferredBlock<HotWaterUrnBlock> HOT_WATER_URN = ModBlocks.BLOCKS.registerBlock(
            "hot_water_urn", HotWaterUrnBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Countertop Toaster Oven — see {@link ToasterOvenCrumbBlock}. */
    static final DeferredBlock<ToasterOvenCrumbBlock> TOASTER_OVEN_CRUMB = ModBlocks.BLOCKS.registerBlock(
            "toaster_oven_crumb", ToasterOvenCrumbBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 9 : 0));

    /** Dry Goods Pantry Shelf — see {@link DryGoodsPantryShelfBlock}. */
    static final DeferredBlock<DryGoodsPantryShelfBlock> DRY_GOODS_PANTRY_SHELF = ModBlocks.BLOCKS.registerBlock(
            "dry_goods_pantry_shelf", DryGoodsPantryShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Kitchen Exhaust Duct — see {@link GreaseDuctRunBlock}. */
    static final DeferredBlock<GreaseDuctRunBlock> GREASE_DUCT_RUN = ModBlocks.BLOCKS.registerBlock(
            "grease_duct_run", GreaseDuctRunBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Dish Sanitizer — see {@link CommercialDishSanitizerBlock}. */
    static final DeferredBlock<CommercialDishSanitizerBlock> COMMERCIAL_DISH_SANITIZER = ModBlocks.BLOCKS.registerBlock(
            "commercial_dish_sanitizer", CommercialDishSanitizerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Garbage Disposal Unit — see {@link GarbageDisposalUnitBlock}. */
    static final DeferredBlock<GarbageDisposalUnitBlock> GARBAGE_DISPOSAL_UNIT = ModBlocks.BLOCKS.registerBlock(
            "garbage_disposal_unit", GarbageDisposalUnitBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** UV Knife Sterilizer — see {@link KnifeSterilizerCabinetBlock}. */
    static final DeferredBlock<KnifeSterilizerCabinetBlock> KNIFE_STERILIZER_CABINET = ModBlocks.BLOCKS.registerBlock(
            "knife_sterilizer_cabinet", KnifeSterilizerCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Chafing Steam Table — see {@link SternoSteamTableBlock}. */
    static final DeferredBlock<SternoSteamTableBlock> STERNO_STEAM_TABLE = ModBlocks.BLOCKS.registerBlock(
            "sterno_steam_table", SternoSteamTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Electric Convection Oven — see {@link ConvectionOvenBlock}. */
    static final DeferredBlock<ConvectionOvenBlock> CONVECTION_OVEN = ModBlocks.BLOCKS.registerBlock(
            "convection_oven", ConvectionOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 0));

    /** Lechon Rotisserie — see {@link LechonRotisserieSpitBlock}. */
    static final DeferredBlock<LechonRotisserieSpitBlock> LECHON_ROTISSERIE_SPIT = ModBlocks.BLOCKS.registerBlock(
            "lechon_rotisserie_spit", LechonRotisserieSpitBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 12 : 0));
}
