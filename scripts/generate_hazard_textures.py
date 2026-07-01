#!/usr/bin/env python3
"""Generates 16x16 pixel-art textures for the 20 hazard prop blocks.

Matches the flat-shaded, bordered style already used by computer_case.png /
fire_alarm_bell.png (solid color bands, 1px darker border, small bright
accent highlights). Run from repo root:

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


def rect(im, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            set_px(im, x, y, color)


def border(im, color, thickness=1):
    rect(im, 0, 0, W - 1, thickness - 1, color)
    rect(im, 0, H - thickness, W - 1, H - 1, color)
    rect(im, 0, 0, thickness - 1, H - 1, color)
    rect(im, W - thickness, 0, W - 1, H - 1, color)


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


def diag_hazard_stripe(im, colors=((230, 160, 20), (20, 20, 20)), y0=0, y1=3):
    for y in range(y0, y1 + 1):
        for x in range(W):
            c = colors[(x + y) % 2]
            set_px(im, x, y, c)


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
    hbands(im, [(95, 108, 102), (80, 92, 87), (95, 108, 102), (80, 92, 87)])
    for y in range(0, H, 4):
        rect(im, 0, y, W - 1, y, (70, 80, 76))
    border(im, (55, 63, 60))
    speckle(im, [(105, 118, 112), (65, 75, 72)], density=0.08)
    save(im, "bin_plastic_gray")


def tex_bin_rim_dark():
    im = new_canvas((60, 68, 65))
    hbands(im, [(70, 78, 75), (50, 58, 55)])
    border(im, (35, 42, 40))
    save(im, "bin_rim_dark")


# ---------------------------------------------------------------------------
# 2. daisy_chain_extension
# ---------------------------------------------------------------------------

def tex_cord_strip_black():
    im = new_canvas((25, 25, 25))
    border(im, (10, 10, 10))
    for cx in (3, 7, 11):
        rect(im, cx, 6, cx + 1, 9, (45, 45, 45))
        rect(im, cx, 6, cx, 6, (15, 15, 15))
    speckle(im, [(35, 35, 35)], density=0.08)
    save(im, "cord_strip_black")


def tex_cord_wire_black():
    im = new_canvas((20, 20, 20))
    for y in range(0, H, 3):
        rect(im, 0, y, W - 1, y, (35, 35, 35))
    border(im, (8, 8, 8))
    save(im, "cord_wire_black")


# ---------------------------------------------------------------------------
# 3. woodshop_sawdust_layer
# ---------------------------------------------------------------------------

def tex_floor_tile_clean():
    im = new_canvas((150, 150, 150))
    rect(im, 0, 0, W - 1, 0, (170, 170, 170))
    rect(im, 0, H - 1, W - 1, H - 1, (120, 120, 120))
    rect(im, 0, 0, 0, H - 1, (170, 170, 170))
    rect(im, W - 1, 0, W - 1, H - 1, (120, 120, 120))
    speckle(im, [(160, 160, 160), (135, 135, 135)], density=0.1)
    save(im, "floor_tile_clean")


def tex_sawdust_pile():
    im = new_canvas((205, 178, 120))
    speckle(im, [(225, 200, 145), (180, 150, 95), (235, 215, 170)], density=0.5)
    speckle(im, [(140, 105, 60)], density=0.06)
    save(im, "sawdust_pile")


# ---------------------------------------------------------------------------
# 4. stage_spotlight
# ---------------------------------------------------------------------------

def tex_spotlight_housing_black():
    im = new_canvas((30, 30, 32))
    hbands(im, [(38, 38, 40), (22, 22, 24), (38, 38, 40)])
    border(im, (12, 12, 14))
    for y in (2, 8, 13):
        set_px(im, 1, y, (60, 60, 62))
        set_px(im, 14, y, (60, 60, 62))
    save(im, "spotlight_housing_black")


def tex_spotlight_lens_off():
    im = new_canvas((70, 70, 65))
    rect(im, 2, 2, 13, 13, (110, 108, 95))
    rect(im, 4, 4, 11, 11, (150, 148, 130))
    border(im, (40, 40, 35))
    save(im, "spotlight_lens_off")


# ---------------------------------------------------------------------------
# 5. archive_box_stack
# ---------------------------------------------------------------------------

def tex_cardboard_box():
    im = new_canvas((178, 140, 90))
    speckle(im, [(190, 152, 100), (162, 126, 78)], density=0.15)
    rect(im, 0, 6, W - 1, 7, (140, 105, 62))  # tape line
    rect(im, 6, 0, 9, H - 1, (140, 105, 62))  # vertical tape
    rect(im, 2, 10, 12, 13, (235, 230, 215))  # label
    for y in (11, 12):
        rect(im, 3, y, 10, y, (120, 118, 108))
    border(im, (110, 82, 48))
    save(im, "cardboard_box")


def tex_cardboard_box_charred():
    im = new_canvas((70, 45, 25))
    speckle(im, [(35, 22, 12), (90, 60, 30), (15, 10, 6)], density=0.4)
    rect(im, 0, 6, W - 1, 7, (20, 12, 6))
    border(im, (10, 6, 3))
    save(im, "cardboard_box_charred")


# ---------------------------------------------------------------------------
# 6. dust_choked_pc
# ---------------------------------------------------------------------------

def tex_pc_tower_case():
    im = new_canvas((60, 62, 66))
    hbands(im, [(70, 72, 76), (50, 52, 56)])
    vents(im, [3, 4, 5, 9, 10, 11], (35, 37, 40))
    border(im, (25, 26, 28))
    rect(im, 11, 1, 13, 2, (30, 32, 34))
    save(im, "pc_tower_case")


def tex_pc_tower_vent_dusty():
    im = new_canvas((60, 62, 66))
    hbands(im, [(70, 72, 76), (50, 52, 56)])
    vents(im, [3, 4, 5, 9, 10, 11], (35, 37, 40))
    speckle(im, [(190, 175, 140), (165, 150, 115)], density=0.45, y0=2, y1=13)
    border(im, (25, 26, 28))
    save(im, "pc_tower_vent_dusty")


# ---------------------------------------------------------------------------
# 7. charging_cart
# ---------------------------------------------------------------------------

def tex_cart_cabinet_metal():
    im = new_canvas((150, 152, 155))
    hbands(im, [(160, 162, 165), (140, 142, 145), (160, 162, 165), (140, 142, 145)])
    grille_dots(im, 2, 2, 13, 13, (110, 112, 115), spacing=3)
    border(im, (95, 97, 100))
    save(im, "cart_cabinet_metal")


# ---------------------------------------------------------------------------
# 8. frayed_console_wire — reuses cord_wire_black + hazard_bare_copper + spark
# ---------------------------------------------------------------------------

def tex_wire_cord_black():
    im = new_canvas((22, 22, 22))
    for y in range(2, 14, 3):
        rect(im, 1, y, 14, y + 1, (34, 34, 34))
    border(im, (12, 12, 12))
    save(im, "wire_cord_black")


# ---------------------------------------------------------------------------
# 9. malfunctioning_vending
# ---------------------------------------------------------------------------

def tex_vending_body_blue():
    im = new_canvas((45, 85, 150))
    hbands(im, [(55, 95, 160), (35, 70, 130)])
    border(im, (20, 45, 90))
    rect(im, 2, 12, 13, 14, (215, 215, 215))
    grille_dots(im, 3, 12, 12, 14, (150, 150, 150), spacing=2)
    save(im, "vending_body_blue")


# ---------------------------------------------------------------------------
# 10. ceiling_projector
# ---------------------------------------------------------------------------

def tex_projector_housing_white():
    im = new_canvas((225, 222, 210))
    hbands(im, [(235, 232, 220), (205, 202, 190)])
    vents(im, [3, 4, 11, 12], (175, 172, 160))
    border(im, (150, 148, 138))
    save(im, "projector_housing_white")


def tex_projector_lens_ring():
    im = new_canvas((40, 40, 40))
    rect(im, 2, 2, 13, 13, (25, 25, 28))
    rect(im, 5, 5, 10, 10, (60, 90, 120))
    rect(im, 6, 6, 9, 9, (140, 190, 220))
    border(im, (15, 15, 18))
    save(im, "projector_lens_ring")


# ---------------------------------------------------------------------------
# 11. swollen_phone_battery
# ---------------------------------------------------------------------------

def tex_phone_body_black():
    im = new_canvas((25, 25, 28))
    border(im, (10, 10, 12))
    rect(im, 10, 2, 12, 4, (15, 15, 17))
    save(im, "phone_body_black")


def tex_phone_screen_glass():
    im = new_canvas((20, 24, 32))
    rect(im, 1, 1, 14, 14, (30, 36, 48))
    rect(im, 2, 2, 6, 5, (55, 65, 85))
    border(im, (10, 12, 16))
    save(im, "phone_screen_glass")


def tex_phone_body_swollen():
    im = new_canvas((45, 42, 30))
    speckle(im, [(60, 55, 38), (30, 28, 18)], density=0.3)
    border(im, (20, 18, 10))
    save(im, "phone_body_swollen")


# ---------------------------------------------------------------------------
# 12. damaged_lipo_pack
# ---------------------------------------------------------------------------

def tex_lipo_foil_silver():
    im = new_canvas((195, 198, 202))
    hbands(im, [(210, 213, 217), (180, 183, 187)])
    rect(im, 2, 6, 13, 6, (235, 60, 40))  # warning stripe
    speckle(im, [(220, 223, 227), (165, 168, 172)], density=0.15)
    border(im, (140, 143, 147))
    save(im, "lipo_foil_silver")


def tex_lipo_foil_damaged():
    im = new_canvas((120, 110, 95))
    speckle(im, [(80, 40, 20), (150, 130, 100), (40, 20, 10)], density=0.4)
    rect(im, 2, 6, 13, 6, (150, 40, 25))
    border(im, (50, 30, 15))
    save(im, "lipo_foil_damaged")


# ---------------------------------------------------------------------------
# 13. vape_in_iron_locker
# ---------------------------------------------------------------------------

def tex_locker_door_steel():
    im = new_canvas((95, 100, 105))
    hbands(im, [(105, 110, 115), (85, 90, 95)])
    rect(im, 7, 0, 8, H - 1, (75, 80, 85))
    vents(im, [2, 3, 12, 13], (60, 65, 70))
    border(im, (55, 60, 65))
    rect(im, 11, 7, 12, 9, (170, 170, 30))  # lock/handle
    save(im, "locker_door_steel")


# ---------------------------------------------------------------------------
# 14. pa_system_backup
# ---------------------------------------------------------------------------

def tex_pa_rack_metal():
    im = new_canvas((35, 35, 38))
    hbands(im, [(45, 45, 48), (28, 28, 30), (45, 45, 48), (28, 28, 30)])
    grille_dots(im, 2, 2, 13, 13, (18, 18, 20), spacing=2)
    border(im, (15, 15, 17))
    save(im, "pa_rack_metal")


# ---------------------------------------------------------------------------
# 15. smartboard_inverter
# ---------------------------------------------------------------------------

def tex_smartboard_bezel_black():
    im = new_canvas((20, 20, 22))
    border(im, (8, 8, 10), thickness=2)
    rect(im, 2, 2, 13, 13, (12, 14, 30))
    rect(im, 3, 3, 8, 6, (30, 45, 90))
    save(im, "smartboard_bezel_black")


# ---------------------------------------------------------------------------
# 16. unattended_grease_pan
# ---------------------------------------------------------------------------

def tex_stove_burner_metal():
    im = new_canvas((150, 150, 152))
    hbands(im, [(160, 160, 162), (135, 135, 137)])
    rect(im, 4, 4, 11, 11, (60, 60, 62))
    rect(im, 6, 6, 9, 9, (30, 30, 32))
    border(im, (100, 100, 102))
    save(im, "stove_burner_metal")


def tex_pan_metal():
    im = new_canvas((110, 110, 115))
    rect(im, 1, 1, 14, 14, (130, 130, 135))
    rect(im, 3, 3, 12, 12, (85, 85, 90))
    border(im, (60, 60, 65))
    save(im, "pan_metal")


def tex_pan_oil_hazard():
    im = new_canvas((200, 150, 30))
    speckle(im, [(230, 180, 50), (170, 120, 15), (255, 210, 90)], density=0.35)
    rect(im, 3, 3, 12, 12, (215, 160, 35))
    border(im, (120, 80, 10))
    save(im, "pan_oil_hazard")


# ---------------------------------------------------------------------------
# 17. grease_clogged_hood
# ---------------------------------------------------------------------------

def tex_hood_steel_clean():
    im = new_canvas((175, 178, 180))
    hbands(im, [(185, 188, 190), (160, 163, 165)])
    grille_dots(im, 2, 10, 13, 13, (130, 133, 135), spacing=2)
    border(im, (120, 123, 125))
    save(im, "hood_steel_clean")


def tex_hood_grease_dirty():
    im = new_canvas((70, 52, 22))
    speckle(im, [(95, 70, 30), (45, 32, 12), (115, 88, 40)], density=0.45)
    border(im, (30, 20, 8))
    save(im, "hood_grease_dirty")


# ---------------------------------------------------------------------------
# 18. contaminated_kitchen_bin
# ---------------------------------------------------------------------------

def tex_bin_green_clean():
    im = new_canvas((45, 110, 60))
    hbands(im, [(55, 122, 70), (35, 95, 50)])
    border(im, (22, 70, 35))
    save(im, "bin_green_clean")


def tex_bin_grease_stained():
    im = new_canvas((60, 90, 50))
    speckle(im, [(120, 95, 30), (80, 60, 15), (150, 120, 40)], density=0.45)
    border(im, (35, 55, 25))
    save(im, "bin_grease_stained")


# ---------------------------------------------------------------------------
# 19. jammed_panini_press
# ---------------------------------------------------------------------------

def tex_press_body_metal():
    im = new_canvas((140, 140, 145))
    hbands(im, [(150, 150, 155), (125, 125, 130)])
    border(im, (85, 85, 90))
    save(im, "press_body_metal")


def tex_press_grill_ridged():
    im = new_canvas((60, 55, 50))
    for y in range(1, H - 1, 2):
        rect(im, 1, y, 14, y, (85, 78, 70))
    border(im, (35, 32, 28))
    save(im, "press_grill_ridged")


def tex_press_grill_charred():
    im = new_canvas((30, 22, 15))
    for y in range(1, H - 1, 2):
        rect(im, 1, y, 14, y, (50, 38, 25))
    speckle(im, [(15, 10, 5)], density=0.2)
    border(im, (10, 7, 4))
    save(im, "press_grill_charred")


# ---------------------------------------------------------------------------
# 20. commercial_deep_fryer
# ---------------------------------------------------------------------------

def tex_fryer_body_steel():
    im = new_canvas((165, 168, 170))
    hbands(im, [(175, 178, 180), (150, 153, 155), (175, 178, 180)])
    border(im, (110, 113, 115))
    save(im, "fryer_body_steel")


def tex_fryer_oil_surface():
    im = new_canvas((190, 145, 30))
    speckle(im, [(215, 170, 50), (160, 115, 15)], density=0.25)
    border(im, (110, 80, 10))
    save(im, "fryer_oil_surface")


def tex_fryer_oil_hazard():
    im = new_canvas((120, 60, 10))
    speckle(im, [(200, 90, 10), (60, 25, 5), (255, 160, 40)], density=0.4)
    border(im, (60, 25, 5))
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
    tex_pc_tower_case, tex_pc_tower_vent_dusty,
    tex_cart_cabinet_metal,
    tex_wire_cord_black,
    tex_vending_body_blue,
    tex_projector_housing_white, tex_projector_lens_ring,
    tex_phone_body_black, tex_phone_screen_glass, tex_phone_body_swollen,
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
