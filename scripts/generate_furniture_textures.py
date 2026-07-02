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
import random
from PIL import Image

random.seed(11)

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


def speckle(im, colors, density=0.1, x0=0, y0=0, x1=W - 1, y1=H - 1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if random.random() < density:
                set_px(im, x, y, random.choice(colors))


def mesh_weave(im, x0, y0, x1, y1, base, dark):
    """Alternating checker dots simulating a woven mesh fabric."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x + y) % 2 == 0:
                set_px(im, x, y, dark)
            else:
                set_px(im, x, y, base)


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


# ---------------------------------------------------------------------------
# ChairBlock — modern task chair (was: raw dark_oak_planks + gray_concrete)
# ---------------------------------------------------------------------------

def tex_chair_frame_black():
    base = (30, 30, 33)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.15)
    inset_edges(im, 0, 0, W - 1, H - 1, (65, 65, 70), (8, 8, 10))
    save(im, "chair_frame_black")


def tex_chair_mesh_fabric():
    base = (60, 63, 68)
    dark = (38, 40, 44)
    im = new_canvas(base)
    mesh_weave(im, 0, 0, W - 1, H - 1, base, dark)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.12, dark=0.92)
    border(im, (25, 26, 29))
    save(im, "chair_mesh_fabric")


def tex_chair_cushion_fabric():
    base = (52, 54, 58)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    speckle(im, [scale(base, 1.25), scale(base, 0.8)], density=0.14, x0=1, y0=1, x1=14, y1=14)
    inset_edges(im, 0, 0, W - 1, H - 1, (85, 87, 92), (20, 21, 24))
    save(im, "chair_cushion_fabric")


# ---------------------------------------------------------------------------
# DrawersBlock — modern flat-panel dresser (was: raw dark_oak/birch planks)
# ---------------------------------------------------------------------------

def tex_cabinet_body_painted():
    base = (233, 233, 230)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.06, dark=0.95)
    inset_edges(im, 0, 0, W - 1, H - 1, (248, 248, 246), (198, 198, 195))
    border(im, (175, 175, 172))
    save(im, "cabinet_body_painted")


def tex_drawer_front_painted():
    base = (208, 209, 212)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.92)
    inset_edges(im, 0, 0, W - 1, H - 1, (228, 229, 232), (150, 151, 154))
    border(im, (130, 131, 134))
    save(im, "drawer_front_painted")


def tex_handle_bar_metal():
    base = (172, 174, 178)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    rect(im, 0, 3, 15, 4, (215, 216, 220))  # highlight stripe
    inset_edges(im, 0, 0, W - 1, H - 1, (210, 211, 214), (110, 111, 114))
    save(im, "handle_bar_metal")


# ---------------------------------------------------------------------------
# FilingCabinetBlock — graphite metal cabinet (was: raw iron_block reuse)
# ---------------------------------------------------------------------------

def tex_cabinet_body_graphite():
    base = (58, 60, 63)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.1)
    inset_edges(im, 0, 0, W - 1, H - 1, (95, 97, 101), (18, 19, 21))
    save(im, "cabinet_body_graphite")


def tex_drawer_face_graphite():
    base = (78, 81, 85)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.88)
    rect(im, 1, 1, 14, 1, scale(base, 1.35))  # top seam highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (110, 113, 117), (30, 31, 34))
    border(im, (22, 23, 25))
    save(im, "drawer_face_graphite")


def tex_handle_bar_black():
    base = (26, 27, 29)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.4, dark=0.8)
    inset_edges(im, 0, 0, W - 1, H - 1, (55, 56, 59), (5, 5, 6))
    save(im, "handle_bar_black")


def tex_label_holder_white():
    base = (240, 240, 238)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.05, dark=0.95)
    border(im, (150, 150, 148))
    save(im, "label_holder_white")


# ---------------------------------------------------------------------------
# LockerBlock — painted-steel school locker (was: raw iron_block reuse)
# Reuses handle_bar_black and label_holder_white from FilingCabinetBlock.
# ---------------------------------------------------------------------------

def tex_locker_body_painted():
    base = (38, 56, 88)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.12)
    inset_edges(im, 0, 0, W - 1, H - 1, (70, 92, 130), (12, 18, 32))
    save(im, "locker_body_painted")


def tex_locker_door_painted():
    base = (52, 72, 108)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.9)
    inset_edges(im, 0, 0, W - 1, H - 1, (85, 108, 148), (20, 28, 46))
    border(im, (15, 21, 36))
    save(im, "locker_door_painted")


def tex_locker_seam_dark():
    base = (20, 27, 42)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.25, dark=0.85)
    save(im, "locker_seam_dark")


def tex_vent_slats_dark():
    base = (22, 23, 25)
    im = new_canvas(base)
    vents(im, [2, 5, 8, 11, 14], (45, 46, 49), x0=0, x1=15)
    border(im, (10, 10, 11))
    save(im, "vent_slats_dark")


def tex_keypad_lock():
    base = (24, 25, 27)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.2, dark=0.85)
    border(im, (10, 10, 11))
    for y in (4, 7, 10):
        for x in (4, 8, 12):
            set_px(im, x, y, (60, 62, 66))  # keypad buttons
    set_px(im, 8, 13, (60, 220, 110))  # status LED
    save(im, "keypad_lock")


ALL = [
    tex_desk_laminate_white, tex_desk_leg_metal_black, tex_desk_cable_panel_dark,
    tex_chair_frame_black, tex_chair_mesh_fabric, tex_chair_cushion_fabric,
    tex_cabinet_body_painted, tex_drawer_front_painted, tex_handle_bar_metal,
    tex_cabinet_body_graphite, tex_drawer_face_graphite, tex_handle_bar_black,
    tex_label_holder_white,
    tex_locker_body_painted, tex_locker_door_painted, tex_locker_seam_dark,
    tex_vent_slats_dark, tex_keypad_lock,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
