package net.necookie.disastersim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.necookie.disastersim.command.BfpAdminCommands;
import net.necookie.disastersim.command.ModCommands;
import net.necookie.disastersim.common.player.DuckCoverHoldManager;
import net.necookie.disastersim.common.safety.SafetyDeviceManager;
import net.necookie.disastersim.common.structure.AcademyBuildingManager;
import net.necookie.disastersim.common.structure.LobbyManager;
import net.necookie.disastersim.common.structure.TutorialLobbyManager;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.network.AcademyCompassPayload;
import net.necookie.disastersim.network.AcademyShakePayload;
import net.necookie.disastersim.network.AcademyStatusPayload;
import net.necookie.disastersim.network.DropAndRollPayload;
import net.necookie.disastersim.network.SimulationStatusPayload;
import net.necookie.disastersim.network.TutorialStatusPayload;
import net.necookie.disastersim.registry.ModAttachments;
import net.necookie.disastersim.registry.ModBlocks;
import net.necookie.disastersim.registry.ModCreativeTabs;
import net.necookie.disastersim.registry.ModEntities;
import net.necookie.disastersim.registry.ModItems;
import net.necookie.disastersim.registry.ModSounds;
import net.necookie.disastersim.session.AuthManager;
import net.necookie.disastersim.session.SessionManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/**
 * Main entry point for the BerongSMP mod — a thin bootstrap. All game-object registrations live
 * in {@code registry/} (ModBlocks, ModItems, ModCreativeTabs, ModEntities, ModSounds,
 * ModAttachments); this class wires them to the mod event bus, registers network payloads and
 * lifecycle listeners, and performs one-time world setup on server start.
 */
@Mod(BerongSMP.MODID)
public class BerongSMP {
    /** The unique identifier for the mod. */
    public static final String MODID = "berongsmp";
    
    /** Logger instance for mod-specific logging. */
    public static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Wires all registries, network payloads, and lifecycle listeners to the event buses.
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
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModAttachments.register(modEventBus);

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
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
        // Same class-loading rule for the safety-device tick handler (smoke detectors,
        // sprinklers, emergency lights) — see SafetyDeviceManager's javadoc.
        SafetyDeviceManager.bootstrap();
        // Same class-loading rule again — AuthManager's static block registers the per-station
        // logout hook that clears who's logged in; nothing else in the codebase calls real code
        // on this class otherwise.
        AuthManager.bootstrap();
        // Same class-loading rule again — BfpAdminCommands' static block registers the logout
        // hook that clears a station's BFP admin grant/test-bypass so it doesn't carry over to
        // the next student sitting down there.
        BfpAdminCommands.bootstrap();
        // Same class-loading rule again — SessionManager's static block registers the logout hook
        // that properly closes out a StudentSession (and its Turso row) when a station disconnects
        // without an explicit /bfp checkout, instead of leaving it stuck at status='active'.
        SessionManager.bootstrap();
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
            event.accept(ModItems.EXAMPLE_BLOCK_ITEM);
        }
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
