package ytslim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.random.Random

private val SlitShape = RoundedCornerShape(2.dp)

/**
 * A "fresta" que marca a aba ativa: uma barrinha na cor de destaque, sem brilho em volta (pedido
 * do Johan, 2026-11-03: o brilho fica só na marca do canto).
 */
@Composable
fun LightSlit(modifier: Modifier) {
    Box(modifier.background(Look.Glow, SlitShape))
}

/** O miolo da fresta e os grãos de poeira: a luz misturada com branco, como no ícone. */
private val SlitCore = Color(0xFFFFF4DA)

/**
 * No máximo quantas camadas desenham o facho: cada uma um pouco mais estreita, para a borda sair
 * suave. Num logotipo pequeno bastam menos (uma a cada 2,5 px de borda), e a luz fraca do fim do
 * facho sai mais lisa, porque cada camada fica mais forte.
 */
private const val BEAM_LAYERS = 6

/**
 * Um grão de poeira dentro do facho. [along] vai de 0 (na fresta) a 1 (onde o facho some), [across]
 * de -1 a 1 (de uma borda à outra), [radius] é uma fração da altura da fresta, e [soft] diz se o
 * grão está fora de foco.
 */
private class Speck(val along: Float, val across: Float, val radius: Float, val alpha: Float, val soft: Boolean)

/**
 * A poeira que passa na frente da luz (pedido do Johan, 2026-11-13: "como se fosse mesmo a
 * poeirinha passando na frente do brilho da TV de noite"): grãos pequenos e nítidos, médios fora
 * de foco e alguns discos grandes, em grupinhos, mais fracos longe da fresta e perto das bordas do
 * facho. A semente é fixa, para o desenho ser sempre o mesmo.
 */
private fun dust(count: Int, seed: Int): List<Speck> {
    val random = Random(seed)
    val clusters = List(3) { random.nextFloat() * 0.6f + 0.1f to random.nextFloat() * 1.2f - 0.6f }
    val specks = ArrayList<Speck>(count)
    var tries = 0
    while (specks.size < count && tries++ < count * 40) {
        val along: Float
        val across: Float
        if (random.nextFloat() < 0.4f) {
            val (x, y) = clusters[random.nextInt(clusters.size)]
            along = x + (random.nextFloat() - 0.5f) * 0.16f
            across = y + (random.nextFloat() - 0.5f) * 0.5f
        } else {
            along = random.nextFloat().let { it * it * 0.4f + it * 0.6f }
            across = random.nextFloat() * 2f - 1f
        }
        if (along !in 0.03f..0.92f || across !in -0.9f..0.9f) continue
        val fall = (1f - along).coerceAtLeast(0.2f) * (0.5f + (1f - kotlin.math.abs(across)) * 2f).coerceAtMost(1f)
        val kind = random.nextFloat()
        specks += when {
            kind < 0.5f -> Speck(along, across, 0.011f + random.nextFloat() * 0.015f, (0.4f + random.nextFloat() * 0.6f) * fall, soft = false)
            kind < 0.86f -> Speck(along, across, 0.04f + random.nextFloat() * 0.04f, (0.4f + random.nextFloat() * 0.4f) * fall, soft = true)
            else -> Speck(along, across, 0.09f + random.nextFloat() * 0.08f, (0.16f + random.nextFloat() * 0.14f) * fall, soft = true)
        }
    }
    return specks
}

private class BeamLayer(val path: Path, val paint: Paint)

private class CachedSpeck(val center: Offset, val radius: Float, val brush: Brush?, val color: Color)

/**
 * Peças pré-calculadas da marca ([beamMarkPlan]): paths, pinceis e degradês criados uma vez só por
 * tamanho e cor (via `drawWithCache`), sem realocar dezenas de shaders a cada redesenho.
 */
private class BeamMarkPlan(
    val bloomCenter: Offset,
    val bloomRadius: Float,
    val bloomBrush: Brush,
    val layers: List<BeamLayer>,
    val specks: List<CachedSpeck>,
    val slitLight: Color,
    val slitOffset: Offset,
    val slitSize: Size,
    val slitCorner: CornerRadius,
    val coreBrush: Brush,
    val coreOffset: Offset,
    val coreSize: Size,
    val coreCorner: CornerRadius,
) {
    fun draw(scope: DrawScope) = with(scope) {
        withTransform({ scale(1f, 1.5f, bloomCenter) }) {
            drawCircle(bloomBrush, radius = bloomRadius, center = bloomCenter)
        }
        drawIntoCanvas { canvas ->
            for (i in 0 until layers.size) {
                val layer = layers[i]
                canvas.drawPath(layer.path, layer.paint)
            }
        }
        for (i in 0 until specks.size) {
            val speck = specks[i]
            val brush = speck.brush
            if (brush != null) {
                drawCircle(brush, radius = speck.radius, center = speck.center)
            } else {
                drawCircle(speck.color, radius = speck.radius, center = speck.center)
            }
        }
        drawRoundRect(slitLight, slitOffset, slitSize, slitCorner)
        drawRoundRect(coreBrush, coreOffset, coreSize, coreCorner)
    }
}

/**
 * Monta as peças da marca sem fundo: a fresta com o brilho em volta e o facho de luz saindo para a
 * direita, com a poeira dentro dele. Desenhada a partir da fresta: [slitLeft] e [centerY] dizem onde
 * ela fica, [slitHeight] é a altura dela (a largura é 13,6% disso), e o facho vai até [length] depois
 * dela, abrindo [spread] da altura da fresta em cada lado. As proporções são as do logotipo aprovado.
 */
private fun beamMarkPlan(
    size: Size,
    slitLeft: Float,
    centerY: Float,
    slitHeight: Float,
    length: Float,
    spread: Float,
    strength: Float,
    specks: List<Speck>,
    light: Color,
    glow: Color,
): BeamMarkPlan {
    val slitWidth = slitHeight * 0.136f
    val slitCenter = slitLeft + slitWidth / 2
    // O brilho em volta da fresta: uma elipse em pé.
    val bloom = slitHeight * 0.41f
    val center = Offset(slitCenter, centerY)
    val bloomBrush = Brush.radialGradient(
        0f to glow.copy(alpha = 0.55f),
        0.45f to glow.copy(alpha = 0.18f),
        1f to Color.Transparent,
        center = center,
        radius = bloom,
    )
    // O facho: forte junto da fresta, sumindo até o fim, com a borda de cima e a de baixo suaves.
    val startHalf = slitHeight * 0.496f
    val endHalf = startHalf + slitHeight * spread
    val edge = slitHeight * 0.09f
    // Cada camada leva uma parte diferente da luz, e o degradê sai pontilhado (dither): camadas
    // fracas e iguais mudariam de tom todas no mesmo lugar, e o facho apareceria em faixas.
    val layerCount = (Math.round(edge / 2.5f) + 1).coerceIn(2, BEAM_LAYERS)
    val layers = List(layerCount) { i ->
        val step = i / (layerCount - 1f)
        val share = (0.7f + 0.6f * step) / layerCount
        fun layerAlpha(alpha: Float) = 1f - Math.pow(1.0 - alpha * strength, share.toDouble()).toFloat()
        val brush = Brush.horizontalGradient(
            0f to glow.copy(alpha = layerAlpha(0.38f)),
            0.25f to glow.copy(alpha = layerAlpha(0.26f)),
            0.5f to glow.copy(alpha = layerAlpha(0.15f)),
            0.75f to glow.copy(alpha = layerAlpha(0.06f)),
            0.9f to glow.copy(alpha = layerAlpha(0.018f)),
            1f to Color.Transparent,
            startX = slitCenter,
            endX = slitCenter + length * (1f - 0.06f * step),
        )
        val inset = edge * (step - 0.5f)
        val path = Path().apply {
            moveTo(slitCenter, centerY - startHalf + inset)
            lineTo(slitCenter + length, centerY - endHalf + inset * 1.6f)
            lineTo(slitCenter + length, centerY + endHalf - inset * 1.6f)
            lineTo(slitCenter, centerY + startHalf - inset)
            close()
        }
        val paint = Paint().apply { asFrameworkPaint().isDither = true }
        brush.applyTo(size, paint, 1f)
        BeamLayer(path, paint)
    }
    // A poeira, só dentro do facho.
    val grain = lerp(light, Color.White, 0.5f)
    val cachedSpecks = specks.map { speck ->
        val x = slitCenter + slitWidth + speck.along * (length - slitWidth)
        val half = startHalf + (endHalf - startHalf) * (x - slitCenter) / length
        val spot = Offset(x, centerY + speck.across * half * 0.9f)
        val radius = speck.radius * slitHeight
        if (speck.soft) {
            CachedSpeck(
                center = spot,
                radius = radius,
                brush = Brush.radialGradient(
                    0f to grain.copy(alpha = speck.alpha),
                    0.35f to grain.copy(alpha = speck.alpha * 0.6f),
                    1f to Color.Transparent,
                    center = spot,
                    radius = radius,
                ),
                color = Color.Transparent,
            )
        } else {
            CachedSpeck(spot, radius.coerceAtLeast(0.6f), brush = null, color = grain.copy(alpha = speck.alpha))
        }
    }
    // A fresta, com o miolo mais claro.
    val top = centerY - slitHeight / 2
    val coreBrush = Brush.verticalGradient(
        0f to Color.Transparent,
        0.25f to SlitCore.copy(alpha = 0.8f),
        0.75f to SlitCore.copy(alpha = 0.8f),
        1f to Color.Transparent,
        startY = top + slitHeight * 0.07f,
        endY = top + slitHeight * 0.93f,
    )
    return BeamMarkPlan(
        bloomCenter = center,
        bloomRadius = bloom,
        bloomBrush = bloomBrush,
        layers = layers,
        specks = cachedSpecks,
        slitLight = light,
        slitOffset = Offset(slitLeft, top),
        slitSize = Size(slitWidth, slitHeight),
        slitCorner = CornerRadius(slitWidth / 6),
        coreBrush = coreBrush,
        coreOffset = Offset(slitLeft + slitWidth * 0.4f, top + slitHeight * 0.07f),
        coreSize = Size(slitWidth * 0.2f, slitHeight * 0.86f),
        coreCorner = CornerRadius(slitWidth * 0.1f),
    )
}

/**
 * A marca sozinha, como no ícone do app: a fresta à esquerda e o facho indo até a borda direita.
 * Ocupa a caixa inteira; a fresta tem a altura de [slit] (uma fração da altura da caixa).
 */
@Composable
fun BeamGlyph(modifier: Modifier, slit: Float = 0.62f) {
    val specks = remember { dust(count = 9, seed = 7) }
    val light = Look.GlowSoft
    val glow = Look.Glow
    Box(
        modifier.drawWithCache {
            val slitHeight = size.height * slit
            val plan = if (slitHeight > 0f && size.width > 0f) {
                beamMarkPlan(
                    size = size,
                    slitLeft = slitHeight * 0.3f,
                    centerY = size.height / 2,
                    slitHeight = slitHeight,
                    length = size.width - slitHeight * 0.3f,
                    spread = 0.2f * (size.width / slitHeight - 0.3f),
                    strength = 1f,
                    specks = specks,
                    light = light,
                    glow = glow,
                )
            } else {
                null
            }
            onDrawBehind { plan?.draw(this) }
        },
    )
}

/**
 * O logotipo: a fresta, o facho com a poeira atravessando o nome e "beamtube" em dois pesos da
 * Outfit, com as letras acesas do lado da luz (aprovado pelo Johan em 2026-11-13). Sem fundo: fica
 * na barra de cima da TV, na mesma linha das abas, e nas telas de entrada. A luz é a cor de
 * destaque. [size] é o tamanho da letra; a caixa tem a altura de uma linha, e o facho passa um
 * pouco dela para cima, para baixo e para a direita.
 */
@Composable
fun Wordmark(size: TextUnit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val em = with(density) { size.toPx() }
    val light = Look.GlowSoft
    val glow = Look.Glow
    val specks = remember { dust(count = 34, seed = 41) }
    var baseline by remember { mutableFloatStateOf(Float.NaN) }
    // As letras "acesas": mais quentes junto da fresta, esfriando para o fim da palavra.
    val letters = remember(light) {
        Brush.horizontalGradient(
            0f to lerp(light, Look.Text, 0.35f),
            0.57f to Look.Text,
            1f to lerp(Look.Text, Look.Sub, 0.55f),
        )
    }
    val text = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("beam") }
            withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("tube") }
        }
    }
    val style = remember(letters, size) {
        TextStyle(
            brush = letters,
            fontFamily = Look.Outfit,
            fontSize = size,
            lineHeight = size,
            letterSpacing = (-0.02).em,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    }
    val lead: Dp = with(density) { (em * WORDMARK_LEAD).toDp() }
    val tail: Dp = with(density) { (em * WORDMARK_TAIL).toDp() }
    Text(
        text,
        style = style,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { baseline = it.firstBaseline },
        modifier = modifier
            .clearAndSetSemantics { contentDescription = "BeamTube" }
            .drawWithCache {
                // No logotipo a fresta tem 1,39 da altura da letra e fica centrada 0,38 acima da linha de base.
                val line = if (baseline.isNaN()) this.size.height * 0.87f else baseline
                val slitHeight = em * 1.389f
                val plan = if (slitHeight > 0f) {
                    beamMarkPlan(
                        size = this.size,
                        slitLeft = em * (WORDMARK_LEAD - 0.497f - 0.189f),
                        centerY = line - em * 0.377f,
                        slitHeight = slitHeight,
                        length = em * 6.14f,
                        spread = 0.412f,
                        strength = 1f,
                        specks = specks,
                        light = light,
                        glow = glow,
                    )
                } else {
                    null
                }
                onDrawBehind { plan?.draw(this) }
            }
            .padding(start = lead, end = tail),
    )
}

/** Da borda esquerda da caixa ao começo do nome, e do fim do nome à borda direita, em alturas de letra. */
private const val WORDMARK_LEAD = 0.92f
private const val WORDMARK_TAIL = 0.5f
