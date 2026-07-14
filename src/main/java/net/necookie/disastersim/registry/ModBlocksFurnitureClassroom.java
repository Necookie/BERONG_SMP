package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BulletinBoardBlock;
import net.necookie.disastersim.block.CeilingFanBlock;
import net.necookie.disastersim.block.ChairBlock;
import net.necookie.disastersim.block.ComputerTableBlock;
import net.necookie.disastersim.block.DrawersBlock;
import net.necookie.disastersim.block.FilingCabinetBlock;
import net.necookie.disastersim.block.FireHoseCabinetBlock;
import net.necookie.disastersim.block.LightBulbBlock;
import net.necookie.disastersim.block.LockerBlock;
import net.necookie.disastersim.block.SinkBlock;
import net.necookie.disastersim.block.TableBlock;
import net.necookie.disastersim.block.ToiletBlock;
import net.necookie.disastersim.block.TrashCanBlock;
import net.necookie.disastersim.block.WhiteboardBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Classroom/office furniture block registrations — split out of {@link ModBlocks} (see its
 * class javadoc) purely to keep that file from being one 1300-line monolith. Package-private:
 * {@link ModBlocks} re-exports every field below in the same declared order.
 */
final class ModBlocksFurnitureClassroom {

    private ModBlocksFurnitureClassroom() {}

    /** Classroom whiteboard — wall-mounted flat panel with marker tray. */
    static final DeferredBlock<WhiteboardBlock> WHITEBOARD = ModBlocks.BLOCKS.registerBlock("whiteboard",
            WhiteboardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Wall-mounted fire hose reel cabinet — decorative safety-equipment prop, not a hazard. */
    static final DeferredBlock<FireHoseCabinetBlock> FIRE_HOSE_CABINET = ModBlocks.BLOCKS.registerBlock("fire_hose_cabinet",
            FireHoseCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Toilet block — ceramic basin+tank; right-click to flush. */
    static final DeferredBlock<ToiletBlock> TOILET = ModBlocks.BLOCKS.registerBlock("toilet",
            ToiletBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Wall-mounted sink with iron faucet and handles; right-click for water sound. */
    static final DeferredBlock<SinkBlock> SINK = ModBlocks.BLOCKS.registerBlock("sink",
            SinkBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /** Office chest-of-drawers — dark oak body with birch drawer fronts and iron handles. */
    static final DeferredBlock<DrawersBlock> DRAWERS = ModBlocks.BLOCKS.registerBlock("drawers",
            DrawersBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Flammable oak computer desk with 4 legs and a back cable-management panel. */
    static final DeferredBlock<ComputerTableBlock> COMPUTER_TABLE = ModBlocks.BLOCKS.registerBlock("computer_table",
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
    static final DeferredBlock<TableBlock> TABLE = ModBlocks.BLOCKS.registerBlock("table",
            TableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Dark oak classroom/office chair with gray cushion seat and backrest. */
    static final DeferredBlock<ChairBlock> CHAIR = ModBlocks.BLOCKS.registerBlock("chair",
            ChairBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Tall metal filing cabinet with 2 drawers, label slots, and pull handles. */
    static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = ModBlocks.BLOCKS.registerBlock("filing_cabinet",
            FilingCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Tall metal school/office locker with vents, door seam, handle, and gold lock. */
    static final DeferredBlock<LockerBlock> LOCKER = ModBlocks.BLOCKS.registerBlock("locker",
            LockerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Small cylindrical trash can — no facing, symmetric, open-top. */
    static final DeferredBlock<TrashCanBlock> TRASH_CAN = ModBlocks.BLOCKS.registerBlock("trash_can",
            TrashCanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Wall-mounted cork bulletin board with pinned paper slips. */
    static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = ModBlocks.BLOCKS.registerBlock("bulletin_board",
            BulletinBoardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** Ceiling fan — motor housing + 4 blades + glowstone light bowl; symmetric/no facing. */
    static final DeferredBlock<CeilingFanBlock> CEILING_FAN = ModBlocks.BLOCKS.registerBlock("ceiling_fan",
            CeilingFanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(s -> 5));

    /** Full-cube glowing ceiling tile — max-brightness (level 15, vanilla cap) light source; tiles seamlessly with no gaps or visible seams when placed edge-to-edge, like a lit ceiling carpet. */
    static final DeferredBlock<LightBulbBlock> LIGHT_BULB = ModBlocks.BLOCKS.registerBlock("light_bulb",
            LightBulbBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .lightLevel(s -> 15));
}
