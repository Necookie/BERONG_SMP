package net.necookie.disastersim.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.item.CO2ExtinguisherItem;
import net.necookie.disastersim.item.FireExtinguisherItem;
import net.necookie.disastersim.item.HazardWandItem;
import net.necookie.disastersim.item.NpcSpawnerItem;
import net.necookie.disastersim.item.WetChemicalExtinguisherItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All item registrations for the mod: NPC spawners, block items (referencing {@link ModBlocks}),
 * extinguishers, the hazard wand, and the firefighter uniform, plus the two lookup maps behind
 * {@code /item hazard}, {@code /item get}, and {@code /item kit}. Extracted from {@code BerongSMP}
 * so the entry point stays a thin bootstrap; {@link #register(IEventBus)} is called from its
 * constructor.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BerongSMP.MODID);

    public static final DeferredItem<NpcSpawnerItem> NPC_SGT_REYES =
            ITEMS.registerItem("npc_sgt_reyes",
                    p -> new NpcSpawnerItem(NpcType.SGT_REYES, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_SGT_SANTOS =
            ITEMS.registerItem("npc_sgt_santos",
                    p -> new NpcSpawnerItem(NpcType.SGT_SANTOS, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_OFFICER_CRUZ =
            ITEMS.registerItem("npc_officer_cruz",
                    p -> new NpcSpawnerItem(NpcType.OFFICER_CRUZ, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_CAPT_MORFE =
            ITEMS.registerItem("npc_capt_morfe",
                    p -> new NpcSpawnerItem(NpcType.CAPT_MORFE, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_SECURITY_TUAZON =
            ITEMS.registerItem("npc_security_tuazon",
                    p -> new NpcSpawnerItem(NpcType.SECURITY_TUAZON, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_DM_ORLANDA =
            ITEMS.registerItem("npc_dm_orlanda",
                    p -> new NpcSpawnerItem(NpcType.DM_ORLANDA, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_NECOOKIE =
            ITEMS.registerItem("npc_necookie",
                    p -> new NpcSpawnerItem(NpcType.NECOOKIE, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_SIR_BOOKMARK =
            ITEMS.registerItem("npc_sir_bookmark",
                    p -> new NpcSpawnerItem(NpcType.SIR_BOOKMARK, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT =
            ITEMS.registerItem("npc_student",
                    p -> new NpcSpawnerItem(NpcType.STUDENT, p.stacksTo(16)));

    /** Example block item registration. */
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", ModBlocks.EXAMPLE_BLOCK);

    /** Computer block item. */
    public static final DeferredItem<BlockItem> COMPUTER_ITEM = ITEMS.registerSimpleBlockItem("computer", ModBlocks.COMPUTER);

    /** Fire alarm block item. */
    public static final DeferredItem<BlockItem> FIRE_ALARM_ITEM =
            ITEMS.registerSimpleBlockItem("fire_alarm", ModBlocks.FIRE_ALARM_BLOCK);

    public static final DeferredItem<BlockItem> WHITEBOARD_ITEM = ITEMS.registerSimpleBlockItem("whiteboard", ModBlocks.WHITEBOARD);

    public static final DeferredItem<BlockItem> FIRE_HOSE_CABINET_ITEM = ITEMS.registerSimpleBlockItem("fire_hose_cabinet", ModBlocks.FIRE_HOSE_CABINET);

    public static final DeferredItem<BlockItem> TOILET_ITEM = ITEMS.registerSimpleBlockItem("toilet", ModBlocks.TOILET);

    public static final DeferredItem<BlockItem> SINK_ITEM = ITEMS.registerSimpleBlockItem("sink", ModBlocks.SINK);

    public static final DeferredItem<BlockItem> DRAWERS_ITEM = ITEMS.registerSimpleBlockItem("drawers", ModBlocks.DRAWERS);

    public static final DeferredItem<BlockItem> COMPUTER_TABLE_ITEM = ITEMS.registerSimpleBlockItem("computer_table", ModBlocks.COMPUTER_TABLE);

    public static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem("table", ModBlocks.TABLE);

    public static final DeferredItem<BlockItem> CHAIR_ITEM = ITEMS.registerSimpleBlockItem("chair", ModBlocks.CHAIR);

    public static final DeferredItem<BlockItem> FILING_CABINET_ITEM = ITEMS.registerSimpleBlockItem("filing_cabinet", ModBlocks.FILING_CABINET);

    public static final DeferredItem<BlockItem> LOCKER_ITEM = ITEMS.registerSimpleBlockItem("locker", ModBlocks.LOCKER);

    public static final DeferredItem<BlockItem> TRASH_CAN_ITEM = ITEMS.registerSimpleBlockItem("trash_can", ModBlocks.TRASH_CAN);

    public static final DeferredItem<BlockItem> BULLETIN_BOARD_ITEM = ITEMS.registerSimpleBlockItem("bulletin_board", ModBlocks.BULLETIN_BOARD);

    public static final DeferredItem<BlockItem> CEILING_FAN_ITEM = ITEMS.registerSimpleBlockItem("ceiling_fan", ModBlocks.CEILING_FAN);

    public static final DeferredItem<BlockItem> LIGHT_BULB_ITEM = ITEMS.registerSimpleBlockItem("light_bulb", ModBlocks.LIGHT_BULB);

    /** Example food item registration. */
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    /** The Fire Extinguisher item registration. */
    public static final DeferredItem<FireExtinguisherItem> FIRE_EXTINGUISHER = ITEMS.registerItem("fire_extinguisher",
            props -> new FireExtinguisherItem(props.durability(300)));

    /** CO2 extinguisher — for Class C (electrical) fires; targets lit ComputerBlocks. */
    public static final DeferredItem<CO2ExtinguisherItem> CO2_EXTINGUISHER = ITEMS.registerItem("co2_extinguisher",
            props -> new CO2ExtinguisherItem(props.durability(200)));

    /** Wet chemical extinguisher — Philippine BFP yellow-coded Class F/K tool for kitchen grease fires. */
    public static final DeferredItem<WetChemicalExtinguisherItem> WET_CHEMICAL_EXTINGUISHER = ITEMS.registerItem("wet_chemical_extinguisher",
            props -> new WetChemicalExtinguisherItem(props.durability(240)));

    /** Dev-only tool: right-click a hazard prop to toggle its state, or shift+right-click to force its failure. */
    public static final DeferredItem<HazardWandItem> HAZARD_WAND = ITEMS.registerItem("hazard_wand",
            props -> new HazardWandItem(props.stacksTo(1)));

    // ── Firefighter uniform (armor) ──────────────────────────────────────────
    // MC 26.1.2 has no ArmorItem class — armor is a plain Item built via
    // Item.Properties.humanoidArmor(ArmorMaterial, ArmorType), and ArmorMaterial
    // is a plain record, not a DeferredRegister entry. Rendering is driven by the
    // resource-only assets/berongsmp/equipment/firefighter_uniform.json file.

    /** Points at assets/berongsmp/equipment/firefighter_uniform.json — not a Java registration. */
    public static final ResourceKey<EquipmentAsset> FIREFIGHTER_UNIFORM_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "firefighter_uniform"));

    /**
     * Protective training gear, not PVP equipment — defense/toughness deliberately modest
     * (comparable to leather/chainmail). Reuses vanilla's leather equip sound/repair tag rather
     * than registering new ones.
     */
    public static final ArmorMaterial FIREFIGHTER_MATERIAL =
            new ArmorMaterial(
                    15,
                    java.util.Map.of(
                            ArmorType.BOOTS, 2,
                            ArmorType.LEGGINGS, 4,
                            ArmorType.CHESTPLATE, 6,
                            ArmorType.HELMET, 2,
                            ArmorType.BODY, 0),
                    9,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    0.0F, 0.0F,
                    ItemTags.REPAIRS_LEATHER_ARMOR,
                    FIREFIGHTER_UNIFORM_ASSET);

    public static final DeferredItem<Item> FIREFIGHTER_HELMET = ITEMS.registerItem("firefighter_helmet",
            props -> new Item(props.humanoidArmor(FIREFIGHTER_MATERIAL, ArmorType.HELMET)));

    public static final DeferredItem<Item> FIREFIGHTER_COAT = ITEMS.registerItem("firefighter_coat",
            props -> new Item(props.humanoidArmor(FIREFIGHTER_MATERIAL, ArmorType.CHESTPLATE)));

    public static final DeferredItem<Item> FIREFIGHTER_PANTS = ITEMS.registerItem("firefighter_pants",
            props -> new Item(props.humanoidArmor(FIREFIGHTER_MATERIAL, ArmorType.LEGGINGS)));

    public static final DeferredItem<Item> FIREFIGHTER_BOOTS = ITEMS.registerItem("firefighter_boots",
            props -> new Item(props.humanoidArmor(FIREFIGHTER_MATERIAL, ArmorType.BOOTS)));

    /** Maps hazard block registry name → its DeferredItem for the /item hazard command. */
    public static final LinkedHashMap<String, DeferredItem<BlockItem>> HAZARD_ITEM_MAP = new LinkedHashMap<>();

    public static final DeferredItem<BlockItem> PLASTIC_TRASH_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("plastic_trash_bin", ModBlocks.PLASTIC_TRASH_BIN);

    static { HAZARD_ITEM_MAP.put("plastic_trash_bin", PLASTIC_TRASH_BIN_ITEM); }

    public static final DeferredItem<BlockItem> DAISY_CHAIN_EXTENSION_ITEM =
            ITEMS.registerSimpleBlockItem("daisy_chain_extension", ModBlocks.DAISY_CHAIN_EXTENSION);

    static { HAZARD_ITEM_MAP.put("daisy_chain_extension", DAISY_CHAIN_EXTENSION_ITEM); }

    public static final DeferredItem<BlockItem> WOODSHOP_SAWDUST_LAYER_ITEM =
            ITEMS.registerSimpleBlockItem("woodshop_sawdust_layer", ModBlocks.WOODSHOP_SAWDUST_LAYER);

    static { HAZARD_ITEM_MAP.put("woodshop_sawdust_layer", WOODSHOP_SAWDUST_LAYER_ITEM); }

    public static final DeferredItem<BlockItem> STAGE_SPOTLIGHT_ITEM =
            ITEMS.registerSimpleBlockItem("stage_spotlight", ModBlocks.STAGE_SPOTLIGHT);

    static { HAZARD_ITEM_MAP.put("stage_spotlight", STAGE_SPOTLIGHT_ITEM); }

    public static final DeferredItem<BlockItem> ARCHIVE_BOX_STACK_ITEM =
            ITEMS.registerSimpleBlockItem("archive_box_stack", ModBlocks.ARCHIVE_BOX_STACK);

    static { HAZARD_ITEM_MAP.put("archive_box_stack", ARCHIVE_BOX_STACK_ITEM); }

    public static final DeferredItem<BlockItem> DUST_CHOKED_PC_ITEM =
            ITEMS.registerSimpleBlockItem("dust_choked_pc", ModBlocks.DUST_CHOKED_PC);

    static { HAZARD_ITEM_MAP.put("dust_choked_pc", DUST_CHOKED_PC_ITEM); }

    public static final DeferredItem<BlockItem> CHARGING_CART_ITEM =
            ITEMS.registerSimpleBlockItem("charging_cart", ModBlocks.CHARGING_CART);

    static { HAZARD_ITEM_MAP.put("charging_cart", CHARGING_CART_ITEM); }

    public static final DeferredItem<BlockItem> FRAYED_CONSOLE_WIRE_ITEM =
            ITEMS.registerSimpleBlockItem("frayed_console_wire", ModBlocks.FRAYED_CONSOLE_WIRE);

    static { HAZARD_ITEM_MAP.put("frayed_console_wire", FRAYED_CONSOLE_WIRE_ITEM); }

    public static final DeferredItem<BlockItem> MALFUNCTIONING_VENDING_ITEM =
            ITEMS.registerSimpleBlockItem("malfunctioning_vending", ModBlocks.MALFUNCTIONING_VENDING);

    static { HAZARD_ITEM_MAP.put("malfunctioning_vending", MALFUNCTIONING_VENDING_ITEM); }

    public static final DeferredItem<BlockItem> CEILING_PROJECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("ceiling_projector", ModBlocks.CEILING_PROJECTOR);

    static { HAZARD_ITEM_MAP.put("ceiling_projector", CEILING_PROJECTOR_ITEM); }

    public static final DeferredItem<BlockItem> SWOLLEN_PHONE_BATTERY_ITEM =
            ITEMS.registerSimpleBlockItem("swollen_phone_battery", ModBlocks.SWOLLEN_PHONE_BATTERY);

    static { HAZARD_ITEM_MAP.put("swollen_phone_battery", SWOLLEN_PHONE_BATTERY_ITEM); }

    public static final DeferredItem<BlockItem> DAMAGED_LIPO_PACK_ITEM =
            ITEMS.registerSimpleBlockItem("damaged_lipo_pack", ModBlocks.DAMAGED_LIPO_PACK);

    static { HAZARD_ITEM_MAP.put("damaged_lipo_pack", DAMAGED_LIPO_PACK_ITEM); }

    public static final DeferredItem<BlockItem> VAPE_IN_IRON_LOCKER_ITEM =
            ITEMS.registerSimpleBlockItem("vape_in_iron_locker", ModBlocks.VAPE_IN_IRON_LOCKER);

    static { HAZARD_ITEM_MAP.put("vape_in_iron_locker", VAPE_IN_IRON_LOCKER_ITEM); }

    public static final DeferredItem<BlockItem> PA_SYSTEM_BACKUP_ITEM =
            ITEMS.registerSimpleBlockItem("pa_system_backup", ModBlocks.PA_SYSTEM_BACKUP);

    static { HAZARD_ITEM_MAP.put("pa_system_backup", PA_SYSTEM_BACKUP_ITEM); }

    public static final DeferredItem<BlockItem> SMARTBOARD_INVERTER_ITEM =
            ITEMS.registerSimpleBlockItem("smartboard_inverter", ModBlocks.SMARTBOARD_INVERTER);

    static { HAZARD_ITEM_MAP.put("smartboard_inverter", SMARTBOARD_INVERTER_ITEM); }

    public static final DeferredItem<BlockItem> UNATTENDED_GREASE_PAN_ITEM =
            ITEMS.registerSimpleBlockItem("unattended_grease_pan", ModBlocks.UNATTENDED_GREASE_PAN);

    static { HAZARD_ITEM_MAP.put("unattended_grease_pan", UNATTENDED_GREASE_PAN_ITEM); }

    public static final DeferredItem<BlockItem> GREASE_CLOGGED_HOOD_ITEM =
            ITEMS.registerSimpleBlockItem("grease_clogged_hood", ModBlocks.GREASE_CLOGGED_HOOD);

    static { HAZARD_ITEM_MAP.put("grease_clogged_hood", GREASE_CLOGGED_HOOD_ITEM); }

    public static final DeferredItem<BlockItem> CONTAMINATED_KITCHEN_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("contaminated_kitchen_bin", ModBlocks.CONTAMINATED_KITCHEN_BIN);

    static { HAZARD_ITEM_MAP.put("contaminated_kitchen_bin", CONTAMINATED_KITCHEN_BIN_ITEM); }

    public static final DeferredItem<BlockItem> JAMMED_PANINI_PRESS_ITEM =
            ITEMS.registerSimpleBlockItem("jammed_panini_press", ModBlocks.JAMMED_PANINI_PRESS);

    static { HAZARD_ITEM_MAP.put("jammed_panini_press", JAMMED_PANINI_PRESS_ITEM); }

    public static final DeferredItem<BlockItem> COMMERCIAL_DEEP_FRYER_ITEM =
            ITEMS.registerSimpleBlockItem("commercial_deep_fryer", ModBlocks.COMMERCIAL_DEEP_FRYER);

    static { HAZARD_ITEM_MAP.put("commercial_deep_fryer", COMMERCIAL_DEEP_FRYER_ITEM); }

    public static final DeferredItem<BlockItem> OVERLOADED_MICROWAVE_ITEM =
            ITEMS.registerSimpleBlockItem("overloaded_microwave", ModBlocks.OVERLOADED_MICROWAVE);
    static { HAZARD_ITEM_MAP.put("overloaded_microwave", OVERLOADED_MICROWAVE_ITEM); }

    public static final DeferredItem<BlockItem> BUNSEN_BURNER_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("bunsen_burner_station", ModBlocks.BUNSEN_BURNER_STATION);
    static { HAZARD_ITEM_MAP.put("bunsen_burner_station", BUNSEN_BURNER_STATION_ITEM); }

    public static final DeferredItem<BlockItem> REAGENT_STORAGE_SHELF_ITEM =
            ITEMS.registerSimpleBlockItem("reagent_storage_shelf", ModBlocks.REAGENT_STORAGE_SHELF);
    static { HAZARD_ITEM_MAP.put("reagent_storage_shelf", REAGENT_STORAGE_SHELF_ITEM); }

    public static final DeferredItem<BlockItem> OVERLOADED_BREAKER_PANEL_ITEM =
            ITEMS.registerSimpleBlockItem("overloaded_breaker_panel", ModBlocks.OVERLOADED_BREAKER_PANEL);
    static { HAZARD_ITEM_MAP.put("overloaded_breaker_panel", OVERLOADED_BREAKER_PANEL_ITEM); }

    public static final DeferredItem<BlockItem> OVERHEATING_WALL_AIRCON_ITEM =
            ITEMS.registerSimpleBlockItem("overheating_wall_aircon", ModBlocks.OVERHEATING_WALL_AIRCON);
    static { HAZARD_ITEM_MAP.put("overheating_wall_aircon", OVERHEATING_WALL_AIRCON_ITEM); }

    public static final DeferredItem<BlockItem> JAMMED_LASER_PRINTER_ITEM =
            ITEMS.registerSimpleBlockItem("jammed_laser_printer", ModBlocks.JAMMED_LASER_PRINTER);
    static { HAZARD_ITEM_MAP.put("jammed_laser_printer", JAMMED_LASER_PRINTER_ITEM); }

    public static final DeferredItem<BlockItem> UNATTENDED_SHRINE_CANDLE_ITEM =
            ITEMS.registerSimpleBlockItem("unattended_shrine_candle", ModBlocks.UNATTENDED_SHRINE_CANDLE);
    static { HAZARD_ITEM_MAP.put("unattended_shrine_candle", UNATTENDED_SHRINE_CANDLE_ITEM); }

    public static final DeferredItem<BlockItem> LEAKING_GAS_VALVE_ITEM =
            ITEMS.registerSimpleBlockItem("leaking_gas_valve", ModBlocks.LEAKING_GAS_VALVE);
    static { HAZARD_ITEM_MAP.put("leaking_gas_valve", LEAKING_GAS_VALVE_ITEM); }

    public static final DeferredItem<BlockItem> ALCOHOL_DISPENSER_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("alcohol_dispenser_station", ModBlocks.ALCOHOL_DISPENSER_STATION);
    static { HAZARD_ITEM_MAP.put("alcohol_dispenser_station", ALCOHOL_DISPENSER_STATION_ITEM); }

    public static final DeferredItem<BlockItem> CLOGGED_EXHAUST_FAN_ITEM =
            ITEMS.registerSimpleBlockItem("clogged_exhaust_fan", ModBlocks.CLOGGED_EXHAUST_FAN);
    static { HAZARD_ITEM_MAP.put("clogged_exhaust_fan", CLOGGED_EXHAUST_FAN_ITEM); }

    public static final DeferredItem<BlockItem> TEACHERS_DESK_ITEM =
            ITEMS.registerSimpleBlockItem("teachers_desk", ModBlocks.TEACHERS_DESK);

    public static final DeferredItem<BlockItem> ARMCHAIR_DESK_ITEM =
            ITEMS.registerSimpleBlockItem("armchair_desk", ModBlocks.ARMCHAIR_DESK);

    public static final DeferredItem<BlockItem> TALL_BOOKSHELF_ITEM =
            ITEMS.registerSimpleBlockItem("tall_bookshelf", ModBlocks.TALL_BOOKSHELF);

    public static final DeferredItem<BlockItem> PHILIPPINE_FLAG_STAND_ITEM =
            ITEMS.registerSimpleBlockItem("philippine_flag_stand", ModBlocks.PHILIPPINE_FLAG_STAND);

    public static final DeferredItem<BlockItem> TROPHY_CABINET_ITEM =
            ITEMS.registerSimpleBlockItem("trophy_cabinet", ModBlocks.TROPHY_CABINET);

    public static final DeferredItem<BlockItem> WATER_DISPENSER_ITEM =
            ITEMS.registerSimpleBlockItem("water_dispenser", ModBlocks.WATER_DISPENSER);

    /** Maps every custom BerongSMP item's registry name → its DeferredItem, for {@code /item get} and {@code /item kit}. */
    public static final LinkedHashMap<String, DeferredItem<? extends Item>> ALL_ITEM_MAP = new LinkedHashMap<>();

    static {
        ALL_ITEM_MAP.put("fire_extinguisher", FIRE_EXTINGUISHER);
        ALL_ITEM_MAP.put("co2_extinguisher", CO2_EXTINGUISHER);
        ALL_ITEM_MAP.put("wet_chemical_extinguisher", WET_CHEMICAL_EXTINGUISHER);
        ALL_ITEM_MAP.put("hazard_wand", HAZARD_WAND);
        ALL_ITEM_MAP.put("firefighter_helmet", FIREFIGHTER_HELMET);
        ALL_ITEM_MAP.put("firefighter_coat", FIREFIGHTER_COAT);
        ALL_ITEM_MAP.put("firefighter_pants", FIREFIGHTER_PANTS);
        ALL_ITEM_MAP.put("firefighter_boots", FIREFIGHTER_BOOTS);
        ALL_ITEM_MAP.put("computer", COMPUTER_ITEM);
        ALL_ITEM_MAP.put("fire_alarm", FIRE_ALARM_ITEM);
        ALL_ITEM_MAP.put("npc_sgt_reyes", NPC_SGT_REYES);
        ALL_ITEM_MAP.put("npc_sgt_santos", NPC_SGT_SANTOS);
        ALL_ITEM_MAP.put("npc_officer_cruz", NPC_OFFICER_CRUZ);
        ALL_ITEM_MAP.put("npc_capt_morfe", NPC_CAPT_MORFE);
        ALL_ITEM_MAP.put("npc_security_tuazon", NPC_SECURITY_TUAZON);
        ALL_ITEM_MAP.put("npc_dm_orlanda", NPC_DM_ORLANDA);
        ALL_ITEM_MAP.put("npc_necookie", NPC_NECOOKIE);
        ALL_ITEM_MAP.put("npc_sir_bookmark", NPC_SIR_BOOKMARK);
        ALL_ITEM_MAP.put("npc_student", NPC_STUDENT);
        ALL_ITEM_MAP.put("whiteboard", WHITEBOARD_ITEM);
        ALL_ITEM_MAP.put("fire_hose_cabinet", FIRE_HOSE_CABINET_ITEM);
        ALL_ITEM_MAP.put("bulletin_board", BULLETIN_BOARD_ITEM);
        ALL_ITEM_MAP.put("computer_table", COMPUTER_TABLE_ITEM);
        ALL_ITEM_MAP.put("table", TABLE_ITEM);
        ALL_ITEM_MAP.put("chair", CHAIR_ITEM);
        ALL_ITEM_MAP.put("drawers", DRAWERS_ITEM);
        ALL_ITEM_MAP.put("filing_cabinet", FILING_CABINET_ITEM);
        ALL_ITEM_MAP.put("locker", LOCKER_ITEM);
        ALL_ITEM_MAP.put("toilet", TOILET_ITEM);
        ALL_ITEM_MAP.put("sink", SINK_ITEM);
        ALL_ITEM_MAP.put("trash_can", TRASH_CAN_ITEM);
        ALL_ITEM_MAP.put("ceiling_fan", CEILING_FAN_ITEM);
        ALL_ITEM_MAP.put("light_bulb", LIGHT_BULB_ITEM);
        ALL_ITEM_MAP.put("teachers_desk", TEACHERS_DESK_ITEM);
        ALL_ITEM_MAP.put("armchair_desk", ARMCHAIR_DESK_ITEM);
        ALL_ITEM_MAP.put("tall_bookshelf", TALL_BOOKSHELF_ITEM);
        ALL_ITEM_MAP.put("philippine_flag_stand", PHILIPPINE_FLAG_STAND_ITEM);
        ALL_ITEM_MAP.put("trophy_cabinet", TROPHY_CABINET_ITEM);
        ALL_ITEM_MAP.put("water_dispenser", WATER_DISPENSER_ITEM);
        ALL_ITEM_MAP.putAll(HAZARD_ITEM_MAP);
    }

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
