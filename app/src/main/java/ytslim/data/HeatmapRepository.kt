package ytslim.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** O que a barra de tempo marca: o gráfico de "mais repetidos" e os capítulos. */
data class VideoMarkers(val heat: List<HeatSegment> = emptyList(), val chapters: List<Chapter> = emptyList())

/**
 * Gráfico de "mais repetidos" e capítulos de um vídeo.
 *
 * O motor não expõe o gráfico, e os capítulos dele só vêm com login, então o app pede a página
 * "next" do cliente WEB, sem login (não mexe na conta nem no histórico), e lê os marcadores do
 * tipo heatmap e os capítulos da barra de tempo.
 */
object HeatmapRepository {
    private const val ENDPOINT = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false"

    // Versão do cliente WEB que o motor usa (CLIENTS.WEB.VERSION); o YouTube aceita versões antigas.
    private const val WEB_VERSION = "2.20260907.06.00"
    private const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    private val JSON_MEDIA = MediaType.parse("application/json; charset=utf-8")

    /** Cliente próprio, sem cookies nem login, mas com pool de conexões HTTP/2 entre vídeos. */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Listas vazias se o vídeo não tem gráfico (poucos views) nem capítulos, ou se a busca falhar. */
    suspend fun load(videoId: String): VideoMarkers = io {
        runCatching { fetchAndParse(videoId) }.getOrDefault(VideoMarkers())
    }

    private fun fetchAndParse(videoId: String): VideoMarkers {
        val body = """{"context":{"client":{"clientName":"WEB","clientVersion":"$WEB_VERSION","hl":"en","gl":"US"}},"videoId":"$videoId"}"""
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", BROWSER_UA)
            .post(RequestBody.create(JSON_MEDIA, body))
            .build()
        client.newCall(request).execute().use { response ->
            check(response.code() == 200) { "HTTP ${response.code()}" }
            val stream = response.body()?.byteStream() ?: return VideoMarkers()
            return parse(stream)
        }
    }

    internal fun parse(text: String): VideoMarkers = toMarkers(AppJson.decodeFromString(NextResponse.serializer(), text))

    @OptIn(ExperimentalSerializationApi::class)
    private fun parse(stream: InputStream): VideoMarkers =
        toMarkers(BufferedInputStream(stream).use { AppJson.decodeFromStream(NextResponse.serializer(), it) })

    private fun toMarkers(root: NextResponse): VideoMarkers = VideoMarkers(heat = heat(root), chapters = chapters(root))

    private fun heat(root: NextResponse): List<HeatSegment> {
        val mutations = root.frameworkUpdates?.entityBatchUpdate?.mutations ?: return emptyList()
        val markers = mutations.firstNotNullOfOrNull { mutation ->
            val list = mutation.payload?.macroMarkersListEntity?.markersList
            list?.takeIf { it.markerType == "MARKER_TYPE_HEATMAP" }?.markers
        } ?: return emptyList()
        return markers.mapNotNull { m ->
            HeatSegment(
                startMs = m.startMillis?.longOrNull ?: return@mapNotNull null,
                durationMs = m.durationMillis?.longOrNull ?: 0,
                intensity = m.intensityScoreNormalized?.floatOrNull ?: 0f,
            )
        }.sortedBy { it.startMs }
    }

    /** Os capítulos da barra de tempo (os da descrição ou os automáticos). Um só não conta. */
    private fun chapters(root: NextResponse): List<Chapter> {
        val markers = root.playerOverlays?.playerOverlayRenderer?.decoratedPlayerBarRenderer
            ?.decoratedPlayerBarRenderer?.playerBar?.multiMarkersPlayerBarRenderer?.markersMap
            ?: return emptyList()
        val list = markers.firstNotNullOfOrNull { it.value?.chapters?.takeIf(List<ChapterItem>::isNotEmpty) }
            ?: return emptyList()
        val chapters = list.mapNotNull { item ->
            val chapter = item.chapterRenderer ?: return@mapNotNull null
            Chapter(
                startMs = chapter.timeRangeStartMillis?.longOrNull ?: return@mapNotNull null,
                title = chapter.title?.text() ?: return@mapNotNull null,
            )
        }.sortedBy { it.startMs }
        return if (chapters.size >= 2) chapters else emptyList()
    }

    @Serializable
    private class NextResponse(
        val frameworkUpdates: FrameworkUpdates? = null,
        val playerOverlays: PlayerOverlays? = null,
    )

    @Serializable
    private class FrameworkUpdates(val entityBatchUpdate: EntityBatchUpdate? = null)

    @Serializable
    private class EntityBatchUpdate(val mutations: List<Mutation>? = null)

    @Serializable
    private class Mutation(val payload: MutationPayload? = null)

    @Serializable
    private class MutationPayload(val macroMarkersListEntity: MacroMarkersListEntity? = null)

    @Serializable
    private class MacroMarkersListEntity(val markersList: MarkersList? = null)

    @Serializable
    private class MarkersList(val markerType: String? = null, val markers: List<HeatMarker>? = null)

    @Serializable
    private class HeatMarker(
        val startMillis: JsonPrimitive? = null,
        val durationMillis: JsonPrimitive? = null,
        val intensityScoreNormalized: JsonPrimitive? = null,
    )

    @Serializable
    private class PlayerOverlays(val playerOverlayRenderer: PlayerOverlayRenderer? = null)

    @Serializable
    private class PlayerOverlayRenderer(val decoratedPlayerBarRenderer: DecoratedPlayerBarWrapper? = null)

    @Serializable
    private class DecoratedPlayerBarWrapper(val decoratedPlayerBarRenderer: DecoratedPlayerBar? = null)

    @Serializable
    private class DecoratedPlayerBar(val playerBar: PlayerBar? = null)

    @Serializable
    private class PlayerBar(val multiMarkersPlayerBarRenderer: MultiMarkersPlayerBar? = null)

    @Serializable
    private class MultiMarkersPlayerBar(val markersMap: List<MarkersMapEntry>? = null)

    @Serializable
    private class MarkersMapEntry(val value: MarkersMapValue? = null)

    @Serializable
    private class MarkersMapValue(val chapters: List<ChapterItem>? = null)

    @Serializable
    private class ChapterItem(val chapterRenderer: ChapterRenderer? = null)

    @Serializable
    private class ChapterRenderer(val title: TextNode? = null, val timeRangeStartMillis: JsonPrimitive? = null)

    @Serializable
    private class TextNode(val simpleText: String? = null, val runs: List<TextRun>? = null) {
        fun text(): String? {
            val raw = simpleText ?: runs?.joinToString("") { it.text.orEmpty() }
            return raw?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    @Serializable
    private class TextRun(val text: String? = null)
}
