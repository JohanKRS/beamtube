package ytslim.data

/** Um trecho do gráfico de "mais repetidos" do YouTube. [intensity] vai de 0 a 1. */
data class HeatSegment(val startMs: Long, val durationMs: Long, val intensity: Float)

/**
 * "Pular partes menos interessantes": vai para o próximo trecho que a audiência repete
 * bem mais que o normal do vídeo. Sem gráfico, vai para o próximo capítulo.
 */
object SkipRules {
    /** Não vale a pena pular menos que isso. */
    const val MIN_JUMP_MS = 5_000L

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

    fun nextChapter(chapterStartsMs: List<Long>, positionMs: Long): Long? =
        chapterStartsMs.sorted().firstOrNull { it >= positionMs + MIN_JUMP_MS }
}
