package ytslim.data

/** Um capítulo do vídeo, como o YouTube mostra na barra de tempo. */
data class Chapter(val startMs: Long, val title: String)

object Chapters {
    /** O capítulo em que [positionMs] está, ou null (sem capítulos, ou antes do primeiro). */
    fun at(chapters: List<Chapter>, positionMs: Long): Chapter? {
        var lo = 0
        var hi = chapters.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (chapters[mid].startMs <= positionMs) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return if (found >= 0) chapters[found] else null
    }
}
