package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import ytslim.data.AuthRepository
import ytslim.data.Channel
import ytslim.data.Playlist
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

    ui.Theme {
        NavHost(navController = nav, startDestination = start) {
            composable<SignInRoute> {
                val vm: SignInViewModel = viewModel()
                ui.SignIn(vm) {
                    signed = true
                    nav.navigate(HomeRoute) { popUpTo<SignInRoute> { inclusive = true } }
                }
            }
            composable<HomeRoute> {
                var tab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
                ui.Home(tab = tab, onTab = { tab = it }) { current ->
                    when (current) {
                        HomeTab.SUBSCRIPTIONS -> ui.Feed(viewModel<FeedViewModel>(), onPlay = play)
                        HomeTab.RECOMMENDED -> ui.Recommended(viewModel<RecommendedViewModel>(), onPlay = play)
                        HomeTab.SEARCH -> ui.Search(
                            vm = viewModel<SearchViewModel>(),
                            onPlay = play,
                            onOpenChannel = openChannel,
                            onOpenPlaylist = openPlaylist,
                        )
                        HomeTab.PLAYLISTS -> ui.Playlists(viewModel<PlaylistsViewModel>(), onOpen = openPlaylist)
                        HomeTab.HISTORY -> ui.History(viewModel<HistoryViewModel>(), onPlay = play)
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
                ui.PlaylistPage(title = route.title, vm = vm, onPlay = play, onBack = { nav.popBackStack() })
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

    /** Moldura do início: as abas e o conteúdo da aba atual. */
    @Composable
    fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit)

    @Composable
    fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit)

    @Composable
    fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit)

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
    fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit)

    @Composable
    fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit)

    @Composable
    fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit)

    @Composable
    fun ChannelPage(
        title: String,
        vm: ChannelViewModel,
        onPlay: (Video) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
        onBack: () -> Unit,
    )
}
