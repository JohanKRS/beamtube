package ytslim.data

import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
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

/** Salvar com um toque e reordenar vídeos nas playlists do usuário. */
object PlaylistEditor {
    /** Qualquer vídeo serve: a lista de "salvar em" traz as playlists do próprio usuário. */
    private const val PROBE_VIDEO = "jNQXAC9IVRw"

    /** Páginas lidas para achar a posição dos vídeos ao reordenar. */
    private const val MAX_PAGES = 30

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
     * Leva a playlist da ordem [original] para [final] (IDs dos vídeos), com o mínimo de movimentos.
     * O motor não tem "mover": o app pede ao YouTube a posição de cada vídeo na playlist (o
     * setVideoId) e manda os movimentos para o mesmo endpoint que o motor usa para adicionar.
     */
    suspend fun reorder(playlistId: String, original: List<String>, final: List<String>) {
        val moves = ReorderPlan.moves(original, final)
        if (moves.isEmpty()) return
        val needed = moves.flatMapTo(HashSet()) { listOfNotNull(it.videoId, it.successor) }
        val ids = setVideoIds(playlistId, needed)
        check(ids.keys.containsAll(needed)) { "YouTube didn't say where these videos are in the playlist" }
        val actions = buildJsonArray {
            moves.forEach { move ->
                add(
                    buildJsonObject {
                        put("action", "ACTION_MOVE_VIDEO_BEFORE")
                        put("setVideoId", ids.getValue(move.videoId))
                        move.successor?.let { put("movedSetVideoIdSuccessor", ids.getValue(it)) }
                    },
                )
            }
        }
        val result = Innertube.post("browse/edit_playlist", PostDataHelper.createQueryTV("\"playlistId\":${JsonPrimitive(playlistId)},\"actions\":$actions"))
        val status = (result as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull
        check(status == null || status == "STATUS_SUCCEEDED") { "YouTube didn't accept the new order ($status)" }
    }

    /** setVideoId de cada vídeo: primeiro pelo cliente de TV (o do login), depois pelo WEB. */
    private suspend fun setVideoIds(playlistId: String, needed: Set<String>): Map<String, String> {
        val found = HashMap<String, String>()
        for (query in listOf<(String) -> String>(PostDataHelper::createQueryTV, PostDataHelper::createQueryWeb)) {
            var body = query("\"browseId\":${JsonPrimitive("VL$playlistId")}")
            for (page in 1..MAX_PAGES) {
                val parsed = PlaylistItemIds.parse(runCatching { Innertube.post("browse", body) }.getOrNull() ?: break)
                parsed.setVideoIds.forEach { (video, set) -> found.putIfAbsent(video, set) }
                if (found.keys.containsAll(needed)) return found
                // Esse cliente não traz a posição dos vídeos: tenta o próximo.
                if (page == 1 && parsed.setVideoIds.isEmpty()) break
                val token = parsed.continuation ?: break
                body = query("\"continuation\":${JsonPrimitive(token)}")
            }
        }
        return found
    }
}

/** Um movimento: pôr [videoId] logo antes de [successor] (null = no fim). */
data class Move(val videoId: String, val successor: String?)

/**
 * Os movimentos para ir de uma ordem a outra. Os vídeos da maior subsequência que já está na ordem
 * certa ficam parados; cada um dos outros vai para antes do vizinho que fica depois dele.
 */
object ReorderPlan {
    fun moves(original: List<String>, final: List<String>): List<Move> {
        require(original.size == final.size && original.toSet() == final.toSet() && original.toSet().size == original.size) {
            "the new order must have the same videos"
        }
        val position = original.withIndex().associate { it.value to it.index }
        val keep = longestIncreasing(final.map { position.getValue(it) }).map { final[it] }.toHashSet()
        val current = original.toMutableList()
        val moves = mutableListOf<Move>()
        // Da direita para a esquerda: quem fica depois de cada vídeo já está no lugar.
        for (i in final.indices.reversed()) {
            val id = final[i]
            if (id in keep) continue
            current.remove(id)
            val successor = final.getOrNull(i + 1)
            if (successor == null) current.add(id) else current.add(current.indexOf(successor), id)
            moves += Move(id, successor)
        }
        check(current == final)
        return moves
    }

    /** Índices (em [values]) de uma maior subsequência crescente. */
    private fun longestIncreasing(values: List<Int>): List<Int> {
        if (values.isEmpty()) return emptyList()
        val tails = IntArray(values.size)
        val previous = IntArray(values.size) { -1 }
        var length = 0
        for (i in values.indices) {
            var lo = 0
            var hi = length
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (values[tails[mid]] < values[i]) lo = mid + 1 else hi = mid
            }
            if (lo > 0) previous[i] = tails[lo - 1]
            tails[lo] = i
            if (lo == length) length++
        }
        val result = ArrayList<Int>(length)
        var k = tails[length - 1]
        while (k >= 0) {
            result += k
            k = previous[k]
        }
        return result.reversed()
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
