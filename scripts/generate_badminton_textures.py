#!/usr/bin/env python3
"""Generates textures for the badminton court-marking blocks: the smooth
white connecting court_line (a redstone-wire-style auto-connecting line,
just plain white instead of powered dust), the badminton_net_post anchor
("stitch"), and the badminton_net_mesh panel that auto-fills between two
posts (a real diamond-lattice net with transparent gaps and a solid top
tape band).

Mirrors the flat-shaded, bevel-highlighted style used by the other
generate_*_textures.py scripts (same helper functions, same output
directory). Run from repo root:

    python3 scripts/generate_badminton_textures.py

Outputs to src/main/resources/assets/berongsmp/textures/block/.
"""
import os
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from _texture_style import apply_material_signature

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "berongsmp", "textures", "block")
os.makedirs(OUT, exist_ok=True)

W = H = 16


def new_canvas(bg):
    return Image.new("RGB", (W, H), bg)


def new_canvas_rgba():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


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
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3]) + tuple(color[3:])


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


def save(im, name):
    apply_material_signature(im)
    im.save(os.path.join(OUT, f"{name}.png"))


def save_raw(im, name):
    """No signature pass — used for the transparent net-lattice texture so
    rim_light doesn't tint scattered edge pixels of a mostly-empty alpha
    canvas."""
    im.save(os.path.join(OUT, f"{name}.png"))


# ---------------------------------------------------------------------------
# Court line — a smooth, solid white marking tile (the "redstone but plain
# white" connecting line). Kept deliberately flat/clean, like fresh court tape.
# ---------------------------------------------------------------------------

def tex_court_line():
    base = (248, 248, 244)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.05, dark=0.97)
    border(im, (208, 208, 202))
    save(im, "court_line")


# ---------------------------------------------------------------------------
# Badminton net post ("stitch") — dark pole with a tensioning cleat/wheel.
# ---------------------------------------------------------------------------

def tex_badminton_net_post():
    base = (42, 44, 48)
    im = new_canvas(base)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.22)
    d = ImageDraw.Draw(im)
    # Tensioning wheel/cleat near the top — the recognizable "net post" cue.
    d.ellipse((3, 1, 12, 7), outline=(198, 200, 204), width=1)
    d.line([(7.5, 1), (7.5, 7)], fill=(198, 200, 204), width=1)
    d.point((7, 4), fill=(230, 200, 60))
    inset_edges(im, 0, 8, W - 1, H - 1, scale(base, 1.55), scale(base, 0.6))
    border(im, (16, 17, 19))
    save(im, "badminton_net_post")


# ---------------------------------------------------------------------------
# Badminton net mesh — diamond lattice with transparent gaps + top tape band
# (the large flat faces), plus a plain opaque rope trim for the thin edges.
# ---------------------------------------------------------------------------

def tex_badminton_net_mesh():
    im = new_canvas_rgba()
    cord = (244, 244, 238, 255)
    cord_dim = (214, 214, 206, 235)
    d = ImageDraw.Draw(im)
    # Diagonal diamond lattice: two crossing families of parallel lines.
    for offset in range(-16, 32, 4):
        d.line([(offset, 3), (offset + 13, 16)], fill=cord, width=1)
        d.line([(offset, 16), (offset + 13, 3)], fill=cord_dim, width=1)
    # Solid top tape band — real nets carry a marker strip along the top edge.
    rect(im, 0, 0, W - 1, 2, (250, 250, 246, 255))
    rect(im, 0, 2, W - 1, 2, (200, 200, 194, 255))
    save_raw(im, "badminton_net_mesh")


def tex_badminton_net_mesh_edge():
    base = (232, 232, 226)
    im = new_canvas(base)
    gradient_shade(im, 0, 0, W - 1, H - 1, base, light=1.15, dark=0.9)
    vertical_brush(im, 0, 0, 15, 15, base, stripe=1.08)
    border(im, (168, 168, 162))
    save(im, "badminton_net_mesh_edge")


ALL = [
    tex_court_line,
    tex_badminton_net_post,
    tex_badminton_net_mesh,
    tex_badminton_net_mesh_edge,
]

if __name__ == "__main__":
    for fn in ALL:
        fn()
    print(f"Wrote {len(ALL)} texture(s) to {OUT}")
