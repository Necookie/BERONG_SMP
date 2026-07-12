# Hazard-State Tier, Flat-Modern Restyle & Creative-Tab Reorg Log (2026-07-12)

Tracks a visual + organizational overhaul requested directly (not from an audit): hazard props and
furniture were reported as looking dated and hard to tell apart in the `HAZARDOUS` state, and the
single 85-item hazard tab / 86-item furniture tab were hard to browse in creative mode. Frozen for
context; not updated going forward (see `docs/history/` convention in `CLAUDE.md`).

## 1. Universal hazard-state tiers ("Caution Amber" / "Char & Ember")

**Problem:** every hazard prop's `on_fire=true` blockstate variant aliased straight to its
`_hazardous` model (`scripts/add_onfire_blockstates.py`'s original scope explicitly deferred bespoke
burning art), and several props' `HAZARDOUS` model differed from `NORMAL` by only a small accent
swap — easy to miss at a glance, and inconsistent prop to prop.

**Fix:** `scripts/_texture_style.py` gained two deterministic pixel transforms:
- `hazardize(im, seed)` — Tier 2. Warm-shifts the texture (R×1.15, G×1.00, B×0.72) and bakes a 2px
  amber/near-black (`HAZARD_CAUTION` `#FFC400` / `HAZARD_CHEVRON_DARK`) diagonal chevron band across
  the top edge, plus a few `HAZARD_CAUTION_HI` stress-glint pixels.
- `charify(im, seed)` — Tier 3. Blends the texture 65% toward `FIRE_CHAR` (near-black) and bakes
  2-3 jagged `FIRE_EMBER`/`FIRE_EMBER_CORE` (`#FF5A1E` / `#FFD666`) cracks plus a 1px ember rim
  along the bottom edge.

New `scripts/generate_hazard_state_tiers.py` applies these mechanically to all 84 stateful hazard
props (`woodshop_sawdust_layer` uses its own `ACCUMULATION` mechanic, no `HAZARDOUS` property):
for every texture referenced by a `<prop>_hazardous.json` model, it generates a deduplicated
`<texture>_hz.png` (by source filename, so a texture shared across many props like `fixture_chrome`
is only transformed once), repoints the hazardous model, then does the same with `charify()` into a
new sibling `<prop>_on_fire.json` model and repoints every blockstate's `on_fire=true` variants at
it. **Source textures are never mutated in place** — recovered idempotently by stripping any
existing `_hz`/`_of` suffix before reading — because some accent files (e.g.
`hazard_glass_screen_off`) are also referenced by unrelated normal-state furniture models
(`conference_wall_display`, `oscilloscope_cart`, `computer_lab_desk_row`) and must stay untouched.

Result: 84 hazardous models + 84 new on-fire models, 262 unique tier textures, 84 blockstates
repointed. Verified via JSON validation of every touched file, a full `./gradlew build`, and visual
spot-checks (e.g. `fixture_chrome.png` → `fixture_chrome_hz.png` → `fixture_chrome_of.png` reads as
grey chrome → amber/black chevron banding → charred with orange ember cracks).

Re-run after editing any hazard prop's normal-state texture or model:
```bash
python3 scripts/generate_hazard_state_tiers.py
```

## 2. Flat-modern shading pass

**Problem:** every `generate_*_textures.py` script had its own local `gradient_shade` helper — a
smooth per-pixel diagonal top-left-lit gradient, duplicated near-identically across 8 scripts with
slightly different (and lower) contrast than the documented mod-wide signature
(`_texture_style.LIGHT_FACTOR=1.45`/`SHADOW_FACTOR=0.62`). This read as a soft airbrushed gradient
rather than the "clean modern flat-shaded" look requested.

**Fix:** each script's `gradient_shade` (and `gradient_shade_rgba` for flat RGBA item icons, in
`generate_safety_textures.py` and `generate_basketball_textures.py`) now quantizes the interpolation
factor into 4 discrete tone bands before scaling, at the higher documented contrast — a flat-shaded,
cel-shaded look instead of a smooth gradient. Because every object in a script routes its shading
through this one shared helper, editing it once restyles every object the script draws, rather than
needing to touch each per-object drawing function individually. Default parameters only (`bands=4`,
`light=1.45`, `dark=0.62`); explicit per-call overrides elsewhere in a script are preserved.

Applied to all 8 scripts with the helper, one micro-commit each: `generate_hazard_textures.py`
(props 1-55), `generate_room_textures.py` (props 56-85 + 30 Conference/Office/Lab furniture),
`generate_furniture_textures.py` (11 original furniture blocks), `generate_school_textures.py` (10
school decor blocks), `generate_cafeteria_textures.py` (16 cafeteria blocks),
`generate_safety_textures.py` (5 safety-equipment blocks + item sprites), `generate_badminton_textures.py`,
`generate_basketball_textures.py`. `generate_light_bulb_textures.py` was deliberately left alone —
it's a seamless glow tile by design; banding would draw a visible seam when tiles are placed
edge-to-edge.

`generate_hazard_state_tiers.py` was re-run after every script in this list that also feeds hazard
prop bodies, so the `_hz`/`_of` tier textures stay derived from the current (now flat-shaded)
normal-state art rather than the pre-restyle originals.

## 3. Creative-tab reorganization

**Problem:** `HAZARD_TAB` held all 85 hazard props in one scrollable tab; `FURN_TAB` held 86
furniture items (83 after the fire hose cabinet moved out) in another. Both were hard to browse.

**Fix:** `ModCreativeTabs` now registers 4 hazard tabs by zone and 3 furniture tabs by room, plus
the unchanged `SIM_TAB`/`NPC_TAB` (9 mod tabs total):

| Tab | Items | Source |
|---|---|---|
| Hazards: Classroom | 16 | `ModItems.HAZARD_ZONE_CLASSROOM` |
| Hazards: Kitchen | 27 | `ModItems.HAZARD_ZONE_KITCHEN` |
| Hazards: Electrical & Lab | 22 | `ModItems.HAZARD_ZONE_ELECTRICAL_LAB` |
| Hazards: Conference & Office | 20 | `ModItems.HAZARD_ZONE_CONFERENCE_OFFICE` |
| Furniture: School | 35 | hand-listed in `FURN_SCHOOL_TAB` |
| Furniture: Cafeteria | 16 | hand-listed in `FURN_CAFETERIA_TAB` |
| Furniture: Office & Lab | 31 | hand-listed in `FURN_OFFICE_LAB_TAB` (30 room-batch blocks + `science_lab_workbench`) |

`ModItems.HAZARD_ITEM_MAP` (and its `/item hazard` tab-completion order) is untouched; the 4 zone
lists are a separate, additive grouping read only by the tab builders.
`ModItems.assertHazardZonesCoverMap()` runs at `ModCreativeTabs` class-load and throws with the
exact missing/extra keys if the zone lists and the map ever drift apart, rather than silently
dropping a future hazard prop from every tab. `FIRE_HOSE_CABINET_ITEM` moved from furniture into
`SIM_TAB` alongside the other safety-equipment items.

Both splits were verified by script before committing: every hazard-zone list diffed against
`HAZARD_ITEM_MAP`'s actual 85 keys (0 missing, 0 extra, 0 cross-list duplicates), and the 3
furniture tabs' combined item set diffed against the original `FURN_TAB`'s 83 non-fire-hose items
(0 missing, 0 extra, 0 cross-tab duplicates).
