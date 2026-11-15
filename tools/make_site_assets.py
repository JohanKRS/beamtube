#!/usr/bin/env python3
"""Generates the brand art, fonts and screenshots of the project site (site/).

The site is plain HTML, CSS and JS with no build step: this script is only needed when the brand,
the fonts or the screenshots change. Everything it writes is committed.

  - site/assets/brand/hero-back.svg, hero-slit.svg, hero-word.svg: the logo of the hero in three
    layers (the glow and the beam / the slit / the name), so the page can light them up one after
    the other and draw the moving dust (a canvas) between the beam and the name. They share one
    viewBox, HERO_VIEWBOX, which site.js also uses to place the dust.
  - site/assets/brand/logo.svg: the whole logo, still, for the header.
  - site/assets/brand/icon-<color>.svg: the app icon in each of the seven colors (option C).
  - site/favicon.svg, site/apple-touch-icon.png, site/icon-192.png.
  - site/og-image.png: the picture a link to the site shows in chats and social networks.
  - site/assets/fonts/outfit-*.woff2: Outfit, cut down to the Latin letters the three languages use.
  - site/assets/img/*.webp: the screenshots from docs/screenshots that index.html uses.

The drawing itself comes from tools/make_icons.py, so the site always matches the app.

Usage: python3 tools/make_site_assets.py
Needs fonttools, uharfbuzz, brotli, cairosvg and Pillow.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_icons as mi  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
BRAND = SITE / "assets/brand"
FONTS = SITE / "assets/fonts"
IMG = SITE / "assets/img"

# The hero logo, in the units of the logo study (make_icons.lockup): the glow starts at x 108,
# the name ends near 603 and the beam fades out at 700.
HERO_VIEWBOX = (100, 30, 600, 240)
# The header and footer logo: the whole glow, the name and a little of the beam past it, with room
# above and below for the light to fade out.
LOGO_VIEWBOX = (104, 52, 538, 196)

# The link preview: the size Facebook, X, WhatsApp, Telegram and Discord all show in full.
OG_SIZE = (1200, 630)

# Light (300) for the name's "tube" and big text, Regular for running text, Medium and SemiBold for
# labels and headings. Bold is only in the logo, which is drawn as paths.
FONT_WEIGHTS = {"light": 300, "regular": 400, "medium": 500, "semibold": 600}
# Basic Latin, Latin-1 (ã, ç, é, ñ, ¿, ¡...), and the punctuation the copy uses (’ “ ” – · … →).
UNICODES = "U+0020-007E,U+00A0-00FF,U+2013-2014,U+2018-201F,U+2022,U+2026,U+2192,U+2190,U+00B7"


def svg(viewbox, defs, body, extra=""):
    x, y, w, h = viewbox
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{mi.num(x)} {mi.num(y)} {mi.num(w)} {mi.num(h)}"{extra}>'
            f'<defs>{"".join(defs)}</defs>{body}</svg>\n')


class Raw:
    """SVG written by hand, for what make_icons.Shape can't say (a blur)."""

    def __init__(self, defs, body):
        self.defs, self.body = defs, body


def soft_beam(light, glow, blur=True):
    cone = mi.mix(light, glow, 0.5)
    x0, x1, top0, bottom0, top1, bottom1 = mi.LOCKUP_BEAM
    # The layers end between x1 - 36 and x1; the light fades to nothing over that stretch.
    end = x1 - 18
    d = mi.polygon([(x0, top0), (x1, top1), (x1, bottom1), (x0, bottom0)])
    defs = [
        f'<linearGradient id="beam" gradientUnits="userSpaceOnUse" x1="{x0}" y1="0" x2="{end}" y2="0">'
        f'<stop offset="0" stop-color="{cone}" stop-opacity="0.38"/>'
        f'<stop offset="0.45" stop-color="{cone}" stop-opacity="0.17"/>'
        f'<stop offset="1" stop-color="{cone}" stop-opacity="0"/></linearGradient>',
        f'<filter id="soft" x="-5%" y="-15%" width="110%" height="130%"><feGaussianBlur stdDeviation="{mi.LOCKUP_BEAM_SOFT * 0.8:g}"/></filter>',
    ]
    if not blur:
        return Raw(defs[:1], f'<path d="{d}" fill="url(#beam)"/>')
    return Raw(defs, f'<path d="{d}" fill="url(#beam)" filter="url(#soft)"/>')


def shapes_svg(shapes, defs, prefix):
    """make_icons.svg_shapes, plus the hand-written parts."""
    out = ""
    for i, s in enumerate(shapes):
        if isinstance(s, Raw):
            defs.extend(s.defs)
            out += s.body
        else:
            out += mi.svg_shapes([s], defs, f"{prefix}{i}_")
    return out


def hero():
    top, bottom, diagonal, light, glow, letters = mi.PALETTE["night"]
    shapes, _ = mi.lockup(light, glow, letters, flat=False)
    # lockup(): the glow, five beam layers, the dust, the slit, its core, then "beam" and "tube".
    # The app draws the soft edge of the beam with five layers, because an Android vector has no
    # blur. In a browser those layers show as lines along the edges and where each one ends, so
    # the site draws one beam with the same light, blurred by the browser.
    back = shapes[:1] + [soft_beam(light, glow)]
    slit = shapes[-4:-2]
    word = shapes[-2:]
    for name, part in (("hero-back", back), ("hero-slit", slit), ("hero-word", word)):
        defs = []
        body = shapes_svg(part, defs, name[5])
        (BRAND / f"{name}.svg").write_text(svg(HERO_VIEWBOX, defs, body, ' aria-hidden="true"'))
    # The header logo: everything, with the dust standing still, cut closer around the name so it
    # reads at header size. The light fades out before every edge of the cut (the beam widens past
    # the top and the bottom on the right) instead of ending in a hard line.
    # Only the sharp grains: the soft ones would add weight to the file and not show.
    dots = [s for s in shapes[6:-4] if s.fill[0] == "solid"]
    x, y, w, h = LOGO_VIEWBOX
    edge = 26
    defs = [f'<linearGradient id="fx" gradientUnits="userSpaceOnUse" x1="{x}" y1="0" x2="{x + w}" y2="0">'
            f'<stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset="{edge / w:.3f}" stop-color="#fff"/>'
            f'<stop offset="{1 - 150 / w:.3f}" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>',
            f'<linearGradient id="fy" gradientUnits="userSpaceOnUse" x1="0" y1="{y}" x2="0" y2="{y + h}">'
            f'<stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset="{edge / h:.3f}" stop-color="#fff"/>'
            f'<stop offset="{1 - edge / h:.3f}" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>',
            f'<mask id="mx" maskUnits="userSpaceOnUse" x="{x}" y="{y}" width="{w}" height="{h}">'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="url(#fx)"/></mask>',
            f'<mask id="my" maskUnits="userSpaceOnUse" x="{x}" y="{y}" width="{w}" height="{h}">'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="url(#fy)"/></mask>']
    light_part = shapes_svg(back + dots, defs, "l")
    body = f'<g mask="url(#my)"><g mask="url(#mx)">{light_part}</g></g>' + mi.svg_shapes(slit + word, defs, "n")
    (BRAND / "logo.svg").write_text(svg(LOGO_VIEWBOX, defs, body, ' role="img" aria-label="BeamTube"'))


def og_image():
    """The hero logo on the night blue, 1200 x 630, for link previews.

    cairosvg has no blur, so the glow and the beam are drawn sharp and blurred here with Pillow (on
    premultiplied colors, so the edges don't go dark), then the dust, the slit and the name go on top.
    """
    import io
    import cairosvg
    from PIL import Image, ImageFilter

    top, bottom, diagonal, light, glow, letters = mi.PALETTE["night"]
    shapes, _ = mi.lockup(light, glow, letters, flat=False)
    width, height = OG_SIZE
    # Centered between the slit and the end of the name, like the hero and the TV banner.
    middle_x, middle_y = (mi.LOCKUP_SLIT[0] + 603) / 2, mi.LOCKUP_MIDDLE
    units = 700
    box = (middle_x - units / 2, middle_y - units * height / width / 2, units, units * height / width)
    scale = width / units

    def render(part, background=""):
        defs = []
        body = background + shapes_svg(part, defs, "o")
        png = cairosvg.svg2png(bytestring=svg(box, defs, body).encode(), output_width=width, output_height=height)
        return Image.open(io.BytesIO(png)).convert("RGBA")

    x, y, w, h = box
    sky = render([], f'<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1b1f3b"/>'
                     f'<stop offset="1" stop-color="#161931"/></linearGradient>'
                     f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="url(#sky)"/>')
    beam = render(shapes[:1] + [soft_beam(light, glow, blur=False)])
    beam = beam.convert("RGBa").filter(ImageFilter.GaussianBlur(mi.LOCKUP_BEAM_SOFT * 0.8 * scale)).convert("RGBA")
    image = Image.alpha_composite(sky, beam)
    image = Image.alpha_composite(image, render(shapes[6:-4]))
    image = Image.alpha_composite(image, render(shapes[-4:]))
    image.convert("RGB").save(SITE / "og-image.png", optimize=True)


def icons():
    import cairosvg

    for key, (top, bottom, diagonal, light, glow, _) in mi.PALETTE.items():
        defs = []
        bg = mi.svg_shapes([mi.background(top, bottom, diagonal, 108, 108)], defs, "b")
        fg = mi.svg_shapes(mi.mark(light, glow), defs, "f")
        defs.append('<clipPath id="m"><rect x="18" y="18" width="72" height="72" rx="20"/></clipPath>')
        text = svg((18, 18, 72, 72), defs, f'<g clip-path="url(#m)">{bg}{fg}</g>')
        (BRAND / f"icon-{key}.svg").write_text(text)
        if key == "night":
            (SITE / "favicon.svg").write_text(text)
            # The home screen icon of iOS and Android masks the corners by itself: a full square.
            full = svg((18, 18, 72, 72), defs[:-1], f"{bg}{fg}")
            cairosvg.svg2png(bytestring=full.encode(), write_to=str(SITE / "apple-touch-icon.png"), output_width=180, output_height=180)
            cairosvg.svg2png(bytestring=text.encode(), write_to=str(SITE / "icon-192.png"), output_width=192, output_height=192)


def fonts():
    from fontTools import subset

    for name, _ in FONT_WEIGHTS.items():
        source = mi.RES / f"font/outfit_{name}.ttf"
        options = subset.Options()
        options.flavor = "woff2"
        options.layout_features = ["kern", "liga", "calt", "tnum"]
        options.name_IDs = ["*"]
        font = subset.load_font(str(source), options)
        subsetter = subset.Subsetter(options)
        subsetter.populate(unicodes=subset.parse_unicodes(UNICODES))
        subsetter.subset(font)
        subset.save_font(font, str(FONTS / f"outfit-{name}.woff2"), options)
    (FONTS / "OFL.txt").write_text((ROOT / "licenses/Outfit-OFL.txt").read_text())


def screenshots():
    """Only the screenshots the page uses: a new one goes into index.html first."""
    import re
    from PIL import Image

    used = set(re.findall(r"assets/img/([\w-]+)\.webp", (SITE / "index.html").read_text()))
    for old in IMG.glob("*.webp"):
        if old.stem not in used:
            old.unlink()
    for name in sorted(used):
        image = Image.open(ROOT / f"docs/screenshots/{name}.jpg").convert("RGB")
        image.save(IMG / f"{name}.webp", "WEBP", quality=82, method=6)


def main():
    for folder in (BRAND, FONTS, IMG):
        folder.mkdir(parents=True, exist_ok=True)
    hero()
    og_image()
    icons()
    fonts()
    screenshots()
    print("viewBox", HERO_VIEWBOX)


if __name__ == "__main__":
    main()
