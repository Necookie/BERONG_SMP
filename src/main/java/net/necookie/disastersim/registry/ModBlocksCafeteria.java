package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BeverageJuiceDispenserBlock;
import net.necookie.disastersim.block.CafeteriaMenuBoardBlock;
import net.necookie.disastersim.block.CafeteriaStoolBlock;
import net.necookie.disastersim.block.CafeteriaTableBlock;
import net.necookie.disastersim.block.CafeteriaTrashBinBlock;
import net.necookie.disastersim.block.CondimentStationBlock;
import net.necookie.disastersim.block.CutleryNapkinCaddyBlock;
import net.necookie.disastersim.block.DishwashingSinkStationBlock;
import net.necookie.disastersim.block.GlowingOakPlanksBlock;
import net.necookie.disastersim.block.KitchenPrepCounterBlock;
import net.necookie.disastersim.block.SaladBarBlock;
import net.necookie.disastersim.block.ServingCounterBlock;
import net.necookie.disastersim.block.ServingHatchWindowBlock;
import net.necookie.disastersim.block.SnackVendingMachineBlock;
import net.necookie.disastersim.block.SodaFountainMachineBlock;
import net.necookie.disastersim.block.TrayStackBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Cafeteria block registrations — split out of {@link ModBlocks} (see its class javadoc).
 * Package-private: {@link ModBlocks} re-exports every field below in the same declared order.
 */
final class ModBlocksCafeteria {

    private ModBlocksCafeteria() {}

    /** Long cafeteria lunch table with attached bench seats — see {@link CafeteriaTableBlock}. */
    static final DeferredBlock<CafeteriaTableBlock> CAFETERIA_TABLE = ModBlocks.BLOCKS.registerBlock(
            "cafeteria_table", CafeteriaTableBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion());

    /** Stack of plastic lunch trays — see {@link TrayStackBlock}. */
    static final DeferredBlock<TrayStackBlock> TRAY_STACK = ModBlocks.BLOCKS.registerBlock(
            "cafeteria_tray_stack", TrayStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f, 1.0f).sound(SoundType.WOOD).noOcclusion());

    /** Steam-table serving counter with sneeze guard — see {@link ServingCounterBlock}. */
    static final DeferredBlock<ServingCounterBlock> SERVING_COUNTER = ModBlocks.BLOCKS.registerBlock(
            "serving_counter", ServingCounterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Wall-mounted cafeteria menu board — see {@link CafeteriaMenuBoardBlock}. */
    static final DeferredBlock<CafeteriaMenuBoardBlock> CAFETERIA_MENU_BOARD = ModBlocks.BLOCKS.registerBlock(
            "cafeteria_menu_board", CafeteriaMenuBoardBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 2.0f).sound(SoundType.WOOD).noOcclusion());

    /** Condiment station with squeeze bottles and napkin holder — see {@link CondimentStationBlock}. */
    static final DeferredBlock<CondimentStationBlock> CONDIMENT_STATION = ModBlocks.BLOCKS.registerBlock(
            "condiment_station", CondimentStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Dual recycle/trash bin station — see {@link CafeteriaTrashBinBlock}. */
    static final DeferredBlock<CafeteriaTrashBinBlock> CAFETERIA_TRASH_BIN = ModBlocks.BLOCKS.registerBlock(
            "cafeteria_trash_bin", CafeteriaTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.5f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Soda fountain dispenser machine — see {@link SodaFountainMachineBlock}. */
    static final DeferredBlock<SodaFountainMachineBlock> SODA_FOUNTAIN_MACHINE = ModBlocks.BLOCKS.registerBlock(
            "soda_fountain_machine", SodaFountainMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Round vinyl-seat cafeteria stool — see {@link CafeteriaStoolBlock}. */
    static final DeferredBlock<CafeteriaStoolBlock> CAFETERIA_STOOL = ModBlocks.BLOCKS.registerBlock(
            "cafeteria_stool", CafeteriaStoolBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Refrigerated salad bar with sneeze guard — see {@link SaladBarBlock}. */
    static final DeferredBlock<SaladBarBlock> SALAD_BAR = ModBlocks.BLOCKS.registerBlock(
            "salad_bar", SaladBarBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Glass-front snack vending machine — see {@link SnackVendingMachineBlock}. */
    static final DeferredBlock<SnackVendingMachineBlock> SNACK_VENDING_MACHINE = ModBlocks.BLOCKS.registerBlock(
            "snack_vending_machine", SnackVendingMachineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(SnackVendingMachineBlock.CONNECTED_DOWN) ? 4 : 0));

    /** Kitchen Prep Counter — see {@link KitchenPrepCounterBlock}. */
    static final DeferredBlock<KitchenPrepCounterBlock> KITCHEN_PREP_COUNTER = ModBlocks.BLOCKS.registerBlock(
            "kitchen_prep_counter", KitchenPrepCounterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Dishwashing Sink Station — see {@link DishwashingSinkStationBlock}. */
    static final DeferredBlock<DishwashingSinkStationBlock> DISHWASHING_SINK_STATION = ModBlocks.BLOCKS.registerBlock(
            "dishwashing_sink_station", DishwashingSinkStationBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Iced-Tea / Juice Dispenser — see {@link BeverageJuiceDispenserBlock}. */
    static final DeferredBlock<BeverageJuiceDispenserBlock> BEVERAGE_JUICE_DISPENSER = ModBlocks.BLOCKS.registerBlock(
            "beverage_juice_dispenser", BeverageJuiceDispenserBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion());

    /** Cutlery & Napkin Caddy — see {@link CutleryNapkinCaddyBlock}. */
    static final DeferredBlock<CutleryNapkinCaddyBlock> CUTLERY_NAPKIN_CADDY = ModBlocks.BLOCKS.registerBlock(
            "cutlery_napkin_caddy", CutleryNapkinCaddyBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(0.5f, 1.0f).sound(SoundType.METAL).noOcclusion());

    /** Serving Hatch Window — see {@link ServingHatchWindowBlock}. */
    static final DeferredBlock<ServingHatchWindowBlock> SERVING_HATCH_WINDOW = ModBlocks.BLOCKS.registerBlock(
            "serving_hatch_window", ServingHatchWindowBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Disguised light source — looks/sounds/burns exactly like vanilla oak planks — see {@link GlowingOakPlanksBlock}. */
    static final DeferredBlock<GlowingOakPlanksBlock> GLOWING_OAK_PLANKS = ModBlocks.BLOCKS.registerBlock(
            "glowing_oak_planks", GlowingOakPlanksBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f).sound(SoundType.WOOD)
                    .lightLevel(s -> 15));
}
