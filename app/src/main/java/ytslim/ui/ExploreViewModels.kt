package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ytslim.data.AppScope
import ytslim.data.ExploreDraw
import ytslim.data.ExploreRepository
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.TopicRules
import ytslim.data.Video

/**
 * A aba Explore: vídeos recentes e antigos dos canais que os canais inscritos destacam
 * ([ExploreRepository]). Puxar para atualizar (ou segurar OK na aba, na TV) sorteia outros
 * canais; o fim da lista traz mais canais.
 */
class ExploreViewModel : ViewModel(), Refreshable {
    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val videos: List<Video>, val channels: Set<String>, val hasMore: Boolean, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    // Um canal bloqueado depois do sorteio sai da tela na hora.
    val state: StateFlow<UiState<VideoListContent>> = combine(raw, settings) { raw, settings ->
        when (raw) {
            Raw.Loading -> UiState.Loading
            is Raw.Error -> UiState.Error(raw.message)
            is Raw.Loaded -> UiState.Ready(
                VideoListContent(raw.videos.filterNot { TopicRules.isBlocked(it, settings.filters) }, raw.hasMore, raw.loadingMore),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

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

    private fun load(quiet: Boolean) {
        job?.cancel()
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { draw(emptySet()) }.fold(
                { Raw.Loaded(it.videos, it.channels.toSet(), it.hasMore, loadingMore = false) },
                { if (quiet) raw.value else Raw.Error(it.toMessage()) },
            )
            _refreshing.value = false
        }
    }

    /** Fim da lista: mais canais, sem repetir os que já estão na tela. */
    fun loadMore() {
        val loaded = raw.value as? Raw.Loaded ?: return
        if (!loaded.hasMore || loaded.loadingMore || _refreshing.value) return
        raw.value = loaded.copy(loadingMore = true)
        job = viewModelScope.launch {
            val next = runCatching { draw(loaded.channels) }.getOrNull()
            val known = loaded.videos.mapTo(HashSet()) { it.id }
            raw.value = Raw.Loaded(
                videos = loaded.videos + next?.videos.orEmpty().filter { known.add(it.id) },
                channels = loaded.channels + next?.channels.orEmpty(),
                hasMore = next?.hasMore == true,
                loadingMore = false,
            )
        }
    }

    // Em AppScope: a leitura das páginas dos canais (e o cache) termina mesmo saindo da aba.
    private suspend fun draw(shown: Set<String>): ExploreDraw = AppScope.async { ExploreRepository.draw(shown) }.await()
}
