package ytslim.data

/** Um trecho do gráfico de "mais repetidos" do YouTube. [intensity] vai de 0 a 1. */
data class HeatSegment(val startMs: Long, val durationMs: Long, val intensity: Float)

/**
 * "Skip ahead", o mais perto que dá do "Jump ahead" do YouTube Premium (que não chega a apps de
 * fora): pular o trecho que o público pula, quase sempre o merchandising. Usa o gráfico de "mais
 * repetidos" do próprio YouTube, onde esse trecho aparece como um vale fundo.
 *
 * Calibrado em 2026-11-02 com 25 vídeos de canais com patrocínio (Veritasium, Kurzgesagt,
 * Wendover, Johnny Harris…), comparando com os trechos marcados no SponsorBlock: tocando no botão
 * perto do começo do patrocínio, o pulo cai no fim dele em 33 de 45 casos. Fora do patrocínio, o
 * botão só pula em 1 de cada 5 toques (a regra antiga, de pico em pico, pulava em 9 de 10).
 */
object SkipRules {
    /** Não vale a pena pular menos que isso. */
    const val MIN_JUMP_MS = 5_000L

    /** Um vale começa abaixo desta fração da mediana do vídeo… */
    private const val VALLEY = 0.3f

    /** …e acaba quando a audiência volta à mediana. */
    private const val RECOVERED = 1.0f

    /** Um vale que começa até aqui à frente também conta (o botão foi tocado logo antes). */
    const val LOOKAHEAD_MS = 20_000L

    /** Vales mais curtos são ruído; mais longos não são um trecho para pular. */
    const val MIN_VALLEY_MS = 15_000L
    const val MAX_VALLEY_MS = 300_000L

    /** Sem vale à frente, num trecho pouco visto: o próximo pico, se estiver perto. */
    const val MAX_PEAK_JUMP_MS = 60_000L

    /** Para onde o botão pula a partir de [positionMs], ou null se não há o que pular aqui. */
    fun nextSkip(segments: List<HeatSegment>, positionMs: Long): Long? =
        valleyEnd(segments, positionMs) ?: nearPeak(segments, positionMs)

    /** O fim do vale em que a posição está (ou que começa logo à frente). */
    internal fun valleyEnd(segments: List<HeatSegment>, positionMs: Long): Long? {
        if (segments.size < 5) return null
        val median = median(segments)
        if (median <= 0f) return null
        val low = median * VALLEY
        val back = median * RECOVERED
        val current = currentIndex(segments, positionMs)
        var start = current
        while (start > 0 && segments[start - 1].intensity < low) start--
        if (segments[start].intensity >= low) {
            // Fora de um vale: o próximo, se começar logo à frente.
            start = current + 1
            while (start < segments.size && segments[start].startMs <= positionMs + LOOKAHEAD_MS && segments[start].intensity >= low) start++
            if (start >= segments.size || segments[start].startMs > positionMs + LOOKAHEAD_MS) return null
        }
        var end = start
        while (end < segments.size && segments[end].intensity < back) end++
        // O vale vai até o fim do vídeo: pular seria só encerrar o vídeo.
        if (end >= segments.size) return null
        val length = segments[end].startMs - segments[start].startMs
        if (length !in MIN_VALLEY_MS..MAX_VALLEY_MS) return null
        return segments[end].startMs.takeIf { it >= positionMs + MIN_JUMP_MS }
    }

    /**
     * O começo de cada trecho que o botão pularia inteiro (os mesmos vales de [valleyEnd]), para
     * marcar na barra de tempo, como o YouTube marca o do Jump ahead.
     */
    fun valleys(segments: List<HeatSegment>): List<Long> {
        if (segments.size < 5) return emptyList()
        val median = median(segments)
        if (median <= 0f) return emptyList()
        val low = median * VALLEY
        val back = median * RECOVERED
        val starts = ArrayList<Long>()
        var i = 0
        while (i < segments.size) {
            if (segments[i].intensity >= low) {
                i++
                continue
            }
            var end = i
            while (end < segments.size && segments[end].intensity < back) end++
            if (end >= segments.size) break
            val length = segments[end].startMs - segments[i].startMs
            if (length in MIN_VALLEY_MS..MAX_VALLEY_MS) starts += segments[i].startMs
            i = end
        }
        return starts
    }

    /** Num trecho abaixo da mediana, o próximo pico, se estiver a menos de [MAX_PEAK_JUMP_MS]. */
    internal fun nearPeak(segments: List<HeatSegment>, positionMs: Long): Long? {
        if (segments.size < 5) return null
        if (segments[currentIndex(segments, positionMs)].intensity >= median(segments)) return null
        val peak = nextHighlight(segments, positionMs) ?: return null
        return peak.takeIf { it - positionMs <= MAX_PEAK_JUMP_MS }
    }

    private fun median(segments: List<HeatSegment>): Float = segments.map { it.intensity }.sorted()[segments.size / 2]

    private fun currentIndex(segments: List<HeatSegment>, positionMs: Long): Int =
        segments.indexOfFirst { it.startMs + it.durationMs > positionMs }.takeIf { it >= 0 } ?: segments.lastIndex

    /** Um trecho é "quente" acima do meio do caminho entre a mediana e o topo (95%) do vídeo. */
    fun threshold(segments: List<HeatSegment>): Float {
        val sorted = segments.map { it.intensity }.sorted()
        val median = sorted[sorted.size / 2]
        val top = sorted[((sorted.size - 1) * 95) / 100]
        return median + (top - median) / 2
    }

    /** Início do próximo trecho quente depois de [positionMs], ou null se não houver. */
    fun nextHighlight(segments: List<HeatSegment>, positionMs: Long): Long? {
        if (segments.size < 3) return null
        val t = threshold(segments)
        // O primeiro trecho (o começo do vídeo) é sempre muito visto; não conta.
        for (i in 1 until segments.size) {
            val s = segments[i]
            if (s.startMs < positionMs + MIN_JUMP_MS) continue
            if (s.intensity >= t && segments[i - 1].intensity < t) return s.startMs
        }
        return null
    }

    /** Vídeo sem gráfico (pouca audiência): o próximo capítulo. */
    fun nextChapter(chapterStartsMs: List<Long>, positionMs: Long): Long? =
        chapterStartsMs.sorted().firstOrNull { it >= positionMs + MIN_JUMP_MS }
}
