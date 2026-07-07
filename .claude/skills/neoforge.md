# neoforge

Load this repo's NeoForge 26.1.2 conventions before writing mod code. Use at the start of any task that adds/changes blocks, items, entities, packets, events, or registrations — it prevents the recurring crash patterns this codebase has already hit and fixed.

## Registration (registry/ package)

- All registrations live in `registry/`: `ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModEntities`, `ModSounds`, `ModAttachments`. `BerongSMP` is a thin bootstrap that calls each `register(modEventBus)` — never add registration fields back into `BerongSMP`.
- **Block registration MUST use** `BLOCKS.registerBlock(name, Constructor::new, () -> Block.Properties.of()...)` — NOT the plain `Supplier` overload. NeoForge 26.x calls `effectiveDrops()` in `BlockBehaviour.<init>`, which needs the registry key already injected into Properties; the plain overload doesn't inject it and crashes at startup with `NullPointerException: Block id not set`.
- New block = field in `ModBlocks` + block item in `ModItems` + (if player-obtainable) `ALL_ITEM_MAP.put` + creative tab entry in `ModCreativeTabs`. Hazard props additionally need `HAZARD_ITEM_MAP.put` (insertion order = tab order = `/item hazard` tab-completion order).

## Event buses (the duality)

- `modEventBus` — lifecycle (registration, setup, payload registration). Wired in the `BerongSMP`/`BerongSMPClient` constructors.
- `NeoForge.EVENT_BUS` — runtime game events. Classes annotated `@EventBusSubscriber(modid = BerongSMP.MODID)` auto-register static `@SubscribeEvent` methods here.
- Network payloads register themselves via `@SubscribeEvent` on the mod bus — new payload classes must be added to the `modEventBus.register(...)` list in `BerongSMP`'s constructor.

## Per-tick and per-player lifecycle plumbing

- Per-tick handlers self-register with `TickScheduler.register(...)` in a `static {}` block — **and the class must be loaded by a real code path**. Javadoc `{@code}` mentions don't count as references; a handler class with no natural caller silently never ticks. If there's no natural caller, add a no-op `bootstrap()` called from `BerongSMP.commonSetup` (precedent: `DuckCoverHoldManager`, which broke exactly this way).
- Per-player logout/login cleanup goes through `PlayerLifecycleRegistry.registerLogoutHook`/`registerLoginHook` (`common/player/`) — do not add new `PlayerLoggedOutEvent` subscribers.

## Client–server split

- Client-only classes (`client/` package: HUDs, KeyMappings, renderers) are wired in `BerongSMPClient` (`@Mod(dist = CLIENT)`), never in `BerongSMP` — a `KeyMapping` field touched from common code crashes a dedicated server.
- Server→client state goes through payloads (`network/`) handled with `context.enqueueWork()`; never touch client statics from server code.

## Structures

- `.nbt` via `SimulationStructureLoader` (StructureTemplateManager, identifier without extension: `berongsmp:lspu_library_main`); `.schem` via `SchemLoader` (resource path with extension: `structure/academy_building.schem`). Both under `src/main/resources/data/berongsmp/structure/`.
- Anything spawning entities at boot belongs in `onServerStarted` (entity chunk storage ready), not `onServerStarting` — otherwise fresh spawns collide with same-UUID copies persisted by the previous run.

## Known API traps (MC 26.1.2)

- `ServerPlayer.level()` → use `serverLevel()` where a `ServerLevel` is needed.
- `SavedData` uses the `SavedDataType` pattern — copy `TutorialSavedData`/`AcademySavedData`, don't write from memory.
- Armor: no `ArmorItem` class — plain `Item` + `Item.Properties.humanoidArmor(ArmorMaterial, ArmorType)`; `ArmorMaterial` is a plain record (see `ModItems.FIREFIGHTER_MATERIAL`).
- Gamerules renamed: `ADVANCE_TIME`/`ADVANCE_WEATHER` (not doDaylightCycle/doWeatherCycle).
- Block model faces must reference textures via `"#variable"` through the model's own `"textures"` map — a raw `minecraft:block/x` string at the face level renders magenta/black.

## Verification ladder

1. `./gradlew compileJava` — after every change.
2. Boot smoke test (see `/run-server`) — required when a change touches registration, class-loading order, static init, or structure paths; compile success does NOT catch these.
3. `./gradlew build` — before calling work finished (runs the tests).
