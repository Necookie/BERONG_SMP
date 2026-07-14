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
 * All block registrations for the mod: the computer/fire-alarm interactive blocks, the furniture
 * blocks, and the 20 hazard prop blocks. Extracted from {@code BerongSMP} so the entry point
 * stays a thin bootstrap; {@link #register(IEventBus)} is called from its constructor.
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

    /** Plastic waste bin — emits smoke when has_vape=true (hazardous). */
    public static final DeferredBlock<PlasticTrashBinBlock> PLASTIC_TRASH_BIN = BLOCKS.registerBlock(
            "plastic_trash_bin", PlasticTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(0.5f, 1.0f).sound(SoundType.STONE).noOcclusion());

    /** Overloaded daisy-chain extension cord — electric sparks when overloaded=true. */
    public static final DeferredBlock<DaisyChainExtensionBlock> DAISY_CHAIN_EXTENSION = BLOCKS.registerBlock(
            "daisy_chain_extension", DaisyChainExtensionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f, 0.5f).sound(SoundType.STONE).noOcclusion());

    /** Floor sawdust accumulation layer — emits ash particles at accumulation >= 3. */
    public static final DeferredBlock<WoodshopSawdustLayerBlock> WOODSHOP_SAWDUST_LAYER = BLOCKS.registerBlock(
            "woodshop_sawdust_layer", WoodshopSawdustLayerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.1f, 0.1f).sound(SoundType.SAND).noOcclusion());

    /** Stage/theatre spotlight — overheating housing emits flame and smoke when hazardous. */
    public static final DeferredBlock<StageSpotlightBlock> STAGE_SPOTLIGHT = BLOCKS.registerBlock(
            "stage_spotlight", StageSpotlightBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Stack of flammable archive document boxes — fire proximity raises hazard. */
    public static final DeferredBlock<ArchiveBoxStackBlock> ARCHIVE_BOX_STACK = BLOCKS.registerBlock(
            "archive_box_stack", ArchiveBoxStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0f, 1.0f).sound(SoundType.WOOL).noOcclusion());

    /** Desktop PC tower with dust-clogged vents — overheats and emits smoke when hazardous. */
    public static final DeferredBlock<DustChokedPcBlock> DUST_CHOKED_PC = BLOCKS.registerBlock(
            "dust_choked_pc", DustChokedPcBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Rolling Chromebook/laptop charging cart — overloaded outlets spark when hazardous. */
    public static final DeferredBlock<ChargingCartBlock> CHARGING_CART = BLOCKS.registerBlock(
            "charging_cart", ChargingCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 5 : 0));

    /** Frayed AV/console wire on the floor — exposed copper arcs blue sparks when hazardous. */
    public static final DeferredBlock<FrayedConsoleWireBlock> FRAYED_CONSOLE_WIRE = BLOCKS.registerBlock(
            "frayed_console_wire", FrayedConsoleWireBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Vending machine with shorted compressor — smokes from back vents when hazardous. */
    public static final DeferredBlock<MalfunctioningVendingBlock> MALFUNCTIONING_VENDING = BLOCKS.registerBlock(
            "malfunctioning_vending", MalfunctioningVendingBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** Ceiling-mounted projector with failed cooling fan — overheats and smokes. */
    public static final DeferredBlock<CeilingProjectorBlock> CEILING_PROJECTOR = BLOCKS.registerBlock(
            "ceiling_projector", CeilingProjectorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 0));

    /** Swollen Li-ion phone battery left on desk — thermal runaway risk; cyan soul-flame gas when hazardous. */
    public static final DeferredBlock<SwollenPhoneBatteryBlock> SWOLLEN_PHONE_BATTERY = BLOCKS.registerBlock(
            "swollen_phone_battery", SwollenPhoneBatteryBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.METAL).noOcclusion());

    /** Damaged LiPo battery pack (drone/RC) — punctured cells off-gas smoke when hazardous. */
    public static final DeferredBlock<DamagedLipoPackBlock> DAMAGED_LIPO_PACK = BLOCKS.registerBlock(
            "damaged_lipo_pack", DamagedLipoPackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Iron locker with a vape device inside — sparks and smoke leak from vent slot when hazardous. */
    public static final DeferredBlock<VapeInIronLockerBlock> VAPE_IN_IRON_LOCKER = BLOCKS.registerBlock(
            "vape_in_iron_locker", VapeInIronLockerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** PA/public-address backup amp rack — faulty capacitors spark and glow when hazardous. */
    public static final DeferredBlock<PaSystemBackupBlock> PA_SYSTEM_BACKUP = BLOCKS.registerBlock(
            "pa_system_backup", PaSystemBackupBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Smartboard power inverter — roof leak drips on live electronics when hazardous. */
    public static final DeferredBlock<SmartboardInverterBlock> SMARTBOARD_INVERTER = BLOCKS.registerBlock(
            "smartboard_inverter", SmartboardInverterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Stove with a grease pan left unattended — grease fire erupts when hazardous. */
    public static final DeferredBlock<UnattendedGreasePanBlock> UNATTENDED_GREASE_PAN = BLOCKS.registerBlock(
            "unattended_grease_pan", UnattendedGreasePanBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Kitchen range hood with clogged grease filters — backflow smoke when hazardous. */
    public static final DeferredBlock<GreaseCloggedHoodBlock> GREASE_CLOGGED_HOOD = BLOCKS.registerBlock(
            "grease_clogged_hood", GreaseCloggedHoodBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Kitchen waste bin with oil-soaked contaminated rags — self-heating rags emit steam when hazardous. */
    public static final DeferredBlock<ContaminatedKitchenBinBlock> CONTAMINATED_KITCHEN_BIN = BLOCKS.registerBlock(
            "contaminated_kitchen_bin", ContaminatedKitchenBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Panini press with jammed lid and burning food — smoking when hazardous. */
    public static final DeferredBlock<JammedPaniniPressBlock> JAMMED_PANINI_PRESS = BLOCKS.registerBlock(
            "jammed_panini_press", JammedPaniniPressBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Commercial deep fryer — overheated oil ignites and erupts smoke/flame when hazardous. */
    public static final DeferredBlock<CommercialDeepFryerBlock> COMMERCIAL_DEEP_FRYER = BLOCKS.registerBlock(
            "commercial_deep_fryer", CommercialDeepFryerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 12 : 0));

    /** Overloaded Microwave (Electrical Hazard) — see {@link OverloadedMicrowaveBlock}. */
    public static final DeferredBlock<OverloadedMicrowaveBlock> OVERLOADED_MICROWAVE = BLOCKS.registerBlock(
            "overloaded_microwave", OverloadedMicrowaveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Bunsen Burner Station (Open Flame Hazard) — see {@link BunsenBurnerStationBlock}. */
    public static final DeferredBlock<BunsenBurnerStationBlock> BUNSEN_BURNER_STATION = BLOCKS.registerBlock(
            "bunsen_burner_station", BunsenBurnerStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 9 : 0));

    /** Reagent Storage Shelf (Chemical Hazard) — see {@link ReagentStorageShelfBlock}. */
    public static final DeferredBlock<ReagentStorageShelfBlock> REAGENT_STORAGE_SHELF = BLOCKS.registerBlock(
            "reagent_storage_shelf", ReagentStorageShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Overloaded Breaker Panel (Electrical Hazard) — see {@link OverloadedBreakerPanelBlock}. */
    public static final DeferredBlock<OverloadedBreakerPanelBlock> OVERLOADED_BREAKER_PANEL = BLOCKS.registerBlock(
            "overloaded_breaker_panel", OverloadedBreakerPanelBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Overheating Wall Aircon (Electrical Hazard) — see {@link OverheatingWallAirconBlock}. */
    public static final DeferredBlock<OverheatingWallAirconBlock> OVERHEATING_WALL_AIRCON = BLOCKS.registerBlock(
            "overheating_wall_aircon", OverheatingWallAirconBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Jammed Laser Printer (Overheat Hazard) — see {@link JammedLaserPrinterBlock}. */
    public static final DeferredBlock<JammedLaserPrinterBlock> JAMMED_LASER_PRINTER = BLOCKS.registerBlock(
            "jammed_laser_printer", JammedLaserPrinterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Unattended Shrine Candle (Open Flame Hazard) — see {@link UnattendedShrineCandleBlock}. */
    public static final DeferredBlock<UnattendedShrineCandleBlock> UNATTENDED_SHRINE_CANDLE = BLOCKS.registerBlock(
            "unattended_shrine_candle", UnattendedShrineCandleBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Leaking Gas Valve (Kitchen Gas Hazard) — see {@link LeakingGasValveBlock}. */
    public static final DeferredBlock<LeakingGasValveBlock> LEAKING_GAS_VALVE = BLOCKS.registerBlock(
            "leaking_gas_valve", LeakingGasValveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion());

    /** Alcohol Dispenser Station (Flammable Liquid Hazard) — see {@link AlcoholDispenserStationBlock}. */
    public static final DeferredBlock<AlcoholDispenserStationBlock> ALCOHOL_DISPENSER_STATION = BLOCKS.registerBlock(
            "alcohol_dispenser_station", AlcoholDispenserStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 2 : 0));

    /** Clogged Exhaust Fan (Dust Overheat Hazard) — see {@link CloggedExhaustFanBlock}. */
    public static final DeferredBlock<CloggedExhaustFanBlock> CLOGGED_EXHAUST_FAN = BLOCKS.registerBlock(
            "clogged_exhaust_fan", CloggedExhaustFanBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Overloaded Wall Outlet — see {@link OverloadedWallOutletBlock}. */
    public static final DeferredBlock<OverloadedWallOutletBlock> OVERLOADED_WALL_OUTLET = BLOCKS.registerBlock(
            "overloaded_wall_outlet", OverloadedWallOutletBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Jammed Circuit Breaker Box — see {@link JammedCircuitBreakerBlock}. */
    public static final DeferredBlock<JammedCircuitBreakerBlock> JAMMED_CIRCUIT_BREAKER = BLOCKS.registerBlock(
            "jammed_circuit_breaker", JammedCircuitBreakerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 5 : 0));

    /** Unsealed Solvent Shelf — see {@link UnsealedSolventShelfBlock}. */
    public static final DeferredBlock<UnsealedSolventShelfBlock> UNSEALED_SOLVENT_SHELF = BLOCKS.registerBlock(
            "unsealed_solvent_shelf", UnsealedSolventShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Unattended Welding Station — see {@link UnattendedWeldingStationBlock}. */
    public static final DeferredBlock<UnattendedWeldingStationBlock> UNATTENDED_WELDING_STATION = BLOCKS.registerBlock(
            "unattended_welding_station", UnattendedWeldingStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Leaking Butane Canister Stove — see {@link LeakingButaneCanisterStoveBlock}. */
    public static final DeferredBlock<LeakingButaneCanisterStoveBlock> LEAKING_BUTANE_CANISTER_STOVE = BLOCKS.registerBlock(
            "leaking_butane_canister_stove", LeakingButaneCanisterStoveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Chef's Prep Table — see {@link ChefsPrepDrawersBlock}. */
    public static final DeferredBlock<ChefsPrepDrawersBlock> CHEFS_PREP_DRAWERS = BLOCKS.registerBlock(
            "chefs_prep_drawers", ChefsPrepDrawersBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Culinary Lab Refrigerator — see {@link CulinaryFridgeBlock}. */
    public static final DeferredBlock<CulinaryFridgeBlock> CULINARY_FRIDGE = BLOCKS.registerBlock(
            "culinary_fridge", CulinaryFridgeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Student Lab Microwave — see {@link StudentLabMicrowaveBlock}. */
    public static final DeferredBlock<StudentLabMicrowaveBlock> STUDENT_LAB_MICROWAVE = BLOCKS.registerBlock(
            "student_lab_microwave", StudentLabMicrowaveBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Corroded Gas Line Joint — see {@link CorrodedGasLineJointBlock}. */
    public static final DeferredBlock<CorrodedGasLineJointBlock> CORRODED_GAS_LINE_JOINT = BLOCKS.registerBlock(
            "corroded_gas_line_joint", CorrodedGasLineJointBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Gas Range, Stuck Burner — see {@link GasRangeStuckBurnerBlock}. */
    public static final DeferredBlock<GasRangeStuckBurnerBlock> GAS_RANGE_STUCK_BURNER = BLOCKS.registerBlock(
            "gas_range_stuck_burner", GasRangeStuckBurnerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 11 : 0));

    /** Commercial Stand Mixer — see {@link CommercialStandMixerBlock}. */
    public static final DeferredBlock<CommercialStandMixerBlock> COMMERCIAL_STAND_MIXER = BLOCKS.registerBlock(
            "commercial_stand_mixer", CommercialStandMixerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Gas Deck Oven — see {@link GasDeckOvenBlock}. */
    public static final DeferredBlock<GasDeckOvenBlock> GAS_DECK_OVEN = BLOCKS.registerBlock(
            "gas_deck_oven", GasDeckOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));

    /** Induction Cooktop — see {@link InductionCooktopStationBlock}. */
    public static final DeferredBlock<InductionCooktopStationBlock> INDUCTION_COOKTOP_STATION = BLOCKS.registerBlock(
            "induction_cooktop_station", InductionCooktopStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Rice Cooker Bank — see {@link RiceCookerBankBlock}. */
    public static final DeferredBlock<RiceCookerBankBlock> RICE_COOKER_BANK = BLOCKS.registerBlock(
            "rice_cooker_bank", RiceCookerBankBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Espresso Machine — see {@link EspressoMachineBlock}. */
    public static final DeferredBlock<EspressoMachineBlock> ESPRESSO_MACHINE = BLOCKS.registerBlock(
            "espresso_machine", EspressoMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Hot Water Urn — see {@link HotWaterUrnBlock}. */
    public static final DeferredBlock<HotWaterUrnBlock> HOT_WATER_URN = BLOCKS.registerBlock(
            "hot_water_urn", HotWaterUrnBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Countertop Toaster Oven — see {@link ToasterOvenCrumbBlock}. */
    public static final DeferredBlock<ToasterOvenCrumbBlock> TOASTER_OVEN_CRUMB = BLOCKS.registerBlock(
            "toaster_oven_crumb", ToasterOvenCrumbBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 9 : 0));

    /** Dry Goods Pantry Shelf — see {@link DryGoodsPantryShelfBlock}. */
    public static final DeferredBlock<DryGoodsPantryShelfBlock> DRY_GOODS_PANTRY_SHELF = BLOCKS.registerBlock(
            "dry_goods_pantry_shelf", DryGoodsPantryShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Kitchen Exhaust Duct — see {@link GreaseDuctRunBlock}. */
    public static final DeferredBlock<GreaseDuctRunBlock> GREASE_DUCT_RUN = BLOCKS.registerBlock(
            "grease_duct_run", GreaseDuctRunBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Dish Sanitizer — see {@link CommercialDishSanitizerBlock}. */
    public static final DeferredBlock<CommercialDishSanitizerBlock> COMMERCIAL_DISH_SANITIZER = BLOCKS.registerBlock(
            "commercial_dish_sanitizer", CommercialDishSanitizerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Garbage Disposal Unit — see {@link GarbageDisposalUnitBlock}. */
    public static final DeferredBlock<GarbageDisposalUnitBlock> GARBAGE_DISPOSAL_UNIT = BLOCKS.registerBlock(
            "garbage_disposal_unit", GarbageDisposalUnitBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** UV Knife Sterilizer — see {@link KnifeSterilizerCabinetBlock}. */
    public static final DeferredBlock<KnifeSterilizerCabinetBlock> KNIFE_STERILIZER_CABINET = BLOCKS.registerBlock(
            "knife_sterilizer_cabinet", KnifeSterilizerCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Chafing Steam Table — see {@link SternoSteamTableBlock}. */
    public static final DeferredBlock<SternoSteamTableBlock> STERNO_STEAM_TABLE = BLOCKS.registerBlock(
            "sterno_steam_table", SternoSteamTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));

    /** Electric Convection Oven — see {@link ConvectionOvenBlock}. */
    public static final DeferredBlock<ConvectionOvenBlock> CONVECTION_OVEN = BLOCKS.registerBlock(
            "convection_oven", ConvectionOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 0));

    /** Lechon Rotisserie — see {@link LechonRotisserieSpitBlock}. */
    public static final DeferredBlock<LechonRotisserieSpitBlock> LECHON_ROTISSERIE_SPIT = BLOCKS.registerBlock(
            "lechon_rotisserie_spit", LechonRotisserieSpitBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 12 : 0));

    /** Teacher's Desk — see {@link TeachersDeskBlock}. */
    public static final DeferredBlock<TeachersDeskBlock> TEACHERS_DESK = BLOCKS.registerBlock(
            "teachers_desk", TeachersDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Armchair Desk — see {@link ArmchairDeskBlock}. */
    public static final DeferredBlock<ArmchairDeskBlock> ARMCHAIR_DESK = BLOCKS.registerBlock(
            "armchair_desk", ArmchairDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Tall Bookshelf — see {@link TallBookshelfBlock}. */
    public static final DeferredBlock<TallBookshelfBlock> TALL_BOOKSHELF = BLOCKS.registerBlock(
            "tall_bookshelf", TallBookshelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Philippine Flag Stand — see {@link PhilippineFlagStandBlock}. */
    public static final DeferredBlock<PhilippineFlagStandBlock> PHILIPPINE_FLAG_STAND = BLOCKS.registerBlock(
            "philippine_flag_stand", PhilippineFlagStandBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.GOLD)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Trophy Cabinet — see {@link TrophyCabinetBlock}. */
    public static final DeferredBlock<TrophyCabinetBlock> TROPHY_CABINET = BLOCKS.registerBlock(
            "trophy_cabinet", TrophyCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 4.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 3));

    /** Water Dispenser — see {@link WaterDispenserBlock}. */
    public static final DeferredBlock<WaterDispenserBlock> WATER_DISPENSER = BLOCKS.registerBlock(
            "water_dispenser", WaterDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall Clock — see {@link WallClockBlock}. */
    public static final DeferredBlock<WallClockBlock> WALL_CLOCK = BLOCKS.registerBlock(
            "wall_clock", WallClockBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Blackboard — see {@link BlackboardBlock}. */
    public static final DeferredBlock<BlackboardBlock> BLACKBOARD = BLOCKS.registerBlock(
            "blackboard", BlackboardBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Podium Lectern — see {@link PodiumLecternBlock}. */
    public static final DeferredBlock<PodiumLecternBlock> PODIUM_LECTERN = BLOCKS.registerBlock(
            "podium_lectern", PodiumLecternBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Classroom Globe — see {@link ClassroomGlobeBlock}. */
    public static final DeferredBlock<ClassroomGlobeBlock> CLASSROOM_GLOBE = BLOCKS.registerBlock(
            "classroom_globe", ClassroomGlobeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Modern Single-Seat Student Desk — see {@link ModernStudentDeskBlock}. */
    public static final DeferredBlock<ModernStudentDeskBlock> MODERN_STUDENT_DESK = BLOCKS.registerBlock(
            "modern_student_desk", ModernStudentDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Science Lab Workbench — see {@link ScienceLabWorkbenchBlock}. */
    public static final DeferredBlock<ScienceLabWorkbenchBlock> SCIENCE_LAB_WORKBENCH = BLOCKS.registerBlock(
            "science_lab_workbench", ScienceLabWorkbenchBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.STONE).noOcclusion());

    /** Computer Lab Desk Row — see {@link ComputerLabDeskRowBlock}. */
    public static final DeferredBlock<ComputerLabDeskRowBlock> COMPUTER_LAB_DESK_ROW = BLOCKS.registerBlock(
            "computer_lab_desk_row", ComputerLabDeskRowBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Library Study Carrel — see {@link LibraryStudyCarrelBlock}. */
    public static final DeferredBlock<LibraryStudyCarrelBlock> LIBRARY_STUDY_CARREL = BLOCKS.registerBlock(
            "library_study_carrel", LibraryStudyCarrelBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Rolling Book Cart — see {@link RollingBookCartBlock}. */
    public static final DeferredBlock<RollingBookCartBlock> ROLLING_BOOK_CART = BLOCKS.registerBlock(
            "rolling_book_cart", RollingBookCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Emergency Exit Sign — see {@link ExitSignBlock}. */
    public static final DeferredBlock<ExitSignBlock> EXIT_SIGN = BLOCKS.registerBlock(
            "exit_sign", ExitSignBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.EMERALD)
                    .strength(0.5f, 2.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 7));

    /** Smoke Detector — see {@link SmokeDetectorBlock}. */
    public static final DeferredBlock<SmokeDetectorBlock> SMOKE_DETECTOR = BLOCKS.registerBlock(
            "smoke_detector", SmokeDetectorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Sprinkler Head — see {@link SprinklerHeadBlock}. */
    public static final DeferredBlock<SprinklerHeadBlock> SPRINKLER_HEAD = BLOCKS.registerBlock(
            "sprinkler_head", SprinklerHeadBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.GOLD)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Emergency Light — see {@link EmergencyLightBlock}. */
    public static final DeferredBlock<EmergencyLightBlock> EMERGENCY_LIGHT = BLOCKS.registerBlock(
            "emergency_light", EmergencyLightBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0));

    /** Evacuation Map — see {@link EvacuationMapBlock}. */
    public static final DeferredBlock<EvacuationMapBlock> EVACUATION_MAP = BLOCKS.registerBlock(
            "evacuation_map", EvacuationMapBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Fire Extinguisher Cabinet — see {@link FireExtinguisherCabinetBlock}. */
    public static final DeferredBlock<FireExtinguisherCabinetBlock> FIRE_EXTINGUISHER_CABINET = BLOCKS.registerBlock(
            "fire_extinguisher_cabinet", FireExtinguisherCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Evacuation Assembly Point Sign — see {@link AssemblyPointSignBlock}. */
    public static final DeferredBlock<AssemblyPointSignBlock> ASSEMBLY_POINT_SIGN = BLOCKS.registerBlock(
            "assembly_point_sign", AssemblyPointSignBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.EMERALD)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s -> 7));

    /** First-Aid Wall Cabinet — see {@link FirstAidWallCabinetBlock}. */
    public static final DeferredBlock<FirstAidWallCabinetBlock> FIRST_AID_WALL_CABINET = BLOCKS.registerBlock(
            "first_aid_wall_cabinet", FirstAidWallCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Fire Safety Poster — see {@link FireSafetyPosterBlock}. */
    public static final DeferredBlock<FireSafetyPosterBlock> FIRE_SAFETY_POSTER = BLOCKS.registerBlock(
            "fire_safety_poster", FireSafetyPosterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.2f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Blocked Exit Clutter Pile — see {@link BlockedExitClutterBlock}. */
    public static final DeferredBlock<BlockedExitClutterBlock> BLOCKED_EXIT_CLUTTER = BLOCKS.registerBlock(
            "blocked_exit_clutter", BlockedExitClutterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Long cafeteria lunch table with attached bench seats — see {@link CafeteriaTableBlock}. */
    public static final DeferredBlock<CafeteriaTableBlock> CAFETERIA_TABLE = BLOCKS.registerBlock(
            "cafeteria_table", CafeteriaTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Stack of plastic lunch trays — see {@link TrayStackBlock}. */
    public static final DeferredBlock<TrayStackBlock> TRAY_STACK = BLOCKS.registerBlock(
            "cafeteria_tray_stack", TrayStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Steam-table serving counter with sneeze guard — see {@link ServingCounterBlock}. */
    public static final DeferredBlock<ServingCounterBlock> SERVING_COUNTER = BLOCKS.registerBlock(
            "serving_counter", ServingCounterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall-mounted cafeteria menu board — see {@link CafeteriaMenuBoardBlock}. */
    public static final DeferredBlock<CafeteriaMenuBoardBlock> CAFETERIA_MENU_BOARD = BLOCKS.registerBlock(
            "cafeteria_menu_board", CafeteriaMenuBoardBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Condiment station with squeeze bottles and napkin holder — see {@link CondimentStationBlock}. */
    public static final DeferredBlock<CondimentStationBlock> CONDIMENT_STATION = BLOCKS.registerBlock(
            "condiment_station", CondimentStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Dual recycle/trash bin station — see {@link CafeteriaTrashBinBlock}. */
    public static final DeferredBlock<CafeteriaTrashBinBlock> CAFETERIA_TRASH_BIN = BLOCKS.registerBlock(
            "cafeteria_trash_bin", CafeteriaTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Soda fountain dispenser machine — see {@link SodaFountainMachineBlock}. */
    public static final DeferredBlock<SodaFountainMachineBlock> SODA_FOUNTAIN_MACHINE = BLOCKS.registerBlock(
            "soda_fountain_machine", SodaFountainMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Round vinyl-seat cafeteria stool — see {@link CafeteriaStoolBlock}. */
    public static final DeferredBlock<CafeteriaStoolBlock> CAFETERIA_STOOL = BLOCKS.registerBlock(
            "cafeteria_stool", CafeteriaStoolBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Refrigerated salad bar with sneeze guard — see {@link SaladBarBlock}. */
    public static final DeferredBlock<SaladBarBlock> SALAD_BAR = BLOCKS.registerBlock(
            "salad_bar", SaladBarBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Glass-front snack vending machine — see {@link SnackVendingMachineBlock}. */
    public static final DeferredBlock<SnackVendingMachineBlock> SNACK_VENDING_MACHINE = BLOCKS.registerBlock(
            "snack_vending_machine", SnackVendingMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(SnackVendingMachineBlock.CONNECTED_DOWN) ? 4 : 0));

    /** Kitchen Prep Counter — see {@link KitchenPrepCounterBlock}. */
    public static final DeferredBlock<KitchenPrepCounterBlock> KITCHEN_PREP_COUNTER = BLOCKS.registerBlock(
            "kitchen_prep_counter", KitchenPrepCounterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Dishwashing Sink Station — see {@link DishwashingSinkStationBlock}. */
    public static final DeferredBlock<DishwashingSinkStationBlock> DISHWASHING_SINK_STATION = BLOCKS.registerBlock(
            "dishwashing_sink_station", DishwashingSinkStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Iced-Tea / Juice Dispenser — see {@link BeverageJuiceDispenserBlock}. */
    public static final DeferredBlock<BeverageJuiceDispenserBlock> BEVERAGE_JUICE_DISPENSER = BLOCKS.registerBlock(
            "beverage_juice_dispenser", BeverageJuiceDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Cutlery & Napkin Caddy — see {@link CutleryNapkinCaddyBlock}. */
    public static final DeferredBlock<CutleryNapkinCaddyBlock> CUTLERY_NAPKIN_CADDY = BLOCKS.registerBlock(
            "cutlery_napkin_caddy", CutleryNapkinCaddyBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Serving Hatch Window — see {@link ServingHatchWindowBlock}. */
    public static final DeferredBlock<ServingHatchWindowBlock> SERVING_HATCH_WINDOW = BLOCKS.registerBlock(
            "serving_hatch_window", ServingHatchWindowBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Disguised light source — looks/sounds/burns exactly like vanilla oak planks — see {@link GlowingOakPlanksBlock}. */
    public static final DeferredBlock<GlowingOakPlanksBlock> GLOWING_OAK_PLANKS = BLOCKS.registerBlock(
            "glowing_oak_planks", GlowingOakPlanksBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f).sound(SoundType.WOOD)
                    .lightLevel(s -> 15));

    /** Smooth white auto-connecting court-marking line — see {@link CourtLineBlock}. */
    public static final DeferredBlock<CourtLineBlock> COURT_LINE = BLOCKS.registerBlock(
            "court_line", CourtLineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.2f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Badminton net anchor post ("stitch") — see {@link BadmintonNetPostBlock}. */
    public static final DeferredBlock<BadmintonNetPostBlock> BADMINTON_NET_POST = BLOCKS.registerBlock(
            "badminton_net_post", BadmintonNetPostBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Auto-filled badminton net panel — see {@link BadmintonNetMeshBlock}. */
    public static final DeferredBlock<BadmintonNetMeshBlock> BADMINTON_NET_MESH = BLOCKS.registerBlock(
            "badminton_net_mesh", BadmintonNetMeshBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Basketball hoop stand base (bottom anchor of the expandable pole) — see {@link BasketballHoopPostBlock}. */
    public static final DeferredBlock<BasketballHoopPostBlock> BASKETBALL_HOOP_POST = BLOCKS.registerBlock(
            "basketball_hoop_post", BasketballHoopPostBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Auto-filled basketball pole segment — see {@link BasketballPoleSegmentBlock}. */
    public static final DeferredBlock<BasketballPoleSegmentBlock> BASKETBALL_POLE = BLOCKS.registerBlock(
            "basketball_pole", BasketballPoleSegmentBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Basketball backboard + rim + net (top anchor of the expandable pole) — see {@link BasketballHoopBlock}. */
    public static final DeferredBlock<BasketballHoopBlock> BASKETBALL_HOOP = BLOCKS.registerBlock(
            "basketball_hoop", BasketballHoopBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    // ── Conference Room furniture ───────────────────────────────────────────

    /** Self-connecting dark-laminate boardroom table — see {@link ConferenceTableBlock}. */
    public static final DeferredBlock<ConferenceTableBlock> CONFERENCE_TABLE = BLOCKS.registerBlock(
            "conference_table", ConferenceTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** High-back leather swivel chair — see {@link ExecutiveOfficeChairBlock}. */
    public static final DeferredBlock<ExecutiveOfficeChairBlock> EXECUTIVE_OFFICE_CHAIR = BLOCKS.registerBlock(
            "executive_office_chair", ExecutiveOfficeChairBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Low walnut sideboard with sliding doors — see {@link ConferenceCredenzaBlock}. */
    public static final DeferredBlock<ConferenceCredenzaBlock> CONFERENCE_CREDENZA = BLOCKS.registerBlock(
            "conference_credenza", ConferenceCredenzaBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall-mounted meeting display, dark standby screen — see {@link ConferenceWallDisplayBlock}. */
    public static final DeferredBlock<ConferenceWallDisplayBlock> CONFERENCE_WALL_DISPLAY = BLOCKS.registerBlock(
            "conference_wall_display", ConferenceWallDisplayBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** A-frame flip-chart easel with paper pad — see {@link FlipChartEaselBlock}. */
    public static final DeferredBlock<FlipChartEaselBlock> FLIP_CHART_EASEL = BLOCKS.registerBlock(
            "flip_chart_easel", FlipChartEaselBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Three-legged "spider" speakerphone centerpiece — see {@link ConferenceSpeakerphoneBlock}. */
    public static final DeferredBlock<ConferenceSpeakerphoneBlock> CONFERENCE_SPEAKERPHONE = BLOCKS.registerBlock(
            "conference_speakerphone", ConferenceSpeakerphoneBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Frosted-glass partition panel in an aluminum frame — see {@link GlassOfficePartitionBlock}. */
    public static final DeferredBlock<GlassOfficePartitionBlock> GLASS_OFFICE_PARTITION = BLOCKS.registerBlock(
            "glass_office_partition", GlassOfficePartitionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Two-seat fabric reception sofa — see {@link LoungeSofaBlock}. */
    public static final DeferredBlock<LoungeSofaBlock> LOUNGE_SOFA = BLOCKS.registerBlock(
            "lounge_sofa", LoungeSofaBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Decorative floor planter with a broadleaf plant — see {@link PottedOfficePlantBlock}. */
    public static final DeferredBlock<PottedOfficePlantBlock> POTTED_OFFICE_PLANT = BLOCKS.registerBlock(
            "potted_office_plant", PottedOfficePlantBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.PLANT)
                    .strength(0.5f, 1.0f).sound(SoundType.GRASS).noOcclusion());

    /** Wall-mounted venetian blinds, slats half-open — see {@link WindowBlindsBlock}. */
    public static final DeferredBlock<WindowBlindsBlock> WINDOW_BLINDS = BLOCKS.registerBlock(
            "window_blinds", WindowBlindsBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    // ── Conference Room hazards (props 56-65) ───────────────────────────────

    /** Space heater tipped against papers — see {@link PortableSpaceHeaterBlock}. */
    public static final DeferredBlock<PortableSpaceHeaterBlock> PORTABLE_SPACE_HEATER = BLOCKS.registerBlock(
            "portable_space_heater", PortableSpaceHeaterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Tipped torchiere scorching a curtain — see {@link HalogenFloorLampBlock}. */
    public static final DeferredBlock<HalogenFloorLampBlock> HALOGEN_FLOOR_LAMP = BLOCKS.registerBlock(
            "halogen_floor_lamp", HalogenFloorLampBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 3));

    /** Stalled ceiling projection-screen motor — see {@link JammedProjectionScreenBlock}. */
    public static final DeferredBlock<JammedProjectionScreenBlock> JAMMED_PROJECTION_SCREEN = BLOCKS.registerBlock(
            "jammed_projection_screen", JammedProjectionScreenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Overheating driver stack behind an LED video wall — see {@link OverheatingVideoWallBlock}. */
    public static final DeferredBlock<OverheatingVideoWallBlock> OVERHEATING_VIDEO_WALL = BLOCKS.registerBlock(
            "overheating_video_wall", OverheatingVideoWallBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Aerosol dispenser misting propellant near a heat register — see {@link AerosolFreshenerDispenserBlock}. */
    public static final DeferredBlock<AerosolFreshenerDispenserBlock> AEROSOL_FRESHENER_DISPENSER = BLOCKS.registerBlock(
            "aerosol_freshener_dispenser", AerosolFreshenerDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Laptop smothered under sofa cushions — see {@link SmotheredLaptopBlock}. */
    public static final DeferredBlock<SmotheredLaptopBlock> SMOTHERED_LAPTOP = BLOCKS.registerBlock(
            "smothered_laptop", SmotheredLaptopBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Cigarette smoldering in a planter's peat — see {@link SmolderingPlanterBlock}. */
    public static final DeferredBlock<SmolderingPlanterBlock> SMOLDERING_PLANTER = BLOCKS.registerBlock(
            "smoldering_planter", SmolderingPlanterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.PLANT)
                    .strength(0.5f, 1.0f).sound(SoundType.GRAVEL).noOcclusion());

    /** TV power cord crushed by its wall mount — see {@link PinchedTvCordBlock}. */
    public static final DeferredBlock<PinchedTvCordBlock> PINCHED_TV_CORD = BLOCKS.registerBlock(
            "pinched_tv_cord", PinchedTvCordBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());

    /** Failing UPS battery off-gassing under a credenza — see {@link VentingUpsBatteryBlock}. */
    public static final DeferredBlock<VentingUpsBatteryBlock> VENTING_UPS_BATTERY = BLOCKS.registerBlock(
            "venting_ups_battery", VentingUpsBatteryBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Over-lamped dimmer switch scorching its faceplate — see {@link FaultyDimmerSwitchBlock}. */
    public static final DeferredBlock<FaultyDimmerSwitchBlock> FAULTY_DIMMER_SWITCH = BLOCKS.registerBlock(
            "faulty_dimmer_switch", FaultyDimmerSwitchBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());

    // ── Office furniture ─────────────────────────────────────────────────

    /** Fabric-covered cubicle divider panel — see {@link OfficeCubiclePartitionBlock}. */
    public static final DeferredBlock<OfficeCubiclePartitionBlock> OFFICE_CUBICLE_PARTITION = BLOCKS.registerBlock(
            "office_cubicle_partition", OfficeCubiclePartitionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Tall front-counter reception desk — see {@link ReceptionDeskBlock}. */
    public static final DeferredBlock<ReceptionDeskBlock> RECEPTION_DESK = BLOCKS.registerBlock(
            "reception_desk", ReceptionDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall pigeonhole mail-slot unit — see {@link MailSortingShelfBlock}. */
    public static final DeferredBlock<MailSortingShelfBlock> MAIL_SORTING_SHELF = BLOCKS.registerBlock(
            "mail_sorting_shelf", MailSortingShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Large floor office copier — see {@link OfficePhotocopierBlock}. */
    public static final DeferredBlock<OfficePhotocopierBlock> OFFICE_PHOTOCOPIER = BLOCKS.registerBlock(
            "office_photocopier", OfficePhotocopierBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Desktop stack of letter trays — see {@link DocumentTrayStackBlock}. */
    public static final DeferredBlock<DocumentTrayStackBlock> DOCUMENT_TRAY_STACK = BLOCKS.registerBlock(
            "document_tray_stack", DocumentTrayStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall shelf of colored ring binders — see {@link WallBinderShelfBlock}. */
    public static final DeferredBlock<WallBinderShelfBlock> WALL_BINDER_SHELF = BLOCKS.registerBlock(
            "wall_binder_shelf", WallBinderShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Squat steel safe with a dial — see {@link OfficeSafeBlock}. */
    public static final DeferredBlock<OfficeSafeBlock> OFFICE_SAFE = BLOCKS.registerBlock(
            "office_safe", OfficeSafeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 8.0f).sound(SoundType.METAL).noOcclusion());

    /** Standing wooden coat rack — see {@link CoatRackStandBlock}. */
    public static final DeferredBlock<CoatRackStandBlock> COAT_RACK_STAND = BLOCKS.registerBlock(
            "coat_rack_stand", CoatRackStandBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall punch clock with a timecard rack — see {@link BundyTimeClockBlock}. */
    public static final DeferredBlock<BundyTimeClockBlock> BUNDY_TIME_CLOCK = BLOCKS.registerBlock(
            "bundy_time_clock", BundyTimeClockBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Tall stationery cabinet, doors ajar — see {@link OfficeSupplyCabinetBlock}. */
    public static final DeferredBlock<OfficeSupplyCabinetBlock> OFFICE_SUPPLY_CABINET = BLOCKS.registerBlock(
            "office_supply_cabinet", OfficeSupplyCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    // ── Office hazards (props 66-75) ────────────────────────────────────────

    /** Jammed, overfed paper shredder — see {@link JammedPaperShredderBlock}. */
    public static final DeferredBlock<JammedPaperShredderBlock> JAMMED_PAPER_SHREDDER = BLOCKS.registerBlock(
            "jammed_paper_shredder", JammedPaperShredderBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Clogged-fan comms cabinet overheating — see {@link OverheatedNetworkCabinetBlock}. */
    public static final DeferredBlock<OverheatedNetworkCabinetBlock> OVERHEATED_NETWORK_CABINET = BLOCKS.registerBlock(
            "overheated_network_cabinet", OverheatedNetworkCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** E-bike thermal-runaway charging station — see {@link EbikeChargingStationBlock}. */
    public static final DeferredBlock<EbikeChargingStationBlock> EBIKE_CHARGING_STATION = BLOCKS.registerBlock(
            "ebike_charging_station", EbikeChargingStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Failing fluorescent ballast dripping tar — see {@link FailingFluorescentBallastBlock}. */
    public static final DeferredBlock<FailingFluorescentBallastBlock> FAILING_FLUORESCENT_BALLAST = BLOCKS.registerBlock(
            "failing_fluorescent_ballast", FailingFluorescentBallastBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 8));

    /** Aquarium heater exposed by a dropped water level — see {@link DryAquariumHeaterBlock}. */
    public static final DeferredBlock<DryAquariumHeaterBlock> DRY_AQUARIUM_HEATER = BLOCKS.registerBlock(
            "dry_aquarium_heater", DryAquariumHeaterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Desk mug warmer buried under a memo pile — see {@link UnattendedMugWarmerBlock}. */
    public static final DeferredBlock<UnattendedMugWarmerBlock> UNATTENDED_MUG_WARMER = BLOCKS.registerBlock(
            "unattended_mug_warmer", UnattendedMugWarmerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 2 : 0));

    /** Old CRT monitor arcing through years of dust — see {@link DustyCrtMonitorBlock}. */
    public static final DeferredBlock<DustyCrtMonitorBlock> DUSTY_CRT_MONITOR = BLOCKS.registerBlock(
            "dusty_crt_monitor", DustyCrtMonitorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Baseboard wiring gnawed to bare copper — see {@link RodentChewedWiringBlock}. */
    public static final DeferredBlock<RodentChewedWiringBlock> RODENT_CHEWED_WIRING = BLOCKS.registerBlock(
            "rodent_chewed_wiring", RodentChewedWiringBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());

    /** 24/7 security DVR with blocked vents — see {@link OverheatingCctvDvrBlock}. */
    public static final DeferredBlock<OverheatingCctvDvrBlock> OVERHEATING_CCTV_DVR = BLOCKS.registerBlock(
            "overheating_cctv_dvr", OverheatingCctvDvrBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Faulty parol lantern with substandard series lights — see {@link FaultyParolLanternBlock}. */
    public static final DeferredBlock<FaultyParolLanternBlock> FAULTY_PAROL_LANTERN = BLOCKS.registerBlock(
            "faulty_parol_lantern", FaultyParolLanternBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 2));

    // ── Laboratory furniture ─────────────────────────────────────────────

    /** Full-height fume hood with glass sash — see {@link LaboratoryFumeHoodBlock}. */
    public static final DeferredBlock<LaboratoryFumeHoodBlock> LABORATORY_FUME_HOOD = BLOCKS.registerBlock(
            "laboratory_fume_hood", LaboratoryFumeHoodBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Floor-standing 19-inch instrument rack — see {@link EquipmentRackBlock}. */
    public static final DeferredBlock<EquipmentRackBlock> EQUIPMENT_RACK = BLOCKS.registerBlock(
            "equipment_rack", EquipmentRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Adjustable lab stool on a steel column — see {@link LabStoolBlock}. */
    public static final DeferredBlock<LabStoolBlock> LAB_STOOL = BLOCKS.registerBlock(
            "lab_stool", LabStoolBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Rolling oscilloscope instrument cart — see {@link OscilloscopeCartBlock}. */
    public static final DeferredBlock<OscilloscopeCartBlock> OSCILLOSCOPE_CART = BLOCKS.registerBlock(
            "oscilloscope_cart", OscilloscopeCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Bench microscope on a small stand — see {@link MicroscopeStationBlock}. */
    public static final DeferredBlock<MicroscopeStationBlock> MICROSCOPE_STATION = BLOCKS.registerBlock(
            "microscope_station", MicroscopeStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Green wall eyewash fountain — see {@link EyeWashStationBlock}. */
    public static final DeferredBlock<EyeWashStationBlock> EYE_WASH_STATION = BLOCKS.registerBlock(
            "eye_wash_station", EyeWashStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall cabinet of labeled electronics-parts drawers — see {@link ComponentDrawerCabinetBlock}. */
    public static final DeferredBlock<ComponentDrawerCabinetBlock> COMPONENT_DRAWER_CABINET = BLOCKS.registerBlock(
            "component_drawer_cabinet", ComponentDrawerCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall rack of properly chained gas cylinders — see {@link SecuredCylinderRackBlock}. */
    public static final DeferredBlock<SecuredCylinderRackBlock> SECURED_CYLINDER_RACK = BLOCKS.registerBlock(
            "secured_cylinder_rack", SecuredCylinderRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Shelf of labeled sample boxes and vial trays — see {@link SampleStorageRackBlock}. */
    public static final DeferredBlock<SampleStorageRackBlock> SAMPLE_STORAGE_RACK = BLOCKS.registerBlock(
            "sample_storage_rack", SampleStorageRackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Anti-vibration granite balance table — see {@link BalanceScaleTableBlock}. */
    public static final DeferredBlock<BalanceScaleTableBlock> BALANCE_SCALE_TABLE = BLOCKS.registerBlock(
            "balance_scale_table", BalanceScaleTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.STONE).noOcclusion());

    // ── Laboratory hazards (props 76-85) ────────────────────────────────────

    /** Unbalanced centrifuge rotor walking the bench — see {@link UnbalancedCentrifugeBlock}. */
    public static final DeferredBlock<UnbalancedCentrifugeBlock> UNBALANCED_CENTRIFUGE = BLOCKS.registerBlock(
            "unbalanced_centrifuge", UnbalancedCentrifugeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** 3D printer thermal-runaway on a dislodged heater cartridge — see {@link Runaway3dPrinterBlock}. */
    public static final DeferredBlock<Runaway3dPrinterBlock> RUNAWAY_3D_PRINTER = BLOCKS.registerBlock(
            "runaway_3d_printer", Runaway3dPrinterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Soldering iron left powered at full temperature — see {@link UnattendedSolderingIronBlock}. */
    public static final DeferredBlock<UnattendedSolderingIronBlock> UNATTENDED_SOLDERING_IRON = BLOCKS.registerBlock(
            "unattended_soldering_iron", UnattendedSolderingIronBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Class-4 test laser running with its beam dump missing — see {@link UnshieldedTestLaserBlock}. */
    public static final DeferredBlock<UnshieldedTestLaserBlock> UNSHIELDED_TEST_LASER = BLOCKS.registerBlock(
            "unshielded_test_laser", UnshieldedTestLaserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Leaking oxygen cylinder — oxidizer enrichment, CO2-only — see {@link LeakingOxygenCylinderBlock}. */
    public static final DeferredBlock<LeakingOxygenCylinderBlock> LEAKING_OXYGEN_CYLINDER = BLOCKS.registerBlock(
            "leaking_oxygen_cylinder", LeakingOxygenCylinderBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Shorted bench DC power supply — see {@link ShortedBenchSupplyBlock}. */
    public static final DeferredBlock<ShortedBenchSupplyBlock> SHORTED_BENCH_SUPPLY = BLOCKS.registerBlock(
            "shorted_bench_supply", ShortedBenchSupplyBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Vacuum pump running on degraded oil, belt slipping — see {@link OverheatedVacuumPumpBlock}. */
    public static final DeferredBlock<OverheatedVacuumPumpBlock> OVERHEATED_VACUUM_PUMP = BLOCKS.registerBlock(
            "overheated_vacuum_pump", OverheatedVacuumPumpBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Environmental chamber with a welded heater relay — see {@link StuckEnvironmentChamberBlock}. */
    public static final DeferredBlock<StuckEnvironmentChamberBlock> STUCK_ENVIRONMENT_CHAMBER = BLOCKS.registerBlock(
            "stuck_environment_chamber", StuckEnvironmentChamberBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Drying oven with flammable solvent vapor accumulating — see {@link SolventDryingOvenBlock}. */
    public static final DeferredBlock<SolventDryingOvenBlock> SOLVENT_DRYING_OVEN = BLOCKS.registerBlock(
            "solvent_drying_oven", SolventDryingOvenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Dehumidifier with an iced coil, compressor short-cycling — see {@link FaultyDehumidifierBlock}. */
    public static final DeferredBlock<FaultyDehumidifierBlock> FAULTY_DEHUMIDIFIER = BLOCKS.registerBlock(
            "faulty_dehumidifier", FaultyDehumidifierBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
