#!/usr/bin/env python3
"""Generates 16x16 textures for the 10 cafeteria furniture blocks: the long
lunch table, tray stack, steam-table serving counter, cafeteria menu board,
condiment station, dual recycle/trash bin, soda fountain machine, stool,
refrigerated salad bar, and snack vending machine.

Mirrors the flat-shaded, bevel-highlighted, hand-iconography style used by
generate_school_textures.py / generate_furniture_textures.py (same helper
functions, same muted/realistic palette approach, same output directory).
Run from repo root:

    python3 scripts/generate_cafeteria_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import os
import random
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import apply_material_signature

random.seed(41)

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


def diagonal_sheen(im, x0, y0, x1, y1, factor=1.25, band=2, offset=0):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if abs((x - y) - offset) <= band:
                cur = im.getpixel((x, y))
                set_px(im, x, y, scale(cur, factor))


def save(im, name):
    apply_material_signature(im)
    im.save(os.path.join(OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# Cafeteria lunch table — bright laminate top, molded bench seats, steel tube
# ---------------------------------------------------------------------------

def tex_cafeteria_table_top():
    base = (58, 120, 168)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.85)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.22, band=2, offset=-3)
    inset_edges(im, 0, 0, W - 1, H - 1, (96, 158, 206), (24, 62, 96))
    border(im, (18, 46, 72))
    save(im, "cafeteria_table_top")


def tex_cafeteria_bench_seat():
    base = (222, 128, 40)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.16, dark=0.85)
    # Molded-plastic ridge lines running the bench length.
    for y in (4, 8, 12):
        rect(im, 1, y, 14, y, scale(base, 0.82))
    inset_edges(im, 0, 0, W - 1, H - 1, (248, 168, 90), (140, 72, 20))
    border(im, (96, 48, 12))
    save(im, "cafeteria_bench_seat")


def tex_steel_tube_frame():
    base = (150, 154, 160)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.2)
    rect(im, 6, 0, 9, 15, scale(base, 1.55))  # rounded tube-steel highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (206, 210, 214), (78, 82, 88))
    save(im, "steel_tube_frame")


# ---------------------------------------------------------------------------
# Cafeteria tray stack — glossy molded plastic
# ---------------------------------------------------------------------------

def _tray_body(base, edge, name):
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.82)
    # Molded-plastic cafeteria-tray compartment grooves (the recognizable
    # divided-tray silhouette, not just a flat colored slab).
    rect(im, 2, 2, 13, 2, scale(base, 1.4))
    rect(im, 2, 6, 13, 6, scale(base, 0.65))
    rect(im, 2, 10, 8, 10, scale(base, 0.65))
    rect(im, 9, 10, 13, 10, scale(base, 0.65))
    inset_edges(im, 0, 0, W - 1, H - 1, scale(base, 1.5), scale(base, 0.55))
    border(im, edge)
    save(im, name)


def tex_tray_plastic_orange():
    _tray_body((224, 108, 24), (98, 40, 6), "tray_plastic_orange")


def tex_tray_plastic_blue():
    _tray_body((36, 108, 186), (10, 40, 78), "tray_plastic_blue")


def tex_tray_plastic_green():
    _tray_body((54, 148, 76), (14, 62, 28), "tray_plastic_green")


def tex_tray_plastic_orange_rim():
    base = (250, 158, 78)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.15, dark=0.92)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.2, band=2, offset=2)
    border(im, (150, 74, 20))
    save(im, "tray_plastic_orange_rim")


def tex_tray_rim_light():
    base = (222, 224, 226)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.12, dark=0.92)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.15, band=2, offset=2)
    border(im, (150, 152, 154))
    save(im, "tray_rim_light")


# ---------------------------------------------------------------------------
# Steam-table serving counter — brushed stainless steel + glass sneeze guard
# ---------------------------------------------------------------------------

def tex_steel_counter_body():
    base = (176, 180, 186)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (218, 220, 224), (96, 98, 102))
    rect(im, 0, 11, 15, 11, scale(base, 0.75))  # kick-panel seam
    border(im, (70, 72, 76))
    save(im, "steel_counter_body")


def tex_food_well_metal():
    base = (120, 124, 130)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.8)
    # Three recessed steam-table wells.
    for x0 in (1, 6, 11):
        rect(im, x0, 3, x0 + 3, 12, scale(base, 0.68))
        inset_edges(im, x0, 3, x0 + 3, 12, scale(base, 0.5), scale(base, 0.9))
    border(im, (52, 54, 58))
    save(im, "food_well_metal")


def tex_sneeze_guard_glass():
    base = (198, 220, 224)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.1, dark=0.95)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.35, band=2, offset=-5)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.2, band=1, offset=6)
    # Thin aluminium edge frame top/bottom.
    rect(im, 0, 0, 15, 1, (150, 156, 160))
    rect(im, 0, 14, 15, 15, (150, 156, 160))
    save(im, "sneeze_guard_glass")


# ---------------------------------------------------------------------------
# Cafeteria menu board — chalk surface, header stripe, wood frame
# ---------------------------------------------------------------------------

def tex_menu_board_frame_wood():
    base = (120, 78, 46)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.14)
    inset_edges(im, 0, 0, W - 1, H - 1, (166, 116, 72), (58, 36, 18))
    border(im, (40, 24, 12))
    save(im, "menu_board_frame_wood")


def tex_menu_board_surface():
    base = (30, 40, 34)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.08, dark=0.94)
    speckle(im, [scale(base, 1.3)], density=0.05)
    # Faint painted price-list lines.
    for y in (3, 6, 9, 12):
        rect(im, 2, y, 11, y, scale(base, 1.25))
    border(im, (16, 22, 18))
    save(im, "menu_board_surface")


def tex_menu_board_header():
    red = (196, 30, 30)
    im = new_canvas(red)
    gradient_shade(im, 0, 0, W - 1, H - 1, red, light=1.15, dark=0.88)
    cream = (250, 240, 220)
    d = ImageDraw.Draw(im)
    # Universal fork + knife "menu/dining" pictogram — instantly recognizable,
    # unlike a vague lettering block.
    d.line([(4, 1), (4, 7)], fill=cream, width=1)          # fork handle
    d.line([(3, 1), (3, 4)], fill=cream, width=1)          # fork tine (left)
    d.line([(5, 1), (5, 4)], fill=cream, width=1)          # fork tine (right)
    d.line([(4, 7), (4, 14)], fill=cream, width=1)         # fork shaft
    d.line([(11, 1), (11, 6)], fill=cream, width=1)        # knife blade
    d.polygon([(10, 1), (12, 1), (11, 7)], fill=cream)     # knife blade taper
    d.line([(11, 7), (11, 14)], fill=cream, width=1)       # knife handle
    inset_edges(im, 0, 0, W - 1, H - 1, (226, 70, 70), (110, 10, 10))
    border(im, (90, 8, 8))
    save(im, "menu_board_header")


# ---------------------------------------------------------------------------
# Condiment station — metal tray, ketchup/mustard bottles, chrome napkin box
# ---------------------------------------------------------------------------

def tex_condiment_tray_metal():
    base = (172, 176, 182)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (210, 213, 217), (94, 96, 100))
    border(im, (74, 76, 80))
    save(im, "condiment_tray_metal")


def tex_bottle_ketchup_red():
    base = (188, 30, 30)
    im = new_canvas(base)
    gradient_shade(im, 2, 2, 13, 15, base, light=1.3, dark=0.75)
    rect(im, 6, 0, 9, 2, (240, 240, 236))  # nozzle cap
    rect(im, 4, 5, 5, 13, scale(base, 1.4))  # highlight streak
    # White oval-ish label with a bold "K" — reads as a labelled squeeze
    # bottle rather than a plain red blob.
    rect(im, 6, 7, 12, 12, (250, 248, 244))
    rect(im, 7, 8, 8, 11, (188, 30, 30))
    rect(im, 9, 8, 9, 8, (188, 30, 30))
    rect(im, 9, 9, 10, 9, (188, 30, 30))
    rect(im, 9, 10, 9, 10, (188, 30, 30))
    rect(im, 9, 11, 10, 11, (188, 30, 30))
    inset_edges(im, 2, 3, 13, 15, scale(base, 1.2), scale(base, 0.7))
    border(im, (74, 8, 8))
    save(im, "bottle_ketchup_red")


def tex_bottle_mustard_yellow():
    base = (222, 186, 30)
    im = new_canvas(base)
    gradient_shade(im, 2, 2, 13, 15, base, light=1.3, dark=0.78)
    rect(im, 6, 0, 9, 2, (240, 240, 236))  # nozzle cap
    rect(im, 4, 5, 5, 13, scale(base, 1.35))  # highlight streak
    # White label with a bold "M", same layout as the ketchup bottle so the
    # pair reads as a matched squeeze-bottle set.
    rect(im, 6, 7, 12, 12, (250, 248, 244))
    rect(im, 7, 8, 7, 11, (200, 160, 20))
    rect(im, 11, 8, 11, 11, (200, 160, 20))
    rect(im, 8, 8, 8, 9, (200, 160, 20))
    rect(im, 10, 8, 10, 9, (200, 160, 20))
    rect(im, 9, 9, 9, 10, (200, 160, 20))
    inset_edges(im, 2, 3, 13, 15, scale(base, 1.2), scale(base, 0.72))
    border(im, (110, 88, 8))
    save(im, "bottle_mustard_yellow")


def tex_napkin_holder_chrome():
    base = (206, 210, 214)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.25)
    rect(im, 2, 2, 13, 12, (250, 250, 248))  # napkin stack showing through
    inset_edges(im, 2, 2, 13, 12, (255, 255, 255), (210, 210, 208))
    inset_edges(im, 0, 0, W - 1, H - 1, (232, 234, 236), (120, 122, 126))
    border(im, (96, 98, 102))
    save(im, "napkin_holder_chrome")


# ---------------------------------------------------------------------------
# Dual recycle / trash bin
# ---------------------------------------------------------------------------

def tex_bin_recycle_green():
    base = (46, 128, 64)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.16, dark=0.82)
    white = (240, 250, 240)
    d = ImageDraw.Draw(im)
    # The universal three-chasing-arrows recycling triangle, not an abstract hint.
    cx, cy, r = 7.5, 8, 5.5
    import math
    for i in range(3):
        ang = math.radians(-90 + i * 120)
        ang_next = math.radians(-90 + i * 120 + 95)
        x0, y0 = cx + r * math.cos(ang), cy + r * math.sin(ang)
        x1, y1 = cx + r * math.cos(ang_next), cy + r * math.sin(ang_next)
        d.line([(x0, y0), (x1, y1)], fill=white, width=2)
        # Arrowhead at the end of each chasing stroke.
        head_ang = ang_next
        hx, hy = x1, y1
        perp = head_ang + math.radians(90)
        d.polygon([
            (hx, hy),
            (hx - 2.2 * math.cos(head_ang - 0.5), hy - 2.2 * math.sin(head_ang - 0.5)),
            (hx - 1.0 * math.cos(perp), hy - 1.0 * math.sin(perp)),
        ], fill=white)
    inset_edges(im, 0, 0, W - 1, H - 1, (78, 168, 96), (18, 62, 30))
    border(im, (14, 48, 22))
    save(im, "bin_recycle_green")


def tex_bin_trash_gray():
    base = (98, 100, 104)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.14, dark=0.82)
    dark = (52, 53, 56)
    d = ImageDraw.Draw(im)
    # Simple trash-can pictogram: lid + body + vertical rib lines.
    d.rectangle((4, 2, 11, 3), fill=dark)     # lid
    d.rectangle((6, 0, 9, 1), fill=dark)      # lid handle
    d.polygon([(5, 4), (10, 4), (9, 13), (6, 13)], outline=dark, width=1)  # tapered can body
    for x in (6, 7.5, 9):
        d.line([(x, 5), (x - 0.3, 12)], fill=dark, width=1)
    inset_edges(im, 0, 0, W - 1, H - 1, (140, 142, 146), (46, 47, 50))
    border(im, (34, 35, 38))
    save(im, "bin_trash_gray")


def tex_bin_flap_black():
    base = (34, 34, 36)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.3, dark=0.8)
    rect(im, 0, 6, 15, 6, scale(base, 1.6))  # hinge line highlight
    border(im, (12, 12, 13))
    save(im, "bin_flap_black")


# ---------------------------------------------------------------------------
# Soda fountain machine — red housing, chrome control panel, dark nozzle
# ---------------------------------------------------------------------------

def tex_soda_machine_body_red():
    base = (176, 24, 32)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.18, dark=0.82)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.2, band=2, offset=-4)
    inset_edges(im, 0, 0, W - 1, H - 1, (216, 60, 66), (86, 8, 12))
    border(im, (60, 6, 8))
    save(im, "soda_machine_body_red")


def tex_soda_machine_panel_chrome():
    base = (190, 194, 198)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.18)
    d = ImageDraw.Draw(im)
    # Red splash-drop brand badge at the top — the recognizable "soda logo"
    # cue real dispensers always carry.
    d.ellipse((5, 1, 10, 6), fill=(196, 24, 30))
    d.polygon([(7.5, 0), (9.5, 3), (5.5, 3)], fill=(196, 24, 30))
    d.ellipse((6.5, 2.3, 8.5, 4.3), fill=(240, 210, 210))
    # Row of larger, clearly separated colored drink-selection buttons.
    colors = [(200, 30, 30), (230, 170, 20), (30, 110, 190), (40, 140, 70)]
    for i, c in enumerate(colors):
        x0 = 1 + i * 3
        rect(im, x0, 8, x0 + 1, 9, c)
        rect(im, x0, 8, x0, 8, scale(c, 1.4))
    rect(im, 1, 11, 14, 14, (48, 50, 54))  # dark cup-fill recess
    inset_edges(im, 1, 11, 14, 14, (30, 32, 35), (70, 72, 76))
    inset_edges(im, 0, 0, W - 1, H - 1, (224, 226, 228), (112, 114, 118))
    border(im, (80, 82, 86))
    save(im, "soda_machine_panel_chrome")


def tex_soda_nozzle_dark():
    base = (36, 36, 38)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.4, dark=0.75)
    rect(im, 6, 12, 9, 15, scale(base, 0.6))  # drip tray shadow
    border(im, (14, 14, 15))
    save(im, "soda_nozzle_dark")


# ---------------------------------------------------------------------------
# Cafeteria stool — vinyl seat, chrome pole/base
# ---------------------------------------------------------------------------

def tex_stool_seat_vinyl_red():
    base = (168, 26, 34)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.25, dark=0.78)
    # Diamond-quilted diner-stool stitch pattern — a much more recognizable
    # "padded vinyl seat" cue than a plain gradient disc.
    stitch = scale(base, 0.6)
    d = ImageDraw.Draw(im)
    for i in range(-1, 5):
        d.line([(i * 4 - 2, 15), (i * 4 + 6, -1)], fill=stitch, width=1)
        d.line([(i * 4 + 6, 15), (i * 4 - 2, -1)], fill=stitch, width=1)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.15, band=1, offset=0)
    inset_edges(im, 1, 1, 14, 14, scale(base, 1.3), scale(base, 0.65))
    border(im, (64, 8, 12))
    save(im, "stool_seat_vinyl_red")


def tex_stool_pole_chrome():
    base = (200, 204, 208)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.3)
    rect(im, 7, 0, 8, 15, scale(base, 1.6))  # bright center pole highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (232, 234, 236), (100, 102, 106))
    save(im, "stool_pole_chrome")


# ---------------------------------------------------------------------------
# Refrigerated salad bar — steel body, chilled ice-well top
# ---------------------------------------------------------------------------

def tex_salad_bar_body_steel():
    base = (182, 186, 192)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (220, 222, 226), (100, 102, 106))
    rect(im, 0, 12, 15, 12, scale(base, 0.7))
    border(im, (76, 78, 82))
    save(im, "salad_bar_body_steel")


def tex_salad_bar_ice_well():
    base = (208, 232, 238)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.9)
    # Chilled produce wells with faint cold-mist highlight.
    for x0 in (1, 6, 11):
        rect(im, x0, 2, x0 + 3, 13, scale(base, 0.9))
        set_px(im, x0 + 1, 3, (255, 255, 255))
    border(im, (120, 168, 176))
    save(im, "salad_bar_ice_well")


# ---------------------------------------------------------------------------
# Snack vending machine — red frame, glass front with snack rows, coin slot
# ---------------------------------------------------------------------------

def tex_vending_body_red():
    base = (168, 26, 30)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.16, dark=0.82)
    inset_edges(im, 0, 0, W - 1, H - 1, (206, 62, 64), (78, 8, 10))
    border(im, (54, 6, 8))
    save(im, "vending_body_red")


def tex_vending_glass_front():
    base = (48, 52, 58)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.1, dark=0.85)
    # Snack rows behind glass: each shelf gets a metal spiral coil (the
    # unmistakable vending-machine cue) holding distinct, larger packets —
    # clearer and more recognizable than loose colored blobs.
    snack_colors = [(230, 190, 40), (210, 60, 50), (60, 150, 90), (230, 140, 30), (90, 110, 200)]
    coil = (170, 172, 176)
    for row_y in (1, 6, 11):
        rect(im, 0, row_y + 4, 15, row_y + 4, scale(coil, 0.6))  # shelf ledge
        x = 0
        col_i = 0
        while x <= 14:
            wpk = 3
            c = snack_colors[col_i % len(snack_colors)]
            rect(im, x, row_y, min(x + wpk - 1, 15), row_y + 2, c)
            rect(im, x, row_y, min(x + wpk - 1, 15), row_y, scale(c, 1.3))
            for cx in range(x, min(x + wpk - 1, 15) + 1):
                set_px(im, cx, row_y + 3, coil if cx % 2 == 0 else scale(coil, 0.7))
            x += wpk
            col_i += 1
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.25, band=1, offset=-6)
    border(im, (18, 20, 24))
    save(im, "vending_glass_front")


def tex_vending_marquee_header():
    base = (196, 24, 30)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.2, dark=0.85)
    d = ImageDraw.Draw(im)
    # Illuminated marquee: a bright "lit panel" strip plus a bold snack-bag
    # silhouette badge, reading as branded signage rather than a blank cap.
    rect(im, 1, 3, 14, 9, (250, 236, 60))
    inset_edges(im, 1, 3, 14, 9, (255, 250, 150), (196, 170, 20))
    d.polygon([(6, 4), (10, 4), (11, 8), (5, 8)], fill=(196, 24, 30))
    d.line([(6, 4), (5.3, 2.5)], fill=(120, 14, 18), width=1)
    d.line([(10, 4), (10.7, 2.5)], fill=(120, 14, 18), width=1)
    border(im, (110, 10, 12))
    save(im, "vending_marquee_header")


def tex_vending_coin_slot():
    base = (60, 62, 66)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.3, dark=0.8)
    rect(im, 4, 6, 11, 8, (24, 24, 26))  # coin slot mouth
    rect(im, 5, 11, 10, 12, (30, 140, 60))  # ready LED
    border(im, (20, 20, 22))
    save(im, "vending_coin_slot")


ALL = [
    tex_cafeteria_table_top, tex_cafeteria_bench_seat, tex_steel_tube_frame,
    tex_tray_plastic_orange, tex_tray_plastic_blue, tex_tray_plastic_green,
    tex_tray_plastic_orange_rim, tex_tray_rim_light,
    tex_steel_counter_body, tex_food_well_metal, tex_sneeze_guard_glass,
    tex_menu_board_frame_wood, tex_menu_board_surface, tex_menu_board_header,
    tex_condiment_tray_metal, tex_bottle_ketchup_red, tex_bottle_mustard_yellow, tex_napkin_holder_chrome,
    tex_bin_recycle_green, tex_bin_trash_gray, tex_bin_flap_black,
    tex_soda_machine_body_red, tex_soda_machine_panel_chrome, tex_soda_nozzle_dark,
    tex_stool_seat_vinyl_red, tex_stool_pole_chrome,
    tex_salad_bar_body_steel, tex_salad_bar_ice_well,
    tex_vending_body_red, tex_vending_glass_front, tex_vending_coin_slot, tex_vending_marquee_header,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
