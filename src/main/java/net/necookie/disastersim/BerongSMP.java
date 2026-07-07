package net.necookie.disastersim;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BulletinBoardBlock;
import net.necookie.disastersim.block.CeilingFanBlock;
import net.necookie.disastersim.block.ChairBlock;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.block.ComputerTableBlock;
import net.necookie.disastersim.block.DrawersBlock;
import net.necookie.disastersim.block.FilingCabinetBlock;
import net.necookie.disastersim.block.FireAlarmBlock;
import net.necookie.disastersim.block.LightBulbBlock;
import net.necookie.disastersim.block.LockerBlock;
import net.necookie.disastersim.block.SinkBlock;
import net.necookie.disastersim.block.TableBlock;
import net.necookie.disastersim.block.ToiletBlock;
import net.necookie.disastersim.block.TrashCanBlock;
import net.necookie.disastersim.block.FireHoseCabinetBlock;
import net.necookie.disastersim.block.WhiteboardBlock;
import net.necookie.disastersim.common.hazard.*;
import net.minecraft.world.level.storage.LevelData;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.item.NpcSpawnerItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.necookie.disastersim.command.ModCommands;
import net.necookie.disastersim.common.structure.LobbyManager;
import net.necookie.disastersim.common.structure.AcademyBuildingManager;
import net.necookie.disastersim.common.structure.TutorialLobbyManager;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.item.CO2ExtinguisherItem;
import net.necookie.disastersim.item.FireExtinguisherItem;
import net.necookie.disastersim.item.HazardWandItem;
import net.necookie.disastersim.item.WetChemicalExtinguisherItem;
import net.necookie.disastersim.network.AcademyCompassPayload;
import net.necookie.disastersim.network.AcademyShakePayload;
import net.necookie.disastersim.network.AcademyStatusPayload;
import net.necookie.disastersim.network.DropAndRollPayload;
import net.necookie.disastersim.network.SimulationStatusPayload;
import net.necookie.disastersim.network.TutorialStatusPayload;
import net.necookie.disastersim.common.player.DuckCoverHoldManager;
import net.necookie.disastersim.session.SessionManager;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Main entry point for the BerongSMP mod.
 * This class handles the registration of blocks, items, and other game elements,
 * as well as setting up common mod logic and server-side initialization.
 */
@Mod(BerongSMP.MODID)
public class BerongSMP {
    /** The unique identifier for the mod. */
    public static final String MODID = "berongsmp";
    
    /** Logger instance for mod-specific logging. */
    public static final Logger LOGGER = LogUtils.getLogger();
    
    /** Deferred Register for Blocks. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    
    /** Deferred Register for Items. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    
    /** Deferred Register for Creative Mode Tabs. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Deferred Register for Entity Types. */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    /** Deferred Register for Sound Events. */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    /** Deferred Register for Attachment Types (per-entity synced client-visible state). */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    /**
     * Ticks remaining in a player's drop-and-roll "dropped" window (0 = not dropped), mirroring
     * {@code DropAndRollManager.droppedTicksRemaining}. Auto-synced to every client tracking the
     * player (including their own client) so {@code DropAndRollRenderModifier} can drive the
     * crouch/roll animation — purely cosmetic, never touches the real entity Pose/hitbox.
     */
    public static final java.util.function.Supplier<AttachmentType<Integer>> DROPPED_TICKS =
            ATTACHMENT_TYPES.register("dropped_ticks",
                    () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());

    // ── NPC entity ───────────────────────────────────────────────────────────

    /** Single entity type shared by all instructor/character NPCs; NpcType stored in NBT selects the skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<CustomNpcEntity>> CUSTOM_NPC =
            ENTITY_TYPES.register("custom_npc", id ->
                    EntityType.Builder.<CustomNpcEntity>of(CustomNpcEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    // ── NPC spawner items (one per instructor) ───────────────────────────────

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

    /** Fire alarm ringing sound — loops via scheduled block ticks while ACTIVATED=true. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_ALARM_RING =
            SOUND_EVENTS.register("block.fire_alarm.ring",
                    () -> SoundEvent.createVariableRangeEvent(
                            Identifier.fromNamespaceAndPath(MODID, "block.fire_alarm.ring")));

    /** Example block registration. */
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));

    /** Example block item registration. */
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    /** Computer/terminal block — can be set LIT=true for Class C electrical fire scenarios. */
    public static final DeferredBlock<ComputerBlock> COMPUTER = BLOCKS.registerBlock("computer",
            ComputerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> {
                        if (state.getValue(ComputerBlock.BURNING)) return 15;
                        if (state.getValue(BlockStateProperties.LIT)) return 7;
                        return 0;
                    }));

    /** Computer block item. */
    public static final DeferredItem<BlockItem> COMPUTER_ITEM = ITEMS.registerSimpleBlockItem("computer", COMPUTER);

    /** Wall-mounted fire alarm pull station — activates during FIRE simulations, logs fire_alarm_activate telemetry. */
    public static final DeferredBlock<FireAlarmBlock> FIRE_ALARM_BLOCK = BLOCKS.registerBlock(
            "fire_alarm",
            FireAlarmBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(FireAlarmBlock.ACTIVATED) ? 7 : 0));

    /** Fire alarm block item. */
    public static final DeferredItem<BlockItem> FIRE_ALARM_ITEM =
            ITEMS.registerSimpleBlockItem("fire_alarm", FIRE_ALARM_BLOCK);

    // ── Furniture blocks ─────────────────────────────────────────────────────

    /** Classroom whiteboard — wall-mounted flat panel with marker tray. */
    public static final DeferredBlock<WhiteboardBlock> WHITEBOARD = BLOCKS.registerBlock("whiteboard",
            WhiteboardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> WHITEBOARD_ITEM = ITEMS.registerSimpleBlockItem("whiteboard", WHITEBOARD);

    /** Wall-mounted fire hose reel cabinet — decorative safety-equipment prop, not a hazard. */
    public static final DeferredBlock<FireHoseCabinetBlock> FIRE_HOSE_CABINET = BLOCKS.registerBlock("fire_hose_cabinet",
            FireHoseCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> FIRE_HOSE_CABINET_ITEM = ITEMS.registerSimpleBlockItem("fire_hose_cabinet", FIRE_HOSE_CABINET);

    /** Toilet block — ceramic basin+tank; right-click to flush. */
    public static final DeferredBlock<ToiletBlock> TOILET = BLOCKS.registerBlock("toilet",
            ToiletBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> TOILET_ITEM = ITEMS.registerSimpleBlockItem("toilet", TOILET);

    /** Wall-mounted sink with iron faucet and handles; right-click for water sound. */
    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink",
            SinkBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> SINK_ITEM = ITEMS.registerSimpleBlockItem("sink", SINK);

    /** Office chest-of-drawers — dark oak body with birch drawer fronts and iron handles. */
    public static final DeferredBlock<DrawersBlock> DRAWERS = BLOCKS.registerBlock("drawers",
            DrawersBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> DRAWERS_ITEM = ITEMS.registerSimpleBlockItem("drawers", DRAWERS);

    /** Flammable oak computer desk with 4 legs and a back cable-management panel. */
    public static final DeferredBlock<ComputerTableBlock> COMPUTER_TABLE = BLOCKS.registerBlock("computer_table",
            ComputerTableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> COMPUTER_TABLE_ITEM = ITEMS.registerSimpleBlockItem("computer_table", COMPUTER_TABLE);

    /**
     * Extendable study/library table, 2 blocks tall (HALF=LOWER/UPPER like a vanilla tall
     * flower) with a walkable kneehole underneath — the first placeable prop that gives players
     * a real "duck and cover" shelter (see {@code DuckCoverHoldManager}). Lining tables up
     * side by side (NORTH/SOUTH/EAST/WEST connections) merges them into one continuous tabletop.
     */
    public static final DeferredBlock<TableBlock> TABLE = BLOCKS.registerBlock("table",
            TableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem("table", TABLE);

    /** Dark oak classroom/office chair with gray cushion seat and backrest. */
    public static final DeferredBlock<ChairBlock> CHAIR = BLOCKS.registerBlock("chair",
            ChairBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> CHAIR_ITEM = ITEMS.registerSimpleBlockItem("chair", CHAIR);

    /** Tall metal filing cabinet with 2 drawers, label slots, and pull handles. */
    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = BLOCKS.registerBlock("filing_cabinet",
            FilingCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> FILING_CABINET_ITEM = ITEMS.registerSimpleBlockItem("filing_cabinet", FILING_CABINET);

    /** Tall metal school/office locker with vents, door seam, handle, and gold lock. */
    public static final DeferredBlock<LockerBlock> LOCKER = BLOCKS.registerBlock("locker",
            LockerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> LOCKER_ITEM = ITEMS.registerSimpleBlockItem("locker", LOCKER);

    /** Small cylindrical trash can — no facing, symmetric, open-top. */
    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = BLOCKS.registerBlock("trash_can",
            TrashCanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> TRASH_CAN_ITEM = ITEMS.registerSimpleBlockItem("trash_can", TRASH_CAN);

    /** Wall-mounted cork bulletin board with pinned paper slips. */
    public static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = BLOCKS.registerBlock("bulletin_board",
            BulletinBoardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> BULLETIN_BOARD_ITEM = ITEMS.registerSimpleBlockItem("bulletin_board", BULLETIN_BOARD);

    /** Ceiling fan — motor housing + 4 blades + glowstone light bowl; symmetric/no facing. */
    public static final DeferredBlock<CeilingFanBlock> CEILING_FAN = BLOCKS.registerBlock("ceiling_fan",
            CeilingFanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(s -> 5));
    public static final DeferredItem<BlockItem> CEILING_FAN_ITEM = ITEMS.registerSimpleBlockItem("ceiling_fan", CEILING_FAN);

    /** Full-cube glowing ceiling tile — max-brightness (level 15, vanilla cap) light source; tiles seamlessly with no gaps or visible seams when placed edge-to-edge, like a lit ceiling carpet. */
    public static final DeferredBlock<LightBulbBlock> LIGHT_BULB = BLOCKS.registerBlock("light_bulb",
            LightBulbBlock::new,
            () -> Block.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .lightLevel(s -> 15));
    public static final DeferredItem<BlockItem> LIGHT_BULB_ITEM = ITEMS.registerSimpleBlockItem("light_bulb", LIGHT_BULB);

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
                    Identifier.fromNamespaceAndPath(MODID, "firefighter_uniform"));

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

    /** Creative tab: simulation tools and interactive blocks. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIM_TAB = CREATIVE_MODE_TABS.register("sim_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.simulation"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FIRE_EXTINGUISHER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FIRE_EXTINGUISHER.get());
                output.accept(CO2_EXTINGUISHER.get());
                output.accept(WET_CHEMICAL_EXTINGUISHER.get());
                output.accept(HAZARD_WAND.get());
                output.accept(FIREFIGHTER_HELMET.get());
                output.accept(FIREFIGHTER_COAT.get());
                output.accept(FIREFIGHTER_PANTS.get());
                output.accept(FIREFIGHTER_BOOTS.get());
                output.accept(COMPUTER_ITEM.get());
                output.accept(FIRE_ALARM_ITEM.get());
                output.accept(NPC_SGT_REYES.get());
                output.accept(NPC_SGT_SANTOS.get());
                output.accept(NPC_OFFICER_CRUZ.get());
                output.accept(NPC_CAPT_MORFE.get());
                output.accept(NPC_SECURITY_TUAZON.get());
                output.accept(NPC_DM_ORLANDA.get());
                output.accept(NPC_NECOOKIE.get());
                output.accept(NPC_SIR_BOOKMARK.get());
                output.accept(NPC_STUDENT.get());
            }).build());

    /** Creative tab: furniture and props for building scenarios. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURN_TAB = CREATIVE_MODE_TABS.register("furn_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.furniture"))
            .withTabsBefore(SIM_TAB.getKey())
            .icon(() -> CHAIR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WHITEBOARD_ITEM.get());
                output.accept(FIRE_HOSE_CABINET_ITEM.get());
                output.accept(BULLETIN_BOARD_ITEM.get());
                output.accept(COMPUTER_TABLE_ITEM.get());
                output.accept(TABLE_ITEM.get());
                output.accept(CHAIR_ITEM.get());
                output.accept(DRAWERS_ITEM.get());
                output.accept(FILING_CABINET_ITEM.get());
                output.accept(LOCKER_ITEM.get());
                output.accept(TOILET_ITEM.get());
                output.accept(SINK_ITEM.get());
                output.accept(TRASH_CAN_ITEM.get());
                output.accept(CEILING_FAN_ITEM.get());
                output.accept(LIGHT_BULB_ITEM.get());
            }).build());

    // ── Hazard prop blocks (20 items — populated below as blocks are declared) ──

    /** Maps hazard block registry name → its DeferredItem for the /item hazard command. */
    public static final LinkedHashMap<String, DeferredItem<BlockItem>> HAZARD_ITEM_MAP = new LinkedHashMap<>();

    // ── Classroom Zone (5 blocks) ─────────────────────────────────────────────

    /** Plastic waste bin — emits smoke when has_vape=true (hazardous). */
    public static final DeferredBlock<PlasticTrashBinBlock> PLASTIC_TRASH_BIN = BLOCKS.registerBlock(
            "plastic_trash_bin", PlasticTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(0.5f, 1.0f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> PLASTIC_TRASH_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("plastic_trash_bin", PLASTIC_TRASH_BIN);
    static { HAZARD_ITEM_MAP.put("plastic_trash_bin", PLASTIC_TRASH_BIN_ITEM); }

    /** Overloaded daisy-chain extension cord — electric sparks when overloaded=true. */
    public static final DeferredBlock<DaisyChainExtensionBlock> DAISY_CHAIN_EXTENSION = BLOCKS.registerBlock(
            "daisy_chain_extension", DaisyChainExtensionBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.2f, 0.5f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> DAISY_CHAIN_EXTENSION_ITEM =
            ITEMS.registerSimpleBlockItem("daisy_chain_extension", DAISY_CHAIN_EXTENSION);
    static { HAZARD_ITEM_MAP.put("daisy_chain_extension", DAISY_CHAIN_EXTENSION_ITEM); }

    /** Floor sawdust accumulation layer — emits ash particles at accumulation >= 3. */
    public static final DeferredBlock<WoodshopSawdustLayerBlock> WOODSHOP_SAWDUST_LAYER = BLOCKS.registerBlock(
            "woodshop_sawdust_layer", WoodshopSawdustLayerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.1f, 0.1f).sound(SoundType.SAND).noOcclusion());
    public static final DeferredItem<BlockItem> WOODSHOP_SAWDUST_LAYER_ITEM =
            ITEMS.registerSimpleBlockItem("woodshop_sawdust_layer", WOODSHOP_SAWDUST_LAYER);
    static { HAZARD_ITEM_MAP.put("woodshop_sawdust_layer", WOODSHOP_SAWDUST_LAYER_ITEM); }

    /** Stage/theatre spotlight — overheating housing emits flame and smoke when hazardous. */
    public static final DeferredBlock<StageSpotlightBlock> STAGE_SPOTLIGHT = BLOCKS.registerBlock(
            "stage_spotlight", StageSpotlightBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));
    public static final DeferredItem<BlockItem> STAGE_SPOTLIGHT_ITEM =
            ITEMS.registerSimpleBlockItem("stage_spotlight", STAGE_SPOTLIGHT);
    static { HAZARD_ITEM_MAP.put("stage_spotlight", STAGE_SPOTLIGHT_ITEM); }

    /** Stack of flammable archive document boxes — fire proximity raises hazard. */
    public static final DeferredBlock<ArchiveBoxStackBlock> ARCHIVE_BOX_STACK = BLOCKS.registerBlock(
            "archive_box_stack", ArchiveBoxStackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0f, 1.0f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> ARCHIVE_BOX_STACK_ITEM =
            ITEMS.registerSimpleBlockItem("archive_box_stack", ARCHIVE_BOX_STACK);
    static { HAZARD_ITEM_MAP.put("archive_box_stack", ARCHIVE_BOX_STACK_ITEM); }

    /** Desktop PC tower with dust-clogged vents — overheats and emits smoke when hazardous. */
    public static final DeferredBlock<DustChokedPcBlock> DUST_CHOKED_PC = BLOCKS.registerBlock(
            "dust_choked_pc", DustChokedPcBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 3 : 0));
    public static final DeferredItem<BlockItem> DUST_CHOKED_PC_ITEM =
            ITEMS.registerSimpleBlockItem("dust_choked_pc", DUST_CHOKED_PC);
    static { HAZARD_ITEM_MAP.put("dust_choked_pc", DUST_CHOKED_PC_ITEM); }

    /** Rolling Chromebook/laptop charging cart — overloaded outlets spark when hazardous. */
    public static final DeferredBlock<ChargingCartBlock> CHARGING_CART = BLOCKS.registerBlock(
            "charging_cart", ChargingCartBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 5 : 0));
    public static final DeferredItem<BlockItem> CHARGING_CART_ITEM =
            ITEMS.registerSimpleBlockItem("charging_cart", CHARGING_CART);
    static { HAZARD_ITEM_MAP.put("charging_cart", CHARGING_CART_ITEM); }

    /** Frayed AV/console wire on the floor — exposed copper arcs blue sparks when hazardous. */
    public static final DeferredBlock<FrayedConsoleWireBlock> FRAYED_CONSOLE_WIRE = BLOCKS.registerBlock(
            "frayed_console_wire", FrayedConsoleWireBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> FRAYED_CONSOLE_WIRE_ITEM =
            ITEMS.registerSimpleBlockItem("frayed_console_wire", FRAYED_CONSOLE_WIRE);
    static { HAZARD_ITEM_MAP.put("frayed_console_wire", FRAYED_CONSOLE_WIRE_ITEM); }

    /** Vending machine with shorted compressor — smokes from back vents when hazardous. */
    public static final DeferredBlock<MalfunctioningVendingBlock> MALFUNCTIONING_VENDING = BLOCKS.registerBlock(
            "malfunctioning_vending", MalfunctioningVendingBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> MALFUNCTIONING_VENDING_ITEM =
            ITEMS.registerSimpleBlockItem("malfunctioning_vending", MALFUNCTIONING_VENDING);
    static { HAZARD_ITEM_MAP.put("malfunctioning_vending", MALFUNCTIONING_VENDING_ITEM); }

    /** Ceiling-mounted projector with failed cooling fan — overheats and smokes. */
    public static final DeferredBlock<CeilingProjectorBlock> CEILING_PROJECTOR = BLOCKS.registerBlock(
            "ceiling_projector", CeilingProjectorBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 7 : 0));
    public static final DeferredItem<BlockItem> CEILING_PROJECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("ceiling_projector", CEILING_PROJECTOR);
    static { HAZARD_ITEM_MAP.put("ceiling_projector", CEILING_PROJECTOR_ITEM); }

    /** Swollen Li-ion phone battery left on desk — thermal runaway risk; cyan soul-flame gas when hazardous. */
    public static final DeferredBlock<SwollenPhoneBatteryBlock> SWOLLEN_PHONE_BATTERY = BLOCKS.registerBlock(
            "swollen_phone_battery", SwollenPhoneBatteryBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5f, 0.5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> SWOLLEN_PHONE_BATTERY_ITEM =
            ITEMS.registerSimpleBlockItem("swollen_phone_battery", SWOLLEN_PHONE_BATTERY);
    static { HAZARD_ITEM_MAP.put("swollen_phone_battery", SWOLLEN_PHONE_BATTERY_ITEM); }

    /** Damaged LiPo battery pack (drone/RC) — punctured cells off-gas smoke when hazardous. */
    public static final DeferredBlock<DamagedLipoPackBlock> DAMAGED_LIPO_PACK = BLOCKS.registerBlock(
            "damaged_lipo_pack", DamagedLipoPackBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> DAMAGED_LIPO_PACK_ITEM =
            ITEMS.registerSimpleBlockItem("damaged_lipo_pack", DAMAGED_LIPO_PACK);
    static { HAZARD_ITEM_MAP.put("damaged_lipo_pack", DAMAGED_LIPO_PACK_ITEM); }

    /** Iron locker with a vape device inside — sparks and smoke leak from vent slot when hazardous. */
    public static final DeferredBlock<VapeInIronLockerBlock> VAPE_IN_IRON_LOCKER = BLOCKS.registerBlock(
            "vape_in_iron_locker", VapeInIronLockerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> VAPE_IN_IRON_LOCKER_ITEM =
            ITEMS.registerSimpleBlockItem("vape_in_iron_locker", VAPE_IN_IRON_LOCKER);
    static { HAZARD_ITEM_MAP.put("vape_in_iron_locker", VAPE_IN_IRON_LOCKER_ITEM); }

    /** PA/public-address backup amp rack — faulty capacitors spark and glow when hazardous. */
    public static final DeferredBlock<PaSystemBackupBlock> PA_SYSTEM_BACKUP = BLOCKS.registerBlock(
            "pa_system_backup", PaSystemBackupBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 8 : 0));
    public static final DeferredItem<BlockItem> PA_SYSTEM_BACKUP_ITEM =
            ITEMS.registerSimpleBlockItem("pa_system_backup", PA_SYSTEM_BACKUP);
    static { HAZARD_ITEM_MAP.put("pa_system_backup", PA_SYSTEM_BACKUP_ITEM); }

    /** Smartboard power inverter — roof leak drips on live electronics when hazardous. */
    public static final DeferredBlock<SmartboardInverterBlock> SMARTBOARD_INVERTER = BLOCKS.registerBlock(
            "smartboard_inverter", SmartboardInverterBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> SMARTBOARD_INVERTER_ITEM =
            ITEMS.registerSimpleBlockItem("smartboard_inverter", SMARTBOARD_INVERTER);
    static { HAZARD_ITEM_MAP.put("smartboard_inverter", SMARTBOARD_INVERTER_ITEM); }

    /** Stove with a grease pan left unattended — grease fire erupts when hazardous. */
    public static final DeferredBlock<UnattendedGreasePanBlock> UNATTENDED_GREASE_PAN = BLOCKS.registerBlock(
            "unattended_grease_pan", UnattendedGreasePanBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 10 : 0));
    public static final DeferredItem<BlockItem> UNATTENDED_GREASE_PAN_ITEM =
            ITEMS.registerSimpleBlockItem("unattended_grease_pan", UNATTENDED_GREASE_PAN);
    static { HAZARD_ITEM_MAP.put("unattended_grease_pan", UNATTENDED_GREASE_PAN_ITEM); }

    /** Kitchen range hood with clogged grease filters — backflow smoke when hazardous. */
    public static final DeferredBlock<GreaseCloggedHoodBlock> GREASE_CLOGGED_HOOD = BLOCKS.registerBlock(
            "grease_clogged_hood", GreaseCloggedHoodBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> GREASE_CLOGGED_HOOD_ITEM =
            ITEMS.registerSimpleBlockItem("grease_clogged_hood", GREASE_CLOGGED_HOOD);
    static { HAZARD_ITEM_MAP.put("grease_clogged_hood", GREASE_CLOGGED_HOOD_ITEM); }

    /** Kitchen waste bin with oil-soaked contaminated rags — self-heating rags emit steam when hazardous. */
    public static final DeferredBlock<ContaminatedKitchenBinBlock> CONTAMINATED_KITCHEN_BIN = BLOCKS.registerBlock(
            "contaminated_kitchen_bin", ContaminatedKitchenBinBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f, 1.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> CONTAMINATED_KITCHEN_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("contaminated_kitchen_bin", CONTAMINATED_KITCHEN_BIN);
    static { HAZARD_ITEM_MAP.put("contaminated_kitchen_bin", CONTAMINATED_KITCHEN_BIN_ITEM); }

    /** Panini press with jammed lid and burning food — smoking when hazardous. */
    public static final DeferredBlock<JammedPaniniPressBlock> JAMMED_PANINI_PRESS = BLOCKS.registerBlock(
            "jammed_panini_press", JammedPaniniPressBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 2.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> JAMMED_PANINI_PRESS_ITEM =
            ITEMS.registerSimpleBlockItem("jammed_panini_press", JAMMED_PANINI_PRESS);
    static { HAZARD_ITEM_MAP.put("jammed_panini_press", JAMMED_PANINI_PRESS_ITEM); }

    /** Commercial deep fryer — overheated oil ignites and erupts smoke/flame when hazardous. */
    public static final DeferredBlock<CommercialDeepFryerBlock> COMMERCIAL_DEEP_FRYER = BLOCKS.registerBlock(
            "commercial_deep_fryer", CommercialDeepFryerBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? 12 : 0));
    public static final DeferredItem<BlockItem> COMMERCIAL_DEEP_FRYER_ITEM =
            ITEMS.registerSimpleBlockItem("commercial_deep_fryer", COMMERCIAL_DEEP_FRYER);
    static { HAZARD_ITEM_MAP.put("commercial_deep_fryer", COMMERCIAL_DEEP_FRYER_ITEM); }

    /** Creative tab: all 20 hazard prop blocks for the simulation building. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAZARD_TAB = CREATIVE_MODE_TABS.register("hazards_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.hazards"))
            .withTabsBefore(FURN_TAB.getKey())
            .icon(() -> DAISY_CHAIN_EXTENSION_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> HAZARD_ITEM_MAP.values().forEach(i -> output.accept(i.get())))
            .build());

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
        ALL_ITEM_MAP.putAll(HAZARD_ITEM_MAP);
    }

    /**
     * Constructor for BerongSMP. Registers registers and listeners to the mod event bus.
     *
     * <p>NeoForge has two separate event buses:
     * <ul>
     *   <li>{@code modEventBus} — fires mod lifecycle events (setup, registration, client setup).
     *       Only this mod's classes listen here.</li>
     *   <li>{@code NeoForge.EVENT_BUS} — fires runtime game events (server start, player join,
     *       block interact, ticks). Shared across all mods.</li>
     * </ul>
     *
     * @param modEventBus The event bus for mod-specific lifecycle events.
     * @param modContainer The container for this mod (holds config, extension points).
     */
    public BerongSMP(IEventBus modEventBus, ModContainer modContainer) {
        // Wire up our common setup listener so it runs during FML's common setup phase,
        // which fires after all registries are filled but before the server/client starts.
        modEventBus.addListener(this::commonSetup);

        // DeferredRegisters batch-register objects (blocks, items, tabs) into the correct
        // vanilla registries when NeoForge fires the matching RegistryEvent.
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributes);

        // SimulationStatusPayload registers its own network channel via @SubscribeEvent
        // on the mod bus — it must be registered here so NeoForge picks it up.
        modEventBus.register(SimulationStatusPayload.class);
        modEventBus.register(TutorialStatusPayload.class);
        modEventBus.register(DropAndRollPayload.class);
        modEventBus.register(AcademyStatusPayload.class);
        modEventBus.register(AcademyCompassPayload.class);
        modEventBus.register(AcademyShakePayload.class);

        // BerongSMPClient is annotated @Mod(dist = CLIENT) so it only loads on the
        // physical client, keeping server JARs free of client-only Minecraft classes.
        modEventBus.register(BerongSMPClient.class);

        // NOTE: KeyMappings is intentionally NOT registered here. It holds a real KeyMapping
        // field, a client-only Minecraft type — registering it from this common constructor
        // (which runs on both distributions) would crash a dedicated server. It's registered
        // from BerongSMPClient's own constructor instead, which is dist-gated by @Mod(dist=CLIENT)
        // and therefore never even loaded on a dedicated server.

        // Register 'this' on the global runtime bus so @SubscribeEvent methods in this
        // class (e.g., onServerStarting) receive game events.
        NeoForge.EVENT_BUS.register(this);

        // RegisterCommandsEvent fires on the runtime bus, so we attach it separately.
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Shut down the session manager and flush pending DB writes on server stop.
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // BuildCreativeModeTabContentsEvent lets us inject items into vanilla creative tabs.
        modEventBus.addListener(this::addCreative);

        // Load berongsmp-common.toml and bind it to our Config class.
        // COMMON type means the file lives server-side; values sync to clients on join.
        modContainer.registerConfig(ModConfig.Type.COMMON, net.necookie.disastersim.Config.SPEC);
    }

    /**
     * Delegates command registration to {@link ModCommands}.
     * This fires before the server opens for connections, so all commands are available
     * from the first tick.
     *
     * @param event Provides the Brigadier {@link com.mojang.brigadier.CommandDispatcher}
     *              that maps command literals to execution logic.
     */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    /**
     * Common setup logic that runs on both the physical client and dedicated server.
     * This phase is the right place for cross-side initialisation that doesn't depend
     * on the world being loaded (e.g., capability registration, recipe unlocking).
     *
     * @param event The FML common setup event (enqueued, not immediate).
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        // Wood furniture flammability is handled via IBlockExtension overrides in each block class.

        // Force DuckCoverHoldManager to class-load so its static TickScheduler.register block
        // actually runs — nothing else in the codebase calls real code on that class, only
        // Javadoc {@code} mentions, which the JVM doesn't count. Without this, the crawl-under-
        // table pose-shrink assist (and duck/cover/hold tracking generally) silently never ticks.
        DuckCoverHoldManager.bootstrap();
    }

    /**
     * Injects mod items into vanilla creative mode tabs when NeoForge rebuilds
     * the tab contents. The example block is added to the Building Blocks tab
     * as a developer reference; production content should use the mod's own tab.
     *
     * @param event Provides the tab key and an output list to append items to.
     */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Only inject into the vanilla Building Blocks tab
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    /** Registers attribute defaults for the custom NPC entity (every {@code Mob} EntityType needs them). */
    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CUSTOM_NPC.get(), CustomNpcEntity.createAttributes().build());
    }

    /**
     * Item frames always return their item to the player's inventory, regardless of the
     * doEntityDrops gamerule.  Vanilla ItemFrame.hurt() skips the drop when doEntityDrops=false,
     * silently deleting the item.  We intercept the attack here, handle the removal ourselves
     * with a forced inventory-add, and cancel the event so vanilla logic doesn't run.
     *
     * <p>Covers both ItemFrame and GlowItemFrame (GlowItemFrame extends ItemFrame).
     */
    @SubscribeEvent
    public void onAttackItemFrame(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ItemFrame frame)) return;
        if (event.getEntity().level().isClientSide()) return;

        ItemStack frameItem = frame.getItem();
        if (frameItem.isEmpty()) return; // frame has no item — let vanilla handle frame breaking

        event.setCanceled(true);

        Player player = event.getEntity();
        ItemStack toGive = frameItem.copy();

        // Clear the frame and notify clients via entity data sync
        frame.setItem(ItemStack.EMPTY);
        frame.level().playSound(null, frame.getX(), frame.getY(), frame.getZ(),
                SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                SoundSource.BLOCKS, 1.0f, 1.0f);

        // Give item directly — bypasses doEntityDrops
        if (!player.getInventory().add(toGive)) {
            // Inventory full: force-spawn item entity at player position
            ItemEntity drop = new ItemEntity(
                    frame.level(), player.getX(), player.getY(), player.getZ(), toGive);
            drop.setDefaultPickUpDelay();
            frame.level().addFreshEntity(drop);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Entity chunk storage is fully loaded by now — safe to find and remove old NPCs
        TutorialLobbyManager.initNpcs(event.getServer().overworld());
        // Also bakes in its own NPCs/armor stands from the schematic's Entities tag — same
        // entity-storage-must-be-ready requirement as the call above.
        AcademyBuildingManager.place(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SessionManager.shutdown();
        TelemetryCsvWriter.shutdown();
    }

    /**
     * Server-side initialisation that runs once after the world is loaded but
     * before any players can connect.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Place the lobby NBT structure and discover the two simulation buttons.</li>
     *   <li>Build the tutorial lobby structure and initialise the session manager + telemetry.</li>
     *   <li>Pin the world respawn point to the lobby centre so that players who die
     *       outside a simulation (no {@code pendingLobbyRespawn} entry) still land
     *       inside the lobby rather than at world origin (0, 0, 0).</li>
     *   <li>Freeze the sun and weather so the arena lighting is always consistent
     *       regardless of how long a session has been running.</li>
     * </ol>
     *
     * @param event Provides the running {@link MinecraftServer}.
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("=== BerongSMP session build: UUID-subquery writes (2026-06-21-v4) ===");
        LOGGER.info("Initializing Lobby and World Settings for BerongSMP...");

        MinecraftServer server = event.getServer();

        // The overworld is the dimension that hosts both the lobby and the simulation arena.
        ServerLevel level = server.overworld();

        // Parse and place the lobby_structure NBT file, then scan it for ButtonBlock
        // instances to determine which button triggers fire vs. earthquake.
        LobbyManager.createLobby(level);
        TutorialLobbyManager.buildLobby(level); // structure only — NPCs need entity storage loaded first
        SessionManager.init(server);
        ModCommands.clearAuthorizations();
        TelemetryCsvWriter.init(server.getServerDirectory());

        String bfpPin = Config.BFP_ADMIN_PIN.get();
        if (bfpPin.isBlank()) {
            LOGGER.warn("[BerongSMP] BFP admin PIN is not set. '/bfp login' is disabled until 'bfpAdminPin' is configured in berongsmp-common.toml.");
        }

        // setRespawnData pins the global world spawn.  This is the fallback respawn
        // position used when a player has no bed or individual respawn anchor.
        // BlockPos(8, -31, 8) is the centre of the lobby floor at lobby elevation.
        // Using the type-safe API avoids locale or version fragility compared to
        // running a /setworldspawn command string.
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, new BlockPos(8, -31, 8), 0.0f, 0.0f));

        // ADVANCE_TIME (formerly doDaylightCycle) — stops the sun from moving.
        // ADVANCE_WEATHER (formerly doWeatherCycle) — stops rain/thunder from starting.
        // Both gamerules were renamed in Minecraft 1.21 / NeoForge 26.x.
        GameRules rules = level.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
    }
}
