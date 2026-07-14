package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BundyTimeClockBlock;
import net.necookie.disastersim.block.CoatRackStandBlock;
import net.necookie.disastersim.block.DocumentTrayStackBlock;
import net.necookie.disastersim.block.MailSortingShelfBlock;
import net.necookie.disastersim.block.OfficeCubiclePartitionBlock;
import net.necookie.disastersim.block.OfficePhotocopierBlock;
import net.necookie.disastersim.block.OfficeSafeBlock;
import net.necookie.disastersim.block.OfficeSupplyCabinetBlock;
import net.necookie.disastersim.block.ReceptionDeskBlock;
import net.necookie.disastersim.block.WallBinderShelfBlock;
import net.necookie.disastersim.common.hazard.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Office furniture + hazard prop registrations (props 66-75) — split out of {@link ModBlocks}
 * (see its class javadoc). Package-private: {@link ModBlocks} re-exports every field below in the
 * same declared order. Furniture and hazards are interleaved in a single file because that's how
 * the original section was ordered.
 */
final class ModBlocksOffice {

    private ModBlocksOffice() {}

    // ── Office furniture ─────────────────────────────────────────────────

    /** Fabric-covered cubicle divider panel — see {@link OfficeCubiclePartitionBlock}. */
    static final DeferredBlock<OfficeCubiclePartitionBlock> OFFICE_CUBICLE_PARTITION = ModBlocks.BLOCKS.registerBlock(
            "office_cubicle_partition", OfficeCubiclePartitionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Tall front-counter reception desk — see {@link ReceptionDeskBlock}. */
    static final DeferredBlock<ReceptionDeskBlock> RECEPTION_DESK = ModBlocks.BLOCKS.registerBlock(
            "reception_desk", ReceptionDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall pigeonhole mail-slot unit — see {@link MailSortingShelfBlock}. */
    static final DeferredBlock<MailSortingShelfBlock> MAIL_SORTING_SHELF = ModBlocks.BLOCKS.registerBlock(
            "mail_sorting_shelf", MailSortingShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Large floor office copier — see {@link OfficePhotocopierBlock}. */
    static final DeferredBlock<OfficePhotocopierBlock> OFFICE_PHOTOCOPIER = ModBlocks.BLOCKS.registerBlock(
            "office_photocopier", OfficePhotocopierBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Desktop stack of letter trays — see {@link DocumentTrayStackBlock}. */
    static final DeferredBlock<DocumentTrayStackBlock> DOCUMENT_TRAY_STACK = ModBlocks.BLOCKS.registerBlock(
            "document_tray_stack", DocumentTrayStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall shelf of colored ring binders — see {@link WallBinderShelfBlock}. */
    static final DeferredBlock<WallBinderShelfBlock> WALL_BINDER_SHELF = ModBlocks.BLOCKS.registerBlock(
            "wall_binder_shelf", WallBinderShelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Squat steel safe with a dial — see {@link OfficeSafeBlock}. */
    static final DeferredBlock<OfficeSafeBlock> OFFICE_SAFE = ModBlocks.BLOCKS.registerBlock(
            "office_safe", OfficeSafeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 8.0f).sound(SoundType.METAL).noOcclusion());

    /** Standing wooden coat rack — see {@link CoatRackStandBlock}. */
    static final DeferredBlock<CoatRackStandBlock> COAT_RACK_STAND = ModBlocks.BLOCKS.registerBlock(
            "coat_rack_stand", CoatRackStandBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall punch clock with a timecard rack — see {@link BundyTimeClockBlock}. */
    static final DeferredBlock<BundyTimeClockBlock> BUNDY_TIME_CLOCK = ModBlocks.BLOCKS.registerBlock(
            "bundy_time_clock", BundyTimeClockBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Tall stationery cabinet, doors ajar — see {@link OfficeSupplyCabinetBlock}. */
    static final DeferredBlock<OfficeSupplyCabinetBlock> OFFICE_SUPPLY_CABINET = ModBlocks.BLOCKS.registerBlock(
            "office_supply_cabinet", OfficeSupplyCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    // ── Office hazards (props 66-75) ────────────────────────────────────────

    /** Jammed, overfed paper shredder — see {@link JammedPaperShredderBlock}. */
    static final DeferredBlock<JammedPaperShredderBlock> JAMMED_PAPER_SHREDDER = ModBlocks.BLOCKS.registerBlock(
            "jammed_paper_shredder", JammedPaperShredderBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Clogged-fan comms cabinet overheating — see {@link OverheatedNetworkCabinetBlock}. */
    static final DeferredBlock<OverheatedNetworkCabinetBlock> OVERHEATED_NETWORK_CABINET = ModBlocks.BLOCKS.registerBlock(
            "overheated_network_cabinet", OverheatedNetworkCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** E-bike thermal-runaway charging station — data-driven via {@link HazardSpecs#EBIKE_CHARGING_STATION} (see {@link SimpleHazardFacingBlock}). */
    static final DeferredBlock<SimpleHazardFacingBlock> EBIKE_CHARGING_STATION = ModBlocks.BLOCKS.registerBlock(
            "ebike_charging_station", p -> new SimpleHazardFacingBlock(p, HazardSpecs.EBIKE_CHARGING_STATION),
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Failing fluorescent ballast dripping tar — see {@link FailingFluorescentBallastBlock}. */
    static final DeferredBlock<FailingFluorescentBallastBlock> FAILING_FLUORESCENT_BALLAST = ModBlocks.BLOCKS.registerBlock(
            "failing_fluorescent_ballast", FailingFluorescentBallastBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 8));

    /** Aquarium heater exposed by a dropped water level — data-driven via {@link HazardSpecs#DRY_AQUARIUM_HEATER} (see {@link SimpleHazardFacingBlock}). */
    static final DeferredBlock<SimpleHazardFacingBlock> DRY_AQUARIUM_HEATER = ModBlocks.BLOCKS.registerBlock(
            "dry_aquarium_heater", p -> new SimpleHazardFacingBlock(p, HazardSpecs.DRY_AQUARIUM_HEATER),
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Desk mug warmer buried under a memo pile — see {@link UnattendedMugWarmerBlock}. */
    static final DeferredBlock<UnattendedMugWarmerBlock> UNATTENDED_MUG_WARMER = ModBlocks.BLOCKS.registerBlock(
            "unattended_mug_warmer", UnattendedMugWarmerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 2 : 0));

    /** Old CRT monitor arcing through years of dust — data-driven via {@link HazardSpecs#DUSTY_CRT_MONITOR} (see {@link SimpleHazardFacingBlock}). */
    static final DeferredBlock<SimpleHazardFacingBlock> DUSTY_CRT_MONITOR = ModBlocks.BLOCKS.registerBlock(
            "dusty_crt_monitor", p -> new SimpleHazardFacingBlock(p, HazardSpecs.DUSTY_CRT_MONITOR),
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Baseboard wiring gnawed to bare copper — data-driven via {@link HazardSpecs#RODENT_CHEWED_WIRING} (see {@link SimpleHazardFacingBlock}). */
    static final DeferredBlock<SimpleHazardFacingBlock> RODENT_CHEWED_WIRING = ModBlocks.BLOCKS.registerBlock(
            "rodent_chewed_wiring", p -> new SimpleHazardFacingBlock(p, HazardSpecs.RODENT_CHEWED_WIRING),
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());

    /** 24/7 security DVR with blocked vents — see {@link OverheatingCctvDvrBlock}. */
    static final DeferredBlock<OverheatingCctvDvrBlock> OVERHEATING_CCTV_DVR = ModBlocks.BLOCKS.registerBlock(
            "overheating_cctv_dvr", OverheatingCctvDvrBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Faulty parol lantern with substandard series lights — see {@link FaultyParolLanternBlock}. */
    static final DeferredBlock<FaultyParolLanternBlock> FAULTY_PAROL_LANTERN = ModBlocks.BLOCKS.registerBlock(
            "faulty_parol_lantern", FaultyParolLanternBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 2));
}
