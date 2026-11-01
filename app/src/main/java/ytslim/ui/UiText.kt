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

/** Um filtro da busca na tela: o nome e as opções, cada uma já com os filtros que ela aplica. */
class SearchFilterChoice(@StringRes val label: Int, val options: List<Option>) {
    class Option(@StringRes val label: Int, val filters: SearchFilters, val selected: Boolean)

    val selected: Option get() = options.first { it.selected }

    /** A primeira opção é a de "sem filtro". */
    val isDefault: Boolean get() = options.first().selected

    /** O próximo valor (na TV, o botão vai passando pelos valores). */
    fun next(): SearchFilters = options[(options.indexOf(selected) + 1) % options.size].filters
}

private fun <T> choice(@StringRes label: Int, current: T, values: List<Pair<T, Int>>, apply: (T) -> SearchFilters) =
    SearchFilterChoice(label, values.map { (value, text) -> SearchFilterChoice.Option(text, apply(value), value == current) })

/** Os quatro filtros da busca, na ordem da tela. */
fun searchFilterChoices(filters: SearchFilters): List<SearchFilterChoice> = listOf(
    choice(
        R.string.search_filter_date,
        filters.date,
        listOf(
            UploadDate.ANY to R.string.date_any,
            UploadDate.HOUR to R.string.date_hour,
            UploadDate.TODAY to R.string.date_today,
            UploadDate.WEEK to R.string.date_week,
            UploadDate.MONTH to R.string.date_month,
            UploadDate.YEAR to R.string.date_year,
        ),
    ) { filters.copy(date = it) },
    choice(
        R.string.search_filter_duration,
        filters.duration,
        listOf(
            SearchDuration.ANY to R.string.duration_any,
            SearchDuration.UNDER_4 to R.string.duration_under_4,
            SearchDuration.FROM_4_TO_20 to R.string.duration_4_20,
            SearchDuration.OVER_20 to R.string.duration_over_20,
        ),
    ) { filters.copy(duration = it) },
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
