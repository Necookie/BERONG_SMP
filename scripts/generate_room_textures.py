#!/usr/bin/env python3
"""Generates 16x16 textures for the 60 Conference Room / Office / Laboratory blocks
(30 furniture + 30 hazard props, props 56-85 in docs/hazard_props_spec.md).

One batch script per content drop, matching the repo's existing precedent
(generate_school_textures.py, generate_cafeteria_textures.py,
generate_safety_textures.py) rather than growing generate_hazard_textures.py
further. Same helper functions/style as those scripts; hazard props reuse the
shared hazard-accent PNGs already on disk (hazard_ember_glow, hazard_warning_led,
hazard_spark_arc, hazard_smoke_stain, hazard_scorch_char, hazard_bare_copper,
hazard_water_stain, hazard_glass_screen_off/_glitch, ...) by reference in model
JSON rather than regenerating them here.

Run from repo root:

    python3 scripts/generate_room_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import math
import os
import random
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import apply_material_signature

random.seed(85)

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "berongsmp", "textures", "block")
os.makedirs(OUT, exist_ok=True)

W = H = 16


def new_canvas(bg):
    return Image.new("RGB", (W, H), bg)


def set_px(im, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), color)


def rect(im, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            set_px(im, x, y, color)


def border(im, color, thickness=1):
    rect(im, 0, 0, W - 1, thickness - 1, color)
    rect(im, 0, H - thickness, W - 1, H - 1, color)
    rect(im, 0, 0, thickness - 1, H - 1, color)
    rect(im, W - thickness, 0, W - 1, H - 1, color)


def scale(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def gradient_shade(im, x0, y0, x1, y1, base, light=1.3, dark=0.75):
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) + (y - y0)) / span
            factor = light + (dark - light) * t
            set_px(im, x, y, scale(base, factor))


def vertical_brush(im, x0, y0, x1, y1, base, stripe=1.15):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            factor = stripe if (x % 2 == 0) else 1.0 / stripe
            set_px(im, x, y, scale(base, factor))


def inset_edges(im, x0, y0, x1, y1, hi, lo):
    rect(im, x0, y0, x1, y0, hi)
    rect(im, x0, y0, x0, y1, hi)
    rect(im, x0, y1, x1, y1, lo)
    rect(im, x1, y0, x1, y1, lo)


def speckle(im, colors, density=0.1, x0=0, y0=0, x1=W - 1, y1=H - 1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if random.random() < density:
                set_px(im, x, y, random.choice(colors))


def wood_grain(im, x0, y0, x1, y1, base, rows, variance=0.06):
    for y in rows:
        factor = 1.0 + random.uniform(-variance, variance)
        rect(im, x0, y, x1, y, scale(base, factor))


def disc_fill(im, cx, cy, r, color):
    for y in range(H):
        for x in range(W):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                set_px(im, x, y, color)


def disc_ring(im, cx, cy, r, color, thickness=1.1):
    for y in range(H):
        for x in range(W):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if r - thickness <= d <= r:
                set_px(im, x, y, color)


def diagonal_sheen(im, x0, y0, x1, y1, factor=1.25, band=2, offset=0):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if abs((x - y) - offset) <= band:
                cur = im.getpixel((x, y))
                set_px(im, x, y, scale(cur, factor))


def indicator_dots(im, positions, color):
    """Small 1-2px status LEDs at given (x, y) points."""
    for (x, y) in positions:
        set_px(im, x, y, color)


def save(im, name):
    apply_material_signature(im)
    im.save(os.path.join(OUT, f"{name}.png"))


ALL = []

# ---------------------------------------------------------------------------
# Phase 1: Conference Room furniture textures appended here.
# Phase 2: Conference Room hazard textures appended here.
# Phase 3: Office furniture textures appended here.
# Phase 4: Office hazard textures appended here.
# Phase 5: Laboratory furniture textures appended here.
# Phase 6: Laboratory hazard textures appended here.
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
