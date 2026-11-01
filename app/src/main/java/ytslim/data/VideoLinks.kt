package ytslim.data

import java.net.URI

/** Um vídeo aberto por link externo (WhatsApp, navegador, "Compartilhar"). */
data class VideoLink(val videoId: String, val startSeconds: Long = 0)

/**
 * Lê links do YouTube: youtube.com/watch?v=, youtu.be/, /live/, /embed/, /v/ e m./music.
 * Shorts ficam de fora de propósito (o app não tem Shorts). Links de playlist têm [parsePlaylist].
 */
object VideoLinks {
    private val ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val PLAYLIST_ID = Regex("^[A-Za-z0-9_-]{2,64}$")
    private val URL_IN_TEXT = Regex("""https?://\S+""")
    private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
    private val ID_PATHS = listOf("/live/", "/embed/", "/v/")

    /** Aceita um link ou um texto compartilhado que contenha um link. */
    fun parse(text: String?): VideoLink? {
        if (text.isNullOrBlank()) return null
        val candidates = URL_IN_TEXT.findAll(text).map { it.value }.toList().ifEmpty { listOf(text.trim()) }
        return candidates.firstNotNullOfOrNull { parseUrl(it.trimEnd('.', ',', ')', ']')) }
    }

    /** ID de uma playlist em youtube.com/playlist?list= (abre a página da playlist). */
    fun parsePlaylist(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val candidates = URL_IN_TEXT.findAll(text).map { it.value }.toList().ifEmpty { listOf(text.trim()) }
        return candidates.firstNotNullOfOrNull { candidate ->
            val uri = runCatching { URI(candidate.trimEnd('.', ',', ')', ']')) }.getOrNull() ?: return@firstNotNullOfOrNull null
            if (uri.host?.lowercase() !in YOUTUBE_HOSTS || uri.path != "/playlist") return@firstNotNullOfOrNull null
            parseQuery(uri.rawQuery)["list"]?.takeIf { PLAYLIST_ID.matches(it) }
        }
    }

    private fun parseUrl(url: String): VideoLink? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path.orEmpty()
        val query = parseQuery(uri.rawQuery)

        val id = when {
            host == "youtu.be" -> path.trim('/').substringBefore('/')
            host in YOUTUBE_HOSTS && path == "/watch" -> query["v"]
            host in YOUTUBE_HOSTS -> ID_PATHS.firstOrNull { path.startsWith(it) }
                ?.let { path.removePrefix(it).substringBefore('/') }
            else -> null
        }
        if (id == null || !ID.matches(id)) return null
        return VideoLink(id, parseTime(query["t"] ?: query["start"]))
    }

    private fun parseQuery(raw: String?): Map<String, String> =
        raw.orEmpty().split('&').filter { '=' in it }.associate {
            val (k, v) = it.split('=', limit = 2)
            k to java.net.URLDecoder.decode(v, "UTF-8")
        }

    /** "90", "90s", "1m30s", "1h2m3s" -> segundos. */
    internal fun parseTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0
        value.toLongOrNull()?.let { return it }
        val match = Regex("""^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""").matchEntire(value) ?: return 0
        val (h, m, s) = match.destructured
        return (h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (s.toLongOrNull() ?: 0)
    }
}
