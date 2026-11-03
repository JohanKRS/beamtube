package ytslim.data

import android.util.Base64
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * Filtros da busca (e da Explore, onde o app aplica o mesmo filtro localmente: ver
 * [FeedFilter]). Cada [code] é o número que o próprio YouTube usa no parâmetro de filtros
 * (o "sp" da busca no site); 0 é "sem filtro".
 */
enum class UploadDate(val code: Int, internal val maxAgeMs: Long?) {
    ANY(0, null),
    HOUR(1, HOUR_MS),
    TODAY(2, DAY_MS),
    WEEK(3, 7 * DAY_MS),
    MONTH(4, 30 * DAY_MS),
    YEAR(5, 365 * DAY_MS),
}

/**
 * Duração. O YouTube só filtra por menos de 4, 4 a 20 e mais de 20 minutos; "30, 45 e 60 minutos
 * ou mais" pedem ao YouTube "mais de 20" e o app corta o resto pela duração de cada vídeo.
 */
enum class SearchDuration(val code: Int, private val minMs: Long, private val maxMs: Long) {
    ANY(0, 0, Long.MAX_VALUE),
    UNDER_4(1, 1, 4 * MINUTE_MS - 1),
    FROM_4_TO_20(3, 4 * MINUTE_MS, 20 * MINUTE_MS),
    OVER_20(2, 20 * MINUTE_MS + 1, Long.MAX_VALUE),
    AT_LEAST_30(2, 30 * MINUTE_MS, Long.MAX_VALUE),
    AT_LEAST_45(2, 45 * MINUTE_MS, Long.MAX_VALUE),
    AT_LEAST_60(2, 60 * MINUTE_MS, Long.MAX_VALUE),
    ;

    /** O vídeo tem esta duração? Duração desconhecida (0) só passa sem filtro. */
    fun matches(durationMs: Long): Boolean = this == ANY || durationMs in minMs..maxMs

    /** O filtro do YouTube não basta: o app confere a duração de cada resultado. */
    val checkedLocally: Boolean get() = minMs > OVER_20.minMs
}

enum class ResultType(val code: Int) { ANY(0), VIDEO(1), CHANNEL(2), PLAYLIST(3) }

enum class SortBy(val code: Int) { RELEVANCE(0), RATING(1), DATE(2), VIEWS(3) }

/**
 * @param beforeYear só vídeos publicados antes de 1º de janeiro deste ano (ex.: 2022). Fica no
 * lugar do [date]: os dois não se combinam.
 */
data class SearchFilters(
    val date: UploadDate = UploadDate.ANY,
    val duration: SearchDuration = SearchDuration.ANY,
    val type: ResultType = ResultType.ANY,
    val sort: SortBy = SortBy.RELEVANCE,
    val beforeYear: Int? = null,
) {
    val isDefault: Boolean get() = this == SearchFilters()
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/**
 * O parâmetro de filtros do YouTube: um protobuf pequeno em base64.
 * Campo 1 = ordem; campo 2 = filtros (1 data, 2 tipo, 3 duração). Qualquer combinação vale,
 * ao contrário da tabela do motor, que só tem algumas.
 */
object SearchParams {
    fun of(filters: SearchFilters): String? {
        val inner = ByteArrayOutputStream()
        listOf(1 to filters.date.code, 2 to filters.type.code, 3 to filters.duration.code).forEach { (field, value) ->
            if (value != 0) inner.varintField(field, value)
        }
        val outer = ByteArrayOutputStream()
        if (filters.sort.code != 0) outer.varintField(1, filters.sort.code)
        if (inner.size() > 0) {
            outer.write(2 shl 3 or 2)
            outer.varint(inner.size())
            outer.write(inner.toByteArray())
        }
        if (outer.size() == 0) return null
        // Mesmo formato do motor: base64 com o "=" escapado.
        return Base64.encodeToString(outer.toByteArray(), Base64.NO_WRAP).replace("=", "%3D")
    }

    /**
     * O texto buscado. "Antes de um ano" não existe no parâmetro de filtros, mas a busca do YouTube
     * entende o operador "before:AAAA-MM-DD" no próprio texto (testado com os clientes TV e WEB).
     */
    fun query(text: String, filters: SearchFilters): String =
        filters.beforeYear?.let { "$text before:$it-01-01" } ?: text

    private fun ByteArrayOutputStream.varintField(field: Int, value: Int) {
        write(field shl 3)
        varint(value)
    }

    private fun ByteArrayOutputStream.varint(value: Int) {
        var v = value
        while (v >= 0x80) {
            write(v and 0x7F or 0x80)
            v = v ushr 7
        }
        write(v)
    }
}

/** Uma página de resultados. [next] é a continuação opaca do motor. */
class SearchPage(val items: List<SearchItem>, internal val next: MediaGroup?) {
    val hasMore: Boolean get() = next != null
}

/** Busca do YouTube, com os filtros e as sugestões enquanto se digita. */
object SearchRepository {
    private const val MAX_SUGGESTIONS = 8

    suspend fun search(query: String, filters: SearchFilters): SearchPage {
        val params = SearchParams.of(filters)
        val fields = "\"query\":" + JsonPrimitive(SearchParams.query(query, filters)) + (params?.let { ",\"params\":\"$it\"" } ?: "")
        val groups = io { EngineSearch.search(PostDataHelper.createQueryTV(fields)) }
        // O primeiro grupo é o dos resultados (com continuação). Os outros são prateleiras
        // ("People also watched", Shorts…), que ficam de fora.
        val main = groups.orEmpty().firstOrNull { it.nextPageKey != null } ?: groups.orEmpty().firstOrNull { !it.isEmpty }
        return main.toSearchPage()
    }

    suspend fun more(page: SearchPage): SearchPage {
        val next = page.next ?: return SearchPage(emptyList(), null)
        return io { Engine.content.continueGroup(next) }.toSearchPage()
    }

    suspend fun suggestions(text: String): List<String> =
        runCatching { Engine.content.getSearchTagsObserve(text).firstOrNull() }.getOrNull().orEmpty().take(MAX_SUGGESTIONS)

    private fun MediaGroup?.toSearchPage() = SearchPage(
        items = this?.mediaItems.orEmpty().mapNotNull { it.toSearchItem() },
        next = this?.takeIf { it.nextPageKey != null },
    )
}
