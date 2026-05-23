package net.necookie.disastersim;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

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
     * @param modEventBus The event bus for mod-specific events.
     * @param modContainer The container for this mod.
     */
    public BerongSMP(IEventBus modEventBus, ModContainer modContainer) {
        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);
        
        // Register our DeferredRegisters
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        
        // Register network payloads and client-side setup
        modEventBus.register(net.necookie.disastersim.network.SimulationStatusPayload.class);
        modEventBus.register(net.necookie.disastersim.BerongSMPClient.class);
        modEventBus.register(net.necookie.disastersim.client.KeyMappings.class);

        // Register with the global NeoForge event bus
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // Add items to vanilla creative tabs
        modEventBus.addListener(this::addCreative);
        
        // Register mod configuration
        modContainer.registerConfig(ModConfig.Type.COMMON, net.necookie.disastersim.Config.SPEC);
    }

    /**
     * Registers custom commands when the server starts.
     */
    private void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        net.necookie.disastersim.command.ModCommands.register(event.getDispatcher());
    }

    /**
     * Common setup logic that runs on both client and server.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        // Future common setup logic can be added here
    }

    /**
     * Adds mod items to vanilla creative mode tabs.
     */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    /**
     * Server-side initialization logic.
     * Handles lobby creation and world settings.
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Initializing Lobby and World Settings for BerongSMP...");
        
        net.minecraft.server.level.ServerLevel level = event.getServer().overworld();
        
        // Create the initial lobby area
        LobbyManager.createLobby(level);
        
        // Set default world settings for the simulation environment
        net.minecraft.commands.CommandSourceStack source = event.getServer().createCommandSourceStack().withSuppressedOutput();
        event.getServer().getCommands().performPrefixedCommand(source, "setworldspawn 0 65 0 0");
        event.getServer().getCommands().performPrefixedCommand(source, "gamerule doDaylightCycle false");
        event.getServer().getCommands().performPrefixedCommand(source, "gamerule doWeatherCycle false");
    }
}
