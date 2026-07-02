#!/usr/bin/env python3
"""Generates the light_bulb block's texture.

light_bulb is a full-cube glowing ceiling tile (light level 15, vanilla's
hard cap) meant to be tiled edge-to-edge like a lit ceiling carpet. The
texture is deliberately a flat, seamless, near-solid white fill with no
border, bevel, or grid lines — any of those would draw a visible "frame"
around each tile and break the continuous-glow look when several are placed
together. A faint symmetric noise keeps it from looking like a flat digital
fill without introducing any edge-vs-center asymmetry (which would tile
visibly). Run from repo root:

    python3 scripts/generate_light_bulb_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import os
import random
from PIL import Image

random.seed(7)

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "berongsmp", "textures", "block")
os.makedirs(OUT, exist_ok=True)

W = H = 16

# Near-pure daylight white — bright enough to read as maximally lit,
# with a faint cool tint so it looks like a modern LED rather than a
# warm/yellow vanilla light source.
BASE = (250, 251, 255)


def tex_led_diffuser_glow():
    im = Image.new("RGB", (W, H), BASE)
    for y in range(H):
        for x in range(W):
            # Tiny symmetric noise (+/-2) for texture believability; no
            # directional gradient and no border, so tiles butt seamlessly.
            n = random.randint(-2, 2)
            c = tuple(max(0, min(255, v + n)) for v in BASE)
            im.putpixel((x, y), c)
    im.save(os.path.join(OUT, "led_diffuser_glow.png"))


ALL = [tex_led_diffuser_glow]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
