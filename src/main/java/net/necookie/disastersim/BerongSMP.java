package net.necookie.disastersim;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelData;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.necookie.disastersim.world.LobbyManager;

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

    /** Example block registration. */
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    
    /** Example block item registration. */
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    /** Example food item registration. */
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    /** The Fire Extinguisher item registration. */
    public static final DeferredItem<net.necookie.disastersim.item.FireExtinguisherItem> FIRE_EXTINGUISHER = ITEMS.registerItem("fire_extinguisher",
            net.necookie.disastersim.item.FireExtinguisherItem::new);

    /** Custom Creative Mode Tab for BerongSMP items. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.berongsmp"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(FIRE_EXTINGUISHER.get());
            }).build());

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
        CREATIVE_MODE_TABS.register(modEventBus);

        // SimulationStatusPayload registers its own network channel via @SubscribeEvent
        // on the mod bus — it must be registered here so NeoForge picks it up.
        modEventBus.register(net.necookie.disastersim.network.SimulationStatusPayload.class);

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

    /**
     * Server-side initialisation that runs once after the world is loaded but
     * before any players can connect.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Place the lobby NBT structure and discover the two simulation buttons.</li>
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
        LOGGER.info("Initializing Lobby and World Settings for BerongSMP...");

        net.minecraft.server.MinecraftServer server = event.getServer();

        // The overworld is the dimension that hosts both the lobby and the simulation arena.
        net.minecraft.server.level.ServerLevel level = server.overworld();

        // Parse and place the lobby_structure NBT file, then scan it for ButtonBlock
        // instances to determine which button triggers fire vs. earthquake.
        LobbyManager.createLobby(level);

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
