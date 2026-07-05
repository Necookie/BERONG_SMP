"""Adds the new `on_fire` boolean property to all 19 HazardBlock/HazardFacingBlock blockstate
JSONs (woodshop_sawdust_layer uses its own ACCUMULATION mechanic and is untouched). Each
`on_fire=true` variant reuses the existing `_hazardous` model — no new art, per the scope
decision to signal "on fire" via amplified particles + real adjacent fire rather than bespoke
burning textures for all 20 props. Idempotent: safe to re-run.
"""
import json
from pathlib import Path

BLOCKSTATES = Path("src/main/resources/assets/berongsmp/blockstates")

FACING_BLOCKS = [
    "archive_box_stack", "daisy_chain_extension", "stage_spotlight", "dust_choked_pc",
    "charging_cart", "malfunctioning_vending", "ceiling_projector", "vape_in_iron_locker",
    "pa_system_backup", "smartboard_inverter", "unattended_grease_pan", "grease_clogged_hood",
    "contaminated_kitchen_bin", "jammed_panini_press", "commercial_deep_fryer", "plastic_trash_bin",
]
NON_FACING_BLOCKS = ["frayed_console_wire", "swollen_phone_battery", "damaged_lipo_pack"]

FACINGS = ["north", "south", "east", "west"]
FACING_Y = {"north": None, "south": 180, "east": 90, "west": 270}


def hazardous_entry(name, facing=None):
    entry = {"model": f"berongsmp:block/{name}_hazardous"}
    if facing is not None and FACING_Y[facing] is not None:
        entry["y"] = FACING_Y[facing]
    return entry


def normal_entry(name, facing=None):
    entry = {"model": f"berongsmp:block/{name}"}
    if facing is not None and FACING_Y[facing] is not None:
        entry["y"] = FACING_Y[facing]
    return entry


def build_facing(name):
    variants = {}
    for facing in FACINGS:
        variants[f"facing={facing},hazardous=false,on_fire=false"] = normal_entry(name, facing)
        variants[f"facing={facing},hazardous=false,on_fire=true"] = hazardous_entry(name, facing)
        variants[f"facing={facing},hazardous=true,on_fire=false"] = hazardous_entry(name, facing)
        variants[f"facing={facing},hazardous=true,on_fire=true"] = hazardous_entry(name, facing)
    return variants


def build_non_facing(name):
    return {
        "hazardous=false,on_fire=false": normal_entry(name),
        "hazardous=false,on_fire=true": hazardous_entry(name),
        "hazardous=true,on_fire=false": hazardous_entry(name),
        "hazardous=true,on_fire=true": hazardous_entry(name),
    }


def write(name, variants):
    path = BLOCKSTATES / f"{name}.json"
    path.write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {path}")


def main():
    for name in FACING_BLOCKS:
        write(name, build_facing(name))
    for name in NON_FACING_BLOCKS:
        write(name, build_non_facing(name))


if __name__ == "__main__":
    main()
