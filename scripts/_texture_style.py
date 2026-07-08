"""Shared visual-identity module for every BerongSMP texture generator.

See docs/texture_design_system.md for the full design rationale. This module
is the single source of truth for the mod-wide "signature core": every
generate_*_textures.py script should import apply_signature(im) and call it
as the very last step before saving a texture, so every PNG in the mod goes
through the same corner-chamfer + rim-light + outline-color finishing pass.

Deliberately dependency-free beyond Pillow so every existing script can adopt
it with a one-line import.
"""
from PIL import Image

W = H = 16

# A warm near-black, never pure (0,0,0), used for every outline in the mod.
SIGNATURE_OUTLINE = (18, 14, 12)

# The one universal accent color: the corner maker's-mark and every
# "highlight glint" pixel mod-wide (specular hotspots, screw heads, lens
# glints). Semantic hazard colors (red LED, blue spark, orange ember) are
# unaffected — this is the shared identity color, not a hazard-state color.
SIGNATURE_ACCENT = (255, 177, 0)

LIGHT_FACTOR = 1.45
SHADOW_FACTOR = 0.62


def _scale(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3])


def ramp(base):
    """5-tone value ramp from one base color: deep shadow -> shadow -> base ->
    highlight -> specular hotspot."""
    return [
        _scale(base, 0.42),
        _scale(base, SHADOW_FACTOR),
        base,
        _scale(base, LIGHT_FACTOR),
        _scale(base, 1.85),
    ]


def _get(im, x, y):
    if 0 <= x < im.width and 0 <= y < im.height:
        return im.getpixel((x, y))
    return None


def _put(im, x, y, color):
    if 0 <= x < im.width and 0 <= y < im.height:
        if im.mode == "RGBA" and len(color) == 3:
            color = (*color, 255)
        im.putpixel((x, y), color)


def chamfer_corners(im):
    """Clips the 4 corner pixels to transparent, breaking the perfect-square
    silhouette every flat vanilla-style icon has. The signature core's
    silhouette rule (docs/texture_design_system.md, section 3)."""
    if im.mode != "RGBA":
        return
    for (x, y) in ((0, 0), (im.width - 1, 0), (0, im.height - 1), (im.width - 1, im.height - 1)):
        _put(im, x, y, (0, 0, 0, 0))


def signature_mark(im):
    """Places the one-pixel amber maker's-mark just inside the chamfered
    top-left corner, at partial alpha so it reads as a glint, not a sticker."""
    if im.mode != "RGBA":
        return
    existing = _get(im, 1, 1)
    if existing is None or existing[3] == 0:
        return
    _put(im, 1, 1, (*SIGNATURE_ACCENT, 140))


def rim_light(im, x0=0, y0=0, x1=None, y1=None, factor=1.25):
    """Brightens the strict top and left silhouette edge of the opaque
    pixels within the given box — the mod-wide rim-light rule that separates
    this style from flat vanilla texture-painting."""
    x1 = im.width - 1 if x1 is None else x1
    y1 = im.height - 1 if y1 is None else y1
    for x in range(x0, x1 + 1):
        px = _get(im, x, y0)
        if px and (len(px) < 4 or px[3] > 0):
            _put(im, x, y0, _scale(px, factor))
    for y in range(y0, y1 + 1):
        px = _get(im, x0, y)
        if px and (len(px) < 4 or px[3] > 0):
            _put(im, x0, y, _scale(px, factor))


def apply_icon_signature(im):
    """Full signature treatment for a *flat item icon* texture — one where
    the entire 16x16 (or 32x32 armor-icon) image is the whole visible icon
    (item/generated model), so real alpha transparency at the corners is
    safe and reads as the intended chamfer. Use for handheld items
    (extinguishers, megaphone, whistle, flashlight, fire_blanket,
    first_aid_kit, hazard_wand, firefighter armor icons)."""
    rim_light(im)
    chamfer_corners(im)
    signature_mark(im)
    return im


def apply_material_signature(im):
    """Safe signature treatment for a *block material swatch* texture —
    one sampled via arbitrary per-face UV sub-rects across many block
    models, where punching real alpha holes in the corners would render as
    literal black artifacts under Minecraft's solid render type. Only
    applies the rim-light contrast rule (§2 of the design system); never
    touches alpha. Use for every generate_hazard/furniture/school_textures.py
    block-facing texture."""
    rim_light(im)
    return im


# Backwards-compatible alias; new scripts should call the explicit variant.
apply_signature = apply_icon_signature
