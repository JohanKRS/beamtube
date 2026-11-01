package ytslim.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import ytslim.R
import ytslim.data.Playlist
import ytslim.data.Video
import ytslim.ui.FeedViewModel
import ytslim.ui.HomeTab
import ytslim.ui.PlaylistViewModel
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.Screens
import ytslim.ui.SettingsViewModel
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListContent
import ytslim.ui.externalLinkOptions
import ytslim.ui.formatDuration
import ytslim.ui.subtitleOf

/**
 * Na abertura do app o foco vai para o primeiro vídeo das Inscrições. Depois disso, as abas não
 * puxam o foco: andar pelas abas com o D-pad troca o conteúdo sem tirar o foco das abas.
 */
private val LocalInitialFocus = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/** Interface da Android TV: D-pad, foco visível e cards maiores (Compose for TV). */
object TvUi : Screens {
    private val screenPadding = PaddingValues(horizontal = 48.dp, vertical = 27.dp)

    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) { content() }
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
                is SignInViewModel.State.ShowCode -> {
                    Text(stringResource(R.string.sign_in_tv_open), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sign_in_tv_url), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.sign_in_tv_enter), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(s.code, fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.sign_in_waits), style = MaterialTheme.typography.bodyMedium)
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
        HomeTab.PLAYLISTS to R.string.tab_playlists,
        HomeTab.SETTINGS to R.string.tab_settings,
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val initialFocus = rememberSaveable { mutableStateOf(true) }
        var tabsFocused by remember { mutableStateOf(false) }
        val selectedTab = remember { FocusRequester() }
        // Voltar, de dentro do conteúdo, sobe para as abas; nas abas, sai do app.
        BackHandler(enabled = !tabsFocused) { runCatching { selectedTab.requestFocus() } }

        Column(Modifier.fillMaxSize().padding(screenPadding)) {
            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == tab },
                modifier = Modifier.onFocusChanged { tabsFocused = it.hasFocus },
            ) {
                tabs.forEach { (item, label) ->
                    Tab(
                        selected = item == tab,
                        onFocus = { onTab(item) },
                        onClick = { onTab(item) },
                        modifier = if (item == tab) Modifier.focusRequester(selectedTab) else Modifier,
                    ) {
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            CompositionLocalProvider(LocalInitialFocus provides initialFocus) {
                Box(Modifier.fillMaxSize()) { content(tab) }
            }
        }
    }

    @Composable
    override fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val initialFocus = LocalInitialFocus.current
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(
                content = content,
                emptyText = stringResource(R.string.feed_empty),
                onNearEnd = vm::loadMore,
                onPlay = onPlay,
                takeFocus = initialFocus.value,
                onFocusTaken = { initialFocus.value = false },
            )
        }
    }

    @Composable
    override fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { playlists ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
    }

    @Composable
    override fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit) {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val playlists by vm.playlists.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()

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
            item {
                Text(
                    stringResource(R.string.settings_playlists_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
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
    }

    @Composable
    override fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val subscription by vm.subscription.collectAsStateWithLifecycle()
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }

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
                    if (shown.subscribed) OutlinedButton(onClick = toggle, enabled = !shown.busy) { Text(label) }
                    else Button(onClick = toggle, enabled = !shown.busy) { Text(label) }
                }
            }
            (subscription as? PlaylistViewModel.Subscription.Shown)?.let { shown ->
                Text(
                    shown.error?.let { stringResource(R.string.action_failed, it) } ?: stringResource(R.string.playlist_subscribe_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (shown.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Loadable(state, onRetry = vm::reload) { content ->
                VideoGrid(
                    content = content,
                    emptyText = stringResource(R.string.list_empty),
                    onNearEnd = vm::loadMore,
                    onPlay = onPlay,
                    takeFocus = true,
                    onFocusTaken = {},
                )
            }
        }
    }

    @Composable
    private fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        takeFocus: Boolean,
        onFocusTaken: () -> Unit,
    ) {
        val firstFocus = remember { FocusRequester() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
        ) {
            if (content.videos.isEmpty() && !content.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(24.dp))
                }
            }
            itemsIndexed(content.videos, key = { _, v -> v.id + v.playlistIndex }) { index, video ->
                if (index >= content.videos.size - 8) LaunchedEffect(Unit) { onNearEnd() }
                MediaCard(
                    title = video.title,
                    subtitle = subtitleOf(video),
                    imageUrl = video.thumbnailUrl,
                    badge = video.durationMs.takeIf { it > 0 }?.let(::formatDuration),
                    onClick = { onPlay(video) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
        LaunchedEffect(content.videos.isNotEmpty()) {
            if (takeFocus && content.videos.isNotEmpty()) {
                runCatching { firstFocus.requestFocus() }
                onFocusTaken()
            }
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
            Card(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    if (badge != null) {
                        Text(
                            badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
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
