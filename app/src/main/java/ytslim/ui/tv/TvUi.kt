package ytslim.ui.tv

import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
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
import ytslim.data.AppScope
import ytslim.data.AuthRepository
import ytslim.data.User
import ytslim.data.Users
import ytslim.data.Channel
import ytslim.data.PlaybackRepository
import ytslim.data.Playlist
import ytslim.data.SavedVideos
import ytslim.data.SearchFilters
import ytslim.data.SearchItem
import ytslim.data.Topic
import ytslim.data.TopicRules
import ytslim.data.UpNext
import ytslim.data.Video
import ytslim.player.ScreenOffService
import ytslim.ui.AppIconPreview
import ytslim.ui.ChannelViewModel
import ytslim.ui.ExploreViewModel
import ytslim.ui.Accent
import ytslim.ui.Accents
import ytslim.ui.Background
import ytslim.ui.BackgroundSwatch
import ytslim.ui.Backgrounds
import ytslim.ui.FeedViewModel
import ytslim.ui.HistoryViewModel
import ytslim.ui.HomeTab
import ytslim.ui.LightSlit
import ytslim.ui.LocalAddAccount
import ytslim.ui.LocalSession
import ytslim.ui.homeTabs
import ytslim.ui.monogramOf
import ytslim.ui.ListTop
import ytslim.ui.MetaLine
import ytslim.ui.channelNameOf
import ytslim.ui.metaAfterChannel
import ytslim.ui.AppLanguage
import ytslim.ui.Language
import ytslim.ui.Look
import ytslim.ui.TopicsViewModel
import ytslim.ui.findActivity
import ytslim.ui.label
import ytslim.ui.TypeScale
import ytslim.ui.Wordmark
import ytslim.ui.BeamGlyph
import ytslim.ui.Notice
import ytslim.ui.Notices
import ytslim.ui.blockNotice
import ytslim.ui.PlaylistViewModel
import ytslim.ui.Subscription
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.PrefetchThumbnails
import ytslim.ui.SeenRows
import ytslim.ui.QrCode
import ytslim.ui.RecommendedViewModel
import ytslim.ui.Screens
import ytslim.ui.SearchFilterChoice
import ytslim.ui.SearchViewModel
import ytslim.ui.SettingsViewModel
import ytslim.ui.SubscribedPlaylistsViewModel
import ytslim.ui.TopicListsViewModel
import ytslim.ui.TopicListsViewModel.TopicList
import ytslim.ui.topicCounts
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListContent
import ytslim.ui.openChannelOf
import ytslim.ui.exploreFilterChoices
import ytslim.ui.captionChoices
import ytslim.ui.endActionOptions
import ytslim.ui.externalLinkOptions
import ytslim.ui.formatDuration
import ytslim.ui.searchFilterChoices
import ytslim.ui.sleepDim
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
    focusProperties {
        onEnter = {
            // Só se entra nas abas subindo ou descendo. Com as abas encostadas à direita, a seta para a
            // direita no último card de uma fileira achava uma aba "mais à direita" e pulava para lá.
            if (requestedFocusDirection == FocusDirection.Left || requestedFocusDirection == FocusDirection.Right) {
                cancelFocusChange()
            } else {
                selected.requestFocus()
            }
        }
    }.focusGroup()

/**
 * Entrar no menu lateral (pela esquerda, vindo do conteúdo) para na aba aberta ([selected]), como
 * subir até as abas de cima ([entersAt]). Subindo ou descendo não se entra: o menu fica ao lado, e
 * subir da primeira fileira de cards (ou descer da última) achava o item mais perto dele e trocava a
 * página.
 */
private fun Modifier.sideEntersAt(selected: FocusRequester) =
    focusProperties {
        onEnter = {
            if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) {
                cancelFocusChange()
            } else {
                selected.requestFocus()
            }
        }
    }.focusGroup()

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
private val Floating: Color get() = Look.Floating

private val CardShape = RoundedCornerShape(14.dp)

/** O canto dos cartões dos Ajustes (o mesmo do celular). */
private val GroupCorner = 18.dp

/** Interface da Android TV: D-pad, foco visível e cards maiores (Compose for TV). */
object TvUi : Screens {
    private const val TAG = "BeamTube"
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
        MaterialTheme(colorScheme = remember(Look.accent, Look.tones) { colors() }, typography = Type) {
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
    override fun SignIn(vm: SignInViewModel, onGuest: (() -> Unit)?, onCancel: (() -> Unit)?) {
        val state by vm.state.collectAsStateWithLifecycle()
        BackHandler(enabled = onCancel != null) { onCancel?.invoke() }

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
            // Sem conta: o convidado (só na primeira vez); ao acrescentar uma conta, voltar. O botão
            // fica com o foco, que não tem outro lugar para ir enquanto o código está na tela.
            val other = onGuest ?: onCancel
            if (other != null && state !is SignInViewModel.State.Error) {
                Spacer(Modifier.height(32.dp))
                val focus = remember { FocusRequester() }
                OutlinedButton(onClick = other, modifier = Modifier.focusRequester(focus)) {
                    Text(stringResource(if (onGuest != null) R.string.sign_in_guest else R.string.cancel))
                }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            }
        }
    }

    private val allTabs = listOf(
        HomeTab.SUBSCRIPTIONS to R.string.tab_subscriptions,
        HomeTab.RECOMMENDED to R.string.tab_recommended,
        HomeTab.EXPLORE to R.string.tab_explore,
        HomeTab.SEARCH to R.string.tab_search,
        HomeTab.PLAYLISTS to R.string.tab_playlists,
        HomeTab.HISTORY to R.string.tab_history,
        HomeTab.SETTINGS to R.string.tab_settings,
    )

    /**
     * O menu lateral ([TvLayout.SIDE_MENU]): os ícones do trilho do tablet deitado, na mesma ordem,
     * com a Busca no meio das abas. Os Ajustes ficam no pé do trilho.
     */
    private val sideTabs = listOf(
        HomeTab.SUBSCRIPTIONS to Icons.Outlined.Subscriptions,
        HomeTab.RECOMMENDED to Icons.Outlined.Explore,
        HomeTab.EXPLORE to Icons.Outlined.GridView,
        HomeTab.SEARCH to Icons.Outlined.Search,
        HomeTab.PLAYLISTS to Icons.AutoMirrored.Outlined.PlaylistPlay,
        HomeTab.HISTORY to Icons.Outlined.History,
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val initialFocus = rememberSaveable(LocalSession.current) { mutableStateOf(tab == HomeTab.SUBSCRIPTIONS) }
        var tabsFocused by remember { mutableStateOf(false) }
        // OK na aba aberta recarrega a lista dela (1.0.6, pedido do Johan: "quando tiver em cima da
        // aba e apertar OK, recarrega"; até a 1.0.5 só segurando o OK). Conta só o OK que desceu
        // nesta aba: a subida de um OK apertado em outro lugar (no launcher, ao abrir o app) não
        // recarrega nada. A aba segue recebendo as teclas (o clique dela só escolhe a aba, que já
        // está escolhida). Vale nas abas de cima e no menu lateral.
        val pressedTab = remember { mutableStateOf<HomeTab?>(null) }
        val selectedTab = remember { FocusRequester() }
        // Voltar, de dentro do conteúdo, vai para as abas (ou o menu lateral); nelas, sai do app.
        BackHandler(enabled = !tabsFocused) { runCatching { selectedTab.requestFocus() } }
        // O convidado só tem a busca ([homeTabs]), nos dois layouts.
        val allowed = remember { homeTabs() }
        val tabs = remember(allowed) { allTabs.filter { it.first in allowed } }
        val labels = tabs.map { stringResource(it.second) }
        // As abas em cima (o padrão) ou o menu lateral do tablet (pedido do Johan, 2026-11-14), à
        // escolha nos Ajustes. O conteúdo fica no mesmo lugar da composição nos dois, então trocar
        // nos Ajustes não perde a posição nem o foco da linha escolhida.
        val side = TvLayouts.current == TvLayout.SIDE_MENU

        Row(Modifier.fillMaxSize()) {
            if (side) {
                SideMenu(
                    tab = tab,
                    onTab = onTab,
                    tabs = tabs.map { it.first },
                    labels = labels,
                    itemModifier = { item ->
                        (if (item == tab) Modifier.focusRequester(selectedTab) else Modifier).reloadsOnOk(item, pressedTab, onReload)
                    },
                    modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus }.sideEntersAt(selectedTab)
                        .oneTabPerPress(android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN),
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(if (side) sideScreenPadding else listScreenPadding)) {
                // A foto do usuário abre o menu de troca (pedido do Johan, 2026-11-14): no fim da
                // linha das abas, ou no canto direito da barra do título no menu lateral.
                if (side) {
                    TitleBar(labels[tabs.indexOfFirst { it.first == tab }], trailing = { UserButton(Modifier.photoFocus(selectedTab, side = true)) })
                } else {
                  // O logotipo inteiro (a fresta, o facho com a poeira e o nome), sem fundo, na mesma linha
                  // das abas (pedido do Johan, 2026-11-13), com a letra do tamanho da das abas (ele achou o de
                  // 22sp grande demais). O logotipo fica à esquerda e as abas, encostadas à direita (pedido dele
                  // do mesmo dia): o nome da última aba termina na linha da borda direita dos cards.
                  LogoAndTabs(labels, logo = { Wordmark(LogoSize.toFixedSp()) }, trailing = { UserButton(Modifier.photoFocus(selectedTab, side = false)) }) { labelSize ->
                    val selectedIndex = tabs.indexOfFirst { it.first == tab }
                    TabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus }.entersAt(selectedTab)
                            .oneTabPerPress(android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT),
                        separator = { Spacer(Modifier.width(TabSeparator)) },
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
                                    .reloadsOnOk(item, pressedTab, onReload),
                            ) {
                                TabLabel(labels[tabs.indexOfFirst { it.first == item }], selected = item == tab, size = labelSize)
                            }
                        }
                    }
                  }
                }
                Spacer(Modifier.height(ContentGap))
                CompositionLocalProvider(LocalInitialFocus provides initialFocus) {
                    // Sair da lista para o menu (ou para as abas) e voltar devolve o foco ao mesmo card
                    // (pedido do Johan): até a 1.0.7 a volta caía no card mais perto do item do menu,
                    // quase sempre uma fileira acima, e a lista subia. Só aqui, na 1.0.8, não bastava:
                    // o que guarda o card é o focusRestorer na própria grade (VideoGrid e as outras
                    // listas das abas), que também segura o card composto enquanto o foco está fora.
                    Box(Modifier.fillMaxSize().focusRestorer().focusGroup()) { content(tab) }
                }
            }
        }
    }

    /**
     * O foco da foto do usuário ao lado das abas. Com as abas em cima, subir do conteúdo continua
     * parando na aba aberta (0.6.0), mesmo debaixo da foto: ela só se alcança andando pelas abas até
     * o fim. Com o menu lateral, subir da primeira fileira chega nela. Nos dois, a seta para a
     * esquerda volta para a aba aberta.
     */
    private fun Modifier.photoFocus(selected: FocusRequester, side: Boolean): Modifier =
        focusProperties {
            onEnter = {
                if (!side && requestedFocusDirection == FocusDirection.Up) selected.requestFocus()
            }
        }.focusGroup().onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && event.type == KeyEventType.KeyDown) {
                runCatching { selected.requestFocus() }
                true
            } else {
                false
            }
        }

    /**
     * Nas abas, e no menu lateral, cada toque anda uma aba só: as repetições de uma seta segurada
     * ([keys]) não contam (1.0.11, pedido do Johan). Logo depois de ligar a TV, o controle às vezes
     * manda a soltura da tecla atrasada, o Android repete a seta como se ela estivesse segurada, e
     * um toque pulava duas ou três abas (das Inscrições direto para a Busca). Como cada aba troca a
     * página, andar uma por toque é o esperado.
     */
    private fun Modifier.oneTabPerPress(vararg keys: Int): Modifier =
        onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            val held = event.type == KeyEventType.KeyDown && key.repeatCount > 0 && key.keyCode in keys
            if (held && key.repeatCount == 1) Log.i(TAG, "tabs: held arrow ignored (${key.eventTime - key.downTime} ms)")
            held
        }

    /** OK nesta aba ([item]) recarrega a lista dela, se o OK desceu nela (ver [Home]). */
    private fun Modifier.reloadsOnOk(item: HomeTab, pressed: MutableState<HomeTab?>, onReload: (HomeTab) -> Unit): Modifier =
        onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            if (key.keyCode in OkKeys) {
                if (event.type == KeyEventType.KeyDown && key.repeatCount == 0) {
                    pressed.value = item
                } else if (event.type == KeyEventType.KeyUp) {
                    if (pressed.value == item) onReload(item)
                    pressed.value = null
                }
            }
            false
        }

    /**
     * A largura do menu lateral, da borda da tela até o fio que o separa do conteúdo. Um pouco mais
     * estreito na 1.0.8 (pedido do Johan: os ícones estavam grandes), junto com [SideIconSize],
     * [SideFocusSize] e [SideItemHeight]; até a 1.0.7, 88, 24, 46 e 52.
     */
    private val SideMenuWidth = 80.dp

    /** O ícone de cada aba no menu lateral. */
    private val SideIconSize = 21.dp

    /** O círculo do foco em volta do ícone. */
    private val SideFocusSize = 40.dp

    /** A altura de cada item do menu lateral. */
    private val SideItemHeight = 46.dp

    /** O espaço entre as abas (ou a barra do título) e o conteúdo. */
    private val ContentGap = 24.dp

    /** O respiro em cima da grade de vídeos (para o contorno do card focado não ser cortado). */
    private val GridTop = 8.dp

    /**
     * O espaço entre a fresta do alto do menu e o primeiro item, calculado para o topo do círculo do
     * foco do primeiro item ficar na linha do topo da primeira fileira de miniaturas. A 1.0.8 alinhou
     * o desenho do ícone, e o círculo focado parecia subir; o Johan preferiu o bloco alinhado, como
     * era até a 1.0.7 (1.0.9).
     */
    private val SideFirstItemGap = ContentGap + GridTop - (SideItemHeight - SideFocusSize) / 2

    /** O espaço de cima do menu lateral e da barra do título, que ficam na mesma linha. */
    private val SideTop = 22.dp

    /** A altura da barra do título, a mesma da fresta com o facho no alto do menu. */
    private val SideTitleHeight = 30.dp

    /** O conteúdo ao lado do menu lateral: a margem da direita é a mesma das abas em cima. */
    private val sideScreenPadding = PaddingValues(start = 40.dp, end = 48.dp, top = SideTop)

    /**
     * O menu lateral, como o trilho do tablet deitado: a fresta com o facho em cima, os ícones das abas
     * e os Ajustes no pé, com um fio separando do conteúdo. Como nas abas de cima, a aba muda com o
     * foco, e a aberta tem a fresta de luz ao lado. O foco é um círculo na cor de destaque.
     * [itemModifier] vai em cada item (o foco da aba aberta e o OK que recarrega). [tabs] são as
     * abas do usuário aberto, na ordem de [labels] (o convidado não tem os Ajustes nem as outras abas).
     */
    @Composable
    private fun SideMenu(
        tab: HomeTab,
        onTab: (HomeTab) -> Unit,
        tabs: List<HomeTab>,
        labels: List<String>,
        itemModifier: (HomeTab) -> Modifier,
        modifier: Modifier,
    ) {
        Box(Modifier.fillMaxHeight().width(SideMenuWidth)) {
            Column(
                modifier.fillMaxSize().padding(top = SideTop, bottom = SideTop),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BeamGlyph(Modifier.size(width = 40.dp, height = SideTitleHeight))
                Spacer(Modifier.height(SideFirstItemGap))
                sideTabs.filter { it.first in tabs }.forEach { (item, icon) ->
                    SideItem(item, icon, labels[tabs.indexOf(item)], item == tab, onTab, itemModifier(item))
                }
                Spacer(Modifier.weight(1f))
                if (HomeTab.SETTINGS in tabs) {
                    SideItem(HomeTab.SETTINGS, Icons.Outlined.Tune, labels[tabs.indexOf(HomeTab.SETTINGS)], tab == HomeTab.SETTINGS, onTab, itemModifier(HomeTab.SETTINGS))
                }
            }
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(1.dp).background(Look.Hairline))
        }
    }

    @Composable
    private fun SideItem(item: HomeTab, icon: ImageVector, label: String, selected: Boolean, onTab: (HomeTab) -> Unit, modifier: Modifier) {
        Box(Modifier.fillMaxWidth().height(SideItemHeight), contentAlignment = Alignment.Center) {
            if (selected) LightSlit(Modifier.align(Alignment.CenterStart).padding(start = 9.dp).size(3.dp, 20.dp))
            Surface(
                onClick = { onTab(item) },
                modifier = modifier.size(SideFocusSize).onFocusChanged { if (it.isFocused) onTab(item) },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = if (selected) Look.Text else Look.Sub,
                    focusedContainerColor = Look.GlowSoft,
                    focusedContentColor = Look.OnGlow,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, modifier = Modifier.size(SideIconSize))
                }
            }
        }
    }

    /**
     * O nome da aba aberta em cima do conteúdo, no menu lateral (como a barra de cima do tablet), com
     * [trailing] (a foto do usuário) no canto direito.
     */
    @Composable
    private fun TitleBar(title: String, trailing: @Composable () -> Unit) {
        Row(Modifier.fillMaxWidth().height(SideTitleHeight), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = (-0.2).sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
    }

    /**
     * O nome de uma aba, nos dois pesos do logotipo: a escolhida em bold, as outras em light. A
     * largura é sempre a do bold (desenhado invisível por baixo), para as abas não andarem de lado
     * quando a escolhida muda.
     */
    @Composable
    private fun TabLabel(text: String, selected: Boolean, size: TextUnit) {
        val style = MaterialTheme.typography.titleMedium.copy(fontSize = size)
        Box(Modifier.padding(horizontal = TabLabelPadding, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(text, style = style.copy(fontWeight = FontWeight.Bold), color = Color.Transparent, maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
            Text(text, style = style.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Light), maxLines = 1)
        }
    }

    /** O espaço de cada lado do nome de uma aba. */
    private val TabLabelPadding = 12.dp

    /** O espaço entre uma aba e a outra, além do [TabLabelPadding] de cada uma. */
    private val TabSeparator = 2.dp

    /** O espaço mínimo entre o logotipo e o nome da primeira aba. */
    private val LogoGap = 40.dp

    /** O espaço entre o nome da última aba e a foto do usuário. */
    private val TrailingGap = 24.dp

    /**
     * A altura da letra do logotipo, a mesma das abas no tamanho normal. Ela não cresce com a letra
     * maior da Acessibilidade ([toFixedSp]), para sobrar espaço para os nomes das abas.
     */
    private val LogoSize = 16.dp

    @Composable
    private fun Dp.toFixedSp(): TextUnit = with(LocalDensity.current) { this@toFixedSp.toSp() }

    /**
     * O logotipo à esquerda e as abas encostadas à direita, sempre numa linha só (pedido do Johan,
     * 2026-11-13). Quando os nomes não cabem (num idioma de palavras mais longas, ou com a letra maior
     * da Acessibilidade), a letra das abas diminui até caberem todas, com pelo menos [LogoGap] entre o
     * logotipo e o nome da primeira. As abas passam [TabLabelPadding] da borda direita, para o nome da
     * última terminar na linha dos cards. [tabs] recebe o tamanho da letra dos nomes.
     */
    @Composable
    private fun LogoAndTabs(
        labels: List<String>,
        logo: @Composable () -> Unit,
        trailing: @Composable () -> Unit,
        tabs: @Composable (TextUnit) -> Unit,
    ) {
        val measurer = rememberTextMeasurer(cacheSize = 16)
        // O nome escolhido fica em bold, e cada aba já tem a largura do bold ([TabLabel]).
        val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        // O tamanho que coube em cada largura, para não medir os nomes de novo a cada foco.
        val fitted = remember(labels, style, LocalDensity.current) { HashMap<Int, TextUnit>() }
        SubcomposeLayout { constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val logoPlaceables = subcompose("logo", logo).map { it.measure(loose) }
            val logoWidth = logoPlaceables.maxOfOrNull { it.width } ?: 0
            val trailingPlaceables = subcompose("trailing", trailing).map { it.measure(loose) }
            val trailingWidth = trailingPlaceables.maxOfOrNull { it.width } ?: 0
            // A foto encosta na borda direita dos cards, a [TrailingGap] do nome da última aba.
            val trailingRoom = if (trailingWidth > 0) trailingWidth + TrailingGap.roundToPx() else 0
            val padding = TabLabelPadding.roundToPx()
            // Cabe quando o começo do primeiro nome fica a [LogoGap] do logotipo.
            val room = constraints.maxWidth - logoWidth - LogoGap.roundToPx() - trailingRoom -
                (labels.size * 2 - 2) * padding - (labels.size - 1).coerceAtLeast(0) * TabSeparator.roundToPx()
            val size = fitted.getOrPut(room) {
                fun width(size: TextUnit) =
                    labels.sumOf { measurer.measure(it, style.copy(fontSize = size), maxLines = 1, softWrap = false).size.width }
                // Diminui de pouco em pouco: com a letra maior da Acessibilidade (Android 14+), a
                // letra pequena cresce mais que a grande, e a largura não cai na mesma proporção.
                var size = style.fontSize
                var width = width(size)
                var tries = 0
                while (width > room && size.value > style.fontSize.value * 0.4f && tries++ < 8) {
                    size = (size.value * room / width * 0.98f).coerceAtLeast(style.fontSize.value * 0.4f).sp
                    width = width(size)
                }
                size
            }
            val tabPlaceables = subcompose("tabs") { tabs(size) }.map { it.measure(loose) }
            val tabsWidth = tabPlaceables.maxOfOrNull { it.width } ?: 0
            val height = maxOf(
                logoPlaceables.maxOfOrNull { it.height } ?: 0,
                tabPlaceables.maxOfOrNull { it.height } ?: 0,
                trailingPlaceables.maxOfOrNull { it.height } ?: 0,
            )
            layout(constraints.maxWidth, height) {
                logoPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
                tabPlaceables.forEach { it.placeRelative(constraints.maxWidth - trailingRoom - tabsWidth + padding, (height - it.height) / 2) }
                trailingPlaceables.forEach { it.placeRelative(constraints.maxWidth - it.width, (height - it.height) / 2) }
            }
        }
    }

    /**
     * A foto do usuário aberto, no fim da linha das abas ou no canto da barra do título (pedido do
     * Johan, 2026-11-14). O foco não troca nada; OK abre o menu de troca ([UserMenu]). [modifier]
     * diz como o foco chega e sai dela ([photoFocus]).
     */
    @Composable
    internal fun UserButton(modifier: Modifier = Modifier) {
        val user by Users.current.collectAsStateWithLifecycle()
        var open by remember { mutableStateOf(false) }
        val label = stringResource(R.string.user_menu)
        Box(modifier) {
            Card(
                onClick = { open = true },
                shape = CardDefaults.shape(CircleShape),
                colors = CardDefaults.colors(containerColor = Look.Surface),
                scale = CardDefaults.scale(focusedScale = 1.12f),
                border = CardDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Look.GlowSoft), shape = CircleShape)),
                // Do tamanho dela mesmo na barra do título do menu lateral, um pouco mais baixa.
                modifier = Modifier.requiredSize(UserPhotoSize).semantics { contentDescription = label },
            ) {
                UserPhoto(user, UserPhotoSize)
            }
        }
        if (open) UserMenu(user, onDismiss = { open = false })
    }

    /** O tamanho da foto do usuário na linha das abas. */
    private val UserPhotoSize = 34.dp

    /**
     * O menu da foto: o usuário aberto em cima (a foto e o nome), as outras contas, o convidado e
     * "Add account". OK num usuário troca na hora; Voltar fecha.
     */
    @Composable
    private fun UserMenu(user: User?, onDismiss: () -> Unit) {
        val accounts by Users.accounts.collectAsStateWithLifecycle()
        val addAccount = LocalAddAccount.current
        val others = accounts.filter { it.slot != user?.slot } + listOfNotNull(Users.guestUser.takeIf { user?.guest != true })
        val first = remember { FocusRequester() }
        val dim = sleepDim()
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(440.dp).sleepDim(dim, RoundedCornerShape(16.dp))) {
                Column(Modifier.padding(vertical = 20.dp, horizontal = 16.dp)) {
                    Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserPhoto(user, 48.dp)
                        Text(
                            userName(user),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    others.forEachIndexed { index, other ->
                        SettingsRow(
                            onClick = {
                                onDismiss()
                                AppScope.launch { Users.switchTo(other) }
                            },
                            headlineContent = { Text(userName(other), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = if (other.guest) ({ Text(stringResource(R.string.user_guest_desc)) }) else null,
                            leadingContent = { UserPhoto(other, 32.dp) },
                            modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                        )
                    }
                    SettingsRow(
                        onClick = {
                            onDismiss()
                            addAccount()
                        },
                        headlineContent = { Text("+ " + stringResource(R.string.user_add), color = Look.Glow) },
                        modifier = if (others.isEmpty()) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
            // Dentro do diálogo: a janela dele já existe quando o foco é pedido.
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        }
    }

    /** O nome de um usuário no menu e nos Ajustes ("Guest" no convidado). */
    @Composable
    private fun userName(user: User?): String =
        if (user == null || user.guest) stringResource(R.string.user_guest) else user.name.orEmpty().ifBlank { "?" }

    /** A foto de um usuário: a da conta (sem foto, a inicial num círculo), ou um boneco no convidado. */
    @Composable
    private fun UserPhoto(user: User?, size: Dp) {
        val name = user?.name.orEmpty()
        Box(Modifier.size(size).clip(CircleShape).background(if (user == null || user.guest) Look.SurfaceHigh else Look.monogramTone(name)), contentAlignment = Alignment.Center) {
            when {
                user == null || user.guest -> Icon(Icons.Outlined.Person, contentDescription = null, tint = Look.Sub, modifier = Modifier.size(size * 0.62f))
                user.photo != null -> AsyncImage(model = user.photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Text(monogramOf(name), style = MaterialTheme.typography.labelLarge.copy(fontSize = (size.value * 0.45f).sp), color = Look.Text)
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
        // Com a aba na tela, a lista nova entra abaixo do que já foi visto ([RecommendedViewModel.onScreen]).
        DisposableEffect(vm) {
            vm.onScreen(true)
            onDispose { vm.onScreen(false) }
        }
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, stringResource(R.string.recommended_empty), vm::loadMore, onPlay, onOpenChannel, takeFocus = false, onFocusTaken = {}, onSeen = vm::seen)
        }
    }

    @Composable
    override fun Explore(vm: ExploreViewModel, onPlay: (Video) -> Unit, onOpenChannel: (Channel) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val filters by vm.filters.collectAsStateWithLifecycle()
        val showIntro by vm.showIntro.collectAsStateWithLifecycle()
        // A aba abriu de verdade (ela carrega por trás antes disso): o próximo sorteio pode ser preparado.
        LaunchedEffect(Unit) { vm.seen() }
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
        val dim = sleepDim()
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(560.dp).sleepDim(dim, RoundedCornerShape(16.dp))) {
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
                // A largura toda do conteúdo (1.0.8, pedido do Johan; até a 1.0.7, 60%).
                modifier = Modifier.fillMaxWidth(),
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
        val dim = sleepDim()
        Popup(
            popupPositionProvider = remember(gap) { BelowAnchor(gap) },
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            LazyColumn(
                state = list,
                modifier = Modifier.width(300.dp).heightIn(max = 420.dp)
                    .sleepDim(dim, RoundedCornerShape(22.dp))
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
            modifier = Modifier.focusRestorer(),
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
            modifier = Modifier.focusRestorer(),
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
    override fun Settings(
        vm: SettingsViewModel,
        onOpenPlaylists: () -> Unit,
        onOpenTopics: () -> Unit,
        onOpenTopicLists: () -> Unit,
        onAddAccount: () -> Unit,
    ) {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val user by vm.user.collectAsStateWithLifecycle()
        val accounts by vm.accounts.collectAsStateWithLifecycle()
        val ownPlaylists by vm.ownPlaylists.collectAsStateWithLifecycle()
        val appIcon by vm.appIcon.collectAsStateWithLifecycle()
        var choosingTarget by rememberSaveable { mutableStateOf(false) }
        val context = LocalContext.current
        val activity = context.findActivity()
        val currentLanguage = remember { activity?.let(AppLanguage::current) ?: Language.SYSTEM }
        // Lido de novo a cada volta para o app: a pessoa liga o serviço nos ajustes do Android e o
        // botão é aprendido com o BeamTube fora da tela.
        var remoteButton by remember { mutableStateOf(RemoteButtonState.of(context)) }
        // Nas TCL, sem o autoinício, o botão some depois de cada atualização ou reinício até o app abrir.
        var autoStartBlocked by remember { mutableStateOf(ScreenOffService.autoStartBlocked(context)) }
        LifecycleResumeEffect(Unit) {
            remoteButton = RemoteButtonState.of(context)
            autoStartBlocked = ScreenOffService.autoStartBlocked(context)
            onPauseOrDispose { }
        }

        // Em cartões, como no celular (2026-11-03): o nome do grupo em cima, as linhas num cartão e a
        // explicação embaixo. O foco continua o do ListItem (a linha fica creme). Os cartões ocupam a
        // largura toda, como a grade dos vídeos (1.0, pedido do Johan: com 70% da largura, à
        // esquerda, sobrava um buraco no lado direito).
        LazyColumn(
            modifier = Modifier.fillMaxWidth().focusRestorer(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            // Os usuários (pedido do Johan, 2026-11-14): o aberto primeiro; OK em outro troca na hora.
            // O Sign out continua no fim dos Ajustes, longe do foco que chega aqui em cima.
            group(
                "users",
                R.string.users_title,
                hint = R.string.users_hint,
                rows = buildList<Pair<Any, @Composable () -> Unit>> {
                    add(
                        row("current") {
                            SettingsRow(
                                onClick = {},
                                headlineContent = { Text(userName(user), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(stringResource(R.string.settings_account)) },
                                leadingContent = { UserPhoto(user, 32.dp) },
                                trailingContent = { RadioButton(selected = true, onClick = null) },
                            )
                        },
                    )
                    (accounts.filter { it.slot != user?.slot } + Users.guestUser).forEach { other ->
                        add(
                            row("user:${other.slot}") {
                                SettingsRow(
                                    onClick = { vm.switchTo(other) },
                                    headlineContent = { Text(userName(other), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = if (other.guest) ({ Text(stringResource(R.string.user_guest_desc)) }) else null,
                                    leadingContent = { UserPhoto(other, 32.dp) },
                                    trailingContent = { RadioButton(selected = false, onClick = null) },
                                )
                            },
                        )
                    }
                    add(
                        row("add") {
                            SettingsRow(
                                onClick = onAddAccount,
                                headlineContent = { Text("+ " + stringResource(R.string.user_add), color = Look.Glow) },
                            )
                        },
                    )
                },
            )

            group(
                "lives",
                R.string.settings_subscriptions,
                rows = listOf(
                    row("show") {
                        SettingsRow(
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
                        SettingsRow(
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
                                SettingsRow(
                                    onClick = vm::loadOwnPlaylists,
                                    headlineContent = { Text(stringResource(R.string.error_generic, own.message)) },
                                    trailingContent = { Text(stringResource(R.string.retry)) },
                                )
                            },
                        )
                        is UiState.Ready -> own.data.forEach { playlist ->
                            add(
                                row(playlist.id) {
                                    SettingsRow(
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

            // O fim do vídeo (1.0, pedido do Johan): sugestões, voltar, tocar a próxima ou ficar parado.
            group(
                "end",
                R.string.settings_end,
                hint = R.string.settings_end_hint,
                rows = endActionOptions.map { (action, label) ->
                    row(action) {
                        SettingsRow(
                            onClick = { vm.setEndAction(action) },
                            headlineContent = { Text(stringResource(label)) },
                            trailingContent = { RadioButton(selected = settings.endAction == action, onClick = null) },
                        )
                    }
                },
            )

            // O idioma da legenda (pedido do Johan depois da 1.0.9): o botão do player só liga e desliga.
            group(
                "captions",
                R.string.settings_captions,
                hint = R.string.settings_captions_hint,
                rows = captionChoices(context, settings.captionLanguage).map { choice ->
                    row(choice.value) {
                        SettingsRow(
                            onClick = { vm.setCaptionLanguage(choice.value) },
                            headlineContent = { Text(choice.title) },
                            supportingContent = if (choice.subtitle != null) ({ Text(choice.subtitle) }) else null,
                            trailingContent = { RadioButton(selected = choice.selected, onClick = null) },
                        )
                    }
                },
            )

            // Os tópicos um por um ficam numa tela só deles (1.0.8, pedido do Johan: as duas listas
            // no meio dos Ajustes faziam a página crescer a cada tópico).
            group(
                "topics",
                R.string.settings_topics,
                hint = R.string.settings_topics_hint,
                rows = listOf(
                    row("choose") {
                        SettingsRow(
                            onClick = onOpenTopics,
                            headlineContent = { Text(stringResource(R.string.settings_topics_choose)) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        )
                    },
                    row("yours") {
                        SettingsRow(
                            onClick = onOpenTopicLists,
                            headlineContent = { Text(stringResource(R.string.settings_topics_yours)) },
                            supportingContent = { Text(spacedDots(topicCounts(settings.filters.preferred.size, settings.filters.excluded.size))) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        )
                    },
                ),
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
                            SettingsRow(
                                onClick = { vm.unblock(channel.id) },
                                headlineContent = { Text(channel.name.ifBlank { channel.id }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                // O bloqueio vale neste aparelho; esta linha diz se a conta já recebeu o "Don't recommend channel".
                                supportingContent = {
                                    Text(stringResource(if (channel.onAccount) R.string.settings_blocked_account else R.string.settings_blocked_device))
                                },
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
                        SettingsRow(
                            onClick = { vm.setExternalLinks(mode) },
                            headlineContent = { Text(stringResource(label)) },
                            leadingContent = { RadioButton(selected = settings.externalLinks == mode, onClick = null) },
                        )
                    }
                },
            )

            // O botão YouTube do controle (1.0.4, pedido do Johan): desde a 0.6.1 o botão abre o
            // BeamTube, mas nada no app dizia isso nem onde ligar. A linha diz se o serviço de
            // Acessibilidade está ligado e leva aos ajustes do Android, onde ele é ligado.
            group(
                "remote",
                R.string.settings_remote,
                hint = R.string.settings_remote_hint,
                rows = listOf(
                    row("accessibility") {
                        val on = remoteButton != RemoteButtonState.OFF
                        SettingsRow(
                            onClick = {
                                if (!ScreenOffService.openSettings(activity ?: context)) {
                                    Notices.show(context.getString(R.string.settings_remote_no_settings), long = true)
                                }
                            },
                            headlineContent = { Text(stringResource(R.string.settings_remote_open)) },
                            supportingContent = {
                                // Nas TCL (1.0.12): o app não consegue se liberar, então diz o que esperar.
                                val text = stringResource(remoteButton.text)
                                Text(if (on && autoStartBlocked) text + "\n" + stringResource(R.string.settings_remote_auto_start) else text)
                            },
                            trailingContent = {
                                Text(stringResource(if (on) R.string.settings_remote_on else R.string.settings_remote_off))
                            },
                        )
                    },
                ),
            )

            // As playlists inscritas ficam numa tela só delas (1.0.9, pedido do Johan, como os tópicos).
            group(
                "playlists",
                R.string.tab_playlists,
                hint = R.string.settings_playlists_hint,
                rows = listOf(
                    row("subscribed") {
                        SettingsRow(
                            onClick = onOpenPlaylists,
                            headlineContent = { Text(stringResource(R.string.settings_playlists)) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        )
                    },
                ),
            )

            // O layout do início (pedido do Johan, 2026-11-14): as abas em cima, como sempre foi, ou o
            // menu lateral do tablet. Muda na hora, sem sair dos Ajustes.
            group(
                "layout",
                R.string.settings_tv_layout,
                hint = R.string.settings_tv_layout_hint,
                rows = TvLayout.entries.map { layout ->
                    row(layout.name) {
                        SettingsRow(
                            onClick = { TvLayouts.select(layout) },
                            headlineContent = { Text(stringResource(layout.label)) },
                            trailingContent = { RadioButton(selected = TvLayouts.current == layout, onClick = null) },
                        )
                    }
                },
            )

            // O idioma do app (pedido do Johan, 2026-11-13); cada um com o nome na própria língua.
            group(
                "language",
                R.string.settings_language,
                rows = Language.entries.map { language ->
                    row(language.name) {
                        SettingsRow(
                            onClick = { activity?.let { AppLanguage.select(it, language) } },
                            headlineContent = { Text(language.native ?: stringResource(language.label ?: R.string.language_system)) },
                            trailingContent = { RadioButton(selected = currentLanguage == language, onClick = null) },
                        )
                    }
                },
            )

            group(
                "icon",
                R.string.settings_app_icon,
                hint = R.string.settings_app_icon_hint,
                rows = AppIcon.entries.map { icon ->
                    row(icon.name) {
                        SettingsRow(
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
                        SettingsRow(
                            onClick = { Accents.select(option) },
                            headlineContent = { Text(stringResource(option.label)) },
                            leadingContent = { Box(Modifier.size(24.dp).background(Color(option.glow), CircleShape)) },
                            trailingContent = { RadioButton(selected = Look.accent == option, onClick = null) },
                        )
                    }
                },
            )

            // O fundo do app (1.0.8, pedido do Johan): o grafite do celular, o quase preto da TV
            // (o padrão aqui) ou o preto puro das telas OLED. Vale para o aparelho.
            group(
                "background",
                R.string.settings_background,
                hint = R.string.settings_background_hint,
                rows = Background.entries.map { option ->
                    row(option.name) {
                        SettingsRow(
                            onClick = { Backgrounds.select(option) },
                            headlineContent = { Text(stringResource(option.label)) },
                            leadingContent = { BackgroundSwatch(option, 24.dp) },
                            trailingContent = { RadioButton(selected = Backgrounds.current == option, onClick = null) },
                        )
                    }
                },
            )

            group(
                "account",
                R.string.settings_account,
                rows = listOf(
                    row("signout") {
                        SettingsRow(
                            onClick = vm::signOut,
                            headlineContent = { Text(stringResource(R.string.sign_out)) },
                            supportingContent = user?.name?.let { { Text(stringResource(R.string.settings_signed_in_as, it)) } },
                        )
                    },
                ),
            )
        }

    }

    @Composable
    override fun TopicLists(vm: TopicListsViewModel, onBack: () -> Unit) {
        val settings by vm.settings.collectAsStateWithLifecycle()
        var editing by remember { mutableStateOf<Pair<TopicList, Int?>?>(null) }
        val first = remember { FocusRequester() }

        Column(Modifier.fillMaxSize().padding(listScreenPadding)) {
            Text(stringResource(R.string.settings_topics_yours), style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().focusRequester(first),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                topicGroup(
                    key = "preferred",
                    title = R.string.settings_preferred,
                    hint = R.string.settings_preferred_hint,
                    topics = settings.filters.preferred,
                    onEdit = { editing = TopicList.PREFERRED to it },
                    onRemove = { vm.removeTopic(TopicList.PREFERRED, it) },
                )
                topicGroup(
                    key = "excluded",
                    title = R.string.settings_excluded,
                    hint = R.string.settings_excluded_hint,
                    topics = settings.filters.excluded,
                    onEdit = { editing = TopicList.EXCLUDED to it },
                    onRemove = { vm.removeTopic(TopicList.EXCLUDED, it) },
                )
            }
        }
        // O foco entra na lista (na primeira linha que aceita foco) assim que a tela abre.
        LaunchedEffect(Unit) {
            repeat(FOCUS_TRIES) {
                withFrameNanos { }
                if (runCatching { first.requestFocus() }.getOrElse { false } != false) return@LaunchedEffect
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

    @Composable
    override fun SubscribedPlaylists(vm: SubscribedPlaylistsViewModel, onOpen: (Playlist) -> Unit, onBack: () -> Unit) {
        val playlists by vm.playlists.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val list = remember { FocusRequester() }

        val rows = buildList<Pair<Any, @Composable () -> Unit>> {
            error?.let { message ->
                add(row("error") { EmptyRow(stringResource(R.string.action_failed, message), MaterialTheme.colorScheme.error) })
            }
            when (val p = playlists) {
                UiState.Loading -> add(row("loading") { EmptyRow(stringResource(R.string.loading)) })
                is UiState.Error -> add(
                    row("retry") {
                        SettingsRow(
                            onClick = vm::reload,
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
                                    SettingsRow(
                                        onClick = { onOpen(item.playlist) },
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

        Column(Modifier.fillMaxSize().padding(listScreenPadding)) {
            Text(stringResource(R.string.settings_playlists), style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().focusRequester(list),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                group("playlists", title = null, hint = R.string.settings_playlists_hint, rows = rows)
            }
        }
        // O foco entra na lista assim que ela tem uma linha que aceita foco (a lista chega da rede).
        val ready = playlists !is UiState.Loading
        LaunchedEffect(ready) {
            if (!ready) return@LaunchedEffect
            repeat(FOCUS_TRIES) {
                withFrameNanos { }
                if (runCatching { list.requestFocus() }.getOrElse { false } != false) return@LaunchedEffect
            }
        }
    }

    /** O que a linha do botão YouTube do controle diz: o serviço desligado, à espera do botão ou pronto. */
    private enum class RemoteButtonState(val text: Int) {
        OFF(R.string.settings_remote_state_off),
        WAITING(R.string.settings_remote_state_waiting),
        READY(R.string.settings_remote_state_ready);

        companion object {
            fun of(context: Context): RemoteButtonState = when {
                !ScreenOffService.isOn(context) -> OFF
                ScreenOffService.knowsYouTubeButton(context) -> READY
                else -> WAITING
            }
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
                            SettingsRow(
                                onClick = { onEdit(index) },
                                headlineContent = { Text(topic.label()) },
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
                    SettingsRow(
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

    /**
     * Uma linha dos Ajustes: o ListItem da TV, sem crescer com o foco. Crescendo (o padrão, 5%), a
     * linha com foco passava da borda do cartão, e a de baixo era desenhada por cima dela: no meio do
     * grupo a barra creme ficava cortada reto embaixo, e na última linha (ou na única) o canto do
     * cartão aparecia por baixo dos cantos dela (achado do Johan, 2026-11-14). Sem crescer, a barra
     * fica dentro do cartão e é igual em qualquer linha.
     */
    @Composable
    private fun SettingsRow(
        onClick: () -> Unit,
        headlineContent: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        supportingContent: (@Composable () -> Unit)? = null,
        leadingContent: (@Composable BoxScope.() -> Unit)? = null,
        trailingContent: (@Composable () -> Unit)? = null,
    ) = ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = headlineContent,
        modifier = modifier,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        scale = ListItemDefaults.scale(focusedScale = 1f),
    )

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
        val dim = sleepDim()
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(640.dp).sleepDim(dim, RoundedCornerShape(16.dp))) {
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
    override fun Topics(vm: TopicsViewModel, firstRun: Boolean, onDone: () -> Unit) = TvTopics(vm, firstRun, onDone)

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
                    supportingContent = channelNameOf(video)?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
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
        val res = LocalContext.current.resources
        ChannelPage(
            title, state, blocked, subscription,
            onSubscribe = vm::toggleSubscribed,
            onBlock = { vm.toggleBlocked { blockNotice(res, it) } },
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
        var focused by remember { mutableStateOf(false) }
        val label = stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggle,
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
        onSeen: ((Int) -> Unit)? = null,
    ) {
        val firstFocus = remember { FocusRequester() }
        val saved by SavedVideos.ids.collectAsStateWithLifecycle()
        val grid = rememberLazyGridState()
        var inGrid by remember { mutableStateOf(false) }
        val hadFocus = remember { booleanArrayOf(false) }
        var resetFocus by remember { mutableStateOf(false) }
        PrefetchThumbnails(content.videos, grid)
        if (onSeen != null) SeenRows(grid, onSeen)
        // De volta ao começo com vídeos novos: o foco que estava na grade vai para o primeiro deles.
        // Com o foco fora (nas abas ou no menu lateral), o focusRestorer ainda lembraria o card
        // antigo (agora fileiras abaixo), e entrar na grade rolaria até ele: [resetFocus] faz a
        // próxima entrada cair no primeiro vídeo.
        ListTop(
            grid = grid,
            content = content,
            onStartTop = { hadFocus[0] = inGrid },
        ) {
            val focused = hadFocus[0] || inGrid
            hadFocus[0] = false
            if (!focused) {
                resetFocus = true
                return@ListTop
            }
            // O primeiro card só existe depois que a grade mede a lista nova.
            repeat(FOCUS_TRIES) {
                withFrameNanos { }
                if (runCatching { firstFocus.requestFocus() }.getOrElse { false } != false) return@ListTop
            }
            resetFocus = true
        }
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(GridGapX),
            verticalArrangement = Arrangement.spacedBy(GridGapY),
            contentPadding = PaddingValues(bottom = 48.dp, top = GridTop),
            // Ir ao menu (ou às abas) e voltar devolve o foco a este card (1.0.9, [Home]);
            // depois de subir ao topo com o foco fora, [resetFocus] ignora o card antigo.
            modifier = Modifier
                .onFocusChanged {
                    inGrid = it.hasFocus
                    if (it.hasFocus) resetFocus = false
                }
                .then(
                    if (resetFocus) {
                        Modifier.focusProperties {
                            onEnter = { if (content.videos.isNotEmpty()) runCatching { firstFocus.requestFocus() } }
                        }
                    } else {
                        Modifier.focusRestorer()
                    },
                ),
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
                ) { UpNext.opened(content.videos, video); onPlay(video) }
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
        // O convidado não tem conta: nem playlist do Save, nem marcador.
        @Suppress("NAME_SHADOWING")
        val showSaved = showSaved && !Users.isGuest
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
                        channelName = channelNameOf(video),
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
        val dim = sleepDim()
        Popup(
            popupPositionProvider = remember(gap) { BesideAnchor(gap) },
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = focusable),
        ) {
            Column(
                Modifier.width(320.dp)
                    .sleepDim(dim, RoundedCornerShape(22.dp))
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
        val name = channelNameOf(video)
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
