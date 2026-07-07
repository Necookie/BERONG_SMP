# new-hazard-prop

Add a new hazard prop block end-to-end — the thesis content pipeline. A hazard prop is a classroom/campus object with the 3-state lifecycle **normal → hazardous → on-fire**, driven by `HazardManager` during FIRE simulations, preventable bare-handed while merely hazardous, and defusable by extinguisher.

## Arguments

`/new-hazard-prop <registry_name> [facing] [kitchen]`
- `<registry_name>` — snake_case id, e.g. `overloaded_microwave`
- `facing` — the prop has a visual front (extends `HazardFacingBlock`); omit for symmetric props (extends `HazardBlock`)
- `kitchen` — it's a Class F/K grease hazard: only the wet chemical extinguisher may defuse it (add its id to `AbstractExtinguisherItem.KITCHEN_HAZARD_IDS`)

## Checklist (do all of these — a missed step = invisible block or silent no-op)

### 1. Java

- [ ] `common/hazard/<Name>Block.java` — extend `HazardBlock` (symmetric) or `HazardFacingBlock` (FACING). Implement `spawnHazardParticles` (pick particles that telegraph the danger: SMOKE, ELECTRIC_SPARK, FLAME, LARGE_SMOKE, CAMPFIRE_COSY_SMOKE, SOUL_FIRE_FLAME, LAVA, DRIPPING_WATER). Override `failureDelayTicks()` (how long hazardous before igniting), `failureMessage()`, `preventMessage()`, and `onHazardFailure` (usually `igniteAdjacent`). For FACING props also `shapeFor(Direction)` via `byFacing(...)`.
- [ ] `registry/ModBlocks` — `registerBlock` field (NEVER the plain Supplier overload — startup crash). Add `.lightLevel(state -> state.getValue(HazardBlock.HAZARDOUS) ? N : 0)` if it should glow when hazardous.
- [ ] `registry/ModItems` — block item + `static { HAZARD_ITEM_MAP.put("<name>", <NAME>_ITEM); }` right after it. HAZARD_ITEM_MAP order = creative tab + `/item hazard` order — put it next to its zone-mates.
- [ ] `kitchen` variant only: add the id to `AbstractExtinguisherItem.KITCHEN_HAZARD_IDS`.

### 2. Assets (all under `src/main/resources/assets/berongsmp/`)

- [ ] `blockstates/<name>.json` — variants for `hazardous` × `on_fire` (× `facing` with y-rotations if FACING). Normal state → `block/<name>`; every hazardous/on-fire combination → `block/<name>_hazardous`. Copy `plastic_trash_bin.json` (facing) or `swollen_phone_battery.json` (symmetric) as the template.
- [ ] `models/block/<name>.json` + `models/block/<name>_hazardous.json` — face `"texture"` values MUST be `"#variable"` refs resolved via the model's own `"textures"` map; a raw `minecraft:block/x` at face level renders magenta.
- [ ] `models/item/<name>.json` — `{"parent": "berongsmp:block/<name>"}`.
- [ ] `items/<name>.json` — the MC 26.x client item definition; without it the item renders as missing in inventory. **Known gap: the existing 20 hazard props are currently missing these files** — if touching this area, offer to regenerate all of them.
- [ ] `textures/block/` — add to `scripts/generate_hazard_textures.py` (deterministic, seeded) and run `python3 scripts/generate_hazard_textures.py`. Reuse the shared hazard-accent library (`hazard_ember_glow`, `hazard_warning_led`, `hazard_spark_arc`, `hazard_smoke_stain`, …) before drawing new accents.
- [ ] `lang/en_us.json` — `"block.berongsmp.<name>"` display name; include the hazard type in parentheses like the others, e.g. `"Overloaded Microwave (Kitchen Hazard)"`.

### 3. Behavior wiring (mostly automatic — verify, don't rewrite)

- `HazardManager.scanHazardProps` picks up any `HazardBlock`/`HazardFacingBlock` automatically; no registration needed there.
- Verify with the dev wand: `/item get hazard_wand` → right-click toggles normal↔hazardous, shift+right-click forces failure.

### 4. Docs + ship

- [ ] Add a row to `CLAUDE.md`'s Key Classes table (one line, matching the other hazard rows).
- [ ] Add the prop to `docs/hazard_props_spec.md` (zone, real-world rationale, failure consequence — this is the thesis artifact).
- [ ] `/micro-commit` — Java, assets, and docs can be separate commits if independently coherent.

## Verify

1. `./gradlew compileJava`
2. Boot (`/run-server`), then in-game: `/item hazard <name>`, place it, wand it hazardous (particles?), force failure (ignition?), spray the correct extinguisher (defuses? wrong extinguisher warned for kitchen props?).
