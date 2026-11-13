#!/usr/bin/env python3
"""Gera os ícones do BeamTube, um por cor, para o seletor "App icon" das Configurações.

O desenho é a marca aprovada pelo Johan em 2026-11-13: a fresta de luz e o facho que sai dela para
a direita, com a poeira passando na frente da luz ("como a poeirinha na frente do brilho da TV de
noite"). No ícone, a fresta fica no centro e o facho vai até a borda (a opção C dos estudos). No
banner da TV, o facho atravessa o nome, "beamtube" em dois pesos da Outfit, com as letras acesas do
lado da luz. Só as cores mudam de um ícone para o outro. A poeira sai de uma semente fixa: o
desenho é sempre o mesmo.

Para cada cor de PALETTE saem:
  - values/launcher.xml (as cores de todas);
  - drawable/ic_launcher_<cor>_background.xml (o fundo, em degradê);
  - drawable-v24/ic_launcher_<cor>_foreground.xml (a marca, com degradês: Android 7 ou mais);
  - drawable/ic_launcher_<cor>_foreground.xml (a mesma marca em camadas chapadas, para o Android 6);
  - mipmap-anydpi-v26/ic_launcher_<cor>.xml (ícone adaptativo) e mipmap-anydpi/ic_launcher_<cor>.xml (Android 6 e 7);
  - drawable-v24/tv_banner_<cor>.xml e drawable/tv_banner_<cor>.xml (banner da TV, 320x180dp, vetor).
E também drawable/ic_launcher_monochrome.xml (ícones temáticos do Android 13+) e
drawable/ic_notification.xml (o ícone pequeno da notificação do player com a tela desligada).

Cada cor tem um activity-alias no AndroidManifest.xml e uma entrada em ytslim.data.AppIcon.

Uso: python3 tools/make_icons.py [--preview PASTA]
Requer fonttools e uharfbuzz (o nome do banner); --preview também requer cairosvg.
"""
import argparse
import random
from pathlib import Path

import uharfbuzz as hb
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
# O nome, em dois pesos da fonte do app: "beam" em bold e "tube" em light.
NAME = (("beam", RES / "font/outfit_bold.ttf"), ("tube", RES / "font/outfit_light.ttf"))

# cor: (fundo em cima, fundo embaixo, degradê na diagonal, luz, brilho, letras)
# letras: o degradê do nome no banner, do lado da luz ao fim da palavra (começo, meio, fim).
PALETTE = {
    "night": ("#1B1F3B", "#2A3060", False, "#FFE3A3", "#FFC861", ("#FFEFC2", "#FBF2DC", "#BFC3E4")),  # 01 original, o padrão
    "black": ("#050506", "#050506", False, "#F5F7FA", "#A9C1FF", ("#FFFFFF", "#F5F7FA", "#AEB4C6")),  # 02 oled
    "teal": ("#0B3D44", "#11606A", False, "#CFFFF0", "#7FF0CF", ("#E9FFF9", "#F2FFFB", "#A9D6D2")),  # 03
    "plum": ("#2B1638", "#4A2350", False, "#FFE0CC", "#FF9E7A", ("#FFE6D6", "#FFF1E8", "#CDB6D6")),  # 04 ameixa
    "mustard": ("#B7791F", "#8A5A12", False, "#FFF6DF", "#FFF6DF", ("#FFFFFF", "#FFF9E8", "#FFE9B8")),  # 05 mostarda
    "vivid": ("#FF6A5C", "#7B3FE4", True, "#FFFFFF", "#FFE6F0", ("#FFFFFF", "#FFFFFF", "#F3DDFF")),  # 06 vivo
    "red": ("#E53935", "#E53935", False, "#FFFFFF", "#FFFFFF", ("#FFFFFF", "#FFFFFF", "#FFDEDB")),  # 07 vermelho (pedido em 2026-11-02)
}

# A marca, na grade de 108 do ícone adaptativo. O launcher mostra os 72 do meio e corta com a
# máscara (círculo, gota, quadrado arredondado). A fresta fica no centro, e o facho passa da borda
# direita da área visível (até o fim da grade), para chegar à borda em qualquer recorte.
SLIT = (51.0, 32.0, 57.0, 76.0, 1.0)  # a fresta: x0, y0, x1, y1, raio
CORE = (53.4, 35.0, 54.6, 73.0, 0.6)  # o miolo, mais claro
BLOOM = (54.0, 54.0, 15.6, 27.3)  # o brilho em volta: centro x, y, raio x, y
BEAM_END = 108.0  # até onde o facho vai
BEAM_SPREAD = 0.2  # quanto ele abre, para cima e para baixo, a cada unidade para a direita
BEAM_NEAR, BEAM_FAR = 0.42, 0.20  # a força da luz junto da fresta e na borda visível (x = 90)
# O facho em camadas, da mais larga à mais estreita, para a borda sair suave sem desfoque: quanto
# da luz fica em cada uma. Poucas camadas, e cada uma com o degradê terminando num ponto um pouco
# diferente: muitas camadas fracas iguais somam os degraus de cor nos mesmos lugares, e o facho
# aparece em faixas. Com menos de cinco, a borda aparece em degraus.
LAYERS = (0.14, 0.17, 0.2, 0.23, 0.26)

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

def num(v):
    """Um número para o vetor, como o {:g}, mas nunca em notação científica nem "-0". O Android recusa
    o desenho inteiro por um número só: o "--1.15463e-14" que a conta da largura da fresta dava (1.0.1)
    deixou o banner da TV e o ícone sem abrir fora do app, e o launcher ficava sem o cartão."""
    text = f"{v:g}"
    if "e" in text:
        text = f"{v:.6f}".rstrip("0").rstrip(".")
    return "0" if text in ("-0", "") else text


def rounded(x0, y0, x1, y1, r):
    f = num
    # A fresta tem a largura do dobro do raio: a conta dá um resto como -1e-14, que vira 0.
    w, h = max(0.0, x1 - x0 - 2 * r), max(0.0, y1 - y0 - 2 * r)
    return (f"M{f(x0 + r)},{f(y0)}h{f(w)}a{f(r)},{f(r)} 0,0 1,{f(r)} {f(r)}v{f(h)}a{f(r)},{f(r)} 0,0 1,{f(-r)} {f(r)}"
            f"h{f(-w)}a{f(r)},{f(r)} 0,0 1,{f(-r)} {f(-r)}v{f(-h)}a{f(r)},{f(r)} 0,0 1,{f(r)} {f(-r)}z")


def polygon(points):
    return "M" + "L".join(f"{num(x)},{num(y)}" for x, y in points) + "Z"


def circle(cx, cy, r):
    return f"M{num(cx - r)},{num(cy)}a{num(r)},{num(r)} 0,1 0,{num(2 * r)},0a{num(r)},{num(r)} 0,1 0,{num(-2 * r)},0z"


class Shape:
    """Um caminho com preenchimento: ("solid", cor, alfa), ("linear", x1, y1, x2, y2, paradas) ou
    ("radial", cx, cy, r, paradas), em que paradas = [(posição, cor, alfa)]. [squash] achata o
    caminho na vertical em volta de (cx, cy), para o brilho redondo virar elipse."""

    def __init__(self, d, fill, squash=None):
        self.d, self.fill, self.squash = d, fill, squash


def ellipse(cx, cy, rx, ry, stops):
    return Shape(circle(cx, cy, rx), ("radial", cx, cy, rx, stops), squash=(cx, cy, ry / rx))


def layer_alpha(alpha, share):
    """A opacidade de uma camada do facho que leva [share] da luz, para todas somadas darem [alpha] no miolo."""
    return 1 - (1 - alpha) ** share


def speck_shapes(items, dust, flat):
    """Os grãos de poeira: (tipo, x, y, raio, opacidade). "dot" é nítido; "soft", fora de foco."""
    shapes = []
    for kind, x, y, r, o in items:
        if kind == "dot":
            shapes.append(Shape(circle(x, y, r), ("solid", dust, o)))
        elif flat:
            shapes.append(Shape(circle(x, y, r * 0.55), ("solid", dust, o * 0.5)))
        else:
            shapes.append(Shape(circle(x, y, r), ("radial", x, y, r, [(0, dust, o), (0.35, dust, o * 0.6), (1, dust, 0.0)])))
    return shapes


def icon_specks():
    """A poeira do ícone: poucos grãos, dentro do facho, em grupinhos e sem encostar um no outro."""
    rng = random.Random(13)
    count, unit = 14, 0.8
    x0, x1 = SLIT[2] + 0.5, 91.0
    cy = (SLIT[1] + SLIT[3]) / 2
    half0 = (SLIT[3] - SLIT[1]) / 2 - 1.0
    top = lambda x: cy - (half0 + (x - SLIT[2]) * BEAM_SPREAD) + 1.0
    bottom = lambda x: cy + (half0 + (x - SLIT[2]) * BEAM_SPREAD) - 1.0
    out = []
    span = x1 - x0
    centers = [(x0 + span * rng.uniform(0.1, 0.8), rng.uniform(0.25, 0.75)) for _ in range(3)]
    tries = 0
    while len(out) < count and tries < count * 60:
        tries += 1
        if rng.random() < 0.5:
            cx, cv = rng.choice(centers)
            x = rng.gauss(cx, span * 0.07)
            v = cv + rng.gauss(0, 0.16)
        else:
            x = x0 + span * rng.random() ** 1.15
            v = rng.random()
        if not (x0 + 2 * unit <= x <= x1) or not (0.06 <= v <= 0.94):
            continue
        y = top(x) + (bottom(x) - top(x)) * v
        if any((x - px) ** 2 + (y - py) ** 2 < (2.2 * unit) ** 2 for _, px, py, _, _ in out):
            continue
        t = (x - x0) / span
        fall = max(0.35, 1 - 0.6 * t)
        fall *= min(1.0, 0.5 + min(v, 1 - v) * 4)
        k = rng.random()
        if k < 0.5:
            o = rng.choice([0.45, 0.6, 0.8, 1.0]) * rng.uniform(0.85, 1.0)
            out.append(("dot", x, y, rng.uniform(0.55, 1.25) * unit, min(1, o * fall * 1.1)))
        elif k < 0.86:
            out.append(("soft", x, y, rng.uniform(2.0, 3.8) * unit, rng.uniform(0.45, 0.8) * fall))
        else:
            out.append(("soft", x, y, rng.uniform(5, 8.5) * unit, rng.uniform(0.18, 0.3) * fall))
    return out


def mark(light, glow, flat=False):
    """A marca do ícone: o brilho, o facho com a poeira, a fresta e o miolo dela.
    [flat]: sem degradês (o Android 6 não desenha degradê em vetor)."""
    core = mix(light, "#FFFFFF", 0.6)
    cone = mix(light, glow, 0.5)
    dust = mix(light, "#FFFFFF", 0.5)
    cx, cy, rx, ry = BLOOM
    x0 = SLIT[2]
    half0 = (SLIT[3] - SLIT[1]) / 2 - 1.0
    half1 = half0 + (BEAM_END - x0) * BEAM_SPREAD

    def beam(inset0, inset1):
        return polygon([(x0 - 0.5, cy - half0 - 1.4 + inset0), (BEAM_END, cy - half1 - 2.5 + inset1),
                        (BEAM_END, cy + half1 + 2.5 - inset1), (x0 - 0.5, cy + half0 + 1.4 - inset0)])

    if flat:
        return [
            Shape(rounded(cx - rx * 0.62, cy - ry * 0.9, cx + rx * 0.62, cy + ry * 0.9, rx * 0.6), ("solid", glow, 0.07)),
            Shape(rounded(cx - rx * 0.4, cy - ry * 0.84, cx + rx * 0.4, cy + ry * 0.84, rx * 0.36), ("solid", glow, 0.12)),
            Shape(beam(0, 0), ("solid", cone, 0.09)),
            Shape(beam(1.6, 3.4), ("solid", cone, 0.12)),
            *speck_shapes(icon_specks(), dust, flat=True),
            Shape(rounded(*SLIT), ("solid", light, 1.0)),
            Shape(rounded(*CORE), ("solid", core, 0.9)),
        ]
    edge = (90 - x0) / (BEAM_END - x0)  # onde fica a borda visível, ao longo do facho

    def layer(i, share):
        stops = [
            (0, cone, layer_alpha(BEAM_NEAR, share)),
            (edge * 0.45, cone, layer_alpha((BEAM_NEAR + BEAM_FAR) / 2 * 0.92, share)),
            (edge, cone, layer_alpha(BEAM_FAR, share)),
            (1, cone, layer_alpha(BEAM_FAR * 0.45, share)),
        ]
        f = i / (len(LAYERS) - 1)
        return Shape(beam(f * 2.6, f * 5.6), ("linear", x0, cy, BEAM_END + f * 3.4, cy, stops))

    return [
        ellipse(cx, cy, rx, ry, [(0, glow, 0.55), (0.45, glow, 0.18), (1, glow, 0.0)]),
        *(layer(i, share) for i, share in enumerate(LAYERS)),
        *speck_shapes(icon_specks(), dust, flat=False),
        Shape(rounded(*SLIT), ("solid", light, 1.0)),
        Shape(rounded(*CORE), ("linear", 54, CORE[1], 54, CORE[3], [(0, core, 0.0), (0.25, core, 0.8), (0.75, core, 0.8), (1, core, 0.0)])),
    ]


def text_run(text, font_path, size, x, baseline, spacing):
    """Um trecho do nome com as curvas da fonte e o kerning dela. [spacing] entra depois de cada
    letra (o nome é um pouco apertado, -0,02 da altura da letra). Devolve o caminho e a largura."""
    font = TTFont(font_path)
    upem = font["head"].unitsPerEm
    face = hb.Face(Path(font_path).read_bytes())
    hbfont = hb.Font(face)
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(hbfont, buf, {"kern": True, "liga": True})
    glyphs = font.getGlyphSet()
    order = font.getGlyphOrder()
    scale = size / upem
    pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
    pen_x = 0.0
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        name = order[info.codepoint]
        transform = (scale, 0, 0, -scale, x + pen_x + pos.x_offset * scale, baseline - pos.y_offset * scale)
        glyphs[name].draw(TransformPen(pen, transform))
        pen_x += pos.x_advance * scale + spacing
    return pen.getCommands(), pen_x


# ---- saída: vetor do Android -------------------------------------------------------------------

def vector_fill(fill, indent):
    kind = fill[0]
    if kind == "solid":
        _, color, alpha = fill
        return f'{indent}android:fillColor="{argb(color, alpha)}"', ""
    stops = fill[-1]
    items = "".join(f'{indent}        <item android:color="{argb(c, a)}" android:offset="{num(o)}" />\n' for o, c, a in stops)
    if kind == "linear":
        _, x1, y1, x2, y2, _ = fill
        geometry = (f'{indent}        android:startX="{num(x1)}"\n{indent}        android:startY="{num(y1)}"\n'
                    f'{indent}        android:endX="{num(x2)}"\n{indent}        android:endY="{num(y2)}"\n'
                    f'{indent}        android:type="linear"')
    else:
        _, cx, cy, r, _ = fill
        geometry = (f'{indent}        android:centerX="{num(cx)}"\n{indent}        android:centerY="{num(cy)}"\n'
                    f'{indent}        android:gradientRadius="{num(r)}"\n{indent}        android:type="radial"')
    body = (f'{indent}<aapt:attr name="android:fillColor">\n{indent}    <gradient\n{geometry}>\n'
            f"{items}{indent}    </gradient>\n{indent}</aapt:attr>\n")
    return None, body


def vector_shapes(shapes, indent="    "):
    out = ""
    for s in shapes:
        inner = indent
        if s.squash:
            cx, cy, k = s.squash
            out += f'{indent}<group\n{indent}    android:pivotX="{num(cx)}"\n{indent}    android:pivotY="{num(cy)}"\n{indent}    android:scaleY="{k:.4f}">\n'
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
            f'    android:viewportWidth="{num(viewport_w)}"\n    android:viewportHeight="{num(viewport_h)}">\n' + body + "</vector>\n")


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
    for key, (top, bottom, _, light, glow, _) in PALETTE.items():
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
    return Shape(f"M0,0h{num(w)}v{num(h)}h{num(-w)}z", fill)


def icon(key, top, bottom, diagonal, light, glow):
    write(RES / f"drawable/ic_launcher_{key}_background.xml",
          HEADER + '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
          + gradient_xml(key, diagonal) + "</shape>\n")
    write(RES / f"drawable-v24/ic_launcher_{key}_foreground.xml", vector(108, 108, 108, 108, vector_shapes(mark(light, glow)), True))
    write(RES / f"drawable/ic_launcher_{key}_foreground.xml", vector(108, 108, 108, 108, vector_shapes(mark(light, glow, flat=True)), False))

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


def one_color_mark(flat=False):
    """Uma cor só (o sistema pinta, e só a opacidade conta): a fresta inteira e o facho mais fraco,
    sumindo até a borda visível, com os grãos de poeira mais fortes do ícone. [flat]: o facho
    chapado, sem a poeira (Android 6, e o tamanho pequeno da notificação)."""
    x0 = SLIT[2]
    cy = (SLIT[1] + SLIT[3]) / 2
    half0 = (SLIT[3] - SLIT[1]) / 2 - 1.0
    half1 = half0 + (90 - x0) * BEAM_SPREAD
    beam = polygon([(x0, cy - half0), (90, cy - half1), (90, cy + half1), (x0, cy + half0)])
    slit = Shape(rounded(*SLIT), ("solid", "#FFFFFF", 1.0))
    if flat:
        return [Shape(beam, ("solid", "#FFFFFF", 0.4)), slit]
    fade = ("linear", x0, cy, 90, cy, [(0, "#FFFFFF", 0.62), (0.5, "#FFFFFF", 0.34), (1, "#FFFFFF", 0.1)])
    dots = [Shape(circle(x, y, max(r, 0.9)), ("solid", "#FFFFFF", 0.9)) for kind, x, y, r, o in icon_specks() if kind == "dot" and o >= 0.45]
    return [Shape(beam, fade), *dots, slit]


def monochrome():
    # O ícone temático só existe do Android 13 em diante, que desenha degradê: o arquivo fica na
    # pasta dos ícones adaptativos (v26), fora do alcance do Android 6.
    write(RES / "drawable-v26/ic_launcher_monochrome.xml", vector(108, 108, 108, 108, vector_shapes(one_color_mark()), True))


def notification_icon():
    # O mesmo desenho do monocromático, recortado em volta da marca: o ícone pequeno da barra de
    # status tem 24dp, e na grade de 108 a marca ficaria minúscula. A fresta começa em 51 e o facho
    # vai até 90: o quadrado de 48 em volta deles começa em (46, 30). Sem a poeira, que não aparece
    # nesse tamanho; o facho some em degradê do Android 7 em diante.
    faded = [s for s in one_color_mark() if s.fill[0] != "solid" or s.d == rounded(*SLIT)]
    for folder, shapes, gradient in (("drawable-v24", faded, True), ("drawable", one_color_mark(flat=True), False)):
        write(RES / f"{folder}/ic_notification.xml", vector(24, 24, 48, 48, group(vector_shapes(shapes), -46, -30, 1), gradient))


# O banner da TV: 320x180dp. O logotipo aprovado, desenhado nas medidas do estudo (um bloco de
# 760x300 com a letra em 88) e reduzido: a fresta, o facho com a poeira atravessando o nome e o
# nome. A fresta e o fim da palavra ficam à mesma distância das bordas.
BANNER_W, BANNER_H = 320, 180
LOCKUP_SCALE = 37 / 88  # a letra do nome fica com 37dp
LOCKUP_SLIT = (149.7, 88.3, 166.3, 210.6, 2.8)
LOCKUP_CORE = (156.4, 96.0, 159.6, 203.0, 1.6)
LOCKUP_BLOOM = (158.0, 149.4, 50.0, 75.0)
LOCKUP_BEAM = (160.0, 700.0, 89.4, 210.0, 39.0, 261.0)  # x0, x1, em cima e embaixo na fresta, em cima e embaixo no fim
LOCKUP_BEAM_SOFT = 5.0  # a borda suave do facho, para cada lado
LOCKUP_TEXT_X, LOCKUP_TEXT_SIZE, LOCKUP_BASELINE = 210.0, 88.0, 182.56
LOCKUP_MIDDLE = 150.0  # a altura que fica no meio do banner


def lockup_specks():
    """A poeira do logotipo: grãos pequenos e nítidos de brilho variado, médios fora de foco e
    alguns discos grandes, em grupinhos, mais fracos longe da fresta e perto das bordas do facho."""
    rng = random.Random(41)
    count, unit, clusters = 120, 1.0, 7
    x0, x1 = 170.0, 700.0
    top = lambda x: 89.4 - (x - 160) * 0.0933 + 3
    bottom = lambda x: 210.0 + (x - 160) * 0.0943 - 3
    out = []
    span = x1 - x0
    centers = [(x0 + span * rng.uniform(0.05, 0.85) ** 1.15, rng.uniform(0.2, 0.8)) for _ in range(clusters)]
    tries = 0
    while len(out) < count and tries < count * 40:
        tries += 1
        if rng.random() < 0.4:
            cx, cv = rng.choice(centers)
            x = rng.gauss(cx, span * 0.05)
            v = cv + rng.gauss(0, 0.16)
        else:
            x = x0 + span * rng.random() ** 1.35
            v = rng.random()
        if not (x0 + 2 * unit <= x <= x1) or not (0.04 <= v <= 0.96):
            continue
        y = top(x) + (bottom(x) - top(x)) * v
        t = (x - x0) / span
        fall = max(0.16, (1 - t) ** 1.05)
        fall *= min(1.0, 0.5 + min(v, 1 - v) * 4)
        k = rng.random()
        if k < 0.50:
            o = rng.choice([0.3, 0.45, 0.6, 0.8, 1.0]) * rng.uniform(0.8, 1.0)
            out.append(("dot", x, y, rng.uniform(0.55, 1.5) * unit, min(1, o * fall * 1.15)))
        elif k < 0.54:  # um grão alongado, de passagem: aqui, um grão nítido um pouco maior
            out.append(("dot", x, y, rng.uniform(1.4, 2.4) * unit * 0.62, rng.uniform(0.4, 0.7) * fall))
            rng.uniform(-25, 25)
        elif k < 0.86:
            out.append(("soft", x, y, rng.uniform(2.2, 4.6) * unit, rng.uniform(0.4, 0.8) * fall))
        else:
            out.append(("soft", x, y, rng.uniform(6, 13) * unit, rng.uniform(0.16, 0.3) * fall))
    # Os que mal apareceriam no tamanho do banner ficam de fora.
    return [s for s in out if s[4] >= 0.06]


def lockup(light, glow, letters, flat):
    """As formas do logotipo, nas medidas do estudo, e a largura do nome."""
    core = mix(light, "#FFFFFF", 0.6)
    cone = mix(light, glow, 0.5)
    dust = mix(light, "#FFFFFF", 0.5)
    first, middle, last = letters
    spacing = -0.02 * LOCKUP_TEXT_SIZE
    runs, x = [], LOCKUP_TEXT_X
    for (text, font), (start, end) in zip(NAME, ((first, middle), (middle, last))):
        d, width = text_run(text, font, LOCKUP_TEXT_SIZE, x, LOCKUP_BASELINE, spacing)
        fill = ("solid", mix(start, end, 0.5), 1.0) if flat else ("linear", x, 0, x + width, 0, [(0, start, 1.0), (1, end, 1.0)])
        runs.append(Shape(d, fill))
        x += width
    text_end = x - spacing
    cx, cy, rx, ry = LOCKUP_BLOOM
    bx0, bx1, top0, bottom0, top1, bottom1 = LOCKUP_BEAM

    def beam(inset):
        return polygon([(bx0, top0 + inset), (bx1, top1 + inset), (bx1, bottom1 - inset), (bx0, bottom0 - inset)])

    if flat:
        shapes = [
            Shape(rounded(cx - rx * 0.62, cy - ry * 0.9, cx + rx * 0.62, cy + ry * 0.9, rx * 0.6), ("solid", glow, 0.07)),
            Shape(rounded(cx - rx * 0.4, cy - ry * 0.84, cx + rx * 0.4, cy + ry * 0.84, rx * 0.36), ("solid", glow, 0.12)),
            Shape(beam(0), ("solid", cone, 0.1)),
            *speck_shapes([s for s in lockup_specks() if s[0] == "dot"], dust, flat=True),
            Shape(rounded(*LOCKUP_SLIT), ("solid", light, 1.0)),
            Shape(rounded(*LOCKUP_CORE), ("solid", core, 0.9)),
        ]
    else:
        def layer(i, share):
            stops = [(0, cone, layer_alpha(0.38, share)), (0.45, cone, layer_alpha(0.17, share)), (1, cone, 0.0)]
            inset = LOCKUP_BEAM_SOFT * (2 * i / (len(LAYERS) - 1) - 1)
            return Shape(beam(inset), ("linear", bx0, cy, bx1 - i * 9, cy, stops))

        shapes = [
            ellipse(cx, cy, rx, ry, [(0, glow, 0.55), (0.45, glow, 0.18), (1, glow, 0.0)]),
            *(layer(i, share) for i, share in enumerate(LAYERS)),
            *speck_shapes(lockup_specks(), dust, flat=False),
            Shape(rounded(*LOCKUP_SLIT), ("solid", light, 1.0)),
            Shape(rounded(*LOCKUP_CORE), ("linear", 158, LOCKUP_CORE[1], 158, LOCKUP_CORE[3], [(0, core, 0.0), (0.25, core, 0.8), (0.75, core, 0.8), (1, core, 0.0)])),
        ]
    return shapes + runs, text_end


def banner_parts(top, bottom, diagonal, light, glow, letters, flat):
    shapes, text_end = lockup(light, glow, letters, flat)
    # A fresta de um lado e o fim da palavra do outro, à mesma distância das bordas.
    width = (text_end - LOCKUP_SLIT[0]) * LOCKUP_SCALE
    tx = (BANNER_W - width) / 2 - LOCKUP_SLIT[0] * LOCKUP_SCALE
    ty = BANNER_H / 2 - LOCKUP_MIDDLE * LOCKUP_SCALE
    bg = background(top, bottom, diagonal, BANNER_W, BANNER_H)
    if flat:
        bg = Shape(bg.d, ("solid", mix(top, bottom, 0.5), 1.0))
    return bg, shapes, (tx, ty)


def tv_banner(key, top, bottom, diagonal, light, glow, letters):
    for folder, flat in (("drawable-v24", False), ("drawable", True)):
        bg, shapes, (tx, ty) = banner_parts(top, bottom, diagonal, light, glow, letters, flat)
        body = vector_shapes([bg]) + group(vector_shapes(shapes), tx, ty, LOCKUP_SCALE)
        write(RES / f"{folder}/tv_banner_{key}.xml", vector(BANNER_W, BANNER_H, BANNER_W, BANNER_H, body, not flat))


def preview(folder):
    """Desenha o ícone (com as máscaras) e o banner em PNG, para conferir."""
    import cairosvg

    folder.mkdir(parents=True, exist_ok=True)
    for key, (top, bottom, diagonal, light, glow, letters) in PALETTE.items():
        masks = {
            "circle": '<circle cx="54" cy="54" r="36"/>',
            "squircle": '<rect x="18" y="18" width="72" height="72" rx="20"/>',
        }
        for flat in (False, True):
            defs = []
            bg = svg_shapes([background(top, bottom, diagonal, 108, 108)], defs, "b")
            fg = svg_shapes(mark(light, glow, flat), defs, "f")
            for mask_name, mask in masks.items():
                svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="432" height="432">'
                       f'<defs>{"".join(defs)}<clipPath id="m">{mask}</clipPath></defs>'
                       f'<g clip-path="url(#m)">{bg}{fg}</g></svg>')
                name = f"icon_{key}_{mask_name}{'_flat' if flat else ''}.png"
                cairosvg.svg2png(bytestring=svg.encode(), write_to=str(folder / name))
            defs = []
            bgs, shapes, (tx, ty) = banner_parts(top, bottom, diagonal, light, glow, letters, flat)
            body = (svg_shapes([bgs], defs, "B") + f'<g transform="translate({tx} {ty}) scale({LOCKUP_SCALE})">'
                    + svg_shapes(shapes, defs, "D") + "</g>")
            svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {BANNER_W} {BANNER_H}" width="{BANNER_W * 4}" height="{BANNER_H * 4}">'
                   f'<defs>{"".join(defs)}</defs>{body}</svg>')
            cairosvg.svg2png(bytestring=svg.encode(), write_to=str(folder / f"banner_{key}{'_flat' if flat else ''}.png"))
    defs = []
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="432" height="432"><rect x="18" y="18" width="72" height="72" fill="#333"/>'
           f'{svg_shapes(one_color_mark(), defs, "m")}</svg>')
    cairosvg.svg2png(bytestring=svg.encode(), write_to=str(folder / "monochrome.png"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path, help="só desenha PNGs nesta pasta, sem mexer no app")
    args = parser.parse_args()
    if args.preview:
        preview(args.preview)
        return
    colors()
    monochrome()
    notification_icon()
    for key, (top, bottom, diagonal, light, glow, letters) in PALETTE.items():
        icon(key, top, bottom, diagonal, light, glow)
        tv_banner(key, top, bottom, diagonal, light, glow, letters)
        print(key)


if __name__ == "__main__":
    main()
