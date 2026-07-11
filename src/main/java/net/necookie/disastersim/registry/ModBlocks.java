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

    /** Classroom whiteboard — wall-mounted flat panel with marker tray. */
    public static final DeferredBlock<WhiteboardBlock> WHITEBOARD = BLOCKS.registerBlock("whiteboard",
            WhiteboardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Wall-mounted fire hose reel cabinet — decorative safety-equipment prop, not a hazard. */
    public static final DeferredBlock<FireHoseCabinetBlock> FIRE_HOSE_CABINET = BLOCKS.registerBlock("fire_hose_cabinet",
            FireHoseCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Toilet block — ceramic basin+tank; right-click to flush. */
    public static final DeferredBlock<ToiletBlock> TOILET = BLOCKS.registerBlock("toilet",
            ToiletBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Wall-mounted sink with iron faucet and handles; right-click for water sound. */
    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink",
            SinkBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Office chest-of-drawers — dark oak body with birch drawer fronts and iron handles. */
    public static final DeferredBlock<DrawersBlock> DRAWERS = BLOCKS.registerBlock("drawers",
            DrawersBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Flammable oak computer desk with 4 legs and a back cable-management panel. */
    public static final DeferredBlock<ComputerTableBlock> COMPUTER_TABLE = BLOCKS.registerBlock("computer_table",
            ComputerTableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /**
     * Extendable study/library table, 2 blocks tall (HALF=LOWER/UPPER like a vanilla tall
     * flower) with a walkable kneehole underneath — the first placeable prop that gives players
     * a real "duck and cover" shelter (see {@code DuckCoverHoldManager}). Lining tables up
     * side by side (NORTH/SOUTH/EAST/WEST connections) merges them into one continuous tabletop.
     */
    public static final DeferredBlock<TableBlock> TABLE = BLOCKS.registerBlock("table",
            TableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Dark oak classroom/office chair with gray cushion seat and backrest. */
    public static final DeferredBlock<ChairBlock> CHAIR = BLOCKS.registerBlock("chair",
            ChairBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Tall metal filing cabinet with 2 drawers, label slots, and pull handles. */
    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = BLOCKS.registerBlock("filing_cabinet",
            FilingCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Tall metal school/office locker with vents, door seam, handle, and gold lock. */
    public static final DeferredBlock<LockerBlock> LOCKER = BLOCKS.registerBlock("locker",
            LockerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Small cylindrical trash can — no facing, symmetric, open-top. */
    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = BLOCKS.registerBlock("trash_can",
            TrashCanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Wall-mounted cork bulletin board with pinned paper slips. */
    public static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = BLOCKS.registerBlock("bulletin_board",
            BulletinBoardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Ceiling fan — motor housing + 4 blades + glowstone light bowl; symmetric/no facing. */
    public static final DeferredBlock<CeilingFanBlock> CEILING_FAN = BLOCKS.registerBlock("ceiling_fan",
            CeilingFanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(s -> 5));

    /** Full-cube glowing ceiling tile — max-brightness (level 15, vanilla cap) light source; tiles seamlessly with no gaps or visible seams when placed edge-to-edge, like a lit ceiling carpet. */
    public static final DeferredBlock<LightBulbBlock> LIGHT_BULB = BLOCKS.registerBlock("light_bulb",
            LightBulbBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .lightLevel(s -> 15));

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

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
