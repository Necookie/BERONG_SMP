package net.necookie.disastersim.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.NpcType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's creative tabs, pulling their contents from {@link ModItems}. Extracted from
 * {@code BerongSMP} so the entry point stays a thin bootstrap; {@link #register(IEventBus)} is
 * called from its constructor.
 *
 * <p><b>Hard cap: at most 5 mod tabs.</b> NeoForge 26.1.2.36-beta's
 * {@code CreativeModeInventoryScreen} (patched) paginates the sorted tab list into pages of
 * exactly 10 ({@code if (tabIndex == 10)}), and {@code CreativeTabsScreenPage} splits each page
 * 5-top-row/5-bottom-row ({@code maxLength = 10; topLength = maxLength / 2}). Vanilla registers
 * exactly 10 non-pinned tabs, so they alone fill page 1 — every mod tab therefore always lands on
 * page 2, starting at its top row. A first attempt (2026-07-12) split into 7 sub-tabs (4 hazard +
 * 3 furniture, 9 mod tabs total with SIM_TAB/NPC_TAB): the first 5 landed in page 2's top row and
 * worked, but the remaining 4 spilled into page 2's bottom row and were not visible/reachable in
 * practice. Fixed same-day by consolidating back down to exactly 5 mod tabs — {@link #SIM_TAB},
 * {@link #FURN_TAB}, {@link #HAZARD_SCHOOL_TAB}, {@link #HAZARD_OFFICE_LAB_TAB}, {@link #NPC_TAB}
 * — so all 5 fill page 2's top row exactly and none fall into the broken bottom row. Verify any
 * future tab-count change in-game (open creative, page to page 2 with the {@code >} button, and
 * confirm every mod tab actually renders) before assuming more tabs are safe.
 */
public final class ModCreativeTabs {

    static {
        ModItems.assertHazardZonesCoverMap();
    }

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
                output.accept(ModItems.FIRE_HOSE_CABINET_ITEM.get());
                output.accept(ModItems.COMPUTER_ITEM.get());
                output.accept(ModItems.FIRE_ALARM_ITEM.get());
                output.accept(ModItems.EVACUATION_MAP_ITEM.get());
                output.accept(ModItems.EMERGENCY_LIGHT_ITEM.get());
                output.accept(ModItems.SPRINKLER_HEAD_ITEM.get());
                output.accept(ModItems.SMOKE_DETECTOR_ITEM.get());
                output.accept(ModItems.EXIT_SIGN_ITEM.get());
                output.accept(ModItems.FIRE_EXTINGUISHER_CABINET_ITEM.get());
                output.accept(ModItems.ASSEMBLY_POINT_SIGN_ITEM.get());
                output.accept(ModItems.FIRST_AID_WALL_CABINET_ITEM.get());
                output.accept(ModItems.FIRE_SAFETY_POSTER_ITEM.get());
            }).build());

    /** Creative tab: all furniture — school/classroom, cafeteria, and Conference Room/Office/Laboratory. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURN_TAB = CREATIVE_MODE_TABS.register("furn_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.furniture"))
            .withTabsBefore(SIM_TAB.getKey())
            .icon(() -> ModItems.CHAIR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Classroom & School
                output.accept(ModItems.WHITEBOARD_ITEM.get());
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
                output.accept(ModItems.GLOWING_OAK_PLANKS_ITEM.get());
                output.accept(ModItems.COURT_LINE_ITEM.get());
                output.accept(ModItems.BADMINTON_NET_POST_ITEM.get());
                output.accept(ModItems.BADMINTON_NET_MESH_ITEM.get());
                output.accept(ModItems.BASKETBALL_HOOP_POST_ITEM.get());
                output.accept(ModItems.BASKETBALL_POLE_ITEM.get());
                output.accept(ModItems.BASKETBALL_HOOP_ITEM.get());
                output.accept(ModItems.BASKETBALL.get());
                output.accept(ModItems.CLASSROOM_GLOBE_ITEM.get());
                output.accept(ModItems.MODERN_STUDENT_DESK_ITEM.get());
                output.accept(ModItems.COMPUTER_LAB_DESK_ROW_ITEM.get());
                output.accept(ModItems.LIBRARY_STUDY_CARREL_ITEM.get());
                output.accept(ModItems.ROLLING_BOOK_CART_ITEM.get());
                output.accept(ModItems.PODIUM_LECTERN_ITEM.get());
                output.accept(ModItems.BLACKBOARD_ITEM.get());
                output.accept(ModItems.WALL_CLOCK_ITEM.get());
                output.accept(ModItems.WATER_DISPENSER_ITEM.get());
                output.accept(ModItems.TROPHY_CABINET_ITEM.get());
                output.accept(ModItems.PHILIPPINE_FLAG_STAND_ITEM.get());
                output.accept(ModItems.TALL_BOOKSHELF_ITEM.get());
                output.accept(ModItems.ARMCHAIR_DESK_ITEM.get());
                output.accept(ModItems.TEACHERS_DESK_ITEM.get());
                // Cafeteria
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
                output.accept(ModItems.KITCHEN_PREP_COUNTER_ITEM.get());
                output.accept(ModItems.DISHWASHING_SINK_STATION_ITEM.get());
                output.accept(ModItems.BEVERAGE_JUICE_DISPENSER_ITEM.get());
                output.accept(ModItems.CUTLERY_NAPKIN_CADDY_ITEM.get());
                output.accept(ModItems.SERVING_HATCH_WINDOW_ITEM.get());
                output.accept(ModItems.BLOCKED_EXIT_CLUTTER_ITEM.get());
                // Conference Room
                output.accept(ModItems.CONFERENCE_TABLE_ITEM.get());
                output.accept(ModItems.EXECUTIVE_OFFICE_CHAIR_ITEM.get());
                output.accept(ModItems.CONFERENCE_CREDENZA_ITEM.get());
                output.accept(ModItems.CONFERENCE_WALL_DISPLAY_ITEM.get());
                output.accept(ModItems.FLIP_CHART_EASEL_ITEM.get());
                output.accept(ModItems.CONFERENCE_SPEAKERPHONE_ITEM.get());
                output.accept(ModItems.GLASS_OFFICE_PARTITION_ITEM.get());
                output.accept(ModItems.LOUNGE_SOFA_ITEM.get());
                output.accept(ModItems.POTTED_OFFICE_PLANT_ITEM.get());
                output.accept(ModItems.WINDOW_BLINDS_ITEM.get());
                // Office
                output.accept(ModItems.OFFICE_CUBICLE_PARTITION_ITEM.get());
                output.accept(ModItems.RECEPTION_DESK_ITEM.get());
                output.accept(ModItems.MAIL_SORTING_SHELF_ITEM.get());
                output.accept(ModItems.OFFICE_PHOTOCOPIER_ITEM.get());
                output.accept(ModItems.DOCUMENT_TRAY_STACK_ITEM.get());
                output.accept(ModItems.WALL_BINDER_SHELF_ITEM.get());
                output.accept(ModItems.OFFICE_SAFE_ITEM.get());
                output.accept(ModItems.COAT_RACK_STAND_ITEM.get());
                output.accept(ModItems.BUNDY_TIME_CLOCK_ITEM.get());
                output.accept(ModItems.OFFICE_SUPPLY_CABINET_ITEM.get());
                // Laboratory
                output.accept(ModItems.LABORATORY_FUME_HOOD_ITEM.get());
                output.accept(ModItems.EQUIPMENT_RACK_ITEM.get());
                output.accept(ModItems.LAB_STOOL_ITEM.get());
                output.accept(ModItems.OSCILLOSCOPE_CART_ITEM.get());
                output.accept(ModItems.MICROSCOPE_STATION_ITEM.get());
                output.accept(ModItems.EYE_WASH_STATION_ITEM.get());
                output.accept(ModItems.COMPONENT_DRAWER_CABINET_ITEM.get());
                output.accept(ModItems.SECURED_CYLINDER_RACK_ITEM.get());
                output.accept(ModItems.SAMPLE_STORAGE_RACK_ITEM.get());
                output.accept(ModItems.BALANCE_SCALE_TABLE_ITEM.get());
                output.accept(ModItems.SCIENCE_LAB_WORKBENCH_ITEM.get());
            }).build());

    /**
     * Creative tab: Classroom &amp; Kitchen hazard props — merges {@link ModItems#HAZARD_ZONE_CLASSROOM}
     * and {@link ModItems#HAZARD_ZONE_KITCHEN} (16 + 27 = 43 items). {@code ModItems.HAZARD_ITEM_MAP}
     * and the 4 zone lists are unchanged; the merge happens only here, at display time.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAZARD_SCHOOL_TAB = CREATIVE_MODE_TABS.register("hazards_school_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.hazards_school"))
            .withTabsBefore(FURN_TAB.getKey())
            .icon(() -> ModItems.COMMERCIAL_DEEP_FRYER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ModItems.HAZARD_ZONE_CLASSROOM.forEach(id -> output.accept(ModItems.HAZARD_ITEM_MAP.get(id).get()));
                ModItems.HAZARD_ZONE_KITCHEN.forEach(id -> output.accept(ModItems.HAZARD_ITEM_MAP.get(id).get()));
            })
            .build());

    /**
     * Creative tab: Electrical/Lab &amp; Conference/Office hazard props — merges
     * {@link ModItems#HAZARD_ZONE_ELECTRICAL_LAB} and {@link ModItems#HAZARD_ZONE_CONFERENCE_OFFICE}
     * (22 + 20 = 42 items).
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAZARD_OFFICE_LAB_TAB = CREATIVE_MODE_TABS.register("hazards_office_lab_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.hazards_officelab"))
            .withTabsBefore(HAZARD_SCHOOL_TAB.getKey())
            .icon(() -> ModItems.VENTING_UPS_BATTERY_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ModItems.HAZARD_ZONE_ELECTRICAL_LAB.forEach(id -> output.accept(ModItems.HAZARD_ITEM_MAP.get(id).get()));
                ModItems.HAZARD_ZONE_CONFERENCE_OFFICE.forEach(id -> output.accept(ModItems.HAZARD_ITEM_MAP.get(id).get()));
            })
            .build());

    /**
     * Creative tab: every NPC spawner, pulled out of the crowded {@link #SIM_TAB} into its own
     * space so all 24 characters are easy to browse/find. Grouped by role with the same ordering
     * as {@link NpcType}: the 4 active New Tutorial (Academy) room instructors, the 5 Academy
     * background NPCs, the 4 sim-building faculty, then the 11 sim-building students.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NPC_TAB = CREATIVE_MODE_TABS.register("npc_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.npcs"))
            .withTabsBefore(HAZARD_OFFICE_LAB_TAB.getKey())
            .icon(() -> ModItems.NPC_OFFICER_CRUZ.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // New Tutorial (Academy) — active room instructors
                output.accept(ModItems.NPC_SGT_REYES.get());
                output.accept(ModItems.NPC_SGT_SANTOS.get());
                output.accept(ModItems.NPC_OFFICER_CRUZ.get());
                output.accept(ModItems.NPC_CAPT_MORFE.get());
                // New Tutorial (Academy) — decorative background NPCs
                output.accept(ModItems.NPC_SECURITY_TUAZON.get());
                output.accept(ModItems.NPC_DM_ORLANDA.get());
                output.accept(ModItems.NPC_NECOOKIE.get());
                output.accept(ModItems.NPC_SIR_BOOKMARK.get());
                output.accept(ModItems.NPC_STUDENT.get());
                // Sim building faculty
                output.accept(ModItems.NPC_PROF_INSTRUCTOR_DAVID.get());
                output.accept(ModItems.NPC_PROF_PRINCIPAL_BROWN.get());
                output.accept(ModItems.NPC_PROF_PROFESSOR_BALDWIN.get());
                output.accept(ModItems.NPC_PROF_PROFESSOR_KEVIN.get());
                // Sim building students
                output.accept(ModItems.NPC_STUDENT_GOLDY.get());
                output.accept(ModItems.NPC_STUDENT_HARVEY.get());
                output.accept(ModItems.NPC_STUDENT_JENNY.get());
                output.accept(ModItems.NPC_STUDENT_KAEFLA.get());
                output.accept(ModItems.NPC_STUDENT_KARL.get());
                output.accept(ModItems.NPC_STUDENT_KATH.get());
                output.accept(ModItems.NPC_STUDENT_KELLY.get());
                output.accept(ModItems.NPC_STUDENT_NATH.get());
                output.accept(ModItems.NPC_STUDENT_NECOOKIE.get());
                output.accept(ModItems.NPC_STUDENT_NELL.get());
                output.accept(ModItems.NPC_STUDENT_PRINCESS.get());
            }).build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
