# BerongSMP Texture Design System

A unified visual language for every hand-authored 16×16 (and 32×32 armor-layer) texture in the
mod — hazard props, furniture/decor, safety equipment, and handheld items. Introduced 2026-07-08
to replace the previous per-script, ad hoc styling (each `generate_*_textures.py` script had its
own local `border()`/shading helpers with no shared identity) with one signature look implemented
in a single shared module: **`scripts/_texture_style.py`**.

Every texture-generating script imports this module and calls `apply_signature(im)` as the last
step before saving. That is the enforcement mechanism — the rules below are not just guidelines,
they are code every generated PNG passes through.

## 1. The Signature Core

One recurring motif ties every item and block texture together: the **chamfered corner + amber
maker's-mark**.

- The four corner pixels of every 16×16 texture are clipped to transparent (`chamfer_corners`),
  breaking the perfect square silhouette that every vanilla-style flat icon has.
- The top-left corner pixel (where vanilla Minecraft's implicit light source sits) carries a single
  **amber accent pixel** (`#FFB100`, `SIGNATURE_ACCENT`) at partial alpha — a one-pixel "maker's
  mark" present on every texture in the mod, the same way `computer_block.json` reused `case`/`dark`
  across models. It reads as a deliberate design signature, not noise.
- All outlines mod-wide use one shared near-black outline color, `SIGNATURE_OUTLINE = (18, 14, 12)`
  — a warm near-black, never pure `(0,0,0)`, so silhouettes read as "painted" rather than
  vector-clip-art.

## 2. Palette & Shading Rules

- **5-tone value ramp per material**, not the old ad hoc 2–3 tone shading: deepest shadow → shadow →
  base → highlight → specular hotspot. `ramp(base_color)` in `_texture_style.py` generates this
  automatically from one base color so every object's material reads with consistent depth.
- **Fixed light direction, mod-wide**: top-left 45°, same as vanilla, but pushed to **higher
  contrast** than the vanilla-style shading already in use — `LIGHT_FACTOR = 1.45` /
  `SHADOW_FACTOR = 0.62` (vanilla-style textures typically sit closer to 1.2/0.85). This is what
  makes the mod's items read as "premium" next to a vanilla hotbar: harder falloff, not flatter.
- **One universal accent-color family**: `SIGNATURE_ACCENT` (amber `#FFB100`) for the corner
  maker's-mark and for every "this is interactive / this is a highlight glint" pixel mod-wide
  (specular hotspots, screw heads catching light, glass-lens glints). Hazard-specific danger colors
  (red LEDs, blue sparks, orange embers) are unchanged — they're semantic, not part of the shared
  signature — but they now sit on top of the same 5-tone ramp and higher-contrast rim light as
  everything else, so they don't look like a different mod's assets pasted in.
- **Rim light**: a single bright pixel-line along the strict top and left silhouette edge
  (`rim_light`), applied after an object's own shading. This is what most separates the new look
  from flat vanilla texture-painting — vanilla rarely rim-lights individual props.

## 3. Silhouette Identity

- **Corner chamfer is mandatory and universal** (see §1) — no texture in the mod is a perfect
  16×16 square anymore. This alone measurably breaks the "flat Minecraft box" read at a glance.
- **Diagonal-first for handheld/flat icons**: items that are drawn as flat `item/generated` icons
  (extinguishers, megaphone, whistle, flashlight, fire blanket, first-aid kit, hazard wand) are
  composed on a diagonal bottom-left → top-right axis (see `generate_safety_textures.py`'s existing
  `diagonal_capsule` helper and the redesigned `hazard_wand`), never a straight vertical/horizontal
  object centered in the tile. A flat icon that just fills a centered vertical rectangle is
  treated as a defect to fix, not an acceptable baseline.
- **Multi-element block geometry keeps breaking the cube silhouette** where it already does
  (ceiling fan blades, chairs, table legs) — the design system doesn't change block *geometry*,
  only its *texturing*, since geometry already diverges from flat cubes per `docs/history/*-visual-log.md`.
  New block models should keep following that precedent: avoid a plain full cube unless the real
  object is genuinely a sealed box (filing cabinet, locker).

## 4. NeoForge Texture Mapping Guidelines

- **Block model `"parent"` is mandatory.** Every custom block model must declare
  `"parent": "minecraft:block/block"` (see CLAUDE.md's "Second known model-format gotcha") — this
  is what supplies the GUI inventory-icon transform. A model missing it renders unscaled/unrotated
  in the inventory slot no matter how good the texture is. This was audited and fixed for all 37
  offending files in the 2026-07-08 rendering-bug pass; keep it true for every new block model.
- **Face `"texture"` values must be `"#variable"` references**, never a raw `"minecraft:block/xxx"`
  string (the other documented CLAUDE.md gotcha) — resolved per-model via the `"textures"` map.
- **Emissive/glow textures are simulated, not literal.** Vanilla Minecraft has no glow-map/emissive
  layer for block or item textures without a shader pack. The mod's existing approach — a genuinely
  bright, near-white/saturated texture region (`led_diffuser_glow`, `hazard_ember_glow`) combined
  with the block's real `lightLevel` supplier (`LightBulbBlock`, `EmergencyLightBlock`, hazard
  props' hazardous-state light bump) — is the correct pattern and should stay the pattern. Item
  icons that want to *look* like they glow (hazard_wand's tip) should use the same trick: a
  small, very light, desaturated-toward-white patch at the object's "hot" point, not a separate
  render layer.
- **Multi-layer item models** (`layer0`/`layer1` in an `item/generated`-parented model) are reserved
  for genuinely separate visual layers — e.g. a base object + a colored overlay that could
  eventually be player-dyed or state-tinted via `item_model` predicates. None of the current
  handheld items need this yet (each is a single flat design), but if a future item needs a
  state-driven color swap (e.g. an extinguisher whose gauge needle changes color), add it as
  `layer1` rather than baking every state into a separate full texture + separate item model file.
- **State-driven variants, not frame animation.** The mod already expresses "the object changed"
  through real blockstate properties (`HAZARDOUS`, `ON_FIRE`, `BURNING`, `ALARMING`, `LIT`) driving
  separate baked models (`HazardManager`, `SafetyDeviceManager`). Keep using that pattern for any
  new stateful object instead of introducing `.mcmeta` animated textures — it integrates with the
  existing tick-driven managers and telemetry, and avoids the animation/tint-index plumbing a true
  animated texture would need.
- **Inventory scaling comes from the parent chain, not custom `"display"` blocks.** No model in the
  mod defines its own `"display"` transforms; every correctly-rendering icon relies on inheriting
  `minecraft:block/block`'s defaults. Don't hand-author custom GUI transforms unless a specific
  object is proven to need different framing after this fix — it's one more thing to keep in sync
  per block and the shared default already reads correctly for every existing shape.

## Applying it

```bash
python3 scripts/generate_hazard_textures.py
python3 scripts/generate_furniture_textures.py
python3 scripts/generate_school_textures.py
python3 scripts/generate_safety_textures.py
python3 scripts/generate_light_bulb_textures.py
python3 scripts/generate_firefighter_armor_textures.py
python3 scripts/generate_hazard_wand_texture.py
```

Each script now imports `scripts/_texture_style.py` and finishes every texture through
`apply_signature(im)` before writing — regenerating is how the signature core, palette rules, and
corner-chamfer silhouette rule get applied mod-wide without hand-editing every PNG individually.
