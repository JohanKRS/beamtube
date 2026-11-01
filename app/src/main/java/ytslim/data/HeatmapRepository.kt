package ytslim.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gráfico de "mais repetidos" de um vídeo.
 *
 * O motor não expõe esse gráfico, então o app pede a página "next" do cliente WEB, sem login
 * (não mexe na conta nem no histórico), e lê só os marcadores do tipo heatmap.
 */
object HeatmapRepository {
    private const val ENDPOINT = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false"

    // Versão do cliente WEB que o motor usa (CLIENTS.WEB.VERSION); o YouTube aceita versões antigas.
    private const val WEB_VERSION = "2.20260907.06.00"
    private const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true }

    /** Lista vazia se o vídeo não tem gráfico (poucos views) ou se a busca falhar. */
    suspend fun load(videoId: String): List<HeatSegment> = io {
        runCatching { parse(fetch(videoId)) }.getOrDefault(emptyList())
    }

    private fun fetch(videoId: String): String {
        val body = """{"context":{"client":{"clientName":"WEB","clientVersion":"$WEB_VERSION","hl":"en","gl":"US"}},"videoId":"$videoId"}"""
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", BROWSER_UA)
            connection.outputStream.use { it.write(body.toByteArray()) }
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(text: String): List<HeatSegment> {
        val root = json.parseToJsonElement(text).jsonObject
        val mutations = root.path("frameworkUpdates", "entityBatchUpdate", "mutations") as? JsonArray ?: return emptyList()
        val markers = mutations.firstNotNullOfOrNull { mutation ->
            val list = mutation.path("payload", "macroMarkersListEntity", "markersList") as? JsonObject
            list?.takeIf { it["markerType"]?.jsonPrimitive?.content == "MARKER_TYPE_HEATMAP" }?.get("markers")?.jsonArray
        } ?: return emptyList()
        return markers.mapNotNull { marker ->
            val m = marker as? JsonObject ?: return@mapNotNull null
            HeatSegment(
                startMs = m["startMillis"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null,
                durationMs = m["durationMillis"]?.jsonPrimitive?.longOrNull ?: 0,
                intensity = m["intensityScoreNormalized"]?.jsonPrimitive?.floatOrNull ?: 0f,
            )
        }.sortedBy { it.startMs }
    }

    private fun JsonElement.path(vararg keys: String): JsonElement? =
        keys.fold(this as JsonElement?) { node, key -> (node as? JsonObject)?.get(key) }
}
