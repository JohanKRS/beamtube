#!/usr/bin/env python3
"""Gera o banner da Android TV (320x180dp, xhdpi = 640x360px) com o ícone e o nome do app.

Uso: python3 tools/make_tv_banner.py [nome] [caminho da fonte .otf/.ttf]
Requer Pillow. Cores iguais às de app/src/main/res/values/launcher.xml.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter

NAME = sys.argv[1] if len(sys.argv) > 1 else "Doorglow"
FONT = sys.argv[2] if len(sys.argv) > 2 else "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-xhdpi/tv_banner.png"

W, H = 640, 360
TOP, BOTTOM = (0x1B, 0x1F, 0x3B), (0x2A, 0x30, 0x60)
LIGHT, GLOW = (0xFF, 0xE3, 0xA3), (0xFF, 0xC8, 0x61)

img = Image.new("RGBA", (W, H))
draw = ImageDraw.Draw(img)
for y in range(H):
    t = y / (H - 1)
    draw.line([(0, y), (W, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(TOP, BOTTOM)) + (255,))

# Ícone no grid de 108 (o mesmo do ic_launcher_foreground), escalado e posicionado à esquerda.
s = 3.2
ox, oy = 40 - 32 * s, H / 2 - 58 * s


def p(x, y):
    return ox + x * s, oy + y * s


def layer(draw_fn, alpha, blur=0):
    over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(over), alpha)
    if blur:
        over = over.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(over)


layer(lambda d, a: d.polygon([p(50, 78), p(58, 78), p(76, 92), p(32, 92)], fill=LIGHT + (a,)), round(255 * 0.14))
layer(lambda d, a: d.rounded_rectangle([p(42, 25), p(66, 83)], radius=11 * s, fill=GLOW + (a,)), round(255 * 0.07), 2)
layer(lambda d, a: d.rounded_rectangle([p(46, 28), p(62, 80)], radius=7 * s, fill=GLOW + (a,)), round(255 * 0.13), 1)
layer(lambda d, a: d.rounded_rectangle([p(50, 30), p(58, 78)], radius=2 * s, fill=LIGHT + (a,)), 255)

font = ImageFont.truetype(FONT, 76)
draw = ImageDraw.Draw(img)
x0 = p(76, 0)[0] + 24
box = draw.textbbox((0, 0), NAME, font=font)
draw.text((x0, (H - (box[3] - box[1])) / 2 - box[1]), NAME, font=font, fill=(0xF5, 0xF1, 0xE6, 255))

OUT.parent.mkdir(parents=True, exist_ok=True)
img.convert("RGB").save(OUT, optimize=True)
print(OUT)
