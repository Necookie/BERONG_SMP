#!/usr/bin/env python3
"""Applies the universal Tier-2 (HAZARDOUS / "Caution Amber") and Tier-3
(ON_FIRE / "Char & Ember") visual language (docs/texture_design_system.md
§5) to every hazard prop block, so a prop's danger state is readable at a
glance regardless of which specific object it is.

For every `<prop>_hazardous.json` model:
  1. Every texture it references gets its own `<name>_hz.png` copy (deduped
     by source filename across all props, so a shared texture like
     fixture_chrome is only transformed once) generated via hazardize() from
     the *original* texture and repointed via the model's own "textures"
     map. The original PNG is never mutated in place — several accent/body
     textures (e.g. hazard_glass_screen_off) are also referenced by
     normal-state furniture models elsewhere and must stay untouched.
  2. A sibling `<prop>_on_fire.json` model is generated the same way with
     charify() and `_of.png` copies.
  3. Every blockstate JSON's `on_fire=true` variants (previously aliased to
     the `_hazardous` model — see add_onfire_blockstates.py's original scope
     note, which only covered the first 19 props and always aliased on_fire
     to hazardous art) are repointed to the new `_on_fire` model.

Idempotent: source textures are recovered by stripping any existing
`_hz`/`_of` suffix before reading, so re-running (e.g. after hand-editing a
body texture) always regenerates from the pristine original.

Run from repo root:
    python3 scripts/generate_hazard_state_tiers.py
"""
import json
import os
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import hazardize, charify

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/berongsmp/models/block"
TEXTURES = ROOT / "src/main/resources/assets/berongsmp/textures/block"
BLOCKSTATES = ROOT / "src/main/resources/assets/berongsmp/blockstates"

NS = "berongsmp:block/"
TIER_SUFFIXES = ("_hz", "_of")


def texture_path(ref: str) -> Path:
    return TEXTURES / f"{ref[len(NS):]}.png"


def strip_tier_suffix(ref: str) -> str:
    for suf in TIER_SUFFIXES:
        if ref.endswith(suf):
            return ref[: -len(suf)]
    return ref


def derive_ref(ref: str, suffix: str) -> str:
    return f"{ref}_{suffix}"


def build_tier(src_ref: str, suffix: str, transform, cache: dict, seed: int) -> str:
    if not src_ref.startswith(NS):
        return src_ref  # non-mod texture (rare); leave untouched
    base_ref = strip_tier_suffix(src_ref)
    key = (base_ref, suffix)
    if key in cache:
        return cache[key]
    im = Image.open(texture_path(base_ref))
    out_im = transform(im, seed=seed)
    new_ref = derive_ref(base_ref, suffix)
    out_im.save(texture_path(new_ref))
    cache[key] = new_ref
    return new_ref


def process_model(hazardous_path: Path, cache: dict) -> str:
    prop_name = hazardous_path.stem[: -len("_hazardous")]
    data = json.loads(hazardous_path.read_text(encoding="utf-8"))
    original_textures = dict(data.get("textures", {}))
    seed = sum(prop_name.encode()) % 7

    hz_textures = {k: build_tier(v, "hz", hazardize, cache, seed) for k, v in original_textures.items()}
    data["textures"] = hz_textures
    hazardous_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

    onfire_data = json.loads(json.dumps(data))
    of_textures = {k: build_tier(v, "of", charify, cache, seed) for k, v in original_textures.items()}
    onfire_data["textures"] = of_textures
    onfire_path = hazardous_path.parent / f"{prop_name}_on_fire.json"
    onfire_path.write_text(json.dumps(onfire_data, indent=2) + "\n", encoding="utf-8")
    return prop_name


def repoint_blockstates(prop_names: set) -> int:
    count = 0
    for path in BLOCKSTATES.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        variants = data.get("variants")
        if not variants:
            continue
        changed = False
        for key, entry in variants.items():
            if "on_fire=true" not in key or not isinstance(entry, dict):
                continue
            model = entry.get("model", "")
            if not model.startswith(NS) or not model.endswith("_hazardous"):
                continue
            base = model[len(NS): -len("_hazardous")]
            if base in prop_names:
                entry["model"] = f"{NS}{base}_on_fire"
                changed = True
        if changed:
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            count += 1
    return count


def main():
    cache = {}
    prop_names = set()
    hazardous_models = sorted(MODELS.glob("*_hazardous.json"))
    for path in hazardous_models:
        prop_names.add(process_model(path, cache))
    changed = repoint_blockstates(prop_names)
    print(f"Processed {len(hazardous_models)} hazardous models "
          f"({len(cache)} unique tier textures generated).")
    print(f"Repointed on_fire=true variants in {changed} blockstate files.")


if __name__ == "__main__":
    main()
