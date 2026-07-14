package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.ArmchairDeskBlock;
import net.necookie.disastersim.block.BlackboardBlock;
import net.necookie.disastersim.block.ClassroomGlobeBlock;
import net.necookie.disastersim.block.ComputerLabDeskRowBlock;
import net.necookie.disastersim.block.LibraryStudyCarrelBlock;
import net.necookie.disastersim.block.ModernStudentDeskBlock;
import net.necookie.disastersim.block.PhilippineFlagStandBlock;
import net.necookie.disastersim.block.PodiumLecternBlock;
import net.necookie.disastersim.block.RollingBookCartBlock;
import net.necookie.disastersim.block.ScienceLabWorkbenchBlock;
import net.necookie.disastersim.block.TallBookshelfBlock;
import net.necookie.disastersim.block.TeachersDeskBlock;
import net.necookie.disastersim.block.TrophyCabinetBlock;
import net.necookie.disastersim.block.WallClockBlock;
import net.necookie.disastersim.block.WaterDispenserBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * School decor/furniture block registrations — split out of {@link ModBlocks} (see its class
 * javadoc). Package-private: {@link ModBlocks} re-exports every field below in the same declared
 * order.
 */
final class ModBlocksSchoolDecor {

    private ModBlocksSchoolDecor() {}

    /** Teacher's Desk — see {@link TeachersDeskBlock}. */
    static final DeferredBlock<TeachersDeskBlock> TEACHERS_DESK = ModBlocks.BLOCKS.registerBlock(
            "teachers_desk", TeachersDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Armchair Desk — see {@link ArmchairDeskBlock}. */
    static final DeferredBlock<ArmchairDeskBlock> ARMCHAIR_DESK = ModBlocks.BLOCKS.registerBlock(
            "armchair_desk", ArmchairDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Tall Bookshelf — see {@link TallBookshelfBlock}. */
    static final DeferredBlock<TallBookshelfBlock> TALL_BOOKSHELF = ModBlocks.BLOCKS.registerBlock(
            "tall_bookshelf", TallBookshelfBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Philippine Flag Stand — see {@link PhilippineFlagStandBlock}. */
    static final DeferredBlock<PhilippineFlagStandBlock> PHILIPPINE_FLAG_STAND = ModBlocks.BLOCKS.registerBlock(
            "philippine_flag_stand", PhilippineFlagStandBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.GOLD)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Trophy Cabinet — see {@link TrophyCabinetBlock}. */
    static final DeferredBlock<TrophyCabinetBlock> TROPHY_CABINET = ModBlocks.BLOCKS.registerBlock(
            "trophy_cabinet", TrophyCabinetBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 4.0f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 3));

    /** Water Dispenser — see {@link WaterDispenserBlock}. */
    static final DeferredBlock<WaterDispenserBlock> WATER_DISPENSER = ModBlocks.BLOCKS.registerBlock(
            "water_dispenser", WaterDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall Clock — see {@link WallClockBlock}. */
    static final DeferredBlock<WallClockBlock> WALL_CLOCK = ModBlocks.BLOCKS.registerBlock(
            "wall_clock", WallClockBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Blackboard — see {@link BlackboardBlock}. */
    static final DeferredBlock<BlackboardBlock> BLACKBOARD = ModBlocks.BLOCKS.registerBlock(
            "blackboard", BlackboardBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Podium Lectern — see {@link PodiumLecternBlock}. */
    static final DeferredBlock<PodiumLecternBlock> PODIUM_LECTERN = ModBlocks.BLOCKS.registerBlock(
            "podium_lectern", PodiumLecternBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Classroom Globe — see {@link ClassroomGlobeBlock}. */
    static final DeferredBlock<ClassroomGlobeBlock> CLASSROOM_GLOBE = ModBlocks.BLOCKS.registerBlock(
            "classroom_globe", ClassroomGlobeBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Modern Single-Seat Student Desk — see {@link ModernStudentDeskBlock}. */
    static final DeferredBlock<ModernStudentDeskBlock> MODERN_STUDENT_DESK = ModBlocks.BLOCKS.registerBlock(
            "modern_student_desk", ModernStudentDeskBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Science Lab Workbench — see {@link ScienceLabWorkbenchBlock}. */
    static final DeferredBlock<ScienceLabWorkbenchBlock> SCIENCE_LAB_WORKBENCH = ModBlocks.BLOCKS.registerBlock(
            "science_lab_workbench", ScienceLabWorkbenchBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.STONE).noOcclusion());

    /** Computer Lab Desk Row — see {@link ComputerLabDeskRowBlock}. */
    static final DeferredBlock<ComputerLabDeskRowBlock> COMPUTER_LAB_DESK_ROW = ModBlocks.BLOCKS.registerBlock(
            "computer_lab_desk_row", ComputerLabDeskRowBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Library Study Carrel — see {@link LibraryStudyCarrelBlock}. */
    static final DeferredBlock<LibraryStudyCarrelBlock> LIBRARY_STUDY_CARREL = ModBlocks.BLOCKS.registerBlock(
            "library_study_carrel", LibraryStudyCarrelBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Rolling Book Cart — see {@link RollingBookCartBlock}. */
    static final DeferredBlock<RollingBookCartBlock> ROLLING_BOOK_CART = ModBlocks.BLOCKS.registerBlock(
            "rolling_book_cart", RollingBookCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f).sound(SoundType.WOOD).noOcclusion());
}
