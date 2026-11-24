package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ytslim.data.AppScope
import ytslim.data.ContentFilters
import ytslim.data.ExploreDraw
import ytslim.data.ExploreRepository
import ytslim.data.ExploreRules
import ytslim.data.FeedFilter
import ytslim.data.SearchFilters
import ytslim.data.SettingsRepository
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
    private val contentFilters = SettingsRepository.settings
        .map { it.filters }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ContentFilters())

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    // Um canal bloqueado ou um tópico excluído depois do sorteio sai da tela na hora. Fora da
    // thread da tela: os filtros olham cada vídeo.
    val state: StateFlow<UiState<VideoListContent>> = combine(raw, contentFilters, _filters) { raw, content, filters ->
        when (raw) {
            Raw.Loading -> UiState.Loading
            is Raw.Error -> UiState.Error(raw.message)
            is Raw.Loaded -> UiState.Ready(VideoListContent(shown(raw.draws, content, filters), raw.hasMore, raw.loadingMore))
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private var loadJob: Job? = null
    private var fillJob: Job? = null

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

    /** A aba já apareceu na tela ([seen]). Antes do init, que o lê (ver o [RecommendedViewModel]). */
    private var seen = false

    init {
        // Abre na hora com o sorteio preparado na abertura anterior (1.0.11, pedido do Johan: logo
        // depois de ligar a TV, a Explore demorava a aparecer). Sem ele, sorteia como antes; como a
        // aba carrega por trás, antes de abrir, um sorteio que ninguém viu fica guardado para a
        // próxima abertura do app, em vez de se perder.
        viewModelScope.launch {
            val saved = ExploreRepository.next()
            if (saved != null && raw.value == Raw.Loading) {
                raw.value = Raw.Loaded(listOf(saved), saved.ids.toSet(), saved.hasMore, loadingMore = false)
                fill(extra = 0)
            } else {
                load(quiet = false)
                val drawn = (raw.first { it !is Raw.Loading } as? Raw.Loaded)?.draws?.firstOrNull()
                if (drawn != null && !seen) ExploreRepository.saveNext(drawn)
            }
        }
    }

    /**
     * A aba apareceu na tela. Só então (uma vez, com a tela já assentada) o app sorteia por trás o
     * que a próxima abertura mostra, sem os canais desta: quem não abre a Explore não gasta pedidos
     * nem canais que nunca viu.
     */
    fun seen() {
        if (seen) return
        seen = true
        AppScope.launch {
            val first = withTimeoutOrNull(NEXT_DRAW_WAIT_MS) { raw.first { it is Raw.Loaded } } as? Raw.Loaded ?: return@launch
            // Até o próximo sorteio ficar pronto (ou se ele falhar), o guardado é o da tela com outra
            // escolha de vídeos e outra ordem: a próxima abertura não repete a tela de agora
            // (sugestão do Johan, 1.0.11).
            first.draws.firstOrNull()?.let { ExploreRepository.saveNext(it.reshuffled()) }
            delay(NEXT_DRAW_DELAY_MS)
            runCatching { ExploreRepository.prepareNext((raw.value as? Raw.Loaded)?.channels ?: first.channels) }
        }
    }

    fun reload() = load(quiet = false)

    /** Outro sorteio; a lista de agora fica na tela até a nova chegar. */
    override fun refresh() = load(quiet = raw.value is Raw.Loaded)

    fun setFilters(filters: SearchFilters) {
        if (filters == _filters.value) return
        _filters.value = filters
        // Uma busca de canais em andamento recomeça com o alvo dos filtros novos.
        val loaded = raw.value as? Raw.Loaded
        if (loaded != null && (loaded.loadingMore || fillJob?.isActive == true) && !_refreshing.value) {
            fillJob?.cancel()
            if (loaded.loadingMore) raw.value = loaded.copy(loadingMore = false)
        }
        fill(extra = 0)
    }

    private fun load(quiet: Boolean) {
        loadJob?.cancel()
        fillJob?.cancel()
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        loadJob = viewModelScope.launch {
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
        if (start.loadingMore || _refreshing.value || !start.hasMore || fillJob?.isActive == true) return
        fillJob = viewModelScope.launch {
            var current = start
            var have = count(current)
            val target = maxOf(MIN_ON_SCREEN, have + extra)
            if (have >= target) return@launch
            ensureActive()
            current = current.copy(loadingMore = true)
            raw.value = current
            var draws = 0
            val limit = if (_filters.value.isDefault) 1 else MAX_DRAWS_FILTERED
            while (current.hasMore && draws < limit && have < target) {
                val next = try {
                    draw(current.channels)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } ?: break
                ensureActive()
                current = Raw.Loaded(current.draws + next, current.channels + next.ids, next.hasMore, loadingMore = true)
                raw.value = current
                draws++
                if (current.hasMore && draws < limit) have = count(current)
            }
            raw.value = current.copy(loadingMore = false)
        }
    }

    private fun shown(draws: List<ExploreDraw>, content: ContentFilters, filters: SearchFilters): List<Video> {
        val keep = FeedFilter.matcher(filters, System.currentTimeMillis())
        // ExploreRules.pick já filtra canais bloqueados e tópicos excluídos via TopicRules.isExcluded.
        val videos = ExploreRules.screen(draws, content, keep)
        return FeedFilter.sort(videos, filters.sort)
    }

    private suspend fun count(loaded: Raw.Loaded): Int =
        withContext(Dispatchers.Default) { shown(loaded.draws, contentFilters.value, _filters.value).size }

    // Em AppScope: a leitura das páginas dos canais (e o cache) termina mesmo saindo da aba.
    private suspend fun draw(shown: Set<String>): ExploreDraw = AppScope.async { ExploreRepository.draw(shown) }.await()

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MIN_NEW = 6
        const val MAX_DRAWS_FILTERED = 4

        /** Quanto esperar para preparar o sorteio da próxima abertura (as miniaturas da tela vêm antes). */
        const val NEXT_DRAW_DELAY_MS = 5_000L

        /** Quanto esperar pela lista da tela antes de desistir de preparar a próxima. */
        const val NEXT_DRAW_WAIT_MS = 30_000L
    }
}
