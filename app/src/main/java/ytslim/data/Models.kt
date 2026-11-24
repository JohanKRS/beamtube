package ytslim.data

import com.liskovsoft.googlecommon.common.helpers.YouTubeHelper
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Um vídeo, já convertido do modelo do motor. */
@Serializable
data class Video(
    val id: String,
    val title: String,
    val channelName: String?,
    val channelId: String?,
    val thumbnailUrl: String?,
    /** Linha secundária pronta do YouTube (canal, visualizações, data). */
    val info: String?,
    /** Tempo relativo da publicação, como o YouTube escreve ("3 hours ago"). */
    val published: String?,
    val durationMs: Long,
    val isLive: Boolean,
    val isUpcoming: Boolean,
    val isShorts: Boolean,
    val percentWatched: Int,
    val playlistId: String?,
    val playlistIndex: Int,
    /** No feed de Inscrições: o título da playlist inscrita de onde o vídeo veio. */
    val fromPlaylist: String? = null,
    /** No Histórico: o que o YouTube pede para tirar o vídeo de lá (o menu do cartão manda). */
    val feedbackToken: String? = null,
    /** Onde o YouTube diz que o vídeo parou (o tempo do card); 0 quando não foi começado. */
    val resumeMs: Long = 0,
    /**
     * Só nos Recomendados (o Início do YouTube): o "Don't recommend channel" do menu do card, que
     * o app manda quando o canal está bloqueado ([ChannelFeedback]). Não é guardado no aparelho.
     */
    @Transient val notRecommend: NotRecommend? = null,
)

/**
 * O pedido "Don't recommend channel" de um card do Início. O cliente de TV manda o token no menu
 * do card ([Token]) ou, desde 2026, só um painel que traz o menu quando é pedido ([Panel]).
 */
sealed interface NotRecommend {
    data class Token(val value: String) : NotRecommend
    data class Panel(val panelId: String, val params: String) : NotRecommend
}

/**
 * Uma playlist da conta (inclui "Assistir mais tarde").
 *
 * [browseId] e [params] são o que o motor precisa para abrir a playlist.
 */
data class Playlist(
    val browseId: String,
    val params: String?,
    val playlistId: String?,
    val title: String,
    val info: String?,
    val thumbnailUrl: String?,
)

/** Um canal (resultado de busca, ou o canal de um vídeo). */
data class Channel(val id: String, val title: String, val info: String?, val thumbnailUrl: String?)

/** Um resultado de busca: vídeo, canal ou playlist. */
sealed interface SearchItem {
    val key: String

    data class VideoItem(val video: Video) : SearchItem {
        override val key get() = "v:" + video.id
    }

    data class ChannelItem(val channel: Channel) : SearchItem {
        override val key get() = "c:" + channel.id
    }

    data class PlaylistItem(val playlist: Playlist) : SearchItem {
        override val key get() = "p:" + playlist.browseId
    }
}

/**
 * Uma página de vídeos. [next] é a continuação opaca do motor (null quando acabou). [home]: a
 * página é dos Recomendados (o Início do YouTube), e as próximas também são.
 */
class VideoPage(val videos: List<Video>, internal val next: MediaGroup?, val title: String? = null, internal val home: Boolean = false) {
    val hasMore: Boolean get() = next != null
}

/** [home]: o item veio do Início, e o menu dele traz o "Don't recommend channel". */
internal fun MediaItem.toVideo(home: Boolean = false): Video? {
    val id = videoId ?: return null
    val info = Handles.strip(secondTitle?.toString())
    return Video(
        id = id,
        title = title.orEmpty(),
        channelName = Handles.resolveChannelName(author, info),
        channelId = channelId,
        thumbnailUrl = cardImageUrl,
        info = info,
        published = productionDate,
        durationMs = durationMs,
        isLive = isLive,
        isUpcoming = isUpcoming,
        // A marca do YouTube às vezes falta; a regra do motor olha duração, "#" no título e a capa.
        isShorts = isShorts || YouTubeHelper.isShorts(this, false),
        percentWatched = percentWatched,
        playlistId = playlistId,
        playlistIndex = playlistIndex,
        feedbackToken = feedbackToken,
        resumeMs = startTimeSeconds.coerceAtLeast(0) * 1000L,
        notRecommend = if (home) notRecommend() else null,
    )
}

/**
 * O "Don't recommend channel" de um card do Início, como o SmartTube lê: o segundo pedido do menu
 * (o primeiro é o "Not interested") ou, quando o card só traz o painel, o painel.
 */
private fun MediaItem.notRecommend(): NotRecommend? =
    feedbackEndpoint?.let { NotRecommend.Panel(it.panelId, it.params) } ?: feedbackToken2?.let(NotRecommend::Token)

internal fun MediaItem.toPlaylist(): Playlist? {
    val browseId = channelId ?: playlistId?.let { "VL$it" } ?: return null
    return Playlist(
        browseId = browseId,
        params = params,
        playlistId = playlistId ?: browseId.removePrefix("VL").takeIf { browseId.startsWith("VL") },
        title = title.orEmpty(),
        info = Handles.strip(secondTitle?.toString()),
        thumbnailUrl = cardImageUrl,
    )
}

internal fun MediaGroup?.toPage(home: Boolean = false): VideoPage {
    val items = this?.mediaItems.orEmpty()
    return VideoPage(
        videos = items.mapNotNull { it.toVideo(home) },
        next = this?.takeIf { it.nextPageKey != null },
        title = this?.title,
        home = home,
    )
}

/**
 * Converte um resultado de busca: playlist ("VL…", ou um item com playlistId: a busca manda a
 * playlist junto com o primeiro vídeo dela), vídeo ou canal ("UC…").
 */
internal fun MediaItem.toSearchItem(): SearchItem? {
    val channel = channelId
    if (channel?.startsWith("VL") == true || playlistId != null) {
        return toPlaylist()?.let { playlist ->
            // "3Blue1Brown • 16 lessons"
            val info = listOfNotNull(playlist.info, badgeText).filter { it.isNotBlank() }.joinToString(" • ")
            SearchItem.PlaylistItem(playlist.copy(info = info.ifEmpty { null }))
        }
    }
    videoId?.let { return toVideo()?.let(SearchItem::VideoItem) }
    if (channel == null) return null
    val photo = ChannelPhotos.normalize(cardImageUrl)
    return SearchItem.ChannelItem(Channel(channel, title.orEmpty(), Handles.strip(secondTitle?.toString()), photo))
}

/**
 * O @ do canal ("@veritasium") fica fora do app (pedido do Johan). O YouTube manda o @ junto com o
 * nome nos cartões da TV ("Veritasium • @veritasium") e na linha dos canais da busca
 * ("@veritasium • 21.4M subscribers").
 */
object Handles {
    /** Marcas invisíveis de direção que o motor põe em cada pedaço da linha (BidiFormatter). */
    private val BIDI = Regex("[\u200E\u200F\u202A-\u202E\u2066-\u2069]")
    private val AGO_MARKER = Regex("""(?<!\p{L})(ago|há|ha|hace)(?!\p{L})""")
    private val WATCHING = Regex("""\d.*(watching|assistindo|espectadores|mirando|viendo)""")
    private val SCHEDULED = listOf("scheduled", "premieres", "estreia", "programado", "agendado")
    private val BADGES = setOf(
        "4k", "8k", "hd", "sd", "hdr", "cc", "vr180", "360°",
        "new", "novo", "nova", "nuevo", "nueva",
        "live", "ao vivo", "en vivo", "en directo",
        "fundraiser", "members only", "só para membros", "solo para miembros",
    )

    /** "Veritasium • @veritasium" vira "Veritasium". Só com o @, o nome sem ele. */
    fun channelName(raw: String?): String? {
        val parts = parts(raw)
        return parts.firstOrNull { !it.startsWith("@") } ?: parts.firstOrNull()?.removePrefix("@")?.ifBlank { null }
    }

    /**
     * Tira as partes "@…" e as marcas invisíveis de direção de uma linha do YouTube
     * (sem elas, a linha fica como veio).
     */
    fun strip(info: String?): String? {
        if (info == null) return null
        val cleaned = clean(info)
        val parts = parts(cleaned)
        if (parts.isEmpty()) return null
        if (parts.none { it.startsWith("@") }) return cleaned.trim('•', '·', ' ', '\t', '\n', '\r').ifEmpty { null }
        return parts.filterNot { it.startsWith("@") }.joinToString(" • ").ifEmpty { null }
    }

    /**
     * O nome do canal vindo da linha pronta do YouTube ("Canal • 16.1M views • 9 hours ago").
     * Usado quando o motor não entrega o autor separado (nos Recomendados, onde o menu do card
     * mudou para painel e o autor chega nulo ou com um selo como "4K"). Nas páginas de canal,
     * onde a linha já começa nas visualizações ou na data, devolve null.
     */
    fun channelFromInfo(info: String?): String? {
        val parts = parts(info)
        val first = parts.firstOrNull() ?: return null
        if (first.startsWith("@")) return null
        val hasMetaAfter = parts.drop(1).any { ViewCount.parse(it) != null || isRelativeTime(it) }
        if (isNonTimeMeta(first)) return null
        if (!hasMetaAfter && isRelativeTime(first)) return null
        return first
    }

    /**
     * O nome do canal de um vídeo, combinando o [author] do motor com a linha [info]:
     * - quando [author] falta ou traz um selo ("4K", "New"), usa o canal de [info];
     * - quando ambos existem e [info] começa por [author] (colaboração: "A and B"), fica [author];
     * - quando [info] não tem canal (página de canal), usa [author] (se não for selo ou detalhe).
     */
    fun resolveChannelName(author: String?, info: String?): String? {
        val fromInfo = channelFromInfo(info)
        val fromAuthor = channelName(author)?.takeUnless { isNonTimeMeta(it) || isRelativeTime(it) }
        if (fromAuthor == null) return fromInfo
        if (fromInfo == null) return fromAuthor
        val a = TopicRules.normalize(fromAuthor)
        val i = TopicRules.normalize(fromInfo)
        val matches = if (a.isNotEmpty() && i.isNotEmpty()) a == i || i.startsWith("$a ")
        else fromAuthor.equals(fromInfo, ignoreCase = true) || fromInfo.startsWith("$fromAuthor ", ignoreCase = true)
        return if (matches) fromAuthor else fromInfo
    }

    internal fun clean(text: String): String = text.replace(BIDI, "").trim()

    internal fun parts(text: String?): List<String> =
        text.orEmpty().split('•', '·').map(::clean).filter { it.isNotEmpty() }

    private fun isRelativeTime(part: String): Boolean =
        RelativeTime.agoMs(part) != null && AGO_MARKER.containsMatchIn(part.lowercase())

    private fun isNonTimeMeta(part: String): Boolean {
        val lower = part.lowercase()
        return lower in BADGES ||
            ViewCount.parse(part) != null ||
            WATCHING.containsMatchIn(lower) ||
            SCHEDULED.any { lower.startsWith(it) }
    }
}
