package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ytslim.data.AppScope
import ytslim.data.AuthRepository
import ytslim.data.BlockedChannel
import ytslim.data.ChannelFeedback
import ytslim.data.ChannelRepository
import ytslim.data.ContentFilters
import ytslim.data.FeedRepository
import ytslim.data.LibraryRepository
import ytslim.data.Playlist
import ytslim.data.SearchDuration
import ytslim.data.SearchFilters
import ytslim.data.SearchItem
import ytslim.data.SearchPage
import ytslim.data.SearchRepository
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.TopicRules
import ytslim.data.Video
import ytslim.data.VideoActions
import ytslim.data.VideoPage

/**
 * Recomendações da conta, com os filtros do BeamTube: sem Shorts, sem lives, sem canais bloqueados
 * e sem tópicos excluídos; tópicos preferidos sobem (ver [TopicRules.recommend]). Os filtros de
 * data, duração e ordem saíram daqui para a Explore (pedido do Johan, 2026-11-03: as
 * Recomendações vêm poucas por vez). Cada página passa antes pelo [ChannelFeedback], que manda o
 * "Don't recommend channel" dos canais bloqueados.
 */
class RecommendedViewModel : ViewModel(), Refreshable {
    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val pages: List<List<Video>>, val lastPage: VideoPage, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    // Os filtros olham o título de cada vídeo: fora da thread da tela, para a rolagem não engasgar.
    val state: StateFlow<UiState<VideoListContent>> = combine(raw, settings, ::content)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private fun shown(pages: List<List<Video>>, settings: Settings): List<Video> = TopicRules.recommend(pages, settings.filters)

    private fun content(raw: Raw, settings: Settings): UiState<VideoListContent> = when (raw) {
        Raw.Loading -> UiState.Loading
        is Raw.Error -> UiState.Error(raw.message)
        is Raw.Loaded -> UiState.Ready(VideoListContent(shown(raw.pages, settings), raw.lastPage.hasMore, raw.loadingMore))
    }

    private var job: Job? = null
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        reload()
    }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = raw.value is Raw.Loaded)

    private fun load(quiet: Boolean) {
        job?.cancel()
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { LibraryRepository.recommended() }
                .onSuccess { ChannelFeedback.note(it.videos) }
                .fold({ Raw.Loaded(listOf(it.videos), it, loadingMore = false) }, { Raw.Error(it.toMessage()) })
            _refreshing.value = false
            fill(extra = 0)
        }
    }

    /** Fim da lista na tela: busca a próxima página (e mais, se os filtros deixarem poucos vídeos). */
    fun loadMore() = fill(extra = MIN_NEW)

    /** Busca páginas até a tela ter pelo menos [MIN_ON_SCREEN] vídeos (e [extra] a mais que agora), até [MAX_PAGES]. */
    private fun fill(extra: Int) {
        val start = raw.value as? Raw.Loaded ?: return
        if (start.loadingMore || _refreshing.value || !start.lastPage.hasMore) return
        raw.value = start.copy(loadingMore = true)
        job = viewModelScope.launch {
            var current = start
            var pages = 0
            val target = maxOf(MIN_ON_SCREEN, count(current) + extra)
            while (current.lastPage.hasMore && pages < MAX_PAGES && count(current) < target) {
                val next = runCatching { LibraryRepository.more(current.lastPage) }.getOrNull()
                // Um recarregar cancelou esta busca: não escreve por cima dele.
                ensureActive()
                next?.let { ChannelFeedback.note(it.videos) }
                current = Raw.Loaded(current.pages + listOf(next?.videos.orEmpty()), next ?: VideoPage(emptyList(), null), loadingMore = true)
                raw.value = current
                pages++
            }
            raw.value = current.copy(loadingMore = false)
        }
    }

    private suspend fun count(loaded: Raw.Loaded): Int =
        withContext(Dispatchers.Default) { shown(loaded.pages, settings.value).size }

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MIN_NEW = 6
        const val MAX_PAGES = 5
    }
}

/** Histórico da conta (o que foi assistido). */
class HistoryViewModel : VideoListViewModel("history", { LibraryRepository.history() }) {
    /**
     * Tira [video] do histórico: some da tela na hora, e a conta muda quando o prazo do Undo passa
     * (devolve o Undo). Se a conta não mudar, [onError] avisa e a lista volta a ser a da conta.
     */
    fun remove(video: Video, onError: (String) -> Unit): () -> Unit =
        removeLater(video, onError) { LibraryRepository.removeFromHistory(video) }
}

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

    val results: StateFlow<UiState<Results>?> = combine(raw, settings, _filters) { state, settings, filters ->
        if (state !is UiState.Ready) return@combine state
        UiState.Ready(state.data.copy(items = shown(state.data.items, settings, filters)))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun shown(items: List<SearchItem>, settings: Settings, filters: SearchFilters) =
        items.filter { it.isAllowed(settings.filters, filters.duration) }

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
        autoPages = 0
        raw.value = UiState.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { SearchRepository.search(query, _filters.value) }.fold(
                { page ->
                    lastPage = page
                    UiState.Ready(Results(query, page.items.distinctBy { it.key }, page.hasMore, loadingMore = false))
                },
                { UiState.Error(it.toMessage()) },
            )
            fillScreen()
        }
    }

    fun setFilters(filters: SearchFilters) {
        if (filters == _filters.value) return
        _filters.value = filters
        (raw.value as? UiState.Ready)?.data?.query?.let { search(it) }
    }

    fun loadMore() {
        autoPages = 0
        more()
    }

    private fun more() {
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
            fillScreen()
        }
    }

    private var autoPages = 0

    /** Com "30 min ou mais" (e afins) o app corta resultados: se sobrou pouco, busca mais páginas sozinho. */
    private fun fillScreen() {
        val data = (raw.value as? UiState.Ready)?.data ?: return
        if (!_filters.value.duration.checkedLocally || !data.hasMore || autoPages >= MAX_AUTO_PAGES) return
        if (shown(data.items, settings.value, _filters.value).size >= MIN_ON_SCREEN) return
        autoPages++
        more()
    }

    private fun SearchItem.isAllowed(filters: ContentFilters, duration: SearchDuration): Boolean = when (this) {
        is SearchItem.VideoItem -> !video.isShorts && !TopicRules.isBlocked(video, filters) &&
            (!duration.checkedLocally || duration.matches(video.durationMs))
        is SearchItem.ChannelItem -> channel.id !in filters.blockedIds
        is SearchItem.PlaylistItem -> true
    }

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MAX_AUTO_PAGES = 5
    }
}

/** Página do canal: vídeos (paginados), playlists, a inscrição e o bloqueio do canal. */
class ChannelViewModel(private val channelId: String, private val name: String) : ViewModel(), Refreshable {
    data class Content(val videos: VideoListContent, val playlists: List<Playlist>)

    private val _state = MutableStateFlow<UiState<Content>>(UiState.Loading)
    val state: StateFlow<UiState<Content>> = _state.asStateFlow()
    private var lastPage: VideoPage? = null
    private var job: Job? = null
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val blocked: StateFlow<Boolean> = SettingsRepository.settings
        .map { s -> s.filters.blocked.any { it.id == channelId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _subscription = MutableStateFlow<Subscription>(Subscription.Hidden)
    /** Subscribe / Subscribed (pedido do Johan, 2026-11-04): escondido até saber se a conta está inscrita. */
    val subscription: StateFlow<Subscription> = _subscription.asStateFlow()

    init {
        reload()
        // A página do canal que o motor lê não diz se a conta está inscrita: a lista de canais
        // inscritos (a mesma das Inscrições e da Explore) diz. Sem ela, o botão não aparece.
        viewModelScope.launch {
            if (!AuthRepository.isSigned()) return@launch
            val ids = FeedRepository.subscribedChannelIds() ?: return@launch
            _subscription.value = Subscription.Shown(subscribed = channelId in ids)
        }
    }

    fun toggleSubscribed() {
        val current = _subscription.value as? Subscription.Shown ?: return
        if (current.busy) return
        val to = !current.subscribed
        _subscription.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            _subscription.value = runCatching { VideoActions.setSubscribed(channelId, to) }.fold(
                { Subscription.Shown(subscribed = to) },
                { current.copy(busy = false, error = it.toMessage()) },
            )
        }
    }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = _state.value is UiState.Ready)

    private fun load(quiet: Boolean) {
        job?.cancel()
        _refreshing.value = quiet
        if (!quiet) _state.value = UiState.Loading
        job = viewModelScope.launch {
            _state.value = runCatching { ChannelRepository.channel(channelId) }.fold(
                { page ->
                    lastPage = page.videos
                    val videos = page.videos.videos.filterNot { it.isShorts }
                    UiState.Ready(Content(VideoListContent(videos, page.videos.hasMore, loadingMore = false), page.playlists))
                },
                { UiState.Error(it.toMessage()) },
            )
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        val page = lastPage ?: return
        if (!page.hasMore || current.videos.loadingMore || _refreshing.value) return
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

    /** O que o toque no bloquear fez, para o aviso ([blockNotice]). */
    enum class BlockOutcome { BLOCKED_HERE, BLOCKED_AND_ACCOUNT, UNBLOCKED, UNBLOCKED_ACCOUNT_STAYS }

    /**
     * Bloqueia o canal neste aparelho (na hora) e manda o "Don't recommend channel" para a conta
     * ([ChannelFeedback]), ou desbloqueia. [onDone] recebe o resultado para o aviso, que espera a
     * resposta do YouTube por até [ACCOUNT_WAIT_MS]. Roda no [AppScope]: sair da página logo depois
     * do toque não cancela o pedido.
     */
    fun toggleBlocked(onDone: (BlockOutcome) -> Unit) {
        val wasBlocked = blocked.value
        AppScope.launch {
            if (wasBlocked) {
                val onAccount = SettingsRepository.current().filters.blocked.any { it.id == channelId && it.onAccount }
                SettingsRepository.unblock(channelId)
                onDone(if (onAccount) BlockOutcome.UNBLOCKED_ACCOUNT_STAYS else BlockOutcome.UNBLOCKED)
            } else {
                val channel = BlockedChannel(channelId, name)
                SettingsRepository.block(channel)
                val sending = async { ChannelFeedback.dontRecommend(channel) }
                val sent = withTimeoutOrNull(ACCOUNT_WAIT_MS) { sending.await() } == true
                onDone(if (sent) BlockOutcome.BLOCKED_AND_ACCOUNT else BlockOutcome.BLOCKED_HERE)
            }
        }
    }

    private companion object {
        const val ACCOUNT_WAIT_MS = 5_000L
    }
}
