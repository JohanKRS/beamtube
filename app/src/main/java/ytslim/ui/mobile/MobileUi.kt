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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
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
import ytslim.ui.ExploreViewModel
import ytslim.ui.Accent
import ytslim.ui.Accents
import ytslim.ui.DoorGlyph
import ytslim.ui.FeedViewModel
import ytslim.ui.HistoryViewModel
import ytslim.ui.HomeTab
import ytslim.ui.LightSlit
import ytslim.ui.MetaLine
import ytslim.ui.ListMotion
import ytslim.ui.Look
import ytslim.ui.Notice
import ytslim.ui.NoticeAction
import ytslim.ui.Notices
import ytslim.ui.PlaylistViewModel
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.PrefetchThumbnails
import ytslim.ui.RecommendedViewModel
import ytslim.ui.Refreshable
import ytslim.ui.Screens
import ytslim.ui.SearchFilterChoice
import ytslim.ui.SearchViewModel
import ytslim.ui.SettingsViewModel
import ytslim.ui.SettingsViewModel.TopicList
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListContent
import ytslim.ui.channelOf
import ytslim.ui.externalLinkOptions
import ytslim.ui.feedFilterChoices
import ytslim.ui.formatDuration
import ytslim.ui.metaAfterChannel
import ytslim.ui.monogramOf
import ytslim.ui.searchFilterChoices
import ytslim.ui.text
import ytslim.ui.toggleSaved

/** Altura das barras de vidro, sem contar as barras do sistema que ficam atrás delas. */
private val TopBarHeight = 56.dp

/** A chave do aviso no topo da lista de reordenar (os vídeos usam o próprio ID). */
private val BottomBarHeight = 60.dp
private val RailWidth = 80.dp

/** Quantos cartões o esqueleto de carregamento mostra (o bastante para encher a tela). */
private const val SKELETON_CARDS = 8

/**
 * Quanto as barras de vidro cobrem de cada lado. O conteúdo ocupa a tela inteira e rola por baixo
 * delas; as listas usam isto no contentPadding para o primeiro e o último item ficarem visíveis.
 */
private val LocalBars = compositionLocalOf { PaddingValues(0.dp) }

/** De que borda da tela a barra de vidro sai: ali ela é mais opaca, e menos junto da lista. */
private enum class GlassEdge { Top, Bottom, Start }

/** Vidro fosco com o desfoque do que passa por baixo (Android 12+; antes, quase opaco), em degradê (ver [Look.GlassEdge]). */
private fun glassStyle(edge: GlassEdge): HazeStyle {
    fun fade(outer: Color, inner: Color): HazeTint = HazeTint(
        when (edge) {
            GlassEdge.Top -> Brush.verticalGradient(listOf(outer, inner))
            GlassEdge.Bottom -> Brush.verticalGradient(listOf(inner, outer))
            GlassEdge.Start -> Brush.horizontalGradient(listOf(outer, inner))
        },
    )
    return HazeStyle(
        backgroundColor = Look.Ground,
        tints = listOf(fade(Look.GlassEdge, Look.GlassInner)),
        blurRadius = 24.dp,
        noiseFactor = 0f,
        fallbackTint = fade(Look.GlassFallbackEdge, Look.GlassFallbackInner),
    )
}

private val TopGlass = glassStyle(GlassEdge.Top)
private val BottomGlass = glassStyle(GlassEdge.Bottom)
private val StartGlass = glassStyle(GlassEdge.Start)

/**
 * O desfoque calculado numa cópia menor do que passa por baixo e ampliado (o Haze escolhe a
 * escala): com 24 dp de desfoque a diferença não aparece, e rolar fica bem mais barato
 * (2026-11-03, sugestão da thread de desempenho).
 */
@OptIn(ExperimentalHazeApi::class)
private val LightGlass: HazeEffectScope.() -> Unit = { inputScale = HazeInputScale.Auto }

/** As cores do tema, refeitas quando a cor de destaque muda ([Look.accent]). */
private fun colors() = darkColorScheme(
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
        MaterialTheme(colorScheme = remember(Look.accent) { colors() }, typography = Type) {
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
                val action = shown?.action
                Row(
                    Modifier.padding(horizontal = 24.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .background(Look.Pill, RoundedCornerShape(22.dp))
                        .border(1.dp, Look.Outline, RoundedCornerShape(22.dp))
                        .padding(start = 18.dp, end = if (action != null) 6.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        shown?.text.orEmpty(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        color = Look.Text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp).weight(1f, fill = false),
                    )
                    if (action != null) {
                        // "Undo": desfaz e a pílula some na hora.
                        TextButton(onClick = {
                            action.run()
                            shown?.let { Notices.dismiss(it.id) }
                        }) {
                            Text(action.label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = Look.Glow)
                        }
                    }
                }
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

    /**
     * As abas da barra de baixo, na ordem do deslizar de lado. A Busca abre pela lupa da barra de
     * cima e as Configurações pelo avatar da conta, também em cima.
     */
    private val tabs = listOf(
        TabInfo(HomeTab.SUBSCRIPTIONS, R.string.tab_subscriptions, Icons.Outlined.Subscriptions),
        TabInfo(HomeTab.RECOMMENDED, R.string.tab_recommended, Icons.Outlined.Explore),
        TabInfo(HomeTab.EXPLORE, R.string.tab_explore, Icons.Outlined.GridView),
        TabInfo(HomeTab.PLAYLISTS, R.string.tab_playlists, Icons.AutoMirrored.Outlined.PlaylistPlay),
        TabInfo(HomeTab.HISTORY, R.string.tab_history, Icons.Outlined.History),
    )
    private val swipeTabs = tabs.map { it.tab }

    /** No trilho lateral (tela deitada) cabe a Busca também. */
    private val railTabs = tabs.take(3) + TabInfo(HomeTab.SEARCH, R.string.tab_search, Icons.Outlined.Search) + tabs.drop(3)

    /** Os títulos da barra de cima, inclusive das telas que não estão nas barras de abas. */
    private val titles = railTabs + TabInfo(HomeTab.SETTINGS, R.string.tab_settings, Icons.Outlined.Tune)

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        // Voltar, na Busca e nas Configurações, leva de volta à aba de onde elas foram abertas.
        var lastTab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
        LaunchedEffect(tab) { if (tab in swipeTabs) lastTab = tab }
        val aside = tab !in swipeTabs
        BackHandler(enabled = aside) { onTab(lastTab) }

        val haze = rememberHazeState()
        val config = LocalConfiguration.current
        // Tela deitada (tablet, ou celular de lado): trilho de ícones à esquerda em vez da barra de baixo.
        val rail = config.screenWidthDp > config.screenHeightDp && config.screenWidthDp >= 600
        val showBottom = !rail && !aside
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
                CompositionLocalProvider(LocalBars provides bars) {
                    if (aside) content(tab) else SwipeTabs(tab, onTab, content)
                }
            }
            val title = stringResource(titles.first { it.tab == tab }.label)
            TopBar(haze, system, Modifier.padding(start = if (rail) railWidth else 0.dp)) {
                val back = tab == HomeTab.SETTINGS || (tab == HomeTab.SEARCH && !rail)
                if (back) BackButton { onTab(lastTab) } else Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, letterSpacing = (-0.2).sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (!aside && !rail) {
                    IconButton(onClick = { onTab(HomeTab.SEARCH) }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.tab_search))
                    }
                }
                if (tab != HomeTab.SETTINGS) AccountButton { onTab(HomeTab.SETTINGS) }
            }
            if (rail) {
                Rail(haze, system, railTabs, tab, onTab, Modifier.align(Alignment.CenterStart))
            } else if (showBottom) {
                BottomNav(haze, system, tab, onTab, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    /**
     * As abas da barra de baixo lado a lado: deslizar para o lado troca de aba, e tocar numa aba
     * desliza até ela. Cada aba guarda a própria posição da lista (ver AppRoot).
     */
    @Composable
    private fun SwipeTabs(tab: HomeTab, onTab: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val pager = rememberPagerState(initialPage = swipeTabs.indexOf(tab).coerceAtLeast(0)) { swipeTabs.size }
        val currentOnTab by rememberUpdatedState(onTab)
        LaunchedEffect(tab) {
            val page = swipeTabs.indexOf(tab)
            if (page >= 0 && page != pager.currentPage) pager.animateScrollToPage(page)
        }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { currentOnTab(swipeTabs[it]) }
        }
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { swipeTabs[it].name }) { page ->
            content(swipeTabs[page])
        }
    }

    /** O avatar da conta, no canto da barra de cima: abre as Configurações (com o Sign out). */
    @Composable
    private fun AccountButton(onClick: () -> Unit) {
        val account = remember { AuthRepository.account() }
        val name = account?.name.orEmpty()
        IconButton(onClick = onClick) {
            Avatar(name.ifBlank { "?" }, account?.photo, 30.dp)
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
        Box(modifier.fillMaxWidth().height(system.top + TopBarHeight).hazeEffect(haze, TopGlass, block = LightGlass)) {
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
        Box(modifier.fillMaxWidth().height(system.bottom + BottomBarHeight).hazeEffect(haze, BottomGlass, block = LightGlass)) {
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
        Box(modifier.fillMaxHeight().width(RailWidth + system.start).hazeEffect(haze, StartGlass, block = LightGlass)) {
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
    internal fun DetailFrame(
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
        Loadable(state, onRetry = vm::reload, refresh = vm, placeholder = { VideoGridSkeleton() }) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.feed_empty), onNearEnd = vm::loadMore, onPlay = onPlay, onOpenChannel = onOpenChannel)
        }
    }

    @Composable
    override fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val filters by vm.filters.collectAsStateWithLifecycle()
        val filterRow: @Composable () -> Unit = { FilterRow(feedFilterChoices(filters), onChange = vm::setFilters, padding = PaddingValues(0.dp)) }
        Loadable(state, onRetry = vm::reload, refresh = vm, placeholder = { VideoGridSkeleton(header = filterRow) }) { content ->
            VideoGrid(
                content,
                emptyText = stringResource(if (filters.isDefault) R.string.recommended_empty else R.string.recommended_filtered_empty),
                onNearEnd = vm::loadMore,
                onPlay = onPlay,
                onOpenChannel = onOpenChannel,
                // Os filtros rolam junto com a lista, no topo dela.
                header = filterRow,
            )
        }
    }

    @Composable
    override fun Explore(vm: ExploreViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val showIntro by vm.showIntro.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload, refresh = vm, placeholder = { VideoGridSkeleton() }) { content ->
            VideoGrid(
                content,
                emptyText = stringResource(R.string.explore_empty),
                onNearEnd = vm::loadMore,
                onPlay = onPlay,
                onOpenChannel = onOpenChannel,
            )
        }
        // Só na primeira vez (enquanto os primeiros vídeos carregam): o que é a Explore.
        if (showIntro) {
            AlertDialog(
                onDismissRequest = vm::dismissIntro,
                title = { Text(stringResource(R.string.tab_explore)) },
                text = { Text(stringResource(R.string.explore_intro)) },
                confirmButton = { TextButton(onClick = vm::dismissIntro) { Text(stringResource(R.string.got_it)) } },
            )
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        val remove = { video: Video ->
            val undo = vm.remove(video) { Notices.show(res.getString(R.string.history_remove_failed, it)) }
            Notices.show(res.getString(R.string.history_removed), NoticeAction(res.getString(R.string.undo), undo))
        }
        // O mesmo visual das playlists (pedido do Johan): miniatura ao lado do título.
        Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
            VideoRows(
                content,
                onNearEnd = vm::loadMore,
                onPlay = onPlay,
                onOpenChannel = onOpenChannel,
                showSaved = true,
                header = null,
                onRemove = remove,
                emptyText = stringResource(R.string.history_empty),
                removeLabel = stringResource(R.string.history_remove),
            )
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
            FilterRow(searchFilterChoices(filters), onChange = vm::setFilters)
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
                        Loadable(state, onRetry = { vm.search() }, placeholder = { VideoGridSkeleton(top = 12.dp) }) { data ->
                            SearchResults(data, onNearEnd = vm::loadMore, onPlay, onOpenChannel, onOpenPlaylist)
                        }
                    }
                }
            }
        }
    }

    /** Os filtros (busca e Recomendações): um chip por filtro, cada um com o menu dos valores. */
    @Composable
    internal fun FilterRow(
        choices: List<SearchFilterChoice>,
        onChange: (SearchFilters) -> Unit,
        padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(padding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                key(choice.label) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = !choice.isDefault,
                            onClick = { open = true },
                            label = { Text(if (choice.isDefault) stringResource(choice.label) else choice.selected.text()) },
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
                                    text = { Text(option.text()) },
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
    internal fun SearchResults(
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
            columns = videoColumns(),
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
    internal fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Playlist) -> Unit, header: (@Composable () -> Unit)? = null) {
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

        // Em grupos (cartões arredondados com as linhas dentro), com a explicação embaixo de cada
        // um, em letra pequena. A conta fica em cima: as Configurações abrem pelo avatar.
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = barsPlus(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "account") {
                SettingsGroup(null) {
                    val account = remember { AuthRepository.account() }
                    val name = account?.name ?: vm.accountName
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name.orEmpty().ifBlank { "?" }, account?.photo, 44.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(name.orEmpty(), style = RowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.settings_account), style = Meta)
                        }
                        TextButton(onClick = { vm.signOut(onSignedOut) }) { Text(stringResource(R.string.sign_out)) }
                    }
                }
            }

            item(key = "subscriptions") {
                SettingsGroup(stringResource(R.string.settings_subscriptions)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_show_lives),
                        subtitle = stringResource(R.string.settings_show_lives_desc),
                        modifier = Modifier.clickable(role = Role.Switch) { vm.setShowLives(!settings.showLives) },
                    ) { SettingsSwitch(settings.showLives) }
                }
            }

            item(key = "save") {
                SettingsGroup(stringResource(R.string.settings_save_target)) {
                    val target = settings.saveTarget.title ?: stringResource(R.string.watch_later)
                    SettingsRow(
                        title = stringResource(R.string.settings_save_target_row),
                        modifier = Modifier.clickable {
                            choosingTarget = !choosingTarget
                            if (choosingTarget) vm.loadOwnPlaylists()
                        },
                    ) { Value(target, open = choosingTarget) }
                    if (choosingTarget) {
                        when (val own = ownPlaylists) {
                            null, UiState.Loading -> { RowDivider(); Progress() }
                            is UiState.Error -> { RowDivider(); ErrorRow(own.message, onRetry = vm::loadOwnPlaylists) }
                            is UiState.Ready -> own.data.forEach { playlist ->
                                val selected = playlist.id == settings.saveTarget.playlistId
                                RowDivider()
                                SettingsRow(
                                    title = playlist.title,
                                    modifier = Modifier.selectable(selected = selected, role = Role.RadioButton) {
                                        vm.setSaveTarget(playlist)
                                        choosingTarget = false
                                    },
                                    indent = true,
                                ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Look.Glow, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }

            item(key = "excluded") {
                TopicGroup(
                    title = R.string.settings_excluded,
                    hint = R.string.settings_excluded_hint,
                    topics = settings.filters.excluded,
                    onEdit = { editing = TopicList.EXCLUDED to it },
                    onRemove = { vm.removeTopic(TopicList.EXCLUDED, it) },
                )
            }
            item(key = "preferred") {
                TopicGroup(
                    title = R.string.settings_preferred,
                    hint = R.string.settings_preferred_hint,
                    topics = settings.filters.preferred,
                    onEdit = { editing = TopicList.PREFERRED to it },
                    onRemove = { vm.removeTopic(TopicList.PREFERRED, it) },
                )
            }

            item(key = "blocked") {
                SettingsGroup(stringResource(R.string.settings_blocked), hint = stringResource(R.string.settings_blocked_hint)) {
                    if (settings.filters.blocked.isEmpty()) EmptyRow(stringResource(R.string.settings_blocked_empty))
                    settings.filters.blocked.forEachIndexed { i, channel ->
                        if (i > 0) RowDivider()
                        SettingsRow(title = channel.name.ifBlank { channel.id }) {
                            TextButton(onClick = { vm.unblock(channel.id) }) { Text(stringResource(R.string.unblock)) }
                        }
                    }
                }
            }

            item(key = "links") {
                SettingsGroup(stringResource(R.string.settings_links)) {
                    externalLinkOptions.forEachIndexed { i, (mode, label) ->
                        if (i > 0) RowDivider()
                        val selected = settings.externalLinks == mode
                        SettingsRow(
                            title = stringResource(label),
                            modifier = Modifier.selectable(selected = selected, role = Role.RadioButton) { vm.setExternalLinks(mode) },
                        ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Look.Glow, modifier = Modifier.size(20.dp)) }
                    }
                }
            }

            item(key = "playlists") {
                SettingsGroup(stringResource(R.string.settings_playlists), hint = stringResource(R.string.settings_playlists_hint)) {
                    error?.let { message ->
                        Text(
                            stringResource(R.string.action_failed, message),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    when (val p = playlists) {
                        UiState.Loading -> Progress()
                        is UiState.Error -> ErrorRow(p.message, onRetry = vm::reloadPlaylists)
                        is UiState.Ready -> {
                            if (p.data.isEmpty()) EmptyRow(stringResource(R.string.settings_playlists_empty))
                            p.data.forEachIndexed { i, item ->
                                if (i > 0) RowDivider()
                                Row(
                                    Modifier.fillMaxWidth().clickable { onOpenPlaylist(item.playlist) }
                                        .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Thumbnail(item.playlist.thumbnailUrl, Modifier.width(80.dp), corner = 8.dp)
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(item.playlist.title, style = RowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        item.playlist.info?.let { Text(it, style = Meta, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    }
                                    if (item.busy) {
                                        CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(22.dp), strokeWidth = 2.dp)
                                    } else {
                                        TextButton(onClick = { vm.unsubscribe(item.playlist) }) { Text(stringResource(R.string.settings_unsubscribe)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "icon") {
                // As sete cores lado a lado; a escolhida ganha um anel âmbar e o nome embaixo.
                SettingsGroup(stringResource(R.string.settings_app_icon), hint = stringResource(R.string.settings_app_icon_hint)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppIcon.entries.forEach { icon ->
                            val selected = appIcon == icon
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .border(2.dp, if (selected) Look.Glow else Color.Transparent, CircleShape)
                                    .selectable(selected = selected, role = Role.RadioButton) { vm.setAppIcon(icon) },
                                contentAlignment = Alignment.Center,
                            ) { AppIconPreview(icon, 36.dp) }
                        }
                    }
                    Text(
                        stringResource(appIcon.label),
                        style = Meta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                    )
                }
            }

            item(key = "accent") {
                // As cores de destaque do mesmo jeito que as do ícone: bolinhas lado a lado, a
                // escolhida com um anel claro e o nome embaixo (pedido do Johan, 2026-11-03).
                val accent = Look.accent
                SettingsGroup(stringResource(R.string.settings_accent), hint = stringResource(R.string.settings_accent_hint)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Accent.entries.forEach { option ->
                            val selected = accent == option
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .border(2.dp, if (selected) Look.Text else Color.Transparent, CircleShape)
                                    .selectable(selected = selected, role = Role.RadioButton) { Accents.select(option) },
                                contentAlignment = Alignment.Center,
                            ) { Box(Modifier.size(30.dp).background(Color(option.glow), CircleShape)) }
                        }
                    }
                    Text(
                        stringResource(accent.label),
                        style = Meta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                    )
                }
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

    /** Um grupo das Configurações: o nome em cima, um cartão com as linhas e a explicação embaixo. */
    @Composable
    private fun SettingsGroup(title: String?, hint: String? = null, content: @Composable () -> Unit) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = 8.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.2.sp),
                    color = Look.Sub,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                )
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Look.Surface)) { content() }
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = Look.Sub.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                )
            }
        }
    }

    /** Uma linha dentro de um grupo: o título (e uma linha menor embaixo) e, à direita, o controle. */
    @Composable
    private fun SettingsRow(
        title: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        indent: Boolean = false,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = if (indent) 32.dp else 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = Meta, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            trailing?.invoke()
        }
    }

    /** O valor escolhido, à direita da linha, com a seta que abre a escolha. */
    @Composable
    private fun Value(text: String, open: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = Meta.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = Look.Sub,
                modifier = Modifier.graphicsLayer { rotationZ = if (open) 180f else 0f },
            )
        }
    }

    @Composable
    private fun SettingsSwitch(checked: Boolean) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Look.OnGlow,
                checkedTrackColor = Look.Glow,
                checkedBorderColor = Look.Glow,
                uncheckedThumbColor = Look.Sub,
                uncheckedTrackColor = Look.Ground,
                uncheckedBorderColor = Look.Outline,
            ),
        )
    }

    /** O fio entre as linhas de um grupo, a partir do começo do texto. */
    @Composable
    private fun RowDivider() {
        Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(Look.Hairline))
    }

    @Composable
    private fun EmptyRow(text: String) {
        Text(text, style = Meta.copy(fontSize = 14.sp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp))
    }

    @Composable
    private fun TopicGroup(title: Int, hint: Int, topics: List<Topic>, onEdit: (Int?) -> Unit, onRemove: (Int) -> Unit) {
        SettingsGroup(stringResource(title), hint = stringResource(hint)) {
            topics.forEachIndexed { index, topic ->
                if (index > 0) RowDivider()
                SettingsRow(
                    title = topic.name,
                    subtitle = topic.keywords.joinToString(", "),
                    modifier = Modifier.clickable { onEdit(index) },
                ) {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.topic_remove), tint = Look.Sub, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (topics.isNotEmpty()) RowDivider()
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(null) }.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Look.Glow, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.topic_add), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = Look.Glow)
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
        val editable by vm.editable.collectAsStateWithLifecycle()
        val saveTarget by SavedVideos.target.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }

        DetailFrame(title = shownTitle, navigation = { BackButton(onBack) }) {
            Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
                val shown = subscription as? PlaylistViewModel.Subscription.Shown
                VideoRows(
                    content = content,
                    onNearEnd = vm::loadMore,
                    onPlay = onPlay,
                    onOpenChannel = onOpenChannel,
                    // Na própria playlist do botão Save, o marcador estaria em todos os vídeos.
                    showSaved = vm.playlistId != saveTarget.playlistId,
                    header = shown?.let { { SubscribeHeader(it) { vm.toggleSubscription(shownTitle) } } },
                    onRemove = if (editable) {
                        { video ->
                            val undo = vm.remove(video) { Notices.show(res.getString(R.string.playlist_remove_failed, it)) }
                            Notices.show(res.getString(R.string.playlist_removed, shownTitle), NoticeAction(res.getString(R.string.undo), undo))
                        }
                    } else {
                        null
                    },
                    // Nas suas playlists, segurar o vídeo e arrastar muda a ordem na hora, e deslizar para a direita leva ao topo.
                    // A lista guardada no aparelho pode estar velha: os dois valem só com a da conta.
                    onMove = if (editable && !content.cached) vm::move else null,
                    onDrop = { vm.drop { Notices.show(res.getString(R.string.reorder_failed, it)) } },
                    onMoveToTop = { video ->
                        if (vm.moveToTop(video) { Notices.show(res.getString(R.string.reorder_failed, it)) }) {
                            Notices.show(res.getString(R.string.moved_to_top))
                        }
                    },
                )
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
        val res = LocalContext.current.resources
        DetailFrame(
            title = title.ifBlank { stringResource(R.string.channel_default_title) },
            navigation = { BackButton(onBack) },
            // Bloquear: só um ícone pequeno na barra de cima (âmbar quando bloqueado); o resultado vem num aviso.
            actions = {
                IconButton(onClick = {
                    vm.toggleBlocked()
                    Notices.show(res.getString(if (blocked) R.string.channel_unblocked_notice else R.string.channel_blocked_notice))
                }) {
                    Icon(
                        Icons.Outlined.Block,
                        contentDescription = stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block),
                        tint = if (blocked) Look.Glow else Look.Sub,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        ) {
            Loadable(state, onRetry = vm::reload, refresh = vm) { content ->
                val hasPlaylists = content.playlists.isNotEmpty()
                val header: (@Composable () -> Unit)? = if (!hasPlaylists) null else {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(R.string.channel_videos, R.string.channel_playlists).forEachIndexed { i, label ->
                                Segment(stringResource(label), selected = tab == i) { tab = i }
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
    internal fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
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
    internal fun SubscribeHeader(state: PlaylistViewModel.Subscription.Shown, onToggle: () -> Unit) {
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
    /**
     * Pede a próxima página quando faltam [ahead] itens para o fim. Cada página que chega muda
     * [count] e pede de novo, então a lista vai carregando à frente enquanto rola. [ready] muda
     * quando a lista da conta troca a guardada no aparelho.
     */
    @Composable
    private fun NearEnd(grid: LazyGridState, count: Int, onNearEnd: () -> Unit, ahead: Int = 6, ready: Boolean = true) {
        val nearEnd by remember(ahead) {
            derivedStateOf {
                val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= grid.layoutInfo.totalItemsCount - ahead
            }
        }
        LaunchedEffect(nearEnd, count, ready) { if (nearEnd) onNearEnd() }
    }

    /**
     * O que os cards precisam além do vídeo: quais estão no Watch later, as fotos dos canais e o
     * toque longo (salvar). [onLongPress] é null dentro da própria playlist do botão Save.
     */
    private class CardState(val saved: Set<String>, val photos: Map<String, String>, val showSaved: Boolean, val onLongPress: ((Video) -> Unit)?)

    /**
     * @param showSaved false na própria playlist do botão Save: lá o marcador estaria em todos os
     * vídeos, e segurar a miniatura não faz nada (pedido do Johan, 2026-11-03: confundia com o
     * deslizar, que é o jeito de tirar da playlist).
     */
    @Composable
    private fun rememberCardState(showSaved: Boolean = true): CardState {
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        val photos by ChannelPhotos.photos.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        return CardState(saved, photos, showSaved, if (showSaved) { video -> toggleSaved(res, video) } else null)
    }

    @Composable
    internal fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: ((Channel) -> Unit)?,
        header: (@Composable () -> Unit)? = null,
    ) {
        val grid = rememberLazyGridState()
        NearEnd(grid, content.videos.size, onNearEnd)
        PrefetchThumbnails(content.videos)
        val cards = rememberCardState()
        val motion = rememberListMotion(content.videos)
        LazyVerticalGrid(
            state = grid,
            columns = videoColumns(),
            contentPadding = barsPlus(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (content.videos.isEmpty() && !content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, textAlign = TextAlign.Center, color = Look.Sub, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(content.videos, key = { it.rowKey }) { video ->
                VideoCard(video, cards, onPlay, onOpenChannel, listMotion(motion))
            }
            listEnd(content, onNearEnd)
        }
    }

    /**
     * O fim da lista: o círculo enquanto a próxima página carrega; se parou com mais páginas (os
     * filtros esvaziaram várias seguidas), um botão para continuar.
     */
    private fun LazyGridScope.listEnd(content: VideoListContent, onMore: () -> Unit) {
        if (content.loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) { Progress() }
        } else if (content.hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth(), Alignment.Center) { TextButton(onClick = onMore) { Text(stringResource(R.string.load_more)) } }
            }
        }
    }

    /**
     * Lista de uma playlist (e do Histórico): miniatura pequena ao lado do título (uma coluna no
     * celular, duas no tablet deitado).
     */
    @Composable
    internal fun VideoRows(
        content: VideoListContent,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        showSaved: Boolean,
        header: (@Composable () -> Unit)?,
        onRemove: ((Video) -> Unit)? = null,
        emptyText: String = stringResource(R.string.list_empty),
        removeLabel: String = stringResource(R.string.playlist_remove),
        onMove: ((Int, Int) -> Unit)? = null,
        onDrop: () -> Unit = {},
        onMoveToTop: (Video) -> Unit = {},
    ) {
        val grid = rememberLazyGridState()
        // Playlists e Histórico (pedido do Johan, 2026-11-03): as páginas vêm bem antes do fim, para
        // rolar sem esperar.
        NearEnd(grid, content.videos.size, onNearEnd, ahead = ROWS_AHEAD, ready = !content.cached)
        PrefetchThumbnails(content.videos)
        val cards = rememberCardState(showSaved)
        val keys = remember(content.videos) { content.videos.map { it.rowKey } }
        val drag = if (onMove != null) rememberDragReorderState(grid, keys, onMove, onDrop) else null
        val moveCards = remember(cards) { CardState(cards.saved, cards.photos, cards.showSaved, onLongPress = null) }
        val haptics = LocalHapticFeedback.current
        // Enquanto um vídeo é arrastado, os outros deslizam para abrir espaço.
        val motion = rememberListMotion(content.videos) || drag?.draggingKey != null
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(minSize = 520.dp),
            contentPadding = barsPlus(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (content.videos.isEmpty() && !content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, textAlign = TextAlign.Center, color = Look.Sub, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(content.videos, key = { it.rowKey }) { video ->
                val key = video.rowKey
                val lifted = drag != null && drag.draggingKey == key
                SwipeToRemove(
                    enabled = onRemove != null,
                    label = removeLabel,
                    corner = 10.dp,
                    onRemove = { onRemove?.invoke(video) },
                    // Nas suas playlists, deslizar para a direita leva ao topo (o primeiro já está lá).
                    onSwipeRight = if (drag != null && key != keys.firstOrNull()) {
                        { onMoveToTop(video) }
                    } else {
                        null
                    },
                    rightLabel = stringResource(R.string.move_to_top),
                    modifier = if (lifted) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationX = drag.offset.x
                                translationY = drag.offset.y
                                scaleX = LIFT_SCALE
                                scaleY = LIFT_SCALE
                            }
                            .shadow(12.dp, RoundedCornerShape(10.dp))
                    } else {
                        listMotion(motion)
                    },
                ) {
                    VideoRow(
                        video,
                        // Segurar pega o vídeo para arrastar, em vez de salvar no Watch later.
                        if (drag != null) moveCards else cards,
                        onPlay,
                        onOpenChannel,
                        reorder = drag?.let { Modifier.dragReorderOnLongPress(it, key) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } }
                            ?: Modifier,
                    )
                }
            }
            listEnd(content, onNearEnd)
        }
    }

    /**
     * Card de vídeo. Tocar abre o vídeo; tocar e segurar põe ou tira do Watch later (com um aviso);
     * o avatar e o nome do canal abrem o canal.
     */
    @Composable
    private fun VideoCard(
        video: Video,
        cards: CardState,
        onPlay: (Video) -> Unit,
        onOpenChannel: ((Channel) -> Unit)?,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Look.Ground).combinedClickable(
                onClickLabel = video.title,
                onLongClickLabel = cards.onLongPress?.let { stringResource(R.string.video_toggle_save) },
                onLongClick = cards.onLongPress?.let { press -> { press(video) } },
                onClick = { onPlay(video) },
            ),
        ) {
            VideoThumb(video, saved = cards.showSaved && video.id in cards.saved, corner = 14.dp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(video.title, style = CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
            // Em várias colunas, o canal e o resto sempre em duas linhas (os cartões lado a lado ficam iguais).
            MetaRow(video, cards.photos[video.channelId], onOpenChannel, stacked = LocalConfiguration.current.screenWidthDp >= 560)
        }
    }

    @Composable
    private fun VideoRow(
        video: Video,
        cards: CardState,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        reorder: Modifier = Modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Look.Ground).combinedClickable(
                onClickLabel = video.title,
                onLongClickLabel = cards.onLongPress?.let { stringResource(R.string.video_toggle_save) },
                onLongClick = cards.onLongPress?.let { press -> { press(video) } },
                onClick = { onPlay(video) },
            ).then(reorder),
            verticalAlignment = Alignment.Top,
        ) {
            VideoThumb(video, saved = cards.showSaved && video.id in cards.saved, corner = 10.dp, modifier = Modifier.width(152.dp), compact = true)
            Column(Modifier.weight(1f).padding(start = 14.dp, top = 1.dp)) {
                Text(video.title, style = RowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                MetaRow(video, cards.photos[video.channelId], onOpenChannel, stacked = true)
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
    private fun MetaRow(video: Video, photo: String?, onOpenChannel: ((Channel) -> Unit)?, stacked: Boolean) {
        val rest = video.fromPlaylist?.let { stringResource(R.string.from_playlist_short, it) }
            ?: metaAfterChannel(video.info, video.channelName, video.published)
        val name = video.channelName?.takeIf { it.isNotBlank() }
        val channel = channelOf(video)
        // O canal e o resto numa linha só quando cabem; senão o resto desce, inteiro (MetaLine).
        MetaLine(
            channel = name?.let {
                {
                    val open = if (onOpenChannel != null && channel != null) {
                        val label = stringResource(R.string.video_open_channel, name)
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = label) { onOpenChannel(channel) }
                    } else {
                        Modifier
                    }
                    Row(open.padding(horizontal = 2.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name, photo, 18.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(name, style = Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            rest = rest?.let {
                { Text(rest, style = Meta, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp)) }
            },
            separator = { Text("·", style = Meta, modifier = Modifier.padding(start = 3.dp, end = 1.dp)) },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            stacked = stacked,
            // A linha de baixo encosta na de cima (os 7 dp em volta de cada uma são para o toque).
            lineGap = (-12).dp,
        )
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
    internal fun BackButton(onBack: () -> Unit) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }

    /**
     * O esqueleto de uma grade de vídeos enquanto a primeira página carrega, no lugar do círculo
     * (2026-11-03): os cartões aparecem no lugar onde os vídeos vão entrar. Parado, sem brilho
     * correndo, para não custar nada enquanto a rede trabalha.
     */
    @Composable
    internal fun VideoGridSkeleton(header: (@Composable () -> Unit)? = null, top: Dp = 20.dp) {
        LazyVerticalGrid(
            columns = videoColumns(),
            contentPadding = barsPlus(top = top),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            userScrollEnabled = false,
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            items(SKELETON_CARDS) { SkeletonCard() }
        }
    }

    @Composable
    private fun SkeletonCard() {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Look.Surface, RoundedCornerShape(14.dp)))
            Spacer(Modifier.height(14.dp))
            Bone(Modifier.fillMaxWidth(0.88f).height(13.dp))
            Spacer(Modifier.height(8.dp))
            Bone(Modifier.fillMaxWidth(0.5f).height(13.dp))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).background(Look.Surface, CircleShape))
                Spacer(Modifier.width(8.dp))
                Bone(Modifier.width(132.dp).height(10.dp))
            }
        }
    }

    @Composable
    private fun Bone(modifier: Modifier) {
        Box(modifier.background(Look.Surface, RoundedCornerShape(6.dp)))
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
    internal fun <T> Loadable(
        state: UiState<T>,
        onRetry: () -> Unit,
        refresh: Refreshable? = null,
        placeholder: (@Composable () -> Unit)? = null,
        content: @Composable (T) -> Unit,
    ) {
        when (state) {
            UiState.Loading -> placeholder?.invoke()
                ?: Box(Modifier.fillMaxSize().padding(LocalBars.current), Alignment.Center) { CircularProgressIndicator() }
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

/** Se os cartões podem deslizar nesta troca da lista ([ListMotion]): só quando vídeos saem ou chegam no fim. */
@Composable
private fun rememberListMotion(videos: List<Video>): Boolean {
    val keys = remember(videos) { videos.map { it.rowKey } }
    val shown = remember { arrayOf(keys) }
    val motion = ListMotion.animates(shown[0], keys)
    SideEffect { shown[0] = keys }
    return motion
}

/**
 * O cartão desliza para o lugar novo, ou só aparece nele. Sem animação, o modificador continua
 * no cartão (com as animações nulas): assim a grade segue sabendo onde cada um estava, e a próxima
 * saída de um vídeo anima direito.
 */
private fun LazyGridItemScope.listMotion(motion: Boolean): Modifier =
    if (motion) Modifier.animateItem() else Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)

@Composable
private fun videoColumns(): GridCells {
    val config = LocalConfiguration.current
    return VideoColumns(portrait = config.screenHeightDp > config.screenWidthDp)
}

/**
 * As colunas das grades de vídeo: uma no celular em pé; três no tablet em pé (pedido do Johan,
 * também no de 14,6", que em pé passa de 960 dp) e no celular deitado; quatro ou mais no tablet
 * deitado.
 */
private data class VideoColumns(val portrait: Boolean) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val width = availableSize.toDp()
        val count = when {
            width < 560.dp -> 1
            portrait || width < 960.dp -> 3
            else -> maxOf(4, (availableSize + spacing) / (240.dp.roundToPx() + spacing))
        }
        val cells = availableSize - spacing * (count - 1)
        return List(count) { cells / count + if (it < cells % count) 1 else 0 }
    }
}

/** A chave de um vídeo nas listas (um vídeo pode aparecer duas vezes numa playlist). */
private val Video.rowKey: String get() = id + playlistIndex

/** Nas listas em linhas, quantos vídeos à frente a próxima página já é pedida. */
private const val ROWS_AHEAD = 50

/** O vídeo pego para mudar a ordem cresce um pouco, para ficar claro que saiu do lugar. */
private const val LIFT_SCALE = 1.03f
