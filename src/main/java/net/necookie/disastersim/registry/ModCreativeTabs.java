package net.necookie.disastersim.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's three creative tabs (simulation tools, furniture, hazard props), pulling their
 * contents from {@link ModItems}. Extracted from {@code BerongSMP} so the entry point stays a
 * thin bootstrap; {@link #register(IEventBus)} is called from its constructor.
 */
public final class ModCreativeTabs {

    /** Deferred Register for Creative Mode Tabs. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BerongSMP.MODID);

    /** Creative tab: simulation tools and interactive blocks. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIM_TAB = CREATIVE_MODE_TABS.register("sim_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.simulation"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.FIRE_EXTINGUISHER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.FIRE_EXTINGUISHER.get());
                output.accept(ModItems.CO2_EXTINGUISHER.get());
                output.accept(ModItems.WET_CHEMICAL_EXTINGUISHER.get());
                output.accept(ModItems.HAZARD_WAND.get());
                output.accept(ModItems.FIREFIGHTER_HELMET.get());
                output.accept(ModItems.FIREFIGHTER_COAT.get());
                output.accept(ModItems.FIREFIGHTER_PANTS.get());
                output.accept(ModItems.FIREFIGHTER_BOOTS.get());
                output.accept(ModItems.FLASHLIGHT.get());
                output.accept(ModItems.SAFETY_WHISTLE.get());
                output.accept(ModItems.MEGAPHONE.get());
                output.accept(ModItems.FIRST_AID_KIT.get());
                output.accept(ModItems.FIRE_BLANKET.get());
                output.accept(ModItems.COMPUTER_ITEM.get());
                output.accept(ModItems.FIRE_ALARM_ITEM.get());
                output.accept(ModItems.EVACUATION_MAP_ITEM.get());
                output.accept(ModItems.EMERGENCY_LIGHT_ITEM.get());
                output.accept(ModItems.SPRINKLER_HEAD_ITEM.get());
                output.accept(ModItems.SMOKE_DETECTOR_ITEM.get());
                output.accept(ModItems.EXIT_SIGN_ITEM.get());
                output.accept(ModItems.NPC_SGT_REYES.get());
                output.accept(ModItems.NPC_SGT_SANTOS.get());
                output.accept(ModItems.NPC_OFFICER_CRUZ.get());
                output.accept(ModItems.NPC_CAPT_MORFE.get());
                output.accept(ModItems.NPC_SECURITY_TUAZON.get());
                output.accept(ModItems.NPC_DM_ORLANDA.get());
                output.accept(ModItems.NPC_NECOOKIE.get());
                output.accept(ModItems.NPC_SIR_BOOKMARK.get());
                output.accept(ModItems.NPC_STUDENT.get());
            }).build());

    /** Creative tab: furniture and props for building scenarios. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURN_TAB = CREATIVE_MODE_TABS.register("furn_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.furniture"))
            .withTabsBefore(SIM_TAB.getKey())
            .icon(() -> ModItems.CHAIR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.WHITEBOARD_ITEM.get());
                output.accept(ModItems.FIRE_HOSE_CABINET_ITEM.get());
                output.accept(ModItems.BULLETIN_BOARD_ITEM.get());
                output.accept(ModItems.COMPUTER_TABLE_ITEM.get());
                output.accept(ModItems.TABLE_ITEM.get());
                output.accept(ModItems.CHAIR_ITEM.get());
                output.accept(ModItems.DRAWERS_ITEM.get());
                output.accept(ModItems.FILING_CABINET_ITEM.get());
                output.accept(ModItems.LOCKER_ITEM.get());
                output.accept(ModItems.TOILET_ITEM.get());
                output.accept(ModItems.SINK_ITEM.get());
                output.accept(ModItems.TRASH_CAN_ITEM.get());
                output.accept(ModItems.CEILING_FAN_ITEM.get());
                output.accept(ModItems.LIGHT_BULB_ITEM.get());
                output.accept(ModItems.CLASSROOM_GLOBE_ITEM.get());
                output.accept(ModItems.PODIUM_LECTERN_ITEM.get());
                output.accept(ModItems.BLACKBOARD_ITEM.get());
                output.accept(ModItems.WALL_CLOCK_ITEM.get());
                output.accept(ModItems.WATER_DISPENSER_ITEM.get());
                output.accept(ModItems.TROPHY_CABINET_ITEM.get());
                output.accept(ModItems.PHILIPPINE_FLAG_STAND_ITEM.get());
                output.accept(ModItems.TALL_BOOKSHELF_ITEM.get());
                output.accept(ModItems.ARMCHAIR_DESK_ITEM.get());
                output.accept(ModItems.TEACHERS_DESK_ITEM.get());
                output.accept(ModItems.CAFETERIA_TABLE_ITEM.get());
                output.accept(ModItems.CAFETERIA_STOOL_ITEM.get());
                output.accept(ModItems.TRAY_STACK_ITEM.get());
                output.accept(ModItems.SERVING_COUNTER_ITEM.get());
                output.accept(ModItems.SALAD_BAR_ITEM.get());
                output.accept(ModItems.CONDIMENT_STATION_ITEM.get());
                output.accept(ModItems.SODA_FOUNTAIN_MACHINE_ITEM.get());
                output.accept(ModItems.SNACK_VENDING_MACHINE_ITEM.get());
                output.accept(ModItems.CAFETERIA_MENU_BOARD_ITEM.get());
                output.accept(ModItems.CAFETERIA_TRASH_BIN_ITEM.get());
            }).build());

    /** Creative tab: all 30 hazard prop blocks for the simulation building. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAZARD_TAB = CREATIVE_MODE_TABS.register("hazards_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.hazards"))
            .withTabsBefore(FURN_TAB.getKey())
            .icon(() -> ModItems.DAISY_CHAIN_EXTENSION_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.HAZARD_ITEM_MAP.values().forEach(i -> output.accept(i.get())))
            .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
