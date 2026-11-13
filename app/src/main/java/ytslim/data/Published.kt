package ytslim.data

/**
 * Há quanto tempo um vídeo foi publicado, para a linha do canal no player (pedido do Johan,
 * 2026-11-13: "como na listagem"). Sem Android, para dar para testar no JVM.
 */
object Published {
    /** A idade de um vídeo em dias, semanas, meses ou anos. [count] 0 em [Span.DAY] = hoje. */
    data class Age(val count: Int, val span: Span)

    enum class Span { DAY, WEEK, MONTH, YEAR }

    /**
     * O que o card mostra ("3 hours ago", "Streamed 2 days ago"): o tempo relativo que veio com o
     * vídeo ou, sem ele, a parte da linha do YouTube que tem um ("Canal • 12K views • 3 hours ago").
     */
    fun of(video: Video): String? {
        video.published?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return video.info.orEmpty().split('•', '·').map { it.trim() }
            .firstOrNull { it.isNotEmpty() && RelativeTime.agoMs(it) != null }
    }

    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    /** "Nov 10, 2025", "Premiered Nov 10, 2025", "Streamed live on Nov 10, 2025". */
    private val MONTH_DAY_YEAR = Regex("""\b([A-Za-z]{3})[A-Za-z]*\.? (\d{1,2}), (\d{4})\b""")

    /** "10 Nov 2025". */
    private val DAY_MONTH_YEAR = Regex("""\b(\d{1,2}) ([A-Za-z]{3})[A-Za-z]*\.? (\d{4})\b""")

    /**
     * A idade pela data da página do vídeo (a do motor vem por extenso, sem a hora: "Nov 10, 2025").
     * null se o texto não tiver uma data em inglês.
     *
     * @param todayEpochDay o dia de hoje, contado em dias desde 1970-01-01 no fuso do aparelho.
     */
    fun age(dateText: String?, todayEpochDay: Long): Age? {
        if (dateText.isNullOrBlank()) return null
        val (day, month, year) = MONTH_DAY_YEAR.find(dateText)?.destructured?.let { (m, d, y) -> Triple(d, m, y) }
            ?: DAY_MONTH_YEAR.find(dateText)?.destructured?.let { (d, m, y) -> Triple(d, m, y) }
            ?: return null
        val monthIndex = MONTHS.indexOf(month.lowercase()).takeIf { it >= 0 } ?: return null
        val days = (todayEpochDay - epochDay(year.toInt(), monthIndex + 1, day.toInt())).coerceAtLeast(0)
        return when {
            days < 7 -> Age(days.toInt(), Span.DAY)
            days < 30 -> Age((days / 7).toInt(), Span.WEEK)
            days < 365 -> Age((days / 30).toInt().coerceAtLeast(1), Span.MONTH)
            else -> Age((days / 365).toInt(), Span.YEAR)
        }
    }

    /** Dias desde 1970-01-01 (o calendário de sempre), sem java.time (o app vai até o Android 6). */
    internal fun epochDay(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097L + dayOfEra - 719_468L
    }
}
