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
# The header and footer logo: from just left of the glow's bright part to a little past the name.
LOGO_VIEWBOX = (122, 62, 520, 176)

# Light (300) for the name's "tube" and big text, Regular for running text, Medium and SemiBold for
# labels and headings. Bold is only in the logo, which is drawn as paths.
FONT_WEIGHTS = {"light": 300, "regular": 400, "medium": 500, "semibold": 600}
# Basic Latin, Latin-1 (ã, ç, é, ñ, ¿, ¡...), and the punctuation the copy uses (’ “ ” – · … →).
UNICODES = "U+0020-007E,U+00A0-00FF,U+2013-2014,U+2018-201F,U+2022,U+2026,U+2192,U+2190,U+00B7"


def svg(viewbox, defs, body, extra=""):
    x, y, w, h = viewbox
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{mi.num(x)} {mi.num(y)} {mi.num(w)} {mi.num(h)}"{extra}>'
            f'<defs>{"".join(defs)}</defs>{body}</svg>\n')


def hero():
    top, bottom, diagonal, light, glow, letters = mi.PALETTE["night"]
    shapes, _ = mi.lockup(light, glow, letters, flat=False)
    # lockup(): the glow, five beam layers, the dust, the slit, its core, then "beam" and "tube".
    back = shapes[:6]
    slit = shapes[-4:-2]
    word = shapes[-2:]
    for name, part in (("hero-back", back), ("hero-slit", slit), ("hero-word", word)):
        defs = []
        body = mi.svg_shapes(part, defs, name[5])
        (BRAND / f"{name}.svg").write_text(svg(HERO_VIEWBOX, defs, body, ' aria-hidden="true"'))
    # The header logo: everything, with the dust standing still, cut closer around the name so it
    # reads at header size. The beam fades out before the cut instead of ending in a hard edge.
    # Only the sharp grains: the soft ones would add weight to the file and not show.
    dots = [s for s in shapes[6:-4] if s.fill[0] == "solid"]
    x, y, w, h = LOGO_VIEWBOX
    defs = [f'<linearGradient id="fade" gradientUnits="userSpaceOnUse" x1="{x + w - 80}" y1="0" x2="{x + w}" y2="0">'
            '<stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>',
            f'<mask id="m" maskUnits="userSpaceOnUse" x="{x}" y="{y}" width="{w}" height="{h}">'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="url(#fade)"/></mask>']
    light_part = mi.svg_shapes(back + dots, defs, "l")
    body = f'<g mask="url(#m)">{light_part}</g>' + mi.svg_shapes(slit + word, defs, "n")
    (BRAND / "logo.svg").write_text(svg(LOGO_VIEWBOX, defs, body, ' role="img" aria-label="BeamTube"'))


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
    icons()
    fonts()
    screenshots()
    print("viewBox", HERO_VIEWBOX)


if __name__ == "__main__":
    main()
