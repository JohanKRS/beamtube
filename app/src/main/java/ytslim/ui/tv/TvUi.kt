package ytslim.ui.tv

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
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
import kotlinx.coroutines.launch
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.AuthRepository
import ytslim.data.Channel
import ytslim.data.Playlist
import ytslim.data.SearchItem
import ytslim.data.Topic
import ytslim.data.TopicRules
import ytslim.data.Video
import ytslim.ui.AppIconPreview
import ytslim.ui.ChannelViewModel
import ytslim.ui.FeedViewModel
import ytslim.ui.HistoryViewModel
import ytslim.ui.HomeTab
import ytslim.ui.PlaylistViewModel
import ytslim.ui.PlaylistViewModel.Reorder
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.QrCode
import ytslim.ui.RecommendedViewModel
import ytslim.ui.Screens
import ytslim.ui.SearchViewModel
import ytslim.ui.SettingsViewModel
import ytslim.ui.SettingsViewModel.TopicList
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListContent
import ytslim.ui.externalLinkOptions
import ytslim.ui.formatDuration
import ytslim.ui.searchFilterChoices
import ytslim.ui.subtitleOf

/**
 * Na abertura do app o foco vai para o primeiro vídeo das Inscrições. Depois disso, as abas não
 * puxam o foco: andar pelas abas com o D-pad troca o conteúdo sem tirar o foco das abas.
 */
private val LocalInitialFocus = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/** Interface da Android TV: D-pad, foco visível e cards maiores (Compose for TV). */
object TvUi : Screens {
    private val screenPadding = PaddingValues(horizontal = 48.dp, vertical = 27.dp)
    private val liveRed = Color(0xCCCC0000)

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
    override fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, stringResource(R.string.recommended_empty), vm::loadMore, onPlay, takeFocus = false, onFocusTaken = {})
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, stringResource(R.string.history_empty), vm::loadMore, onPlay, takeFocus = false, onFocusTaken = {})
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
            // Cada botão passa para o próximo valor do filtro.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                searchFilterChoices(filters).forEach { choice ->
                    val label = stringResource(R.string.search_filter_value, stringResource(choice.label), stringResource(choice.selected.label))
                    if (choice.isDefault) {
                        OutlinedButton(onClick = { vm.setFilters(choice.next()) }) { Text(label) }
                    } else {
                        Button(onClick = { vm.setFilters(choice.next()) }) { Text(label) }
                    }
                }
            }
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

    @Composable
    private fun SearchResults(
        data: SearchViewModel.Results,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        onOpenChannel: (Channel) -> Unit,
        onOpenPlaylist: (Playlist) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
                    is SearchItem.VideoItem -> VideoCard(item.video) { onPlay(item.video) }
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
    override fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit) {
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
                    if (shown.subscribed) OutlinedButton(onClick = toggle, enabled = !shown.busy) { Text(label) }
                    else Button(onClick = toggle, enabled = !shown.busy) { Text(label) }
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
                        ?: (stringResource(R.string.playlist_subscribe_hint) to false)
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
                Spacer(Modifier.width(24.dp))
                OutlinedButton(onClick = vm::toggleBlocked) {
                    Text(stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block))
                }
            }
            if (blocked) {
                Text(
                    stringResource(R.string.channel_blocked_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
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
                            takeFocus = true,
                            onFocusTaken = {},
                        )
                    }
                }
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
                VideoCard(video, modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier) { onPlay(video) }
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
    private fun VideoCard(video: Video, modifier: Modifier = Modifier, onClick: () -> Unit) {
        MediaCard(
            title = video.title,
            subtitle = subtitleOf(video),
            imageUrl = video.thumbnailUrl,
            badge = if (video.isLive) stringResource(R.string.search_live) else video.durationMs.takeIf { it > 0 }?.let(::formatDuration),
            badgeColor = if (video.isLive) liveRed else Color(0xCC000000),
            onClick = onClick,
            modifier = modifier,
        )
    }

    @Composable
    private fun MediaCard(
        title: String,
        subtitle: String?,
        imageUrl: String?,
        badge: String?,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        badgeColor: Color = Color(0xCC000000),
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
                                .background(badgeColor, RoundedCornerShape(4.dp))
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
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
