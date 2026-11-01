package ytslim.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.AuthRepository
import ytslim.data.Playlist
import ytslim.data.Video
import ytslim.ui.AppIconPreview
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

/** Interface de toque, para celular e tablet. */
@OptIn(ExperimentalMaterial3Api::class)
object MobileUi : Screens {

    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }

    @Composable
    override fun SignIn(vm: SignInViewModel, onSignedIn: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        LaunchedEffect(state) { if (state == SignInViewModel.State.SignedIn) onSignedIn() }

        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
                when (val s = state) {
                    SignInViewModel.State.Requesting, SignInViewModel.State.SignedIn -> CircularProgressIndicator()
                    is SignInViewModel.State.ShowCode -> {
                        Text(
                            stringResource(R.string.sign_in_instructions_touch),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            s.code,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(s.code)) },
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(s.code))
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AuthRepository.ACTIVATE_URL)))
                        }) { Text(stringResource(R.string.sign_in_open_site)) }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.sign_in_waits), style = MaterialTheme.typography.bodySmall)
                    }
                    is SignInViewModel.State.Error -> {
                        Text(stringResource(R.string.sign_in_error, s.message), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = vm::requestCode) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        }
    }

    private data class TabInfo(val tab: HomeTab, val label: Int, val icon: ImageVector)

    private val tabs = listOf(
        TabInfo(HomeTab.SUBSCRIPTIONS, R.string.tab_subscriptions, Icons.Filled.Subscriptions),
        TabInfo(HomeTab.PLAYLISTS, R.string.tab_playlists, Icons.AutoMirrored.Filled.PlaylistPlay),
        TabInfo(HomeTab.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        val current = tabs.first { it.tab == tab }
        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(current.label)) }) },
            bottomBar = {
                NavigationBar {
                    tabs.forEach { info ->
                        NavigationBarItem(
                            selected = info.tab == tab,
                            onClick = { onTab(info.tab) },
                            icon = { Icon(info.icon, contentDescription = null) },
                            label = { Text(stringResource(info.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) { content(tab) }
        }
    }

    @Composable
    override fun Feed(vm: FeedViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.feed_empty), onNearEnd = vm::loadMore, onPlay = onPlay)
        }
    }

    @Composable
    override fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { playlists ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(playlists, key = { it.browseId }) { playlist ->
                    PlaylistRow(playlist, onClick = { onOpen(playlist) })
                }
            }
        }
    }

    @Composable
    override fun Settings(vm: SettingsViewModel, onOpenPlaylist: (Playlist) -> Unit, onSignedOut: () -> Unit) {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val playlists by vm.playlists.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val appIcon by vm.appIcon.collectAsStateWithLifecycle()

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle(stringResource(R.string.settings_subscriptions)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_show_lives)) },
                    supportingContent = { Text(stringResource(R.string.settings_show_lives_desc)) },
                    trailingContent = { Switch(checked = settings.showLives, onCheckedChange = null) },
                    modifier = Modifier.clickable(role = Role.Switch) { vm.setShowLives(!settings.showLives) },
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
            item {
                Text(
                    stringResource(R.string.settings_playlists_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
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
                UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() }
                }
                is UiState.Error -> item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.error_generic, p.message), Modifier.weight(1f))
                        TextButton(onClick = vm::reloadPlaylists) { Text(stringResource(R.string.retry)) }
                    }
                }
                is UiState.Ready -> {
                    if (p.data.isEmpty()) {
                        item {
                            Text(stringResource(R.string.settings_playlists_empty), Modifier.padding(16.dp))
                        }
                    }
                    items(p.data, key = { it.playlist.browseId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.playlist.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = item.playlist.info?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                            leadingContent = { Thumbnail(item.playlist.thumbnailUrl, Modifier.width(96.dp)) },
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
            item {
                Text(
                    stringResource(R.string.settings_app_icon_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
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
    }

    @Composable
    override fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val subscription by vm.subscription.collectAsStateWithLifecycle()
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(shownTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Loadable(state, onRetry = vm::reload) { content ->
                    VideoGrid(
                        content = content,
                        emptyText = stringResource(R.string.list_empty),
                        onNearEnd = vm::loadMore,
                        onPlay = onPlay,
                        header = (subscription as? PlaylistViewModel.Subscription.Shown)?.let { shown ->
                            { SubscribeHeader(shown) { vm.toggleSubscription(shownTitle) } }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun SubscribeHeader(state: PlaylistViewModel.Subscription.Shown, onToggle: () -> Unit) {
        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = stringResource(if (state.subscribed) R.string.playlist_subscribed else R.string.playlist_subscribe)
                if (state.subscribed) {
                    OutlinedButton(onClick = onToggle, enabled = !state.busy) { Text(label) }
                } else {
                    Button(onClick = onToggle, enabled = !state.busy) { Text(label) }
                }
                if (state.busy) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            Text(
                stringResource(R.string.playlist_subscribe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            state.error?.let {
                Text(stringResource(R.string.action_failed, it), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    @Composable
    private fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        header: (@Composable () -> Unit)? = null,
    ) {
        val grid = rememberLazyGridState()
        val nearEnd by remember {
            derivedStateOf {
                val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= grid.layoutInfo.totalItemsCount - 6
            }
        }
        LaunchedEffect(nearEnd, content.videos.size) { if (nearEnd) onNearEnd() }
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (content.videos.isEmpty() && !content.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(emptyText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(content.videos, key = { it.id + it.playlistIndex }) { video ->
                VideoCard(video, onClick = { onPlay(video) })
            }
            if (content.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }

    @Composable
    private fun VideoCard(video: Video, onClick: () -> Unit) {
        Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
            Box {
                Thumbnail(video.thumbnailUrl, Modifier.fillMaxWidth())
                if (video.durationMs > 0) {
                    Text(
                        formatDuration(video.durationMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            subtitleOf(video)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Composable
    private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(playlist.thumbnailUrl, Modifier.width(160.dp))
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                playlist.info?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Column {
            HorizontalDivider()
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            )
        }
    }

    @Composable
    private fun Thumbnail(url: String?, modifier: Modifier) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }

    @Composable
    private fun <T> Loadable(state: UiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
        when (state) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.error_generic, state.message), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Ready -> content(state.data)
        }
    }
}
