# new-extinguisher

Add a new handheld extinguisher (or similar spray-tool safety item). The shared mechanics — safety-pin gate, held-spray ray, durability drain, charge tooltip, kitchen-hazard refusal — all live in `AbstractExtinguisherItem`; a new extinguisher is a small subclass plus registration and assets.

## Arguments

`/new-extinguisher <registry_name> <fire_class>` — e.g. `/new-extinguisher foam_extinguisher B` (Philippine BFP color-coding: A=water/red, ABC dry-chem=red, B foam=cream, C CO2=black/green here, F/K wet-chem=yellow, D=blue).

## Checklist

### 1. Java

- [ ] `item/<Name>Item.java` extending `AbstractExtinguisherItem`. Implement only the hooks:
  - `extinguishAt(level, pos, user)` — what a spray hit does. Fire/soul-fire suppression + whatever's specific (see `CO2ExtinguisherItem` for a block-state target, `WetChemicalExtinguisherItem` for `HazardManager.defuse` + special-cased feedback).
  - `spawnSprayParticlesServer` / `spawnSprayParticlesClient` — give it a visually distinct discharge (color via `DustParticleOptions`).
  - `sprayPitch(sputtering, random)` — sound character.
  - `onSprayResolved` — telemetry: `recordExtinguish` for FIRE score if appropriate, `extinguisher_use` CSV row + event-log entry (copy the pattern from an existing subclass verbatim — the CSV contract is fixed).
  - `pinPulledMessage()` + `appendHoverText` (class rating, usage line, safety warning).
  - Decide kitchen-hazard policy: can it defuse the 5 Class F/K props? If not (most), nothing to do — the base class refuses them; if yes, mirror `WetChemicalExtinguisherItem`.
- [ ] `registry/ModItems` — `ITEMS.registerItem("<name>", props -> new <Name>Item(props.durability(N)))` (existing durabilities: ABC 300, CO2 200, wet-chem 240) + `ALL_ITEM_MAP.put`.
- [ ] `registry/ModCreativeTabs` — add to `SIM_TAB`'s displayItems.
- [ ] `command/ItemCommands` — `/get_<name>` command if instructors need to hand it out directly (mirror `/get_wet_chemical_extinguisher`); `/item get <name>` works automatically via ALL_ITEM_MAP.
- [ ] Decide auto-issue: should any simulation type give it at session start? If yes, wire in `SimulationManager.startSimulation` next to the existing extinguisher grants.

### 2. Assets

- [ ] `models/item/<name>.json` + `items/<name>.json` (MC 26.x client item definition — required or it renders missing in inventory).
- [ ] `textures/item/<name>.png` — 16×16, body color per BFP class code; base it on the existing extinguisher textures for consistent silhouette.
- [ ] `lang/en_us.json` — display name including the class rating, e.g. `"Foam Extinguisher [Class B]"`.

### 3. Docs + ship

- [ ] `CLAUDE.md` Key Classes row (one line, matching the other extinguisher rows: class purpose, what it can/can't defuse, durability, how it's obtained).
- [ ] If it interacts with hazard props, note it in `docs/hazard_props_spec.md`.
- [ ] `/micro-commit`.

## Verify

1. `./gradlew compileJava`
2. Boot: `/item get <name>` → tooltip correct → right-click pulls pin → hold-spray drains durability + particles → extinguishes a fire block → refuses (or defuses, per design) a kitchen hazard prop → telemetry row appears in `run/telemetry/gameplay_logs_*.csv` during a live session.
