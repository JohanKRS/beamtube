package ytslim.data

import kotlinx.serialization.Serializable

/** Regras do feed de Inscrições. Sem Android, para dar para testar no JVM. */
object FeedRules {
    /** Shorts nunca aparecem; lives e agendados só com o toggle das configurações ligado. */
    fun keep(video: Video, showLives: Boolean): Boolean =
        !video.isShorts && (showLives || (!video.isLive && !video.isUpcoming))
}

/** Um vídeo com a data (estimada) em que ele entrou no feed. */
@Serializable
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

/**
 * Os filtros da busca aplicados a uma lista que já veio do YouTube (as Recomendações): data,
 * duração e ordem. O YouTube não filtra essas listas, então o app olha cada vídeo: a duração, o
 * tempo relativo ("3 days ago") e as visualizações da linha do vídeo.
 */
object FeedFilter {
    fun apply(videos: List<Video>, filters: SearchFilters, nowMs: Long): List<Video> {
        if (filters.isDefault) return videos
        val cutoff = filters.beforeYear?.let(::startOfYearMs)
        val kept = videos.filter { video ->
            if (!filters.duration.matches(video.durationMs)) return@filter false
            if (filters.date == UploadDate.ANY && cutoff == null) return@filter true
            val ago = agoMs(video) ?: return@filter false
            // "3 days ago" quer dizer de 3 a 4 dias: o tempo lido é o mínimo da idade do vídeo.
            val dateOk = filters.date.maxAgeMs?.let { ago < it } ?: true
            dateOk && (cutoff == null || nowMs - ago < cutoff)
        }
        return when (filters.sort) {
            SortBy.DATE -> kept.sortedBy { agoMs(it) ?: Long.MAX_VALUE }
            SortBy.VIEWS -> kept.sortedByDescending { ViewCount.parse(it.info) ?: -1L }
            SortBy.RELEVANCE, SortBy.RATING -> kept
        }
    }

    private fun agoMs(video: Video): Long? = RelativeTime.agoMs(video.published) ?: RelativeTime.agoMs(video.info)

    /** 1º de janeiro de [year], 0h, no fuso do aparelho. */
    fun startOfYearMs(year: Int): Long = java.util.Calendar.getInstance().run {
        clear()
        set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        timeInMillis
    }
}

/**
 * Lê as visualizações da linha do YouTube: "1.2M views", "12K views", "1,234 views",
 * "1,2 mi de visualizações", "12 mil visualizações".
 */
object ViewCount {
    private val VIEWS_WORDS = listOf("view", "visualiza", "exibi", "vista")
    private val NUMBER = Regex("""(\d+(?:[.,]\d+)*)\s*(mil|mi|bi|mln|mld|bn|k|m|b)?(?![a-z])""")

    fun parse(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        // A parte com "views" e um número (o nome do canal também pode ter "view": "Review Lab").
        return text.lowercase().split('•', '·').map { it.trim() }
            .filter { part -> VIEWS_WORDS.any { part.contains(it) } }
            .firstNotNullOfOrNull(::parsePart)
    }

    private fun parsePart(part: String): Long? {
        if (part.startsWith("no ") || part.startsWith("nenhuma")) return 0
        val match = NUMBER.find(part) ?: return null
        val (digits, suffix) = match.destructured
        val factor = when (suffix) {
            "k", "mil" -> 1_000.0
            "m", "mi", "mln" -> 1_000_000.0
            "b", "bi", "bn", "mld" -> 1_000_000_000.0
            else -> null
        }
        return if (factor == null) {
            // Sem sufixo, o ponto ou a vírgula separam milhares.
            digits.replace(".", "").replace(",", "").toLongOrNull()
        } else {
            // Com sufixo, separam decimais ("1.2M", "1,2 mi").
            digits.replace(',', '.').toDoubleOrNull()?.let { Math.round(it * factor) }
        }
    }
}
