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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ytslim.data.AppIcon
import ytslim.data.AppScope
import ytslim.data.AppIcons
import ytslim.data.AuthRepository
import ytslim.data.AuthRepository.SignInEvent
import ytslim.data.ExploreRepository
import ytslim.data.ExternalLinks
import ytslim.data.FeedCache
import ytslim.data.FeedMerge
import ytslim.data.FeedRepository
import ytslim.data.FeedRules
import ytslim.data.LibraryRepository
import ytslim.data.OwnPlaylist
import ytslim.data.PlayQueue
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
        /** [cached]: o feed guardado no aparelho ([FeedCache]), na tela até o novo chegar. */
        data class Loaded(val feed: List<Timed>, val lastPage: VideoPage, val loadingMore: Boolean, val cached: Boolean = false) : Raw
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
            val complete = !raw.lastPage.hasMore && !raw.cached
            val videos = FeedMerge.merge(raw.feed.filter(keep), extras.filter(keepExtra), feedComplete = complete)
            UiState.Ready(VideoListContent(videos, raw.lastPage.hasMore, raw.loadingMore))
        }
    }

    private var loadJob: Job? = null
    private var extrasJob: Job? = null
    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        // Abre com o feed guardado da última vez, se houver, e atualiza por trás.
        viewModelScope.launch {
            FeedCache.read()?.let { saved ->
                raw.value = Raw.Loaded(saved.feed, VideoPage(emptyList(), null), loadingMore = false, cached = true)
                extras.value = saved.extras
            }
            load(quiet = raw.value is Raw.Loaded)
        }
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
                { page ->
                    val feed = FeedMerge.estimate(page.videos, System.currentTimeMillis())
                    AppScope.launch { FeedCache.saveFeed(feed) }
                    Raw.Loaded(feed, page, loadingMore = false)
                },
                // Sem rede, a lista que está na tela (a guardada, ou a de antes do puxar) fica.
                { if (quiet) raw.value else Raw.Error(it.toMessage()) },
            )
            _refreshing.value = false
            fillScreen()
        }
        // As playlists inscritas chegam depois, sem segurar o feed dos canais.
        extrasJob = viewModelScope.launch {
            val channels = FeedRepository.subscribedChannelIds().orEmpty()
            runCatching { PlaylistSubscriptions.newVideos(channels) }.onSuccess { found ->
                extras.value = found
                AppScope.launch { FeedCache.saveExtras(found) }
            }
            // Com as inscrições lidas, a Explore lê em segundo plano o que os canais destacam.
            ExploreRepository.prepare()
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

    /** Vídeos tirados da tela que a conta ainda não tirou (dentro do prazo do Undo): não voltam ao recarregar. */
    private val removing = HashSet<String>()

    /** A última página carregada, para continuar a lista além do que está na tela. */
    protected val loadedPage: VideoPage? get() = lastPage

    /** Os vídeos na tela, ou null enquanto a lista não carregou. */
    protected val shown: VideoListContent? get() = (_state.value as? UiState.Ready)?.data

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
                    val videos = page.videos.filterNot { it.id in removing }
                    UiState.Ready(VideoListContent(videos, page.hasMore, loadingMore = false, title = page.title))
                },
                { UiState.Error(it.toMessage()) },
            )
            _refreshing.value = false
        }
    }

    private fun setVideos(videos: List<Video>) {
        val current = shown ?: return
        _state.value = UiState.Ready(current.copy(videos = videos))
    }

    /**
     * Tira [video] da tela na hora e só manda [commit] para a conta depois de [Notices.UNDO_MS].
     * Devolve o "Undo": chamado a tempo, o vídeo volta ao mesmo lugar e a conta não muda. Se a
     * conta recusar, [onError] avisa e a lista volta a ser a da conta. Roda no [AppScope]: sair da
     * tela não cancela a mudança.
     */
    protected fun removeLater(video: Video, onError: (String) -> Unit, commit: suspend () -> Unit): () -> Unit {
        val videos = shown?.videos ?: return {}
        val index = videos.indexOfFirst { it.sameEntry(video) }
        if (index < 0) return {}
        removing += video.id
        setVideos(videos.filterIndexed { i, _ -> i != index })
        val edit = PendingEdit(AppScope, Notices.UNDO_MS) {
            val result = runCatching { commit() }
            removing -= video.id
            result.onFailure {
                onError(it.toMessage())
                refresh()
            }
        }
        return undo@{
            if (!edit.undo()) return@undo
            removing -= video.id
            val now = shown?.videos ?: return@undo
            if (now.none { it.sameEntry(video) }) setVideos(ListEdits.putBack(now, video, index))
        }
    }

    /** Leva o vídeo da posição [from] para [to], só na tela. */
    protected fun moveShown(from: Int, to: Int) {
        val videos = shown?.videos ?: return
        setVideos(ListEdits.moved(videos, from, to))
    }

    /** Carrega a próxima página e espera. true se carregou (ou se não havia mais). */
    protected suspend fun loadNext(): Boolean {
        moreJob?.join()
        val page = lastPage ?: return false
        if (!page.hasMore) return true
        val next = runCatching { LibraryRepository.more(page) }.getOrElse { return false }
        append(next)
        return true
    }

    fun loadMore() {
        val current = shown ?: return
        val page = lastPage ?: return
        if (!page.hasMore || moreJob?.isActive == true || _refreshing.value) return
        _state.value = UiState.Ready(current.copy(loadingMore = true))
        moreJob = viewModelScope.launch {
            append(runCatching { LibraryRepository.more(page) }.getOrNull() ?: VideoPage(emptyList(), null))
        }
    }

    /** Junta [next] ao que está na tela agora (que pode ter mudado enquanto a página carregava). */
    private fun append(next: VideoPage) {
        lastPage = next
        val current = shown ?: return
        val known = current.videos.mapTo(HashSet()) { it.id }
        val added = next.videos.filter { it.id !in removing && known.add(it.id) }
        _state.value = UiState.Ready(current.copy(videos = current.videos + added, hasMore = next.hasMore, loadingMore = false))
    }
}

/** O mesmo item da lista (um vídeo pode aparecer duas vezes numa playlist). */
private fun Video.sameEntry(other: Video) = id == other.id && playlistIndex == other.playlistIndex

/**
 * Página de uma playlist: os vídeos; se for de outro canal, o botão de inscrição; se for do
 * usuário (inclui Assistir mais tarde), tirar vídeos e mudar a ordem.
 */
class PlaylistViewModel(private val playlist: Playlist) : VideoListViewModel({ LibraryRepository.playlistVideos(playlist) }) {
    val playlistId: String? get() = playlist.playlistId

    /** Abrir [video] daqui começa a sequência: os vídeos seguintes da playlist tocam depois dele. */
    fun queueFrom(video: Video) {
        val videos = shown?.videos
        if (videos == null) PlayQueue.clear() else PlayQueue.start(videos, video.id, loadedPage)
    }

    sealed interface Subscription {
        data object Hidden : Subscription
        data class Shown(val subscribed: Boolean, val busy: Boolean = false, val error: String? = null) : Subscription
    }

    private val _subscription = MutableStateFlow<Subscription>(Subscription.Hidden)
    val subscription: StateFlow<Subscription> = _subscription.asStateFlow()

    private val _editable = MutableStateFlow(false)
    /** Playlist do próprio usuário (inclui Watch later): dá para tirar vídeos dela e mudar a ordem. */
    val editable: StateFlow<Boolean> = _editable.asStateFlow()

    /** O vídeo sendo arrastado e quem vinha logo depois dele antes de arrastar. */
    private var dragged: Video? = null
    private var successorBefore: String? = null

    init {
        viewModelScope.launch {
            val own = PlaylistSubscriptions.ownPlaylistIds()
            if (own != null && playlist.playlistId in own) _editable.value = true
            _subscription.value = when (val status = PlaylistSubscriptions.status(playlist.playlistId)) {
                PlaylistSubscriptions.Status.NotApplicable -> Subscription.Hidden
                is PlaylistSubscriptions.Status.Foreign -> Subscription.Shown(status.subscribed)
            }
        }
    }

    /**
     * Tira [video] da playlist: some da tela na hora, e a conta muda quando o prazo do Undo passa
     * (devolve o Undo). Na playlist do botão Save, passa pelo [SavedVideos] (para o marcador dos
     * cards acompanhar). Se a conta não mudar, [onError] avisa e a lista volta a ser a da conta.
     */
    fun remove(video: Video, onError: (String) -> Unit): () -> Unit {
        val id = playlist.playlistId ?: return {}
        if (!_editable.value) return {}
        return removeLater(video, onError) {
            if (id == SavedVideos.target.value.playlistId) {
                SavedVideos.toggle(video.id, current = true) { error ->
                    onError(error.toMessage())
                    refresh()
                }
            } else {
                PlaylistEditor.setSaved(video.id, id, saved = false)
            }
        }
    }

    /**
     * Arrastando: leva o vídeo da posição [from] para [to], só na tela. A conta muda ao soltar
     * ([drop]), com um movimento só, sem precisar da playlist inteira (pedido do Johan, 2026-11-03).
     */
    fun move(from: Int, to: Int) {
        if (!_editable.value) return
        val videos = shown?.videos ?: return
        if (from !in videos.indices || to !in videos.indices || from == to) return
        val video = videos[from]
        if (dragged?.sameEntry(video) != true) {
            dragged = video
            successorBefore = videos.getOrNull(from + 1)?.id
        }
        moveShown(from, to)
    }

    /**
     * Soltou o vídeo arrastado: a posição nova vai para a conta ("pôr antes de" quem ficou logo
     * depois dele). Os movimentos vão um por vez, na ordem em que foram feitos.
     */
    fun drop(onError: (String) -> Unit) {
        val video = dragged ?: return
        dragged = null
        val id = playlist.playlistId ?: return
        val content = shown ?: return
        val index = content.videos.indexOfFirst { it.sameEntry(video) }
        if (index < 0) return
        // Solto no fim do que já carregou: quem vem depois está na próxima página.
        val atLoadedEnd = index == content.videos.lastIndex && content.hasMore
        val successor = content.videos.getOrNull(index + 1)?.id
        if (!atLoadedEnd && successor == successorBefore) return
        send(id, video, onError) {
            if (!atLoadedEnd) {
                successor
            } else {
                check(loadNext()) { "couldn't load the rest of the playlist" }
                val videos = shown?.videos.orEmpty()
                videos.getOrNull(videos.indexOfFirst { it.sameEntry(video) } + 1)?.id
            }
        }
    }

    /**
     * Leva [video] para o topo da playlist (pedido do Johan, 2026-11-03: sem rolar a lista toda
     * arrastando). O topo já está carregado, então é um movimento só. true se mudou algo.
     */
    fun moveToTop(video: Video, onError: (String) -> Unit): Boolean {
        val id = playlist.playlistId ?: return false
        if (!_editable.value) return false
        val videos = shown?.videos ?: return false
        val index = videos.indexOfFirst { it.sameEntry(video) }
        if (index <= 0) return false
        val first = videos[0].id
        moveShown(index, 0)
        send(id, video, onError) { first }
        return true
    }

    /** Manda para a conta "pôr [video] antes de [successor]", depois dos movimentos anteriores. */
    private fun send(playlistId: String, video: Video, onError: (String) -> Unit, successor: suspend () -> String?) {
        AppScope.launch {
            edits.withLock {
                runCatching { PlaylistEditor.move(playlistId, video.id, successor()) }.onFailure {
                    onError(it.toMessage())
                    refresh()
                }
            }
        }
    }

    private companion object {
        /** Os movimentos (de qualquer playlist) vão para a conta um por vez, na ordem. */
        val edits = Mutex()
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
            FeedCache.clear()
            onDone()
        }
    }
}
