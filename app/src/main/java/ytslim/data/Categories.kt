package ytslim.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

/**
 * Uma categoria do Explore: um nome e palavras-chave do próprio usuário. Abrir a categoria faz uma
 * busca nova no YouTube por palavra-chave, com os filtros guardados nela (não é um filtro em cima
 * das Recomendações).
 *
 * @param thumbnails até 4 miniaturas da última busca, para o mosaico do cartão.
 */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val sort: SortBy = SortBy.RELEVANCE,
    val duration: SearchDuration = SearchDuration.ANY,
    val date: UploadDate = UploadDate.ANY,
    val beforeYear: Int? = null,
    val thumbnails: List<String> = emptyList(),
) {
    /** Os filtros como a tela os mostra (o tipo não entra: a categoria só traz vídeos). */
    val filters: SearchFilters get() = SearchFilters(date = date, duration = duration, sort = sort, beforeYear = beforeYear)

    fun withFilters(f: SearchFilters) = copy(sort = f.sort, duration = f.duration, date = f.date, beforeYear = f.beforeYear)
}

/** As regras puras do Explore (testadas sem rede). */
object CategoryRules {
    const val MAX_THUMBNAILS = 4

    /**
     * Junta os resultados de cada palavra-chave alternando entre elas (o 1º de cada, depois o 2º…),
     * para nenhuma palavra tomar a lista inteira. Um vídeo que aparece em mais de uma fica só na
     * primeira posição.
     */
    fun interleave(lists: List<List<Video>>): List<Video> {
        val seen = HashSet<String>()
        val out = ArrayList<Video>()
        val longest = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) {
            for (list in lists) {
                val video = list.getOrNull(i) ?: continue
                if (seen.add(video.id)) out += video
            }
        }
        return out
    }

    /** As miniaturas do mosaico: as primeiras diferentes da lista. */
    fun thumbnails(videos: List<Video>): List<String> =
        videos.mapNotNull { it.thumbnailUrl }.distinct().take(MAX_THUMBNAILS)

    /** As palavras-chave: separadas por vírgula, sem repetir (sem diferença de maiúsculas). */
    fun keywords(text: String): List<String> =
        text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
}

/**
 * A busca de uma categoria. [pages] guarda a última página de cada palavra-chave (null quando a
 * busca dela falhou ou acabou), para buscar mais de cada uma.
 */
class CategoryResults(val videos: List<Video>, internal val pages: List<SearchPage?>) {
    val hasMore: Boolean get() = pages.any { it?.hasMore == true }
}

object CategorySearch {
    /** Uma busca por palavra-chave, ao mesmo tempo. Só falha se todas falharem. */
    suspend fun first(category: Category): CategoryResults = coroutineScope {
        val filters = category.filters.copy(type = ResultType.VIDEO)
        val results = category.keywords.map { keyword ->
            async { runCatching { SearchRepository.search(keyword, filters) } }
        }.awaitAll()
        if (results.isNotEmpty() && results.all { it.isFailure }) results.first().getOrThrow()
        val pages = results.map { it.getOrNull() }
        CategoryResults(CategoryRules.interleave(pages.map { it?.videos().orEmpty() }), pages)
    }

    /** A próxima página de cada palavra-chave que ainda tem mais, alternadas como na primeira. */
    suspend fun more(results: CategoryResults): CategoryResults = coroutineScope {
        val next = results.pages.map { page ->
            async { if (page?.hasMore == true) runCatching { SearchRepository.more(page) }.getOrNull() else null }
        }.awaitAll()
        val added = CategoryRules.interleave(next.map { it?.videos().orEmpty() })
        val known = results.videos.mapTo(HashSet()) { it.id }
        CategoryResults(results.videos + added.filter { known.add(it.id) }, next)
    }

    private fun SearchPage.videos(): List<Video> = items.mapNotNull { (it as? SearchItem.VideoItem)?.video }
}
