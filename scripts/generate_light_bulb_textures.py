#!/usr/bin/env python3
"""Generates the two 16x16 textures for the light_bulb block.

Mirrors the flat-shaded, bordered, bevel-highlighted style used by
generate_hazard_textures.py, but tuned for a modern flush-mount LED panel
fixture: a cool-white/daylight diffuser (not the warm yellow of glowstone or
the cyan tint of sea_lantern) recessed slightly behind a white plastic
housing bezel, giving a thin backlit edge-glow when viewed from below. Run
from repo root:

    python3 scripts/generate_light_bulb_textures.py

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


def gradient_shade(im, x0, y0, x1, y1, base, light=1.1, dark=0.85):
    """Diagonal top-left-lit shading across a rectangular region."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) + (y - y0)) / span
            factor = light + (dark - light) * t
            set_px(im, x, y, scale(base, factor))


def inset_edges(im, x0, y0, x1, y1, hi, lo):
    """1px bright top/left + dark bottom/right, like a beveled panel."""
    rect(im, x0, y0, x1, y0, hi)
    rect(im, x0, y0, x0, y1, hi)
    rect(im, x0, y1, x1, y1, lo)
    rect(im, x1, y0, x1, y1, lo)


def radial_hotspot(im, cx, cy, r, base):
    """Brightens pixels toward (cx, cy), simulating an LED-array glow core."""
    for y in range(H):
        for x in range(W):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            t = max(0.0, 1.0 - d / r)
            factor = 1.0 + t * 0.5
            cur = im.getpixel((x, y))
            im.putpixel((x, y), scale(cur, factor))


def save(im, name):
    im.save(os.path.join(OUT, f"{name}.png"))


def tex_led_diffuser_glow():
    """Cool daylight-white diffuser lens — bright, near-neutral, faint grid."""
    im = new_canvas((225, 236, 248))
    gradient_shade(im, 0, 0, W - 1, H - 1, (225, 236, 248), light=1.08, dark=0.92)
    radial_hotspot(im, 8, 8, 10, (225, 236, 248))
    # Faint diffuser grid lines (subtle, reads as a real LED panel lens).
    for x in range(0, W, 4):
        rect(im, x, 0, x, H - 1, scale((225, 236, 248), 0.95))
    for y in range(0, H, 4):
        rect(im, 0, y, W - 1, y, scale((225, 236, 248), 0.95))
    inset_edges(im, 0, 0, W - 1, H - 1, (255, 255, 255), (200, 214, 230))
    border(im, (210, 222, 236))
    save(im, "led_diffuser_glow")


def tex_led_housing_white():
    """Matte white plastic bezel/housing for the fixture frame."""
    im = new_canvas((232, 232, 230))
    gradient_shade(im, 1, 1, 14, 14, (232, 232, 230), light=1.18, dark=0.8)
    inset_edges(im, 1, 1, 14, 14, (250, 250, 248), (170, 170, 168))
    border(im, (150, 150, 148))
    # Two small corner mounting screws.
    set_px(im, 2, 2, (90, 90, 88))
    set_px(im, 13, 13, (90, 90, 88))
    save(im, "led_housing_white")


ALL = [tex_led_diffuser_glow, tex_led_housing_white]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} textures to {OUT}")
