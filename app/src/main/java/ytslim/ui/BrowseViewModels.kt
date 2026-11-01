package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ytslim.data.BlockedChannel
import ytslim.data.ChannelRepository
import ytslim.data.ContentFilters
import ytslim.data.LibraryRepository
import ytslim.data.Playlist
import ytslim.data.SearchFilters
import ytslim.data.SearchItem
import ytslim.data.SearchPage
import ytslim.data.SearchRepository
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.TopicRules
import ytslim.data.Video
import ytslim.data.VideoPage

/**
 * Recomendações da conta, com os filtros do Doorglow: sem Shorts, sem lives, sem canais bloqueados
 * e sem tópicos excluídos; tópicos preferidos sobem (ver [TopicRules.recommend]).
 */
class RecommendedViewModel : ViewModel() {
    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val pages: List<List<Video>>, val lastPage: VideoPage, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val state: StateFlow<UiState<VideoListContent>> = combine(raw, settings, ::content)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private fun content(raw: Raw, settings: Settings): UiState<VideoListContent> = when (raw) {
        Raw.Loading -> UiState.Loading
        is Raw.Error -> UiState.Error(raw.message)
        is Raw.Loaded -> UiState.Ready(
            VideoListContent(TopicRules.recommend(raw.pages, settings.filters), raw.lastPage.hasMore, raw.loadingMore),
        )
    }

    private var job: Job? = null
    private var autoPages = 0

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        autoPages = 0
        raw.value = Raw.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { LibraryRepository.recommended() }
                .fold({ Raw.Loaded(listOf(it.videos), it, loadingMore = false) }, { Raw.Error(it.toMessage()) })
            fillScreen()
        }
    }

    fun loadMore() {
        val loaded = raw.value as? Raw.Loaded ?: return
        if (!loaded.lastPage.hasMore || loaded.loadingMore) return
        raw.value = loaded.copy(loadingMore = true)
        job = viewModelScope.launch {
            val next = runCatching { LibraryRepository.more(loaded.lastPage) }.getOrNull()
            raw.value = Raw.Loaded(loaded.pages + listOf(next?.videos.orEmpty()), next ?: VideoPage(emptyList(), null), false)
            fillScreen()
        }
    }

    /** Se os filtros esvaziaram a tela, busca mais páginas sozinho (até um limite). */
    private fun fillScreen() {
        val loaded = raw.value as? Raw.Loaded ?: return
        val shown = TopicRules.recommend(loaded.pages, settings.value.filters).size
        if (shown < MIN_ON_SCREEN && loaded.lastPage.hasMore && autoPages < MAX_AUTO_PAGES) {
            autoPages++
            loadMore()
        }
    }

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MAX_AUTO_PAGES = 5
    }
}

/** Histórico da conta (o que foi assistido). */
class HistoryViewModel : VideoListViewModel({ LibraryRepository.history() })

/** Busca com filtros. Shorts e canais bloqueados ficam de fora; lives aparecem (só aqui). */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel : ViewModel() {
    data class Results(val query: String, val items: List<SearchItem>, val hasMore: Boolean, val loadingMore: Boolean)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    /** null = nada buscado ainda. */
    private val raw = MutableStateFlow<UiState<Results>?>(null)
    private var lastPage: SearchPage? = null
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val results: StateFlow<UiState<Results>?> = combine(raw, settings) { state, settings ->
        if (state !is UiState.Ready) return@combine state
        UiState.Ready(state.data.copy(items = state.data.items.filter { it.isAllowed(settings.filters) }))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Sugestões do YouTube para o texto digitado (somem quando a busca é feita). */
    val suggestions: StateFlow<List<String>> = _query
        .debounce(250)
        .distinctUntilChanged()
        .mapLatest { text ->
            val done = (raw.value as? UiState.Ready)?.data?.query
            if (text.isBlank() || text == done) emptyList() else SearchRepository.suggestions(text)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var job: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun search(text: String = _query.value) {
        val query = text.trim()
        if (query.isEmpty()) return
        _query.value = query
        job?.cancel()
        raw.value = UiState.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { SearchRepository.search(query, _filters.value) }.fold(
                { page ->
                    lastPage = page
                    UiState.Ready(Results(query, page.items.distinctBy { it.key }, page.hasMore, loadingMore = false))
                },
                { UiState.Error(it.toMessage()) },
            )
        }
    }

    fun setFilters(filters: SearchFilters) {
        if (filters == _filters.value) return
        _filters.value = filters
        (raw.value as? UiState.Ready)?.data?.query?.let { search(it) }
    }

    fun loadMore() {
        val current = (raw.value as? UiState.Ready)?.data ?: return
        val page = lastPage ?: return
        if (!page.hasMore || current.loadingMore) return
        raw.value = UiState.Ready(current.copy(loadingMore = true))
        job = viewModelScope.launch {
            val next = runCatching { SearchRepository.more(page) }.getOrNull()
            lastPage = next ?: SearchPage(emptyList(), null)
            val known = current.items.mapTo(HashSet()) { it.key }
            val added = next?.items.orEmpty().filter { known.add(it.key) }
            raw.value = UiState.Ready(current.copy(items = current.items + added, hasMore = next?.hasMore == true, loadingMore = false))
        }
    }

    private fun SearchItem.isAllowed(filters: ContentFilters): Boolean = when (this) {
        is SearchItem.VideoItem -> !video.isShorts && !TopicRules.isBlocked(video, filters)
        is SearchItem.ChannelItem -> channel.id !in filters.blockedIds
        is SearchItem.PlaylistItem -> true
    }
}

/** Página do canal: vídeos (paginados), playlists e o bloqueio do canal. */
class ChannelViewModel(private val channelId: String, private val name: String) : ViewModel() {
    data class Content(val videos: VideoListContent, val playlists: List<Playlist>)

    private val _state = MutableStateFlow<UiState<Content>>(UiState.Loading)
    val state: StateFlow<UiState<Content>> = _state.asStateFlow()
    private var lastPage: VideoPage? = null
    private var job: Job? = null

    val blocked: StateFlow<Boolean> = SettingsRepository.settings
        .map { s -> s.filters.blocked.any { it.id == channelId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        _state.value = UiState.Loading
        job = viewModelScope.launch {
            _state.value = runCatching { ChannelRepository.channel(channelId) }.fold(
                { page ->
                    lastPage = page.videos
                    val videos = page.videos.videos.filterNot { it.isShorts }
                    UiState.Ready(Content(VideoListContent(videos, page.videos.hasMore, loadingMore = false), page.playlists))
                },
                { UiState.Error(it.toMessage()) },
            )
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        val page = lastPage ?: return
        if (!page.hasMore || current.videos.loadingMore) return
        _state.value = UiState.Ready(current.copy(videos = current.videos.copy(loadingMore = true)))
        job = viewModelScope.launch {
            val next = runCatching { LibraryRepository.more(page) }.getOrNull()
            lastPage = next ?: VideoPage(emptyList(), null)
            val known = current.videos.videos.mapTo(HashSet()) { it.id }
            val added = next?.videos.orEmpty().filter { !it.isShorts && known.add(it.id) }
            _state.value = UiState.Ready(
                current.copy(videos = current.videos.copy(videos = current.videos.videos + added, hasMore = next?.hasMore == true, loadingMore = false)),
            )
        }
    }

    fun toggleBlocked() {
        viewModelScope.launch {
            if (blocked.value) SettingsRepository.unblock(channelId) else SettingsRepository.block(BlockedChannel(channelId, name))
        }
    }
}
