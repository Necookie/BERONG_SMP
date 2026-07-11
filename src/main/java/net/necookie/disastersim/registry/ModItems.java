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
import net.necookie.disastersim.item.FlashlightItem;
import net.necookie.disastersim.item.SafetyWhistleItem;
import net.necookie.disastersim.item.MegaphoneItem;
import net.necookie.disastersim.item.FirstAidKitItem;
import net.necookie.disastersim.item.FireBlanketItem;
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

    // ── Sim building faculty spawners (npc_prof_* — tab-complete groups them) ──

    public static final DeferredItem<NpcSpawnerItem> NPC_PROF_INSTRUCTOR_DAVID =
            ITEMS.registerItem("npc_prof_instructor_david",
                    p -> new NpcSpawnerItem(NpcType.PROF_INSTRUCTOR_DAVID, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_PROF_PRINCIPAL_BROWN =
            ITEMS.registerItem("npc_prof_principal_brown",
                    p -> new NpcSpawnerItem(NpcType.PROF_PRINCIPAL_BROWN, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_PROF_PROFESSOR_BALDWIN =
            ITEMS.registerItem("npc_prof_professor_baldwin",
                    p -> new NpcSpawnerItem(NpcType.PROF_PROFESSOR_BALDWIN, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_PROF_PROFESSOR_KEVIN =
            ITEMS.registerItem("npc_prof_professor_kevin",
                    p -> new NpcSpawnerItem(NpcType.PROF_PROFESSOR_KEVIN, p.stacksTo(16)));

    // ── Sim building student spawners (npc_student_* — tab-complete groups them) ──

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_GOLDY =
            ITEMS.registerItem("npc_student_goldy",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_GOLDY, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_HARVEY =
            ITEMS.registerItem("npc_student_harvey",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_HARVEY, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_JENNY =
            ITEMS.registerItem("npc_student_jenny",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_JENNY, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KAEFLA =
            ITEMS.registerItem("npc_student_kaefla",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KAEFLA, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KARL =
            ITEMS.registerItem("npc_student_karl",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KARL, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KATH =
            ITEMS.registerItem("npc_student_kath",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KATH, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_KELLY =
            ITEMS.registerItem("npc_student_kelly",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_KELLY, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NATH =
            ITEMS.registerItem("npc_student_nath",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NATH, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NECOOKIE =
            ITEMS.registerItem("npc_student_necookie",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NECOOKIE, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_NELL =
            ITEMS.registerItem("npc_student_nell",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_NELL, p.stacksTo(16)));

    public static final DeferredItem<NpcSpawnerItem> NPC_STUDENT_PRINCESS =
            ITEMS.registerItem("npc_student_princess",
                    p -> new NpcSpawnerItem(NpcType.STUDENT_PRINCESS, p.stacksTo(16)));

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

    public static final DeferredItem<BlockItem> GLOWING_OAK_PLANKS_ITEM =
            ITEMS.registerSimpleBlockItem("glowing_oak_planks", ModBlocks.GLOWING_OAK_PLANKS);

    public static final DeferredItem<BlockItem> COURT_LINE_ITEM =
            ITEMS.registerSimpleBlockItem("court_line", ModBlocks.COURT_LINE);

    public static final DeferredItem<BlockItem> BADMINTON_NET_POST_ITEM =
            ITEMS.registerSimpleBlockItem("badminton_net_post", ModBlocks.BADMINTON_NET_POST);

    public static final DeferredItem<BlockItem> BADMINTON_NET_MESH_ITEM =
            ITEMS.registerSimpleBlockItem("badminton_net_mesh", ModBlocks.BADMINTON_NET_MESH);

    public static final DeferredItem<BlockItem> BASKETBALL_HOOP_POST_ITEM =
            ITEMS.registerSimpleBlockItem("basketball_hoop_post", ModBlocks.BASKETBALL_HOOP_POST);

    public static final DeferredItem<BlockItem> BASKETBALL_POLE_ITEM =
            ITEMS.registerSimpleBlockItem("basketball_pole", ModBlocks.BASKETBALL_POLE);

    public static final DeferredItem<BlockItem> BASKETBALL_HOOP_ITEM =
            ITEMS.registerSimpleBlockItem("basketball_hoop", ModBlocks.BASKETBALL_HOOP);

    /** Plain handheld basketball — decorative, no special behaviour. */
    public static final DeferredItem<Item> BASKETBALL = ITEMS.registerSimpleItem("basketball",
            p -> p.stacksTo(16));

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

    public static final DeferredItem<BlockItem> OVERLOADED_WALL_OUTLET_ITEM =
            ITEMS.registerSimpleBlockItem("overloaded_wall_outlet", ModBlocks.OVERLOADED_WALL_OUTLET);
    static { HAZARD_ITEM_MAP.put("overloaded_wall_outlet", OVERLOADED_WALL_OUTLET_ITEM); }

    public static final DeferredItem<BlockItem> JAMMED_CIRCUIT_BREAKER_ITEM =
            ITEMS.registerSimpleBlockItem("jammed_circuit_breaker", ModBlocks.JAMMED_CIRCUIT_BREAKER);
    static { HAZARD_ITEM_MAP.put("jammed_circuit_breaker", JAMMED_CIRCUIT_BREAKER_ITEM); }

    public static final DeferredItem<BlockItem> UNSEALED_SOLVENT_SHELF_ITEM =
            ITEMS.registerSimpleBlockItem("unsealed_solvent_shelf", ModBlocks.UNSEALED_SOLVENT_SHELF);
    static { HAZARD_ITEM_MAP.put("unsealed_solvent_shelf", UNSEALED_SOLVENT_SHELF_ITEM); }

    public static final DeferredItem<BlockItem> UNATTENDED_WELDING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("unattended_welding_station", ModBlocks.UNATTENDED_WELDING_STATION);
    static { HAZARD_ITEM_MAP.put("unattended_welding_station", UNATTENDED_WELDING_STATION_ITEM); }

    public static final DeferredItem<BlockItem> LEAKING_BUTANE_CANISTER_STOVE_ITEM =
            ITEMS.registerSimpleBlockItem("leaking_butane_canister_stove", ModBlocks.LEAKING_BUTANE_CANISTER_STOVE);
    static { HAZARD_ITEM_MAP.put("leaking_butane_canister_stove", LEAKING_BUTANE_CANISTER_STOVE_ITEM); }

    public static final DeferredItem<BlockItem> CHEFS_PREP_DRAWERS_ITEM =
            ITEMS.registerSimpleBlockItem("chefs_prep_drawers", ModBlocks.CHEFS_PREP_DRAWERS);
    static { HAZARD_ITEM_MAP.put("chefs_prep_drawers", CHEFS_PREP_DRAWERS_ITEM); }

    public static final DeferredItem<BlockItem> CULINARY_FRIDGE_ITEM =
            ITEMS.registerSimpleBlockItem("culinary_fridge", ModBlocks.CULINARY_FRIDGE);
    static { HAZARD_ITEM_MAP.put("culinary_fridge", CULINARY_FRIDGE_ITEM); }

    public static final DeferredItem<BlockItem> STUDENT_LAB_MICROWAVE_ITEM =
            ITEMS.registerSimpleBlockItem("student_lab_microwave", ModBlocks.STUDENT_LAB_MICROWAVE);
    static { HAZARD_ITEM_MAP.put("student_lab_microwave", STUDENT_LAB_MICROWAVE_ITEM); }

    public static final DeferredItem<BlockItem> CORRODED_GAS_LINE_JOINT_ITEM =
            ITEMS.registerSimpleBlockItem("corroded_gas_line_joint", ModBlocks.CORRODED_GAS_LINE_JOINT);
    static { HAZARD_ITEM_MAP.put("corroded_gas_line_joint", CORRODED_GAS_LINE_JOINT_ITEM); }

    public static final DeferredItem<BlockItem> GAS_RANGE_STUCK_BURNER_ITEM =
            ITEMS.registerSimpleBlockItem("gas_range_stuck_burner", ModBlocks.GAS_RANGE_STUCK_BURNER);
    static { HAZARD_ITEM_MAP.put("gas_range_stuck_burner", GAS_RANGE_STUCK_BURNER_ITEM); }

    public static final DeferredItem<BlockItem> COMMERCIAL_STAND_MIXER_ITEM =
            ITEMS.registerSimpleBlockItem("commercial_stand_mixer", ModBlocks.COMMERCIAL_STAND_MIXER);
    static { HAZARD_ITEM_MAP.put("commercial_stand_mixer", COMMERCIAL_STAND_MIXER_ITEM); }

    public static final DeferredItem<BlockItem> GAS_DECK_OVEN_ITEM =
            ITEMS.registerSimpleBlockItem("gas_deck_oven", ModBlocks.GAS_DECK_OVEN);
    static { HAZARD_ITEM_MAP.put("gas_deck_oven", GAS_DECK_OVEN_ITEM); }

    public static final DeferredItem<BlockItem> INDUCTION_COOKTOP_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("induction_cooktop_station", ModBlocks.INDUCTION_COOKTOP_STATION);
    static { HAZARD_ITEM_MAP.put("induction_cooktop_station", INDUCTION_COOKTOP_STATION_ITEM); }

    public static final DeferredItem<BlockItem> RICE_COOKER_BANK_ITEM =
            ITEMS.registerSimpleBlockItem("rice_cooker_bank", ModBlocks.RICE_COOKER_BANK);
    static { HAZARD_ITEM_MAP.put("rice_cooker_bank", RICE_COOKER_BANK_ITEM); }

    public static final DeferredItem<BlockItem> ESPRESSO_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem("espresso_machine", ModBlocks.ESPRESSO_MACHINE);
    static { HAZARD_ITEM_MAP.put("espresso_machine", ESPRESSO_MACHINE_ITEM); }

    public static final DeferredItem<BlockItem> HOT_WATER_URN_ITEM =
            ITEMS.registerSimpleBlockItem("hot_water_urn", ModBlocks.HOT_WATER_URN);
    static { HAZARD_ITEM_MAP.put("hot_water_urn", HOT_WATER_URN_ITEM); }

    public static final DeferredItem<BlockItem> TOASTER_OVEN_CRUMB_ITEM =
            ITEMS.registerSimpleBlockItem("toaster_oven_crumb", ModBlocks.TOASTER_OVEN_CRUMB);
    static { HAZARD_ITEM_MAP.put("toaster_oven_crumb", TOASTER_OVEN_CRUMB_ITEM); }

    public static final DeferredItem<BlockItem> DRY_GOODS_PANTRY_SHELF_ITEM =
            ITEMS.registerSimpleBlockItem("dry_goods_pantry_shelf", ModBlocks.DRY_GOODS_PANTRY_SHELF);
    static { HAZARD_ITEM_MAP.put("dry_goods_pantry_shelf", DRY_GOODS_PANTRY_SHELF_ITEM); }

    public static final DeferredItem<BlockItem> GREASE_DUCT_RUN_ITEM =
            ITEMS.registerSimpleBlockItem("grease_duct_run", ModBlocks.GREASE_DUCT_RUN);
    static { HAZARD_ITEM_MAP.put("grease_duct_run", GREASE_DUCT_RUN_ITEM); }

    public static final DeferredItem<BlockItem> COMMERCIAL_DISH_SANITIZER_ITEM =
            ITEMS.registerSimpleBlockItem("commercial_dish_sanitizer", ModBlocks.COMMERCIAL_DISH_SANITIZER);
    static { HAZARD_ITEM_MAP.put("commercial_dish_sanitizer", COMMERCIAL_DISH_SANITIZER_ITEM); }

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

    public static final DeferredItem<BlockItem> WALL_CLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("wall_clock", ModBlocks.WALL_CLOCK);

    public static final DeferredItem<BlockItem> BLACKBOARD_ITEM =
            ITEMS.registerSimpleBlockItem("blackboard", ModBlocks.BLACKBOARD);

    public static final DeferredItem<BlockItem> PODIUM_LECTERN_ITEM =
            ITEMS.registerSimpleBlockItem("podium_lectern", ModBlocks.PODIUM_LECTERN);

    public static final DeferredItem<BlockItem> CLASSROOM_GLOBE_ITEM =
            ITEMS.registerSimpleBlockItem("classroom_globe", ModBlocks.CLASSROOM_GLOBE);

    public static final DeferredItem<BlockItem> MODERN_STUDENT_DESK_ITEM =
            ITEMS.registerSimpleBlockItem("modern_student_desk", ModBlocks.MODERN_STUDENT_DESK);

    public static final DeferredItem<BlockItem> SCIENCE_LAB_WORKBENCH_ITEM =
            ITEMS.registerSimpleBlockItem("science_lab_workbench", ModBlocks.SCIENCE_LAB_WORKBENCH);

    public static final DeferredItem<BlockItem> COMPUTER_LAB_DESK_ROW_ITEM =
            ITEMS.registerSimpleBlockItem("computer_lab_desk_row", ModBlocks.COMPUTER_LAB_DESK_ROW);

    public static final DeferredItem<BlockItem> LIBRARY_STUDY_CARREL_ITEM =
            ITEMS.registerSimpleBlockItem("library_study_carrel", ModBlocks.LIBRARY_STUDY_CARREL);

    public static final DeferredItem<BlockItem> ROLLING_BOOK_CART_ITEM =
            ITEMS.registerSimpleBlockItem("rolling_book_cart", ModBlocks.ROLLING_BOOK_CART);

    public static final DeferredItem<FireBlanketItem> FIRE_BLANKET = ITEMS.registerItem("fire_blanket",
            props -> new FireBlanketItem(props.durability(3)));

    public static final DeferredItem<FirstAidKitItem> FIRST_AID_KIT = ITEMS.registerItem("first_aid_kit",
            props -> new FirstAidKitItem(props.durability(5)));

    public static final DeferredItem<MegaphoneItem> MEGAPHONE = ITEMS.registerItem("megaphone",
            props -> new MegaphoneItem(props.stacksTo(1)));

    public static final DeferredItem<SafetyWhistleItem> SAFETY_WHISTLE = ITEMS.registerItem("safety_whistle",
            props -> new SafetyWhistleItem(props.stacksTo(1)));

    public static final DeferredItem<FlashlightItem> FLASHLIGHT = ITEMS.registerItem("flashlight",
            props -> new FlashlightItem(props.stacksTo(1)));

    public static final DeferredItem<BlockItem> EXIT_SIGN_ITEM =
            ITEMS.registerSimpleBlockItem("exit_sign", ModBlocks.EXIT_SIGN);

    public static final DeferredItem<BlockItem> SMOKE_DETECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("smoke_detector", ModBlocks.SMOKE_DETECTOR);

    public static final DeferredItem<BlockItem> SPRINKLER_HEAD_ITEM =
            ITEMS.registerSimpleBlockItem("sprinkler_head", ModBlocks.SPRINKLER_HEAD);

    public static final DeferredItem<BlockItem> EMERGENCY_LIGHT_ITEM =
            ITEMS.registerSimpleBlockItem("emergency_light", ModBlocks.EMERGENCY_LIGHT);

    public static final DeferredItem<BlockItem> EVACUATION_MAP_ITEM =
            ITEMS.registerSimpleBlockItem("evacuation_map", ModBlocks.EVACUATION_MAP);

    public static final DeferredItem<BlockItem> FIRE_EXTINGUISHER_CABINET_ITEM =
            ITEMS.registerSimpleBlockItem("fire_extinguisher_cabinet", ModBlocks.FIRE_EXTINGUISHER_CABINET);

    public static final DeferredItem<BlockItem> ASSEMBLY_POINT_SIGN_ITEM =
            ITEMS.registerSimpleBlockItem("assembly_point_sign", ModBlocks.ASSEMBLY_POINT_SIGN);

    public static final DeferredItem<BlockItem> FIRST_AID_WALL_CABINET_ITEM =
            ITEMS.registerSimpleBlockItem("first_aid_wall_cabinet", ModBlocks.FIRST_AID_WALL_CABINET);

    public static final DeferredItem<BlockItem> FIRE_SAFETY_POSTER_ITEM =
            ITEMS.registerSimpleBlockItem("fire_safety_poster", ModBlocks.FIRE_SAFETY_POSTER);

    public static final DeferredItem<BlockItem> BLOCKED_EXIT_CLUTTER_ITEM =
            ITEMS.registerSimpleBlockItem("blocked_exit_clutter", ModBlocks.BLOCKED_EXIT_CLUTTER);

    public static final DeferredItem<BlockItem> CAFETERIA_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("cafeteria_table", ModBlocks.CAFETERIA_TABLE);

    public static final DeferredItem<BlockItem> TRAY_STACK_ITEM =
            ITEMS.registerSimpleBlockItem("cafeteria_tray_stack", ModBlocks.TRAY_STACK);

    public static final DeferredItem<BlockItem> SERVING_COUNTER_ITEM =
            ITEMS.registerSimpleBlockItem("serving_counter", ModBlocks.SERVING_COUNTER);

    public static final DeferredItem<BlockItem> CAFETERIA_MENU_BOARD_ITEM =
            ITEMS.registerSimpleBlockItem("cafeteria_menu_board", ModBlocks.CAFETERIA_MENU_BOARD);

    public static final DeferredItem<BlockItem> CONDIMENT_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("condiment_station", ModBlocks.CONDIMENT_STATION);

    public static final DeferredItem<BlockItem> CAFETERIA_TRASH_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("cafeteria_trash_bin", ModBlocks.CAFETERIA_TRASH_BIN);

    public static final DeferredItem<BlockItem> SODA_FOUNTAIN_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem("soda_fountain_machine", ModBlocks.SODA_FOUNTAIN_MACHINE);

    public static final DeferredItem<BlockItem> CAFETERIA_STOOL_ITEM =
            ITEMS.registerSimpleBlockItem("cafeteria_stool", ModBlocks.CAFETERIA_STOOL);

    public static final DeferredItem<BlockItem> SALAD_BAR_ITEM =
            ITEMS.registerSimpleBlockItem("salad_bar", ModBlocks.SALAD_BAR);

    public static final DeferredItem<BlockItem> SNACK_VENDING_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem("snack_vending_machine", ModBlocks.SNACK_VENDING_MACHINE);

    public static final DeferredItem<BlockItem> KITCHEN_PREP_COUNTER_ITEM =
            ITEMS.registerSimpleBlockItem("kitchen_prep_counter", ModBlocks.KITCHEN_PREP_COUNTER);

    public static final DeferredItem<BlockItem> DISHWASHING_SINK_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("dishwashing_sink_station", ModBlocks.DISHWASHING_SINK_STATION);

    public static final DeferredItem<BlockItem> BEVERAGE_JUICE_DISPENSER_ITEM =
            ITEMS.registerSimpleBlockItem("beverage_juice_dispenser", ModBlocks.BEVERAGE_JUICE_DISPENSER);

    public static final DeferredItem<BlockItem> CUTLERY_NAPKIN_CADDY_ITEM =
            ITEMS.registerSimpleBlockItem("cutlery_napkin_caddy", ModBlocks.CUTLERY_NAPKIN_CADDY);

    public static final DeferredItem<BlockItem> SERVING_HATCH_WINDOW_ITEM =
            ITEMS.registerSimpleBlockItem("serving_hatch_window", ModBlocks.SERVING_HATCH_WINDOW);

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
        ALL_ITEM_MAP.put("npc_prof_instructor_david", NPC_PROF_INSTRUCTOR_DAVID);
        ALL_ITEM_MAP.put("npc_prof_principal_brown", NPC_PROF_PRINCIPAL_BROWN);
        ALL_ITEM_MAP.put("npc_prof_professor_baldwin", NPC_PROF_PROFESSOR_BALDWIN);
        ALL_ITEM_MAP.put("npc_prof_professor_kevin", NPC_PROF_PROFESSOR_KEVIN);
        ALL_ITEM_MAP.put("npc_student_goldy", NPC_STUDENT_GOLDY);
        ALL_ITEM_MAP.put("npc_student_harvey", NPC_STUDENT_HARVEY);
        ALL_ITEM_MAP.put("npc_student_jenny", NPC_STUDENT_JENNY);
        ALL_ITEM_MAP.put("npc_student_kaefla", NPC_STUDENT_KAEFLA);
        ALL_ITEM_MAP.put("npc_student_karl", NPC_STUDENT_KARL);
        ALL_ITEM_MAP.put("npc_student_kath", NPC_STUDENT_KATH);
        ALL_ITEM_MAP.put("npc_student_kelly", NPC_STUDENT_KELLY);
        ALL_ITEM_MAP.put("npc_student_nath", NPC_STUDENT_NATH);
        ALL_ITEM_MAP.put("npc_student_necookie", NPC_STUDENT_NECOOKIE);
        ALL_ITEM_MAP.put("npc_student_nell", NPC_STUDENT_NELL);
        ALL_ITEM_MAP.put("npc_student_princess", NPC_STUDENT_PRINCESS);
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
        ALL_ITEM_MAP.put("glowing_oak_planks", GLOWING_OAK_PLANKS_ITEM);
        ALL_ITEM_MAP.put("court_line", COURT_LINE_ITEM);
        ALL_ITEM_MAP.put("badminton_net_post", BADMINTON_NET_POST_ITEM);
        ALL_ITEM_MAP.put("badminton_net_mesh", BADMINTON_NET_MESH_ITEM);
        ALL_ITEM_MAP.put("basketball_hoop_post", BASKETBALL_HOOP_POST_ITEM);
        ALL_ITEM_MAP.put("basketball_pole", BASKETBALL_POLE_ITEM);
        ALL_ITEM_MAP.put("basketball_hoop", BASKETBALL_HOOP_ITEM);
        ALL_ITEM_MAP.put("basketball", BASKETBALL);
        ALL_ITEM_MAP.put("teachers_desk", TEACHERS_DESK_ITEM);
        ALL_ITEM_MAP.put("armchair_desk", ARMCHAIR_DESK_ITEM);
        ALL_ITEM_MAP.put("tall_bookshelf", TALL_BOOKSHELF_ITEM);
        ALL_ITEM_MAP.put("philippine_flag_stand", PHILIPPINE_FLAG_STAND_ITEM);
        ALL_ITEM_MAP.put("trophy_cabinet", TROPHY_CABINET_ITEM);
        ALL_ITEM_MAP.put("water_dispenser", WATER_DISPENSER_ITEM);
        ALL_ITEM_MAP.put("wall_clock", WALL_CLOCK_ITEM);
        ALL_ITEM_MAP.put("blackboard", BLACKBOARD_ITEM);
        ALL_ITEM_MAP.put("podium_lectern", PODIUM_LECTERN_ITEM);
        ALL_ITEM_MAP.put("classroom_globe", CLASSROOM_GLOBE_ITEM);
        ALL_ITEM_MAP.put("modern_student_desk", MODERN_STUDENT_DESK_ITEM);
        ALL_ITEM_MAP.put("science_lab_workbench", SCIENCE_LAB_WORKBENCH_ITEM);
        ALL_ITEM_MAP.put("computer_lab_desk_row", COMPUTER_LAB_DESK_ROW_ITEM);
        ALL_ITEM_MAP.put("library_study_carrel", LIBRARY_STUDY_CARREL_ITEM);
        ALL_ITEM_MAP.put("rolling_book_cart", ROLLING_BOOK_CART_ITEM);
        ALL_ITEM_MAP.put("kitchen_prep_counter", KITCHEN_PREP_COUNTER_ITEM);
        ALL_ITEM_MAP.put("dishwashing_sink_station", DISHWASHING_SINK_STATION_ITEM);
        ALL_ITEM_MAP.put("beverage_juice_dispenser", BEVERAGE_JUICE_DISPENSER_ITEM);
        ALL_ITEM_MAP.put("cutlery_napkin_caddy", CUTLERY_NAPKIN_CADDY_ITEM);
        ALL_ITEM_MAP.put("serving_hatch_window", SERVING_HATCH_WINDOW_ITEM);
        ALL_ITEM_MAP.put("fire_extinguisher_cabinet", FIRE_EXTINGUISHER_CABINET_ITEM);
        ALL_ITEM_MAP.put("assembly_point_sign", ASSEMBLY_POINT_SIGN_ITEM);
        ALL_ITEM_MAP.put("first_aid_wall_cabinet", FIRST_AID_WALL_CABINET_ITEM);
        ALL_ITEM_MAP.put("fire_safety_poster", FIRE_SAFETY_POSTER_ITEM);
        ALL_ITEM_MAP.put("blocked_exit_clutter", BLOCKED_EXIT_CLUTTER_ITEM);
        ALL_ITEM_MAP.put("fire_blanket", FIRE_BLANKET);
        ALL_ITEM_MAP.put("first_aid_kit", FIRST_AID_KIT);
        ALL_ITEM_MAP.put("megaphone", MEGAPHONE);
        ALL_ITEM_MAP.put("safety_whistle", SAFETY_WHISTLE);
        ALL_ITEM_MAP.put("flashlight", FLASHLIGHT);
        ALL_ITEM_MAP.put("exit_sign", EXIT_SIGN_ITEM);
        ALL_ITEM_MAP.put("smoke_detector", SMOKE_DETECTOR_ITEM);
        ALL_ITEM_MAP.put("sprinkler_head", SPRINKLER_HEAD_ITEM);
        ALL_ITEM_MAP.put("emergency_light", EMERGENCY_LIGHT_ITEM);
        ALL_ITEM_MAP.put("evacuation_map", EVACUATION_MAP_ITEM);
        ALL_ITEM_MAP.put("cafeteria_table", CAFETERIA_TABLE_ITEM);
        ALL_ITEM_MAP.put("cafeteria_tray_stack", TRAY_STACK_ITEM);
        ALL_ITEM_MAP.put("serving_counter", SERVING_COUNTER_ITEM);
        ALL_ITEM_MAP.put("cafeteria_menu_board", CAFETERIA_MENU_BOARD_ITEM);
        ALL_ITEM_MAP.put("condiment_station", CONDIMENT_STATION_ITEM);
        ALL_ITEM_MAP.put("cafeteria_trash_bin", CAFETERIA_TRASH_BIN_ITEM);
        ALL_ITEM_MAP.put("soda_fountain_machine", SODA_FOUNTAIN_MACHINE_ITEM);
        ALL_ITEM_MAP.put("cafeteria_stool", CAFETERIA_STOOL_ITEM);
        ALL_ITEM_MAP.put("salad_bar", SALAD_BAR_ITEM);
        ALL_ITEM_MAP.put("snack_vending_machine", SNACK_VENDING_MACHINE_ITEM);
        ALL_ITEM_MAP.putAll(HAZARD_ITEM_MAP);
    }

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
