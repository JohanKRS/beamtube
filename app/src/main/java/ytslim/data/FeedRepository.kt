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
    private const val TAG = "Doorglow"
    private const val SUBSCRIPTIONS = "\"browseId\":\"FEsubscriptions\""

    /** Primeira página do feed dos canais inscritos, do mais novo para o mais antigo. */
    suspend fun subscriptions(): VideoPage = Engine.content.subscriptionsObserve.firstOrNull().toPage()

    /** IDs dos canais inscritos, para a regra de não duplicar vídeos de playlists (e as fotos, em [ChannelPhotos]). null se falhar. */
    suspend fun subscribedChannelIds(): Set<String>? = subscribedChannels(maxAgeMs = 0)?.mapTo(HashSet()) { it.id }

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
            val channels = SubscribedChannels.parse(Innertube.post("browse", PostDataHelper.createQueryTV(SUBSCRIPTIONS), Innertube.TV_HEADERS))
            check(channels.isNotEmpty()) { "no channels in the subscriptions page" }
            // De quebra, a foto de cada canal, para o avatar nos cards dos vídeos.
            ChannelPhotos.putAll(channels.mapNotNull { c -> c.thumbnailUrl?.let { c.id to it } }.toMap())
            channels
        }.onFailure { Log.w(TAG, "couldn't read the subscribed channels", it) }
            .getOrNull()?.also { lastChannels = System.currentTimeMillis() to it }
    }

    @Volatile
    private var lastChannels: Pair<Long, List<Channel>>? = null

    private const val CHANNELS_MAX_AGE_MS = 30 * 60 * 1000L
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

    fun parse(root: JsonElement): List<Channel> {
        val channels = LinkedHashMap<String, Channel>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    for (key in TAB_KEYS) {
                        val tab = element[key] as? JsonObject ?: continue
                        val id = channelId((tab["endpoint"] as? JsonObject)?.get("browseEndpoint") as? JsonObject) ?: continue
                        val name = Handles.channelName(text(tab["title"])).orEmpty()
                        channels.putIfAbsent(id, Channel(id, name, null, ChannelPhotos.normalize(photo(tab["thumbnail"]))))
                    }
                    element.values.forEach(::walk)
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

    /** A maior das fotos. */
    private fun photo(element: JsonElement?): String? = ((element as? JsonObject)?.get("thumbnails") as? JsonArray)
        ?.mapNotNull { it as? JsonObject }
        ?.maxByOrNull { (it["width"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0 }
        ?.string("url")

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
