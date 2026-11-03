package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ytslim.data.AppScope
import ytslim.data.ExploreDraw
import ytslim.data.ExploreRepository
import ytslim.data.ExploreRules
import ytslim.data.FeedFilter
import ytslim.data.SearchFilters
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.TopicRules
import ytslim.data.Video

/**
 * A aba Explore: vídeos recentes e antigos dos canais que os canais inscritos destacam
 * ([ExploreRepository]). Puxar para atualizar (ou segurar OK na aba, na TV) sorteia outros
 * canais; o fim da lista traz mais canais.
 *
 * Os filtros de data, duração e ordem ficam aqui (pedido do Johan, 2026-11-03: a Explore traz
 * vídeos antigos e variados, e as Recomendações vêm poucas por vez). Cada canal sorteado escolhe
 * os vídeos entre os que passam pelos filtros; se sobrarem poucos na tela, o app sorteia mais
 * canais. Os filtros valem até o app fechar.
 */
class ExploreViewModel : ViewModel(), Refreshable {
    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val draws: List<ExploreDraw>, val channels: Set<String>, val hasMore: Boolean, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    // Um canal bloqueado ou um tópico excluído depois do sorteio sai da tela na hora. Fora da
    // thread da tela: os filtros olham cada vídeo.
    val state: StateFlow<UiState<VideoListContent>> = combine(raw, settings, _filters) { raw, settings, filters ->
        when (raw) {
            Raw.Loading -> UiState.Loading
            is Raw.Error -> UiState.Error(raw.message)
            is Raw.Loaded -> UiState.Ready(VideoListContent(shown(raw.draws, settings, filters), raw.hasMore, raw.loadingMore))
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private var job: Job? = null

    /**
     * Mostrar o aviso que explica a Explore (pedido do Johan, 2026-11-03: só na primeira vez, em
     * vez do texto fixo em cima da lista). Começa false para não piscar para quem já viu.
     */
    val showIntro: StateFlow<Boolean> = SettingsRepository.exploreIntroSeen.map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Fechou o aviso: ele não aparece mais. */
    fun dismissIntro() {
        AppScope.launch { SettingsRepository.setExploreIntroSeen() }
    }

    init {
        load(quiet = false)
    }

    fun reload() = load(quiet = false)

    /** Outro sorteio; a lista de agora fica na tela até a nova chegar. */
    override fun refresh() = load(quiet = raw.value is Raw.Loaded)

    fun setFilters(filters: SearchFilters) {
        if (filters == _filters.value) return
        _filters.value = filters
        // Uma busca de canais em andamento recomeça com o alvo dos filtros novos.
        val loaded = raw.value as? Raw.Loaded
        if (loaded != null && loaded.loadingMore && !_refreshing.value) {
            job?.cancel()
            raw.value = loaded.copy(loadingMore = false)
        }
        fill(extra = 0)
    }

    private fun load(quiet: Boolean) {
        job?.cancel()
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        job = viewModelScope.launch {
            raw.value = try {
                draw(emptySet()).let { Raw.Loaded(listOf(it), it.ids.toSet(), it.hasMore, loadingMore = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (quiet) raw.value else Raw.Error(e.toMessage())
            }
            _refreshing.value = false
            fill(extra = 0)
        }
    }

    /** Fim da lista: mais canais, sem repetir os que já estão na tela. */
    fun loadMore() = fill(extra = MIN_NEW)

    /**
     * Sorteia mais canais até a tela ter pelo menos [MIN_ON_SCREEN] vídeos (e [extra] a mais que
     * agora). Sem filtros, um sorteio basta (são uns 40 vídeos); com filtros, até [MAX_DRAWS_FILTERED].
     */
    private fun fill(extra: Int) {
        val start = raw.value as? Raw.Loaded ?: return
        if (start.loadingMore || _refreshing.value || !start.hasMore) return
        raw.value = start.copy(loadingMore = true)
        job = viewModelScope.launch {
            var current = start
            var draws = 0
            val target = maxOf(MIN_ON_SCREEN, count(current) + extra)
            val limit = if (_filters.value.isDefault) 1 else MAX_DRAWS_FILTERED
            while (current.hasMore && draws < limit && count(current) < target) {
                val next = try {
                    draw(current.channels)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } ?: break
                current = Raw.Loaded(current.draws + next, current.channels + next.ids, next.hasMore, loadingMore = true)
                raw.value = current
                draws++
            }
            raw.value = current.copy(loadingMore = false)
        }
    }

    private fun shown(draws: List<ExploreDraw>, settings: Settings, filters: SearchFilters): List<Video> {
        val keep = FeedFilter.matcher(filters, System.currentTimeMillis())
        val videos = ExploreRules.screen(draws, settings.filters, keep).filterNot { TopicRules.isBlocked(it, settings.filters) }
        return FeedFilter.sort(videos, filters.sort)
    }

    private suspend fun count(loaded: Raw.Loaded): Int =
        withContext(Dispatchers.Default) { shown(loaded.draws, settings.value, _filters.value).size }

    // Em AppScope: a leitura das páginas dos canais (e o cache) termina mesmo saindo da aba.
    private suspend fun draw(shown: Set<String>): ExploreDraw = AppScope.async { ExploreRepository.draw(shown) }.await()

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MIN_NEW = 6
        const val MAX_DRAWS_FILTERED = 4
    }
}
