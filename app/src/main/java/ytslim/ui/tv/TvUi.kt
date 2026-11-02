package ytslim.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.AuthRepository
import ytslim.data.Channel
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
import ytslim.ui.Notices
import ytslim.ui.PlaylistViewModel
import ytslim.ui.PlaylistViewModel.Reorder
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.PrefetchThumbnails
import ytslim.ui.QrCode
import ytslim.ui.RecommendedViewModel
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
import ytslim.ui.searchFilterChoices
import ytslim.ui.subtitleOf
import ytslim.ui.text
import ytslim.ui.toggleSaved

/**
 * Na abertura do app o foco vai para o primeiro vídeo das Inscrições. Depois disso, as abas não
 * puxam o foco: andar pelas abas com o D-pad troca o conteúdo sem tirar o foco das abas.
 */
private val LocalInitialFocus = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * As cores do Doorglow na TV. O foco é a luz da porta: botões e linhas focados ficam creme-âmbar com
 * texto escuro, e os cards ganham um contorno e um brilho quentes.
 */
private val Colors = darkColorScheme(
    primary = Look.Glow,
    onPrimary = Look.OnGlow,
    primaryContainer = Look.SurfaceHigh,
    onPrimaryContainer = Look.Text,
    inversePrimary = Look.Glow,
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
    surfaceTint = Look.Ground,
    inverseSurface = Look.GlowSoft,
    inverseOnSurface = Look.OnGlow,
    error = Look.Error,
    onError = Look.OnGlow,
    border = Look.GlowSoft,
    borderVariant = Look.Outline,
    scrim = Color.Black,
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

/** Sem vidro na TV: as caixas de TV costumam ter pouca GPU, e o desfoque não compensa. */
private val Floating = Color(0xF528292D)

private val CardShape = RoundedCornerShape(14.dp)

/** Interface da Android TV: D-pad, foco visível e cards maiores (Compose for TV). */
object TvUi : Screens {
    private val OkKeys = setOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
    private val GridGapX = 20.dp
    private val GridGapY = 28.dp
    private val screenPadding = PaddingValues(horizontal = 48.dp, vertical = 26.dp)

    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = Colors, typography = Type) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) { content() }
        }
    }

    @Composable
    override fun NoticePill(notice: Notice?) {
        var shown by remember { mutableStateOf(notice) }
        if (notice != null) shown = notice
        Box(Modifier.fillMaxSize().padding(bottom = 28.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = notice != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Text(
                    shown?.text.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Look.Text,
                    modifier = Modifier.background(Floating, RoundedCornerShape(28.dp))
                        .border(1.dp, Look.Hairline, RoundedCornerShape(28.dp))
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                )
            }
        }
    }

    @Composable
    override fun SignIn(vm: SignInViewModel, onSignedIn: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        LaunchedEffect(state) { if (state == SignInViewModel.State.SignedIn) onSignedIn() }

        Column(
            Modifier.fillMaxSize().padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            when (val s = state) {
                SignInViewModel.State.Requesting, SignInViewModel.State.SignedIn ->
                    Text(stringResource(R.string.sign_in_requesting), style = MaterialTheme.typography.titleMedium)
                is SignInViewModel.State.ShowCode -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.sign_in_tv_open), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.sign_in_tv_url), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.sign_in_tv_enter), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(s.code, fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.sign_in_waits), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.width(64.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(240.dp)) {
                        QrCode(AuthRepository.activateUrl(s.code), Modifier.size(220.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.sign_in_tv_scan),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                is SignInViewModel.State.Error -> {
                    Text(stringResource(R.string.sign_in_error, s.message), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    val focus = remember { FocusRequester() }
                    Button(onClick = vm::requestCode, modifier = Modifier.focusRequester(focus)) { Text(stringResource(R.string.retry)) }
                    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                }
            }
        }
    }

    private val tabs = listOf(
        HomeTab.SUBSCRIPTIONS to R.string.tab_subscriptions,
        HomeTab.RECOMMENDED to R.string.tab_recommended,
        HomeTab.SEARCH to R.string.tab_search,
        HomeTab.PLAYLISTS to R.string.tab_playlists,
        HomeTab.HISTORY to R.string.tab_history,
        HomeTab.SETTINGS to R.string.tab_settings,
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val initialFocus = rememberSaveable { mutableStateOf(true) }
        var tabsFocused by remember { mutableStateOf(false) }
        // Segurar OK numa aba recarrega a lista dela, quando o OK é solto. O Tab segue recebendo as
        // teclas (o clique dele só escolhe a aba, que já está escolhida).
        var heldTab by remember { mutableStateOf<HomeTab?>(null) }
        val selectedTab = remember { FocusRequester() }
        // Voltar, de dentro do conteúdo, sobe para as abas; nas abas, sai do app.
        BackHandler(enabled = !tabsFocused) { runCatching { selectedTab.requestFocus() } }

        Column(Modifier.fillMaxSize().padding(screenPadding)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            DoorGlyph(Modifier.size(width = 15.dp, height = 22.dp))
            Spacer(Modifier.width(24.dp))
            val selectedIndex = tabs.indexOfFirst { it.first == tab }
            TabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus },
                separator = { Spacer(Modifier.width(4.dp)) },
                indicator = { positions, _ -> positions.getOrNull(selectedIndex)?.let { SlitIndicator(it) } },
            ) {
                tabs.forEach { (item, label) ->
                    Tab(
                        selected = item == tab,
                        onFocus = { onTab(item) },
                        onClick = { onTab(item) },
                        colors = TabDefaults.underlinedIndicatorTabColors(
                            contentColor = Look.Sub,
                            inactiveContentColor = Look.Sub,
                            selectedContentColor = Look.Text,
                            focusedContentColor = Look.GlowSoft,
                            focusedSelectedContentColor = Look.GlowSoft,
                        ),
                        modifier = (if (item == tab) Modifier.focusRequester(selectedTab) else Modifier)
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.keyCode in OkKeys) {
                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 0) {
                                        heldTab = item
                                    } else if (event.type == KeyEventType.KeyUp) {
                                        if (heldTab == item) onReload(item)
                                        heldTab = null
                                    }
                                }
                                false
                            },
                    ) {
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
          }
            Spacer(Modifier.height(24.dp))
            CompositionLocalProvider(LocalInitialFocus provides initialFocus) {
                Box(Modifier.fillMaxSize()) { content(tab) }
            }
        }
    }

    /** A fresta de luz embaixo da aba escolhida, que anda junto quando a aba muda. */
    @Composable
    private fun SlitIndicator(position: androidx.compose.ui.unit.DpRect) {
        val width = 14.dp
        val left by animateDpAsState(position.left + (position.right - position.left - width) / 2, label = "slit")
        LightSlit(
            Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = left).size(width, 3.dp),
        )
    }

    @Composable
    override fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val initialFocus = LocalInitialFocus.current
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(
                content = content,
                emptyText = stringResource(R.string.feed_empty),
                onNearEnd = vm::loadMore,
                onPlay = onPlay,
                onOpenChannel = onOpenChannel,
                takeFocus = initialFocus.value,
                onFocusTaken = { initialFocus.value = false },
            )
        }
    }

    @Composable
    override fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val filters by vm.filters.collectAsStateWithLifecycle()
        Column(Modifier.fillMaxSize()) {
            FilterButtons(feedFilterChoices(filters), onChange = vm::setFilters)
            Spacer(Modifier.height(16.dp))
            Loadable(state, onRetry = vm::reload) { content ->
                val empty = stringResource(if (filters.isDefault) R.string.recommended_empty else R.string.recommended_filtered_empty)
                VideoGrid(content, empty, vm::loadMore, onPlay, onOpenChannel, takeFocus = false, onFocusTaken = {})
            }
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, stringResource(R.string.history_empty), vm::loadMore, onPlay, onOpenChannel, takeFocus = false, onFocusTaken = {})
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
        var text by rememberSaveable { mutableStateOf(vm.query.value) }
        val searched = (results as? UiState.Ready)?.data?.query
        val submit = { query: String ->
            text = query
            vm.search(query)
        }

        Column(Modifier.fillMaxSize()) {
            // OK no campo abre o teclado; a tecla de busca do teclado faz a busca.
            TvTextField(
                value = text,
                onValueChange = {
                    text = it
                    vm.onQueryChange(it)
                },
                placeholder = stringResource(R.string.search_hint),
                imeAction = ImeAction.Search,
                onAction = { submit(text) },
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            Spacer(Modifier.height(12.dp))
            FilterButtons(searchFilterChoices(filters), onChange = vm::setFilters)
            if (suggestions.isNotEmpty() && text.trim() != searched) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(suggestions) { suggestion ->
                        OutlinedButton(onClick = { submit(suggestion) }) { Text(suggestion, maxLines = 1) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            results?.let { state ->
                Loadable(state, onRetry = { vm.search() }) { data ->
                    SearchResults(data, vm::loadMore, onPlay, onOpenChannel, onOpenPlaylist)
                }
            }
        }
    }

    /** Os filtros (busca e Recomendações): cada botão abre a lista dos valores, com o atual em foco. */
    @Composable
    private fun FilterButtons(choices: List<SearchFilterChoice>, onChange: (SearchFilters) -> Unit) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { choice ->
                key(choice.label) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        val label = stringResource(R.string.search_filter_value, stringResource(choice.label), choice.selected.text())
                        if (choice.isDefault) {
                            OutlinedButton(onClick = { open = true }) { Text(label) }
                        } else {
                            Button(onClick = { open = true }) { Text(label) }
                        }
                        if (open) {
                            FilterMenu(choice, onDismiss = { open = false }) { picked ->
                                open = false
                                onChange(picked)
                            }
                        }
                    }
                }
            }
        }
    }

    /** A lista de valores de um filtro, embaixo do botão. "Voltar" fecha sem mudar. */
    @Composable
    private fun FilterMenu(choice: SearchFilterChoice, onDismiss: () -> Unit, onPick: (SearchFilters) -> Unit) {
        val gap = with(LocalDensity.current) { 10.dp.roundToPx() }
        val selected = choice.options.indexOfFirst { it.selected }.coerceAtLeast(0)
        val focus = remember { FocusRequester() }
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 2).coerceAtLeast(0))
        Popup(
            popupPositionProvider = remember(gap) { BelowAnchor(gap) },
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            LazyColumn(
                state = list,
                modifier = Modifier.width(300.dp).heightIn(max = 420.dp)
                    .background(Floating, RoundedCornerShape(22.dp))
                    .border(1.dp, Look.Hairline, RoundedCornerShape(22.dp)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(choice.options) { index, option ->
                    MenuItem(
                        text = option.text(),
                        supporting = null,
                        leading = {
                            if (option.selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Look.Glow, modifier = Modifier.size(20.dp))
                            else Spacer(Modifier.size(20.dp))
                        },
                        modifier = if (index == selected) Modifier.focusRequester(focus) else Modifier,
                    ) { onPick(option.filters) }
                }
            }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        }
    }

    /** Põe a lista embaixo do botão (ou em cima, se não couber). */
    private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val x = anchorBounds.left.coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
            val below = anchorBounds.bottom + gap
            val y = if (below + popupContentSize.height <= windowSize.height) below
            else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
            return IntOffset(x, y)
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
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
        ) {
            if (data.items.isEmpty() && !data.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.search_empty), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(24.dp))
                }
            }
            itemsIndexed(data.items, key = { _, item -> item.key }) { index, item ->
                if (index >= data.items.size - 8) LaunchedEffect(Unit) { onNearEnd() }
                when (item) {
                    is SearchItem.VideoItem -> VideoCard(item.video, saved = item.video.id in saved, onOpenChannel = onOpenChannel) { onPlay(item.video) }
                    is SearchItem.ChannelItem -> ChannelCard(item.channel) { onOpenChannel(item.channel) }
                    is SearchItem.PlaylistItem -> MediaCard(
                        title = item.playlist.title,
                        subtitle = item.playlist.info,
                        imageUrl = item.playlist.thumbnailUrl,
                        badge = null,
                        onClick = { onOpenPlaylist(item.playlist) },
                    )
                }
            }
        }
    }

    @Composable
    override fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { playlists -> PlaylistGrid(playlists, onOpen) }
    }

    @Composable
    private fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Playlist) -> Unit) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
        ) {
            itemsIndexed(playlists, key = { _, p -> p.browseId }) { _, playlist ->
                MediaCard(
                    title = playlist.title,
                    subtitle = playlist.info,
                    imageUrl = playlist.thumbnailUrl,
                    badge = null,
                    onClick = { onOpen(playlist) },
                )
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
        var editing by remember { mutableStateOf<Pair<TopicList, Int?>?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(0.7f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item { SectionTitle(stringResource(R.string.settings_subscriptions)) }
            item {
                ListItem(
                    selected = false,
                    onClick = { vm.setShowLives(!settings.showLives) },
                    headlineContent = { Text(stringResource(R.string.settings_show_lives)) },
                    supportingContent = { Text(stringResource(R.string.settings_show_lives_desc)) },
                    trailingContent = { Switch(checked = settings.showLives, onCheckedChange = null) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_save_target)) }
            item {
                val target = settings.saveTarget.title ?: stringResource(R.string.watch_later)
                ListItem(
                    selected = false,
                    onClick = {
                        choosingTarget = !choosingTarget
                        if (choosingTarget) vm.loadOwnPlaylists()
                    },
                    headlineContent = { Text(stringResource(R.string.settings_save_target_current, target)) },
                    supportingContent = { Text(stringResource(R.string.settings_save_target_choose)) },
                )
            }
            if (choosingTarget) {
                when (val own = ownPlaylists) {
                    null, UiState.Loading -> item { Text(stringResource(R.string.loading), Modifier.padding(16.dp)) }
                    is UiState.Error -> item {
                        ListItem(
                            selected = false,
                            onClick = vm::loadOwnPlaylists,
                            headlineContent = { Text(stringResource(R.string.error_generic, own.message)) },
                            trailingContent = { Text(stringResource(R.string.retry)) },
                        )
                    }
                    is UiState.Ready -> items(own.data, key = { "target:" + it.id }) { playlist ->
                        ListItem(
                            selected = false,
                            onClick = {
                                vm.setSaveTarget(playlist)
                                choosingTarget = false
                            },
                            headlineContent = { Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { RadioButton(selected = playlist.id == settings.saveTarget.playlistId, onClick = null) },
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
                item { Text(stringResource(R.string.settings_blocked_empty), Modifier.padding(16.dp)) }
            }
            items(settings.filters.blocked, key = { "blocked:" + it.id }) { channel ->
                ListItem(
                    selected = false,
                    onClick = { vm.unblock(channel.id) },
                    headlineContent = { Text(channel.name.ifBlank { channel.id }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = { Text(stringResource(R.string.unblock)) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_links)) }
            items(externalLinkOptions) { (mode, label) ->
                ListItem(
                    selected = false,
                    onClick = { vm.setExternalLinks(mode) },
                    headlineContent = { Text(stringResource(label)) },
                    leadingContent = { RadioButton(selected = settings.externalLinks == mode, onClick = null) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_playlists)) }
            item { Hint(stringResource(R.string.settings_playlists_hint)) }
            error?.let { message ->
                item {
                    Text(
                        stringResource(R.string.action_failed, message),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            when (val p = playlists) {
                UiState.Loading -> item { Text(stringResource(R.string.loading), Modifier.padding(16.dp)) }
                is UiState.Error -> item {
                    ListItem(
                        selected = false,
                        onClick = vm::reloadPlaylists,
                        headlineContent = { Text(stringResource(R.string.error_generic, p.message)) },
                        trailingContent = { Text(stringResource(R.string.retry)) },
                    )
                }
                is UiState.Ready -> {
                    if (p.data.isEmpty()) {
                        item { Text(stringResource(R.string.settings_playlists_empty), Modifier.padding(16.dp)) }
                    }
                    items(p.data, key = { it.playlist.browseId }) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ListItem(
                                selected = false,
                                onClick = { onOpenPlaylist(item.playlist) },
                                headlineContent = { Text(item.playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = item.playlist.info?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedButton(onClick = { vm.unsubscribe(item.playlist) }, enabled = !item.busy) {
                                Text(stringResource(R.string.settings_unsubscribe))
                            }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_app_icon)) }
            item { Hint(stringResource(R.string.settings_app_icon_hint)) }
            items(AppIcon.entries) { icon ->
                ListItem(
                    selected = false,
                    onClick = { vm.setAppIcon(icon) },
                    headlineContent = { Text(stringResource(icon.label)) },
                    leadingContent = { AppIconPreview(icon) },
                    trailingContent = { RadioButton(selected = appIcon == icon, onClick = null) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_account)) }
            item {
                ListItem(
                    selected = false,
                    onClick = { vm.signOut(onSignedOut) },
                    headlineContent = { Text(stringResource(R.string.sign_out)) },
                    supportingContent = vm.accountName?.let { { Text(stringResource(R.string.settings_signed_in_as, it)) } },
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
        if (topics.isEmpty()) item { Text(stringResource(R.string.settings_topics_empty), Modifier.padding(16.dp)) }
        itemsIndexed(topics) { index, topic ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListItem(
                    selected = false,
                    onClick = { onEdit(index) },
                    headlineContent = { Text(topic.name) },
                    supportingContent = { Text(topic.keywords.joinToString(", "), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { onRemove(index) }) { Text(stringResource(R.string.topic_remove)) }
            }
        }
        item {
            ListItem(
                selected = false,
                onClick = { onEdit(null) },
                headlineContent = { Text(stringResource(R.string.topic_add)) },
            )
        }
    }

    @Composable
    private fun TopicDialog(initial: Topic?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
        var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
        var keywords by rememberSaveable { mutableStateOf(initial?.keywords?.joinToString(", ").orEmpty()) }
        val valid = name.isNotBlank() && TopicRules.parseKeywords(keywords).isNotEmpty()
        val first = remember { FocusRequester() }
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(640.dp)) {
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(if (initial == null) R.string.topic_add else R.string.topic_edit),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(stringResource(R.string.topic_name), style = MaterialTheme.typography.labelLarge)
                    TvTextField(name, { name = it }, placeholder = "", modifier = Modifier.fillMaxWidth().focusRequester(first))
                    Text(stringResource(R.string.topic_keywords), style = MaterialTheme.typography.labelLarge)
                    TvTextField(keywords, { keywords = it }, placeholder = "", modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onSave(name, keywords) }, enabled = valid) { Text(stringResource(R.string.save)) }
                        OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
            // Dentro do diálogo: a janela dele já existe quando o foco é pedido.
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        }
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
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }
        val editing = reorder as? Reorder.Editing
        // Posição do vídeo "pego" no modo mover (null = nenhum).
        var held by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(editing == null) { if (editing == null) held = null }
        BackHandler(enabled = editing != null) { if (held != null) held = null else vm.cancelReorder() }

        Column(Modifier.fillMaxSize().padding(screenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shownTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                (subscription as? PlaylistViewModel.Subscription.Shown)?.let { shown ->
                    Spacer(Modifier.width(24.dp))
                    val label = stringResource(if (shown.subscribed) R.string.playlist_subscribed else R.string.playlist_subscribe)
                    val toggle = { vm.toggleSubscription(shownTitle) }
                    // Inscrito: ✓ e "Subscribed"; apertar de novo cancela a inscrição.
                    if (shown.subscribed) {
                        OutlinedButton(onClick = toggle, enabled = !shown.busy) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    } else {
                        Button(onClick = toggle, enabled = !shown.busy) { Text(label) }
                    }
                }
                when (val r = reorder) {
                    Reorder.Unavailable -> Unit
                    is Reorder.Available -> {
                        Spacer(Modifier.width(24.dp))
                        OutlinedButton(onClick = vm::startReorder, enabled = !r.preparing && state is UiState.Ready) {
                            Text(stringResource(if (r.preparing) R.string.reorder_preparing else R.string.reorder))
                        }
                    }
                    is Reorder.Editing -> {
                        Spacer(Modifier.width(24.dp))
                        Button(onClick = vm::saveOrder, enabled = !r.saving) {
                            Text(stringResource(if (r.saving) R.string.reorder_saving else R.string.reorder_save))
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = vm::cancelReorder, enabled = !r.saving) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
            val note: Pair<String, Boolean>? = when {
                editing?.error != null -> stringResource(R.string.reorder_failed, editing.error) to true
                editing != null -> stringResource(R.string.reorder_hint_tv) to false
                (reorder as? Reorder.Available)?.loadFailed == true -> stringResource(R.string.reorder_load_failed) to true
                else -> (subscription as? PlaylistViewModel.Subscription.Shown)?.let { shown ->
                    shown.error?.let { stringResource(R.string.action_failed, it) to true }
                        ?: (stringResource(if (shown.subscribed) R.string.playlist_subscribed_hint_tv else R.string.playlist_subscribe_hint) to false)
                }
            }
            note?.let { (text, isError) ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            if (editing != null) {
                ReorderList(editing, held = held, onHold = { held = it }, onMove = vm::move)
            } else {
                val saveTarget by SavedVideos.target.collectAsStateWithLifecycle()
                Loadable(state, onRetry = vm::reload) { content ->
                    VideoGrid(
                        content = content,
                        emptyText = stringResource(R.string.list_empty),
                        onNearEnd = vm::loadMore,
                        onPlay = onPlay,
                        onOpenChannel = onOpenChannel,
                        takeFocus = true,
                        onFocusTaken = {},
                        // Na própria playlist do botão Save, o marcador estaria em todos os vídeos.
                        showSaved = vm.playlistId != saveTarget.playlistId,
                    )
                }
            }
        }
    }

    /**
     * O modo mover: OK pega o vídeo, cima e baixo levam ele pela lista, OK de novo solta.
     * A ordem só vai para a conta no "Save order".
     */
    @Composable
    private fun ReorderList(editing: Reorder.Editing, held: Int?, onHold: (Int?) -> Unit, onMove: (Int, Int) -> Unit) {
        val list = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val first = remember { FocusRequester() }
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            itemsIndexed(editing.videos, key = { _, video -> video.id }) { index, video ->
                val isHeld = held == index
                ListItem(
                    selected = isHeld,
                    onClick = { if (!editing.saving) onHold(if (isHeld) null else index) },
                    headlineContent = { Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = video.channelName?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                    leadingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                            Spacer(Modifier.width(12.dp))
                            AsyncImage(
                                model = video.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(112.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    },
                    trailingContent = if (isHeld) {
                        { Text("↕", style = MaterialTheme.typography.titleLarge) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                        .onPreviewKeyEvent { event ->
                            if (!isHeld || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val to = when (event.key) {
                                Key.DirectionUp -> index - 1
                                Key.DirectionDown -> index + 1
                                else -> return@onPreviewKeyEvent false
                            }
                            if (to in editing.videos.indices) {
                                onMove(index, to)
                                onHold(to)
                                // O vídeo pego anda com o foco; a lista rola quando ele chega na borda.
                                val visible = list.layoutInfo.visibleItemsInfo
                                val top = visible.firstOrNull()?.index ?: 0
                                val bottom = visible.lastOrNull()?.index ?: 0
                                if (to <= top || to >= bottom) {
                                    scope.launch { list.animateScrollToItem((to - visible.size / 2).coerceAtLeast(0)) }
                                }
                            }
                            true
                        },
                )
            }
        }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
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

        Column(Modifier.fillMaxSize().padding(screenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.ifBlank { stringResource(R.string.channel_default_title) },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(16.dp))
                BlockButton(blocked, onToggle = vm::toggleBlocked)
            }
            Spacer(Modifier.height(16.dp))
            Loadable(state, onRetry = vm::reload) { content ->
                Column(Modifier.fillMaxSize()) {
                    if (content.playlists.isNotEmpty()) {
                        TabRow(selectedTabIndex = tab) {
                            listOf(R.string.channel_videos, R.string.channel_playlists).forEachIndexed { i, label ->
                                Tab(selected = tab == i, onFocus = { tab = i }, onClick = { tab = i }) {
                                    Text(
                                        stringResource(label),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    if (tab == 1 && content.playlists.isNotEmpty()) {
                        PlaylistGrid(content.playlists, onOpenPlaylist)
                    } else {
                        VideoGrid(
                            content = content.videos,
                            emptyText = stringResource(R.string.list_empty),
                            onNearEnd = vm::loadMore,
                            onPlay = onPlay,
                            onOpenChannel = null,
                            takeFocus = true,
                            onFocusTaken = {},
                        )
                    }
                }
            }
        }
    }

    /**
     * Bloquear o canal: um ícone pequeno ao lado do nome (âmbar quando bloqueado). O texto só
     * aparece com o foco nele; o resultado vem num aviso.
     */
    @Composable
    private fun BlockButton(blocked: Boolean, onToggle: () -> Unit) {
        val res = LocalContext.current.resources
        var focused by remember { mutableStateOf(false) }
        val label = stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onToggle()
                    Notices.show(res.getString(if (blocked) R.string.channel_unblocked_notice else R.string.channel_blocked_notice))
                },
                modifier = Modifier.size(36.dp).onFocusChanged { focused = it.isFocused },
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = if (blocked) Look.Glow else Look.Sub,
                    focusedContainerColor = Look.GlowSoft.copy(alpha = 0.18f),
                    focusedContentColor = Look.Text,
                ),
            ) {
                Icon(Icons.Outlined.Block, contentDescription = label, modifier = Modifier.size(18.dp))
            }
            if (focused) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = Look.Sub, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    @Composable
    private fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: ((Channel) -> Unit)?,
        takeFocus: Boolean,
        onFocusTaken: () -> Unit,
        showSaved: Boolean = true,
    ) {
        val firstFocus = remember { FocusRequester() }
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        PrefetchThumbnails(content.videos)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
        ) {
            if (content.videos.isEmpty() && !content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(24.dp))
                }
            }
            itemsIndexed(content.videos, key = { _, v -> v.id + v.playlistIndex }) { index, video ->
                if (index >= content.videos.size - 8) LaunchedEffect(Unit) { onNearEnd() }
                VideoCard(
                    video,
                    saved = showSaved && video.id in saved,
                    onOpenChannel = onOpenChannel,
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                ) { onPlay(video) }
            }
            // Parou com mais páginas (os filtros esvaziaram várias seguidas): um botão para continuar.
            if (content.hasMore && !content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        OutlinedButton(onClick = onNearEnd) { Text(stringResource(R.string.load_more)) }
                    }
                }
            }
        }
        LaunchedEffect(content.videos.isNotEmpty()) {
            if (takeFocus && content.videos.isNotEmpty()) {
                runCatching { firstFocus.requestFocus() }
                onFocusTaken()
            }
        }
    }

    /**
     * Card de vídeo. OK abre o vídeo; segurar o OK abre um menu pequeno ao lado do card, com
     * "Save to Watch later" (ou "Remove from…", se já estiver lá) e "Go to channel".
     */
    @Composable
    private fun VideoCard(
        video: Video,
        saved: Boolean,
        onOpenChannel: ((Channel) -> Unit)?,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        var menu by remember { mutableStateOf(false) }
        // O menu abre quando o OK é solto: se abrisse ainda com o botão apertado, o "soltar" iria
        // para o menu (e escolheria a primeira opção) e o card ficaria esperando um soltar que não vem.
        var held by remember { mutableStateOf(false) }
        val res = LocalContext.current.resources
        val channel = channelOf(video)?.takeIf { onOpenChannel != null }
        Column(modifier) {
            Box(
                Modifier.onPreviewKeyEvent { event ->
                    if (held && event.type == KeyEventType.KeyUp && event.nativeKeyEvent.keyCode in OkKeys) {
                        held = false
                        menu = true
                    }
                    false
                },
            ) {
                GlowCard(onClick = onClick, onLongClick = { held = true }) {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(Look.Surface),
                        )
                        val badge = if (video.isLive) stringResource(R.string.search_live) else video.durationMs.takeIf { it > 0 }?.let(::formatDuration)
                        if (badge != null) Badge(badge, if (video.isLive) Look.Live else Look.Badge, Modifier.align(Alignment.BottomEnd))
                        if (video.percentWatched > 0) {
                            Box(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth(video.percentWatched.coerceIn(0, 100) / 100f)
                                    .height(3.dp).background(Look.Glow),
                            )
                        }
                        if (saved) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(7.dp).size(20.dp).background(Look.BadgeSoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_bookmark),
                                    contentDescription = stringResource(R.string.video_saved),
                                    tint = Look.Glow,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                }
                if (menu) {
                    VideoMenu(
                        saved = saved,
                        channelName = channel?.title,
                        onDismiss = { menu = false },
                        onSave = { toggleSaved(res, video) },
                        onChannel = channel?.let { c -> { onOpenChannel?.invoke(c) } },
                    )
                }
            }
            CardText(video.title, subtitleOf(video))
        }
    }

    /** O menu do OK longo: abre ao lado do card (à direita, ou à esquerda na última coluna). */
    @Composable
    private fun VideoMenu(saved: Boolean, channelName: String?, onDismiss: () -> Unit, onSave: () -> Unit, onChannel: (() -> Unit)?) {
        val gap = with(LocalDensity.current) { 18.dp.roundToPx() }
        val target by SavedVideos.target.collectAsStateWithLifecycle()
        val playlist = target.title ?: stringResource(R.string.watch_later)
        val first = remember { FocusRequester() }
        Popup(
            popupPositionProvider = remember(gap) { BesideAnchor(gap) },
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                Modifier.width(320.dp)
                    .background(Floating, RoundedCornerShape(22.dp))
                    .border(1.dp, Look.Hairline, RoundedCornerShape(22.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MenuItem(
                    text = stringResource(if (saved) R.string.video_menu_remove else R.string.video_menu_save, playlist),
                    supporting = null,
                    leading = {
                        Icon(
                            painterResource(if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
                            contentDescription = null,
                            tint = Look.Glow,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    modifier = Modifier.focusRequester(first),
                ) {
                    onDismiss()
                    onSave()
                }
                if (onChannel != null) {
                    MenuItem(
                        text = stringResource(R.string.video_menu_channel),
                        supporting = channelName,
                        leading = {
                            Box(
                                Modifier.size(24.dp).background(Look.monogramTone(channelName.orEmpty()), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(ytslim.ui.monogramOf(channelName), style = MaterialTheme.typography.labelMedium, color = Look.Text)
                            }
                        },
                    ) {
                        onDismiss()
                        onChannel()
                    }
                }
            }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        }
    }

    @Composable
    private fun MenuItem(
        text: String,
        supporting: String?,
        leading: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        val shape = RoundedCornerShape(14.dp)
        ListItem(
            selected = false,
            onClick = onClick,
            headlineContent = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = supporting?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Look.Sub) } },
            leadingContent = { leading() },
            shape = ListItemDefaults.shape(shape),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Look.Text,
                focusedContainerColor = Look.GlowSoft.copy(alpha = 0.14f),
                focusedContentColor = Look.Text,
                pressedContainerColor = Look.GlowSoft.copy(alpha = 0.22f),
                pressedContentColor = Look.Text,
            ),
            border = ListItemDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Look.GlowSoft), shape = shape)),
            scale = ListItemDefaults.scale(focusedScale = 1f),
            modifier = modifier,
        )
    }

    /** Põe o menu ao lado do card: à direita se couber, senão à esquerda. */
    private class BesideAnchor(private val gap: Int) : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val right = anchorBounds.right + gap
            val x = if (right + popupContentSize.width <= windowSize.width) right
            else (anchorBounds.left - gap - popupContentSize.width).coerceAtLeast(0)
            val y = (anchorBounds.top + gap).coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0)
            return IntOffset(x, y)
        }
    }

    /** Card com o foco da TV: cresce 6%, ganha um contorno creme e um brilho âmbar. */
    @Composable
    private fun GlowCard(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = CardDefaults.shape(CardShape),
            colors = CardDefaults.colors(containerColor = Look.Surface),
            scale = CardDefaults.scale(focusedScale = 1.06f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Look.GlowSoft), shape = CardShape)),
            glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = Look.Glow.copy(alpha = 0.45f), elevation = 18.dp)),
        ) { content() }
    }

    @Composable
    private fun Badge(text: String, color: Color, modifier: Modifier) {
        Text(
            text,
            color = Look.Text,
            style = MaterialTheme.typography.labelMedium,
            modifier = modifier.padding(8.dp)
                .background(color, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }

    @Composable
    private fun CardText(title: String, subtitle: String?) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Look.Sub,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
            )
        }
    }

    @Composable
    private fun MediaCard(
        title: String,
        subtitle: String?,
        imageUrl: String?,
        badge: String?,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier) {
            GlowCard(onClick = onClick) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(Look.Surface),
                    )
                    if (badge != null) Badge(badge, Look.Badge, Modifier.align(Alignment.BottomEnd))
                }
            }
            CardText(title, subtitle)
        }
    }

    /** Um canal nos resultados: a foto redonda e o nome. */
    @Composable
    private fun ChannelCard(channel: Channel, onClick: () -> Unit) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                onClick = onClick,
                shape = CardDefaults.shape(CircleShape),
                modifier = Modifier.size(128.dp),
            ) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Text(
                channel.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            channel.info?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * Campo de texto para o D-pad. O teclado só abre com OK (não ao passar o foco), e as setas
     * saem do campo; isso vem do próprio Compose para o controle remoto.
     */
    @Composable
    private fun TvTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        imeAction: ImeAction = ImeAction.Done,
        onAction: () -> Unit = {},
    ) {
        var focused by remember { mutableStateOf(false) }
        val colors = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(8.dp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = imeAction, showKeyboardOnFocus = false),
            keyboardActions = KeyboardActions(onAny = { onAction() }),
            modifier = modifier.onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth()
                        .background(if (focused) colors.surfaceVariant else colors.surface, shape)
                        .border(2.dp, if (focused) colors.border else colors.surfaceVariant, shape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                    inner()
                }
            },
        )
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = Look.Sub,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
        )
    }

    @Composable
    private fun Hint(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    @Composable
    private fun <T> Loadable(state: UiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
        when (state) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.loading), style = MaterialTheme.typography.titleMedium)
            }
            is UiState.Error -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.error_generic, state.message), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Ready -> content(state.data)
        }
    }
}
