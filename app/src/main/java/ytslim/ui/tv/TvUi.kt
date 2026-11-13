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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
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
import ytslim.data.PlaybackRepository
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
import ytslim.ui.FeedViewModel
import ytslim.ui.HistoryViewModel
import ytslim.ui.HomeTab
import ytslim.ui.LightSlit
import ytslim.ui.ListTop
import ytslim.ui.MetaLine
import ytslim.ui.metaAfterChannel
import ytslim.ui.Look
import ytslim.ui.TypeScale
import ytslim.ui.Wordmark
import ytslim.ui.Notice
import ytslim.ui.Notices
import ytslim.ui.PlaylistViewModel
import ytslim.ui.Subscription
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
import ytslim.ui.openChannelOf
import ytslim.ui.exploreFilterChoices
import ytslim.ui.externalLinkOptions
import ytslim.ui.formatDuration
import ytslim.ui.searchFilterChoices
import ytslim.ui.spacedDots
import ytslim.ui.subtitleOf
import ytslim.ui.text
import ytslim.ui.toggleSaved

/**
 * Na abertura do app o foco vai para o primeiro vídeo das Inscrições. Depois disso, as abas não
 * puxam o foco: andar pelas abas com o D-pad troca o conteúdo sem tirar o foco das abas.
 */
private val LocalInitialFocus = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * Subir do conteúdo até uma fileira de abas para na aba aberta ([selected]), e não na aba que
 * estiver mais perto do card focado: como a aba muda com o foco, a de perto trocava a página
 * (achado do Johan na TV, 2026-11-04: das Inscrições ia para os Ajustes).
 */
private fun Modifier.entersAt(selected: FocusRequester) =
    focusProperties { onEnter = { selected.requestFocus() } }.focusGroup()

/**
 * As cores do BeamTube na TV. O foco é a luz da fresta: botões e linhas focados ficam na cor de
 * destaque com texto escuro, e os cards ganham um contorno claro. Refeitas quando a cor de
 * destaque muda ([Look.accent]).
 */
private fun colors() = darkColorScheme(
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

private val Type: Typography = Typography(
    displayLarge = TypeScale.displayLarge,
    displayMedium = TypeScale.displayMedium,
    displaySmall = TypeScale.displaySmall,
    headlineLarge = TypeScale.headlineLarge,
    headlineMedium = TypeScale.headlineMedium,
    headlineSmall = TypeScale.headlineSmall,
    titleLarge = TypeScale.titleLarge,
    titleMedium = TypeScale.titleMedium,
    titleSmall = TypeScale.titleSmall,
    bodyLarge = TypeScale.bodyLarge,
    bodyMedium = TypeScale.bodyMedium,
    bodySmall = TypeScale.bodySmall,
    labelLarge = TypeScale.labelLarge,
    labelMedium = TypeScale.labelMedium,
    labelSmall = TypeScale.labelSmall,
)

/** Sem vidro na TV: as caixas de TV costumam ter pouca GPU, e o desfoque não compensa. */
private val Floating = Color(0xF528292D)

private val CardShape = RoundedCornerShape(14.dp)

/** O canto dos cartões dos Ajustes (o mesmo do celular). */
private val GroupCorner = 18.dp

/** Interface da Android TV: D-pad, foco visível e cards maiores (Compose for TV). */
object TvUi : Screens {
    private val OkKeys = setOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
    private val GridGapX = 20.dp

    /** Quantos quadros esperar pelo primeiro card antes de desistir de pôr o foco nele. */
    private const val FOCUS_TRIES = 3
    private val GridGapY = 28.dp
    private val screenPadding = PaddingValues(horizontal = 48.dp, vertical = 26.dp)

    /**
     * As telas com lista (o início, uma playlist, um canal): sem margem embaixo, para a lista rolar
     * até a borda da tela em vez de ser cortada antes dela (achado do Johan na TV, 2026-11-04). O
     * respiro do fim da lista fica no contentPadding de cada uma.
     */
    private val listScreenPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 26.dp)

    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = remember(Look.accent) { colors() }, typography = Type) {
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
            Wordmark(40.sp)
            Spacer(Modifier.height(44.dp))
            Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(28.dp))
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
        HomeTab.EXPLORE to R.string.tab_explore,
        HomeTab.SEARCH to R.string.tab_search,
        HomeTab.PLAYLISTS to R.string.tab_playlists,
        HomeTab.HISTORY to R.string.tab_history,
        HomeTab.SETTINGS to R.string.tab_settings,
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val initialFocus = rememberSaveable { mutableStateOf(true) }
        var tabsFocused by remember { mutableStateOf(false) }
        // Segurar OK numa aba recarrega a lista dela, quando o OK é solto depois de [OkHold.HOLD_MS].
        // O Tab segue recebendo as teclas (o clique dele só escolhe a aba, que já está escolhida).
        var heldTab by remember { mutableStateOf<HomeTab?>(null) }
        val selectedTab = remember { FocusRequester() }
        // Voltar, de dentro do conteúdo, sobe para as abas; nas abas, sai do app.
        BackHandler(enabled = !tabsFocused) { runCatching { selectedTab.requestFocus() } }

        Column(Modifier.fillMaxSize().padding(listScreenPadding)) {
          // O logotipo inteiro (a fresta, o facho com a poeira e o nome), sem fundo, na mesma linha
          // das abas (pedido do Johan, 2026-11-13). O facho passa um pouco da caixa, por trás das abas.
          Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(22.sp, Modifier.offset(y = (-2).dp))
            Spacer(Modifier.width(6.dp))
            val selectedIndex = tabs.indexOfFirst { it.first == tab }
            TabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus }.entersAt(selectedTab),
                separator = { Spacer(Modifier.width(2.dp)) },
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
                                val key = event.nativeKeyEvent
                                if (key.keyCode in OkKeys) {
                                    if (event.type == KeyEventType.KeyDown && key.repeatCount == 0) {
                                        heldTab = item
                                    } else if (event.type == KeyEventType.KeyUp) {
                                        if (heldTab == item && key.eventTime - key.downTime >= OkHold.HOLD_MS) onReload(item)
                                        heldTab = null
                                    }
                                }
                                false
                            },
                    ) {
                        TabLabel(stringResource(label), selected = item == tab)
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

    /**
     * O nome de uma aba, nos dois pesos do logotipo: a escolhida em bold, as outras em light. A
     * largura é sempre a do bold (desenhado invisível por baixo), para as abas não andarem de lado
     * quando a escolhida muda.
     */
    @Composable
    private fun TabLabel(text: String, selected: Boolean) {
        val style = MaterialTheme.typography.titleMedium
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(text, style = style.copy(fontWeight = FontWeight.Bold), color = Color.Transparent, maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
            Text(text, style = style.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Light), maxLines = 1)
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
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, stringResource(R.string.recommended_empty), vm::loadMore, onPlay, onOpenChannel, takeFocus = false, onFocusTaken = {})
        }
    }

    @Composable
    override fun Explore(vm: ExploreViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val filters by vm.filters.collectAsStateWithLifecycle()
        val showIntro by vm.showIntro.collectAsStateWithLifecycle()
        Column(Modifier.fillMaxSize()) {
            FilterButtons(exploreFilterChoices(filters), onChange = vm::setFilters)
            Spacer(Modifier.height(16.dp))
            Loadable(state, onRetry = vm::reload) { content ->
                val empty = stringResource(if (filters.isDefault) R.string.explore_empty else R.string.explore_filtered_empty)
                VideoGrid(content, empty, vm::loadMore, onPlay, onOpenChannel, takeFocus = false, onFocusTaken = {})
            }
        }
        // Só na primeira vez (enquanto os primeiros vídeos carregam): o que é a Explore.
        if (showIntro) ExploreIntro(onDismiss = vm::dismissIntro)
    }

    @Composable
    private fun ExploreIntro(onDismiss: () -> Unit) {
        val button = remember { FocusRequester() }
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(560.dp)) {
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.tab_explore), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.explore_intro_tv), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onDismiss, modifier = Modifier.focusRequester(button)) { Text(stringResource(R.string.got_it)) }
                }
            }
            // Dentro do diálogo: a janela dele já existe quando o foco é pedido.
            LaunchedEffect(Unit) { runCatching { button.requestFocus() } }
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(
                content,
                stringResource(R.string.history_empty),
                vm::loadMore,
                onPlay,
                onOpenChannel,
                takeFocus = false,
                onFocusTaken = {},
                onRemove = { video ->
                    vm.remove(video) { Notices.show(res.getString(R.string.history_remove_failed, it)) }
                    Notices.show(res.getString(R.string.history_removed))
                },
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
    internal fun FilterButtons(choices: List<SearchFilterChoice>, onChange: (SearchFilters) -> Unit) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { choice ->
                key(choice.label) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        val label = stringResource(R.string.search_filter_value, stringResource(choice.label), choice.selected.text())
                        // Leves (2026-11-03): sem contorno, num cinza um pouco mais claro que o fundo; o filtro
                        // mudado fica âmbar, e o foco, creme como os outros botões da TV.
                        Button(
                            onClick = { open = true },
                            colors = ButtonDefaults.colors(
                                containerColor = if (choice.isDefault) Look.Surface else Look.Glow.copy(alpha = 0.16f),
                                contentColor = if (choice.isDefault) Look.Sub else Look.Glow,
                                focusedContainerColor = Look.Text,
                                focusedContentColor = Look.Ground,
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
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
    internal fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Playlist) -> Unit) {
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

        // Em cartões, como no celular (2026-11-03): o nome do grupo em cima, as linhas num cartão e a
        // explicação embaixo. O foco continua o do ListItem (a linha fica creme).
        LazyColumn(
            modifier = Modifier.fillMaxWidth(0.7f).widthIn(max = 820.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            group(
                "lives",
                R.string.settings_subscriptions,
                rows = listOf(
                    row("show") {
                        ListItem(
                            selected = false,
                            onClick = { vm.setShowLives(!settings.showLives) },
                            headlineContent = { Text(stringResource(R.string.settings_show_lives)) },
                            supportingContent = { Text(stringResource(R.string.settings_show_lives_desc)) },
                            trailingContent = { Switch(checked = settings.showLives, onCheckedChange = null) },
                        )
                    },
                ),
            )

            val targetRows = buildList<Pair<Any, @Composable () -> Unit>> {
                add(
                    row("current") {
                        val target = settings.saveTarget.title ?: stringResource(R.string.watch_later)
                        ListItem(
                            selected = false,
                            onClick = {
                                choosingTarget = !choosingTarget
                                if (choosingTarget) vm.loadOwnPlaylists()
                            },
                            headlineContent = { Text(stringResource(R.string.settings_save_target_row)) },
                            trailingContent = { Text(target, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    },
                )
                if (choosingTarget) {
                    when (val own = ownPlaylists) {
                        null, UiState.Loading -> add(row("loading") { EmptyRow(stringResource(R.string.loading)) })
                        is UiState.Error -> add(
                            row("error") {
                                ListItem(
                                    selected = false,
                                    onClick = vm::loadOwnPlaylists,
                                    headlineContent = { Text(stringResource(R.string.error_generic, own.message)) },
                                    trailingContent = { Text(stringResource(R.string.retry)) },
                                )
                            },
                        )
                        is UiState.Ready -> own.data.forEach { playlist ->
                            add(
                                row(playlist.id) {
                                    ListItem(
                                        selected = false,
                                        onClick = {
                                            vm.setSaveTarget(playlist)
                                            choosingTarget = false
                                        },
                                        headlineContent = { Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingContent = { RadioButton(selected = playlist.id == settings.saveTarget.playlistId, onClick = null) },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            group("target", R.string.settings_save_target, rows = targetRows)

            topicGroup(
                key = "excluded",
                title = R.string.settings_excluded,
                hint = R.string.settings_excluded_hint,
                topics = settings.filters.excluded,
                onEdit = { editing = TopicList.EXCLUDED to it },
                onRemove = { vm.removeTopic(TopicList.EXCLUDED, it) },
            )
            topicGroup(
                key = "preferred",
                title = R.string.settings_preferred,
                hint = R.string.settings_preferred_hint,
                topics = settings.filters.preferred,
                onEdit = { editing = TopicList.PREFERRED to it },
                onRemove = { vm.removeTopic(TopicList.PREFERRED, it) },
            )

            group(
                "blocked",
                R.string.settings_blocked,
                hint = R.string.settings_blocked_hint,
                rows = if (settings.filters.blocked.isEmpty()) {
                    listOf(row("empty") { EmptyRow(stringResource(R.string.settings_blocked_empty)) })
                } else {
                    settings.filters.blocked.map { channel ->
                        row(channel.id) {
                            ListItem(
                                selected = false,
                                onClick = { vm.unblock(channel.id) },
                                headlineContent = { Text(channel.name.ifBlank { channel.id }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingContent = { Text(stringResource(R.string.unblock)) },
                            )
                        }
                    }
                },
            )

            group(
                "links",
                R.string.settings_links,
                rows = externalLinkOptions.map { (mode, label) ->
                    row(mode) {
                        ListItem(
                            selected = false,
                            onClick = { vm.setExternalLinks(mode) },
                            headlineContent = { Text(stringResource(label)) },
                            leadingContent = { RadioButton(selected = settings.externalLinks == mode, onClick = null) },
                        )
                    }
                },
            )

            val playlistRows = buildList<Pair<Any, @Composable () -> Unit>> {
                error?.let { message ->
                    add(row("error") { EmptyRow(stringResource(R.string.action_failed, message), MaterialTheme.colorScheme.error) })
                }
                when (val p = playlists) {
                    UiState.Loading -> add(row("loading") { EmptyRow(stringResource(R.string.loading)) })
                    is UiState.Error -> add(
                        row("retry") {
                            ListItem(
                                selected = false,
                                onClick = vm::reloadPlaylists,
                                headlineContent = { Text(stringResource(R.string.error_generic, p.message)) },
                                trailingContent = { Text(stringResource(R.string.retry)) },
                            )
                        },
                    )
                    is UiState.Ready -> {
                        if (p.data.isEmpty()) add(row("empty") { EmptyRow(stringResource(R.string.settings_playlists_empty)) })
                        p.data.forEach { item ->
                            add(
                                row(item.playlist.browseId) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ListItem(
                                            selected = false,
                                            onClick = { onOpenPlaylist(item.playlist) },
                                            headlineContent = { Text(item.playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            supportingContent = item.playlist.info?.let { { Text(spacedDots(it), maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        OutlinedButton(onClick = { vm.unsubscribe(item.playlist) }, enabled = !item.busy) {
                                            Text(stringResource(R.string.settings_unsubscribe))
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            group("playlists", R.string.settings_playlists, hint = R.string.settings_playlists_hint, rows = playlistRows)

            group(
                "icon",
                R.string.settings_app_icon,
                hint = R.string.settings_app_icon_hint,
                rows = AppIcon.entries.map { icon ->
                    row(icon.name) {
                        ListItem(
                            selected = false,
                            onClick = { vm.setAppIcon(icon) },
                            headlineContent = { Text(stringResource(icon.label)) },
                            leadingContent = { AppIconPreview(icon) },
                            trailingContent = { RadioButton(selected = appIcon == icon, onClick = null) },
                        )
                    }
                },
            )

            // A cor de destaque, como a do ícone (pedido do Johan, 2026-11-03).
            group(
                "accent",
                R.string.settings_accent,
                hint = R.string.settings_accent_hint,
                rows = Accent.entries.map { option ->
                    row(option.name) {
                        ListItem(
                            selected = false,
                            onClick = { Accents.select(option) },
                            headlineContent = { Text(stringResource(option.label)) },
                            leadingContent = { Box(Modifier.size(24.dp).background(Color(option.glow), CircleShape)) },
                            trailingContent = { RadioButton(selected = Look.accent == option, onClick = null) },
                        )
                    }
                },
            )

            group(
                "account",
                R.string.settings_account,
                rows = listOf(
                    row("signout") {
                        ListItem(
                            selected = false,
                            onClick = { vm.signOut(onSignedOut) },
                            headlineContent = { Text(stringResource(R.string.sign_out)) },
                            supportingContent = vm.accountName?.let { { Text(stringResource(R.string.settings_signed_in_as, it)) } },
                        )
                    },
                ),
            )
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

    private fun LazyListScope.topicGroup(
        key: String,
        title: Int,
        hint: Int,
        topics: List<Topic>,
        onEdit: (Int?) -> Unit,
        onRemove: (Int) -> Unit,
    ) {
        val rows = buildList<Pair<Any, @Composable () -> Unit>> {
            if (topics.isEmpty()) add(row("empty") { EmptyRow(stringResource(R.string.settings_topics_empty)) })
            topics.forEachIndexed { index, topic ->
                add(
                    row("$index:${topic.name}") {
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
                    },
                )
            }
            add(
                row("add") {
                    ListItem(
                        selected = false,
                        onClick = { onEdit(null) },
                        headlineContent = { Text("+ " + stringResource(R.string.topic_add), color = Look.Glow) },
                    )
                },
            )
        }
        group(key, title, hint = hint, rows = rows)
    }

    /**
     * Um grupo dos Ajustes: [title] em cima, as [rows] num cartão e [hint] embaixo. O cartão é
     * desenhado em pedaços, um por linha da lista (cantos arredondados só no primeiro e no último).
     */
    private fun LazyListScope.group(key: String, title: Int?, hint: Int? = null, rows: List<Pair<Any, @Composable () -> Unit>>) {
        if (title != null) item(key = "$key:title") { SectionTitle(stringResource(title)) }
        rows.forEachIndexed { index, (rowKey, content) ->
            item(key = "$key:row:$rowKey") { Box(Modifier.groupPiece(index, rows.size)) { content() } }
        }
        if (hint != null) item(key = "$key:hint") { Hint(stringResource(hint)) }
    }

    /** Uma linha de um [group], com a chave dela na lista. */
    private fun row(key: Any, content: @Composable () -> Unit): Pair<Any, @Composable () -> Unit> = key to content

    private fun Modifier.groupPiece(index: Int, count: Int): Modifier {
        val top = if (index == 0) GroupCorner else 0.dp
        val bottom = if (index == count - 1) GroupCorner else 0.dp
        return fillMaxWidth()
            .background(Look.Surface, RoundedCornerShape(top, top, bottom, bottom))
            .padding(start = 8.dp, end = 8.dp, top = if (index == 0) 8.dp else 0.dp, bottom = if (index == count - 1) 8.dp else 0.dp)
    }

    /** Uma linha só de texto num grupo (vazio, carregando, erro). */
    @Composable
    private fun EmptyRow(text: String, color: Color = Look.Sub) {
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
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
        val editable by vm.editable.collectAsStateWithLifecycle()
        val res = LocalContext.current.resources
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }
        // O modo mover: OK pega e solta em vez de abrir o vídeo. Cada vídeo solto vai para a conta na hora.
        var arranging by rememberSaveable { mutableStateOf(false) }
        // Posição do vídeo "pego" no modo mover (null = nenhum).
        var held by remember { mutableStateOf<Int?>(null) }
        val drop = {
            if (held != null) {
                held = null
                vm.drop { Notices.show(res.getString(R.string.reorder_failed, it)) }
            }
        }
        BackHandler(enabled = arranging) { if (held != null) drop() else arranging = false }

        Column(Modifier.fillMaxSize().padding(listScreenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shownTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                (subscription as? Subscription.Shown)?.let { shown ->
                    Spacer(Modifier.width(24.dp))
                    SubscribeButton(shown) { vm.toggleSubscription(shownTitle) }
                }
                if (editable) {
                    Spacer(Modifier.width(24.dp))
                    if (arranging) {
                        Button(onClick = {
                            drop()
                            arranging = false
                        }) { Text(stringResource(R.string.reorder_done)) }
                    } else {
                        OutlinedButton(
                            onClick = { arranging = true },
                            // A lista guardada no aparelho pode estar velha: mover espera a da conta.
                            enabled = (state as? UiState.Ready)?.data?.cached == false,
                        ) { Text(stringResource(R.string.reorder)) }
                    }
                }
            }
            val note: Pair<String, Boolean>? = when {
                arranging -> stringResource(R.string.reorder_hint_tv) to false
                else -> (subscription as? Subscription.Shown)?.let { shown ->
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
            val saveTarget by SavedVideos.target.collectAsStateWithLifecycle()
            Loadable(state, onRetry = vm::reload) { content ->
                if (arranging) {
                    ReorderList(
                        content.videos,
                        held = held,
                        onHold = { held = it },
                        onDrop = drop,
                        onMove = vm::move,
                        onNearEnd = vm::loadMore,
                    )
                } else {
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
                        onRemove = if (editable) {
                            { video ->
                                vm.remove(video) { Notices.show(res.getString(R.string.playlist_remove_failed, it)) }
                                Notices.show(res.getString(R.string.playlist_removed, shownTitle))
                            }
                        } else {
                            null
                        },
                        removeLabel = stringResource(R.string.playlist_remove),
                    )
                }
            }
        }
    }

    /**
     * O modo mover: OK pega o vídeo, cima e baixo levam ele pela lista, OK de novo solta (e a
     * posição nova vai para a conta). Chegando no fim do que carregou, a próxima página vem.
     */
    @Composable
    private fun ReorderList(
        videos: List<Video>,
        held: Int?,
        onHold: (Int) -> Unit,
        onDrop: () -> Unit,
        onMove: (Int, Int) -> Unit,
        onNearEnd: () -> Unit,
    ) {
        val list = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val first = remember { FocusRequester() }
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            itemsIndexed(videos, key = { _, video -> video.id + video.playlistIndex }) { index, video ->
                val isHeld = held == index
                if (index >= videos.size - 5) LaunchedEffect(videos.size) { onNearEnd() }
                ListItem(
                    selected = isHeld,
                    onClick = {
                        // Pegar outro vídeo solta o que estava pego.
                        onDrop()
                        if (!isHeld) onHold(index)
                    },
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
                            if (to in videos.indices) {
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
        val subscription by vm.subscription.collectAsStateWithLifecycle()
        ChannelPage(
            title, state, blocked, subscription,
            onSubscribe = vm::toggleSubscribed,
            onBlock = vm::toggleBlocked,
            onRetry = vm::reload,
            onNearEnd = vm::loadMore,
            onPlay = onPlay,
            onOpenPlaylist = onOpenPlaylist,
        )
    }

    /** A página do canal com o que o [ChannelViewModel] diz (separada para as fotos de tela). */
    @Composable
    internal fun ChannelPage(
        title: String,
        state: UiState<ChannelViewModel.Content>,
        blocked: Boolean,
        subscription: Subscription,
        onSubscribe: () -> Unit,
        onBlock: () -> Unit,
        onRetry: () -> Unit,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
    ) {
        var tab by rememberSaveable { mutableIntStateOf(0) }

        Column(Modifier.fillMaxSize().padding(listScreenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.ifBlank { stringResource(R.string.channel_default_title) },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // A inscrição, o mesmo botão da playlist (pedido do Johan, 2026-11-04), e o bloqueio.
                (subscription as? Subscription.Shown)?.let { shown ->
                    Spacer(Modifier.width(24.dp))
                    SubscribeButton(shown, onToggle = onSubscribe)
                }
                Spacer(Modifier.width(16.dp))
                BlockButton(blocked, onToggle = onBlock)
            }
            (subscription as? Subscription.Shown)?.error?.let {
                Text(
                    stringResource(R.string.action_failed, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Loadable(state, onRetry = onRetry) { content ->
                Column(Modifier.fillMaxSize()) {
                    if (content.playlists.isNotEmpty()) {
                        ChannelTabs(tab, onTab = { tab = it })
                        Spacer(Modifier.height(16.dp))
                    }
                    if (tab == 1 && content.playlists.isNotEmpty()) {
                        PlaylistGrid(content.playlists, onOpenPlaylist)
                    } else {
                        VideoGrid(
                            content = content.videos,
                            emptyText = stringResource(R.string.list_empty),
                            onNearEnd = onNearEnd,
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
     * "Subscribe" ou "✓ Subscribed" (contornado), na playlist e no canal; apertar de novo cancela a
     * inscrição. Um botão só, que só muda de cara: o foco fica nele quando a inscrição muda (dois
     * botões trocando, ou um desligado enquanto vai para a conta, soltavam o foco).
     */
    @Composable
    private fun SubscribeButton(state: Subscription.Shown, onToggle: () -> Unit) {
        Button(
            onClick = { if (!state.busy) onToggle() },
            colors = if (state.subscribed) OutlinedButtonDefaults.colors() else ButtonDefaults.colors(),
            border = if (state.subscribed) OutlinedButtonDefaults.border() else ButtonDefaults.border(),
        ) {
            if (state.subscribed) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(if (state.subscribed) R.string.playlist_subscribed else R.string.playlist_subscribe))
        }
    }

    /** Vídeos e Playlists do canal. A aba muda com o foco, como as de cima. */
    @Composable
    internal fun ChannelTabs(tab: Int, onTab: (Int) -> Unit) {
        val selected = remember { FocusRequester() }
        TabRow(selectedTabIndex = tab, modifier = Modifier.entersAt(selected)) {
            listOf(R.string.channel_videos, R.string.channel_playlists).forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onFocus = { onTab(i) },
                    onClick = { onTab(i) },
                    modifier = if (tab == i) Modifier.focusRequester(selected) else Modifier,
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
    internal fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: ((Channel) -> Unit)?,
        takeFocus: Boolean,
        onFocusTaken: () -> Unit,
        showSaved: Boolean = true,
        onRemove: ((Video) -> Unit)? = null,
        removeLabel: String? = null,
    ) {
        val firstFocus = remember { FocusRequester() }
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        val grid = rememberLazyGridState()
        var inGrid by remember { mutableStateOf(false) }
        PrefetchThumbnails(content.videos, grid)
        // De volta ao começo com vídeos novos: o foco que estava na grade vai para o primeiro deles.
        ListTop(grid, content) {
            if (!inGrid) return@ListTop
            // O primeiro card só existe depois que a grade mede a lista nova.
            repeat(FOCUS_TRIES) {
                withFrameNanos { }
                if (runCatching { firstFocus.requestFocus() }.getOrElse { false } != false) return@ListTop
            }
        }
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
            modifier = Modifier.onFocusChanged { inGrid = it.hasFocus },
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
                    saved = video.id in saved,
                    showSaved = showSaved,
                    onOpenChannel = onOpenChannel,
                    onRemove = onRemove?.let { remove -> removeLabel?.let { it to remove } },
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
     * "Save to Watch later" (ou "Remove from…", se já estiver lá), "Go to channel" e, no Histórico
     * e nas playlists do usuário ([onRemove]: o texto e a ação), tirar o vídeo da lista.
     *
     * @param showSaved false na própria playlist do botão Save: sem o marcador (estaria em todos)
     * e sem a opção de salvar (tirar é o [onRemove]).
     */
    @Composable
    private fun VideoCard(
        video: Video,
        saved: Boolean,
        onOpenChannel: ((Channel) -> Unit)?,
        modifier: Modifier = Modifier,
        showSaved: Boolean = true,
        onRemove: Pair<String, (Video) -> Unit>? = null,
        onClick: () -> Unit,
    ) {
        var menu by remember { mutableStateOf(false) }
        // O menu aparece no toque longo, com o OK ainda apertado (pedido do Johan), mas só pega o
        // foco quando o OK é solto: se pegasse antes, o "soltar" iria para o menu (e escolheria a
        // primeira opção) e o card ficaria esperando um soltar que não vem.
        var menuFocusable by remember { mutableStateOf(false) }
        val res = LocalContext.current.resources
        val scope = rememberCoroutineScope()
        // Com o foco parado no card, o vídeo é pedido ao YouTube antes do OK ([PlaybackRepository.focused]).
        // Uma live ou uma estreia marcada, não: o que o YouTube entrega para elas muda a cada instante.
        val ahead = !video.isLive && !video.isUpcoming
        if (ahead) DisposableEffect(video.id) { onDispose { PlaybackRepository.unfocused(video.id) } }
        Column(modifier) {
            Box(
                Modifier.onFocusChanged { focus ->
                    if (!ahead) return@onFocusChanged
                    if (focus.hasFocus) PlaybackRepository.focused(video.id) else PlaybackRepository.unfocused(video.id)
                }.onPreviewKeyEvent { event ->
                    if (menu && !menuFocusable && event.type == KeyEventType.KeyUp && event.nativeKeyEvent.keyCode in OkKeys) {
                        menuFocusable = true
                    }
                    false
                },
            ) {
                GlowCard(
                    onClick = onClick,
                    onLongClick = {
                        menuFocusable = false
                        menu = true
                    },
                ) {
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
                        if (saved && showSaved) {
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
                        channelName = video.channelName?.takeIf { it.isNotBlank() },
                        focusable = menuFocusable,
                        onDismiss = { menu = false },
                        onSave = if (showSaved) {
                            { toggleSaved(res, video) }
                        } else {
                            null
                        },
                        // Sempre, também sem o ID do canal no card (as Recomendações): ele é procurado na hora.
                        onChannel = onOpenChannel?.let { open -> { scope.openChannelOf(video, res, open) } },
                        remove = onRemove?.let { (label, remove) -> label to { remove(video) } },
                    )
                }
            }
            VideoText(video)
        }
    }

    /** O menu do OK longo: abre ao lado do card (à direita, ou à esquerda na última coluna). */
    @Composable
    private fun VideoMenu(
        saved: Boolean,
        channelName: String?,
        focusable: Boolean,
        onDismiss: () -> Unit,
        onSave: (() -> Unit)?,
        onChannel: (() -> Unit)?,
        remove: Pair<String, () -> Unit>? = null,
    ) {
        val gap = with(LocalDensity.current) { 18.dp.roundToPx() }
        val target by SavedVideos.target.collectAsStateWithLifecycle()
        val playlist = target.title ?: stringResource(R.string.watch_later)
        val first = remember { FocusRequester() }
        Popup(
            popupPositionProvider = remember(gap) { BesideAnchor(gap) },
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = focusable),
        ) {
            Column(
                Modifier.width(320.dp)
                    .background(Floating, RoundedCornerShape(22.dp))
                    .border(1.dp, Look.Hairline, RoundedCornerShape(22.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // O foco começa na primeira opção que aparece.
                val firstFocus = Modifier.focusRequester(first)
                if (onSave != null) {
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
                        modifier = firstFocus,
                    ) {
                        onDismiss()
                        onSave()
                    }
                }
                if (onChannel != null) {
                    MenuItem(
                        text = stringResource(R.string.video_menu_channel),
                        supporting = channelName,
                        modifier = if (onSave == null) firstFocus else Modifier,
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
                if (remove != null) {
                    MenuItem(
                        text = remove.first,
                        supporting = null,
                        leading = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Look.Sub, modifier = Modifier.size(22.dp)) },
                        modifier = if (onSave == null && onChannel == null) firstFocus else Modifier,
                    ) {
                        onDismiss()
                        remove.second()
                    }
                }
            }
            LaunchedEffect(focusable) { if (focusable) runCatching { first.requestFocus() } }
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

    /**
     * Card com o foco da TV: cresce 6% e ganha um contorno claro, sem brilho em volta (pedido do
     * Johan, 2026-11-03: brilho só no ícone do canto; e a sombra borrada pesava nas TVs de 32
     * bits). Com [onLongClick], o OK curto e o longo são medidos pelo app ([OkHold]), e não pelo Card.
     */
    @Composable
    private fun GlowCard(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
        val interaction = remember { MutableInteractionSource() }
        val hold = onLongClick?.let { rememberOkHold(interaction, onClick, it) }
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).then(if (hold != null) Modifier.okHold(hold) else Modifier),
            shape = CardDefaults.shape(CardShape),
            colors = CardDefaults.colors(containerColor = Look.Surface),
            scale = CardDefaults.scale(focusedScale = 1.06f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Look.GlowSoft), shape = CardShape)),
            interactionSource = interaction,
        ) { content() }
    }

    /** O [OkHold] de um card, com o "apertado" passado para o [interaction] dele. */
    @Composable
    private fun rememberOkHold(interaction: MutableInteractionSource, onClick: () -> Unit, onLongClick: () -> Unit): OkHold {
        val scope = rememberCoroutineScope()
        val click by rememberUpdatedState(onClick)
        val longClick by rememberUpdatedState(onLongClick)
        return remember(interaction, scope) {
            var press: PressInteraction.Press? = null
            OkHold(
                scope,
                onPress = { down ->
                    press?.let { interaction.tryEmit(PressInteraction.Release(it)) }
                    press = if (down) PressInteraction.Press(Offset.Zero).also { interaction.tryEmit(it) } else null
                },
                onClick = { click() },
                onLongClick = { longClick() },
            )
        }
    }

    /** Todas as teclas de OK vão para o [hold], antes do Card; o foco saindo cancela o OK apertado. */
    private fun Modifier.okHold(hold: OkHold): Modifier =
        onFocusChanged { if (!it.hasFocus) hold.cancel() }
            .onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.keyCode !in OkKeys) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> if (key.repeatCount == 0) hold.down()
                    KeyEventType.KeyUp -> hold.up()
                }
                true
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

    /**
     * O título e, embaixo, o canal numa linha e o resto (visualizações, data) na outra, em vez de
     * os dois numa linha só, cortados.
     */
    @Composable
    private fun VideoText(video: Video) {
        val name = video.channelName?.takeIf { it.isNotBlank() }
        if (video.fromPlaylist != null || name == null) return CardText(video.title, subtitleOf(video))
        CardText(video.title, null)
        val rest = metaAfterChannel(video.info, name, video.published)
        MetaLine(
            channel = { Text(name, style = CardMeta, color = Look.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            rest = rest?.let { { Text(spacedDots(rest), style = CardMeta, color = Look.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            separator = { Text(" · ", style = CardMeta, color = Look.Sub) },
            modifier = Modifier.padding(top = CardMetaGap, start = 2.dp, end = 2.dp),
            stacked = true,
        )
    }

    /**
     * Os textos dos cards (pedido do Johan, 2026-11-13): o título um pouco menor, para caber mais
     * dele, com as duas linhas mais juntas, e um respiro maior entre ele e o canal. Antes era o
     * titleSmall (14 sp, linhas de 20 sp), com o canal colado, e as linhas do título ficavam tão
     * longe uma da outra quanto do canal.
     */
    private val CardTitle: TextStyle
        @Composable get() = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)

    private val CardMeta: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp)

    private val CardMetaGap = 5.dp

    @Composable
    private fun CardText(title: String, subtitle: String?) {
        Text(
            title,
            style = CardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 11.dp, start = 2.dp, end = 2.dp),
        )
        if (subtitle != null) {
            Text(
                spacedDots(subtitle),
                style = CardMeta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Look.Sub,
                modifier = Modifier.padding(top = CardMetaGap, start = 2.dp, end = 2.dp),
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
                    spacedDots(it),
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
    }

    @Composable
    internal fun GridSkeleton() {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
            userScrollEnabled = false,
        ) {
            items(8) {
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Look.Surface, CardShape))
                    Box(Modifier.padding(top = 14.dp).fillMaxWidth(0.85f).height(16.dp).background(Look.Surface, RoundedCornerShape(6.dp)))
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth(0.5f).height(12.dp).background(Look.Surface, RoundedCornerShape(6.dp)))
                }
            }
        }
    }

    @Composable
    private fun <T> Loadable(state: UiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
        when (state) {
            // No lugar do "Loading…" (2026-11-03): cartões vazios onde os vídeos vão entrar, parados.
            UiState.Loading -> GridSkeleton()
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
