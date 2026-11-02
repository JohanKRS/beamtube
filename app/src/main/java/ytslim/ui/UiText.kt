package ytslim.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ytslim.R
import ytslim.data.ExternalLinks
import ytslim.data.ResultType
import ytslim.data.SearchDuration
import ytslim.data.SearchFilters
import ytslim.data.SortBy
import ytslim.data.UploadDate
import ytslim.data.Video

/** Linha de baixo de um vídeo: de onde ele veio (playlist inscrita) ou a linha do YouTube. */
@Composable
fun subtitleOf(video: Video): String? {
    val playlist = video.fromPlaylist ?: return video.info ?: video.channelName
    return video.channelName?.let { stringResource(R.string.from_playlist, it, playlist) }
        ?: stringResource(R.string.from_playlist_no_channel, playlist)
}

/** As opções de "links de outros apps", na ordem da tela de Configurações. */
val externalLinkOptions = listOf(
    ExternalLinks.NO_HISTORY to R.string.settings_links_no_history,
    ExternalLinks.SAVE_HISTORY to R.string.settings_links_save_history,
    ExternalLinks.ASK to R.string.settings_links_ask,
)

/** Um filtro na tela (busca e Recomendações): o nome e as opções, cada uma já com os filtros que ela aplica. */
class SearchFilterChoice(@StringRes val label: Int, val options: List<Option>) {
    /** [arg] completa o texto da opção ("Before %1$d"). */
    class Option(@StringRes val label: Int, val filters: SearchFilters, val selected: Boolean, val arg: Int? = null)

    val selected: Option get() = options.first { it.selected }

    /** A primeira opção é a de "sem filtro". */
    val isDefault: Boolean get() = options.first().selected
}

/** O texto de uma opção de filtro. */
@Composable
fun SearchFilterChoice.Option.text(): String = arg?.let { stringResource(label, it) } ?: stringResource(label)

private fun <T> choice(@StringRes label: Int, current: T, values: List<Pair<T, Int>>, apply: (T) -> SearchFilters) =
    SearchFilterChoice(label, values.map { (value, text) -> SearchFilterChoice.Option(text, apply(value), value == current) })

/** O ano mais antigo do filtro "Before…" (o YouTube começou em 2005). */
private const val FIRST_YEAR = 2006

/**
 * Data: os períodos recentes do YouTube e, depois deles, "Before <ano>" (do ano atual até 2006),
 * que mostra só vídeos publicados antes de 1º de janeiro daquele ano.
 */
private fun dateChoice(filters: SearchFilters, currentYear: Int): SearchFilterChoice {
    val recent = listOf(
        UploadDate.ANY to R.string.date_any,
        UploadDate.HOUR to R.string.date_hour,
        UploadDate.TODAY to R.string.date_today,
        UploadDate.WEEK to R.string.date_week,
        UploadDate.MONTH to R.string.date_month,
        UploadDate.YEAR to R.string.date_year,
    ).map { (date, text) ->
        SearchFilterChoice.Option(text, filters.copy(date = date, beforeYear = null), filters.beforeYear == null && filters.date == date)
    }
    val years = (currentYear downTo FIRST_YEAR).map { year ->
        SearchFilterChoice.Option(R.string.date_before, filters.copy(date = UploadDate.ANY, beforeYear = year), filters.beforeYear == year, arg = year)
    }
    val chosen = filters.beforeYear?.takeIf { it !in FIRST_YEAR..currentYear }?.let { year ->
        listOf(SearchFilterChoice.Option(R.string.date_before, filters, selected = true, arg = year))
    }.orEmpty()
    return SearchFilterChoice(R.string.search_filter_date, recent + years + chosen)
}

private fun durationChoice(filters: SearchFilters) = choice(
    R.string.search_filter_duration,
    filters.duration,
    listOf(
        SearchDuration.ANY to R.string.duration_any,
        SearchDuration.UNDER_4 to R.string.duration_under_4,
        SearchDuration.FROM_4_TO_20 to R.string.duration_4_20,
        SearchDuration.OVER_20 to R.string.duration_over_20,
        SearchDuration.AT_LEAST_30 to R.string.duration_30_plus,
        SearchDuration.AT_LEAST_45 to R.string.duration_45_plus,
        SearchDuration.AT_LEAST_60 to R.string.duration_60_plus,
    ),
) { filters.copy(duration = it) }

private fun thisYear(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

/** Os quatro filtros da busca, na ordem da tela. */
fun searchFilterChoices(filters: SearchFilters, currentYear: Int = thisYear()): List<SearchFilterChoice> = listOf(
    dateChoice(filters, currentYear),
    durationChoice(filters),
    choice(
        R.string.search_filter_type,
        filters.type,
        listOf(
            ResultType.ANY to R.string.type_any,
            ResultType.VIDEO to R.string.type_video,
            ResultType.CHANNEL to R.string.type_channel,
            ResultType.PLAYLIST to R.string.type_playlist,
        ),
    ) { filters.copy(type = it) },
    choice(
        R.string.search_filter_sort,
        filters.sort,
        listOf(
            SortBy.RELEVANCE to R.string.sort_relevance,
            SortBy.DATE to R.string.sort_date,
            SortBy.VIEWS to R.string.sort_views,
            SortBy.RATING to R.string.sort_rating,
        ),
    ) { filters.copy(sort = it) },
)

/**
 * Os filtros das Recomendações: data, duração e ordem. O tipo não se aplica (só há vídeos), e a
 * nota não vem nas listas do YouTube.
 */
fun feedFilterChoices(filters: SearchFilters, currentYear: Int = thisYear()): List<SearchFilterChoice> = listOf(
    dateChoice(filters, currentYear),
    durationChoice(filters),
    choice(
        R.string.search_filter_sort,
        filters.sort,
        listOf(
            SortBy.RELEVANCE to R.string.sort_relevance,
            SortBy.DATE to R.string.sort_date,
            SortBy.VIEWS to R.string.sort_views,
        ),
    ) { filters.copy(sort = it) },
)

/** Os filtros de uma categoria do Explore, na ordem do desenho: ordem, duração e data (só vídeos, sem o tipo). */
fun categoryFilterChoices(filters: SearchFilters, currentYear: Int = thisYear()): List<SearchFilterChoice> = listOf(
    choice(
        R.string.search_filter_sort,
        filters.sort,
        listOf(
            SortBy.RELEVANCE to R.string.sort_relevance,
            SortBy.DATE to R.string.sort_date,
            SortBy.VIEWS to R.string.sort_views,
            SortBy.RATING to R.string.sort_rating,
        ),
    ) { filters.copy(sort = it) },
    durationChoice(filters),
    dateChoice(filters, currentYear),
)
