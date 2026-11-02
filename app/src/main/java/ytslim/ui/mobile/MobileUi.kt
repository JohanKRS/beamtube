package ytslim.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.AuthRepository
import ytslim.data.Channel
import ytslim.data.ChannelPhotos
import ytslim.data.Playlist
import ytslim.data.SavedVideos
import ytslim.data.SearchFilters
import ytslim.data.SearchItem
import ytslim.data.Topic
import ytslim.data.TopicRules
import ytslim.data.Video
import ytslim.ui.AppIconPreview
import ytslim.ui.ChannelViewModel
import ytslim.ui.DoorGlyph
import ytslim.ui.FeedViewModel
import ytslim.ui.HistoryViewModel
import ytslim.ui.HomeTab
import ytslim.ui.LightSlit
import ytslim.ui.Look
import ytslim.ui.Notice
import ytslim.ui.PlaylistViewModel
import ytslim.ui.PlaylistViewModel.Reorder
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.RecommendedViewModel
import ytslim.ui.Refreshable
import ytslim.ui.Screens
import ytslim.ui.SearchViewModel
import ytslim.ui.SettingsViewModel
import ytslim.ui.SettingsViewModel.TopicList
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListContent
import ytslim.ui.channelOf
import ytslim.ui.externalLinkOptions
import ytslim.ui.formatDuration
import ytslim.ui.metaAfterChannel
import ytslim.ui.monogramOf
import ytslim.ui.searchFilterChoices
import ytslim.ui.toggleSaved

/** Altura das barras de vidro, sem contar as barras do sistema que ficam atrás delas. */
private val TopBarHeight = 56.dp
private val BottomBarHeight = 60.dp
private val RailWidth = 80.dp

/**
 * Quanto as barras de vidro cobrem de cada lado. O conteúdo ocupa a tela inteira e rola por baixo
 * delas; as listas usam isto no contentPadding para o primeiro e o último item ficarem visíveis.
 */
private val LocalBars = compositionLocalOf { PaddingValues(0.dp) }

/** Vidro fosco: o fundo a 62% com o desfoque do que passa por baixo (Android 12+; antes, quase opaco). */
private val GlassStyle = HazeStyle(
    backgroundColor = Look.Ground,
    tints = listOf(HazeTint(Look.Glass)),
    blurRadius = 24.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(Look.GlassFallback),
)

private val Colors = darkColorScheme(
    primary = Look.Glow,
    onPrimary = Look.OnGlow,
    primaryContainer = Look.SurfaceHigh,
    onPrimaryContainer = Look.Text,
    secondary = Look.GlowSoft,
    onSecondary = Look.OnGlow,
    secondaryContainer = Look.SurfaceHigh,
    onSecondaryContainer = Look.Text,
    tertiary = Look.GlowSoft,
    onTertiary = Look.OnGlow,
    background = Look.Ground,
    onBackground = Look.Text,
    surface = Look.Ground,
    onSurface = Look.Text,
    surfaceVariant = Look.Surface,
    onSurfaceVariant = Look.Sub,
    surfaceTint = Color.Transparent,
    surfaceBright = Look.SurfaceHigh,
    surfaceDim = Look.Ground,
    surfaceContainerLowest = Look.Ground,
    surfaceContainerLow = Look.Surface,
    surfaceContainer = Look.Surface,
    surfaceContainerHigh = Look.SurfaceHigh,
    surfaceContainerHighest = Look.SurfaceHigh,
    inverseSurface = Look.Text,
    inverseOnSurface = Look.Ground,
    inversePrimary = Look.Glow,
    outline = Look.Outline,
    outlineVariant = Look.Hairline,
    error = Look.Error,
    onError = Look.OnGlow,
)

private val Type: Typography = Typography().let { t ->
    fun TextStyle.figtree(weight: FontWeight? = null) = copy(fontFamily = Look.Figtree, fontWeight = weight ?: fontWeight)
    Typography(
        displayLarge = t.displayLarge.figtree(),
        displayMedium = t.displayMedium.figtree(),
        displaySmall = t.displaySmall.figtree(),
        headlineLarge = t.headlineLarge.figtree(FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.figtree(FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.figtree(FontWeight.SemiBold),
        titleLarge = t.titleLarge.figtree(FontWeight.SemiBold),
        titleMedium = t.titleMedium.figtree(FontWeight.SemiBold),
        titleSmall = t.titleSmall.figtree(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.figtree(),
        bodyMedium = t.bodyMedium.figtree(),
        bodySmall = t.bodySmall.figtree(),
        labelLarge = t.labelLarge.figtree(FontWeight.SemiBold),
        labelMedium = t.labelMedium.figtree(FontWeight.SemiBold),
        labelSmall = t.labelSmall.figtree(FontWeight.SemiBold),
    )
}

private val CardTitle = TextStyle(fontFamily = Look.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp)
private val RowTitle = TextStyle(fontFamily = Look.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
private val Meta = TextStyle(fontFamily = Look.Figtree, fontSize = 13.sp, lineHeight = 18.sp, color = Look.Sub)

/** Interface de toque, para celular e tablet. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
object MobileUi : Screens {

    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = Colors, typography = Type) {
            // Sem Scaffold, quem dá a cor do texto é isto (senão o texto sai preto).
            CompositionLocalProvider(LocalContentColor provides Look.Text, content = content)
        }
    }

    @Composable
    override fun NoticePill(notice: Notice?) {
        // O texto fica durante a animação de saída.
        var shown by remember { mutableStateOf(notice) }
        if (notice != null) shown = notice
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = BottomBarHeight + 20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = notice != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Text(
                    shown?.text.orEmpty(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    color = Look.Text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .background(Look.Pill, RoundedCornerShape(22.dp))
                        .border(1.dp, Look.Outline, RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }

    @Composable
    override fun SignIn(vm: SignInViewModel, onSignedIn: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        LaunchedEffect(state) { if (state == SignInViewModel.State.SignedIn) onSignedIn() }

        Column(
            modifier = Modifier.fillMaxSize().background(Look.Ground).windowInsetsPadding(WindowInsets.systemBars).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DoorGlyph(Modifier.size(width = 26.dp, height = 40.dp))
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            when (val s = state) {
                SignInViewModel.State.Requesting, SignInViewModel.State.SignedIn -> CircularProgressIndicator()
                is SignInViewModel.State.ShowCode -> {
                    Text(
                        stringResource(R.string.sign_in_instructions_touch),
                        textAlign = TextAlign.Center,
                        color = Look.Sub,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        s.code,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Look.GlowSoft,
                        modifier = Modifier.clickable { clipboard.setText(AnnotatedString(s.code)) },
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(s.code))
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AuthRepository.activateUrl(s.code))))
                    }) { Text(stringResource(R.string.sign_in_open_site)) }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.sign_in_waits), style = MaterialTheme.typography.bodySmall, color = Look.Sub)
                }
                is SignInViewModel.State.Error -> {
                    Text(stringResource(R.string.sign_in_error, s.message), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::requestCode) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }

    private data class TabInfo(val tab: HomeTab, val label: Int, val icon: ImageVector)

    /** As abas da barra de baixo. A Busca abre pela lupa da barra de cima. */
    private val tabs = listOf(
        TabInfo(HomeTab.SUBSCRIPTIONS, R.string.tab_subscriptions, Icons.Outlined.Subscriptions),
        TabInfo(HomeTab.RECOMMENDED, R.string.tab_recommended, Icons.Outlined.Explore),
        TabInfo(HomeTab.PLAYLISTS, R.string.tab_playlists, Icons.AutoMirrored.Outlined.PlaylistPlay),
        TabInfo(HomeTab.HISTORY, R.string.tab_history, Icons.Outlined.History),
        TabInfo(HomeTab.SETTINGS, R.string.tab_settings, Icons.Outlined.Tune),
    )

    /** No trilho lateral (tela deitada) cabe a Busca também. */
    private val railTabs = tabs.take(2) + TabInfo(HomeTab.SEARCH, R.string.tab_search, Icons.Outlined.Search) + tabs.drop(2)

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        // Voltar, na Busca, leva de volta à aba de onde ela foi aberta.
        var lastTab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
        LaunchedEffect(tab) { if (tab != HomeTab.SEARCH) lastTab = tab }
        BackHandler(enabled = tab == HomeTab.SEARCH) { onTab(lastTab) }

        val haze = rememberHazeState()
        val config = LocalConfiguration.current
        // Tela deitada (tablet, ou celular de lado): trilho de ícones à esquerda em vez da barra de baixo.
        val rail = config.screenWidthDp > config.screenHeightDp && config.screenWidthDp >= 600
        val showBottom = !rail && tab != HomeTab.SEARCH
        val system = systemInsets()
        val railWidth = RailWidth + system.start
        val bars = PaddingValues(
            start = if (rail) railWidth else system.start,
            top = system.top + TopBarHeight,
            end = system.end,
            bottom = system.bottom + if (showBottom) BottomBarHeight else 0.dp,
        )

        Box(Modifier.fillMaxSize().background(Look.Ground)) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                CompositionLocalProvider(LocalBars provides bars) { content(tab) }
            }
            val title = stringResource(railTabs.first { it.tab == tab }.label)
            TopBar(haze, system, Modifier.padding(start = if (rail) railWidth else 0.dp)) {
                if (tab == HomeTab.SEARCH && !rail) BackButton { onTab(lastTab) } else Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, letterSpacing = (-0.2).sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (tab != HomeTab.SEARCH && !rail) {
                    IconButton(onClick = { onTab(HomeTab.SEARCH) }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.tab_search))
                    }
                }
            }
            if (rail) {
                Rail(haze, system, railTabs, tab, onTab, Modifier.align(Alignment.CenterStart))
            } else if (showBottom) {
                BottomNav(haze, system, tab, onTab, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    /** As barras do sistema (status, navegação). O recorte da câmera fica de fora de propósito. */
    private class SystemInsets(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp)

    @Composable
    private fun systemInsets(): SystemInsets {
        val dir = LocalLayoutDirection.current
        val nav = WindowInsets.navigationBars.asPaddingValues()
        return SystemInsets(
            start = nav.calculateStartPadding(dir),
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            end = nav.calculateEndPadding(dir),
            bottom = nav.calculateBottomPadding(),
        )
    }

    /** Barra de cima de vidro: passa por trás da barra de status. */
    @Composable
    private fun TopBar(haze: HazeState, system: SystemInsets, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
        Box(modifier.fillMaxWidth().height(system.top + TopBarHeight).hazeEffect(haze, GlassStyle)) {
            Row(
                Modifier.fillMaxSize().padding(top = system.top, start = 4.dp, end = system.end + 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
            Hairline(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp))
        }
    }

    /** Barra de abas de vidro, só com ícones: vai até o fim da tela, por trás da barra de gestos. */
    @Composable
    private fun BottomNav(haze: HazeState, system: SystemInsets, tab: HomeTab, onTab: (HomeTab) -> Unit, modifier: Modifier) {
        Box(modifier.fillMaxWidth().height(system.bottom + BottomBarHeight).hazeEffect(haze, GlassStyle)) {
            Hairline(Modifier.align(Alignment.TopStart).fillMaxWidth().height(1.dp))
            Row(
                Modifier.align(Alignment.TopCenter).widthIn(max = 520.dp).fillMaxWidth().height(BottomBarHeight)
                    .padding(start = system.start + 12.dp, end = system.end + 12.dp),
            ) {
                tabs.forEach { info ->
                    val selected = info.tab == tab
                    val label = stringResource(info.label)
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected = selected, role = Role.Tab, onClick = { onTab(info.tab) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(info.icon, contentDescription = label, tint = if (selected) Look.Text else Look.Sub)
                        if (selected) LightSlit(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(14.dp, 3.dp))
                    }
                }
            }
        }
    }

    /** Trilho de vidro à esquerda, na tela deitada: vai de cima a baixo, por trás das barras do sistema. */
    @Composable
    private fun Rail(haze: HazeState, system: SystemInsets, items: List<TabInfo>, tab: HomeTab, onTab: (HomeTab) -> Unit, modifier: Modifier) {
        Box(modifier.fillMaxHeight().width(RailWidth + system.start).hazeEffect(haze, GlassStyle)) {
            Column(
                Modifier.fillMaxHeight().padding(start = system.start, top = system.top + 20.dp, bottom = system.bottom + 16.dp).width(RailWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DoorGlyph(Modifier.size(width = 20.dp, height = 30.dp))
                Spacer(Modifier.height(24.dp))
                items.forEach { info ->
                    val selected = info.tab == tab
                    Box(
                        Modifier.fillMaxWidth().height(56.dp)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onTab(info.tab) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(info.icon, contentDescription = stringResource(info.label), tint = if (selected) Look.Text else Look.Sub)
                        if (selected) LightSlit(Modifier.align(Alignment.CenterStart).size(3.dp, 22.dp))
                    }
                }
            }
            Hairline(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(1.dp))
        }
    }

    @Composable
    private fun Hairline(modifier: Modifier) {
        Box(modifier.background(Look.Hairline))
    }

    /** Página fora das abas (playlist, canal): barra de vidro com o Voltar, e o conteúdo rolando por baixo. */
    @Composable
    private fun DetailFrame(
        title: String,
        navigation: @Composable () -> Unit,
        actions: @Composable RowScope.() -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        val haze = rememberHazeState()
        val system = systemInsets()
        val bars = PaddingValues(start = system.start, top = system.top + TopBarHeight, end = system.end, bottom = system.bottom)
        Box(Modifier.fillMaxSize().background(Look.Ground)) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                CompositionLocalProvider(LocalBars provides bars) { content() }
            }
            TopBar(haze, system) {
                navigation()
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                actions()
            }
        }
    }

    /** O contentPadding das listas: o espaço das barras de vidro mais uma margem. */
    @Composable
    private fun barsPlus(start: Dp = 16.dp, top: Dp = 16.dp, end: Dp = 16.dp, bottom: Dp = 28.dp): PaddingValues {
        val bars = LocalBars.current
        val dir = LocalLayoutDirection.current
        return PaddingValues(
            start = bars.calculateStartPadding(dir) + start,
            top = bars.calculateTopPadding() + top,
            end = bars.calculateEndPadding(dir) + end,
            bottom = bars.calculateBottomPadding() + bottom,
        )
    }

    @Composable
    override fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.feed_empty), onNearEnd = vm::loadMore, onPlay = onPlay, onOpenChannel = onOpenChannel)
        }
    }

    @Composable
    override fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.recommended_empty), onNearEnd = vm::loadMore, onPlay = onPlay, onOpenChannel = onOpenChannel)
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.history_empty), onNearEnd = vm::loadMore, onPlay = onPlay, onOpenChannel = onOpenChannel)
        }
    }

    @Composable
    override fun Search(
        vm: SearchViewModel,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
    ) {
        val filters by vm.filters.collectAsStateWithLifecycle()
        val results by vm.results.collectAsStateWithLifecycle()
        val suggestions by vm.suggestions.collectAsStateWithLifecycle()
        // O texto do campo fica aqui (um StateFlow no campo faz o cursor pular).
        var text by rememberSaveable { mutableStateOf(vm.query.value) }
        var fieldFocused by remember { mutableStateOf(false) }
        val focus = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val submit = { query: String ->
            text = query
            vm.search(query)
            keyboard?.hide()
            focusManager.clearFocus()
        }
        LaunchedEffect(Unit) { if (results == null) runCatching { focus.requestFocus() } }

        val bars = LocalBars.current
        val dir = LocalLayoutDirection.current
        Column(
            Modifier.fillMaxSize().padding(
                start = bars.calculateStartPadding(dir),
                top = bars.calculateTopPadding(),
                end = bars.calculateEndPadding(dir),
            ),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    vm.onQueryChange(it)
                },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    .focusRequester(focus)
                    .onFocusChanged { fieldFocused = it.isFocused },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (text.isEmpty()) null else {
                    {
                        IconButton(onClick = {
                            text = ""
                            vm.onQueryChange("")
                            runCatching { focus.requestFocus() }
                        }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Look.Surface,
                    unfocusedContainerColor = Look.Surface,
                    focusedBorderColor = Look.Glow.copy(alpha = 0.55f),
                    unfocusedBorderColor = Look.Hairline,
                    cursorColor = Look.Glow,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(text) }),
            )
            FilterRow(filters, onChange = vm::setFilters)
            if (fieldFocused && suggestions.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bars.calculateBottomPadding() + 16.dp)) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Look.Sub) },
                            modifier = Modifier.clickable { submit(suggestion) },
                        )
                    }
                }
            } else {
                results?.let { state ->
                    CompositionLocalProvider(LocalBars provides PaddingValues(bottom = bars.calculateBottomPadding())) {
                        Loadable(state, onRetry = { vm.search() }) { data ->
                            SearchResults(data, onNearEnd = vm::loadMore, onPlay, onOpenChannel, onOpenPlaylist)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FilterRow(filters: SearchFilters, onChange: (SearchFilters) -> Unit) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            searchFilterChoices(filters).forEach { choice ->
                key(choice.label) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = !choice.isDefault,
                            onClick = { open = true },
                            label = { Text(stringResource(if (choice.isDefault) choice.label else choice.selected.label)) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Look.Text,
                                iconColor = Look.Sub,
                                selectedContainerColor = Look.Glow.copy(alpha = 0.14f),
                                selectedLabelColor = Look.GlowSoft,
                                selectedTrailingIconColor = Look.GlowSoft,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !choice.isDefault,
                                borderColor = Look.Outline,
                                selectedBorderColor = Look.Glow.copy(alpha = 0.45f),
                            ),
                        )
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            choice.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.label)) },
                                    leadingIcon = { if (option.selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Look.Glow) },
                                    onClick = {
                                        open = false
                                        onChange(option.filters)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchResults(
        data: SearchViewModel.Results,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
    ) {
        val grid = rememberLazyGridState()
        NearEnd(grid, data.items.size, onNearEnd)
        val cards = rememberCardState()
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(minSize = 240.dp),
            contentPadding = barsPlus(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (data.items.isEmpty() && !data.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.search_empty), textAlign = TextAlign.Center, color = Look.Sub, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(
                data.items,
                key = { it.key },
                span = { if (it is SearchItem.ChannelItem) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { item ->
                when (item) {
                    is SearchItem.VideoItem -> VideoCard(item.video, cards, onPlay, onOpenChannel)
                    is SearchItem.ChannelItem -> ChannelRow(item.channel, onClick = { onOpenChannel(item.channel) })
                    is SearchItem.PlaylistItem -> PlaylistRow(item.playlist, onClick = { onOpenPlaylist(item.playlist) })
                }
            }
            if (data.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) { Progress() }
            }
        }
    }

    @Composable
    override fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload, refresh = vm) { playlists -> PlaylistGrid(playlists, onOpen) }
    }

    @Composable
    private fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Playlist) -> Unit, header: (@Composable () -> Unit)? = null) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = barsPlus(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            items(playlists, key = { it.browseId }) { playlist ->
                PlaylistRow(playlist, onClick = { onOpen(playlist) })
            }
        }
    }

    @Composable
    override fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit) {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val playlists by vm.playlists.collectAsStateWithLifecycle()
        val ownPlaylists by vm.ownPlaylists.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val appIcon by vm.appIcon.collectAsStateWithLifecycle()
        var choosingTarget by rememberSaveable { mutableStateOf(false) }
        // Tópico aberto no diálogo: a lista e a posição (null = tópico novo).
        var editing by remember { mutableStateOf<Pair<TopicList, Int?>?>(null) }

        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = barsPlus(start = 0.dp, top = 4.dp, end = 0.dp),
        ) {
            item { SectionTitle(stringResource(R.string.settings_subscriptions)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_show_lives)) },
                    supportingContent = { Text(stringResource(R.string.settings_show_lives_desc)) },
                    trailingContent = { Switch(checked = settings.showLives, onCheckedChange = null) },
                    modifier = Modifier.clickable(role = Role.Switch) { vm.setShowLives(!settings.showLives) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_save_target)) }
            item {
                val target = settings.saveTarget.title ?: stringResource(R.string.watch_later)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_save_target_current, target)) },
                    supportingContent = { Text(stringResource(R.string.settings_save_target_choose)) },
                    trailingContent = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.clickable {
                        choosingTarget = !choosingTarget
                        if (choosingTarget) vm.loadOwnPlaylists()
                    },
                )
            }
            if (choosingTarget) {
                when (val own = ownPlaylists) {
                    null, UiState.Loading -> item { Progress() }
                    is UiState.Error -> item { ErrorRow(own.message, onRetry = vm::loadOwnPlaylists) }
                    is UiState.Ready -> items(own.data, key = { "target:" + it.id }) { playlist ->
                        val selected = playlist.id == settings.saveTarget.playlistId
                        ListItem(
                            headlineContent = { Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { RadioButton(selected = selected, onClick = null) },
                            modifier = Modifier.selectable(selected = selected, role = Role.RadioButton) {
                                vm.setSaveTarget(playlist)
                                choosingTarget = false
                            },
                        )
                    }
                }
            }

            topicSection(
                title = R.string.settings_excluded,
                hint = R.string.settings_excluded_hint,
                topics = settings.filters.excluded,
                onEdit = { editing = TopicList.EXCLUDED to it },
                onRemove = { vm.removeTopic(TopicList.EXCLUDED, it) },
            )
            topicSection(
                title = R.string.settings_preferred,
                hint = R.string.settings_preferred_hint,
                topics = settings.filters.preferred,
                onEdit = { editing = TopicList.PREFERRED to it },
                onRemove = { vm.removeTopic(TopicList.PREFERRED, it) },
            )

            item { SectionTitle(stringResource(R.string.settings_blocked)) }
            item { Hint(stringResource(R.string.settings_blocked_hint)) }
            if (settings.filters.blocked.isEmpty()) {
                item { EmptyLine(stringResource(R.string.settings_blocked_empty)) }
            }
            items(settings.filters.blocked, key = { "blocked:" + it.id }) { channel ->
                ListItem(
                    headlineContent = { Text(channel.name.ifBlank { channel.id }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = { TextButton(onClick = { vm.unblock(channel.id) }) { Text(stringResource(R.string.unblock)) } },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_links)) }
            items(externalLinkOptions) { (mode, label) ->
                ListItem(
                    headlineContent = { Text(stringResource(label)) },
                    leadingContent = { RadioButton(selected = settings.externalLinks == mode, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = settings.externalLinks == mode,
                        role = Role.RadioButton,
                        onClick = { vm.setExternalLinks(mode) },
                    ),
                )
            }

            item { SectionTitle(stringResource(R.string.settings_playlists)) }
            item { Hint(stringResource(R.string.settings_playlists_hint)) }
            error?.let { message ->
                item {
                    Text(
                        stringResource(R.string.action_failed, message),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            when (val p = playlists) {
                UiState.Loading -> item { Progress() }
                is UiState.Error -> item { ErrorRow(p.message, onRetry = vm::reloadPlaylists) }
                is UiState.Ready -> {
                    if (p.data.isEmpty()) {
                        item { EmptyLine(stringResource(R.string.settings_playlists_empty)) }
                    }
                    items(p.data, key = { it.playlist.browseId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.playlist.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = item.playlist.info?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                            leadingContent = { Thumbnail(item.playlist.thumbnailUrl, Modifier.width(96.dp), corner = 8.dp) },
                            trailingContent = {
                                if (item.busy) CircularProgressIndicator(Modifier.size(24.dp))
                                else TextButton(onClick = { vm.unsubscribe(item.playlist) }) {
                                    Text(stringResource(R.string.settings_unsubscribe))
                                }
                            },
                            modifier = Modifier.clickable { onOpenPlaylist(item.playlist) },
                        )
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_app_icon)) }
            item { Hint(stringResource(R.string.settings_app_icon_hint)) }
            items(AppIcon.entries) { icon ->
                ListItem(
                    headlineContent = { Text(stringResource(icon.label)) },
                    leadingContent = { AppIconPreview(icon) },
                    trailingContent = { RadioButton(selected = appIcon == icon, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = appIcon == icon,
                        role = Role.RadioButton,
                        onClick = { vm.setAppIcon(icon) },
                    ),
                )
            }

            item { SectionTitle(stringResource(R.string.settings_account)) }
            item {
                ListItem(
                    headlineContent = { Text(vm.accountName?.let { stringResource(R.string.settings_signed_in_as, it) } ?: "") },
                    trailingContent = {
                        OutlinedButton(onClick = { vm.signOut(onSignedOut) }) { Text(stringResource(R.string.sign_out)) }
                    },
                )
            }
        }

        editing?.let { (list, index) ->
            val topics = if (list == TopicList.EXCLUDED) settings.filters.excluded else settings.filters.preferred
            TopicDialog(
                initial = index?.let(topics::getOrNull),
                onDismiss = { editing = null },
                onSave = { name, keywords ->
                    vm.saveTopic(list, index, name, keywords)
                    editing = null
                },
            )
        }
    }

    private fun LazyListScope.topicSection(
        title: Int,
        hint: Int,
        topics: List<Topic>,
        onEdit: (Int?) -> Unit,
        onRemove: (Int) -> Unit,
    ) {
        item { SectionTitle(stringResource(title)) }
        item { Hint(stringResource(hint)) }
        if (topics.isEmpty()) item { EmptyLine(stringResource(R.string.settings_topics_empty)) }
        itemsIndexed(topics) { index, topic ->
            ListItem(
                headlineContent = { Text(topic.name) },
                supportingContent = { Text(topic.keywords.joinToString(", "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                trailingContent = {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.topic_remove), tint = Look.Sub)
                    }
                },
                modifier = Modifier.clickable { onEdit(index) },
            )
        }
        item {
            TextButton(onClick = { onEdit(null) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(R.string.topic_add))
            }
        }
    }

    @Composable
    private fun TopicDialog(initial: Topic?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
        var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
        var keywords by rememberSaveable { mutableStateOf(initial?.keywords?.joinToString(", ").orEmpty()) }
        val valid = name.isNotBlank() && TopicRules.parseKeywords(keywords).isNotEmpty()
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(if (initial == null) R.string.topic_add else R.string.topic_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.topic_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text(stringResource(R.string.topic_keywords)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(name, keywords) }, enabled = valid) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }

    @Composable
    override fun PlaylistPage(
        title: String,
        vm: PlaylistViewModel,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onBack: () -> Unit,
    ) {
        val state by vm.state.collectAsStateWithLifecycle()
        val subscription by vm.subscription.collectAsStateWithLifecycle()
        val reorder by vm.reorder.collectAsStateWithLifecycle()
        val saveTarget by SavedVideos.target.collectAsStateWithLifecycle()
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }
        val editing = reorder as? Reorder.Editing
        BackHandler(enabled = editing != null) { vm.cancelReorder() }

        DetailFrame(
            title = shownTitle,
            navigation = {
                if (editing != null) {
                    IconButton(onClick = vm::cancelReorder, enabled = !editing.saving) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                } else {
                    BackButton(onBack)
                }
            },
            actions = {
                when (val r = reorder) {
                    Reorder.Unavailable -> Unit
                    is Reorder.Available ->
                        if (r.preparing) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp))
                        else IconButton(onClick = vm::startReorder, enabled = state is UiState.Ready) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = stringResource(R.string.reorder))
                        }
                    is Reorder.Editing -> TextButton(onClick = vm::saveOrder, enabled = !r.saving) {
                        Text(stringResource(if (r.saving) R.string.reorder_saving else R.string.reorder_save))
                    }
                }
            },
        ) {
            if (editing != null) {
                ReorderList(editing, onMove = vm::move)
            } else {
                // Enquanto o modo de reordenar carrega a playlist inteira, puxar para atualizar fica desligado.
                val preparing = (reorder as? Reorder.Available)?.preparing == true
                Loadable(state, onRetry = vm::reload, refresh = vm.takeUnless { preparing }) { content ->
                    val shown = subscription as? PlaylistViewModel.Subscription.Shown
                    val loadFailed = (reorder as? Reorder.Available)?.loadFailed == true
                    VideoRows(
                        content = content,
                        onNearEnd = vm::loadMore,
                        onPlay = onPlay,
                        onOpenChannel = onOpenChannel,
                        // Na própria playlist do botão Save, o marcador estaria em todos os vídeos.
                        showSaved = vm.playlistId != saveTarget.playlistId,
                        header = when {
                            shown != null -> {
                                { SubscribeHeader(shown) { vm.toggleSubscription(shownTitle) } }
                            }
                            loadFailed -> {
                                { Text(stringResource(R.string.reorder_load_failed), color = MaterialTheme.colorScheme.error) }
                            }
                            else -> null
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun ReorderList(editing: Reorder.Editing, onMove: (Int, Int) -> Unit) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = barsPlus(start = 0.dp, top = 8.dp, end = 0.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Hint(stringResource(R.string.reorder_hint_touch), Modifier)
                    editing.error?.let {
                        Text(stringResource(R.string.reorder_failed, it), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            itemsIndexed(editing.videos, key = { _, video -> video.id }) { index, video ->
                Row(
                    Modifier.animateItem().fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Look.Sub,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Thumbnail(video.thumbnailUrl, Modifier.width(104.dp), corner = 8.dp)
                    Text(
                        video.title,
                        style = RowTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0 && !editing.saving) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.reorder_move_up))
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = index < editing.videos.lastIndex && !editing.saving) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.reorder_move_down))
                    }
                }
            }
        }
    }

    @Composable
    override fun ChannelPage(
        title: String,
        vm: ChannelViewModel,
        onPlay: (Video) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
        onBack: () -> Unit,
    ) {
        val state by vm.state.collectAsStateWithLifecycle()
        val blocked by vm.blocked.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        DetailFrame(
            title = title.ifBlank { stringResource(R.string.channel_default_title) },
            navigation = { BackButton(onBack) },
        ) {
            Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
                val hasPlaylists = content.playlists.isNotEmpty()
                val header: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = vm::toggleBlocked, border = BorderStroke(1.dp, Look.Outline)) {
                            Text(stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block))
                        }
                        if (blocked) Hint(stringResource(R.string.channel_blocked_hint), Modifier)
                        if (hasPlaylists) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(R.string.channel_videos, R.string.channel_playlists).forEachIndexed { i, label ->
                                    Segment(stringResource(label), selected = tab == i) { tab = i }
                                }
                            }
                        }
                    }
                }
                if (tab == 1 && hasPlaylists) {
                    PlaylistGrid(content.playlists, onOpenPlaylist, header = header)
                } else {
                    VideoGrid(
                        content.videos,
                        emptyText = stringResource(R.string.list_empty),
                        onNearEnd = vm::loadMore,
                        onPlay = onPlay,
                        onOpenChannel = null,
                        header = header,
                    )
                }
            }
        }
    }

    @Composable
    private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            shape = RoundedCornerShape(18.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = Look.Sub,
                selectedContainerColor = Look.SurfaceHigh,
                selectedLabelColor = Look.Text,
            ),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Look.Outline, selectedBorderColor = Color.Transparent),
        )
    }

    @Composable
    private fun SubscribeHeader(state: PlaylistViewModel.Subscription.Shown, onToggle: () -> Unit) {
        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.subscribed) {
                    // Inscrito: contorno âmbar e um ✓. Tocar de novo cancela a inscrição.
                    OutlinedButton(
                        onClick = onToggle,
                        enabled = !state.busy,
                        border = BorderStroke(1.dp, Look.Glow.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Look.Glow),
                        contentPadding = PaddingValues(start = 16.dp, end = 22.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.playlist_subscribed))
                    }
                } else {
                    Button(onClick = onToggle, enabled = !state.busy, contentPadding = PaddingValues(start = 16.dp, end = 22.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.playlist_subscribe))
                    }
                }
                if (state.busy) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            Text(
                stringResource(if (state.subscribed) R.string.playlist_subscribed_hint_touch else R.string.playlist_subscribe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Look.Sub,
                modifier = Modifier.padding(top = 8.dp),
            )
            state.error?.let {
                Text(stringResource(R.string.action_failed, it), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    /** Chama [onNearEnd] quando a rolagem chega perto do fim (para carregar a próxima página). */
    @Composable
    private fun NearEnd(grid: LazyGridState, count: Int, onNearEnd: () -> Unit) {
        val nearEnd by remember {
            derivedStateOf {
                val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= grid.layoutInfo.totalItemsCount - 6
            }
        }
        LaunchedEffect(nearEnd, count) { if (nearEnd) onNearEnd() }
    }

    /** O que os cards precisam além do vídeo: quais estão no Watch later e as fotos dos canais. */
    private class CardState(val saved: Set<String>, val photos: Map<String, String>, val showSaved: Boolean, val onLongPress: (Video) -> Unit)

    @Composable
    private fun rememberCardState(showSaved: Boolean = true): CardState {
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        val photos by ChannelPhotos.photos.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        return CardState(saved, photos, showSaved) { toggleSaved(res, it) }
    }

    @Composable
    private fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: ((Channel) -> Unit)?,
        header: (@Composable () -> Unit)? = null,
    ) {
        val grid = rememberLazyGridState()
        NearEnd(grid, content.videos.size, onNearEnd)
        val cards = rememberCardState()
        LazyVerticalGrid(
            state = grid,
            // Uma coluna no celular, três no tablet em pé, quatro deitado.
            columns = GridCells.Adaptive(minSize = 240.dp),
            contentPadding = barsPlus(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (content.videos.isEmpty() && !content.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, textAlign = TextAlign.Center, color = Look.Sub, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(content.videos, key = { it.id + it.playlistIndex }) { video ->
                VideoCard(video, cards, onPlay, onOpenChannel)
            }
            if (content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) { Progress() }
            }
        }
    }

    /** Lista de uma playlist: miniatura pequena ao lado do título (uma coluna no celular, duas no tablet deitado). */
    @Composable
    private fun VideoRows(
        content: VideoListContent,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        showSaved: Boolean,
        header: (@Composable () -> Unit)?,
    ) {
        val grid = rememberLazyGridState()
        NearEnd(grid, content.videos.size, onNearEnd)
        val cards = rememberCardState(showSaved)
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(minSize = 520.dp),
            contentPadding = barsPlus(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (content.videos.isEmpty() && !content.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.list_empty), textAlign = TextAlign.Center, color = Look.Sub, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(content.videos, key = { it.id + it.playlistIndex }) { video ->
                VideoRow(video, cards, onPlay, onOpenChannel)
            }
            if (content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) { Progress() }
            }
        }
    }

    /**
     * Card de vídeo. Tocar abre o vídeo; tocar e segurar põe ou tira do Watch later (com um aviso);
     * o avatar e o nome do canal abrem o canal.
     */
    @Composable
    private fun VideoCard(video: Video, cards: CardState, onPlay: (Video) -> Unit, onOpenChannel: ((Channel) -> Unit)?) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).combinedClickable(
                onClickLabel = video.title,
                onLongClickLabel = stringResource(R.string.video_toggle_save),
                onLongClick = { cards.onLongPress(video) },
                onClick = { onPlay(video) },
            ),
        ) {
            VideoThumb(video, saved = cards.showSaved && video.id in cards.saved, corner = 14.dp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(video.title, style = CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
            MetaRow(video, cards.photos[video.channelId], onOpenChannel)
        }
    }

    @Composable
    private fun VideoRow(video: Video, cards: CardState, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).combinedClickable(
                onClickLabel = video.title,
                onLongClickLabel = stringResource(R.string.video_toggle_save),
                onLongClick = { cards.onLongPress(video) },
                onClick = { onPlay(video) },
            ),
            verticalAlignment = Alignment.Top,
        ) {
            VideoThumb(video, saved = cards.showSaved && video.id in cards.saved, corner = 10.dp, modifier = Modifier.width(152.dp), compact = true)
            Column(Modifier.weight(1f).padding(start = 14.dp, top = 1.dp)) {
                Text(video.title, style = RowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                MetaRow(video, cards.photos[video.channelId], onOpenChannel)
            }
        }
    }

    /** Miniatura com a duração, o progresso em âmbar e, se o vídeo estiver no Watch later, o marcador discreto. */
    @Composable
    private fun VideoThumb(video: Video, saved: Boolean, corner: Dp, modifier: Modifier, compact: Boolean = false) {
        Box(modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(corner)).background(Look.Surface)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            val badge = if (video.isLive) stringResource(R.string.search_live) else video.durationMs.takeIf { it > 0 }?.let(::formatDuration)
            if (badge != null) {
                Text(
                    badge,
                    color = Look.Text,
                    style = TextStyle(fontFamily = Look.Figtree, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 11.sp else 12.sp, lineHeight = 16.sp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 8.dp)
                        .background(if (video.isLive) Look.Live else Look.Badge, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            if (video.percentWatched > 0) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(video.percentWatched.coerceIn(0, 100) / 100f).height(3.dp)
                        .background(Look.Glow),
                )
            }
            if (saved) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(if (compact) 6.dp else 8.dp).size(if (compact) 22.dp else 24.dp)
                        .background(Look.BadgeSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_bookmark),
                        contentDescription = stringResource(R.string.video_saved),
                        tint = Look.Glow,
                        modifier = Modifier.size(if (compact) 12.dp else 13.dp),
                    )
                }
            }
        }
    }

    /** "[avatar] Canal · 12K views · 2 hours ago". O avatar e o nome levam ao canal. */
    @Composable
    private fun MetaRow(video: Video, photo: String?, onOpenChannel: ((Channel) -> Unit)?) {
        val rest = video.fromPlaylist?.let { stringResource(R.string.from_playlist_short, it) }
            ?: metaAfterChannel(video.info, video.channelName, video.published)
        val name = video.channelName?.takeIf { it.isNotBlank() }
        val channel = channelOf(video)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (name != null) {
                val open = if (onOpenChannel != null && channel != null) {
                    val label = stringResource(R.string.video_open_channel, name)
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = label) { onOpenChannel(channel) }
                } else {
                    Modifier
                }
                Row(
                    Modifier.weight(1f, fill = false).then(open).padding(horizontal = 2.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(name, photo, 18.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(name, style = Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (rest != null) {
                Text(
                    (if (name != null) "· " else "") + rest,
                    style = Meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(start = if (name != null) 3.dp else 2.dp, top = 7.dp, bottom = 7.dp),
                )
            }
        }
    }

    /** Foto redonda do canal; sem foto (canal em que não está inscrito), a primeira letra. */
    @Composable
    private fun Avatar(name: String, photo: String?, size: Dp) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape).background(Look.monogramTone(name)),
            )
        } else {
            Box(Modifier.size(size).background(Look.monogramTone(name), CircleShape), contentAlignment = Alignment.Center) {
                Text(
                    monogramOf(name),
                    color = Look.Text,
                    style = TextStyle(fontFamily = Look.Figtree, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.5f).sp, lineHeight = (size.value * 0.6f).sp),
                )
            }
        }
    }

    @Composable
    private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(playlist.thumbnailUrl, Modifier.width(152.dp), corner = 12.dp)
            Column(Modifier.padding(horizontal = 14.dp)) {
                Text(playlist.title, style = RowTitle.copy(fontSize = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                playlist.info?.let { Text(it, style = Meta, maxLines = 2, modifier = Modifier.padding(top = 3.dp)) }
            }
        }
    }

    @Composable
    private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (channel.thumbnailUrl != null) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Look.Surface),
                )
            } else {
                Avatar(channel.title, null, 64.dp)
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(channel.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                channel.info?.let { Text(it, style = Meta, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }

    @Composable
    private fun BackButton(onBack: () -> Unit) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.2.sp),
            color = Look.Sub,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 6.dp),
        )
    }

    @Composable
    private fun Hint(text: String, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Look.Sub, modifier = modifier)
    }

    @Composable
    private fun EmptyLine(text: String) {
        Text(text, color = Look.Sub, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }

    @Composable
    private fun Progress() {
        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() }
    }

    @Composable
    private fun ErrorRow(message: String, onRetry: () -> Unit) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.error_generic, message), Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }

    @Composable
    private fun Thumbnail(url: String?, modifier: Modifier, corner: Dp) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(corner)).background(Look.Surface),
            contentScale = ContentScale.Crop,
        )
    }

    /**
     * Carregando, erro ou o conteúdo. Com [refresh], o conteúdo ganha o "puxar para atualizar": a
     * lista continua na tela enquanto a nova carrega, e o círculo aparece logo abaixo da barra de vidro.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun <T> Loadable(state: UiState<T>, onRetry: () -> Unit, refresh: Refreshable? = null, content: @Composable (T) -> Unit) {
        when (state) {
            UiState.Loading -> Box(Modifier.fillMaxSize().padding(LocalBars.current), Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(
                Modifier.fillMaxSize().padding(LocalBars.current).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.error_generic, state.message), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Ready -> {
                val refreshing = refresh?.refreshing?.collectAsStateWithLifecycle()?.value == true
                val pull = rememberPullToRefreshState()
                Box(Modifier.fillMaxSize().pullToRefresh(refreshing, pull, enabled = refresh != null) { refresh?.refresh() }) {
                    content(state.data)
                    PullToRefreshDefaults.Indicator(
                        state = pull,
                        isRefreshing = refreshing,
                        containerColor = Look.SurfaceHigh,
                        color = Look.Glow,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = LocalBars.current.calculateTopPadding()),
                    )
                }
            }
        }
    }
}
