package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ytslim.data.AuthRepository
import ytslim.data.Playlist
import ytslim.data.Video
import ytslim.player.PlayerActivity
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi

/**
 * Raiz da interface. A navegação e os ViewModels são os mesmos na TV e no celular;
 * só as telas mudam, escolhidas em tempo de execução por [isTv].
 *
 * @param openPlaylistId playlist aberta por um link de outro app.
 */
@Composable
fun AppRoot(isTv: Boolean, openPlaylistId: String? = null) {
    val context = LocalContext.current
    val ui: Screens = remember(isTv) { if (isTv) TvUi else MobileUi }
    val nav = rememberNavController()
    val signed = remember { AuthRepository.isSigned() }
    val start: Any = if (signed) HomeRoute else SignInRoute
    val play: (Video) -> Unit = { video -> context.startActivity(PlayerActivity.intent(context, video.id, video.title)) }
    val openPlaylist: (Playlist) -> Unit = { nav.navigate(PlaylistRoute.from(it)) }

    ui.Theme {
        NavHost(navController = nav, startDestination = start) {
            composable<SignInRoute> {
                val vm: SignInViewModel = viewModel()
                ui.SignIn(vm) {
                    nav.navigate(HomeRoute) { popUpTo<SignInRoute> { inclusive = true } }
                }
            }
            composable<HomeRoute> {
                var tab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
                ui.Home(tab = tab, onTab = { tab = it }) { current ->
                    when (current) {
                        HomeTab.SUBSCRIPTIONS -> ui.Feed(viewModel<FeedViewModel>(), onPlay = play)
                        HomeTab.PLAYLISTS -> ui.Playlists(viewModel<PlaylistsViewModel>(), onOpen = openPlaylist)
                        HomeTab.SETTINGS -> ui.Settings(
                            vm = viewModel<SettingsViewModel>(),
                            onOpenPlaylist = openPlaylist,
                            onSignedOut = { nav.signedOut() },
                        )
                    }
                }
            }
            composable<PlaylistRoute> { entry ->
                val route = entry.toRoute<PlaylistRoute>()
                val vm = viewModel { PlaylistViewModel(route.toPlaylist()) }
                ui.PlaylistPage(title = route.title, vm = vm, onPlay = play, onBack = { nav.popBackStack() })
            }
        }
    }

    // Link de playlist vindo de fora: abre a página por cima do início.
    LaunchedEffect(openPlaylistId) {
        if (openPlaylistId != null && signed) nav.navigate(PlaylistRoute.fromId(openPlaylistId))
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
    fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit)

    @Composable
    fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit)

    @Composable
    fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit)
}
