#!/usr/bin/env python3
"""Generates the 16x16 flat item icon for berongsmp:hazard_wand.

Replaces the vanilla minecraft:item/blaze_rod placeholder texture with a
themed dev tool: a dark rod gripped in caution hazard-stripe tape with a
glowing warning tip, evoking a "hazard detector" wand rather than a generic
reused vanilla item. Run from repo root:

    python3 scripts/generate_hazard_wand_texture.py

Outputs to src/main/resources/assets/berongsmp/textures/item/hazard_wand.png.
"""
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import apply_icon_signature

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "berongsmp", "textures", "item")
os.makedirs(OUT, exist_ok=True)

W = H = 16

ROD_DARK = (46, 40, 38)
ROD_LIGHT = (74, 66, 62)
STRIPE_YELLOW = (232, 178, 26)
STRIPE_BLACK = (24, 22, 20)
TIP_CORE = (255, 214, 92)
TIP_GLOW = (255, 236, 158)
TIP_EDGE = (214, 132, 24)
OUTLINE = (18, 16, 15)


def new_canvas():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def set_px(im, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), color if len(color) == 4 else (*color, 255))


def thick_diag(im, color, offset, width=2):
    """Draws a diagonal band (bottom-left -> top-right) `width` px thick."""
    for i in range(16):
        x = i
        y = 15 - i + offset
        for w in range(width):
            set_px(im, x, y - w, color)


def main():
    im = new_canvas()

    # Rod body: diagonal band from bottom-left grip to top-right glowing tip.
    thick_diag(im, ROD_DARK, offset=0, width=3)
    thick_diag(im, ROD_LIGHT, offset=1, width=1)

    # Caution hazard-tape wrap near the grip (bottom-left third).
    for i in range(0, 6):
        x = i
        y = 15 - i
        color = STRIPE_YELLOW if i % 2 == 0 else STRIPE_BLACK
        set_px(im, x, y, color)
        set_px(im, x, y - 1, color)
        set_px(im, min(x + 1, 15), y - 1, color if i % 2 == 0 else STRIPE_BLACK)

    # Glowing warning tip (top-right).
    tip_cells = [(13, 2), (14, 1), (15, 0), (12, 3), (14, 2), (13, 1)]
    for x, y in tip_cells:
        set_px(im, x, y, TIP_GLOW)
    for x, y in [(14, 3), (15, 1), (13, 3), (15, 2)]:
        set_px(im, x, y, TIP_CORE)
    for x, y in [(12, 4), (15, 3), (12, 2)]:
        set_px(im, x, y, TIP_EDGE)

    # Small detached spark above the tip (detector "ping").
    set_px(im, 14, 0, (255, 245, 200, 180))

    # Thin dark outline under the grip end for a finished silhouette.
    set_px(im, 0, 15, OUTLINE)
    set_px(im, 1, 14, OUTLINE)

    apply_icon_signature(im)

    path = os.path.join(OUT, "hazard_wand.png")
    im.save(path)
    print(f"wrote {path}")


if __name__ == "__main__":
    main()
