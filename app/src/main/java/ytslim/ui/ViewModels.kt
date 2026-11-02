package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ytslim.data.AppIcon
import ytslim.data.AppScope
import ytslim.data.AppIcons
import ytslim.data.AuthRepository
import ytslim.data.AuthRepository.SignInEvent
import ytslim.data.ExternalLinks
import ytslim.data.FeedMerge
import ytslim.data.FeedRepository
import ytslim.data.FeedRules
import ytslim.data.LibraryRepository
import ytslim.data.OwnPlaylist
import ytslim.data.Playlist
import ytslim.data.PlaylistEditor
import ytslim.data.PlaylistSubscriptions
import ytslim.data.SaveTarget
import ytslim.data.SavedVideos
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.Timed
import ytslim.data.Topic
import ytslim.data.TopicRules
import ytslim.data.Video
import ytslim.data.VideoPage

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * Uma lista que dá para recarregar sem tirar da tela o que já está nela: puxar para atualizar, no
 * celular e no tablet. Se a lista ainda não carregou, [refresh] é o mesmo que carregar de novo.
 */
interface Refreshable {
    val refreshing: StateFlow<Boolean>
    fun refresh()
}

internal fun Throwable.toMessage(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

class SignInViewModel : ViewModel() {
    sealed interface State {
        data object Requesting : State
        data class ShowCode(val code: String) : State
        data object SignedIn : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Requesting)
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    init {
        requestCode()
    }

    fun requestCode() {
        job?.cancel()
        _state.value = State.Requesting
        job = viewModelScope.launch {
            AuthRepository.signIn()
                .catch { _state.value = State.Error(it.toMessage()) }
                .collect { event ->
                    _state.value = when (event) {
                        is SignInEvent.Code -> State.ShowCode(event.code)
                        SignInEvent.Done -> State.SignedIn
                    }
                }
        }
    }
}

/** Uma lista de vídeos na tela, com paginação. */
data class VideoListContent(val videos: List<Video>, val hasMore: Boolean, val loadingMore: Boolean, val title: String? = null)

/**
 * Feed de Inscrições: o feed dos canais mais os vídeos novos das playlists inscritas,
 * filtrado pelas regras (sem Shorts; lives só com o toggle).
 */
class FeedViewModel : ViewModel(), Refreshable {
    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val feed: List<Timed>, val lastPage: VideoPage, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val extras = MutableStateFlow<List<Timed>>(emptyList())
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val state: StateFlow<UiState<VideoListContent>> = combine(raw, extras, settings, ::content)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private fun content(raw: Raw, extras: List<Timed>, settings: Settings): UiState<VideoListContent> = when (raw) {
        Raw.Loading -> UiState.Loading
        is Raw.Error -> UiState.Error(raw.message)
        is Raw.Loaded -> {
            val keep = { t: Timed -> FeedRules.keep(t.video, settings.showLives) && !TopicRules.isBlocked(t.video, settings.filters) }
            // Vídeos de playlists inscritas também respeitam os tópicos excluídos.
            val keepExtra = { t: Timed -> keep(t) && !TopicRules.isExcluded(t.video, settings.filters) }
            val videos = FeedMerge.merge(raw.feed.filter(keep), extras.filter(keepExtra), feedComplete = !raw.lastPage.hasMore)
            UiState.Ready(VideoListContent(videos, raw.lastPage.hasMore, raw.loadingMore))
        }
    }

    private var loadJob: Job? = null
    private var extrasJob: Job? = null
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        reload()
    }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = raw.value is Raw.Loaded)

    private fun load(quiet: Boolean) {
        loadJob?.cancel()
        extrasJob?.cancel()
        autoPages = 0
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        loadJob = viewModelScope.launch {
            raw.value = runCatching { FeedRepository.subscriptions() }.fold(
                { page -> Raw.Loaded(FeedMerge.estimate(page.videos, System.currentTimeMillis()), page, loadingMore = false) },
                { Raw.Error(it.toMessage()) },
            )
            _refreshing.value = false
            fillScreen()
        }
        // As playlists inscritas chegam depois, sem segurar o feed dos canais.
        extrasJob = viewModelScope.launch {
            val channels = FeedRepository.subscribedChannelIds().orEmpty()
            extras.value = runCatching { PlaylistSubscriptions.newVideos(channels) }.getOrDefault(emptyList())
        }
    }

    fun loadMore() {
        val loaded = raw.value as? Raw.Loaded ?: return
        if (!loaded.lastPage.hasMore || loaded.loadingMore || _refreshing.value) return
        raw.value = loaded.copy(loadingMore = true)
        loadJob = viewModelScope.launch {
            val next = runCatching { LibraryRepository.more(loaded.lastPage) }.getOrNull()
            val known = loaded.feed.mapTo(HashSet()) { it.video.id }
            val startAt = loaded.feed.lastOrNull()?.atMs ?: System.currentTimeMillis()
            val added = FeedMerge.estimate(next?.videos.orEmpty().filter { known.add(it.id) }, System.currentTimeMillis(), startAt)
            raw.value = Raw.Loaded(loaded.feed + added, next ?: VideoPage(emptyList(), null), loadingMore = false)
            fillScreen()
        }
    }

    /** Se os filtros esvaziaram a página (ex.: muitos Shorts), busca mais sozinho. */
    private fun fillScreen() {
        val loaded = raw.value as? Raw.Loaded ?: return
        val shown = (content(loaded, extras.value, settings.value) as? UiState.Ready)?.data?.videos?.size ?: 0
        if (shown < MIN_ON_SCREEN && loaded.lastPage.hasMore && autoPages < MAX_AUTO_PAGES) {
            autoPages++
            loadMore()
        }
    }

    private var autoPages = 0

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MAX_AUTO_PAGES = 5
    }
}

class PlaylistsViewModel : ViewModel(), Refreshable {
    private val _state = MutableStateFlow<UiState<List<Playlist>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Playlist>>> = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = _state.value is UiState.Ready)

    private fun load(quiet: Boolean) {
        job?.cancel()
        _refreshing.value = quiet
        if (!quiet) _state.value = UiState.Loading
        job = viewModelScope.launch {
            _state.value = runCatching { LibraryRepository.playlists() }
                .fold({ UiState.Ready(it) }, { UiState.Error(it.toMessage()) })
            _refreshing.value = false
        }
    }
}

/** Lista paginada de vídeos (os de uma playlist). */
open class VideoListViewModel(private val firstPage: suspend () -> VideoPage) : ViewModel(), Refreshable {
    private val _state = MutableStateFlow<UiState<VideoListContent>>(UiState.Loading)
    val state: StateFlow<UiState<VideoListContent>> = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private var lastPage: VideoPage? = null
    private var loadJob: Job? = null
    private var moreJob: Job? = null

    init {
        reload()
    }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = _state.value is UiState.Ready)

    private fun load(quiet: Boolean) {
        loadJob?.cancel()
        moreJob?.cancel()
        _refreshing.value = quiet
        if (!quiet) _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = runCatching { firstPage() }.fold(
                { page ->
                    lastPage = page
                    UiState.Ready(VideoListContent(page.videos, page.hasMore, loadingMore = false, title = page.title))
                },
                { UiState.Error(it.toMessage()) },
            )
            _refreshing.value = false
        }
    }

    /** Mostra os vídeos já carregados numa nova ordem (depois de reordenar a playlist). */
    protected fun showOrder(reordered: List<Video>) {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        val moved = reordered.mapTo(HashSet()) { it.id }
        _state.value = UiState.Ready(current.copy(videos = reordered + current.videos.filter { it.id !in moved }))
    }

    /**
     * Carrega as páginas que faltam (até [maxPages]). true se a lista ficou completa.
     * Reordenar precisa da playlist inteira: "mover para o fim" é o fim de verdade.
     */
    protected suspend fun loadAll(maxPages: Int): Boolean {
        moreJob?.join()
        repeat(maxPages) {
            val current = (_state.value as? UiState.Ready)?.data ?: return false
            val page = lastPage ?: return false
            if (!page.hasMore) return true
            val next = runCatching { LibraryRepository.more(page) }.getOrElse { return false }
            lastPage = next
            val known = current.videos.mapTo(HashSet()) { it.id }
            val added = next.videos.filter { known.add(it.id) }
            _state.value = UiState.Ready(current.copy(videos = current.videos + added, hasMore = next.hasMore, loadingMore = false))
        }
        return lastPage?.hasMore == false
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        val page = lastPage ?: return
        if (!page.hasMore || moreJob?.isActive == true || _refreshing.value) return
        _state.value = UiState.Ready(current.copy(loadingMore = true))
        moreJob = viewModelScope.launch {
            val next = runCatching { LibraryRepository.more(page) }.getOrNull()
            lastPage = next ?: VideoPage(emptyList(), null)
            val known = current.videos.mapTo(HashSet()) { it.id }
            val added = next?.videos.orEmpty().filter { known.add(it.id) }
            _state.value = UiState.Ready(
                current.copy(videos = current.videos + added, hasMore = next?.hasMore == true, loadingMore = false),
            )
        }
    }
}

/**
 * Página de uma playlist: os vídeos; se for de outro canal, o botão de inscrição; se for do
 * usuário (inclui Assistir mais tarde), o modo de reordenar.
 */
class PlaylistViewModel(private val playlist: Playlist) : VideoListViewModel({ LibraryRepository.playlistVideos(playlist) }) {
    val playlistId: String? get() = playlist.playlistId

    sealed interface Subscription {
        data object Hidden : Subscription
        data class Shown(val subscribed: Boolean, val busy: Boolean = false, val error: String? = null) : Subscription
    }

    sealed interface Reorder {
        data object Unavailable : Reorder

        /**
         * Dá para reordenar. [preparing]: carregando o resto da playlist antes de editar.
         * [loadFailed]: a playlist não carregou inteira, então o modo não abriu.
         */
        data class Available(val preparing: Boolean = false, val loadFailed: Boolean = false) : Reorder

        /** Editando: [videos] na nova ordem; a conta só muda em "Save order". */
        data class Editing(val videos: List<Video>, val saving: Boolean = false, val error: String? = null) : Reorder
    }

    private val _subscription = MutableStateFlow<Subscription>(Subscription.Hidden)
    val subscription: StateFlow<Subscription> = _subscription.asStateFlow()

    private val _reorder = MutableStateFlow<Reorder>(Reorder.Unavailable)
    val reorder: StateFlow<Reorder> = _reorder.asStateFlow()
    private var originalOrder: List<Video> = emptyList()

    init {
        viewModelScope.launch {
            val own = PlaylistSubscriptions.ownPlaylistIds()
            if (own != null && playlist.playlistId in own) _reorder.value = Reorder.Available()
            _subscription.value = when (val status = PlaylistSubscriptions.status(playlist.playlistId)) {
                PlaylistSubscriptions.Status.NotApplicable -> Subscription.Hidden
                is PlaylistSubscriptions.Status.Foreign -> Subscription.Shown(status.subscribed)
            }
        }
    }

    /** Entra no modo de reordenar, com a playlist inteira carregada. */
    fun startReorder() {
        val available = _reorder.value as? Reorder.Available ?: return
        if (available.preparing) return
        _reorder.value = Reorder.Available(preparing = true)
        viewModelScope.launch {
            val complete = loadAll(MAX_REORDER_PAGES)
            val videos = (state.value as? UiState.Ready)?.data?.videos
            if (!complete || videos == null) {
                _reorder.value = Reorder.Available(loadFailed = true)
                return@launch
            }
            // Um vídeo repetido na playlist não tem como ser movido pelo ID: fica de fora do modo.
            originalOrder = videos.distinctBy { it.id }
            _reorder.value = Reorder.Editing(originalOrder)
        }
    }

    /** Move o vídeo da posição [from] para [to] (na lista em edição). */
    fun move(from: Int, to: Int) {
        val editing = _reorder.value as? Reorder.Editing ?: return
        if (editing.saving || from !in editing.videos.indices || to !in editing.videos.indices || from == to) return
        val list = editing.videos.toMutableList()
        list.add(to, list.removeAt(from))
        _reorder.value = editing.copy(videos = list, error = null)
    }

    fun cancelReorder() {
        if ((_reorder.value as? Reorder.Editing)?.saving == true) return
        _reorder.value = Reorder.Available()
    }

    fun saveOrder() {
        val editing = _reorder.value as? Reorder.Editing ?: return
        val id = playlist.playlistId ?: return
        if (editing.saving) return
        if (editing.videos == originalOrder) return cancelReorder()
        _reorder.value = editing.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { PlaylistEditor.reorder(id, originalOrder.map { it.id }, editing.videos.map { it.id }) }.fold(
                {
                    showOrder(editing.videos)
                    _reorder.value = Reorder.Available()
                },
                { _reorder.value = editing.copy(saving = false, error = it.toMessage()) },
            )
        }
    }

    private companion object {
        /** Até uns 5000 vídeos (100 por página). */
        const val MAX_REORDER_PAGES = 50
    }

    fun toggleSubscription(title: String?) {
        val current = _subscription.value as? Subscription.Shown ?: return
        if (current.busy) return
        val id = playlist.playlistId ?: return
        _subscription.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                if (current.subscribed) PlaylistSubscriptions.unsubscribe(id)
                else PlaylistSubscriptions.subscribe(playlist.copy(title = title?.takeIf { it.isNotBlank() } ?: playlist.title))
            }
            _subscription.value = result.fold(
                { Subscription.Shown(subscribed = !current.subscribed) },
                { current.copy(busy = false, error = it.toMessage()) },
            )
        }
    }
}

/**
 * Configurações: lives nas Inscrições, links externos, playlist do botão Save, tópicos e canais
 * bloqueados, playlists inscritas, ícone e conta.
 */
class SettingsViewModel : ViewModel() {
    val settings: StateFlow<Settings> = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val accountName: String? = AuthRepository.accountName()

    data class SubscribedPlaylist(val playlist: Playlist, val busy: Boolean = false)

    private val _playlists = MutableStateFlow<UiState<List<SubscribedPlaylist>>>(UiState.Loading)
    val playlists: StateFlow<UiState<List<SubscribedPlaylist>>> = _playlists.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        reloadPlaylists()
    }

    fun reloadPlaylists() {
        _playlists.value = UiState.Loading
        viewModelScope.launch {
            _playlists.value = runCatching { PlaylistSubscriptions.subscribed() }
                .fold({ list -> UiState.Ready(list.map { SubscribedPlaylist(it) }) }, { UiState.Error(it.toMessage()) })
        }
    }

    fun setShowLives(value: Boolean) {
        viewModelScope.launch { SettingsRepository.setShowLives(value) }
    }

    fun setExternalLinks(value: ExternalLinks) {
        viewModelScope.launch { SettingsRepository.setExternalLinks(value) }
    }

    /** As playlists do usuário, para escolher a do botão "Save". Carregadas ao abrir a escolha. */
    private val _ownPlaylists = MutableStateFlow<UiState<List<OwnPlaylist>>?>(null)
    val ownPlaylists: StateFlow<UiState<List<OwnPlaylist>>?> = _ownPlaylists.asStateFlow()

    fun loadOwnPlaylists() {
        if (_ownPlaylists.value is UiState.Loading || _ownPlaylists.value is UiState.Ready) return
        _ownPlaylists.value = UiState.Loading
        viewModelScope.launch {
            _ownPlaylists.value = runCatching { PlaylistEditor.ownPlaylists() }
                .fold({ UiState.Ready(it) }, { UiState.Error(it.toMessage()) })
        }
    }

    fun setSaveTarget(playlist: OwnPlaylist) {
        // Fora do viewModelScope: o marcador dos cards precisa da nova playlist mesmo se a tela fechar.
        AppScope.launch {
            SettingsRepository.setSaveTarget(SaveTarget(playlist.id, playlist.title))
            SavedVideos.refresh()
        }
    }

    enum class TopicList { EXCLUDED, PREFERRED }

    private fun topics(list: TopicList): List<Topic> = when (list) {
        TopicList.EXCLUDED -> settings.value.filters.excluded
        TopicList.PREFERRED -> settings.value.filters.preferred
    }

    private fun setTopics(list: TopicList, topics: List<Topic>) {
        viewModelScope.launch {
            when (list) {
                TopicList.EXCLUDED -> SettingsRepository.setExcludedTopics(topics)
                TopicList.PREFERRED -> SettingsRepository.setPreferredTopics(topics)
            }
        }
    }

    /** Cria ([index] null) ou altera um tópico. Palavras-chave separadas por vírgula. */
    fun saveTopic(list: TopicList, index: Int?, name: String, keywords: String) {
        val topic = Topic(name.trim(), TopicRules.parseKeywords(keywords))
        if (topic.name.isEmpty() || topic.keywords.isEmpty()) return
        val current = topics(list).toMutableList()
        if (index != null && index in current.indices) current[index] = topic else current += topic
        setTopics(list, current)
    }

    fun removeTopic(list: TopicList, index: Int) {
        val current = topics(list)
        if (index in current.indices) setTopics(list, current.filterIndexed { i, _ -> i != index })
    }

    fun unblock(channelId: String) {
        viewModelScope.launch { SettingsRepository.unblock(channelId) }
    }

    private val _appIcon = MutableStateFlow(AppIcons.current())
    val appIcon: StateFlow<AppIcon> = _appIcon.asStateFlow()

    fun setAppIcon(icon: AppIcon) {
        if (icon == _appIcon.value) return
        _appIcon.value = icon
        viewModelScope.launch(Dispatchers.IO) { AppIcons.select(icon) }
    }

    fun unsubscribe(playlist: Playlist) {
        val id = playlist.playlistId ?: return
        markBusy(id, true)
        _error.value = null
        viewModelScope.launch {
            runCatching { PlaylistSubscriptions.unsubscribe(id) }.fold(
                {
                    _playlists.update { state ->
                        (state as? UiState.Ready)?.let { UiState.Ready(it.data.filterNot { p -> p.playlist.playlistId == id }) } ?: state
                    }
                },
                {
                    markBusy(id, false)
                    _error.value = it.toMessage()
                },
            )
        }
    }

    private fun markBusy(id: String, busy: Boolean) {
        _playlists.update { state ->
            (state as? UiState.Ready)?.let { ready ->
                UiState.Ready(ready.data.map { if (it.playlist.playlistId == id) it.copy(busy = busy) else it })
            } ?: state
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            AuthRepository.signOut()
            onDone()
        }
    }
}
