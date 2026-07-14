package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.ConferenceCredenzaBlock;
import net.necookie.disastersim.block.ConferenceSpeakerphoneBlock;
import net.necookie.disastersim.block.ConferenceTableBlock;
import net.necookie.disastersim.block.ConferenceWallDisplayBlock;
import net.necookie.disastersim.block.ExecutiveOfficeChairBlock;
import net.necookie.disastersim.block.FlipChartEaselBlock;
import net.necookie.disastersim.block.GlassOfficePartitionBlock;
import net.necookie.disastersim.block.LoungeSofaBlock;
import net.necookie.disastersim.block.PottedOfficePlantBlock;
import net.necookie.disastersim.block.WindowBlindsBlock;
import net.necookie.disastersim.common.hazard.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Conference Room furniture + hazard prop registrations (props 56-65) — split out of
 * {@link ModBlocks} (see its class javadoc). Package-private: {@link ModBlocks} re-exports every
 * field below in the same declared order. Furniture and hazards are interleaved in a single file
 * because that's how the original section was ordered — reordering into all-furniture/all-hazard
 * groups would change registration order.
 */
final class ModBlocksConference {

    private ModBlocksConference() {}

    // ── Conference Room furniture ───────────────────────────────────────────

    /** Self-connecting dark-laminate boardroom table — see {@link ConferenceTableBlock}. */
    static final DeferredBlock<ConferenceTableBlock> CONFERENCE_TABLE = ModBlocks.BLOCKS.registerBlock(
            "conference_table", ConferenceTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** High-back leather swivel chair — see {@link ExecutiveOfficeChairBlock}. */
    static final DeferredBlock<ExecutiveOfficeChairBlock> EXECUTIVE_OFFICE_CHAIR = ModBlocks.BLOCKS.registerBlock(
            "executive_office_chair", ExecutiveOfficeChairBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Low walnut sideboard with sliding doors — see {@link ConferenceCredenzaBlock}. */
    static final DeferredBlock<ConferenceCredenzaBlock> CONFERENCE_CREDENZA = ModBlocks.BLOCKS.registerBlock(
            "conference_credenza", ConferenceCredenzaBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0f, 4.0f).sound(SoundType.WOOD).noOcclusion());

    /** Wall-mounted meeting display, dark standby screen — see {@link ConferenceWallDisplayBlock}. */
    static final DeferredBlock<ConferenceWallDisplayBlock> CONFERENCE_WALL_DISPLAY = ModBlocks.BLOCKS.registerBlock(
            "conference_wall_display", ConferenceWallDisplayBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** A-frame flip-chart easel with paper pad — see {@link FlipChartEaselBlock}. */
    static final DeferredBlock<FlipChartEaselBlock> FLIP_CHART_EASEL = ModBlocks.BLOCKS.registerBlock(
            "flip_chart_easel", FlipChartEaselBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Three-legged "spider" speakerphone centerpiece — see {@link ConferenceSpeakerphoneBlock}. */
    static final DeferredBlock<ConferenceSpeakerphoneBlock> CONFERENCE_SPEAKERPHONE = ModBlocks.BLOCKS.registerBlock(
            "conference_speakerphone", ConferenceSpeakerphoneBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Frosted-glass partition panel in an aluminum frame — see {@link GlassOfficePartitionBlock}. */
    static final DeferredBlock<GlassOfficePartitionBlock> GLASS_OFFICE_PARTITION = ModBlocks.BLOCKS.registerBlock(
            "glass_office_partition", GlassOfficePartitionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 2.0f).sound(SoundType.GLASS).noOcclusion());

    /** Two-seat fabric reception sofa — see {@link LoungeSofaBlock}. */
    static final DeferredBlock<LoungeSofaBlock> LOUNGE_SOFA = ModBlocks.BLOCKS.registerBlock(
            "lounge_sofa", LoungeSofaBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOL).noOcclusion());

    /** Decorative floor planter with a broadleaf plant — see {@link PottedOfficePlantBlock}. */
    static final DeferredBlock<PottedOfficePlantBlock> POTTED_OFFICE_PLANT = ModBlocks.BLOCKS.registerBlock(
            "potted_office_plant", PottedOfficePlantBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.PLANT)
                    .strength(0.5f, 1.0f).sound(SoundType.GRASS).noOcclusion());

    /** Wall-mounted venetian blinds, slats half-open — see {@link WindowBlindsBlock}. */
    static final DeferredBlock<WindowBlindsBlock> WINDOW_BLINDS = ModBlocks.BLOCKS.registerBlock(
            "window_blinds", WindowBlindsBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    // ── Conference Room hazards (props 56-65) ───────────────────────────────

    /** Space heater tipped against papers — see {@link PortableSpaceHeaterBlock}. */
    static final DeferredBlock<PortableSpaceHeaterBlock> PORTABLE_SPACE_HEATER = ModBlocks.BLOCKS.registerBlock(
            "portable_space_heater", PortableSpaceHeaterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 6 : 0));

    /** Tipped torchiere scorching a curtain — see {@link HalogenFloorLampBlock}. */
    static final DeferredBlock<HalogenFloorLampBlock> HALOGEN_FLOOR_LAMP = ModBlocks.BLOCKS.registerBlock(
            "halogen_floor_lamp", HalogenFloorLampBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 3));

    /** Stalled ceiling projection-screen motor — see {@link JammedProjectionScreenBlock}. */
    static final DeferredBlock<JammedProjectionScreenBlock> JAMMED_PROJECTION_SCREEN = ModBlocks.BLOCKS.registerBlock(
            "jammed_projection_screen", JammedProjectionScreenBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Overheating driver stack behind an LED video wall — see {@link OverheatingVideoWallBlock}. */
    static final DeferredBlock<OverheatingVideoWallBlock> OVERHEATING_VIDEO_WALL = ModBlocks.BLOCKS.registerBlock(
            "overheating_video_wall", OverheatingVideoWallBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 4 : 0));

    /** Aerosol dispenser misting propellant near a heat register — see {@link AerosolFreshenerDispenserBlock}. */
    static final DeferredBlock<AerosolFreshenerDispenserBlock> AEROSOL_FRESHENER_DISPENSER = ModBlocks.BLOCKS.registerBlock(
            "aerosol_freshener_dispenser", AerosolFreshenerDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Laptop smothered under sofa cushions — see {@link SmotheredLaptopBlock}. */
    static final DeferredBlock<SmotheredLaptopBlock> SMOTHERED_LAPTOP = ModBlocks.BLOCKS.registerBlock(
            "smothered_laptop", SmotheredLaptopBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));

    /** Cigarette smoldering in a planter's peat — see {@link SmolderingPlanterBlock}. */
    static final DeferredBlock<SmolderingPlanterBlock> SMOLDERING_PLANTER = ModBlocks.BLOCKS.registerBlock(
            "smoldering_planter", SmolderingPlanterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.PLANT)
                    .strength(0.5f, 1.0f).sound(SoundType.GRAVEL).noOcclusion());

    /** TV power cord crushed by its wall mount — see {@link PinchedTvCordBlock}. */
    static final DeferredBlock<PinchedTvCordBlock> PINCHED_TV_CORD = ModBlocks.BLOCKS.registerBlock(
            "pinched_tv_cord", PinchedTvCordBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());

    /** Failing UPS battery off-gassing under a credenza — see {@link VentingUpsBatteryBlock}. */
    static final DeferredBlock<VentingUpsBatteryBlock> VENTING_UPS_BATTERY = ModBlocks.BLOCKS.registerBlock(
            "venting_ups_battery", VentingUpsBatteryBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Over-lamped dimmer switch scorching its faceplate — see {@link FaultyDimmerSwitchBlock}. */
    static final DeferredBlock<FaultyDimmerSwitchBlock> FAULTY_DIMMER_SWITCH = ModBlocks.BLOCKS.registerBlock(
            "faulty_dimmer_switch", FaultyDimmerSwitchBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOD).noOcclusion());
}
