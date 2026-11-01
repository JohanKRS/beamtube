package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ytslim.data.AuthRepository
import ytslim.data.Video
import ytslim.player.PlayerActivity
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi

/**
 * Raiz da interface. A navegação e os ViewModels são os mesmos na TV e no celular;
 * só as telas mudam, escolhidas em tempo de execução por [isTv].
 */
@Composable
fun AppRoot(isTv: Boolean) {
    val context = LocalContext.current
    val ui: Screens = remember(isTv) { if (isTv) TvUi else MobileUi }
    val nav = rememberNavController()
    val start: Any = remember { if (AuthRepository.isSigned()) PlaylistsRoute else SignInRoute }
    val play: (Video) -> Unit = { video -> context.startActivity(PlayerActivity.intent(context, video.id, video.title)) }

    ui.Theme {
        NavHost(navController = nav, startDestination = start) {
            composable<SignInRoute> {
                val vm: SignInViewModel = viewModel()
                ui.SignIn(vm) {
                    nav.navigate(PlaylistsRoute) { popUpTo<SignInRoute> { inclusive = true } }
                }
            }
            composable<PlaylistsRoute> {
                val vm: PlaylistsViewModel = viewModel()
                ui.Playlists(
                    vm = vm,
                    onOpen = { nav.navigate(PlaylistRoute.from(it)) },
                    onSignedOut = {
                        nav.navigate(SignInRoute) { popUpTo<PlaylistsRoute> { inclusive = true } }
                    },
                )
            }
            composable<PlaylistRoute> { entry ->
                val route = entry.toRoute<PlaylistRoute>()
                val vm = viewModel { VideoListViewModel.forPlaylist(route.toPlaylist()) }
                ui.VideoList(title = route.title, vm = vm, onPlay = play, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** As telas que cada interface (TV ou celular) implementa. */
interface Screens {
    @Composable
    fun Theme(content: @Composable () -> Unit)

    @Composable
    fun SignIn(vm: SignInViewModel, onSignedIn: () -> Unit)

    @Composable
    fun Playlists(vm: PlaylistsViewModel, onOpen: (ytslim.data.Playlist) -> Unit, onSignedOut: () -> Unit)

    @Composable
    fun VideoList(title: String, vm: VideoListViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit)
}
