#!/usr/bin/env python3
"""Generates textures for the basketball court set: the expandable hoop
stand's dark pole + weighted base plate, the white backboard (with the
classic black shooter's-square outline), the orange rim, and a plain
orange basketball item icon with black seam lines. The hoop's hanging net
reuses the existing badminton_net_mesh/_edge textures — same diamond
lattice, no need to redraw it.

Mirrors the flat-shaded, bevel-highlighted style used by the other
generate_*_textures.py scripts (block helpers) and the transparent-icon
style used by generate_safety_textures.py (item helpers). Run from repo
root:

    python3 scripts/generate_basketball_textures.py

Outputs to:
    src/main/resources/assets/berongsmp/textures/block/
    src/main/resources/assets/berongsmp/textures/item/
"""
import math
import os
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import apply_icon_signature, apply_material_signature

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "assets", "berongsmp", "textures")
BLOCK_OUT = os.path.join(ASSETS, "block")
ITEM_OUT = os.path.join(ASSETS, "item")
os.makedirs(BLOCK_OUT, exist_ok=True)
os.makedirs(ITEM_OUT, exist_ok=True)

W = H = 16


def block_canvas(bg):
    return Image.new("RGB", (W, H), bg)


def item_canvas():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def set_px(im, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), color)


def rgba(color, alpha=255):
    return (color[0], color[1], color[2], alpha)


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
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3])


def inset_edges(im, x0, y0, x1, y1, hi, lo):
    rect(im, x0, y0, x1, y0, hi)
    rect(im, x0, y0, x0, y1, hi)
    rect(im, x0, y1, x1, y1, lo)
    rect(im, x1, y0, x1, y1, lo)


def gradient_shade(im, x0, y0, x1, y1, base, light=1.45, dark=0.62, bands=4):
    """Flat-shaded diagonal top-left-lit banding — quantized into discrete
    tone steps rather than a smooth per-pixel gradient, matching the
    mod-wide signature contrast (_texture_style.LIGHT_FACTOR/SHADOW_FACTOR)."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) + (y - y0)) / span
            step = round(t * (bands - 1)) / (bands - 1) if bands > 1 else 0
            factor = light + (dark - light) * step
            set_px(im, x, y, scale(base, factor))


def gradient_shade_rgba(im, x0, y0, x1, y1, light_base, dark_base, bands=4):
    """Like gradient_shade but only repaints already-opaque pixels of a
    transparent RGBA canvas, leaving the silhouette intact. Banded the same
    way as gradient_shade for a consistent flat-shaded look."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < H and im.getpixel((x, y))[3] > 0:
                t = ((x - x0) + (y - y0)) / span
                step = round(t * (bands - 1)) / (bands - 1) if bands > 1 else 0
                c = tuple(int(light_base[i] + (dark_base[i] - light_base[i]) * step) for i in range(3))
                im.putpixel((x, y), rgba(c))


def vertical_brush(im, x0, y0, x1, y1, base, stripe=1.15):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            factor = stripe if (x % 2 == 0) else 1.0 / stripe
            set_px(im, x, y, scale(base, factor))


def circle(im, cx, cy, r, color, outline=None, width=1):
    d = ImageDraw.Draw(im)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color, outline=outline, width=width)


def save_block(im, name):
    apply_material_signature(im)
    im.save(os.path.join(BLOCK_OUT, f"{name}.png"))


def save_item(im, name):
    apply_icon_signature(im)
    im.save(os.path.join(ITEM_OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# Hoop stand pole — dark brushed steel, plus a wide weighted base plate.
# ---------------------------------------------------------------------------

def tex_basketball_pole_metal():
    base = (48, 50, 54)
    im = block_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.22)
    rect(im, 6, 0, 9, 15, scale(base, 1.5))  # rounded pole highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (86, 90, 96), (18, 19, 21))
    save_block(im, "basketball_pole_metal")


def tex_basketball_pole_base_plate():
    base = (58, 60, 64)
    im = block_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.2, dark=0.85)
    circle(im, 7.5, 7.5, 6.5, None, outline=scale(base, 1.5), width=1)
    circle(im, 7.5, 7.5, 3.5, None, outline=scale(base, 0.6), width=1)
    border(im, (20, 21, 23))
    save_block(im, "basketball_pole_base_plate")


# ---------------------------------------------------------------------------
# Backboard — white acrylic panel with the classic black shooter's square.
# ---------------------------------------------------------------------------

def tex_basketball_backboard_white():
    base = (248, 248, 244)
    im = block_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.05, dark=0.96)
    rect(im, 1, 1, 14, 1, (60, 60, 58))  # outer frame top
    rect(im, 1, 14, 14, 14, (60, 60, 58))
    rect(im, 1, 1, 1, 14, (60, 60, 58))
    rect(im, 14, 1, 14, 14, (60, 60, 58))
    # The shooter's square, offset toward the bottom (rim side).
    rect(im, 5, 8, 10, 13, base)
    inset_edges(im, 5, 8, 10, 13, (40, 40, 38), (40, 40, 38))
    border(im, (200, 200, 196))
    save_block(im, "basketball_backboard_white")


# ---------------------------------------------------------------------------
# Rim — bright hoop orange with a darker underside shadow.
# ---------------------------------------------------------------------------

def tex_basketball_rim_orange():
    base = (222, 96, 20)
    im = block_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.25, dark=0.7)
    rect(im, 0, 12, 15, 15, scale(base, 0.55))  # underside shadow
    border(im, (110, 42, 6))
    save_block(im, "basketball_rim_orange")


# ---------------------------------------------------------------------------
# Basketball — orange sphere with black seam lines (item icon).
# ---------------------------------------------------------------------------

def tex_basketball():
    im = item_canvas()
    ORANGE_HI = (245, 152, 62)
    ORANGE_DK = (168, 74, 12)
    BLACK = (32, 22, 14)
    circle(im, 7.5, 7.5, 6.5, rgba(ORANGE_HI))
    gradient_shade_rgba(im, 0, 0, W - 1, H - 1, ORANGE_HI, ORANGE_DK)
    d = ImageDraw.Draw(im)
    d.line([(7, 1), (7, 14)], fill=rgba(BLACK), width=1)
    d.line([(1, 7), (14, 7)], fill=rgba(BLACK), width=1)
    d.arc((1, 1, 14, 14), start=200, end=340, fill=rgba(BLACK), width=1)
    d.arc((1, 1, 14, 14), start=20, end=160, fill=rgba(BLACK), width=1)
    set_px(im, 5, 5, rgba((255, 200, 150)))  # highlight glint
    save_item(im, "basketball")


ALL_BLOCK = [
    tex_basketball_pole_metal,
    tex_basketball_pole_base_plate,
    tex_basketball_backboard_white,
    tex_basketball_rim_orange,
]

ALL_ITEM = [
    tex_basketball,
]

if __name__ == "__main__":
    for fn in ALL_BLOCK:
        fn()
    for fn in ALL_ITEM:
        fn()
    print(f"Wrote {len(ALL_BLOCK)} block texture(s) and {len(ALL_ITEM)} item texture(s)")
