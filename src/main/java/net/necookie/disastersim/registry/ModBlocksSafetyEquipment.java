package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.AssemblyPointSignBlock;
import net.necookie.disastersim.block.BlockedExitClutterBlock;
import net.necookie.disastersim.block.EmergencyLightBlock;
import net.necookie.disastersim.block.EvacuationMapBlock;
import net.necookie.disastersim.block.ExitSignBlock;
import net.necookie.disastersim.block.FireExtinguisherCabinetBlock;
import net.necookie.disastersim.block.FireSafetyPosterBlock;
import net.necookie.disastersim.block.FirstAidWallCabinetBlock;
import net.necookie.disastersim.block.SmokeDetectorBlock;
import net.necookie.disastersim.block.SprinklerHeadBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Safety-equipment block registrations — split out of {@link ModBlocks} (see its class javadoc).
 * Package-private: {@link ModBlocks} re-exports every field below in the same declared order.
 */
final class ModBlocksSafetyEquipment {

    private ModBlocksSafetyEquipment() {}

    /** Emergency Exit Sign — see {@link ExitSignBlock}. */
    static final DeferredBlock<ExitSignBlock> EXIT_SIGN = ModBlocks.BLOCKS.registerBlock(
            "exit_sign", ExitSignBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.EMERALD)
                    .strength(0.5f, 2.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 7));

    /** Smoke Detector — see {@link SmokeDetectorBlock}. */
    static final DeferredBlock<SmokeDetectorBlock> SMOKE_DETECTOR = ModBlocks.BLOCKS.registerBlock(
            "smoke_detector", SmokeDetectorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Sprinkler Head — see {@link SprinklerHeadBlock}. */
    static final DeferredBlock<SprinklerHeadBlock> SPRINKLER_HEAD = ModBlocks.BLOCKS.registerBlock(
            "sprinkler_head", SprinklerHeadBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.GOLD)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Emergency Light — see {@link EmergencyLightBlock}. */
    static final DeferredBlock<EmergencyLightBlock> EMERGENCY_LIGHT = ModBlocks.BLOCKS.registerBlock(
            "emergency_light", EmergencyLightBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0));

    /** Evacuation Map — see {@link EvacuationMapBlock}. */
    static final DeferredBlock<EvacuationMapBlock> EVACUATION_MAP = ModBlocks.BLOCKS.registerBlock(
            "evacuation_map", EvacuationMapBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Fire Extinguisher Cabinet — see {@link FireExtinguisherCabinetBlock}. */
    static final DeferredBlock<FireExtinguisherCabinetBlock> FIRE_EXTINGUISHER_CABINET = ModBlocks.BLOCKS.registerBlock(
            "fire_extinguisher_cabinet", FireExtinguisherCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Evacuation Assembly Point Sign — see {@link AssemblyPointSignBlock}. */
    static final DeferredBlock<AssemblyPointSignBlock> ASSEMBLY_POINT_SIGN = ModBlocks.BLOCKS.registerBlock(
            "assembly_point_sign", AssemblyPointSignBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.EMERALD)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s -> 7));

    /** First-Aid Wall Cabinet — see {@link FirstAidWallCabinetBlock}. */
    static final DeferredBlock<FirstAidWallCabinetBlock> FIRST_AID_WALL_CABINET = ModBlocks.BLOCKS.registerBlock(
            "first_aid_wall_cabinet", FirstAidWallCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Fire Safety Poster — see {@link FireSafetyPosterBlock}. */
    static final DeferredBlock<FireSafetyPosterBlock> FIRE_SAFETY_POSTER = ModBlocks.BLOCKS.registerBlock(
            "fire_safety_poster", FireSafetyPosterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.2f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Blocked Exit Clutter Pile — see {@link BlockedExitClutterBlock}. */
    static final DeferredBlock<BlockedExitClutterBlock> BLOCKED_EXIT_CLUTTER = ModBlocks.BLOCKS.registerBlock(
            "blocked_exit_clutter", BlockedExitClutterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());
}
