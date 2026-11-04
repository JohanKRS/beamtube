package ytslim.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.text.Cue
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser

/**
 * A legenda automática do YouTube rola (achado do Johan na TV, 2026-11-04): cada linha nova entra
 * embaixo e empurra a anterior para cima. Aqui ela vira blocos, como a legenda feita por pessoa:
 * duas linhas de uma vez (ou uma, quando a seguinte demora), trocadas inteiras pelas duas seguintes.
 * Uma legenda que não rola passa como veio.
 */
@OptIn(UnstableApi::class)
internal object CaptionBlocks {
    /** A segunda linha só entra no bloco se começar até este tempo depois da primeira. */
    const val PAIR_US = 6_000_000L

    /** Um intervalo menor que isso sem legenda não separa os trechos (e é coberto, para não piscar). */
    const val GAP_US = 100_000L

    /** Quantas vezes uma linha precisa subir para a legenda contar como uma que rola. */
    private const val MIN_ROLLS = 3

    private class Span(val timing: CuesWithTiming, val lines: List<String>, val template: Cue)

    private class Line(val text: String, val span: Int, val startUs: Long)

    fun of(timeline: List<CuesWithTiming>): List<CuesWithTiming> {
        if (timeline.any { t -> t.durationUs == C.TIME_UNSET || t.cues.any { it.bitmap != null } }) return timeline
        val spans = timeline.sortedBy { it.startTimeUs }.mapNotNull(::span)

        // As linhas na ordem em que entram e, para cada trecho, a mais nova que ele mostra.
        val lines = mutableListOf<Line>()
        val newest = IntArray(spans.size)
        var arrivals = 0
        var rolls = 0
        spans.forEachIndexed { i, span ->
            val previous = spans.getOrNull(i - 1)
            val fresh = if (previous != null && previous.lines == span.lines && touches(previous, span)) {
                emptyList()
            } else {
                // A linha que já estava (agora em cima, na legenda que rola) e as que vêm depois dela.
                val kept = span.lines.indexOf(lines.lastOrNull()?.text)
                span.lines.drop(kept + 1).also {
                    if (it.isNotEmpty()) {
                        arrivals++
                        if (kept >= 0) rolls++
                    }
                }
            }
            fresh.forEach { lines += Line(it, i, span.timing.startTimeUs) }
            newest[i] = lines.lastIndex
        }
        if (rolls < MIN_ROLLS || rolls * 2 < arrivals) return timeline

        // Os blocos: duas linhas seguidas, quando a segunda vem logo, sem pausa no meio.
        val blockOf = IntArray(lines.size)
        val blocks = mutableListOf<String>()
        var l = 0
        while (l < lines.size) {
            val size = if (l + 1 < lines.size && pairs(lines[l], lines[l + 1], spans)) 2 else 1
            for (m in l until l + size) blockOf[m] = blocks.size
            blocks += lines.subList(l, l + size).joinToString("\n") { it.text }
            l += size
        }

        // Cada trecho mostra o bloco inteiro da sua linha mais nova; trechos seguidos do mesmo bloco viram um.
        val out = mutableListOf<CuesWithTiming>()
        var first = 0
        for (i in spans.indices) {
            val next = spans.getOrNull(i + 1)?.takeIf { touches(spans[i], it) }
            if (next != null && blockOf[newest[i + 1]] == blockOf[newest[i]]) continue
            val startUs = spans[first].timing.startTimeUs
            val endUs = next?.timing?.startTimeUs ?: spans[i].timing.endTimeUs
            val cue = spans[first].template.buildUpon().setText(blocks[blockOf[newest[i]]]).build()
            out += CuesWithTiming(listOf(cue), startUs, endUs - startUs)
            first = i + 1
        }
        return out
    }

    /** As linhas de texto de um trecho, de cima para baixo, sem as vazias. null = nada a mostrar. */
    private fun span(timing: CuesWithTiming): Span? {
        val texts = timing.cues.filter { it.text != null }
        val lines = texts.flatMap { it.text.toString().split('\n') }
            .map { it.replace(SPACES, " ").trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        return Span(timing, lines, texts.first())
    }

    private fun pairs(a: Line, b: Line, spans: List<Span>): Boolean =
        b.startUs - a.startUs <= PAIR_US && (a.span until b.span).all { touches(spans[it], spans[it + 1]) }

    private fun touches(a: Span, b: Span) = b.timing.startTimeUs - a.timing.endTimeUs <= GAP_US

    private val SPACES = Regex("\\s+")
}

/** Entrega a legenda de [inner] em blocos ([CaptionBlocks]). */
@OptIn(UnstableApi::class)
internal class BlockCaptionParser(private val inner: SubtitleParser) : SubtitleParser {
    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>,
    ) {
        // Os blocos dependem da linha seguinte: a legenda é lida inteira e entregue inteira (o
        // extrator ordena o que recebe).
        val timeline = mutableListOf<CuesWithTiming>()
        inner.parse(data, offset, length, SubtitleParser.OutputOptions.allCues()) { timeline += it }
        val from = outputOptions.startTimeUs
        CaptionBlocks.of(timeline)
            .filter { outputOptions.outputAllCues || from == C.TIME_UNSET || it.endTimeUs > from }
            .forEach(output::accept)
    }

    override fun reset() = inner.reset()

    override fun getCueReplacementBehavior(): Int = inner.cueReplacementBehavior
}
