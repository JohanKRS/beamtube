package ytslim.data

/** Um trecho do gráfico de "mais repetidos" do YouTube. [intensity] vai de 0 a 1. */
data class HeatSegment(val startMs: Long, val durationMs: Long, val intensity: Float)

/**
 * O pulo do trecho que o público pula (quase sempre o merchandising), o mais perto que dá do "Jump
 * ahead" do YouTube Premium (que não chega a apps de fora). Usa o gráfico de "mais repetidos" do
 * próprio YouTube, onde esse trecho aparece como um vale fundo.
 *
 * Desde 2026-11-03 (pedido do Johan, como o app do YouTube faz) não há mais botão: avançar dentro
 * de um desses trechos, ou logo antes dele, vai direto para o fim dele ([valleyEnd]).
 *
 * Calibrado em 2026-11-02 com 25 vídeos de canais com patrocínio (Veritasium, Kurzgesagt,
 * Wendover, Johnny Harris…), comparando com os trechos marcados no SponsorBlock: perto do começo
 * do patrocínio, o pulo cai no fim dele em 33 de 45 casos. No meio do conteúdo normal, quase
 * nunca há vale (o pulo de pico em pico, que a regra antiga também usava, pulava em 9 de 10).
 */
object SkipRules {
    /** Não vale a pena pular menos que isso. */
    const val MIN_JUMP_MS = 5_000L

    /** Um vale começa abaixo desta fração da mediana do vídeo… */
    private const val VALLEY = 0.3f

    /** …e acaba quando a audiência volta à mediana. */
    private const val RECOVERED = 1.0f

    /** Um vale que começa até aqui à frente também conta (o avanço veio logo antes dele). */
    const val LOOKAHEAD_MS = 20_000L

    /** Vales mais curtos são ruído; mais longos não são um trecho para pular. */
    const val MIN_VALLEY_MS = 15_000L
    const val MAX_VALLEY_MS = 300_000L

    /**
     * O fim do trecho em que a posição está (ou que começa logo à frente), ou null se não há o que
     * pular aqui. Vale do começo do trecho até o fim dele, mesmo onde a audiência já subiu um pouco
     * antes de voltar à mediana: avançar no meio do patrocínio também pula o resto dele.
     */
    fun valleyEnd(segments: List<HeatSegment>, positionMs: Long): Long? {
        val span = spans(segments).firstOrNull { positionMs < it.endMs } ?: return null
        if (span.startMs > positionMs + LOOKAHEAD_MS) return null
        return span.endMs.takeIf { it >= positionMs + MIN_JUMP_MS }
    }

    /**
     * O começo de cada trecho que o avanço pula inteiro (os mesmos de [valleyEnd]), para marcar na
     * barra de tempo, como o YouTube marca o do Jump ahead.
     */
    fun valleys(segments: List<HeatSegment>): List<Long> = spans(segments).map { it.startMs }

    private class Span(val startMs: Long, val endMs: Long)

    /** Os vales: de onde a audiência despenca até onde ela volta à mediana. */
    private fun spans(segments: List<HeatSegment>): List<Span> {
        if (segments.size < 5) return emptyList()
        val median = median(segments)
        if (median <= 0f) return emptyList()
        val low = median * VALLEY
        val back = median * RECOVERED
        val spans = ArrayList<Span>()
        var i = 0
        while (i < segments.size) {
            if (segments[i].intensity >= low) {
                i++
                continue
            }
            var end = i
            while (end < segments.size && segments[end].intensity < back) end++
            // O vale vai até o fim do vídeo: pular seria só encerrar o vídeo.
            if (end >= segments.size) break
            val length = segments[end].startMs - segments[i].startMs
            if (length in MIN_VALLEY_MS..MAX_VALLEY_MS) spans += Span(segments[i].startMs, segments[end].startMs)
            i = end
        }
        return spans
    }

    private fun median(segments: List<HeatSegment>): Float = segments.map { it.intensity }.sorted()[segments.size / 2]
}
