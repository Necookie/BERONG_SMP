#!/usr/bin/env python3
"""Generates modern, recognizable 16x16 textures for the furniture blocks.

The 11 furniture blocks (whiteboard, toilet, sink, drawers, computer_table,
chair, filing_cabinet, locker, trash_can, bulletin_board, ceiling_fan)
previously stretched raw vanilla textures (oak_planks, iron_block,
light_gray_concrete, ...) over their cuboids, reading as flat/generic boxes.
This mirrors the same flat-shaded, bevel-highlighted, hand-iconography style
already used for the 20 hazard props (see generate_hazard_textures.py), but
aimed at a clean modern-office/institutional palette (matte panels,
brushed-metal accents) instead of grime/hazard accents. Run from repo root:

    python3 scripts/generate_furniture_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import os
from PIL import Image

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
    """Diagonal top-left-lit shading across a rectangular region."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) + (y - y0)) / span
            factor = light + (dark - light) * t
            set_px(im, x, y, scale(base, factor))


def vertical_brush(im, x0, y0, x1, y1, base, stripe=1.12):
    """Subtle alternating-column brushed-metal streaks."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            factor = stripe if (x % 2 == 0) else 1.0 / stripe
            set_px(im, x, y, scale(base, factor))


def inset_edges(im, x0, y0, x1, y1, hi, lo):
    """1px bright top/left + dark bottom/right, like a beveled panel."""
    rect(im, x0, y0, x1, y0, hi)
    rect(im, x0, y0, x0, y1, hi)
    rect(im, x0, y1, x1, y1, lo)
    rect(im, x1, y0, x1, y1, lo)


def vents(im, y_positions, color, x0=2, x1=13):
    for y in y_positions:
        rect(im, x0, y, x1, y, color)


def save(im, name):
    im.save(os.path.join(OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# ComputerTableBlock — modern office desk (was: raw oak_planks + oak_log)
# ---------------------------------------------------------------------------

def tex_desk_laminate_white():
    base = (232, 233, 235)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.08, dark=0.94)
    inset_edges(im, 0, 0, W - 1, H - 1, (250, 250, 251), (195, 197, 200))
    border(im, (170, 172, 175))
    save(im, "desk_laminate_white")


def tex_desk_leg_metal_black():
    base = (32, 33, 36)
    im = new_canvas(base)
    vertical_brush(im, 1, 0, 14, 15, base, stripe=1.18)
    inset_edges(im, 0, 0, W - 1, H - 1, (70, 72, 76), (10, 10, 12))
    # Foot cap highlight at the bottom.
    rect(im, 1, 14, 14, 15, (55, 56, 60))
    save(im, "desk_leg_metal_black")


def tex_desk_cable_panel_dark():
    base = (24, 25, 28)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.85)
    border(im, (12, 12, 14))
    # Grommet hole (dark ring) + vent slats.
    rect(im, 6, 5, 9, 8, (10, 10, 11))
    rect(im, 7, 6, 8, 7, (2, 2, 3))
    vents(im, [11, 12], (45, 46, 50), x0=3, x1=12)
    save(im, "desk_cable_panel_dark")


ALL = [
    tex_desk_laminate_white, tex_desk_leg_metal_black, tex_desk_cable_panel_dark,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
