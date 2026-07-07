#!/usr/bin/env python3
"""Generates 16x16 textures for 10 safety-equipment items/blocks:

Item sprites (transparent background, front-view icon silhouettes):
    fire_blanket, first_aid_kit, megaphone, safety_whistle, flashlight

Block textures (opaque, face-on plate/panel look):
    evac_map_panel, exit_sign_green, smoke_detector_white,
    smoke_detector_alarm, sprinkler_head_metal, emergency_light_off,
    emergency_light_on

Follows the same flat-shaded, bevel-highlighted, muted-base-plus-bright-
accent style already used by generate_furniture_textures.py and
generate_hazard_textures.py, but built on top of PIL's ImageDraw primitives
(polygon/ellipse/line) for the diagonal/rounded shapes the item icons need.
Run from repo root:

    python3 scripts/generate_safety_textures.py

Outputs to:
    src/main/resources/assets/berongsmp/textures/item/
    src/main/resources/assets/berongsmp/textures/block/
"""
import math
import os
import random
from PIL import Image, ImageDraw

random.seed(37)

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "assets", "berongsmp", "textures")
ITEM_OUT = os.path.join(ASSETS, "item")
BLOCK_OUT = os.path.join(ASSETS, "block")
os.makedirs(ITEM_OUT, exist_ok=True)
os.makedirs(BLOCK_OUT, exist_ok=True)

W = H = 16


def scale(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3])


# ---------------------------------------------------------------------------
# Shared low-level helpers
# ---------------------------------------------------------------------------

def item_canvas():
    """Transparent RGBA canvas for item sprites."""
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def block_canvas(bg):
    """Opaque RGB canvas for block textures."""
    return Image.new("RGB", (W, H), bg)


def set_px(im, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), color)


def rgba(color, alpha=255):
    return (color[0], color[1], color[2], alpha)


def rect(im, x0, y0, x1, y1, color):
    d = ImageDraw.Draw(im)
    d.rectangle((x0, y0, x1, y1), fill=color)


def border(im, color, thickness=1):
    rect(im, 0, 0, W - 1, thickness - 1, color)
    rect(im, 0, H - thickness, W - 1, H - 1, color)
    rect(im, 0, 0, thickness - 1, H - 1, color)
    rect(im, W - thickness, 0, W - 1, H - 1, color)


def inset_edges(im, x0, y0, x1, y1, hi, lo):
    """1px bright top/left + dark bottom/right, like a beveled panel."""
    rect(im, x0, y0, x1, y0, hi)
    rect(im, x0, y0, x0, y1, hi)
    rect(im, x0, y1, x1, y1, lo)
    rect(im, x1, y0, x1, y1, lo)


def gradient_shade(im, x0, y0, x1, y1, base, light=1.25, dark=0.8):
    """Diagonal top-left-lit shading across a rectangular region."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) + (y - y0)) / span
            factor = light + (dark - light) * t
            set_px(im, x, y, scale(base, factor))


def gradient_shade_rgba(im, x0, y0, x1, y1, light_base, dark_base):
    """Like gradient_shade but for a transparent RGBA item canvas — only
    repaints pixels that are already opaque, leaving the silhouette intact."""
    span = max((x1 - x0) + (y1 - y0), 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < H and im.getpixel((x, y))[3] > 0:
                t = ((x - x0) + (y - y0)) / span
                c = tuple(int(light_base[i] + (dark_base[i] - light_base[i]) * t) for i in range(3))
                im.putpixel((x, y), rgba(c))


def speckle(im, colors, density=0.08, x0=0, y0=0, x1=W - 1, y1=H - 1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if random.random() < density:
                set_px(im, x, y, random.choice(colors))


def circle(im, cx, cy, r, color, outline=None, width=1):
    d = ImageDraw.Draw(im)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color, outline=outline, width=width)


def ring(im, cx, cy, r, color, width=1):
    d = ImageDraw.Draw(im)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=color, width=width)


def tapered_line(im, x0, y0, x1, y1, r0, r1, color):
    """A straight capsule/cone from (x0,y0) radius r0 to (x1,y1) radius r1 —
    used for the diagonal item silhouettes (megaphone bell, whistle body,
    flashlight barrel) that plain axis-aligned rects can't produce."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / length, dx / length
    p1 = (x0 + nx * r0, y0 + ny * r0)
    p2 = (x1 + nx * r1, y1 + ny * r1)
    p3 = (x1 - nx * r1, y1 - ny * r1)
    p4 = (x0 - nx * r0, y0 - ny * r0)
    d = ImageDraw.Draw(im)
    d.polygon([p1, p2, p3, p4], fill=color)


def cross_band(im, x0, y0, x1, y1, r, t, color, width=1):
    """A short stroke perpendicular to the (x0,y0)-(x1,y1) spine at
    parameter t in [0,1] — grip rings / stripe bands on a diagonal body."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / length, dx / length
    cx, cy = x0 + dx * t, y0 + dy * t
    d = ImageDraw.Draw(im)
    d.line([(cx + nx * r, cy + ny * r), (cx - nx * r, cy - ny * r)], fill=color, width=width)


def save_item(im, name):
    im.save(os.path.join(ITEM_OUT, f"{name}.png"))


def save_block(im, name):
    im.save(os.path.join(BLOCK_OUT, f"{name}.png"))


# ===========================================================================
# ITEM SPRITES (transparent background)
# ===========================================================================

# ---------------------------------------------------------------------------
# fire_blanket — folded red blanket in a white quick-pull wall pouch with a
# red cross-strap holding the fold shut.
# ---------------------------------------------------------------------------

def item_fire_blanket():
    im = item_canvas()
    WHITE = (240, 240, 238)
    WHITE_SHADE = (210, 211, 209)
    RED = (196, 32, 30)
    RED_DK = (150, 22, 20)
    OUTLINE = (70, 70, 68)

    # Pouch body: plain rect with clipped corners (avoids rounded_rectangle
    # + overlapping-shade-loop artifacts around the outline).
    rect(im, 3, 2, 12, 13, rgba(WHITE))
    for (cx, cy) in ((3, 2), (12, 2), (3, 13), (12, 13)):
        set_px(im, cx, cy, (0, 0, 0, 0))
    gradient_shade_rgba(im, 4, 3, 11, 12, WHITE, WHITE_SHADE)
    inset_edges(im, 4, 3, 11, 12, rgba((255, 255, 255)), rgba((190, 191, 189)))
    # 1px outline around the clipped-corner silhouette.
    rect(im, 4, 2, 11, 2, rgba(OUTLINE))
    rect(im, 4, 13, 11, 13, rgba(OUTLINE))
    rect(im, 3, 3, 3, 12, rgba(OUTLINE))
    rect(im, 12, 3, 12, 12, rgba(OUTLINE))

    d = ImageDraw.Draw(im)
    # Red cross-strap (X) holding the folded blanket shut.
    d.line([(4, 4), (11, 11)], fill=rgba(RED), width=1)
    d.line([(11, 4), (4, 11)], fill=rgba(RED), width=1)
    # Buckle dot where the straps cross.
    set_px(im, 7, 7, rgba(RED_DK))
    set_px(im, 8, 7, rgba(RED_DK))
    set_px(im, 7, 8, rgba(RED_DK))
    set_px(im, 8, 8, rgba(RED_DK))
    # Quick-pull tab sticking out of the bottom edge.
    rect(im, 6, 13, 9, 15, rgba(RED))
    rect(im, 6, 13, 9, 13, rgba(RED_DK))
    save_item(im, "fire_blanket")


# ---------------------------------------------------------------------------
# first_aid_kit — white case, red cross, black carry handle.
# ---------------------------------------------------------------------------

def item_first_aid_kit():
    im = item_canvas()
    WHITE = (245, 245, 242)
    OUTLINE = (60, 60, 58)
    RED = (200, 30, 28)
    BLACK = (30, 30, 32)

    d = ImageDraw.Draw(im)
    d.rounded_rectangle((2, 5, 13, 13), radius=1, fill=rgba(WHITE), outline=rgba(OUTLINE))
    gradient_shade(im, 3, 6, 12, 12, WHITE, light=1.15, dark=0.92)
    inset_edges(im, 3, 6, 12, 12, rgba((255, 255, 255)), rgba((205, 205, 202)))
    # Handle.
    d.rectangle((6, 2, 9, 3), fill=rgba(BLACK))
    d.line([(6, 3), (6, 5)], fill=rgba(BLACK), width=1)
    d.line([(9, 3), (9, 5)], fill=rgba(BLACK), width=1)
    # Red cross.
    d.rectangle((6, 7, 9, 11), fill=rgba(RED))
    d.rectangle((5, 8, 10, 10), fill=rgba(RED))
    save_item(im, "first_aid_kit")


# ---------------------------------------------------------------------------
# megaphone — red/white bullhorn angled diagonally, dark handle + trigger.
# ---------------------------------------------------------------------------

def _lerp_pt(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def item_megaphone():
    im = item_canvas()
    WHITE = (238, 238, 235)
    RED = (196, 32, 30)
    RED_DK = (140, 20, 18)
    DARK = (30, 30, 32)
    OUTLINE = (55, 55, 55)

    # Explicit trapezoid corners (narrow mouth bottom-left -> wide bell
    # top-right) — a hand-specified quadrilateral instead of a computed
    # tapered polygon, so the red bell cap below shares exact edge points
    # with the white body and can't leave a seam/notch between them.
    mouth_top = (2.0, 11.0)
    mouth_bot = (4.0, 13.5)
    bell_top = (11.0, 1.0)
    bell_bot = (15.0, 7.0)

    d = ImageDraw.Draw(im)
    body = [mouth_top, bell_top, bell_bot, mouth_bot]
    d.polygon(body, fill=rgba(WHITE), outline=rgba(OUTLINE))

    # Red bell cap — a sub-slice of the same trapezoid (t=0.62..1.0), so its
    # edges land exactly on the white body's edges with no gap.
    t = 0.62
    cap = [_lerp_pt(mouth_top, bell_top, t), bell_top, bell_bot, _lerp_pt(mouth_bot, bell_bot, t)]
    d.polygon(cap, fill=rgba(RED), outline=rgba(RED_DK))

    # Pistol grip handle, attached right at the mouth end.
    d.rectangle((1, 11, 3, 15), fill=rgba(DARK))
    d.rectangle((3, 12, 4, 13), fill=rgba(DARK))  # trigger nub
    save_item(im, "megaphone")


# ---------------------------------------------------------------------------
# safety_whistle — silver pea whistle with an orange lanyard loop.
# ---------------------------------------------------------------------------

def item_safety_whistle():
    im = item_canvas()
    SILVER = (205, 208, 212)
    SILVER_DK = (140, 143, 148)
    DARK = (40, 40, 42)
    ORANGE = (235, 140, 30)

    tapered_line(im, 2.0, 10.0, 12.0, 5.5, 1.6, 2.3, rgba(SILVER))
    cross_band(im, 2.0, 10.0, 12.0, 5.5, 2.3, 0.55, rgba(SILVER_DK), width=1)
    # Mouthpiece opening (dark).
    d = ImageDraw.Draw(im)
    d.ellipse((0, 9, 3, 11), fill=rgba(DARK))
    # Pea hole on top of the body.
    set_px(im, 7, 7, rgba(DARK))
    # Highlight glint.
    set_px(im, 5, 8, rgba((240, 242, 245)))
    # Orange lanyard loop at the tail end.
    d.ellipse((10, 3, 14, 7), outline=rgba(ORANGE), width=1)
    save_item(im, "safety_whistle")


# ---------------------------------------------------------------------------
# flashlight — yellow-and-black hand torch angled diagonally, lens highlight.
# ---------------------------------------------------------------------------

def item_flashlight():
    im = item_canvas()
    YELLOW = (230, 180, 30)
    YELLOW_DK = (170, 128, 15)
    BLACK = (28, 28, 30)
    LENS = (235, 240, 235)
    LENS_GLOW = (255, 255, 220)

    x0, y0 = 2.0, 14.0   # tail/grip
    x1, y1 = 11.0, 4.5   # head
    tapered_line(im, x0, y0, x1, y1, 1.7, 1.7, rgba(YELLOW))
    cross_band(im, x0, y0, x1, y1, 1.9, 0.30, rgba(BLACK), width=1)
    cross_band(im, x0, y0, x1, y1, 1.9, 0.45, rgba(BLACK), width=1)
    cross_band(im, x0, y0, x1, y1, 1.9, 0.60, rgba(YELLOW_DK), width=1)
    # Lens head.
    circle(im, 12, 4, 3, rgba(BLACK), outline=rgba((10, 10, 11)), width=1)
    circle(im, 12, 4, 2, rgba(LENS))
    set_px(im, 11, 3, rgba(LENS_GLOW))
    set_px(im, 13, 5, rgba((200, 205, 200)))
    save_item(im, "flashlight")


# ===========================================================================
# BLOCK TEXTURES (opaque, face-on panel look)
# ===========================================================================

# ---------------------------------------------------------------------------
# evac_map_panel — wall evacuation-map: pale floor-plan lines on white,
# green you-are-here dot, small red exit arrows, thin dark frame.
# ---------------------------------------------------------------------------

def block_evac_map_panel():
    base = (245, 244, 240)
    im = block_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.04, dark=0.97)
    PLAN = (176, 176, 172)
    GREEN = (40, 175, 90)
    RED = (205, 40, 35)

    rect(im, 2, 2, 13, 2, PLAN)   # outer wall top
    rect(im, 2, 2, 2, 13, PLAN)   # outer wall left
    rect(im, 2, 13, 13, 13, PLAN)  # outer wall bottom
    rect(im, 13, 2, 13, 13, PLAN)  # outer wall right
    rect(im, 6, 2, 6, 8, PLAN)    # internal partition
    rect(im, 6, 8, 12, 8, PLAN)   # internal partition

    circle(im, 4, 10, 1, GREEN)   # you-are-here dot

    d = ImageDraw.Draw(im)
    d.polygon([(11, 11), (14, 11), (12.5, 14)], fill=RED)  # exit arrow 1
    d.polygon([(9, 3), (9, 6), (7, 4.5)], fill=RED)        # exit arrow 2

    border(im, (95, 94, 90))
    save_block(im, "evac_map_panel")


# ---------------------------------------------------------------------------
# exit_sign_green — glowing green exit-sign face: bright green background,
# white running-man + arrow pictogram abstraction.
# ---------------------------------------------------------------------------

def block_exit_sign_green():
    base = (26, 150, 70)
    im = block_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.22, dark=0.8)
    WHITE = (240, 250, 240)

    d = ImageDraw.Draw(im)
    # Abstract running-man pictogram.
    d.ellipse((3, 2, 5, 4), fill=WHITE)          # head
    d.polygon([(3, 5), (6, 5), (5, 9), (3, 9)], fill=WHITE)  # torso lean
    d.line([(3, 6), (1, 8)], fill=WHITE, width=1)   # trailing arm
    d.line([(6, 6), (8, 5)], fill=WHITE, width=1)   # leading arm
    d.line([(3, 9), (2, 13)], fill=WHITE, width=1)  # trailing leg
    d.line([(5, 9), (7, 13)], fill=WHITE, width=1)  # leading leg
    # Directional arrow toward the exit.
    d.rectangle((9, 7, 12, 8), fill=WHITE)
    d.polygon([(12, 5), (15, 7.5), (12, 10)], fill=WHITE)

    border(im, (12, 90, 42))
    save_block(im, "exit_sign_green")


# ---------------------------------------------------------------------------
# smoke_detector_white — round white ceiling smoke detector face-on.
# ---------------------------------------------------------------------------

def block_smoke_detector_white():
    ceiling = (222, 222, 218)
    im = block_canvas(ceiling)
    gradient_shade(im, 0, 0, W - 1, H - 1, ceiling, light=1.05, dark=0.96)
    BODY = (240, 240, 236)
    RING = (200, 200, 196)
    VENT = (175, 175, 170)
    LED = (55, 60, 58)

    circle(im, 8, 8, 6, BODY, outline=RING, width=1)
    ring(im, 8, 8, 4, RING, width=1)
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        vx = 8 + round(5.2 * math.cos(rad))
        vy = 8 + round(5.2 * math.sin(rad))
        set_px(im, vx, vy, VENT)
    circle(im, 11, 9, 1, LED)
    save_block(im, "smoke_detector_white")


# ---------------------------------------------------------------------------
# smoke_detector_alarm — same detector, LED bright red + slightly darkened.
# ---------------------------------------------------------------------------

def block_smoke_detector_alarm():
    ceiling = (222, 222, 218)
    im = block_canvas(ceiling)
    gradient_shade(im, 0, 0, W - 1, H - 1, ceiling, light=1.05, dark=0.96)
    BODY = (222, 216, 212)
    RING = (180, 174, 170)
    VENT = (155, 150, 146)
    LED = (235, 45, 40)
    LED_GLOW = (255, 140, 120)

    circle(im, 8, 8, 6, BODY, outline=RING, width=1)
    ring(im, 8, 8, 4, RING, width=1)
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        vx = 8 + round(5.2 * math.cos(rad))
        vy = 8 + round(5.2 * math.sin(rad))
        set_px(im, vx, vy, VENT)
    circle(im, 11, 9, 2, LED_GLOW)
    circle(im, 11, 9, 1, LED)
    save_block(im, "smoke_detector_alarm")


# ---------------------------------------------------------------------------
# sprinkler_head_metal — brass sprinkler head face-on.
# ---------------------------------------------------------------------------

def block_sprinkler_head_metal():
    ceiling = (232, 232, 228)
    im = block_canvas(ceiling)
    gradient_shade(im, 0, 0, W - 1, H - 1, ceiling, light=1.05, dark=0.97)
    BRASS = (196, 156, 62)
    BRASS_DK = (140, 108, 38)
    BRASS_LT = (226, 190, 100)
    DARK = (40, 32, 18)

    circle(im, 8, 8, 6, BRASS, outline=BRASS_DK, width=1)
    speckle(im, [BRASS_LT, BRASS_DK], density=0.08, x0=3, y0=3, x1=13, y1=13)
    d = ImageDraw.Draw(im)
    for ang in range(0, 360, 60):
        rad = math.radians(ang)
        ex = 8 + 5.6 * math.cos(rad)
        ey = 8 + 5.6 * math.sin(rad)
        d.line([(8, 8), (ex, ey)], fill=BRASS_DK, width=1)
    circle(im, 8, 8, 2, DARK, outline=(15, 12, 8), width=1)  # central orifice
    set_px(im, 6, 6, BRASS_LT)  # highlight glint
    save_block(im, "sprinkler_head_metal")


# ---------------------------------------------------------------------------
# emergency_light_off — wall unit, both lamps unlit.
# ---------------------------------------------------------------------------

def block_emergency_light_off():
    base = (98, 100, 104)
    im = block_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    inset_edges(im, 0, 0, W - 1, H - 1, (140, 142, 146), (55, 56, 60))
    border(im, (40, 41, 44))
    LAMP = (58, 59, 63)
    LAMP_RIM = (35, 36, 39)
    circle(im, 5, 8, 3, LAMP, outline=LAMP_RIM, width=1)
    circle(im, 11, 8, 3, LAMP, outline=LAMP_RIM, width=1)
    save_block(im, "emergency_light_off")


# ---------------------------------------------------------------------------
# emergency_light_on — same unit, both lamps glowing warm yellow-white.
# ---------------------------------------------------------------------------

def block_emergency_light_on():
    base = (98, 100, 104)
    im = block_canvas(base)
    gradient_shade(im, 1, 1, 14, 14, base, light=1.15, dark=0.85)
    inset_edges(im, 0, 0, W - 1, H - 1, (140, 142, 146), (55, 56, 60))
    border(im, (40, 41, 44))
    GLOW = (255, 226, 150)
    HALO = (232, 190, 110)
    BRIGHT = (255, 250, 225)
    for cx in (5, 11):
        circle(im, cx, 8, 3, HALO)
        circle(im, cx, 8, 2, GLOW)
        set_px(im, cx - 1, 7, BRIGHT)
    save_block(im, "emergency_light_on")


ITEM_FNS = [
    item_fire_blanket, item_first_aid_kit, item_megaphone,
    item_safety_whistle, item_flashlight,
]

BLOCK_FNS = [
    block_evac_map_panel, block_exit_sign_green,
    block_smoke_detector_white, block_smoke_detector_alarm,
    block_sprinkler_head_metal,
    block_emergency_light_off, block_emergency_light_on,
]

if __name__ == "__main__":
    for fn in ITEM_FNS:
        fn()
    for fn in BLOCK_FNS:
        fn()
    print(f"Wrote {len(ITEM_FNS)} item sprite(s) to {ITEM_OUT}")
    print(f"Wrote {len(BLOCK_FNS)} block texture(s) to {BLOCK_OUT}")
