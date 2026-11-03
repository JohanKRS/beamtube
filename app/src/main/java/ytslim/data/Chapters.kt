package ytslim.data

/** Um capítulo do vídeo, como o YouTube mostra na barra de tempo. */
data class Chapter(val startMs: Long, val title: String)

object Chapters {
    /** O capítulo em que [positionMs] está, ou null (sem capítulos, ou antes do primeiro). */
    fun at(chapters: List<Chapter>, positionMs: Long): Chapter? = chapters.lastOrNull { it.startMs <= positionMs }
}
