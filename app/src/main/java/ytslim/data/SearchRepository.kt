package ytslim.data

import android.util.Base64
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * Filtros da busca. Cada valor é o número que o próprio YouTube usa no parâmetro de filtros
 * (o "sp" da busca no site); 0 é "sem filtro".
 */
enum class UploadDate(val code: Int) { ANY(0), HOUR(1), TODAY(2), WEEK(3), MONTH(4), YEAR(5) }

enum class SearchDuration(val code: Int) { ANY(0), UNDER_4(1), FROM_4_TO_20(3), OVER_20(2) }

enum class ResultType(val code: Int) { ANY(0), VIDEO(1), CHANNEL(2), PLAYLIST(3) }

enum class SortBy(val code: Int) { RELEVANCE(0), RATING(1), DATE(2), VIEWS(3) }

data class SearchFilters(
    val date: UploadDate = UploadDate.ANY,
    val duration: SearchDuration = SearchDuration.ANY,
    val type: ResultType = ResultType.ANY,
    val sort: SortBy = SortBy.RELEVANCE,
)

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
        val fields = "\"query\":" + JsonPrimitive(query) + (params?.let { ",\"params\":\"$it\"" } ?: "")
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
