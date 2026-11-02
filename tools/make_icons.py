#!/usr/bin/env python3
"""Gera os ícones do Doorglow, um por cor, para o seletor "App icon" das Configurações.

Para cada cor de PALETTE saem:
  - values/launcher.xml (as cores de todas);
  - drawable/ic_launcher_<cor>_background.xml e ic_launcher_<cor>_foreground.xml (grade de 108dp);
  - mipmap-anydpi-v26/ic_launcher_<cor>.xml (ícone adaptativo; a camada de uma cor é a mesma para todas);
  - mipmap-anydpi/ic_launcher_<cor>.xml (Android 6 e 7, sem ícone adaptativo);
  - drawable-xhdpi/tv_banner_<cor>.png (banner da TV, 320x180dp em xhdpi = 640x360px).

O desenho é sempre a porta original; só as cores mudam. Origem: versões 01 a 07 em
/mnt/project-files/nome-e-icone/versoes/. Cada cor tem um activity-alias no AndroidManifest.xml
e uma entrada em ytslim.data.AppIcon.

Uso: python3 tools/make_icons.py [caminho da fonte .otf/.ttf]   (requer Pillow)
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

FONT = sys.argv[1] if len(sys.argv) > 1 else "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"
RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
NAME = "Doorglow"
TEXT = "#F5F1E6"

# cor: (fundo em cima, fundo embaixo, degradê na diagonal, luz, brilho)
PALETTE = {
    "night": ("#1B1F3B", "#2A3060", False, "#FFE3A3", "#FFC861"),  # 01 original, o padrão
    "black": ("#050506", "#050506", False, "#F5F7FA", "#A9C1FF"),  # 02 oled
    "teal": ("#0B3D44", "#11606A", False, "#CFFFF0", "#7FF0CF"),  # 03
    "plum": ("#2B1638", "#4A2350", False, "#FFE0CC", "#FF9E7A"),  # 04 ameixa
    "mustard": ("#B7791F", "#8A5A12", False, "#FFF6DF", "#FFF6DF"),  # 05 mostarda
    "vivid": ("#FF6A5C", "#7B3FE4", True, "#FFFFFF", "#FFE6F0"),  # 06 vivo
    "red": ("#E53935", "#E53935", False, "#FFFFFF", "#FFFFFF"),  # 07 vermelho (pedido em 2026-11-02)
}

# A porta, na grade de 108: (forma, coordenadas, opacidade, cor) — igual aos SVGs.
FLOOR = [(50, 78), (58, 78), (76, 92), (32, 92)]
OUTER = (42, 25, 66, 83, 11)
INNER = (46, 28, 62, 80, 7)
SLIT = (50, 30, 58, 78, 2)

HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Gerado por tools/make_icons.py; não editar à mão. -->\n'


def argb(hex_color):
    return "#FF" + hex_color.lstrip("#").upper()


def rgb(hex_color):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def rounded(x0, y0, x1, y1, r):
    w, h = x1 - x0 - 2 * r, y1 - y0 - 2 * r
    return (f"M{x0 + r},{y0}h{w}a{r},{r} 0,0 1,{r} {r}v{h}a{r},{r} 0,0 1,-{r} {r}"
            f"h-{w}a{r},{r} 0,0 1,-{r} -{r}v-{h}a{r},{r} 0,0 1,{r} -{r}z")


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def colors():
    lines = [HEADER + "<resources>"]
    for key, (top, bottom, _, light, glow) in PALETTE.items():
        lines += [
            f'    <color name="launcher_{key}_top">{argb(top)}</color>',
            f'    <color name="launcher_{key}_bottom">{argb(bottom)}</color>',
            f'    <color name="launcher_{key}_light">{argb(light)}</color>',
            f'    <color name="launcher_{key}_glow">{argb(glow)}</color>',
        ]
    lines.append("</resources>\n")
    write(RES / "values/launcher.xml", "\n".join(lines))


def gradient(key, diagonal, indent="    "):
    # 270 = de cima para baixo; 315 = do canto de cima à esquerda para o de baixo à direita.
    angle = 315 if diagonal else 270
    i = indent
    return (f'{i}<gradient\n{i}    android:angle="{angle}"\n'
            f'{i}    android:endColor="@color/launcher_{key}_bottom"\n'
            f'{i}    android:startColor="@color/launcher_{key}_top" />\n')


def drawables(key, diagonal):
    write(RES / f"drawable/ic_launcher_{key}_background.xml",
          HEADER + '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
          + gradient(key, diagonal) + "</shape>\n")

    floor = "M" + "L".join(f"{x},{y}" for x, y in FLOOR) + "Z"
    paths = [
        ("luz no chão", floor, "0.14", "light"),
        ("brilho externo", rounded(*OUTER), "0.07", "glow"),
        ("brilho interno", rounded(*INNER), "0.13", "glow"),
        ("a fresta", rounded(*SLIT), None, "light"),
    ]
    body = ""
    for comment, data, alpha, color in paths:
        alpha_attr = f'\n        android:fillAlpha="{alpha}"' if alpha else ""
        body += (f"    <!-- {comment} -->\n    <path{alpha_attr}\n"
                 f'        android:fillColor="@color/launcher_{key}_{color}"\n'
                 f'        android:pathData="{data}" />\n')
    write(RES / f"drawable/ic_launcher_{key}_foreground.xml",
          HEADER + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    android:width="108dp"\n    android:height="108dp"\n'
          '    android:viewportWidth="108"\n    android:viewportHeight="108">\n' + body + "</vector>\n")

    write(RES / f"mipmap-anydpi-v26/ic_launcher_{key}.xml",
          HEADER + '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
          f'    <background android:drawable="@drawable/ic_launcher_{key}_background" />\n'
          f'    <foreground android:drawable="@drawable/ic_launcher_{key}_foreground" />\n'
          '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')

    write(RES / f"mipmap-anydpi/ic_launcher_{key}.xml",
          HEADER + "<!-- Android 6 e 7 (sem ícone adaptativo): o mesmo desenho num quadrado arredondado. -->\n"
          '<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n    <item>\n'
          '        <shape android:shape="rectangle">\n'
          + gradient(key, diagonal, indent="            ")
          + '            <corners android:radius="12dp" />\n        </shape>\n    </item>\n'
          f'    <item android:drawable="@drawable/ic_launcher_{key}_foreground" />\n</layer-list>\n')


def banner(key, top, bottom, diagonal, light, glow):
    w, h = 640, 360
    a, b = rgb(top), rgb(bottom)
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            # Na diagonal, o degradê vai do canto de cima à esquerda ao de baixo à direita.
            t = (x * w + y * h) / (w * w + h * h) if diagonal else y / (h - 1)
            px[x, y] = tuple(round(c0 + (c1 - c0) * t) for c0, c1 in zip(a, b)) + (255,)

    s = 3.2
    ox, oy = 40 - 32 * s, h / 2 - 58 * s

    def p(x, y):
        return ox + x * s, oy + y * s

    def layer(draw_fn, alpha, blur=0):
        over = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        draw_fn(ImageDraw.Draw(over), alpha)
        if blur:
            over = over.filter(ImageFilter.GaussianBlur(blur))
        img.alpha_composite(over)

    lc, gc = rgb(light), rgb(glow)
    layer(lambda d, al: d.polygon([p(*pt) for pt in FLOOR], fill=lc + (al,)), round(255 * 0.14))
    for (x0, y0, x1, y1, r), alpha, blur in ((OUTER, 0.07, 2), (INNER, 0.13, 1)):
        layer(lambda d, al, x0=x0, y0=y0, x1=x1, y1=y1, r=r:
              d.rounded_rectangle([p(x0, y0), p(x1, y1)], radius=r * s, fill=gc + (al,)), round(255 * alpha), blur)
    x0, y0, x1, y1, r = SLIT
    layer(lambda d, al: d.rounded_rectangle([p(x0, y0), p(x1, y1)], radius=r * s, fill=lc + (al,)), 255)

    font = ImageFont.truetype(FONT, 76)
    draw = ImageDraw.Draw(img)
    tx = p(76, 0)[0] + 24
    box = draw.textbbox((0, 0), NAME, font=font)
    draw.text((tx, (h - (box[3] - box[1])) / 2 - box[1]), NAME, font=font, fill=rgb(TEXT) + (255,))

    out = RES / f"drawable-xhdpi/tv_banner_{key}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(out, optimize=True)


colors()
for key, (top, bottom, diagonal, light, glow) in PALETTE.items():
    drawables(key, diagonal)
    banner(key, top, bottom, diagonal, light, glow)
    print(key)
