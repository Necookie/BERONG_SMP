# new-furniture-prop

Add a new furniture/decor block end-to-end. Furniture has no hazard lifecycle (no HAZARDOUS/ON_FIRE
state) — it's a placeable prop with a facing (or symmetric) shape, a creative-tab slot, and a
custom-drawn texture.

## Arguments

`/new-furniture-prop <registry_name> [facing] [flammable]`
- `<registry_name>` — snake_case id, e.g. `reception_desk`
- `facing` — the prop has a visual front (extends `HorizontalFacingBlock`); omit for symmetric props
  (extends `Block` with a custom `getShape`, like `TrashCanBlock`)
- `flammable` — wood/fabric/paper construction (extends `FlammableFacingBlock` instead of
  `HorizontalFacingBlock`; only applies to FACING props)

## Checklist (do all of these — a missed step = invisible block or ugly inventory icon)

### 1. Java

- [ ] `block/<Name>Block.java` — extend `FlammableFacingBlock` (wood/fabric/paper FACING),
  `HorizontalFacingBlock` (non-flammable FACING), or plain `Block` (symmetric, override `getShape`).
  FACING blocks implement only `shapeFor(Direction facing)` — return per-orientation `VoxelShape`s
  built with `box(...)`, typically one shape for NORTH/SOUTH and a 90°-rotated one for EAST/WEST.
  Need a self-connecting run (like a long boardroom table)? Extend the existing `TableBlock`
  directly instead of reimplementing the NORTH/SOUTH/EAST/WEST connection idiom — it's a concrete,
  non-final class whose shape/connection logic only checks `neighbourState.is(this)`, so a subclass
  instance connects only to its own type.
- [ ] `registry/ModBlocks` — `registerBlock` field (NEVER the plain `Supplier` overload — startup
  crash, see CLAUDE.md's Block Registration Pattern). Add `.noOcclusion()` for any non-full-cube
  shape (glass, thin panels).
- [ ] `registry/ModItems` — `ITEMS.registerSimpleBlockItem(name, ModBlocks.X)` + add to
  `ALL_ITEM_MAP` in the big static block (superset used by `/item get`/`/item kit`).
- [ ] `registry/ModCreativeTabs` — `output.accept(ModItems.X_ITEM)` inside `FURN_TAB`'s builder,
  grouped with a comment under its room (new blocks go next to their room-mates, not appended
  blindly at the end — this is what keeps the tab organized/easy to find).

### 2. Assets (all under `src/main/resources/assets/berongsmp/`)

- [ ] `blockstates/<name>.json` — 4 `facing=` variants with `y` rotations (0/180/90/270 for
  north/south/east/west; copy `teachers_desk.json`), or a single bare variant for symmetric blocks.
- [ ] `models/block/<name>.json` — **must** declare `"parent": "minecraft:block/block"` even though
  it also has `"elements"` (CLAUDE.md's "Second known model-format gotcha" — without it, the
  inventory icon renders unscaled/front-on instead of the standard isometric GUI icon). Face
  `"texture"` values must be `"#variable"` refs resolved via the model's own `"textures"` map — a
  raw `"minecraft:block/x"` string at face level renders magenta in-game.
- [ ] `models/item/<name>.json` — `{"parent": "berongsmp:block/<name>"}`.
- [ ] `items/<name>.json` — the MC 26.x client item definition:
  `{"model": {"type": "minecraft:model", "model": "berongsmp:item/<name>"}}`. Without it the item
  renders as missing in inventory.
- [ ] `textures/block/` — add drawing functions to the current room-batch script (e.g.
  `scripts/generate_room_textures.py` for Conference/Office/Laboratory) and append them to its
  `ALL` list, then run it. Reuse existing shared palettes before drawing new ones (search sibling
  scripts for `desk_laminate_white`, `handle_bar_black`, `cabinet_body_graphite`,
  `chair_cushion_fabric`, `fixture_chrome`, `ceramic_glossy_white`, ...).
- [ ] `lang/en_us.json` — `"block.berongsmp.<name>"` display name; optional `.desc` tooltip line
  (picked up automatically by `ItemDescriptionTooltip`, no Java needed).

### 3. Facing legibility (do not skip)

A FACING block's identity must read on the front + top + one side — exactly the three faces the
inherited isometric GUI icon shows. Give every FACING prop a unique front-face texture (door,
screen, drawer front, control panel — something that appears nowhere else on the block) rather than
a uniform box on all six faces. Thin wall-mounted panels need real depth (≥3px), not a flat sliver,
or they read as a hairline in the inventory icon.

### 4. Docs + ship

- [ ] Add a row to `CLAUDE.md`'s Key Classes table (one line, matching the other furniture rows).
- [ ] `/micro-commit` — Java, assets, and docs can be separate commits if independently coherent.

## Verify

1. `./gradlew compileJava`
2. Boot (`/run-server`), then in-game: `/item get <name>`, confirm the inventory icon renders
   scaled/isometric (not full-size/front-on — the Second-Gotcha regression), place it in all 4
   facings and confirm it faces the placer, confirm it appears in `FURN_TAB` next to its room-mates.
