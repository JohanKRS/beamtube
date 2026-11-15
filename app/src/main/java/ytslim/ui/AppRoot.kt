package ytslim.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ytslim.data.AppScope
import ytslim.data.Channel
import ytslim.data.EngineWarmup
import ytslim.data.FirstRun
import ytslim.data.PlayQueue
import ytslim.data.PlaybackRepository
import ytslim.data.Playlist
import ytslim.data.Published
import ytslim.data.SavedVideos
import ytslim.data.User
import ytslim.data.Users
import ytslim.data.Video
import ytslim.player.PlayerActivity
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi

/**
 * Raiz da interface. A navegação e os ViewModels são os mesmos na TV e no celular;
 * só as telas mudam, escolhidas em tempo de execução por [isTv].
 *
 * Cada usuário ([Users]) tem as próprias telas: trocar de usuário refaz a navegação e os ViewModels
 * (as listas, a busca, os Ajustes) do zero, com os dados dele. O app abre no último usuário, sem
 * tela de escolha (pedido do Johan, 2026-11-14).
 *
 * @param links páginas pedidas de fora (link de playlist, canal no player). Cada uma é aberta
 * por cima do início assim que houver um usuário, e então sai da fila.
 */
@Composable
fun AppRoot(isTv: Boolean, links: MutableStateFlow<Link?>) {
    val ui: Screens = remember(isTv) { if (isTv) TvUi else MobileUi }
    // A conta salva é lida aqui, uma vez (leva milissegundos): a primeira tela já é a do usuário certo.
    remember { Users.resolve() }
    val user by Users.current.collectAsStateWithLifecycle()
    val stores: UserStores = viewModel()

    ui.Theme {
        Box(Modifier.fillMaxSize()) {
            val slot = user?.slot
            key(slot, user?.guest) {
                val owner = remember { stores.owner(slot) }
                // Girar a tela mantém o usuário (e os ViewModels dele); outro usuário apaga os deste.
                DisposableEffect(Unit) { onDispose { if (Users.current.value?.slot != slot) stores.drop(slot) } }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    UserScreens(ui, user, links)
                }
            }

            val notice by Notices.current.collectAsStateWithLifecycle()
            ui.NoticePill(notice)
            LaunchedEffect(notice?.id) {
                val shown = notice ?: return@LaunchedEffect
                delay(if (shown.action != null) Notices.UNDO_MS else if (shown.long) Notices.LONG_MS else Notices.SHOW_MS)
                Notices.dismiss(shown.id)
            }
        }
    }
}

/** As telas de um usuário ([user] null: ninguém entrou ainda, a tela de entrar com o convidado). */
@Composable
private fun UserScreens(ui: Screens, user: User?, links: MutableStateFlow<Link?>) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val guest = user?.guest == true
    // No primeiro uso de cada conta, depois do login, vem a escolha de tópicos ([TopicsRoute]).
    val start: Any = remember {
        when {
            user == null -> SignInRoute(adding = false)
            !FirstRun.topicsChosen -> TopicsRoute(firstRun = true)
            else -> HomeRoute
        }
    }
    val openPlayer: (Video) -> Unit = { video ->
        // O vídeo começa a ser pedido já no toque, enquanto a tela do player abre.
        PlaybackRepository.prefetch(video.id)
        context.startActivity(PlayerActivity.intent(context, video.id, video.title, video.resumeMs, Published.of(video), video.thumbnailUrl))
    }
    // Só a página de uma playlist toca em sequência; o resto abre um vídeo sozinho.
    val play: (Video) -> Unit = { video ->
        PlayQueue.clear()
        openPlayer(video)
    }
    val openPlaylist: (Playlist) -> Unit = { nav.navigate(PlaylistRoute.from(it)) }
    val openChannel: (Channel) -> Unit = { nav.navigate(ChannelRoute(it.id, it.title)) }
    val addAccount: () -> Unit = { nav.navigate(SignInRoute(adding = true)) { launchSingleTop = true } }

    // O marcador do Watch later nos cards: a playlist do botão Save é lida ao entrar.
    LaunchedEffect(Unit) { if (user != null && !guest) SavedVideos.refresh() }

    CompositionLocalProvider(LocalAddAccount provides addAccount) {
        NavHost(navController = nav, startDestination = start) {
            composable<SignInRoute> { entry ->
                val route = entry.toRoute<SignInRoute>()
                val vm: SignInViewModel = viewModel()
                // Entrou: o usuário novo abre sozinho ([Users.signedIn]). Se a conta já estava aqui
                // e era a aberta, nada muda, e a tela de entrar fecha.
                LaunchedEffect(vm) {
                    vm.state.first { it == SignInViewModel.State.SignedIn }
                    if (route.adding) nav.popBackStack()
                }
                ui.SignIn(
                    vm,
                    onGuest = if (route.adding) null else ({ AppScope.launch { Users.switchTo(Users.guestUser) } }),
                    onCancel = if (route.adding) ({ nav.popBackStack() }) else null,
                )
            }
            composable<TopicsRoute> { entry ->
                val route = entry.toRoute<TopicsRoute>()
                ui.Topics(viewModel<TopicsViewModel>(), firstRun = route.firstRun) {
                    if (route.firstRun) {
                        nav.navigate(HomeRoute) { popUpTo<TopicsRoute> { inclusive = true } }
                    } else {
                        nav.popBackStack()
                    }
                }
            }
            composable<HomeRoute> { entry ->
                // O convidado abre nos Recomendados (só tem eles e a busca).
                var tab by rememberSaveable { mutableStateOf(if (guest) HomeTab.RECOMMENDED else HomeTab.SUBSCRIPTIONS) }
                // Cada aba guarda o próprio estado (a posição da lista) quando sai da tela.
                val tabStates = rememberSaveableStateHolder()
                // As outras abas carregam logo depois das Inscrições, para já estarem prontas ao trocar.
                LaunchedEffect(Unit) {
                    if (!guest) {
                        val vms = ViewModelProvider.create(entry)
                        withTimeoutOrNull(6_000) { vms[FeedViewModel::class].state.first { it !is UiState.Loading } }
                        vms[RecommendedViewModel::class]
                        vms[PlaylistsViewModel::class]
                        vms[HistoryViewModel::class]
                    }
                    // Adianta o que o primeiro vídeo precisa (o token do YouTube, o player dele e a
                    // decifragem), que numa TV fraca leva segundos ([EngineWarmup]).
                    EngineWarmup.start(delayMs = EngineWarmup.AFTER_LISTS_MS)
                }
                // Os ViewModels das abas são os do início (os mesmos que viewModel() devolve abaixo).
                // Na TV (OK na aba aberta), a lista sai na hora e os cartões vazios ficam até a nova
                // chegar, para o OK ter resposta visível; a Explore também (até a 1.0.5 ela mantinha
                // a lista velha na tela, sem sinal de que o OK tinha feito algo).
                val reload: (HomeTab) -> Unit = { which ->
                    val vms = ViewModelProvider.create(entry)
                    when (which) {
                        HomeTab.SUBSCRIPTIONS -> vms[FeedViewModel::class].reload()
                        HomeTab.RECOMMENDED -> vms[RecommendedViewModel::class].reload()
                        HomeTab.PLAYLISTS -> vms[PlaylistsViewModel::class].reload()
                        HomeTab.HISTORY -> vms[HistoryViewModel::class].reload()
                        HomeTab.EXPLORE -> vms[ExploreViewModel::class].reload()
                        HomeTab.SEARCH, HomeTab.SETTINGS -> Unit
                    }
                }
                ui.Home(tab = tab, onTab = { tab = it }, onReload = reload) { current ->
                    tabStates.SaveableStateProvider(current.name) {
                        when (current) {
                            HomeTab.SUBSCRIPTIONS -> ui.Feed(viewModel<FeedViewModel>(), onPlay = play, onOpenChannel = openChannel)
                            HomeTab.RECOMMENDED -> ui.Recommended(viewModel<RecommendedViewModel>(), onPlay = play, onOpenChannel = openChannel)
                            HomeTab.EXPLORE -> ui.Explore(viewModel<ExploreViewModel>(), onPlay = play, onOpenChannel = openChannel)
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
                                onOpenTopics = { nav.navigate(TopicsRoute(firstRun = false)) },
                                onAddAccount = addAccount,
                            )
                        }
                    }
                }
            }
            composable<PlaylistRoute> { entry ->
                val route = entry.toRoute<PlaylistRoute>()
                val vm = viewModel { PlaylistViewModel(route.toPlaylist()) }
                val playInOrder: (Video) -> Unit = { video ->
                    vm.queueFrom(video)
                    openPlayer(video)
                }
                ui.PlaylistPage(title = route.title, vm = vm, onPlay = playInOrder, onOpenChannel = openChannel, onBack = { nav.popBackStack() })
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
    LaunchedEffect(link) {
        val target = link ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        links.value = null
        when (target) {
            is Link.Playlist -> nav.navigate(PlaylistRoute.fromId(target.id))
            is Link.Channel -> nav.navigate(ChannelRoute(target.id, target.name)) { launchSingleTop = true }
        }
    }
}

/** As telas que cada interface (TV ou celular) implementa. */
interface Screens {
    @Composable
    fun Theme(content: @Composable () -> Unit)

    /**
     * Entrar com uma conta pelo código. [onGuest]: usar sem conta (só na primeira vez, quando ninguém
     * entrou ainda); [onCancel]: voltar (ao acrescentar uma conta pelo menu da foto ou pelos Ajustes).
     */
    @Composable
    fun SignIn(vm: SignInViewModel, onGuest: (() -> Unit)?, onCancel: (() -> Unit)?)

    /**
     * Moldura do início: as abas ([homeTabs]: o convidado tem menos), a foto do usuário, que abre o
     * menu de troca, e o conteúdo da aba atual. [onReload] recarrega a lista de uma aba: a TV chama
     * com OK na aba aberta; o celular não usa, porque tem o puxar para atualizar.
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

    /** Vídeos dos canais que os canais inscritos destacam; atualizar sorteia outros canais. */
    @Composable
    fun Explore(vm: ExploreViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit)

    @Composable
    fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit)

    @Composable
    fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit)

    /** [onOpenTopics] abre a tela de tópicos sugeridos ([Topics]); [onAddAccount], a de entrar com outra conta. */
    @Composable
    fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onOpenTopics: () -> Unit, onAddAccount: () -> Unit)

    /**
     * Os tópicos sugeridos, em dois passos (ver mais, ver menos). [firstRun]: a tela do primeiro
     * uso, que pode ser pulada; [onDone] segue (para o início ou de volta aos Ajustes).
     */
    @Composable
    fun Topics(vm: TopicsViewModel, firstRun: Boolean, onDone: () -> Unit)

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
