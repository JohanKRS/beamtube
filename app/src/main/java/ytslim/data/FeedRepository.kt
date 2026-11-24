package ytslim.data

import android.util.Log
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.ByteString.Companion.decodeBase64

/** Feed de Inscrições (canais) e a lista de canais inscritos. */
object FeedRepository {
    private const val TAG = "BeamTube"
    private const val SUBSCRIPTIONS = "\"browseId\":\"FEsubscriptions\""

    /** Primeira página do feed dos canais inscritos, do mais novo para o mais antigo. */
    suspend fun subscriptions(): VideoPage = Engine.content.subscriptionsObserve.firstOrNullMapped { it.toPage() }

    /** IDs dos canais inscritos, para a regra de não duplicar vídeos de playlists (e as fotos, em [ChannelPhotos]). null se falhar. */
    suspend fun subscribedChannelIds(maxAgeMs: Long = 0): Set<String>? =
        subscribedChannels(maxAgeMs = maxAgeMs)?.mapTo(HashSet()) { it.id }

    /**
     * Os canais inscritos (id, nome e foto). Uma lista lida há menos de [maxAgeMs] serve de novo
     * (a Explore aproveita a que o feed acabou de ler). null se falhar.
     *
     * O motor entrega essa lista sem o id dos canais (o TabMediaItem dele não lê o browseId), então
     * o app lê a mesma página da TV ("FEsubscriptions", uma aba por canal) direto no Innertube.
     */
    suspend fun subscribedChannels(maxAgeMs: Long = CHANNELS_MAX_AGE_MS): List<Channel>? {
        lastChannels?.let { (at, channels) -> if (System.currentTimeMillis() - at < maxAgeMs) return channels }
        return runCatching {
            val root = Innertube.post("browse", PostDataHelper.createQueryTV(SUBSCRIPTIONS), Innertube.TV_HEADERS)
            val channels = io { SubscribedChannels.parse(root) }
            check(channels.isNotEmpty()) { "no channels in the subscriptions page" }
            // De quebra, a foto de cada canal, para o avatar nos cards dos vídeos.
            ChannelPhotos.putAll(channels.mapNotNull { c -> c.thumbnailUrl?.let { c.id to it } }.toMap())
            channels
        }.onFailure { Log.w(TAG, "couldn't read the subscribed channels", it) }
            .getOrNull()?.also { lastChannels = System.currentTimeMillis() to it }
    }

    /** A inscrição num canal mudou: a próxima leitura da lista vai à conta. */
    fun forgetChannels() {
        lastChannels = null
    }

    @Volatile
    private var lastChannels: Pair<Long, List<Channel>>? = null

    internal const val CHANNELS_MAX_AGE_MS = 30 * 60 * 1000L
}

/**
 * Os canais da página de inscrições da TV: cada canal é uma aba ("tabRenderer", ou
 * "expandableTabRenderer") com o título e a foto. O id do canal fica nos params da aba
 * (browseId "FEsubscriptions", params = protobuf em base64 com o "UC…" dentro). A primeira aba
 * ("All") não tem canal.
 */
object SubscribedChannels {
    private val TAB_KEYS = listOf("tabRenderer", "expandableTabRenderer")
    private val CHANNEL_ID = Regex("UC[A-Za-z0-9_-]{22}")
    private val PERCENT = Regex("%([0-9A-Fa-f]{2})")
    /** Os avatares nos cards têm 18 dp (72 px a 4x): 88 px já fica nítido sem baixar 176 px. */
    private const val AVATAR_TARGET_PX = 88

    fun parse(root: JsonElement): List<Channel> {
        val channels = LinkedHashMap<String, Channel>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    var isTab = false
                    for (key in TAB_KEYS) {
                        val tab = element[key] as? JsonObject ?: continue
                        isTab = true
                        val id = channelId((tab["endpoint"] as? JsonObject)?.get("browseEndpoint") as? JsonObject) ?: continue
                        val name = Handles.channelName(text(tab["title"])).orEmpty()
                        channels.putIfAbsent(id, Channel(id, name, null, ChannelPhotos.normalize(photo(tab["thumbnail"]))))
                    }
                    // Não desce dentro da aba: evita percorrer todos os vídeos da aba "All".
                    if (isTab) return
                    for ((key, value) in element) {
                        if (key == "responseContext" || key == "trackingParams") continue
                        walk(value)
                    }
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return channels.values.toList()
    }

    /** O "UC…" do browseId, ou o que está dentro dos params ("igMaChhVQ…%3D" → …"UCYrxIIlUsH2xrWiQowOFqHw"). */
    internal fun channelId(browse: JsonObject?): String? {
        val id = browse?.string("browseId")
        if (id != null && CHANNEL_ID.matches(id)) return id
        val params = browse?.string("params") ?: return null
        // Só os %XX: um "+" do base64 continua "+".
        val base64 = PERCENT.replace(params) { it.groupValues[1].toInt(16).toChar().toString() }
        val bytes = base64.decodeBase64() ?: return null
        return CHANNEL_ID.find(bytes.string(Charsets.ISO_8859_1))?.value
    }

    /** O título vem como texto puro na TV; "simpleText" e "runs" ficam por garantia. */
    private fun text(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.takeIf { it.isString }?.content
        is JsonObject -> element.string("simpleText")
            ?: (element["runs"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.string("text") }?.joinToString("")
        else -> null
    }

    /** A menor foto com pelo menos [AVATAR_TARGET_PX] px (ou a maior disponível). */
    private fun photo(element: JsonElement?): String? {
        val list = (element as? JsonObject)?.get("thumbnails") as? JsonArray ?: return null
        var bestUrl: String? = null
        var bestWidth = Int.MAX_VALUE
        var maxUrl: String? = null
        var maxWidth = -1
        for (item in list) {
            val obj = item as? JsonObject ?: continue
            val url = obj.string("url") ?: continue
            val width = (obj["width"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            if (width >= AVATAR_TARGET_PX && width < bestWidth) {
                bestWidth = width
                bestUrl = url
            }
            if (width > maxWidth) {
                maxWidth = width
                maxUrl = url
            }
        }
        return bestUrl ?: maxUrl
    }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
