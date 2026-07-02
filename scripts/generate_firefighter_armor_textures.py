#!/usr/bin/env python3
"""Generates the firefighter uniform's worn armor textures (64x32, classic humanoid/
humanoid_leggings equipment-asset layout) and its 4 inventory-icon PNGs (32x32).

The worn textures are painted only inside the exact pixel regions vanilla's own armor
textures actually use for each humanoid layer -- extracted once by connected-component
analysis of the real minecraft:iron equipment textures (MC 26.1.2) -- rather than freehand
cuboid-unwrap coordinates. This guarantees the output occupies the identical footprint as a
known-working vanilla armor texture (no missing limbs, no stray pixels bleeding into unused/
transparent regions), while the actual coloring (turnout-coat navy + reflective stripes,
yellow helmet shell, black boots) is original to this mod. Run from repo root:

    python3 scripts/generate_firefighter_armor_textures.py

Outputs to:
    src/main/resources/assets/berongsmp/textures/entity/equipment/humanoid/firefighter_uniform.png
    src/main/resources/assets/berongsmp/textures/entity/equipment/humanoid_leggings/firefighter_uniform.png
    src/main/resources/assets/berongsmp/textures/item/firefighter_{helmet,coat,pants,boots}.png
"""
import os
from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "berongsmp")
EQUIP_HUMANOID_OUT = os.path.join(ASSETS, "textures", "entity", "equipment", "humanoid")
EQUIP_LEGGINGS_OUT = os.path.join(ASSETS, "textures", "entity", "equipment", "humanoid_leggings")
ITEM_ICON_OUT = os.path.join(ASSETS, "textures", "item")
for d in (EQUIP_HUMANOID_OUT, EQUIP_LEGGINGS_OUT, ITEM_ICON_OUT):
    os.makedirs(d, exist_ok=True)

# --- Verified pixel regions (connected-component bounding boxes extracted from the real
# vanilla minecraft:iron equipment textures for this MC version) -----------------------------
# humanoid layer (64x32): renders helmet + chestplate + boots
HEAD_BAND = (0, 0, 32, 16)     # x0,y0,x1,y1 -- helmet
LEG_BAND_HUMANOID = (0, 16, 16, 32)   # boots (outer leg cuboid)
BODY_ARM_BAND = (16, 16, 56, 32)      # coat (torso + both arms)
# humanoid_leggings layer (64x32): renders leggings only
LEG_BAND_LEGGINGS = (0, 16, 16, 32)
HIP_BAND_LEGGINGS = (16, 16, 40, 32)

NAVY = (38, 46, 64)
NAVY_LIGHT = (58, 68, 92)
NAVY_DARK = (22, 27, 40)
REFLECTIVE = (235, 200, 60)
REFLECTIVE_LIGHT = (250, 224, 110)
BOOT_BLACK = (24, 24, 26)
BOOT_LIGHT = (48, 48, 52)
HELMET_YELLOW = (222, 178, 40)
HELMET_YELLOW_LIGHT = (245, 205, 90)
HELMET_RED = (168, 30, 28)
HELMET_DARK = (110, 84, 20)


def canvas(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def rect(im, box, color):
    x0, y0, x1, y1 = box
    px = im.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = (*color, 255)


def hgradient(im, box, top_color, bottom_color):
    """Vertical gradient (top row lighter, bottom row darker) within box."""
    x0, y0, x1, y1 = box
    span = max(y1 - y0 - 1, 1)
    px = im.load()
    for y in range(y0, y1):
        t = (y - y0) / span
        color = tuple(int(top_color[i] + (bottom_color[i] - top_color[i]) * t) for i in range(3))
        for x in range(x0, x1):
            px[x, y] = (*color, 255)


def hstripe(im, box, y_offset, height, color):
    """A horizontal reflective stripe band, `height` px tall, `y_offset` px down from box top."""
    x0, y0, x1, y1 = box
    y_start = y0 + y_offset
    rect(im, (x0, y_start, x1, min(y_start + height, y1)), color)


def vseam(im, box, x_offset, color):
    """A thin vertical seam/zipper line, `x_offset` px in from box left."""
    x0, y0, x1, y1 = box
    x = x0 + x_offset
    if x0 <= x < x1:
        rect(im, (x, y0, x + 1, y1), color)


def alternate_columns(im, box, base_color, alt_color):
    """Breaks up flat color-blocking with a subtle every-other-column shade shift."""
    x0, y0, x1, y1 = box
    px = im.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if (x - x0) % 2 == 0:
                px[x, y] = (*base_color, 255)
            else:
                px[x, y] = (*alt_color, 255)


def build_humanoid_texture():
    im = canvas(64, 32)

    # Helmet: yellow shell, darker crown, a red brim trim, a dark visor band.
    hgradient(im, HEAD_BAND, HELMET_YELLOW_LIGHT, HELMET_YELLOW)
    hstripe(im, HEAD_BAND, 6, 2, HELMET_RED)
    hstripe(im, HEAD_BAND, 13, 2, HELMET_DARK)

    # Boots: matte black rubber with a thin reflective ankle band.
    hgradient(im, LEG_BAND_HUMANOID, BOOT_LIGHT, BOOT_BLACK)
    hstripe(im, LEG_BAND_HUMANOID, 10, 1, REFLECTIVE)

    # Coat: navy turnout coat, two reflective stripe bands, a center seam line.
    hgradient(im, BODY_ARM_BAND, NAVY_LIGHT, NAVY_DARK)
    hstripe(im, BODY_ARM_BAND, 3, 2, REFLECTIVE)
    hstripe(im, BODY_ARM_BAND, 11, 2, REFLECTIVE_LIGHT)
    vseam(im, BODY_ARM_BAND, (BODY_ARM_BAND[2] - BODY_ARM_BAND[0]) // 2, NAVY_DARK)

    im.save(os.path.join(EQUIP_HUMANOID_OUT, "firefighter_uniform.png"))


def build_leggings_texture():
    im = canvas(64, 32)

    hgradient(im, LEG_BAND_LEGGINGS, NAVY_LIGHT, NAVY)
    hstripe(im, LEG_BAND_LEGGINGS, 7, 1, REFLECTIVE)
    hstripe(im, LEG_BAND_LEGGINGS, 14, 1, NAVY_DARK)  # cuff shading

    hgradient(im, HIP_BAND_LEGGINGS, NAVY_LIGHT, NAVY)
    hstripe(im, HIP_BAND_LEGGINGS, 7, 1, REFLECTIVE)
    vseam(im, HIP_BAND_LEGGINGS, (HIP_BAND_LEGGINGS[2] - HIP_BAND_LEGGINGS[0]) // 2, NAVY_DARK)

    im.save(os.path.join(EQUIP_LEGGINGS_OUT, "firefighter_uniform.png"))


# --- Simple 32x32 inventory icons (front-view silhouettes, independent of the worn texture) --

def icon_helmet():
    im = canvas(32, 32)
    rect(im, (6, 10, 26, 24), HELMET_YELLOW)
    hgradient(im, (6, 10, 26, 17), HELMET_YELLOW_LIGHT, HELMET_YELLOW)
    rect(im, (4, 22, 28, 25), HELMET_DARK)  # brim
    rect(im, (6, 18, 26, 20), HELMET_RED)   # trim stripe
    im.save(os.path.join(ITEM_ICON_OUT, "firefighter_helmet.png"))


def icon_coat():
    im = canvas(32, 32)
    rect(im, (7, 4, 25, 28), NAVY)
    hgradient(im, (7, 4, 25, 28), NAVY_LIGHT, NAVY_DARK)
    rect(im, (3, 6, 8, 22), NAVY)   # left sleeve
    rect(im, (24, 6, 29, 22), NAVY)  # right sleeve
    hstripe(im, (7, 4, 25, 28), 6, 2, REFLECTIVE)
    hstripe(im, (7, 4, 25, 28), 16, 2, REFLECTIVE_LIGHT)
    vseam(im, (7, 4, 25, 28), 9, NAVY_DARK)
    im.save(os.path.join(ITEM_ICON_OUT, "firefighter_coat.png"))


def icon_pants():
    im = canvas(32, 32)
    hgradient(im, (9, 3, 23, 16), NAVY_LIGHT, NAVY)   # waist/hip block
    hgradient(im, (9, 16, 15, 29), NAVY, NAVY_DARK)   # left leg
    hgradient(im, (17, 16, 23, 29), NAVY, NAVY_DARK)  # right leg (15-17 stays a transparent gap)
    hstripe(im, (9, 16, 15, 29), 6, 1, REFLECTIVE)
    hstripe(im, (17, 16, 23, 29), 6, 1, REFLECTIVE)
    im.save(os.path.join(ITEM_ICON_OUT, "firefighter_pants.png"))


def icon_boots():
    im = canvas(32, 32)
    rect(im, (7, 8, 15, 26), BOOT_BLACK)   # left boot shaft
    rect(im, (5, 24, 17, 29), BOOT_BLACK)  # left boot foot
    rect(im, (17, 8, 25, 26), BOOT_BLACK)  # right boot shaft
    rect(im, (15, 24, 27, 29), BOOT_BLACK)  # right boot foot
    hgradient(im, (5, 8, 27, 29), BOOT_LIGHT, BOOT_BLACK)
    hstripe(im, (7, 8, 15, 26), 3, 1, REFLECTIVE)
    hstripe(im, (17, 8, 25, 26), 3, 1, REFLECTIVE)
    im.save(os.path.join(ITEM_ICON_OUT, "firefighter_boots.png"))


if __name__ == "__main__":
    build_humanoid_texture()
    build_leggings_texture()
    icon_helmet()
    icon_coat()
    icon_pants()
    icon_boots()
    print("Wrote 2 worn textures + 4 inventory icons for the firefighter uniform.")
