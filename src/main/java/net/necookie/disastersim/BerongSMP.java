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
import net.necookie.disastersim.block.LockerBlock;
import net.necookie.disastersim.block.SinkBlock;
import net.necookie.disastersim.block.ToiletBlock;
import net.necookie.disastersim.block.TrashCanBlock;
import net.necookie.disastersim.block.WhiteboardBlock;
import net.necookie.disastersim.block.hazard.*;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;
import net.necookie.disastersim.item.NpcSpawnerItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.necookie.disastersim.world.LobbyManager;
import net.necookie.disastersim.world.TutorialLobbyManager;

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

    // ── NPC entity ───────────────────────────────────────────────────────────

    /** Single entity type shared by all five instructors; NpcType stored in NBT selects the skin. */
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
                        if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) return 7;
                        return 0;
                    }));

    /** Computer block item. */
    public static final DeferredItem<BlockItem> COMPUTER_ITEM = ITEMS.registerSimpleBlockItem("computer", COMPUTER);

    /** Wall-mounted fire alarm pull station — activates during FIRE simulations, logs fire_alarm_activate telemetry. */
    public static final DeferredBlock<FireAlarmBlock> FIRE_ALARM_BLOCK = BLOCKS.registerBlock(
            "fire_alarm",
            FireAlarmBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .strength(0.5f, 4.0f)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .lightLevel(s -> s.getValue(FireAlarmBlock.ACTIVATED) ? 7 : 0));

    /** Fire alarm block item. */
    public static final DeferredItem<BlockItem> FIRE_ALARM_ITEM =
            ITEMS.registerSimpleBlockItem("fire_alarm", FIRE_ALARM_BLOCK);

    // ── Furniture blocks ─────────────────────────────────────────────────────

    /** Classroom whiteboard — wall-mounted flat panel with marker tray. */
    public static final DeferredBlock<WhiteboardBlock> WHITEBOARD = BLOCKS.registerBlock("whiteboard",
            WhiteboardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> WHITEBOARD_ITEM = ITEMS.registerSimpleBlockItem("whiteboard", WHITEBOARD);

    /** Toilet block — ceramic basin+tank; right-click to flush. */
    public static final DeferredBlock<ToiletBlock> TOILET = BLOCKS.registerBlock("toilet",
            ToiletBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> TOILET_ITEM = ITEMS.registerSimpleBlockItem("toilet", TOILET);

    /** Wall-mounted sink with iron faucet and handles; right-click for water sound. */
    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink",
            SinkBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW)
                    .strength(1.0f, 4.0f)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> SINK_ITEM = ITEMS.registerSimpleBlockItem("sink", SINK);

    /** Office chest-of-drawers — dark oak body with birch drawer fronts and iron handles. */
    public static final DeferredBlock<DrawersBlock> DRAWERS = BLOCKS.registerBlock("drawers",
            DrawersBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> DRAWERS_ITEM = ITEMS.registerSimpleBlockItem("drawers", DRAWERS);

    /** Flammable oak computer desk with 4 legs and a back cable-management panel. */
    public static final DeferredBlock<ComputerTableBlock> COMPUTER_TABLE = BLOCKS.registerBlock("computer_table",
            ComputerTableBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> COMPUTER_TABLE_ITEM = ITEMS.registerSimpleBlockItem("computer_table", COMPUTER_TABLE);

    /** Dark oak classroom/office chair with gray cushion seat and backrest. */
    public static final DeferredBlock<ChairBlock> CHAIR = BLOCKS.registerBlock("chair",
            ChairBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(1.0f, 2.0f)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> CHAIR_ITEM = ITEMS.registerSimpleBlockItem("chair", CHAIR);

    /** Tall metal filing cabinet with 2 drawers, label slots, and pull handles. */
    public static final DeferredBlock<FilingCabinetBlock> FILING_CABINET = BLOCKS.registerBlock("filing_cabinet",
            FilingCabinetBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> FILING_CABINET_ITEM = ITEMS.registerSimpleBlockItem("filing_cabinet", FILING_CABINET);

    /** Tall metal school/office locker with vents, door seam, handle, and gold lock. */
    public static final DeferredBlock<LockerBlock> LOCKER = BLOCKS.registerBlock("locker",
            LockerBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> LOCKER_ITEM = ITEMS.registerSimpleBlockItem("locker", LOCKER);

    /** Small cylindrical trash can — no facing, symmetric, open-top. */
    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = BLOCKS.registerBlock("trash_can",
            TrashCanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> TRASH_CAN_ITEM = ITEMS.registerSimpleBlockItem("trash_can", TRASH_CAN);

    /** Wall-mounted cork bulletin board with pinned paper slips. */
    public static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = BLOCKS.registerBlock("bulletin_board",
            BulletinBoardBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> BULLETIN_BOARD_ITEM = ITEMS.registerSimpleBlockItem("bulletin_board", BULLETIN_BOARD);

    /** Ceiling fan — motor housing + 4 blades + glowstone light bowl; symmetric/no facing. */
    public static final DeferredBlock<CeilingFanBlock> CEILING_FAN = BLOCKS.registerBlock("ceiling_fan",
            CeilingFanBlock::new,
            () -> Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(0.5f, 2.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 5));
    public static final DeferredItem<BlockItem> CEILING_FAN_ITEM = ITEMS.registerSimpleBlockItem("ceiling_fan", CEILING_FAN);

    /** Example food item registration. */
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    /** The Fire Extinguisher item registration. */
    public static final DeferredItem<net.necookie.disastersim.item.FireExtinguisherItem> FIRE_EXTINGUISHER = ITEMS.registerItem("fire_extinguisher",
            props -> new net.necookie.disastersim.item.FireExtinguisherItem(props.durability(300)));

    /** CO2 extinguisher — for Class C (electrical) fires; targets lit ComputerBlocks. */
    public static final DeferredItem<net.necookie.disastersim.item.CO2ExtinguisherItem> CO2_EXTINGUISHER = ITEMS.registerItem("co2_extinguisher",
            props -> new net.necookie.disastersim.item.CO2ExtinguisherItem(props.durability(200)));

    /** Creative tab: simulation tools and interactive blocks. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIM_TAB = CREATIVE_MODE_TABS.register("sim_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.simulation"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FIRE_EXTINGUISHER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FIRE_EXTINGUISHER.get());
                output.accept(CO2_EXTINGUISHER.get());
                output.accept(COMPUTER_ITEM.get());
                output.accept(FIRE_ALARM_ITEM.get());
                output.accept(NPC_SGT_REYES.get());
                output.accept(NPC_SGT_SANTOS.get());
                output.accept(NPC_OFFICER_CRUZ.get());
                output.accept(NPC_CAPT_MORFE.get());
                output.accept(NPC_SECURITY_TUAZON.get());
            }).build());

    /** Creative tab: furniture and props for building scenarios. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURN_TAB = CREATIVE_MODE_TABS.register("furn_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.furniture"))
            .withTabsBefore(SIM_TAB.getKey())
            .icon(() -> CHAIR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WHITEBOARD_ITEM.get());
                output.accept(BULLETIN_BOARD_ITEM.get());
                output.accept(COMPUTER_TABLE_ITEM.get());
                output.accept(CHAIR_ITEM.get());
                output.accept(DRAWERS_ITEM.get());
                output.accept(FILING_CABINET_ITEM.get());
                output.accept(LOCKER_ITEM.get());
                output.accept(TOILET_ITEM.get());
                output.accept(SINK_ITEM.get());
                output.accept(TRASH_CAN_ITEM.get());
                output.accept(CEILING_FAN_ITEM.get());
            }).build());

    // ── Hazard prop blocks (20 items — populated below as blocks are declared) ──

    /** Maps hazard block registry name → its DeferredItem for the /item hazard command. */
    public static final LinkedHashMap<String, DeferredItem<BlockItem>> HAZARD_ITEM_MAP = new LinkedHashMap<>();

    // ── Classroom Zone (5 blocks) ─────────────────────────────────────────────

    /** Plastic waste bin — emits smoke when has_vape=true (hazardous). */
    public static final DeferredBlock<PlasticTrashBinBlock> PLASTIC_TRASH_BIN = BLOCKS.registerBlock(
            "plastic_trash_bin", PlasticTrashBinBlock::new,
            () -> Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.QUARTZ)
                    .strength(0.5f, 1.0f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> PLASTIC_TRASH_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("plastic_trash_bin", PLASTIC_TRASH_BIN);
    static { HAZARD_ITEM_MAP.put("plastic_trash_bin", PLASTIC_TRASH_BIN_ITEM); }

    /** Overloaded daisy-chain extension cord — electric sparks when overloaded=true. */
    public static final DeferredBlock<DaisyChainExtensionBlock> DAISY_CHAIN_EXTENSION = BLOCKS.registerBlock(
            "daisy_chain_extension", DaisyChainExtensionBlock::new,
            () -> Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
                    .strength(0.2f, 0.5f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> DAISY_CHAIN_EXTENSION_ITEM =
            ITEMS.registerSimpleBlockItem("daisy_chain_extension", DAISY_CHAIN_EXTENSION);
    static { HAZARD_ITEM_MAP.put("daisy_chain_extension", DAISY_CHAIN_EXTENSION_ITEM); }

    /** Floor sawdust accumulation layer — emits ash particles at accumulation >= 3. */
    public static final DeferredBlock<WoodshopSawdustLayerBlock> WOODSHOP_SAWDUST_LAYER = BLOCKS.registerBlock(
            "woodshop_sawdust_layer", WoodshopSawdustLayerBlock::new,
            () -> Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.SAND)
                    .strength(0.1f, 0.1f).sound(SoundType.SAND).noOcclusion());
    public static final DeferredItem<BlockItem> WOODSHOP_SAWDUST_LAYER_ITEM =
            ITEMS.registerSimpleBlockItem("woodshop_sawdust_layer", WOODSHOP_SAWDUST_LAYER);
    static { HAZARD_ITEM_MAP.put("woodshop_sawdust_layer", WOODSHOP_SAWDUST_LAYER_ITEM); }

    /** Creative tab: all 20 hazard prop blocks for the simulation building. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAZARD_TAB = CREATIVE_MODE_TABS.register("hazards_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp.hazards"))
            .withTabsBefore(FURN_TAB.getKey())
            .icon(() -> net.minecraft.world.item.Items.FIRE_CHARGE.getDefaultInstance())
            .displayItems((parameters, output) -> HAZARD_ITEM_MAP.values().forEach(i -> output.accept(i.get())))
            .build());

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
        modEventBus.addListener(this::onEntityAttributes);

        // SimulationStatusPayload registers its own network channel via @SubscribeEvent
        // on the mod bus — it must be registered here so NeoForge picks it up.
        modEventBus.register(net.necookie.disastersim.network.SimulationStatusPayload.class);
        modEventBus.register(net.necookie.disastersim.network.TutorialStatusPayload.class);

        // BerongSMPClient is annotated @Mod(dist = CLIENT) so it only loads on the
        // physical client, keeping server JARs free of client-only Minecraft classes.
        modEventBus.register(net.necookie.disastersim.BerongSMPClient.class);

        // KeyMappings registers custom keybindings during the client setup phase.
        modEventBus.register(net.necookie.disastersim.client.KeyMappings.class);

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
     * Delegates command registration to {@link net.necookie.disastersim.command.ModCommands}.
     * This fires before the server opens for connections, so all commands are available
     * from the first tick.
     *
     * @param event Provides the Brigadier {@link com.mojang.brigadier.CommandDispatcher}
     *              that maps command literals to execution logic.
     */
    private void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        net.necookie.disastersim.command.ModCommands.register(event.getDispatcher());
    }

    /**
     * Common setup logic that runs on both the physical client and dedicated server.
     * This phase is the right place for cross-side initialisation that doesn't depend
     * on the world being loaded (e.g., capability registration, recipe unlocking).
     * Currently a no-op; expand here if shared setup is needed in future.
     *
     * @param event The FML common setup event (enqueued, not immediate).
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        // No cross-side setup required at this time.
        // Wood furniture flammability is handled via IBlockExtension overrides in each block class.
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
    public void onAttackItemFrame(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (!(event.getTarget() instanceof net.minecraft.world.entity.decoration.ItemFrame frame)) return;
        if (event.getEntity().level().isClientSide()) return;

        net.minecraft.world.item.ItemStack frameItem = frame.getItem();
        if (frameItem.isEmpty()) return; // frame has no item — let vanilla handle frame breaking

        event.setCanceled(true);

        net.minecraft.world.entity.player.Player player = event.getEntity();
        net.minecraft.world.item.ItemStack toGive = frameItem.copy();

        // Clear the frame and notify clients via entity data sync
        frame.setItem(net.minecraft.world.item.ItemStack.EMPTY);
        frame.level().playSound(null, frame.getX(), frame.getY(), frame.getZ(),
                net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        // Give item directly — bypasses doEntityDrops
        if (!player.getInventory().add(toGive)) {
            // Inventory full: force-spawn item entity at player position
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                    frame.level(), player.getX(), player.getY(), player.getZ(), toGive);
            drop.setDefaultPickUpDelay();
            frame.level().addFreshEntity(drop);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Entity chunk storage is fully loaded by now — safe to find and remove old NPCs
        TutorialLobbyManager.initNpcs(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        net.necookie.disastersim.session.SessionManager.shutdown();
        net.necookie.disastersim.world.TelemetryCsvWriter.shutdown();
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
     * @param event Provides the running {@link net.minecraft.server.MinecraftServer}.
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("=== BerongSMP session build: UUID-subquery writes (2026-06-21-v4) ===");
        LOGGER.info("Initializing Lobby and World Settings for BerongSMP...");

        net.minecraft.server.MinecraftServer server = event.getServer();

        // The overworld is the dimension that hosts both the lobby and the simulation arena.
        net.minecraft.server.level.ServerLevel level = server.overworld();

        // Parse and place the lobby_structure NBT file, then scan it for ButtonBlock
        // instances to determine which button triggers fire vs. earthquake.
        LobbyManager.createLobby(level);
        TutorialLobbyManager.buildLobby(level); // structure only — NPCs need entity storage loaded first
        net.necookie.disastersim.session.SessionManager.init(server);
        net.necookie.disastersim.command.ModCommands.clearAuthorizations();
        net.necookie.disastersim.world.TelemetryCsvWriter.init(server.getServerDirectory());

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
