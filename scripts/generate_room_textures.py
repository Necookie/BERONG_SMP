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
# Phase 1: Conference Room furniture
# ---------------------------------------------------------------------------

def tex_conf_table_top():
    base = (38, 38, 44)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.25, dark=0.85)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.3, band=1, offset=2)
    inset_edges(im, 0, 0, W - 1, H - 1, (66, 66, 74), (18, 18, 22))
    border(im, (14, 14, 17))
    save(im, "conf_table_top")


def tex_conf_table_leg():
    base = (176, 181, 189)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.18)
    rect(im, 7, 0, 8, 15, scale(base, 1.4))
    inset_edges(im, 0, 0, W - 1, H - 1, (218, 222, 228), (110, 114, 122))
    save(im, "conf_table_leg")


def tex_exec_chair_leather():
    base = (92, 28, 30)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.82)
    for y in (3, 7, 11):
        for x in (2, 6, 10, 14):
            set_px(im, x, y, scale(base, 0.6))  # tufted-button dimples
    inset_edges(im, 0, 0, W - 1, H - 1, (140, 52, 54), (48, 12, 14))
    border(im, (40, 10, 12))
    save(im, "exec_chair_leather")


def tex_exec_chair_frame():
    base = (36, 38, 42)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.2)
    inset_edges(im, 0, 0, W - 1, H - 1, (70, 74, 80), (10, 11, 13))
    save(im, "exec_chair_frame")


def tex_conf_credenza_wood():
    base = (96, 62, 40)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    wood_grain(im, 1, 1, 14, 14, base, rows=[3, 7, 11], variance=0.05)
    inset_edges(im, 0, 0, W - 1, H - 1, (128, 88, 58), (58, 36, 22))
    border(im, (50, 30, 18))
    save(im, "conf_credenza_wood")


def tex_conf_credenza_front():
    base = (78, 48, 30)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.1, dark=0.9)
    border(im, (40, 22, 12))
    rect(im, 8, 1, 8, 14, scale(base, 0.55))  # sliding-door seam
    for x in (5, 11):
        rect(im, x, 7, x, 8, (22, 20, 18))  # handles
    save(im, "conf_credenza_front")


def tex_conf_display_frame():
    base = (28, 28, 31)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.85)
    inset_edges(im, 0, 0, W - 1, H - 1, (52, 52, 56), (8, 8, 10))
    border(im, (10, 10, 12))
    save(im, "conf_display_frame")


def tex_flip_chart_paper():
    base = (232, 227, 212)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.06, dark=0.94)
    palette = [(60, 90, 160), (170, 50, 50), (50, 130, 80)]
    for i, y in enumerate((4, 7, 10)):
        rect(im, 2, y, 2 + 6 + i * 2, y, random.choice(palette))
    inset_edges(im, 0, 0, W - 1, H - 1, (250, 246, 232), (170, 165, 150))
    border(im, (150, 144, 128))
    save(im, "flip_chart_paper")


def tex_conf_speakerphone_body():
    base = (46, 46, 50)
    im = new_canvas(base)
    disc_fill(im, 7.5, 7.5, 7, base)
    disc_ring(im, 7.5, 7.5, 6, scale(base, 1.4), thickness=1.0)
    disc_ring(im, 7.5, 7.5, 3, scale(base, 0.6), thickness=1.0)
    indicator_dots(im, [(7, 7), (8, 7), (7, 8), (8, 8)], (60, 200, 90))
    save(im, "conf_speakerphone_body")


def tex_glass_partition_pane():
    base = (196, 212, 218)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.94)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.25, band=2, offset=-3)
    inset_edges(im, 0, 0, W - 1, H - 1, (60, 66, 70), (60, 66, 70))
    border(im, (58, 64, 68))
    save(im, "glass_partition_pane")


def tex_sofa_fabric_cushion():
    base = (122, 110, 98)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    for y in (4, 10):
        for x in (3, 7, 11):
            set_px(im, x, y, scale(base, 0.6))
    inset_edges(im, 0, 0, W - 1, H - 1, (156, 142, 128), (70, 60, 50))
    border(im, (62, 52, 44))
    save(im, "sofa_fabric_cushion")


def tex_sofa_fabric_side():
    base = (96, 86, 76)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    inset_edges(im, 0, 0, W - 1, H - 1, (124, 112, 98), (54, 46, 38))
    save(im, "sofa_fabric_side")


def tex_office_plant_leaves():
    base = (58, 96, 52)
    im = new_canvas(base)
    palette = [(46, 110, 54), (70, 128, 60), (38, 90, 46)]
    for _ in range(24):
        x = random.randint(1, 14)
        y = random.randint(1, 14)
        set_px(im, x, y, random.choice(palette))
    border(im, (30, 64, 34))
    save(im, "office_plant_leaves")


def tex_office_planter_pot():
    base = (150, 90, 55)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.8)
    rect(im, 0, 0, 15, 1, scale(base, 1.3))  # rim highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (188, 122, 78), (94, 52, 28))
    border(im, (80, 44, 24))
    save(im, "office_planter_pot")


def tex_window_blinds_slats():
    base = (222, 218, 205)
    im = new_canvas(base)
    for y in range(0, H, 2):
        rect(im, 0, y, W - 1, y, scale(base, 1.1))
        rect(im, 0, y + 1, W - 1, y + 1, scale(base, 0.7))
    border(im, (150, 146, 132))
    save(im, "window_blinds_slats")


ALL += [
    tex_conf_table_top, tex_conf_table_leg,
    tex_exec_chair_leather, tex_exec_chair_frame,
    tex_conf_credenza_wood, tex_conf_credenza_front,
    tex_conf_display_frame,
    tex_flip_chart_paper,
    tex_conf_speakerphone_body,
    tex_glass_partition_pane,
    tex_sofa_fabric_cushion, tex_sofa_fabric_side,
    tex_office_plant_leaves, tex_office_planter_pot,
    tex_window_blinds_slats,
]

# ---------------------------------------------------------------------------
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
