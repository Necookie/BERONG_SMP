package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.block.BadmintonNetMeshBlock;
import net.necookie.disastersim.block.BadmintonNetPostBlock;
import net.necookie.disastersim.block.BasketballHoopBlock;
import net.necookie.disastersim.block.BasketballHoopPostBlock;
import net.necookie.disastersim.block.BasketballPoleSegmentBlock;
import net.necookie.disastersim.block.BulletinBoardBlock;
import net.necookie.disastersim.block.ConferenceTableBlock;
import net.necookie.disastersim.block.ExecutiveOfficeChairBlock;
import net.necookie.disastersim.block.ConferenceCredenzaBlock;
import net.necookie.disastersim.block.ConferenceWallDisplayBlock;
import net.necookie.disastersim.block.FlipChartEaselBlock;
import net.necookie.disastersim.block.ConferenceSpeakerphoneBlock;
import net.necookie.disastersim.block.GlassOfficePartitionBlock;
import net.necookie.disastersim.block.LoungeSofaBlock;
import net.necookie.disastersim.block.PottedOfficePlantBlock;
import net.necookie.disastersim.block.WindowBlindsBlock;
import net.necookie.disastersim.block.OfficeCubiclePartitionBlock;
import net.necookie.disastersim.block.ReceptionDeskBlock;
import net.necookie.disastersim.block.MailSortingShelfBlock;
import net.necookie.disastersim.block.OfficePhotocopierBlock;
import net.necookie.disastersim.block.DocumentTrayStackBlock;
import net.necookie.disastersim.block.WallBinderShelfBlock;
import net.necookie.disastersim.block.OfficeSafeBlock;
import net.necookie.disastersim.block.CoatRackStandBlock;
import net.necookie.disastersim.block.BundyTimeClockBlock;
import net.necookie.disastersim.block.OfficeSupplyCabinetBlock;
import net.necookie.disastersim.block.LaboratoryFumeHoodBlock;
import net.necookie.disastersim.block.EquipmentRackBlock;
import net.necookie.disastersim.block.LabStoolBlock;
import net.necookie.disastersim.block.OscilloscopeCartBlock;
import net.necookie.disastersim.block.MicroscopeStationBlock;
import net.necookie.disastersim.block.EyeWashStationBlock;
import net.necookie.disastersim.block.ComponentDrawerCabinetBlock;
import net.necookie.disastersim.block.SecuredCylinderRackBlock;
import net.necookie.disastersim.block.SampleStorageRackBlock;
import net.necookie.disastersim.block.BalanceScaleTableBlock;
import net.necookie.disastersim.block.CafeteriaMenuBoardBlock;
import net.necookie.disastersim.block.CourtLineBlock;
import net.necookie.disastersim.block.CafeteriaStoolBlock;
import net.necookie.disastersim.block.CafeteriaTableBlock;
import net.necookie.disastersim.block.CafeteriaTrashBinBlock;
import net.necookie.disastersim.block.CondimentStationBlock;
import net.necookie.disastersim.block.SaladBarBlock;
import net.necookie.disastersim.block.ServingCounterBlock;
import net.necookie.disastersim.block.SnackVendingMachineBlock;
import net.necookie.disastersim.block.SodaFountainMachineBlock;
import net.necookie.disastersim.block.TrayStackBlock;
import net.necookie.disastersim.block.EvacuationMapBlock;
import net.necookie.disastersim.block.EmergencyLightBlock;
import net.necookie.disastersim.block.SprinklerHeadBlock;
import net.necookie.disastersim.block.SmokeDetectorBlock;
import net.necookie.disastersim.block.ExitSignBlock;
import net.necookie.disastersim.block.ClassroomGlobeBlock;
import net.necookie.disastersim.block.PodiumLecternBlock;
import net.necookie.disastersim.block.BlackboardBlock;
import net.necookie.disastersim.block.WallClockBlock;
import net.necookie.disastersim.block.WaterDispenserBlock;
import net.necookie.disastersim.block.TrophyCabinetBlock;
import net.necookie.disastersim.block.PhilippineFlagStandBlock;
import net.necookie.disastersim.block.TallBookshelfBlock;
import net.necookie.disastersim.block.ArmchairDeskBlock;
import net.necookie.disastersim.block.TeachersDeskBlock;
import net.necookie.disastersim.block.CeilingFanBlock;
import net.necookie.disastersim.block.ChairBlock;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.block.ComputerTableBlock;
import net.necookie.disastersim.block.DrawersBlock;
import net.necookie.disastersim.block.FilingCabinetBlock;
import net.necookie.disastersim.block.FireAlarmBlock;
import net.necookie.disastersim.block.FireHoseCabinetBlock;
import net.necookie.disastersim.block.GlowingOakPlanksBlock;
import net.necookie.disastersim.block.LightBulbBlock;
import net.necookie.disastersim.block.LockerBlock;
import net.necookie.disastersim.block.ModernStudentDeskBlock;
import net.necookie.disastersim.block.ScienceLabWorkbenchBlock;
import net.necookie.disastersim.block.ComputerLabDeskRowBlock;
import net.necookie.disastersim.block.LibraryStudyCarrelBlock;
import net.necookie.disastersim.block.RollingBookCartBlock;
import net.necookie.disastersim.block.KitchenPrepCounterBlock;
import net.necookie.disastersim.block.DishwashingSinkStationBlock;
import net.necookie.disastersim.block.BeverageJuiceDispenserBlock;
import net.necookie.disastersim.block.CutleryNapkinCaddyBlock;
import net.necookie.disastersim.block.ServingHatchWindowBlock;
import net.necookie.disastersim.block.FireExtinguisherCabinetBlock;
import net.necookie.disastersim.block.AssemblyPointSignBlock;
import net.necookie.disastersim.block.FirstAidWallCabinetBlock;
import net.necookie.disastersim.block.FireSafetyPosterBlock;
import net.necookie.disastersim.block.BlockedExitClutterBlock;
import net.necookie.disastersim.block.SinkBlock;
import net.necookie.disastersim.block.TableBlock;
import net.necookie.disastersim.block.ToiletBlock;
import net.necookie.disastersim.block.TrashCanBlock;
import net.necookie.disastersim.block.WhiteboardBlock;
import net.necookie.disastersim.common.hazard.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All 179 block registrations for the mod, as a lean index over 9 package-private domain
 * registrar classes ({@code ModBlocksFurnitureClassroom}, {@code ModBlocksHazardsSchoolKitchen},
 * {@code ModBlocksSchoolDecor}, {@code ModBlocksSafetyEquipment}, {@code ModBlocksCafeteria},
 * {@code ModBlocksSportsCourts}, {@code ModBlocksConference}, {@code ModBlocksOffice},
 * {@code ModBlocksLaboratory}) — only the computer/fire-alarm/example blocks register directly
 * here. Every sub-registrar field is re-exported below in its original declared order, so no call
 * site elsewhere in the codebase needed to change when this file was split apart (2026-07-14).
 * Extracted from {@code BerongSMP} so the entry point stays a thin bootstrap;
 * {@link #register(IEventBus)} is called from its constructor.
 *
 * <p>Block items live with the other item registrations (which reference these fields); the split
 * per registry type matches the standard NeoForge Mod* convention.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BerongSMP.MODID);

    /** Example block registration. */
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));

    /** Computer/terminal block — can be set LIT=true for Class C electrical fire scenarios. */
    public static final DeferredBlock<ComputerBlock> COMPUTER = BLOCKS.registerBlock("computer",
            ComputerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> {
                        if (state.getValue(ComputerBlock.BURNING)) return 15;
                        if (state.getValue(BlockStateProperties.LIT)) return 7;
                        return 0;
                    }));

    /** Wall-mounted fire alarm pull station — activates during FIRE simulations, logs fire_alarm_activate telemetry. */
    public static final DeferredBlock<FireAlarmBlock> FIRE_ALARM_BLOCK = BLOCKS.registerBlock(
            "fire_alarm",
            FireAlarmBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(FireAlarmBlock.ACTIVATED) ? 7 : 0));

    // ── Classroom/office furniture — see ModBlocksFurnitureClassroom ────────
    public static final DeferredBlock<WhiteboardBlock> WHITEBOARD = ModBlocksFurnitureClassroom.WHITEBOARD;
    public static final DeferredBlock<FireHoseCabinetBlock> FIRE_HOSE_CABINET = ModBlocksFurnitureClassroom.FIRE_HOSE_CABINET;
    public static final DeferredBlock<ToiletBlock> TOILET = ModBlocksFurnitureClassroom.TOILET;
    public static final DeferredBlock<SinkBlock> SINK = ModBlocksFurnitureClassroom.SINK;
    public static final DeferredBlock<DrawersBlock> DRAWERS = ModBlocksFurnitureClassroom.DRAWERS;
    public static final DeferredBlock<ComputerTableBlock> COMPUTER_TABLE = ModBlocksFurnitureClassroom.COMPUTER_TABLE;
    public static final DeferredBlock<TableBlock> TABLE = ModBlocksFurnitureClassroom.TABLE;
    public static final DeferredBlock<ChairBlock> CHAIR = ModBlocksFurnitureClassroom.CHAIR;
    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = ModBlocksFurnitureClassroom.FILING_CABINET;
    public static final DeferredBlock<LockerBlock> LOCKER = ModBlocksFurnitureClassroom.LOCKER;
    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = ModBlocksFurnitureClassroom.TRASH_CAN;
    public static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = ModBlocksFurnitureClassroom.BULLETIN_BOARD;
    public static final DeferredBlock<CeilingFanBlock> CEILING_FAN = ModBlocksFurnitureClassroom.CEILING_FAN;
    public static final DeferredBlock<LightBulbBlock> LIGHT_BULB = ModBlocksFurnitureClassroom.LIGHT_BULB;

    // ── School/kitchen hazard props (55) — see ModBlocksHazardsSchoolKitchen ─
    public static final DeferredBlock<PlasticTrashBinBlock> PLASTIC_TRASH_BIN = ModBlocksHazardsSchoolKitchen.PLASTIC_TRASH_BIN;
    public static final DeferredBlock<DaisyChainExtensionBlock> DAISY_CHAIN_EXTENSION = ModBlocksHazardsSchoolKitchen.DAISY_CHAIN_EXTENSION;
    public static final DeferredBlock<WoodshopSawdustLayerBlock> WOODSHOP_SAWDUST_LAYER = ModBlocksHazardsSchoolKitchen.WOODSHOP_SAWDUST_LAYER;
    public static final DeferredBlock<StageSpotlightBlock> STAGE_SPOTLIGHT = ModBlocksHazardsSchoolKitchen.STAGE_SPOTLIGHT;
    public static final DeferredBlock<ArchiveBoxStackBlock> ARCHIVE_BOX_STACK = ModBlocksHazardsSchoolKitchen.ARCHIVE_BOX_STACK;
    public static final DeferredBlock<DustChokedPcBlock> DUST_CHOKED_PC = ModBlocksHazardsSchoolKitchen.DUST_CHOKED_PC;
    public static final DeferredBlock<ChargingCartBlock> CHARGING_CART = ModBlocksHazardsSchoolKitchen.CHARGING_CART;
    public static final DeferredBlock<FrayedConsoleWireBlock> FRAYED_CONSOLE_WIRE = ModBlocksHazardsSchoolKitchen.FRAYED_CONSOLE_WIRE;
    public static final DeferredBlock<MalfunctioningVendingBlock> MALFUNCTIONING_VENDING = ModBlocksHazardsSchoolKitchen.MALFUNCTIONING_VENDING;
    public static final DeferredBlock<CeilingProjectorBlock> CEILING_PROJECTOR = ModBlocksHazardsSchoolKitchen.CEILING_PROJECTOR;
    public static final DeferredBlock<SwollenPhoneBatteryBlock> SWOLLEN_PHONE_BATTERY = ModBlocksHazardsSchoolKitchen.SWOLLEN_PHONE_BATTERY;
    public static final DeferredBlock<DamagedLipoPackBlock> DAMAGED_LIPO_PACK = ModBlocksHazardsSchoolKitchen.DAMAGED_LIPO_PACK;
    public static final DeferredBlock<VapeInIronLockerBlock> VAPE_IN_IRON_LOCKER = ModBlocksHazardsSchoolKitchen.VAPE_IN_IRON_LOCKER;
    public static final DeferredBlock<PaSystemBackupBlock> PA_SYSTEM_BACKUP = ModBlocksHazardsSchoolKitchen.PA_SYSTEM_BACKUP;
    public static final DeferredBlock<SmartboardInverterBlock> SMARTBOARD_INVERTER = ModBlocksHazardsSchoolKitchen.SMARTBOARD_INVERTER;
    public static final DeferredBlock<UnattendedGreasePanBlock> UNATTENDED_GREASE_PAN = ModBlocksHazardsSchoolKitchen.UNATTENDED_GREASE_PAN;
    public static final DeferredBlock<GreaseCloggedHoodBlock> GREASE_CLOGGED_HOOD = ModBlocksHazardsSchoolKitchen.GREASE_CLOGGED_HOOD;
    public static final DeferredBlock<ContaminatedKitchenBinBlock> CONTAMINATED_KITCHEN_BIN = ModBlocksHazardsSchoolKitchen.CONTAMINATED_KITCHEN_BIN;
    public static final DeferredBlock<JammedPaniniPressBlock> JAMMED_PANINI_PRESS = ModBlocksHazardsSchoolKitchen.JAMMED_PANINI_PRESS;
    public static final DeferredBlock<CommercialDeepFryerBlock> COMMERCIAL_DEEP_FRYER = ModBlocksHazardsSchoolKitchen.COMMERCIAL_DEEP_FRYER;
    public static final DeferredBlock<OverloadedMicrowaveBlock> OVERLOADED_MICROWAVE = ModBlocksHazardsSchoolKitchen.OVERLOADED_MICROWAVE;
    public static final DeferredBlock<BunsenBurnerStationBlock> BUNSEN_BURNER_STATION = ModBlocksHazardsSchoolKitchen.BUNSEN_BURNER_STATION;
    public static final DeferredBlock<ReagentStorageShelfBlock> REAGENT_STORAGE_SHELF = ModBlocksHazardsSchoolKitchen.REAGENT_STORAGE_SHELF;
    public static final DeferredBlock<OverloadedBreakerPanelBlock> OVERLOADED_BREAKER_PANEL = ModBlocksHazardsSchoolKitchen.OVERLOADED_BREAKER_PANEL;
    public static final DeferredBlock<OverheatingWallAirconBlock> OVERHEATING_WALL_AIRCON = ModBlocksHazardsSchoolKitchen.OVERHEATING_WALL_AIRCON;
    public static final DeferredBlock<JammedLaserPrinterBlock> JAMMED_LASER_PRINTER = ModBlocksHazardsSchoolKitchen.JAMMED_LASER_PRINTER;
    public static final DeferredBlock<UnattendedShrineCandleBlock> UNATTENDED_SHRINE_CANDLE = ModBlocksHazardsSchoolKitchen.UNATTENDED_SHRINE_CANDLE;
    public static final DeferredBlock<LeakingGasValveBlock> LEAKING_GAS_VALVE = ModBlocksHazardsSchoolKitchen.LEAKING_GAS_VALVE;
    public static final DeferredBlock<AlcoholDispenserStationBlock> ALCOHOL_DISPENSER_STATION = ModBlocksHazardsSchoolKitchen.ALCOHOL_DISPENSER_STATION;
    public static final DeferredBlock<CloggedExhaustFanBlock> CLOGGED_EXHAUST_FAN = ModBlocksHazardsSchoolKitchen.CLOGGED_EXHAUST_FAN;
    public static final DeferredBlock<OverloadedWallOutletBlock> OVERLOADED_WALL_OUTLET = ModBlocksHazardsSchoolKitchen.OVERLOADED_WALL_OUTLET;
    public static final DeferredBlock<JammedCircuitBreakerBlock> JAMMED_CIRCUIT_BREAKER = ModBlocksHazardsSchoolKitchen.JAMMED_CIRCUIT_BREAKER;
    public static final DeferredBlock<UnsealedSolventShelfBlock> UNSEALED_SOLVENT_SHELF = ModBlocksHazardsSchoolKitchen.UNSEALED_SOLVENT_SHELF;
    public static final DeferredBlock<UnattendedWeldingStationBlock> UNATTENDED_WELDING_STATION = ModBlocksHazardsSchoolKitchen.UNATTENDED_WELDING_STATION;
    public static final DeferredBlock<LeakingButaneCanisterStoveBlock> LEAKING_BUTANE_CANISTER_STOVE = ModBlocksHazardsSchoolKitchen.LEAKING_BUTANE_CANISTER_STOVE;
    public static final DeferredBlock<ChefsPrepDrawersBlock> CHEFS_PREP_DRAWERS = ModBlocksHazardsSchoolKitchen.CHEFS_PREP_DRAWERS;
    public static final DeferredBlock<CulinaryFridgeBlock> CULINARY_FRIDGE = ModBlocksHazardsSchoolKitchen.CULINARY_FRIDGE;
    public static final DeferredBlock<StudentLabMicrowaveBlock> STUDENT_LAB_MICROWAVE = ModBlocksHazardsSchoolKitchen.STUDENT_LAB_MICROWAVE;
    public static final DeferredBlock<CorrodedGasLineJointBlock> CORRODED_GAS_LINE_JOINT = ModBlocksHazardsSchoolKitchen.CORRODED_GAS_LINE_JOINT;
    public static final DeferredBlock<GasRangeStuckBurnerBlock> GAS_RANGE_STUCK_BURNER = ModBlocksHazardsSchoolKitchen.GAS_RANGE_STUCK_BURNER;
    public static final DeferredBlock<CommercialStandMixerBlock> COMMERCIAL_STAND_MIXER = ModBlocksHazardsSchoolKitchen.COMMERCIAL_STAND_MIXER;
    public static final DeferredBlock<GasDeckOvenBlock> GAS_DECK_OVEN = ModBlocksHazardsSchoolKitchen.GAS_DECK_OVEN;
    public static final DeferredBlock<InductionCooktopStationBlock> INDUCTION_COOKTOP_STATION = ModBlocksHazardsSchoolKitchen.INDUCTION_COOKTOP_STATION;
    public static final DeferredBlock<RiceCookerBankBlock> RICE_COOKER_BANK = ModBlocksHazardsSchoolKitchen.RICE_COOKER_BANK;
    public static final DeferredBlock<EspressoMachineBlock> ESPRESSO_MACHINE = ModBlocksHazardsSchoolKitchen.ESPRESSO_MACHINE;
    public static final DeferredBlock<HotWaterUrnBlock> HOT_WATER_URN = ModBlocksHazardsSchoolKitchen.HOT_WATER_URN;
    public static final DeferredBlock<ToasterOvenCrumbBlock> TOASTER_OVEN_CRUMB = ModBlocksHazardsSchoolKitchen.TOASTER_OVEN_CRUMB;
    public static final DeferredBlock<DryGoodsPantryShelfBlock> DRY_GOODS_PANTRY_SHELF = ModBlocksHazardsSchoolKitchen.DRY_GOODS_PANTRY_SHELF;
    public static final DeferredBlock<GreaseDuctRunBlock> GREASE_DUCT_RUN = ModBlocksHazardsSchoolKitchen.GREASE_DUCT_RUN;
    public static final DeferredBlock<CommercialDishSanitizerBlock> COMMERCIAL_DISH_SANITIZER = ModBlocksHazardsSchoolKitchen.COMMERCIAL_DISH_SANITIZER;
    public static final DeferredBlock<GarbageDisposalUnitBlock> GARBAGE_DISPOSAL_UNIT = ModBlocksHazardsSchoolKitchen.GARBAGE_DISPOSAL_UNIT;
    public static final DeferredBlock<KnifeSterilizerCabinetBlock> KNIFE_STERILIZER_CABINET = ModBlocksHazardsSchoolKitchen.KNIFE_STERILIZER_CABINET;
    public static final DeferredBlock<SternoSteamTableBlock> STERNO_STEAM_TABLE = ModBlocksHazardsSchoolKitchen.STERNO_STEAM_TABLE;
    public static final DeferredBlock<ConvectionOvenBlock> CONVECTION_OVEN = ModBlocksHazardsSchoolKitchen.CONVECTION_OVEN;
    public static final DeferredBlock<LechonRotisserieSpitBlock> LECHON_ROTISSERIE_SPIT = ModBlocksHazardsSchoolKitchen.LECHON_ROTISSERIE_SPIT;

    // ── School decor/furniture — see ModBlocksSchoolDecor ───────────────────
    public static final DeferredBlock<TeachersDeskBlock> TEACHERS_DESK = ModBlocksSchoolDecor.TEACHERS_DESK;
    public static final DeferredBlock<ArmchairDeskBlock> ARMCHAIR_DESK = ModBlocksSchoolDecor.ARMCHAIR_DESK;
    public static final DeferredBlock<TallBookshelfBlock> TALL_BOOKSHELF = ModBlocksSchoolDecor.TALL_BOOKSHELF;
    public static final DeferredBlock<PhilippineFlagStandBlock> PHILIPPINE_FLAG_STAND = ModBlocksSchoolDecor.PHILIPPINE_FLAG_STAND;
    public static final DeferredBlock<TrophyCabinetBlock> TROPHY_CABINET = ModBlocksSchoolDecor.TROPHY_CABINET;
    public static final DeferredBlock<WaterDispenserBlock> WATER_DISPENSER = ModBlocksSchoolDecor.WATER_DISPENSER;
    public static final DeferredBlock<WallClockBlock> WALL_CLOCK = ModBlocksSchoolDecor.WALL_CLOCK;
    public static final DeferredBlock<BlackboardBlock> BLACKBOARD = ModBlocksSchoolDecor.BLACKBOARD;
    public static final DeferredBlock<PodiumLecternBlock> PODIUM_LECTERN = ModBlocksSchoolDecor.PODIUM_LECTERN;
    public static final DeferredBlock<ClassroomGlobeBlock> CLASSROOM_GLOBE = ModBlocksSchoolDecor.CLASSROOM_GLOBE;
    public static final DeferredBlock<ModernStudentDeskBlock> MODERN_STUDENT_DESK = ModBlocksSchoolDecor.MODERN_STUDENT_DESK;
    public static final DeferredBlock<ScienceLabWorkbenchBlock> SCIENCE_LAB_WORKBENCH = ModBlocksSchoolDecor.SCIENCE_LAB_WORKBENCH;
    public static final DeferredBlock<ComputerLabDeskRowBlock> COMPUTER_LAB_DESK_ROW = ModBlocksSchoolDecor.COMPUTER_LAB_DESK_ROW;
    public static final DeferredBlock<LibraryStudyCarrelBlock> LIBRARY_STUDY_CARREL = ModBlocksSchoolDecor.LIBRARY_STUDY_CARREL;
    public static final DeferredBlock<RollingBookCartBlock> ROLLING_BOOK_CART = ModBlocksSchoolDecor.ROLLING_BOOK_CART;

    // ── Safety equipment — see ModBlocksSafetyEquipment ─────────────────────
    public static final DeferredBlock<ExitSignBlock> EXIT_SIGN = ModBlocksSafetyEquipment.EXIT_SIGN;
    public static final DeferredBlock<SmokeDetectorBlock> SMOKE_DETECTOR = ModBlocksSafetyEquipment.SMOKE_DETECTOR;
    public static final DeferredBlock<SprinklerHeadBlock> SPRINKLER_HEAD = ModBlocksSafetyEquipment.SPRINKLER_HEAD;
    public static final DeferredBlock<EmergencyLightBlock> EMERGENCY_LIGHT = ModBlocksSafetyEquipment.EMERGENCY_LIGHT;
    public static final DeferredBlock<EvacuationMapBlock> EVACUATION_MAP = ModBlocksSafetyEquipment.EVACUATION_MAP;
    public static final DeferredBlock<FireExtinguisherCabinetBlock> FIRE_EXTINGUISHER_CABINET = ModBlocksSafetyEquipment.FIRE_EXTINGUISHER_CABINET;
    public static final DeferredBlock<AssemblyPointSignBlock> ASSEMBLY_POINT_SIGN = ModBlocksSafetyEquipment.ASSEMBLY_POINT_SIGN;
    public static final DeferredBlock<FirstAidWallCabinetBlock> FIRST_AID_WALL_CABINET = ModBlocksSafetyEquipment.FIRST_AID_WALL_CABINET;
    public static final DeferredBlock<FireSafetyPosterBlock> FIRE_SAFETY_POSTER = ModBlocksSafetyEquipment.FIRE_SAFETY_POSTER;
    public static final DeferredBlock<BlockedExitClutterBlock> BLOCKED_EXIT_CLUTTER = ModBlocksSafetyEquipment.BLOCKED_EXIT_CLUTTER;

    // ── Cafeteria — see ModBlocksCafeteria ───────────────────────────────────
    public static final DeferredBlock<CafeteriaTableBlock> CAFETERIA_TABLE = ModBlocksCafeteria.CAFETERIA_TABLE;
    public static final DeferredBlock<TrayStackBlock> TRAY_STACK = ModBlocksCafeteria.TRAY_STACK;
    public static final DeferredBlock<ServingCounterBlock> SERVING_COUNTER = ModBlocksCafeteria.SERVING_COUNTER;
    public static final DeferredBlock<CafeteriaMenuBoardBlock> CAFETERIA_MENU_BOARD = ModBlocksCafeteria.CAFETERIA_MENU_BOARD;
    public static final DeferredBlock<CondimentStationBlock> CONDIMENT_STATION = ModBlocksCafeteria.CONDIMENT_STATION;
    public static final DeferredBlock<CafeteriaTrashBinBlock> CAFETERIA_TRASH_BIN = ModBlocksCafeteria.CAFETERIA_TRASH_BIN;
    public static final DeferredBlock<SodaFountainMachineBlock> SODA_FOUNTAIN_MACHINE = ModBlocksCafeteria.SODA_FOUNTAIN_MACHINE;
    public static final DeferredBlock<CafeteriaStoolBlock> CAFETERIA_STOOL = ModBlocksCafeteria.CAFETERIA_STOOL;
    public static final DeferredBlock<SaladBarBlock> SALAD_BAR = ModBlocksCafeteria.SALAD_BAR;
    public static final DeferredBlock<SnackVendingMachineBlock> SNACK_VENDING_MACHINE = ModBlocksCafeteria.SNACK_VENDING_MACHINE;
    public static final DeferredBlock<KitchenPrepCounterBlock> KITCHEN_PREP_COUNTER = ModBlocksCafeteria.KITCHEN_PREP_COUNTER;
    public static final DeferredBlock<DishwashingSinkStationBlock> DISHWASHING_SINK_STATION = ModBlocksCafeteria.DISHWASHING_SINK_STATION;
    public static final DeferredBlock<BeverageJuiceDispenserBlock> BEVERAGE_JUICE_DISPENSER = ModBlocksCafeteria.BEVERAGE_JUICE_DISPENSER;
    public static final DeferredBlock<CutleryNapkinCaddyBlock> CUTLERY_NAPKIN_CADDY = ModBlocksCafeteria.CUTLERY_NAPKIN_CADDY;
    public static final DeferredBlock<ServingHatchWindowBlock> SERVING_HATCH_WINDOW = ModBlocksCafeteria.SERVING_HATCH_WINDOW;
    public static final DeferredBlock<GlowingOakPlanksBlock> GLOWING_OAK_PLANKS = ModBlocksCafeteria.GLOWING_OAK_PLANKS;

    // ── Sports courts — see ModBlocksSportsCourts ────────────────────────────
    public static final DeferredBlock<CourtLineBlock> COURT_LINE = ModBlocksSportsCourts.COURT_LINE;
    public static final DeferredBlock<BadmintonNetPostBlock> BADMINTON_NET_POST = ModBlocksSportsCourts.BADMINTON_NET_POST;
    public static final DeferredBlock<BadmintonNetMeshBlock> BADMINTON_NET_MESH = ModBlocksSportsCourts.BADMINTON_NET_MESH;
    public static final DeferredBlock<BasketballHoopPostBlock> BASKETBALL_HOOP_POST = ModBlocksSportsCourts.BASKETBALL_HOOP_POST;
    public static final DeferredBlock<BasketballPoleSegmentBlock> BASKETBALL_POLE = ModBlocksSportsCourts.BASKETBALL_POLE;
    public static final DeferredBlock<BasketballHoopBlock> BASKETBALL_HOOP = ModBlocksSportsCourts.BASKETBALL_HOOP;

    // ── Conference Room furniture + hazards (56-65) — see ModBlocksConference ─
    public static final DeferredBlock<ConferenceTableBlock> CONFERENCE_TABLE = ModBlocksConference.CONFERENCE_TABLE;
    public static final DeferredBlock<ExecutiveOfficeChairBlock> EXECUTIVE_OFFICE_CHAIR = ModBlocksConference.EXECUTIVE_OFFICE_CHAIR;
    public static final DeferredBlock<ConferenceCredenzaBlock> CONFERENCE_CREDENZA = ModBlocksConference.CONFERENCE_CREDENZA;
    public static final DeferredBlock<ConferenceWallDisplayBlock> CONFERENCE_WALL_DISPLAY = ModBlocksConference.CONFERENCE_WALL_DISPLAY;
    public static final DeferredBlock<FlipChartEaselBlock> FLIP_CHART_EASEL = ModBlocksConference.FLIP_CHART_EASEL;
    public static final DeferredBlock<ConferenceSpeakerphoneBlock> CONFERENCE_SPEAKERPHONE = ModBlocksConference.CONFERENCE_SPEAKERPHONE;
    public static final DeferredBlock<GlassOfficePartitionBlock> GLASS_OFFICE_PARTITION = ModBlocksConference.GLASS_OFFICE_PARTITION;
    public static final DeferredBlock<LoungeSofaBlock> LOUNGE_SOFA = ModBlocksConference.LOUNGE_SOFA;
    public static final DeferredBlock<PottedOfficePlantBlock> POTTED_OFFICE_PLANT = ModBlocksConference.POTTED_OFFICE_PLANT;
    public static final DeferredBlock<WindowBlindsBlock> WINDOW_BLINDS = ModBlocksConference.WINDOW_BLINDS;
    public static final DeferredBlock<PortableSpaceHeaterBlock> PORTABLE_SPACE_HEATER = ModBlocksConference.PORTABLE_SPACE_HEATER;
    public static final DeferredBlock<HalogenFloorLampBlock> HALOGEN_FLOOR_LAMP = ModBlocksConference.HALOGEN_FLOOR_LAMP;
    public static final DeferredBlock<JammedProjectionScreenBlock> JAMMED_PROJECTION_SCREEN = ModBlocksConference.JAMMED_PROJECTION_SCREEN;
    public static final DeferredBlock<OverheatingVideoWallBlock> OVERHEATING_VIDEO_WALL = ModBlocksConference.OVERHEATING_VIDEO_WALL;
    public static final DeferredBlock<AerosolFreshenerDispenserBlock> AEROSOL_FRESHENER_DISPENSER = ModBlocksConference.AEROSOL_FRESHENER_DISPENSER;
    public static final DeferredBlock<SmotheredLaptopBlock> SMOTHERED_LAPTOP = ModBlocksConference.SMOTHERED_LAPTOP;
    public static final DeferredBlock<SmolderingPlanterBlock> SMOLDERING_PLANTER = ModBlocksConference.SMOLDERING_PLANTER;
    public static final DeferredBlock<PinchedTvCordBlock> PINCHED_TV_CORD = ModBlocksConference.PINCHED_TV_CORD;
    public static final DeferredBlock<VentingUpsBatteryBlock> VENTING_UPS_BATTERY = ModBlocksConference.VENTING_UPS_BATTERY;
    public static final DeferredBlock<FaultyDimmerSwitchBlock> FAULTY_DIMMER_SWITCH = ModBlocksConference.FAULTY_DIMMER_SWITCH;

    // ── Office furniture + hazards (66-75) — see ModBlocksOffice ─────────────
    public static final DeferredBlock<OfficeCubiclePartitionBlock> OFFICE_CUBICLE_PARTITION = ModBlocksOffice.OFFICE_CUBICLE_PARTITION;
    public static final DeferredBlock<ReceptionDeskBlock> RECEPTION_DESK = ModBlocksOffice.RECEPTION_DESK;
    public static final DeferredBlock<MailSortingShelfBlock> MAIL_SORTING_SHELF = ModBlocksOffice.MAIL_SORTING_SHELF;
    public static final DeferredBlock<OfficePhotocopierBlock> OFFICE_PHOTOCOPIER = ModBlocksOffice.OFFICE_PHOTOCOPIER;
    public static final DeferredBlock<DocumentTrayStackBlock> DOCUMENT_TRAY_STACK = ModBlocksOffice.DOCUMENT_TRAY_STACK;
    public static final DeferredBlock<WallBinderShelfBlock> WALL_BINDER_SHELF = ModBlocksOffice.WALL_BINDER_SHELF;
    public static final DeferredBlock<OfficeSafeBlock> OFFICE_SAFE = ModBlocksOffice.OFFICE_SAFE;
    public static final DeferredBlock<CoatRackStandBlock> COAT_RACK_STAND = ModBlocksOffice.COAT_RACK_STAND;
    public static final DeferredBlock<BundyTimeClockBlock> BUNDY_TIME_CLOCK = ModBlocksOffice.BUNDY_TIME_CLOCK;
    public static final DeferredBlock<OfficeSupplyCabinetBlock> OFFICE_SUPPLY_CABINET = ModBlocksOffice.OFFICE_SUPPLY_CABINET;
    public static final DeferredBlock<JammedPaperShredderBlock> JAMMED_PAPER_SHREDDER = ModBlocksOffice.JAMMED_PAPER_SHREDDER;
    public static final DeferredBlock<OverheatedNetworkCabinetBlock> OVERHEATED_NETWORK_CABINET = ModBlocksOffice.OVERHEATED_NETWORK_CABINET;
    public static final DeferredBlock<EbikeChargingStationBlock> EBIKE_CHARGING_STATION = ModBlocksOffice.EBIKE_CHARGING_STATION;
    public static final DeferredBlock<FailingFluorescentBallastBlock> FAILING_FLUORESCENT_BALLAST = ModBlocksOffice.FAILING_FLUORESCENT_BALLAST;
    public static final DeferredBlock<DryAquariumHeaterBlock> DRY_AQUARIUM_HEATER = ModBlocksOffice.DRY_AQUARIUM_HEATER;
    public static final DeferredBlock<UnattendedMugWarmerBlock> UNATTENDED_MUG_WARMER = ModBlocksOffice.UNATTENDED_MUG_WARMER;
    public static final DeferredBlock<DustyCrtMonitorBlock> DUSTY_CRT_MONITOR = ModBlocksOffice.DUSTY_CRT_MONITOR;
    public static final DeferredBlock<RodentChewedWiringBlock> RODENT_CHEWED_WIRING = ModBlocksOffice.RODENT_CHEWED_WIRING;
    public static final DeferredBlock<OverheatingCctvDvrBlock> OVERHEATING_CCTV_DVR = ModBlocksOffice.OVERHEATING_CCTV_DVR;
    public static final DeferredBlock<FaultyParolLanternBlock> FAULTY_PAROL_LANTERN = ModBlocksOffice.FAULTY_PAROL_LANTERN;

    // ── Laboratory furniture + hazards (76-85) — see ModBlocksLaboratory ─────
    public static final DeferredBlock<LaboratoryFumeHoodBlock> LABORATORY_FUME_HOOD = ModBlocksLaboratory.LABORATORY_FUME_HOOD;
    public static final DeferredBlock<EquipmentRackBlock> EQUIPMENT_RACK = ModBlocksLaboratory.EQUIPMENT_RACK;
    public static final DeferredBlock<LabStoolBlock> LAB_STOOL = ModBlocksLaboratory.LAB_STOOL;
    public static final DeferredBlock<OscilloscopeCartBlock> OSCILLOSCOPE_CART = ModBlocksLaboratory.OSCILLOSCOPE_CART;
    public static final DeferredBlock<MicroscopeStationBlock> MICROSCOPE_STATION = ModBlocksLaboratory.MICROSCOPE_STATION;
    public static final DeferredBlock<EyeWashStationBlock> EYE_WASH_STATION = ModBlocksLaboratory.EYE_WASH_STATION;
    public static final DeferredBlock<ComponentDrawerCabinetBlock> COMPONENT_DRAWER_CABINET = ModBlocksLaboratory.COMPONENT_DRAWER_CABINET;
    public static final DeferredBlock<SecuredCylinderRackBlock> SECURED_CYLINDER_RACK = ModBlocksLaboratory.SECURED_CYLINDER_RACK;
    public static final DeferredBlock<SampleStorageRackBlock> SAMPLE_STORAGE_RACK = ModBlocksLaboratory.SAMPLE_STORAGE_RACK;
    public static final DeferredBlock<BalanceScaleTableBlock> BALANCE_SCALE_TABLE = ModBlocksLaboratory.BALANCE_SCALE_TABLE;
    public static final DeferredBlock<UnbalancedCentrifugeBlock> UNBALANCED_CENTRIFUGE = ModBlocksLaboratory.UNBALANCED_CENTRIFUGE;
    public static final DeferredBlock<Runaway3dPrinterBlock> RUNAWAY_3D_PRINTER = ModBlocksLaboratory.RUNAWAY_3D_PRINTER;
    public static final DeferredBlock<UnattendedSolderingIronBlock> UNATTENDED_SOLDERING_IRON = ModBlocksLaboratory.UNATTENDED_SOLDERING_IRON;
    public static final DeferredBlock<UnshieldedTestLaserBlock> UNSHIELDED_TEST_LASER = ModBlocksLaboratory.UNSHIELDED_TEST_LASER;
    public static final DeferredBlock<LeakingOxygenCylinderBlock> LEAKING_OXYGEN_CYLINDER = ModBlocksLaboratory.LEAKING_OXYGEN_CYLINDER;
    public static final DeferredBlock<ShortedBenchSupplyBlock> SHORTED_BENCH_SUPPLY = ModBlocksLaboratory.SHORTED_BENCH_SUPPLY;
    public static final DeferredBlock<OverheatedVacuumPumpBlock> OVERHEATED_VACUUM_PUMP = ModBlocksLaboratory.OVERHEATED_VACUUM_PUMP;
    public static final DeferredBlock<StuckEnvironmentChamberBlock> STUCK_ENVIRONMENT_CHAMBER = ModBlocksLaboratory.STUCK_ENVIRONMENT_CHAMBER;
    public static final DeferredBlock<SolventDryingOvenBlock> SOLVENT_DRYING_OVEN = ModBlocksLaboratory.SOLVENT_DRYING_OVEN;
    public static final DeferredBlock<FaultyDehumidifierBlock> FAULTY_DEHUMIDIFIER = ModBlocksLaboratory.FAULTY_DEHUMIDIFIER;

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
