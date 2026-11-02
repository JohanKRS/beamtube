package ytslim.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import ytslim.data.AuthRepository
import ytslim.data.Channel
import ytslim.data.Playlist
import ytslim.data.SavedVideos
import ytslim.data.Video
import ytslim.player.PlayerActivity
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi

/**
 * Raiz da interface. A navegação e os ViewModels são os mesmos na TV e no celular;
 * só as telas mudam, escolhidas em tempo de execução por [isTv].
 *
 * @param links páginas pedidas de fora (link de playlist, canal no player). Cada uma é aberta
 * por cima do início assim que houver login, e então sai da fila.
 */
@Composable
fun AppRoot(isTv: Boolean, links: MutableStateFlow<Link?>) {
    val context = LocalContext.current
    val ui: Screens = remember(isTv) { if (isTv) TvUi else MobileUi }
    val nav = rememberNavController()
    var signed by remember { mutableStateOf(AuthRepository.isSigned()) }
    val start: Any = remember { if (signed) HomeRoute else SignInRoute }
    val play: (Video) -> Unit = { video -> context.startActivity(PlayerActivity.intent(context, video.id, video.title)) }
    val openPlaylist: (Playlist) -> Unit = { nav.navigate(PlaylistRoute.from(it)) }
    val openChannel: (Channel) -> Unit = { nav.navigate(ChannelRoute(it.id, it.title)) }
    val signedOut: () -> Unit = {
        signed = false
        nav.signedOut()
    }

    // O marcador do Watch later nos cards: a playlist do botão Save é lida ao entrar.
    LaunchedEffect(signed) { if (signed) SavedVideos.refresh() }

    ui.Theme {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = nav, startDestination = start) {
                composable<SignInRoute> {
                    val vm: SignInViewModel = viewModel()
                    ui.SignIn(vm) {
                        signed = true
                        nav.navigate(HomeRoute) { popUpTo<SignInRoute> { inclusive = true } }
                    }
                }
                composable<HomeRoute> { entry ->
                    var tab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
                    // Os ViewModels das abas são os do início (os mesmos que viewModel() devolve abaixo).
                    val reload: (HomeTab) -> Unit = { which ->
                        val vms = ViewModelProvider.create(entry)
                        when (which) {
                            HomeTab.SUBSCRIPTIONS -> vms[FeedViewModel::class].reload()
                            HomeTab.RECOMMENDED -> vms[RecommendedViewModel::class].reload()
                            HomeTab.PLAYLISTS -> vms[PlaylistsViewModel::class].reload()
                            HomeTab.HISTORY -> vms[HistoryViewModel::class].reload()
                            HomeTab.SEARCH, HomeTab.SETTINGS -> Unit
                        }
                    }
                    ui.Home(tab = tab, onTab = { tab = it }, onReload = reload) { current ->
                        when (current) {
                            HomeTab.SUBSCRIPTIONS -> ui.Feed(viewModel<FeedViewModel>(), onPlay = play, onOpenChannel = openChannel)
                            HomeTab.RECOMMENDED -> ui.Recommended(viewModel<RecommendedViewModel>(), onPlay = play, onOpenChannel = openChannel)
                            HomeTab.SEARCH -> ui.Search(
                                vm = viewModel<SearchViewModel>(),
                                onPlay = play,
                                onOpenChannel = openChannel,
                                onOpenPlaylist = openPlaylist,
                            )
                            HomeTab.PLAYLISTS -> ui.Playlists(viewModel<PlaylistsViewModel>(), onOpen = openPlaylist)
                            HomeTab.HISTORY -> ui.History(viewModel<HistoryViewModel>(), onPlay = play, onOpenChannel = openChannel)
                            HomeTab.SETTINGS -> ui.Settings(
                                vm = viewModel<SettingsViewModel>(),
                                onOpenPlaylist = openPlaylist,
                                onSignedOut = signedOut,
                            )
                        }
                    }
                }
                composable<PlaylistRoute> { entry ->
                    val route = entry.toRoute<PlaylistRoute>()
                    val vm = viewModel { PlaylistViewModel(route.toPlaylist()) }
                    ui.PlaylistPage(title = route.title, vm = vm, onPlay = play, onOpenChannel = openChannel, onBack = { nav.popBackStack() })
                }
                composable<ChannelRoute> { entry ->
                    val route = entry.toRoute<ChannelRoute>()
                    val vm = viewModel { ChannelViewModel(route.channelId, route.title) }
                    ui.ChannelPage(
                        title = route.title,
                        vm = vm,
                        onPlay = play,
                        onOpenPlaylist = openPlaylist,
                        onBack = { nav.popBackStack() },
                    )
                }
            }

            val notice by Notices.current.collectAsStateWithLifecycle()
            ui.NoticePill(notice)
            LaunchedEffect(notice?.id) {
                val id = notice?.id ?: return@LaunchedEffect
                delay(2600)
                Notices.dismiss(id)
            }
        }
    }

    val link by links.collectAsStateWithLifecycle()
    LaunchedEffect(link, signed) {
        val target = link ?: return@LaunchedEffect
        if (!signed) return@LaunchedEffect
        links.value = null
        when (target) {
            is Link.Playlist -> nav.navigate(PlaylistRoute.fromId(target.id))
            is Link.Channel -> nav.navigate(ChannelRoute(target.id, target.name)) { launchSingleTop = true }
        }
    }
}

private fun NavHostController.signedOut() {
    navigate(SignInRoute) { popUpTo(graph.id) { inclusive = true } }
}

/** As telas que cada interface (TV ou celular) implementa. */
interface Screens {
    @Composable
    fun Theme(content: @Composable () -> Unit)

    @Composable
    fun SignIn(vm: SignInViewModel, onSignedIn: () -> Unit)

    /**
     * Moldura do início: as abas e o conteúdo da aba atual. [onReload] recarrega a lista de uma aba:
     * a TV chama ao segurar OK na aba; o celular não usa, porque tem o puxar para atualizar.
     */
    @Composable
    fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit)

    /** O aviso curto do momento ("Saved to Watch later"), em cima de tudo. */
    @Composable
    fun NoticePill(notice: Notice?)

    @Composable
    fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit)

    @Composable
    fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit)

    @Composable
    fun Search(
        vm: SearchViewModel,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
    )

    @Composable
    fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit)

    @Composable
    fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit)

    @Composable
    fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit)

    @Composable
    fun PlaylistPage(
        title: String,
        vm: PlaylistViewModel,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onBack: () -> Unit,
    )

    @Composable
    fun ChannelPage(
        title: String,
        vm: ChannelViewModel,
        onPlay: (Video) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
        onBack: () -> Unit,
    )
}
