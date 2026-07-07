#!/usr/bin/env python3
"""Generates 16x16 textures for a set of school furniture/decor blocks:
teacher's desk, classroom armchair-desk, bookshelf, Philippine flag +
pole, trophy display cabinet, water dispenser + jug, wall clock,
blackboard, lectern, and a classroom globe.

Mirrors the flat-shaded, bevel-highlighted, hand-iconography style used by
generate_furniture_textures.py and generate_hazard_textures.py (same
helper functions, same muted/realistic palette approach, same output
directory). Run from repo root:

    python3 scripts/generate_school_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import math
import os
import random
from PIL import Image

random.seed(27)

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


def wood_grain(im, x0, y0, x1, y1, base, rows, variance=0.06):
    """A handful of thin horizontal streaks of slightly varied shade,
    reading as subtle wood grain rather than plywood-mesh noise."""
    for y in rows:
        factor = 1.0 + random.uniform(-variance, variance)
        rect(im, x0, y, x1, y, scale(base, factor))


def line_px(im, x0, y0, x1, y1, color):
    """Simple Bresenham line — used for clock hands and the globe's
    meridian arc, neither of which are axis-aligned rectangles."""
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    while True:
        set_px(im, x, y, color)
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy


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
    """Brightens a thin diagonal band to fake a glass/varnish reflection."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if abs((x - y) - offset) <= band:
                cur = im.getpixel((x, y))
                set_px(im, x, y, scale(cur, factor))


def save(im, name):
    im.save(os.path.join(OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# Teacher's desk — warm varnished wood, drawer pedestal front, plain side
# ---------------------------------------------------------------------------

def tex_teacher_desk_top():
    base = (168, 118, 74)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.18, dark=0.85)
    wood_grain(im, 1, 2, 14, 13, base, rows=[3, 6, 9, 12])
    rect(im, 2, 1, 6, 1, scale(base, 1.35))  # varnish glare streak
    inset_edges(im, 0, 0, W - 1, H - 1, (206, 156, 104), (108, 74, 44))
    border(im, (86, 58, 34))
    save(im, "teacher_desk_top")


def tex_teacher_desk_front():
    base = (140, 96, 58)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.1, dark=0.9)
    border(im, (72, 48, 28))
    # Two drawer fronts, stacked, each with a dark handle.
    for dy0, dy1 in ((2, 6), (8, 12)):
        inset_edges(im, 2, dy0, 13, dy1, scale(base, 1.25), scale(base, 0.72))
        rect(im, 2, dy0, 13, dy1, scale(base, 1.02))
        inset_edges(im, 2, dy0, 13, dy1, scale(base, 1.2), scale(base, 0.75))
        rect(im, 6, dy0 + 1, 9, dy0 + 2, (30, 26, 22))  # dark handle
    save(im, "teacher_desk_front")


def tex_teacher_desk_side():
    base = (168, 118, 74)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.08, dark=0.9)
    wood_grain(im, 1, 2, 14, 13, base, rows=[4, 8, 11], variance=0.04)
    inset_edges(im, 0, 0, W - 1, H - 1, (198, 148, 96), (100, 68, 40))
    border(im, (86, 58, 34))
    save(im, "teacher_desk_side")


# ---------------------------------------------------------------------------
# Classroom armchair-desk (monobloc chair + fold-up writing tablet)
# ---------------------------------------------------------------------------

def tex_armchair_wood_seat():
    base = (214, 176, 128)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.12, dark=0.88)
    wood_grain(im, 1, 1, 14, 14, base, rows=[3, 7, 11], variance=0.05)
    inset_edges(im, 0, 0, W - 1, H - 1, (238, 202, 156), (150, 116, 78))
    border(im, (120, 90, 58))
    save(im, "armchair_wood_seat")


def tex_armchair_frame_metal():
    base = (40, 44, 50)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.15)
    rect(im, 7, 0, 8, 15, scale(base, 1.5))  # rounded tube-steel highlight
    inset_edges(im, 0, 0, W - 1, H - 1, (78, 84, 92), (12, 13, 15))
    save(im, "armchair_frame_metal")


def tex_armchair_tablet_top():
    base = (224, 188, 138)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.88)
    wood_grain(im, 1, 1, 14, 14, base, rows=[5, 9], variance=0.04)
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.2, band=1, offset=-4)
    inset_edges(im, 0, 0, W - 1, H - 1, (245, 214, 168), (158, 126, 86))
    border(im, (128, 98, 64))
    save(im, "armchair_tablet_top")


# ---------------------------------------------------------------------------
# Bookshelf — two spine arrangements + dark wood frame
# ---------------------------------------------------------------------------

def tex_bookshelf_spines_a():
    shelf_bg = (58, 42, 28)
    im = new_canvas(shelf_bg)
    palette = [(150, 52, 46), (58, 90, 150), (66, 122, 78), (188, 140, 58),
               (120, 66, 130), (170, 168, 150)]
    rows = [(1, 6), (9, 14)]
    for y0, y1 in rows:
        x = 1
        while x <= 14:
            wband = random.choice([1, 2, 2, 3])
            x1 = min(x + wband, 14)
            col = random.choice(palette)
            rect(im, x, y0, x1, y1, col)
            rect(im, x, y0, x, y1, scale(col, 1.35))  # spine seam highlight
            x = x1 + 1
    rect(im, 0, 7, 15, 8, (34, 24, 16))  # shelf board
    border(im, (24, 17, 11))
    save(im, "bookshelf_spines_a")


def tex_bookshelf_spines_b():
    shelf_bg = (58, 42, 28)
    im = new_canvas(shelf_bg)
    palette = [(178, 96, 40), (46, 74, 132), (150, 46, 58), (74, 132, 96),
               (140, 130, 60), (90, 60, 116)]
    rows = [(1, 5), (10, 14)]
    for y0, y1 in rows:
        x = 1
        while x <= 14:
            wband = random.choice([2, 2, 3, 1])
            x1 = min(x + wband, 14)
            col = random.choice(palette)
            rect(im, x, y0, x1, y1, col)
            rect(im, x1, y0, x1, y1, scale(col, 0.65))  # spine seam shadow
            x = x1 + 1
    rect(im, 0, 6, 15, 6, (30, 21, 14))
    rect(im, 0, 15, 15, 15, (30, 21, 14))
    border(im, (24, 17, 11))
    save(im, "bookshelf_spines_b")


def tex_bookshelf_frame_wood():
    base = (58, 40, 26)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.14)
    inset_edges(im, 0, 0, W - 1, H - 1, (92, 66, 42), (22, 14, 8))
    border(im, (16, 10, 6))
    save(im, "bookshelf_frame_wood")


# ---------------------------------------------------------------------------
# Philippine flag panel + brass pole
# ---------------------------------------------------------------------------

def tex_flag_ph_panel():
    blue = (0, 56, 168)
    red = (206, 17, 38)
    white = (250, 250, 248)
    gold = (252, 209, 22)
    im = new_canvas(blue)
    mid = 7.5
    for y in range(H):
        frac = 1.0 - abs(y - mid) / (mid + 0.5)
        tri_end = int(2 + frac * 5)  # hoist triangle apex reaches ~x=7
        for x in range(W):
            if x <= tri_end:
                set_px(im, x, y, white)
            elif y <= 7:
                set_px(im, x, y, blue)
            else:
                set_px(im, x, y, red)
    # Tiny golden sun at the triangle's centre with a few rays.
    set_px(im, 3, 7, gold)
    set_px(im, 4, 7, gold)
    set_px(im, 3, 8, gold)
    set_px(im, 4, 8, gold)
    for rx, ry in ((3, 5), (3, 10), (1, 7), (1, 8), (5, 5), (5, 10)):
        set_px(im, rx, ry, gold)
    # Waving-cloth shading: gentle sinusoidal light/dark vertical bands.
    for y in range(H):
        for x in range(W):
            cur = im.getpixel((x, y))
            factor = 1.0 + 0.1 * math.sin(x * 0.85 + y * 0.15)
            set_px(im, x, y, scale(cur, factor))
    border(im, (30, 26, 20))
    save(im, "flag_ph_panel")


def tex_flag_pole_brass():
    bg = (22, 22, 24)
    im = new_canvas(bg)
    brass = (196, 154, 62)
    rect(im, 7, 0, 8, 12, brass)
    rect(im, 7, 0, 7, 12, scale(brass, 1.35))  # highlight edge
    rect(im, 8, 0, 8, 12, scale(brass, 0.75))  # shadow edge
    set_px(im, 7, 0, (232, 200, 120))  # pole cap glint
    # Round dark base.
    disc_fill(im, 7, 14, 5, (30, 30, 33))
    disc_ring(im, 7, 14, 5, (50, 50, 54))
    save(im, "flag_pole_brass")


# ---------------------------------------------------------------------------
# Trophy display cabinet — shelf of trophies/medal + dark wood frame
# ---------------------------------------------------------------------------

def tex_trophy_shelf_display():
    bg = (40, 34, 26)
    im = new_canvas(bg)
    gold = (218, 165, 32)
    gold_hi = (255, 221, 130)
    rows = [(1, 6), (8, 13)]
    trophy_x = [2, 7, 12]
    for i, (y0, y1) in enumerate(rows):
        for tx in trophy_x:
            # Cup: small U-shaped bowl + stem + base.
            rect(im, tx, y0 + 1, tx + 1, y0 + 2, gold)
            set_px(im, tx, y0 + 1, gold_hi)
            rect(im, tx, y0 + 3, tx, y0 + 3, gold)
            rect(im, tx, y0 + 4, tx + 1, y0 + 4, scale(gold, 0.8))
    # Medal on the lower shelf, replacing the middle trophy with a disc + ribbon.
    disc_fill(im, 7, 10, 2, gold)
    disc_ring(im, 7, 10, 2, gold_hi)
    rect(im, 6, 8, 7, 8, (150, 40, 40))
    rect(im, 8, 8, 8, 8, (40, 60, 140))
    rect(im, 0, 7, 15, 7, (26, 20, 14))  # shelf board
    rect(im, 0, 14, 15, 14, (26, 20, 14))
    diagonal_sheen(im, 0, 0, W - 1, H - 1, factor=1.18, band=2, offset=-3)
    border(im, (16, 12, 8))
    save(im, "trophy_shelf_display")


def tex_cabinet_wood_frame():
    base = (58, 40, 26)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.16)
    inset_edges(im, 0, 0, W - 1, H - 1, (98, 70, 44), (18, 11, 6))
    border(im, (14, 9, 5))
    save(im, "cabinet_wood_frame")


# ---------------------------------------------------------------------------
# Water dispenser + jug
# ---------------------------------------------------------------------------

def tex_dispenser_body_white():
    base = (238, 238, 236)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.08, dark=0.93)
    inset_edges(im, 0, 0, W - 1, H - 1, (252, 252, 251), (188, 189, 187))
    border(im, (165, 166, 164))
    # Two taps.
    rect(im, 5, 5, 6, 6, (192, 40, 36))
    rect(im, 9, 5, 10, 6, (40, 96, 192))
    # Drip tray line.
    rect(im, 2, 10, 13, 10, (170, 171, 169))
    # Vent slits at the base.
    vents(im, [12, 13, 14], (150, 151, 149), x0=3, x1=12)
    save(im, "dispenser_body_white")


def tex_water_jug_blue():
    base = (60, 130, 190)
    im = new_canvas(base)
    gradient_shade(im, 2, 2, 13, 15, base, light=1.3, dark=0.75)
    # Neck/cap.
    rect(im, 5, 0, 10, 2, (225, 232, 238))
    rect(im, 6, 0, 9, 1, (245, 248, 250))
    # Highlight sheen streak down the body.
    rect(im, 4, 4, 5, 13, scale(base, 1.45))
    inset_edges(im, 2, 3, 13, 15, scale(base, 1.25), scale(base, 0.7))
    border(im, (24, 60, 96))
    save(im, "water_jug_blue")


# ---------------------------------------------------------------------------
# Wall clock — classic 10:10 showroom display
# ---------------------------------------------------------------------------

def tex_wall_clock_face():
    im = new_canvas((30, 30, 30))
    disc_fill(im, 7.5, 7.5, 7.5, (40, 40, 40))
    disc_fill(im, 7.5, 7.5, 6.6, (250, 250, 247))
    disc_ring(im, 7.5, 7.5, 6.6, (25, 25, 25))
    # Hour ticks at 12/3/6/9 (longer) and 4 minor ticks between.
    for tx, ty in ((7, 1), (7, 8), (1, 7), (8, 7)):
        set_px(im, tx, ty, (20, 20, 20))
        set_px(im, tx + (1 if tx == 7 else 0), ty + (1 if ty == 7 else 0), (20, 20, 20))
    for tx, ty in ((3, 2), (12, 2), (3, 12), (12, 12)):
        set_px(im, tx, ty, (60, 60, 60))
    # Hands at ten-past-ten: hour hand up-left (short), minute hand up-right (long).
    line_px(im, 8, 8, 5, 4, (15, 15, 15))
    line_px(im, 8, 8, 11, 3, (15, 15, 15))
    set_px(im, 8, 8, (10, 10, 10))  # centre pin
    save(im, "wall_clock_face")


# ---------------------------------------------------------------------------
# Blackboard — chalkboard surface + light wood frame
# ---------------------------------------------------------------------------

def tex_blackboard_surface():
    base = (34, 82, 56)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.06, dark=0.95)
    speckle(im, [scale(base, 1.3), scale(base, 1.15)], density=0.05, x0=1, y0=1, x1=14, y1=14)
    # One faint chalk scribble line.
    line_px(im, 3, 5, 12, 4, scale(base, 1.4))
    line_px(im, 12, 4, 6, 9, scale(base, 1.4))
    border(im, (18, 48, 32))
    save(im, "blackboard_surface")


def tex_blackboard_frame_wood():
    base = (196, 158, 110)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.12)
    inset_edges(im, 0, 0, W - 1, H - 1, (224, 192, 148), (128, 98, 64))
    # Chalk tray groove near the bottom.
    rect(im, 1, 12, 14, 13, scale(base, 0.72))
    # A stick of white chalk resting in the tray.
    rect(im, 6, 12, 9, 12, (245, 245, 240))
    save(im, "blackboard_frame_wood")


# ---------------------------------------------------------------------------
# Lectern — paneled wood front with seal hint, matching plain side
# ---------------------------------------------------------------------------

def tex_lectern_front_wood():
    base = (74, 50, 32)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.15, dark=0.85)
    # Panel grooves splitting the front into three vertical panels.
    rect(im, 5, 1, 5, 14, scale(base, 0.6))
    rect(im, 10, 1, 10, 14, scale(base, 0.6))
    # Lighter circular seal emblem hint, centred.
    disc_fill(im, 7.5, 7.5, 3, scale(base, 1.4))
    disc_ring(im, 7.5, 7.5, 3, (196, 160, 96))
    border(im, (36, 24, 14))
    save(im, "lectern_front_wood")


def tex_lectern_wood_side():
    base = (74, 50, 32)
    im = new_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.1, dark=0.88)
    rect(im, 7, 1, 7, 14, scale(base, 0.62))  # single panel groove
    inset_edges(im, 0, 0, W - 1, H - 1, (102, 72, 46), (38, 25, 15))
    border(im, (36, 24, 14))
    save(im, "lectern_wood_side")


# ---------------------------------------------------------------------------
# Classroom globe — sphere map + dark meridian stand
# ---------------------------------------------------------------------------

def tex_globe_sphere():
    ocean = (44, 96, 168)
    im = new_canvas(ocean)
    gradient_shade(im, 0, 2, W - 1, 13, ocean, light=1.2, dark=0.82)
    land_green = (86, 132, 66)
    land_tan = (176, 150, 96)
    blobs = [
        (2, 3, land_green), (3, 4, land_green), (2, 4, land_green),
        (9, 2, land_tan), (10, 3, land_tan), (11, 3, land_tan), (10, 4, land_tan),
        (5, 8, land_green), (6, 8, land_green), (5, 9, land_green), (6, 9, land_tan),
        (12, 8, land_tan), (13, 9, land_tan), (12, 10, land_green),
        (3, 11, land_tan), (9, 11, land_green), (10, 12, land_green),
    ]
    for x, y, c in blobs:
        set_px(im, x, y, c)
    # Faint latitude line across the equator.
    for x in range(W):
        cur = im.getpixel((x, 7))
        set_px(im, x, 7, scale(cur, 1.1))
    # Polar caps.
    rect(im, 0, 0, W - 1, 1, (238, 240, 240))
    rect(im, 0, 14, W - 1, 15, (238, 240, 240))
    save(im, "globe_sphere")


def tex_globe_stand_dark():
    bg = (20, 20, 22)
    im = new_canvas(bg)
    metal = (70, 74, 80)
    metal_hi = (120, 124, 130)
    # Meridian arc (semi-circle ring) cradling the sphere.
    disc_ring(im, 7.5, 6, 6.5, metal, thickness=1.1)
    line_px(im, 1, 6, 1, 10, metal_hi)
    line_px(im, 14, 6, 14, 10, metal_hi)
    # Round base.
    rect(im, 3, 13, 12, 14, metal)
    rect(im, 3, 13, 12, 13, metal_hi)
    rect(im, 5, 15, 10, 15, scale(metal, 0.7))
    save(im, "globe_stand_dark")


ALL = [
    tex_teacher_desk_top, tex_teacher_desk_front, tex_teacher_desk_side,
    tex_armchair_wood_seat, tex_armchair_frame_metal, tex_armchair_tablet_top,
    tex_bookshelf_spines_a, tex_bookshelf_spines_b, tex_bookshelf_frame_wood,
    tex_flag_ph_panel, tex_flag_pole_brass,
    tex_trophy_shelf_display, tex_cabinet_wood_frame,
    tex_dispenser_body_white, tex_water_jug_blue,
    tex_wall_clock_face,
    tex_blackboard_surface, tex_blackboard_frame_wood,
    tex_lectern_front_wood, tex_lectern_wood_side,
    tex_globe_sphere, tex_globe_stand_dark,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
