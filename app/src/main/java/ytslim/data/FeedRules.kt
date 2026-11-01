package ytslim.data

/** Regras do feed de Inscrições. Sem Android, para dar para testar no JVM. */
object FeedRules {
    /** Shorts nunca aparecem; lives e agendados só com o toggle das configurações ligado. */
    fun keep(video: Video, showLives: Boolean): Boolean =
        !video.isShorts && (showLives || (!video.isLive && !video.isUpcoming))
}

/** Um vídeo com a data (estimada) em que ele entrou no feed. */
data class Timed(val video: Video, val atMs: Long)

/**
 * Lê o tempo relativo que o YouTube escreve nos vídeos ("3 hours ago", "há 3 horas",
 * "Streamed 2 days ago"). O motor não entrega a data de publicação como número.
 */
object RelativeTime {
    private const val DAY = 86_400_000L
    private val UNITS: List<Pair<List<String>, Long>> = listOf(
        listOf("second", "segundo") to 1_000L,
        listOf("minute", "minuto", "min") to 60_000L,
        listOf("hour", "hora") to 3_600_000L,
        listOf("day", "dia") to DAY,
        listOf("week", "semana") to 7 * DAY,
        listOf("month", "mês", "mes") to 30 * DAY,
        listOf("year", "ano") to 365 * DAY,
    )
    private val NUMBER_AND_WORD = Regex("""(\d+)\s*([^\d\s•·,.]+)""")

    /** Quantos milissegundos atrás, ou null se o texto não tiver um tempo relativo. */
    fun agoMs(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        for (match in NUMBER_AND_WORD.findAll(text.lowercase())) {
            val (number, word) = match.destructured
            val unit = UNITS.firstOrNull { (keys, _) -> keys.any { word.startsWith(it) } } ?: continue
            return number.toLong() * unit.second
        }
        return null
    }
}

/** Junta o feed dos canais com os vídeos novos das playlists inscritas. */
object FeedMerge {
    /**
     * Estima a data de cada vídeo do feed, que já vem do mais novo para o mais antigo.
     * Sem data legível, o vídeo herda a do anterior; a ordem do YouTube nunca é trocada.
     */
    fun estimate(feed: List<Video>, nowMs: Long, startAtMs: Long = nowMs): List<Timed> {
        var last = startAtMs
        return feed.map { video ->
            val ago = RelativeTime.agoMs(video.published) ?: RelativeTime.agoMs(video.info)
            val at = ago?.let { (nowMs - it).coerceAtMost(last) } ?: last
            last = at
            Timed(video, at)
        }
    }

    /**
     * Enquanto o feed tem mais páginas, um vídeo de playlist mais antigo que o fim da página
     * espera a próxima, para não aparecer fora de ordem.
     */
    fun merge(feed: List<Timed>, extras: List<Timed>, feedComplete: Boolean): List<Video> {
        val cutoff = if (feedComplete) Long.MIN_VALUE else feed.lastOrNull()?.atMs ?: Long.MIN_VALUE
        val ids = feed.mapTo(HashSet()) { it.video.id }
        val shown = extras.filter { it.atMs >= cutoff && ids.add(it.video.id) }
        // sortedByDescending é estável: o feed mantém a ordem do YouTube nos empates.
        return (feed + shown).sortedByDescending { it.atMs }.map { it.video }
    }
}
