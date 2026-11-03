package ytslim.data

import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import kotlinx.serialization.json.Json as KJson

/** Uma playlist do próprio usuário (inclui "Assistir mais tarde"), para o botão "Save". */
data class OwnPlaylist(val id: String, val title: String)

/** Salvar com um toque e mudar vídeos de lugar nas playlists do usuário. */
object PlaylistEditor {
    /** Qualquer vídeo serve: a lista de "salvar em" traz as playlists do próprio usuário. */
    private const val PROBE_VIDEO = "jNQXAC9IVRw"

    /** Páginas lidas para achar a posição dos vídeos ao mover (playlists de milhares de vídeos). */
    private const val MAX_PAGES = 100

    suspend fun ownPlaylists(): List<OwnPlaylist> =
        io { Engine.mediaItems.getPlaylistsInfo(PROBE_VIDEO) }.orEmpty()
            .mapNotNull { info -> info.playlistId?.let { OwnPlaylist(it, info.title.orEmpty()) } }

    /** O vídeo está em [playlistId]? null se não deu para saber. */
    suspend fun isSaved(videoId: String, playlistId: String): Boolean? = runCatching {
        io { Engine.mediaItems.getPlaylistsInfo(videoId) }.orEmpty().any { it.playlistId == playlistId && it.isSelected }
    }.getOrNull()

    suspend fun setSaved(videoId: String, playlistId: String, saved: Boolean) {
        if (saved) Engine.mediaItems.addToPlaylistObserve(playlistId, videoId).complete()
        else Engine.mediaItems.removeFromPlaylistObserve(playlistId, videoId).complete()
    }

    /**
     * Põe [videoId] logo antes de [successor] na playlist (null = no fim). O motor não tem "mover":
     * o app pede ao YouTube a posição de cada vídeo na playlist (o setVideoId) e manda o movimento
     * para o mesmo endpoint que o motor usa para adicionar.
     */
    suspend fun move(playlistId: String, videoId: String, successor: String?) {
        val needed = setOfNotNull(videoId, successor)
        try {
            sendMove(playlistId, videoId, successor, setVideoIds(playlistId, needed, fresh = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Um vídeo tirado e posto de novo ganha outro setVideoId: lê a playlist de novo e tenta mais uma vez.
            sendMove(playlistId, videoId, successor, setVideoIds(playlistId, needed, fresh = true))
        }
    }

    private suspend fun sendMove(playlistId: String, videoId: String, successor: String?, ids: Map<String, String>) {
        val action = buildJsonObject {
            put("action", "ACTION_MOVE_VIDEO_BEFORE")
            put("setVideoId", ids[videoId] ?: error("YouTube didn't say where this video is in the playlist"))
            if (successor != null) {
                put("movedSetVideoIdSuccessor", ids[successor] ?: error("YouTube didn't say where the next video is in the playlist"))
            }
        }
        val actions = buildJsonArray { add(action) }
        val result = Innertube.post("browse/edit_playlist", PostDataHelper.createQueryTV("\"playlistId\":${JsonPrimitive(playlistId)},\"actions\":$actions"))
        val status = (result as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull
        check(status == null || status == "STATUS_SUCCEEDED") { "YouTube didn't accept the new order ($status)" }
    }

    /**
     * setVideoId já lidos, por playlist. Eles não mudam quando os vídeos mudam de lugar, então uma
     * playlist grande é lida uma vez só, no primeiro movimento.
     */
    private val known = HashMap<String, HashMap<String, String>>()

    /**
     * setVideoId de cada vídeo de [needed]: do que já foi lido ou, se faltar (ou com [fresh]),
     * pedindo a playlist ao YouTube, primeiro pelo cliente de TV (o do login) e depois pelo WEB.
     */
    private suspend fun setVideoIds(playlistId: String, needed: Set<String>, fresh: Boolean): Map<String, String> {
        val found = synchronized(known) {
            if (fresh) known.remove(playlistId)
            HashMap(known[playlistId].orEmpty())
        }
        if (!found.keys.containsAll(needed)) scan(playlistId, needed, found)
        synchronized(known) { known.getOrPut(playlistId) { HashMap() }.putAll(found) }
        return found
    }

    private suspend fun scan(playlistId: String, needed: Set<String>, found: MutableMap<String, String>) {
        for (query in listOf<(String) -> String>(PostDataHelper::createQueryTV, PostDataHelper::createQueryWeb)) {
            var body = query("\"browseId\":${JsonPrimitive("VL$playlistId")}")
            for (page in 1..MAX_PAGES) {
                val parsed = PlaylistItemIds.parse(runCatching { Innertube.post("browse", body) }.getOrNull() ?: break)
                parsed.setVideoIds.forEach { (video, set) -> found.putIfAbsent(video, set) }
                if (found.keys.containsAll(needed)) return
                // Esse cliente não traz a posição dos vídeos: tenta o próximo.
                if (page == 1 && parsed.setVideoIds.isEmpty()) break
                val token = parsed.continuation ?: break
                body = query("\"continuation\":${JsonPrimitive(token)}")
            }
        }
    }
}

/** Lê, da resposta do YouTube para uma playlist, o setVideoId de cada vídeo e a continuação. */
object PlaylistItemIds {
    class Page(val setVideoIds: Map<String, String>, val continuation: String?)

    /** Os formatos de item de playlist da TV e da web. */
    private val ITEM_KEYS = listOf("tileRenderer", "playlistVideoRenderer", "playlistPanelVideoRenderer", "lockupViewModel")

    fun parse(root: JsonElement): Page {
        val ids = LinkedHashMap<String, String>()
        var continuation: String? = null
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    for (key in ITEM_KEYS) {
                        val item = element[key] as? JsonObject ?: continue
                        val videoId = item.string("videoId") ?: item.string("contentId")
                        val setVideoId = find(item, "setVideoId")
                        if (videoId != null && setVideoId != null) ids.putIfAbsent(videoId, setVideoId)
                    }
                    (element["nextContinuationData"] as? JsonObject)?.string("continuation")?.let { continuation = continuation ?: it }
                    (element["continuationCommand"] as? JsonObject)?.string("token")?.let { continuation = continuation ?: it }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return Page(ids, continuation)
    }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** Primeiro valor de [key] em qualquer nível abaixo de [element]. */
    private fun find(element: JsonElement, key: String): String? = when (element) {
        is JsonObject -> element.string(key) ?: element.values.firstNotNullOfOrNull { find(it, key) }
        is JsonArray -> element.firstNotNullOfOrNull { find(it, key) }
        else -> null
    }
}

/** POST direto no Innertube do YouTube, com o cliente HTTP (e o login) do motor. */
internal object Innertube {
    private const val BASE = "https://www.youtube.com/youtubei/v1/"
    private val json = KJson { ignoreUnknownKeys = true }

    /** O cliente de TV do motor manda o User-Agent e o Referer da TV junto: [TV_HEADERS]. */
    val TV_HEADERS = mapOf("User-Agent" to DefaultHeaders.USER_AGENT_TV, "Referer" to "https://www.youtube.com/tv")

    suspend fun post(endpoint: String, body: String, headers: Map<String, String> = emptyMap()): JsonElement = io {
        val request = Request.Builder()
            .url(BASE + endpoint)
            .post(RequestBody.create(MediaType.parse("application/json"), body))
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        EngineHttp.client().newCall(request).execute().use { response ->
            val text = response.body()?.string().orEmpty()
            check(response.isSuccessful) { "YouTube error ${response.code()}" }
            json.parseToJsonElement(text).jsonObject
        }
    }
}
