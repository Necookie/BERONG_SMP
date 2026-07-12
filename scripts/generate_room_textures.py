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
# Phase 2: Conference Room hazards (props 56-65)
# ---------------------------------------------------------------------------

def tex_heater_body():
    base = (58, 58, 62)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (86, 86, 92), (28, 28, 32))
    border(im, (22, 22, 25))
    save(im, "heater_body")


def tex_heater_grille_off():
    base = (32, 32, 35)
    im = new_canvas(base)
    for y in range(2, 14, 2):
        rect(im, 2, y, 13, y, scale(base, 1.3))
    border(im, (16, 16, 18))
    save(im, "heater_grille_off")


def tex_heater_grille_glow():
    base = (200, 90, 30)
    im = new_canvas((30, 22, 16))
    for y in range(2, 14, 2):
        rect(im, 2, y, 13, y, base)
        rect(im, 2, y + 1, 13, y + 1, scale(base, 0.5))
    border(im, (16, 12, 8))
    save(im, "heater_grille_glow")


def tex_lamp_pole():
    base = (42, 42, 46)
    im = new_canvas(base)
    vertical_brush(im, 6, 0, 9, 15, base, stripe=1.2)
    rect(im, 0, 0, 5, 15, scale(base, 0.7))
    rect(im, 10, 0, 15, 15, scale(base, 0.7))
    save(im, "lamp_pole")


def tex_lamp_bowl_off():
    base = (60, 60, 64)
    im = new_canvas(base)
    disc_fill(im, 7.5, 7.5, 7, base)
    disc_ring(im, 7.5, 7.5, 6.5, scale(base, 1.3), thickness=1.0)
    save(im, "lamp_bowl_off")


def tex_lamp_bowl_glow():
    im = new_canvas((60, 44, 20))
    disc_fill(im, 7.5, 7.5, 7, (255, 235, 190))
    disc_ring(im, 7.5, 7.5, 4, (255, 250, 230), thickness=1.5)
    save(im, "lamp_bowl_glow")


def tex_screen_housing():
    base = (222, 222, 218)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.08, dark=0.92)
    border(im, (168, 168, 162))
    save(im, "screen_housing")


def tex_screen_fabric_normal():
    base = (238, 238, 234)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.04, dark=0.96)
    border(im, (200, 200, 196))
    save(im, "screen_fabric_normal")


def tex_screen_fabric_smoking():
    base = (150, 148, 142)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.0, dark=0.8)
    speckle(im, [(90, 88, 82), (70, 68, 64)], density=0.18)
    border(im, (60, 58, 54))
    save(im, "screen_fabric_smoking")


def tex_videowall_frame():
    base = (18, 18, 20)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.3, dark=0.8)
    inset_edges(im, 0, 0, W - 1, H - 1, (40, 40, 44), (6, 6, 7))
    save(im, "videowall_frame")


def tex_dispenser_body():
    base = (228, 228, 224)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    inset_edges(im, 0, 0, W - 1, H - 1, (250, 250, 246), (170, 170, 164))
    save(im, "dispenser_body")


def tex_dispenser_nozzle_normal():
    base = (70, 70, 74)
    im = new_canvas((228, 228, 224))
    rect(im, 5, 6, 10, 9, base)
    save(im, "dispenser_nozzle_normal")


def tex_dispenser_nozzle_misting():
    im = new_canvas((228, 228, 224))
    rect(im, 5, 6, 10, 9, (70, 70, 74))
    for _ in range(14):
        x = random.randint(2, 13)
        y = random.randint(0, 13)
        set_px(im, x, y, (225, 235, 240))
    save(im, "dispenser_nozzle_misting")


def tex_laptop_lid():
    base = (150, 152, 156)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.2, band=1, offset=3)
    inset_edges(im, 0, 0, W - 1, H - 1, (182, 184, 188), (94, 96, 100))
    save(im, "laptop_lid")


def tex_laptop_vents_normal():
    base = (110, 112, 116)
    im = new_canvas(base)
    for x in range(2, 14, 2):
        rect(im, x, 2, x, 13, scale(base, 0.6))
    save(im, "laptop_vents_normal")


def tex_laptop_vents_hot():
    im = new_canvas((70, 40, 20))
    for x in range(2, 14, 2):
        rect(im, x, 2, x, 13, (220, 120, 40))
    save(im, "laptop_vents_hot")


def tex_planter_soil_normal():
    base = (58, 42, 30)
    im = new_canvas(base)
    speckle(im, [(70, 52, 38), (46, 32, 22)], density=0.2)
    border(im, (34, 24, 16))
    save(im, "planter_soil_normal")


def tex_planter_soil_smoldering():
    base = (54, 38, 26)
    im = new_canvas(base)
    speckle(im, [(70, 52, 38), (46, 32, 22)], density=0.15)
    for _ in range(6):
        x = random.randint(2, 13)
        y = random.randint(2, 13)
        set_px(im, x, y, (230, 110, 40))
    border(im, (34, 24, 16))
    save(im, "planter_soil_smoldering")


def tex_cord_normal():
    base = (24, 24, 26)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.3, dark=0.85)
    border(im, (10, 10, 11))
    save(im, "cord_normal")


def tex_ups_case():
    base = (36, 36, 40)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.12)
    inset_edges(im, 0, 0, W - 1, H - 1, (60, 60, 66), (14, 14, 16))
    save(im, "ups_case")


def tex_ups_vent_normal():
    base = (20, 20, 22)
    im = new_canvas((36, 36, 40))
    for y in range(2, 14, 2):
        rect(im, 3, y, 12, y, base)
    save(im, "ups_vent_normal")


def tex_ups_vent_venting():
    im = new_canvas((36, 36, 40))
    for y in range(2, 14, 2):
        rect(im, 3, y, 12, y, (20, 20, 22))
    for _ in range(10):
        x = random.randint(2, 13)
        y = random.randint(0, 13)
        set_px(im, x, y, (150, 220, 210))
    save(im, "ups_vent_venting")


def tex_dimmer_plate():
    base = (232, 230, 224)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.06, dark=0.94)
    inset_edges(im, 0, 0, W - 1, H - 1, (250, 248, 242), (176, 174, 168))
    save(im, "dimmer_plate")


def tex_dimmer_knob_normal():
    im = new_canvas((232, 230, 224))
    disc_fill(im, 7.5, 7.5, 4, (60, 60, 62))
    disc_ring(im, 7.5, 7.5, 3.5, (90, 90, 94), thickness=1.0)
    save(im, "dimmer_knob_normal")


ALL += [
    tex_heater_body, tex_heater_grille_off, tex_heater_grille_glow,
    tex_lamp_pole, tex_lamp_bowl_off, tex_lamp_bowl_glow,
    tex_screen_housing, tex_screen_fabric_normal, tex_screen_fabric_smoking,
    tex_videowall_frame,
    tex_dispenser_body, tex_dispenser_nozzle_normal, tex_dispenser_nozzle_misting,
    tex_laptop_lid, tex_laptop_vents_normal, tex_laptop_vents_hot,
    tex_planter_soil_normal, tex_planter_soil_smoldering,
    tex_cord_normal,
    tex_ups_case, tex_ups_vent_normal, tex_ups_vent_venting,
    tex_dimmer_plate, tex_dimmer_knob_normal,
]

# ---------------------------------------------------------------------------
# Phase 3: Office furniture
# ---------------------------------------------------------------------------

def tex_cubicle_fabric():
    base = (110, 118, 128)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    for y in range(2, 14, 4):
        for x in range(2, 14, 4):
            set_px(im, x, y, scale(base, 0.7))
    inset_edges(im, 0, 0, W - 1, H - 1, (140, 148, 158), (66, 72, 80))
    border(im, (58, 64, 72))
    save(im, "cubicle_fabric")


def tex_reception_front():
    base = (60, 66, 74)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.12, dark=0.88)
    rect(im, 0, 3, 15, 4, scale(base, 1.5))  # raised transaction ledge highlight
    border(im, (26, 30, 34))
    save(im, "reception_front")


def tex_mail_slots():
    base = (150, 106, 62)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.88)
    envelope_colors = [(230, 225, 210), (200, 210, 225), (225, 200, 195)]
    for y0 in (1, 6, 11):
        for x0 in (1, 6, 11):
            rect(im, x0, y0, x0 + 3, y0 + 3, scale(base, 0.6))
            if random.random() < 0.5:
                rect(im, x0 + 1, y0 + 1, x0 + 2, y0 + 1, random.choice(envelope_colors))
    border(im, (78, 54, 30))
    save(im, "mail_slots")


def tex_copier_body():
    base = (206, 206, 202)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.88)
    inset_edges(im, 0, 0, W - 1, H - 1, (230, 230, 226), (150, 150, 146))
    save(im, "copier_body")


def tex_copier_panel():
    base = (70, 72, 76)
    im = new_canvas((206, 206, 202))
    rect(im, 3, 3, 12, 8, base)
    indicator_dots(im, [(5, 5), (7, 5), (9, 5)], (70, 200, 90))
    save(im, "copier_panel")


def tex_doc_tray_stack():
    im = new_canvas((235, 232, 224))
    colors = [(70, 130, 90), (60, 100, 170), (200, 130, 50)]
    for i, y0 in enumerate((1, 6, 11)):
        rect(im, 1, y0, 14, y0 + 3, colors[i % len(colors)])
        rect(im, 1, y0, 14, y0, scale(colors[i % len(colors)], 1.3))
    save(im, "doc_tray_stack")


def tex_binder_spines():
    shelf_bg = (58, 42, 28)
    im = new_canvas(shelf_bg)
    palette = [(180, 60, 50), (60, 100, 170), (70, 140, 90), (200, 150, 40)]
    x = 1
    while x <= 14:
        wband = random.choice([2, 3, 2])
        x1 = min(x + wband, 14)
        col = random.choice(palette)
        rect(im, x, 1, x1, 14, col)
        rect(im, x, 1, x, 14, scale(col, 1.35))
        x = x1 + 1
    border(im, (24, 17, 11))
    save(im, "binder_spines")


def tex_safe_body():
    base = (58, 58, 62)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (86, 86, 92), (24, 24, 27))
    save(im, "safe_body")


def tex_safe_door():
    base = (50, 50, 54)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    disc_fill(im, 10, 7.5, 3, (30, 30, 33))
    disc_ring(im, 10, 7.5, 2.5, (150, 150, 40), thickness=1.0)
    rect(im, 2, 12, 12, 13, (30, 30, 33))  # handle bar
    border(im, (20, 20, 22))
    save(im, "safe_door")


def tex_coatrack_body():
    base = (96, 62, 40)
    im = new_canvas(base)
    vertical_brush(im, 6, 0, 9, 15, base, stripe=1.15)
    rect(im, 0, 0, 5, 15, scale(base, 0.75))
    rect(im, 10, 0, 15, 15, scale(base, 0.75))
    rect(im, 3, 1, 12, 4, (110, 40, 40))  # hung jacket
    save(im, "coatrack_body")


def tex_bundy_clock_face():
    base = (236, 234, 226)
    im = new_canvas(base)
    disc_fill(im, 7.5, 6, 5, (250, 248, 242))
    disc_ring(im, 7.5, 6, 5, (30, 30, 32), thickness=1.0)
    line_px(im, 7.5, 6, 7.5, 3, (20, 20, 22))
    line_px(im, 7.5, 6, 9.5, 6, (20, 20, 22))
    for y0 in (12, 14):
        rect(im, 2, y0, 13, y0, (200, 196, 186))
    save(im, "bundy_clock_face")


def line_px(im, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = round(x0), round(y0)
    x1r, y1r = round(x1), round(y1)
    while True:
        set_px(im, x, y, color)
        if x == x1r and y == y1r:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy


def tex_bundy_frame():
    base = (70, 70, 74)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (100, 100, 106), (30, 30, 33))
    save(im, "bundy_frame")


ALL += [
    tex_cubicle_fabric,
    tex_reception_front,
    tex_mail_slots,
    tex_copier_body, tex_copier_panel,
    tex_doc_tray_stack,
    tex_binder_spines,
    tex_safe_body, tex_safe_door,
    tex_coatrack_body,
    tex_bundy_clock_face, tex_bundy_frame,
]

# ---------------------------------------------------------------------------
# Phase 4: Office hazards (props 66-75)
# ---------------------------------------------------------------------------

def tex_shredder_body():
    base = (64, 66, 70)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (92, 94, 98), (30, 32, 35))
    save(im, "shredder_body")


def tex_shredder_slot_normal():
    im = new_canvas((64, 66, 70))
    rect(im, 2, 6, 13, 9, (20, 20, 22))
    save(im, "shredder_slot_normal")


def tex_shredder_slot_jammed():
    im = new_canvas((64, 66, 70))
    rect(im, 2, 6, 13, 9, (20, 20, 22))
    for _ in range(8):
        x = random.randint(3, 12)
        y = random.randint(6, 9)
        set_px(im, x, y, (225, 220, 205))
    save(im, "shredder_slot_jammed")


def tex_cabinet_frame():
    base = (26, 27, 30)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (48, 49, 53), (8, 8, 10))
    save(im, "cabinet_frame")


def tex_ebike_body():
    base = (40, 44, 48)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    rect(im, 2, 3, 13, 5, (200, 40, 40))  # frame accent stripe
    inset_edges(im, 0, 0, W - 1, H - 1, (64, 68, 72), (16, 18, 20))
    save(im, "ebike_body")


def tex_charge_port_normal():
    im = new_canvas((40, 44, 48))
    rect(im, 5, 5, 10, 10, (20, 20, 22))
    indicator_dots(im, [(6, 6), (9, 9)], (70, 200, 90))
    save(im, "charge_port_normal")


def tex_fixture_housing():
    base = (230, 230, 226)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.08, dark=0.92)
    border(im, (176, 176, 170))
    save(im, "fixture_housing")


def tex_tube_normal():
    im = new_canvas((248, 248, 240))
    disc_ring(im, 7.5, 7.5, 6, (220, 220, 200), thickness=1.0)
    save(im, "tube_normal")


def tex_tube_dripping():
    im = new_canvas((210, 200, 170))
    for _ in range(10):
        x = random.randint(2, 13)
        y = random.randint(6, 14)
        set_px(im, x, y, (120, 90, 40))
    save(im, "tube_dripping")


def tex_tank_glass():
    base = (150, 195, 210)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.85)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.3, band=1, offset=2)
    border(im, (70, 110, 122))
    save(im, "tank_glass")


def tex_water_normal():
    base = (60, 130, 165)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    save(im, "water_normal")


def tex_water_exposed():
    im = new_canvas((40, 90, 115))
    rect(im, 0, 10, 15, 15, (60, 130, 165))  # remaining water at the bottom
    rect(im, 6, 3, 9, 9, (220, 110, 40))  # exposed heater element
    save(im, "water_exposed")


def tex_warmer_body():
    base = (200, 200, 196)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    border(im, (150, 150, 144))
    save(im, "warmer_body")


def tex_warmer_top_normal():
    im = new_canvas((200, 200, 196))
    disc_fill(im, 7.5, 7.5, 6, (60, 60, 62))
    save(im, "warmer_top_normal")


def tex_wire_insulated():
    base = (22, 22, 24)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.3, dark=0.85)
    border(im, (10, 10, 11))
    save(im, "wire_insulated")


def tex_crt_case():
    base = (196, 190, 176)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.88)
    inset_edges(im, 0, 0, W - 1, H - 1, (220, 214, 198), (128, 122, 108))
    save(im, "crt_case")


def tex_dvr_case():
    base = (32, 32, 35)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (54, 54, 58), (10, 10, 12))
    save(im, "dvr_case")


def tex_parol_star_off():
    base = (222, 214, 190)
    im = new_canvas((30, 26, 20))
    for cx, cy in [(7, 7)]:
        for r in range(6, 0, -2):
            disc_fill(im, cx, cy, r, scale(base, 1.0 + r * 0.02))
    save(im, "parol_star_off")


def tex_parol_star_lit():
    im = new_canvas((30, 26, 20))
    disc_fill(im, 7, 7, 6, (255, 210, 90))
    disc_fill(im, 7, 7, 3, (255, 245, 200))
    save(im, "parol_star_lit")


ALL += [
    tex_shredder_body, tex_shredder_slot_normal, tex_shredder_slot_jammed,
    tex_cabinet_frame,
    tex_ebike_body, tex_charge_port_normal,
    tex_fixture_housing, tex_tube_normal, tex_tube_dripping,
    tex_tank_glass, tex_water_normal, tex_water_exposed,
    tex_warmer_body, tex_warmer_top_normal,
    tex_wire_insulated,
    tex_crt_case,
    tex_dvr_case,
    tex_parol_star_off, tex_parol_star_lit,
]

# ---------------------------------------------------------------------------
# Phase 5: Laboratory furniture
# ---------------------------------------------------------------------------

def tex_fume_hood_frame():
    base = (214, 214, 210)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    inset_edges(im, 0, 0, W - 1, H - 1, (236, 236, 232), (156, 156, 150))
    save(im, "fume_hood_frame")


def tex_fume_hood_glass():
    base = (170, 200, 210)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.88)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.3, band=1, offset=3)
    border(im, (90, 120, 128))
    save(im, "fume_hood_glass")


def tex_equipment_rack_front():
    base = (30, 30, 33)
    im = new_canvas(base)
    for y0 in (2, 6, 10):
        rect(im, 1, y0, 14, y0 + 2, scale(base, 1.3))
        for x in range(2, 14, 2):
            set_px(im, x, y0 + 1, (60, 60, 64))
    save(im, "equipment_rack_front")


def tex_lab_stool_seat():
    base = (32, 32, 34)
    im = new_canvas((40, 40, 44))
    disc_fill(im, 7.5, 7.5, 6, base)
    disc_ring(im, 7.5, 7.5, 5.5, scale(base, 1.4), thickness=1.0)
    save(im, "lab_stool_seat")


def tex_cart_body():
    base = (188, 190, 194)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.88)
    inset_edges(im, 0, 0, W - 1, H - 1, (212, 214, 218), (128, 130, 134))
    save(im, "cart_body")


def tex_microscope_base():
    base = (48, 48, 52)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    save(im, "microscope_base")


def tex_microscope_body():
    base = (60, 62, 66)
    im = new_canvas((48, 48, 52))
    rect(im, 6, 1, 9, 12, base)
    rect(im, 6, 1, 9, 1, scale(base, 1.5))
    save(im, "microscope_body")


def tex_eyewash_green():
    base = (46, 122, 70)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.88)
    inset_edges(im, 0, 0, W - 1, H - 1, (70, 152, 92), (24, 72, 40))
    save(im, "eyewash_green")


def tex_component_drawer_front():
    base = (30, 30, 33)
    im = new_canvas(base)
    for y0 in range(1, 15, 3):
        for x0 in range(1, 15, 4):
            rect(im, x0, y0, x0 + 2, y0 + 1, scale(base, 1.5))
            rect(im, x0, y0 + 1, x0, y0 + 1, (230, 228, 220))  # label sliver
    save(im, "component_drawer_front")


def tex_cylinder_body():
    base = (50, 130, 120)
    im = new_canvas(base)
    vertical_brush(im, 3, 0, 12, 15, base, stripe=1.15)
    rect(im, 0, 0, 2, 15, (24, 24, 26))
    rect(im, 13, 0, 15, 15, (24, 24, 26))
    save(im, "cylinder_body")


def tex_granite_top():
    base = (54, 52, 56)
    im = new_canvas(base)
    speckle(im, [(70, 68, 72), (36, 34, 38), (90, 88, 92)], density=0.25)
    inset_edges(im, 0, 0, W - 1, H - 1, (78, 76, 80), (24, 22, 26))
    save(im, "granite_top")


ALL += [
    tex_fume_hood_frame, tex_fume_hood_glass,
    tex_equipment_rack_front,
    tex_lab_stool_seat,
    tex_cart_body,
    tex_microscope_base, tex_microscope_body,
    tex_eyewash_green,
    tex_component_drawer_front,
    tex_cylinder_body,
    tex_granite_top,
]

# ---------------------------------------------------------------------------
# Phase 6: Laboratory hazard textures appended here.
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
