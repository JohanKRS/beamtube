#!/usr/bin/env python3
"""Gera os ícones do Doorglow, um por cor, para o seletor "App icon" das Configurações.

O desenho é sempre a porta entreaberta (a fresta de luz, o brilho em volta e a luz no chão);
só as cores mudam. Na 0.5.4 (pedido do Johan, 2026-11-03) o mesmo desenho ganhou uma execução
melhor: brilho em degradê no lugar das camadas chapadas, um miolo mais claro na fresta e a luz
no chão sumindo aos poucos, tudo dentro da área que os launchers não cortam. O banner da TV
virou vetor, com o nome desenhado pelas curvas da fonte, para ficar nítido em qualquer tamanho.

Para cada cor de PALETTE saem:
  - values/launcher.xml (as cores de todas);
  - drawable/ic_launcher_<cor>_background.xml (o fundo, em degradê);
  - drawable-v24/ic_launcher_<cor>_foreground.xml (a porta, com degradês: Android 7 ou mais);
  - drawable/ic_launcher_<cor>_foreground.xml (a mesma porta em camadas chapadas, para o Android 6);
  - mipmap-anydpi-v26/ic_launcher_<cor>.xml (ícone adaptativo) e mipmap-anydpi/ic_launcher_<cor>.xml (Android 6 e 7);
  - drawable-v24/tv_banner_<cor>.xml e drawable/tv_banner_<cor>.xml (banner da TV, 320x180dp, vetor).
E também drawable/ic_launcher_monochrome.xml (ícones temáticos do Android 13+).

Cada cor tem um activity-alias no AndroidManifest.xml e uma entrada em ytslim.data.AppIcon.

Uso: python3 tools/make_icons.py [--preview PASTA]
Requer fonttools e uharfbuzz (o nome do banner); --preview também requer cairosvg.
"""
import argparse
from pathlib import Path

import uharfbuzz as hb
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
FONT = RES / "font/figtree_semibold.ttf"
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

# A porta, na grade de 108 do ícone adaptativo. O launcher mostra os 72 do meio e corta com a
# máscara (círculo, gota, quadrado arredondado): o que importa fica no círculo de raio 33 em volta
# do centro, e a luz no chão já chega a zero antes da borda, sem linha de corte.
SLIT = (51.0, 30.0, 57.0, 74.0, 1.0)  # a fresta: x0, y0, x1, y1, raio (quase reta, como a de uma porta)
CORE = (53.4, 33.0, 54.6, 71.0, 0.6)  # o miolo, mais claro
BLOOM = (54.0, 52.0, 18.0, 27.0)  # o brilho em volta: centro x, y, raio x, y
FLOOR = [(51.0, 74.0), (57.0, 74.0), (71.0, 84.0), (37.0, 84.0)]  # a luz no chão
FLOOR_TOP, FLOOR_BOTTOM = 74.0, 84.0

HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Gerado por tools/make_icons.py; não editar à mão. -->\n'


# ---- cores ------------------------------------------------------------------------------------

def rgb(hex_color):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def hexrgb(c):
    return "#" + "".join(f"{round(v):02X}" for v in c)


def mix(a, b, t):
    return hexrgb(tuple(x + (y - x) * t for x, y in zip(rgb(a), rgb(b))))


def argb(hex_color, alpha=1.0):
    return f"#{round(alpha * 255):02X}" + hex_color.lstrip("#").upper()


# ---- o desenho, como formas que viram vetor do Android ou SVG ------------------------------------

def rounded(x0, y0, x1, y1, r):
    f = lambda v: f"{v:g}"
    w, h = x1 - x0 - 2 * r, y1 - y0 - 2 * r
    return (f"M{f(x0 + r)},{f(y0)}h{f(w)}a{f(r)},{f(r)} 0,0 1,{f(r)} {f(r)}v{f(h)}a{f(r)},{f(r)} 0,0 1,-{f(r)} {f(r)}"
            f"h-{f(w)}a{f(r)},{f(r)} 0,0 1,-{f(r)} -{f(r)}v-{f(h)}a{f(r)},{f(r)} 0,0 1,{f(r)} -{f(r)}z")


def polygon(points):
    return "M" + "L".join(f"{x:g},{y:g}" for x, y in points) + "Z"


def circle(cx, cy, r):
    return f"M{cx - r:g},{cy:g}a{r:g},{r:g} 0,1 0,{2 * r:g},0a{r:g},{r:g} 0,1 0,-{2 * r:g},0z"


class Shape:
    """Um caminho com preenchimento: ("solid", cor, alfa), ("linear", x1, y1, x2, y2, paradas) ou
    ("radial", cx, cy, r, paradas), em que paradas = [(posição, cor, alfa)]. [squash] achata o
    caminho na vertical em volta de (cx, cy), para o brilho redondo virar elipse."""

    def __init__(self, d, fill, squash=None):
        self.d, self.fill, self.squash = d, fill, squash


def ellipse(cx, cy, rx, ry, stops):
    return Shape(circle(cx, cy, rx), ("radial", cx, cy, rx, stops), squash=(cx, cy, ry / rx))


def door(light, glow, flat=False):
    """A porta entreaberta. [flat]: sem degradês (o Android 6 não desenha degradê em vetor)."""
    core = mix(light, "#FFFFFF", 0.6)
    cx, cy, rx, ry = BLOOM
    if flat:
        return [
            Shape(polygon(FLOOR), ("solid", light, 0.12)),
            Shape(rounded(cx - rx * 0.62, cy - ry * 0.95, cx + rx * 0.62, cy + ry * 0.82, rx * 0.6), ("solid", glow, 0.07)),
            Shape(rounded(cx - rx * 0.4, cy - ry * 0.86, cx + rx * 0.4, cy + ry * 0.8, rx * 0.36), ("solid", glow, 0.12)),
            Shape(rounded(*SLIT), ("solid", light, 1.0)),
            Shape(rounded(*CORE), ("solid", core, 0.9)),
        ]
    return [
        ellipse(cx, cy, rx, ry, [(0, glow, 0.55), (0.45, glow, 0.18), (1, glow, 0.0)]),
        Shape(polygon(FLOOR), ("linear", 54, FLOOR_TOP, 54, FLOOR_BOTTOM, [(0, light, 0.42), (0.5, light, 0.14), (1, light, 0.0)])),
        Shape(rounded(*SLIT), ("solid", light, 1.0)),
        Shape(rounded(*CORE), ("linear", 54, CORE[1], 54, CORE[3], [(0, core, 0.0), (0.25, core, 0.8), (0.75, core, 0.8), (1, core, 0.0)])),
    ]


def text_shape(size, x, baseline, color):
    """O nome com as curvas da fonte do app (Figtree semibold), com o kerning dela."""
    font = TTFont(FONT)
    upem = font["head"].unitsPerEm
    face = hb.Face(FONT.read_bytes())
    hbfont = hb.Font(face)
    buf = hb.Buffer()
    buf.add_str(NAME)
    buf.guess_segment_properties()
    hb.shape(hbfont, buf, {"kern": True, "liga": True})
    glyphs = font.getGlyphSet()
    order = font.getGlyphOrder()
    scale = size / upem
    pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
    pen_x = 0
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        name = order[info.codepoint]
        transform = (scale, 0, 0, -scale, x + (pen_x + pos.x_offset) * scale, baseline - pos.y_offset * scale)
        glyphs[name].draw(TransformPen(pen, transform))
        pen_x += pos.x_advance
    return Shape(pen.getCommands(), ("solid", color, 1.0)), pen_x * scale


# ---- saída: vetor do Android -------------------------------------------------------------------

def vector_fill(fill, indent):
    kind = fill[0]
    if kind == "solid":
        _, color, alpha = fill
        return f'{indent}android:fillColor="{argb(color, alpha)}"', ""
    stops = fill[-1]
    items = "".join(f'{indent}        <item android:color="{argb(c, a)}" android:offset="{o:g}" />\n' for o, c, a in stops)
    if kind == "linear":
        _, x1, y1, x2, y2, _ = fill
        geometry = (f'{indent}        android:startX="{x1:g}"\n{indent}        android:startY="{y1:g}"\n'
                    f'{indent}        android:endX="{x2:g}"\n{indent}        android:endY="{y2:g}"\n'
                    f'{indent}        android:type="linear"')
    else:
        _, cx, cy, r, _ = fill
        geometry = (f'{indent}        android:centerX="{cx:g}"\n{indent}        android:centerY="{cy:g}"\n'
                    f'{indent}        android:gradientRadius="{r:g}"\n{indent}        android:type="radial"')
    body = (f'{indent}<aapt:attr name="android:fillColor">\n{indent}    <gradient\n{geometry}>\n'
            f"{items}{indent}    </gradient>\n{indent}</aapt:attr>\n")
    return None, body


def vector_shapes(shapes, indent="    "):
    out = ""
    for s in shapes:
        inner = indent
        if s.squash:
            cx, cy, k = s.squash
            out += f'{indent}<group\n{indent}    android:pivotX="{cx:g}"\n{indent}    android:pivotY="{cy:g}"\n{indent}    android:scaleY="{k:.4f}">\n'
            inner = indent + "    "
        attr, body = vector_fill(s.fill, inner + "    ")
        if body:
            out += f'{inner}<path android:pathData="{s.d}">\n{body}{inner}</path>\n'
        else:
            out += f'{inner}<path\n{attr}\n{inner}    android:pathData="{s.d}" />\n'
        if s.squash:
            out += f"{indent}</group>\n"
    return out


def vector(width, height, viewport_w, viewport_h, body, uses_gradient):
    ns = ' xmlns:aapt="http://schemas.android.com/aapt"' if uses_gradient else ""
    return (HEADER + f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
            f'    android:width="{width}dp"\n    android:height="{height}dp"\n'
            f'    android:viewportWidth="{viewport_w:g}"\n    android:viewportHeight="{viewport_h:g}">\n' + body + "</vector>\n")


def group(body, tx, ty, s):
    lines = body.splitlines(keepends=True)
    inner = "".join("    " + line for line in lines)
    return (f'    <group\n        android:translateX="{tx:.3f}"\n        android:translateY="{ty:.3f}"\n'
            f'        android:scaleX="{s:.4f}"\n        android:scaleY="{s:.4f}">\n{inner}    </group>\n')


# ---- saída: SVG (só para conferir) ---------------------------------------------------------------

def svg_shapes(shapes, defs, prefix):
    out = ""
    for i, s in enumerate(shapes):
        kind = s.fill[0]
        if kind == "solid":
            _, color, alpha = s.fill
            paint = f'fill="{color}" fill-opacity="{alpha:g}"'
        else:
            gid = f"{prefix}{i}"
            stops = "".join(f'<stop offset="{o:g}" stop-color="{c}" stop-opacity="{a:g}"/>' for o, c, a in s.fill[-1])
            if kind == "linear":
                _, x1, y1, x2, y2, _ = s.fill
                defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{stops}</linearGradient>')
            else:
                _, cx, cy, r, _ = s.fill
                defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{r}">{stops}</radialGradient>')
            paint = f'fill="url(#{gid})"'
        transform = ""
        if s.squash:
            cx, cy, k = s.squash
            transform = f' transform="translate({cx} {cy}) scale(1 {k}) translate({-cx} {-cy})"'
        out += f'<path d="{s.d}" {paint}{transform}/>'
    return out


# ---- os arquivos -------------------------------------------------------------------------------

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


def gradient_xml(key, diagonal, indent="    "):
    # 270 = de cima para baixo; 315 = do canto de cima à esquerda para o de baixo à direita.
    angle = 315 if diagonal else 270
    i = indent
    return (f'{i}<gradient\n{i}    android:angle="{angle}"\n'
            f'{i}    android:endColor="@color/launcher_{key}_bottom"\n'
            f'{i}    android:startColor="@color/launcher_{key}_top" />\n')


def background(top, bottom, diagonal, w, h):
    if diagonal:
        fill = ("linear", 0, 0, w, h, [(0, top, 1), (1, bottom, 1)])
    else:
        fill = ("linear", 0, 0, 0, h, [(0, top, 1), (1, bottom, 1)])
    return Shape(f"M0,0h{w:g}v{h:g}h-{w:g}z", fill)


def icon(key, top, bottom, diagonal, light, glow):
    write(RES / f"drawable/ic_launcher_{key}_background.xml",
          HEADER + '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
          + gradient_xml(key, diagonal) + "</shape>\n")
    write(RES / f"drawable-v24/ic_launcher_{key}_foreground.xml", vector(108, 108, 108, 108, vector_shapes(door(light, glow)), True))
    write(RES / f"drawable/ic_launcher_{key}_foreground.xml", vector(108, 108, 108, 108, vector_shapes(door(light, glow, flat=True)), False))

    write(RES / f"mipmap-anydpi-v26/ic_launcher_{key}.xml",
          HEADER + '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
          f'    <background android:drawable="@drawable/ic_launcher_{key}_background" />\n'
          f'    <foreground android:drawable="@drawable/ic_launcher_{key}_foreground" />\n'
          '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')

    write(RES / f"mipmap-anydpi/ic_launcher_{key}.xml",
          HEADER + "<!-- Android 6 e 7 (sem ícone adaptativo): o mesmo desenho num quadrado arredondado. -->\n"
          '<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n    <item>\n'
          '        <shape android:shape="rectangle">\n'
          + gradient_xml(key, diagonal, indent="            ")
          + '            <corners android:radius="12dp" />\n        </shape>\n    </item>\n'
          f'    <item android:drawable="@drawable/ic_launcher_{key}_foreground" />\n</layer-list>\n')


def monochrome():
    # Uma cor só (o sistema pinta): a fresta inteira e a luz no chão pela metade.
    shapes = [Shape(polygon(FLOOR), ("solid", "#FFFFFF", 0.45)), Shape(rounded(*SLIT), ("solid", "#FFFFFF", 1.0))]
    write(RES / "drawable/ic_launcher_monochrome.xml", vector(108, 108, 108, 108, vector_shapes(shapes), False))


# O banner da TV: 320x180dp. A porta (com o brilho) e o nome, centralizados como um bloco.
BANNER_W, BANNER_H = 320, 180
DOOR_SCALE = 1.75  # grade de 108 -> dp
TEXT_SIZE = 48
GAP = 6  # da borda do brilho da porta ao começo do nome


def banner_parts(top, bottom, diagonal, light, glow, flat):
    text_probe, text_w = text_shape(TEXT_SIZE, 0, 0, TEXT)
    door_left = (BLOOM[0] - BLOOM[2] * 0.62) * DOOR_SCALE  # onde o brilho já está fraco
    door_w = (BLOOM[2] * 0.62) * 2 * DOOR_SCALE
    total = door_w + GAP + text_w
    x0 = (BANNER_W - total) / 2
    tx = x0 - door_left
    # Centralizada na altura pela parte que se vê: da fresta até o meio da luz no chão.
    door_mid = (SLIT[1] + (FLOOR_TOP + FLOOR_BOTTOM) / 2) / 2
    ty = BANNER_H / 2 - door_mid * DOOR_SCALE
    text_x = x0 + door_w + GAP
    # O nome alinhado pela altura das maiúsculas com o meio da fresta.
    font = TTFont(FONT)
    cap = font["OS/2"].sCapHeight / font["head"].unitsPerEm * TEXT_SIZE
    slit_mid = ty + (SLIT[1] + SLIT[3]) / 2 * DOOR_SCALE
    baseline = slit_mid + cap / 2
    text, _ = text_shape(TEXT_SIZE, text_x, baseline, TEXT)
    bg = background(top, bottom, diagonal, BANNER_W, BANNER_H)
    if flat:
        bg = Shape(bg.d, ("solid", mix(top, bottom, 0.5), 1.0))
    return bg, door(light, glow, flat), text, (tx, ty)


def tv_banner(key, top, bottom, diagonal, light, glow):
    for folder, flat in (("drawable-v24", False), ("drawable", True)):
        bg, door_shapes, text, (tx, ty) = banner_parts(top, bottom, diagonal, light, glow, flat)
        body = vector_shapes([bg]) + group(vector_shapes(door_shapes), tx, ty, DOOR_SCALE) + vector_shapes([text])
        write(RES / f"{folder}/tv_banner_{key}.xml", vector(BANNER_W, BANNER_H, BANNER_W, BANNER_H, body, not flat))
    old = RES / f"drawable-xhdpi/tv_banner_{key}.png"
    if old.exists():
        old.unlink()


def preview(folder):
    """Desenha o ícone (com as máscaras) e o banner em PNG, para conferir."""
    import cairosvg

    folder.mkdir(parents=True, exist_ok=True)
    for key, (top, bottom, diagonal, light, glow) in PALETTE.items():
        defs = []
        bg = svg_shapes([background(top, bottom, diagonal, 108, 108)], defs, "b")
        fg = svg_shapes(door(light, glow), defs, "f")
        masks = {
            "circle": '<circle cx="54" cy="54" r="36"/>',
            "squircle": '<rect x="18" y="18" width="72" height="72" rx="20"/>',
        }
        for mask_name, mask in masks.items():
            svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="432" height="432">'
                   f'<defs>{"".join(defs)}<clipPath id="m">{mask}</clipPath></defs>'
                   f'<g clip-path="url(#m)">{bg}{fg}</g></svg>')
            cairosvg.svg2png(bytestring=svg.encode(), write_to=str(folder / f"icon_{key}_{mask_name}.png"))
        defs = []
        bgs, door_shapes, text, (tx, ty) = banner_parts(top, bottom, diagonal, light, glow, False)
        body = (svg_shapes([bgs], defs, "B") + f'<g transform="translate({tx} {ty}) scale({DOOR_SCALE})">'
                + svg_shapes(door_shapes, defs, "D") + "</g>" + svg_shapes([text], defs, "T"))
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {BANNER_W} {BANNER_H}" width="{BANNER_W * 4}" height="{BANNER_H * 4}">'
               f'<defs>{"".join(defs)}</defs>{body}</svg>')
        cairosvg.svg2png(bytestring=svg.encode(), write_to=str(folder / f"banner_{key}.png"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path, help="só desenha PNGs nesta pasta, sem mexer no app")
    args = parser.parse_args()
    if args.preview:
        preview(args.preview)
        return
    colors()
    monochrome()
    for key, (top, bottom, diagonal, light, glow) in PALETTE.items():
        icon(key, top, bottom, diagonal, light, glow)
        tv_banner(key, top, bottom, diagonal, light, glow)
        print(key)


if __name__ == "__main__":
    main()
