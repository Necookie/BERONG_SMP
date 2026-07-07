package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.block.BulletinBoardBlock;
import net.necookie.disastersim.block.CeilingFanBlock;
import net.necookie.disastersim.block.ChairBlock;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.block.ComputerTableBlock;
import net.necookie.disastersim.block.DrawersBlock;
import net.necookie.disastersim.block.FilingCabinetBlock;
import net.necookie.disastersim.block.FireAlarmBlock;
import net.necookie.disastersim.block.FireHoseCabinetBlock;
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

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
