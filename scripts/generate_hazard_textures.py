#!/usr/bin/env python3
"""Generates 16x16 pixel-art textures for the 20 hazard prop blocks.

Builds on the flat-shaded, bordered style already used by computer_case.png /
fire_alarm_bell.png, but adds a directional light gradient, inset highlight/
shadow edges, and small object-specific iconography (plug prongs, screws,
grille slats, warning glyphs) so each block reads as a recognizable object at
a glance instead of a flat colored box. Run from repo root:

    python3 scripts/generate_hazard_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import os
import random
from PIL import Image

random.seed(42)

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "berongsmp", "textures", "block")
os.makedirs(OUT, exist_ok=True)

W = H = 16


def new_canvas(bg):
    return Image.new("RGB", (W, H), bg)


def set_px(im, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), color)


def get_px(im, x, y):
    if 0 <= x < W and 0 <= y < H:
        return im.getpixel((x, y))
    return (0, 0, 0)


def rect(im, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            set_px(im, x, y, color)


def line(im, x0, y0, x1, y1, color):
    """Simple orthogonal/diagonal line via Bresenham-lite stepping."""
    dx = x1 - x0
    dy = y1 - y0
    steps = max(abs(dx), abs(dy), 1)
    for i in range(steps + 1):
        x = x0 + round(dx * i / steps)
        y = y0 + round(dy * i / steps)
        set_px(im, x, y, color)


def border(im, color, thickness=1):
    rect(im, 0, 0, W - 1, thickness - 1, color)
    rect(im, 0, H - thickness, W - 1, H - 1, color)
    rect(im, 0, 0, thickness - 1, H - 1, color)
    rect(im, W - thickness, 0, W - 1, H - 1, color)


def scale(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def add(color, delta):
    return tuple(max(0, min(255, c + d)) for c, d in zip(color, delta))


def gradient_shade(im, x0, y0, x1, y1, base, light=1.35, dark=0.72):
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


def hbands(im, colors, start=0, end=H):
    span = end - start
    n = len(colors)
    band = span / n
    for i, c in enumerate(colors):
        y0 = int(start + i * band)
        y1 = int(start + (i + 1) * band) - 1
        rect(im, 0, y0, W - 1, max(y1, y0), c)


def speckle(im, colors, density=0.12, x0=0, y0=0, x1=W - 1, y1=H - 1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if random.random() < density:
                set_px(im, x, y, random.choice(colors))


def vents(im, y_positions, color, x0=2, x1=13):
    for y in y_positions:
        rect(im, x0, y, x1, y, color)


def grille_dots(im, x0, y0, x1, y1, color, spacing=2):
    for y in range(y0, y1 + 1, spacing):
        for x in range(x0, x1 + 1, spacing):
            set_px(im, x, y, color)


def screws(im, positions, color=(25, 25, 25)):
    for (x, y) in positions:
        set_px(im, x, y, color)


def diag_hazard_stripe(im, colors=((230, 160, 20), (20, 20, 20)), y0=0, y1=3):
    for y in range(y0, y1 + 1):
        for x in range(W):
            c = colors[(x + y) % 2]
            set_px(im, x, y, c)


def plug_prongs(im, x0, y0, color=(190, 175, 60), gap=4):
    """Two vertical prong marks — instantly reads as 'electrical plug'."""
    rect(im, x0, y0, x0, y0 + 2, color)
    rect(im, x0 + gap, y0, x0 + gap, y0 + 2, color)


def save(im, name):
    im.save(os.path.join(OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# Shared hazard-state accent textures (reused across multiple blocks)
# ---------------------------------------------------------------------------

def tex_hazard_ember_glow():
    im = new_canvas((40, 8, 4))
    rect(im, 1, 1, 14, 14, (120, 30, 6))
    rect(im, 3, 3, 12, 12, (210, 80, 10))
    rect(im, 5, 5, 10, 10, (250, 150, 30))
    rect(im, 7, 7, 8, 8, (255, 220, 120))
    speckle(im, [(255, 240, 160), (255, 90, 20)], density=0.10, x0=2, y0=2, x1=13, y1=13)
    save(im, "hazard_ember_glow")


def tex_hazard_warning_led():
    im = new_canvas((20, 20, 20))
    border(im, (10, 10, 10))
    rect(im, 2, 5, 13, 10, (12, 12, 12))
    for x in (3, 7, 11):
        rect(im, x, 6, x + 2, 9, (200, 20, 20))
        rect(im, x + 1, 6, x + 1, 6, (255, 120, 120))
    save(im, "hazard_warning_led")


def tex_hazard_ok_led():
    im = new_canvas((20, 20, 20))
    border(im, (10, 10, 10))
    rect(im, 2, 5, 13, 10, (12, 12, 12))
    for x in (3, 7, 11):
        rect(im, x, 6, x + 2, 9, (30, 190, 60))
        rect(im, x + 1, 6, x + 1, 6, (170, 255, 170))
    save(im, "hazard_ok_led")


def tex_hazard_spark_arc(name="hazard_spark_arc", core=(210, 235, 255), glow=(80, 160, 255)):
    im = new_canvas((8, 8, 12))
    pts = [(2, 13), (5, 10), (4, 8), (8, 6), (7, 4), (11, 2), (10, 1)]
    for (x, y) in pts:
        set_px(im, x, y, core)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            set_px(im, x + dx, y + dy, glow)
    speckle(im, [glow], density=0.05)
    save(im, name)


def tex_hazard_smoke_stain():
    im = new_canvas((60, 60, 60))
    speckle(im, [(90, 90, 90), (40, 40, 40), (120, 120, 120)], density=0.35)
    border(im, (25, 25, 25))
    save(im, "hazard_smoke_stain")


def tex_hazard_grease_stain():
    im = new_canvas((70, 50, 20))
    speckle(im, [(100, 70, 25), (40, 25, 8), (130, 95, 35)], density=0.4)
    border(im, (30, 18, 6))
    save(im, "hazard_grease_stain")


def tex_hazard_scorch_char():
    im = new_canvas((25, 20, 18))
    speckle(im, [(10, 8, 8), (45, 30, 20), (5, 5, 5)], density=0.4)
    border(im, (5, 5, 5))
    save(im, "hazard_scorch_char")


def tex_hazard_crumpled_paper():
    im = new_canvas((225, 220, 200))
    speckle(im, [(200, 195, 175), (240, 236, 220)], density=0.25)
    for y in (3, 7, 11):
        rect(im, 1, y, 14, y, (190, 185, 165))
    save(im, "hazard_crumpled_paper")


def tex_hazard_bare_copper():
    im = new_canvas((30, 30, 30))
    for y in range(0, H, 2):
        rect(im, 0, y, W - 1, y, (60, 60, 60))
    rect(im, 3, 6, 12, 9, (200, 120, 55))
    speckle(im, [(230, 150, 80), (170, 95, 40)], density=0.3, x0=3, y0=6, x1=12, y1=9)
    save(im, "hazard_bare_copper")


def tex_hazard_water_stain():
    im = new_canvas((210, 225, 235))
    for x in (3, 4, 9, 10, 11):
        rect(im, x, 0, x, 15, (150, 185, 210))
    speckle(im, [(120, 160, 195)], density=0.1)
    save(im, "hazard_water_stain")


def tex_hazard_glass_screen_off():
    im = new_canvas((15, 15, 18))
    border(im, (5, 5, 8))
    rect(im, 2, 2, 13, 13, (25, 28, 34))
    rect(im, 2, 2, 5, 5, (45, 50, 58))
    save(im, "hazard_glass_screen_off")


def tex_hazard_glass_screen_glitch():
    im = new_canvas((10, 10, 14))
    border(im, (5, 5, 8))
    colors = [(255, 40, 40), (40, 255, 120), (60, 120, 255), (255, 255, 255), (10, 10, 10)]
    for y in range(2, 14):
        for x in range(2, 14):
            if random.random() < 0.5:
                set_px(im, x, y, random.choice(colors))
    save(im, "hazard_glass_screen_glitch")


def tex_hazard_spark_arc_green():
    tex_hazard_spark_arc(name="hazard_spark_arc_green", core=(210, 255, 220), glow=(50, 220, 90))


# ---------------------------------------------------------------------------
# 1. plastic_trash_bin
# ---------------------------------------------------------------------------

def tex_bin_plastic_gray():
    im = new_canvas((90, 100, 95))
    gradient_shade(im, 1, 1, 14, 14, (90, 100, 95), light=1.25, dark=0.75)
    for y in range(2, H - 1, 3):
        rect(im, 1, y, 14, y, scale((90, 100, 95), 0.65))  # corrugation ribs
    inset_edges(im, 1, 1, 14, 14, (135, 148, 140), (45, 52, 48))
    border(im, (40, 48, 45))
    speckle(im, [(105, 118, 112), (65, 75, 72)], density=0.06)
    save(im, "bin_plastic_gray")


def tex_bin_rim_dark():
    im = new_canvas((60, 68, 65))
    hbands(im, [(75, 84, 80), (48, 55, 52)])
    inset_edges(im, 0, 0, 15, 15, (95, 105, 100), (25, 30, 28))
    border(im, (30, 36, 34))
    save(im, "bin_rim_dark")


# ---------------------------------------------------------------------------
# 2. daisy_chain_extension
# ---------------------------------------------------------------------------

def tex_cord_strip_black():
    im = new_canvas((25, 25, 25))
    gradient_shade(im, 1, 1, 14, 14, (25, 25, 25), light=1.4, dark=0.7)
    border(im, (8, 8, 8))
    for cx in (2, 6, 10):
        rect(im, cx, 5, cx + 2, 10, (48, 48, 48))
        rect(im, cx, 5, cx + 2, 5, (20, 20, 20))
        plug_prongs(im, cx, 7, color=(200, 185, 70), gap=1)
    speckle(im, [(38, 38, 38)], density=0.06)
    save(im, "cord_strip_black")


def tex_cord_wire_black():
    im = new_canvas((20, 20, 20))
    gradient_shade(im, 0, 0, 15, 15, (20, 20, 20), light=1.3, dark=0.8)
    for y in range(1, H - 1, 3):
        rect(im, 0, y, W - 1, y, (38, 38, 38))
        rect(im, 0, y + 1, W - 1, y + 1, (12, 12, 12))
    border(im, (6, 6, 6))
    save(im, "cord_wire_black")


# ---------------------------------------------------------------------------
# 3. woodshop_sawdust_layer
# ---------------------------------------------------------------------------

def tex_floor_tile_clean():
    im = new_canvas((150, 150, 150))
    gradient_shade(im, 1, 1, 14, 14, (150, 150, 150), light=1.15, dark=0.85)
    rect(im, 0, 0, W - 1, 0, (175, 175, 175))
    rect(im, 0, H - 1, W - 1, H - 1, (110, 110, 110))
    rect(im, 0, 0, 0, H - 1, (175, 175, 175))
    rect(im, W - 1, 0, W - 1, H - 1, (110, 110, 110))
    for i in (5, 10):
        line(im, i, 0, i, 15, (128, 128, 128))
    speckle(im, [(160, 160, 160), (135, 135, 135)], density=0.08)
    save(im, "floor_tile_clean")


def tex_sawdust_pile():
    im = new_canvas((205, 178, 120))
    gradient_shade(im, 0, 0, 15, 15, (205, 178, 120), light=1.15, dark=0.85)
    speckle(im, [(225, 200, 145), (180, 150, 95), (235, 215, 170)], density=0.5)
    speckle(im, [(140, 105, 60)], density=0.08)
    for _ in range(6):
        x, y = random.randint(1, 14), random.randint(1, 14)
        set_px(im, x, y, (150, 115, 65))
    save(im, "sawdust_pile")


# ---------------------------------------------------------------------------
# 4. stage_spotlight
# ---------------------------------------------------------------------------

def tex_spotlight_housing_black():
    im = new_canvas((30, 30, 32))
    gradient_shade(im, 1, 1, 14, 14, (30, 30, 32), light=1.5, dark=0.65)
    border(im, (10, 10, 12))
    inset_edges(im, 1, 1, 14, 14, (55, 55, 58), (12, 12, 14))
    for y in (2, 8, 13):
        set_px(im, 1, y, (65, 65, 68))
        set_px(im, 14, y, (65, 65, 68))
    screws(im, [(2, 2), (13, 2), (2, 13), (13, 13)], (12, 12, 13))
    rect(im, 6, 14, 9, 15, (18, 18, 20))  # mounting yoke
    save(im, "spotlight_housing_black")


def tex_spotlight_lens_off():
    im = new_canvas((70, 70, 65))
    gradient_shade(im, 2, 2, 13, 13, (85, 85, 78), light=1.2, dark=0.7)
    rect(im, 4, 4, 11, 11, (55, 55, 50))
    rect(im, 6, 6, 9, 9, (35, 35, 32))
    border(im, (30, 30, 26))
    rect(im, 5, 5, 6, 5, (110, 110, 100))  # cold glass glint
    save(im, "spotlight_lens_off")


# ---------------------------------------------------------------------------
# 5. archive_box_stack
# ---------------------------------------------------------------------------

def tex_cardboard_box():
    im = new_canvas((178, 140, 90))
    gradient_shade(im, 0, 0, 15, 15, (178, 140, 90), light=1.15, dark=0.85)
    speckle(im, [(190, 152, 100), (162, 126, 78)], density=0.12)
    rect(im, 0, 6, W - 1, 7, (140, 105, 62))  # tape line
    rect(im, 6, 0, 9, H - 1, (140, 105, 62))  # vertical tape
    rect(im, 2, 10, 12, 13, (235, 230, 215))  # label
    for y in (11, 12):
        rect(im, 3, y, 10, y, (120, 118, 108))
    # box-flap fold lines to sell the cardboard silhouette
    line(im, 0, 3, 6, 0, (150, 115, 68))
    line(im, 15, 3, 9, 0, (150, 115, 68))
    border(im, (100, 74, 42))
    save(im, "cardboard_box")


def tex_cardboard_box_charred():
    im = new_canvas((70, 45, 25))
    gradient_shade(im, 0, 0, 15, 15, (70, 45, 25), light=1.2, dark=0.7)
    speckle(im, [(35, 22, 12), (90, 60, 30), (15, 10, 6)], density=0.4)
    rect(im, 0, 6, W - 1, 7, (20, 12, 6))
    speckle(im, [(255, 120, 30), (200, 70, 10)], density=0.03)  # smoldering embers
    border(im, (8, 5, 3))
    save(im, "cardboard_box_charred")


# ---------------------------------------------------------------------------
# 6. dust_choked_pc
# ---------------------------------------------------------------------------

def tex_pc_tower_case():
    im = new_canvas((60, 62, 66))
    gradient_shade(im, 1, 1, 14, 14, (60, 62, 66), light=1.4, dark=0.7)
    vents(im, [3, 4, 5, 9, 10, 11], (30, 32, 35))
    inset_edges(im, 1, 1, 14, 14, (95, 98, 102), (22, 24, 27))
    border(im, (18, 19, 21))
    rect(im, 11, 1, 13, 2, (28, 30, 32))  # optical drive bay
    set_px(im, 3, 13, (40, 170, 60))  # power LED
    save(im, "pc_tower_case")


def tex_pc_tower_vent_dusty():
    im = new_canvas((60, 62, 66))
    gradient_shade(im, 1, 1, 14, 14, (60, 62, 66), light=1.4, dark=0.7)
    vents(im, [3, 4, 5, 9, 10, 11], (35, 37, 40))
    speckle(im, [(190, 175, 140), (165, 150, 115), (210, 195, 160)], density=0.5, y0=2, y1=13)
    inset_edges(im, 1, 1, 14, 14, (95, 98, 102), (22, 24, 27))
    border(im, (18, 19, 21))
    set_px(im, 3, 13, (200, 60, 40))  # LED reads hot/red now
    save(im, "pc_tower_vent_dusty")


def tex_backpack_fabric():
    im = new_canvas((35, 55, 80))
    gradient_shade(im, 0, 0, 15, 15, (35, 55, 80), light=1.25, dark=0.75)
    rect(im, 4, 4, 11, 9, (22, 35, 55))  # front pocket
    rect(im, 7, 2, 8, 13, (18, 28, 45))  # zipper line
    for y in range(2, 14, 2):
        set_px(im, 7, y, (140, 140, 145))  # zipper teeth glint
        set_px(im, 8, y, (90, 90, 95))
    speckle(im, [(50, 75, 105)], density=0.1)
    border(im, (12, 18, 30))
    save(im, "backpack_fabric")


# ---------------------------------------------------------------------------
# 7. charging_cart
# ---------------------------------------------------------------------------

def tex_cart_cabinet_metal():
    im = new_canvas((150, 152, 155))
    gradient_shade(im, 1, 1, 14, 14, (150, 152, 155), light=1.25, dark=0.78)
    grille_dots(im, 2, 2, 13, 13, (105, 107, 110), spacing=3)
    inset_edges(im, 1, 1, 14, 14, (190, 192, 195), (95, 97, 100))
    border(im, (85, 87, 90))
    rect(im, 6, 14, 9, 15, (60, 60, 63))  # caster wheel hint
    save(im, "cart_cabinet_metal")


# ---------------------------------------------------------------------------
# 8. frayed_console_wire — reuses cord_wire_black + hazard_bare_copper + spark
# ---------------------------------------------------------------------------

def tex_wire_cord_black():
    im = new_canvas((22, 22, 22))
    gradient_shade(im, 0, 0, 15, 15, (22, 22, 22), light=1.3, dark=0.75)
    for y in range(2, 14, 3):
        rect(im, 1, y, 14, y + 1, (36, 36, 36))
        rect(im, 1, y, 14, y, (14, 14, 14))
    border(im, (8, 8, 8))
    save(im, "wire_cord_black")


# ---------------------------------------------------------------------------
# 9. malfunctioning_vending
# ---------------------------------------------------------------------------

def tex_vending_body_blue():
    im = new_canvas((45, 85, 150))
    gradient_shade(im, 1, 1, 14, 9, (45, 85, 150), light=1.3, dark=0.75)
    inset_edges(im, 1, 1, 14, 9, (85, 130, 200), (20, 45, 90))
    border(im, (18, 40, 82))
    rect(im, 2, 12, 13, 14, (215, 215, 215))
    grille_dots(im, 3, 12, 12, 14, (150, 150, 150), spacing=2)
    for x in (3, 6, 9, 12):
        rect(im, x, 2, x, 8, (25, 55, 105))  # product-column dividers
    save(im, "vending_body_blue")


# ---------------------------------------------------------------------------
# 10. ceiling_projector
# ---------------------------------------------------------------------------

def tex_projector_housing_white():
    im = new_canvas((225, 222, 210))
    gradient_shade(im, 1, 1, 14, 14, (225, 222, 210), light=1.1, dark=0.82)
    vents(im, [3, 4, 11, 12], (170, 167, 155))
    inset_edges(im, 1, 1, 14, 14, (245, 242, 232), (150, 148, 138))
    border(im, (140, 138, 128))
    screws(im, [(2, 2), (13, 13)], (120, 118, 110))
    save(im, "projector_housing_white")


def tex_projector_lens_ring():
    im = new_canvas((40, 40, 40))
    gradient_shade(im, 1, 1, 14, 14, (40, 40, 40), light=1.3, dark=0.7)
    rect(im, 2, 2, 13, 13, (25, 25, 28))
    rect(im, 5, 5, 10, 10, (60, 90, 120))
    rect(im, 6, 6, 9, 9, (140, 190, 220))
    rect(im, 6, 6, 7, 6, (220, 235, 245))  # lens glint
    border(im, (12, 12, 14))
    save(im, "projector_lens_ring")


# ---------------------------------------------------------------------------
# 11. swollen_phone_battery
# ---------------------------------------------------------------------------

def tex_phone_body_black():
    im = new_canvas((25, 25, 28))
    gradient_shade(im, 1, 1, 14, 14, (25, 25, 28), light=1.5, dark=0.65)
    border(im, (8, 8, 10))
    rect(im, 10, 2, 12, 4, (15, 15, 17))  # camera bump
    rect(im, 10, 2, 10, 2, (60, 90, 130))  # lens glint
    rect(im, 3, 13, 12, 13, (14, 14, 16))  # bottom port shadow
    save(im, "phone_body_black")


def tex_phone_screen_glass():
    im = new_canvas((20, 24, 32))
    gradient_shade(im, 1, 1, 14, 14, (30, 36, 48), light=1.4, dark=0.75)
    rect(im, 2, 2, 6, 5, (65, 78, 100))  # screen glare streak
    rect(im, 7, 12, 13, 13, (18, 22, 30))  # nav bar
    border(im, (8, 10, 14))
    save(im, "phone_screen_glass")


def tex_phone_body_swollen():
    im = new_canvas((45, 42, 30))
    gradient_shade(im, 1, 1, 14, 14, (55, 50, 34), light=1.25, dark=0.75)
    speckle(im, [(65, 58, 40), (32, 28, 18)], density=0.3)
    inset_edges(im, 1, 1, 14, 14, (75, 68, 46), (18, 16, 10))
    border(im, (16, 14, 8))
    rect(im, 4, 6, 11, 9, (85, 65, 30))  # bulging seam split
    speckle(im, [(140, 90, 20)], density=0.06, x0=4, y0=6, x1=11, y1=9)
    save(im, "phone_body_swollen")


def tex_leather_jacket_fabric():
    im = new_canvas((90, 55, 35))
    gradient_shade(im, 0, 0, 15, 15, (90, 55, 35), light=1.25, dark=0.75)
    speckle(im, [(110, 70, 45), (60, 35, 20)], density=0.22)
    rect(im, 2, 3, 13, 4, (65, 38, 22))  # seam
    rect(im, 2, 10, 13, 11, (65, 38, 22))  # seam
    for x in (3, 6, 9, 12):
        set_px(im, x, 4, (30, 16, 8))  # stitching dots
        set_px(im, x, 11, (30, 16, 8))
    border(im, (40, 22, 12))
    save(im, "leather_jacket_fabric")


# ---------------------------------------------------------------------------
# 12. damaged_lipo_pack
# ---------------------------------------------------------------------------

def tex_lipo_foil_silver():
    im = new_canvas((195, 198, 202))
    gradient_shade(im, 1, 1, 14, 14, (195, 198, 202), light=1.25, dark=0.8)
    rect(im, 2, 6, 13, 6, (235, 60, 40))  # warning stripe
    rect(im, 4, 9, 11, 12, (150, 155, 160))  # printed spec label block
    speckle(im, [(220, 223, 227), (165, 168, 172)], density=0.12)
    inset_edges(im, 1, 1, 14, 14, (230, 233, 237), (120, 123, 127))
    border(im, (130, 133, 137))
    save(im, "lipo_foil_silver")


def tex_lipo_foil_damaged():
    im = new_canvas((120, 110, 95))
    gradient_shade(im, 0, 0, 15, 15, (120, 110, 95), light=1.2, dark=0.7)
    speckle(im, [(80, 40, 20), (150, 130, 100), (40, 20, 10)], density=0.4)
    rect(im, 2, 6, 13, 6, (150, 40, 25))
    # torn-foil puncture with visible dark cell beneath
    rect(im, 6, 8, 10, 11, (25, 22, 20))
    speckle(im, [(200, 90, 30), (255, 150, 40)], density=0.15, x0=6, y0=8, x1=10, y1=11)
    border(im, (40, 25, 12))
    save(im, "lipo_foil_damaged")


# ---------------------------------------------------------------------------
# 13. vape_in_iron_locker
# ---------------------------------------------------------------------------

def tex_locker_door_steel():
    im = new_canvas((95, 100, 105))
    gradient_shade(im, 1, 1, 14, 14, (95, 100, 105), light=1.3, dark=0.75)
    rect(im, 7, 0, 8, H - 1, (65, 70, 75))  # centre seam between double doors
    vents(im, [2, 3, 12, 13], (55, 60, 65))
    inset_edges(im, 1, 1, 14, 14, (135, 140, 145), (48, 52, 56))
    border(im, (45, 50, 55))
    rect(im, 11, 7, 12, 9, (170, 170, 30))  # lock/handle
    set_px(im, 11, 7, (225, 225, 90))
    rect(im, 2, 2, 5, 2, (60, 65, 70))  # vent louvre accents
    save(im, "locker_door_steel")


# ---------------------------------------------------------------------------
# 14. pa_system_backup
# ---------------------------------------------------------------------------

def tex_pa_rack_metal():
    im = new_canvas((35, 35, 38))
    gradient_shade(im, 1, 1, 14, 14, (35, 35, 38), light=1.4, dark=0.68)
    grille_dots(im, 2, 2, 13, 13, (18, 18, 20), spacing=2)
    inset_edges(im, 1, 1, 14, 14, (58, 58, 62), (14, 14, 16))
    border(im, (12, 12, 14))
    rect(im, 2, 13, 4, 13, (35, 170, 60))  # power indicator strip
    rect(im, 6, 13, 8, 13, (220, 60, 40))  # fault indicator strip
    save(im, "pa_rack_metal")


# ---------------------------------------------------------------------------
# 15. smartboard_inverter
# ---------------------------------------------------------------------------

def tex_smartboard_bezel_black():
    im = new_canvas((20, 20, 22))
    gradient_shade(im, 2, 2, 13, 13, (20, 20, 22), light=1.4, dark=0.7)
    border(im, (8, 8, 10), thickness=2)
    rect(im, 2, 2, 13, 13, (12, 14, 30))
    rect(im, 3, 3, 8, 6, (30, 45, 90))
    rect(im, 3, 3, 5, 3, (55, 75, 130))  # screen glare corner
    save(im, "smartboard_bezel_black")


# ---------------------------------------------------------------------------
# 16. unattended_grease_pan
# ---------------------------------------------------------------------------

def tex_stove_burner_metal():
    im = new_canvas((150, 150, 152))
    gradient_shade(im, 1, 1, 14, 14, (150, 150, 152), light=1.2, dark=0.8)
    rect(im, 4, 4, 11, 11, (60, 60, 62))
    rect(im, 6, 6, 9, 9, (30, 30, 32))
    for i in range(0, 12, 3):
        set_px(im, 4 + i % 8, 4, (85, 85, 88))  # coil highlight ticks
    inset_edges(im, 1, 1, 14, 14, (185, 185, 187), (95, 95, 97))
    border(im, (85, 85, 87))
    save(im, "stove_burner_metal")


def tex_pan_metal():
    im = new_canvas((110, 110, 115))
    gradient_shade(im, 1, 1, 14, 14, (120, 120, 125), light=1.3, dark=0.72)
    rect(im, 3, 3, 12, 12, (85, 85, 90))
    rect(im, 4, 4, 5, 4, (170, 170, 178))  # rim highlight
    rect(im, 0, 6, 1, 9, (95, 95, 100))  # handle stub
    border(im, (50, 50, 55))
    save(im, "pan_metal")


def tex_pan_oil_hazard():
    im = new_canvas((200, 150, 30))
    gradient_shade(im, 0, 0, 15, 15, (200, 150, 30), light=1.2, dark=0.78)
    speckle(im, [(230, 180, 50), (170, 120, 15), (255, 210, 90)], density=0.3)
    rect(im, 3, 3, 12, 12, (215, 160, 35))
    rect(im, 5, 5, 7, 6, (255, 225, 130))  # oil surface glare
    speckle(im, [(90, 55, 5)], density=0.06)  # scorched bits
    border(im, (100, 65, 8))
    save(im, "pan_oil_hazard")


# ---------------------------------------------------------------------------
# 17. grease_clogged_hood
# ---------------------------------------------------------------------------

def tex_hood_steel_clean():
    im = new_canvas((175, 178, 180))
    gradient_shade(im, 1, 1, 14, 14, (175, 178, 180), light=1.18, dark=0.82)
    grille_dots(im, 2, 10, 13, 13, (125, 128, 130), spacing=2)
    inset_edges(im, 1, 1, 14, 14, (205, 208, 210), (110, 113, 115))
    border(im, (100, 103, 105))
    save(im, "hood_steel_clean")


def tex_hood_grease_dirty():
    im = new_canvas((70, 52, 22))
    gradient_shade(im, 0, 0, 15, 15, (70, 52, 22), light=1.15, dark=0.75)
    speckle(im, [(95, 70, 30), (45, 32, 12), (115, 88, 40)], density=0.42)
    for y in (11, 12, 13):
        speckle(im, [(30, 20, 8)], density=0.4, y0=y, y1=y)  # drip streaks pooling low
    border(im, (25, 17, 6))
    save(im, "hood_grease_dirty")


# ---------------------------------------------------------------------------
# 18. contaminated_kitchen_bin
# ---------------------------------------------------------------------------

def tex_bin_green_clean():
    im = new_canvas((45, 110, 60))
    gradient_shade(im, 1, 1, 14, 14, (45, 110, 60), light=1.25, dark=0.78)
    inset_edges(im, 1, 1, 14, 14, (75, 150, 90), (20, 65, 32))
    border(im, (20, 65, 32))
    rect(im, 1, 1, 14, 2, (55, 130, 72))  # lid rim
    save(im, "bin_green_clean")


def tex_bin_grease_stained():
    im = new_canvas((60, 90, 50))
    gradient_shade(im, 0, 0, 15, 15, (60, 90, 50), light=1.15, dark=0.78)
    speckle(im, [(120, 95, 30), (80, 60, 15), (150, 120, 40)], density=0.42)
    rect(im, 1, 1, 14, 2, (45, 65, 38))  # lid rim, grimed over
    speckle(im, [(40, 28, 8)], density=0.1)
    border(im, (28, 40, 22))
    save(im, "bin_grease_stained")


# ---------------------------------------------------------------------------
# 19. jammed_panini_press
# ---------------------------------------------------------------------------

def tex_press_body_metal():
    im = new_canvas((140, 140, 145))
    gradient_shade(im, 1, 1, 14, 14, (140, 140, 145), light=1.25, dark=0.78)
    inset_edges(im, 1, 1, 14, 14, (175, 175, 180), (95, 95, 100))
    border(im, (75, 75, 80))
    rect(im, 6, 13, 9, 14, (50, 50, 54))  # control dial
    set_px(im, 7, 13, (200, 60, 40))  # dial marker
    save(im, "press_body_metal")


def tex_press_grill_ridged():
    im = new_canvas((60, 55, 50))
    gradient_shade(im, 1, 1, 14, 14, (60, 55, 50), light=1.2, dark=0.75)
    for y in range(1, H - 1, 2):
        rect(im, 1, y, 14, y, (90, 82, 72))
        rect(im, 1, y + 1, 14, y + 1, (40, 36, 32))
    border(im, (30, 27, 24))
    save(im, "press_grill_ridged")


def tex_press_grill_charred():
    im = new_canvas((30, 22, 15))
    gradient_shade(im, 1, 1, 14, 14, (30, 22, 15), light=1.2, dark=0.7)
    for y in range(1, H - 1, 2):
        rect(im, 1, y, 14, y, (55, 42, 28))
        rect(im, 1, y + 1, 14, y + 1, (15, 10, 6))
    speckle(im, [(15, 10, 5), (255, 110, 30)], density=0.15)  # char + smolder
    border(im, (8, 6, 3))
    save(im, "press_grill_charred")


# ---------------------------------------------------------------------------
# 20. commercial_deep_fryer
# ---------------------------------------------------------------------------

def tex_fryer_body_steel():
    im = new_canvas((165, 168, 170))
    gradient_shade(im, 1, 1, 14, 14, (165, 168, 170), light=1.25, dark=0.75)
    inset_edges(im, 1, 1, 14, 14, (200, 203, 205), (105, 108, 110))
    border(im, (95, 98, 100))
    rect(im, 3, 13, 6, 14, (40, 40, 42))  # control panel
    set_px(im, 4, 13, (220, 60, 40))  # power light
    save(im, "fryer_body_steel")


def tex_fryer_oil_surface():
    im = new_canvas((190, 145, 30))
    gradient_shade(im, 0, 0, 15, 15, (190, 145, 30), light=1.2, dark=0.78)
    speckle(im, [(215, 170, 50), (160, 115, 15)], density=0.22)
    rect(im, 3, 3, 6, 4, (235, 200, 110))  # surface sheen
    border(im, (100, 72, 8))
    save(im, "fryer_oil_surface")


def tex_fryer_oil_hazard():
    im = new_canvas((120, 60, 10))
    gradient_shade(im, 0, 0, 15, 15, (120, 60, 10), light=1.25, dark=0.7)
    speckle(im, [(200, 90, 10), (60, 25, 5), (255, 160, 40)], density=0.38)
    speckle(im, [(255, 220, 130)], density=0.05)  # boiling highlights
    border(im, (50, 20, 4))
    save(im, "fryer_oil_hazard")


ALL = [
    tex_hazard_ember_glow, tex_hazard_warning_led, tex_hazard_ok_led,
    tex_hazard_spark_arc, tex_hazard_spark_arc_green, tex_hazard_smoke_stain,
    tex_hazard_grease_stain, tex_hazard_scorch_char, tex_hazard_crumpled_paper,
    tex_hazard_bare_copper, tex_hazard_water_stain, tex_hazard_glass_screen_off,
    tex_hazard_glass_screen_glitch,
    tex_bin_plastic_gray, tex_bin_rim_dark,
    tex_cord_strip_black, tex_cord_wire_black,
    tex_floor_tile_clean, tex_sawdust_pile,
    tex_spotlight_housing_black, tex_spotlight_lens_off,
    tex_cardboard_box, tex_cardboard_box_charred,
    tex_pc_tower_case, tex_pc_tower_vent_dusty, tex_backpack_fabric,
    tex_cart_cabinet_metal,
    tex_wire_cord_black,
    tex_vending_body_blue,
    tex_projector_housing_white, tex_projector_lens_ring,
    tex_phone_body_black, tex_phone_screen_glass, tex_phone_body_swollen,
    tex_leather_jacket_fabric,
    tex_lipo_foil_silver, tex_lipo_foil_damaged,
    tex_locker_door_steel,
    tex_pa_rack_metal,
    tex_smartboard_bezel_black,
    tex_stove_burner_metal, tex_pan_metal, tex_pan_oil_hazard,
    tex_hood_steel_clean, tex_hood_grease_dirty,
    tex_bin_green_clean, tex_bin_grease_stained,
    tex_press_body_metal, tex_press_grill_ridged, tex_press_grill_charred,
    tex_fryer_body_steel, tex_fryer_oil_surface, tex_fryer_oil_hazard,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Generated {len(ALL)} textures into {os.path.abspath(OUT)}")
