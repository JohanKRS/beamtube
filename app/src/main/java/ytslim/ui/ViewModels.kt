package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ytslim.data.AuthRepository
import ytslim.data.AuthRepository.SignInEvent
import ytslim.data.LibraryRepository
import ytslim.data.Playlist
import ytslim.data.Video
import ytslim.data.VideoPage

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

private fun Throwable.toMessage(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

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

class PlaylistsViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Playlist>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Playlist>>> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { LibraryRepository.playlists() }
                .fold({ UiState.Ready(it) }, { UiState.Error(it.toMessage()) })
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            AuthRepository.signOut()
            onDone()
        }
    }
}

/** Lista paginada de vídeos (aqui, os de uma playlist). */
class VideoListViewModel(private val firstPage: suspend () -> VideoPage) : ViewModel() {
    data class Content(val videos: List<Video>, val hasMore: Boolean, val loadingMore: Boolean)

    private val _state = MutableStateFlow<UiState<Content>>(UiState.Loading)
    val state: StateFlow<UiState<Content>> = _state.asStateFlow()
    private var lastPage: VideoPage? = null
    private var moreJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        moreJob?.cancel()
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { firstPage() }.fold(
                { page ->
                    lastPage = page
                    UiState.Ready(Content(page.videos, page.hasMore, loadingMore = false))
                },
                { UiState.Error(it.toMessage()) },
            )
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        val page = lastPage ?: return
        if (!page.hasMore || moreJob?.isActive == true) return
        _state.value = UiState.Ready(current.copy(loadingMore = true))
        moreJob = viewModelScope.launch {
            val next = runCatching { LibraryRepository.more(page) }.getOrNull()
            lastPage = next ?: VideoPage(emptyList(), null)
            val known = current.videos.mapTo(HashSet()) { it.id }
            val added = next?.videos.orEmpty().filter { known.add(it.id) }
            _state.value = UiState.Ready(
                Content(current.videos + added, hasMore = next?.hasMore == true, loadingMore = false),
            )
        }
    }

    companion object {
        fun forPlaylist(playlist: Playlist) = VideoListViewModel { LibraryRepository.playlistVideos(playlist) }
    }
}
