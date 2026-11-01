package ytslim.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.AuthRepository
import ytslim.data.Channel
import ytslim.data.Playlist
import ytslim.data.SearchFilters
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
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AuthRepository.activateUrl(s.code))))
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

    /** As abas da barra de baixo. A Busca abre pela lupa da barra de cima. */
    private val tabs = listOf(
        TabInfo(HomeTab.SUBSCRIPTIONS, R.string.tab_subscriptions, Icons.Filled.Subscriptions),
        TabInfo(HomeTab.RECOMMENDED, R.string.tab_recommended, Icons.Filled.Recommend),
        TabInfo(HomeTab.PLAYLISTS, R.string.tab_playlists, Icons.AutoMirrored.Filled.PlaylistPlay),
        TabInfo(HomeTab.HISTORY, R.string.tab_history, Icons.Filled.History),
        TabInfo(HomeTab.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
    )

    @Composable
    override fun Home(tab: HomeTab, onTab: (HomeTab) -> Unit, content: @Composable (HomeTab) -> Unit) {
        // Voltar, na Busca, leva de volta à aba de onde ela foi aberta.
        var lastTab by rememberSaveable { mutableStateOf(HomeTab.SUBSCRIPTIONS) }
        LaunchedEffect(tab) { if (tab != HomeTab.SEARCH) lastTab = tab }
        BackHandler(enabled = tab == HomeTab.SEARCH) { onTab(lastTab) }

        Scaffold(
            topBar = {
                if (tab == HomeTab.SEARCH) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.tab_search)) },
                        navigationIcon = { BackButton { onTab(lastTab) } },
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(tabs.first { it.tab == tab }.label)) },
                        actions = {
                            IconButton(onClick = { onTab(HomeTab.SEARCH) }) {
                                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.tab_search))
                            }
                        },
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEach { info ->
                        NavigationBarItem(
                            selected = info.tab == tab,
                            onClick = { onTab(info.tab) },
                            icon = { Icon(info.icon, contentDescription = null) },
                            label = { Text(stringResource(info.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
    override fun Recommended(vm: RecommendedViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.recommended_empty), onNearEnd = vm::loadMore, onPlay = onPlay)
        }
    }

    @Composable
    override fun History(vm: HistoryViewModel, onPlay: (Video) -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Loadable(state, onRetry = vm::reload) { content ->
            VideoGrid(content, emptyText = stringResource(R.string.history_empty), onNearEnd = vm::loadMore, onPlay = onPlay)
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

        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    vm.onQueryChange(it)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .focusRequester(focus)
                    .onFocusChanged { fieldFocused = it.isFocused },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(text) }),
            )
            FilterRow(filters, onChange = vm::setFilters)
            if (fieldFocused && suggestions.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                            modifier = Modifier.clickable { submit(suggestion) },
                        )
                    }
                }
            } else {
                results?.let { state ->
                    Loadable(state, onRetry = { vm.search() }) { data ->
                        SearchResults(data, onNearEnd = vm::loadMore, onPlay, onOpenChannel, onOpenPlaylist)
                    }
                }
            }
        }
    }

    @Composable
    private fun FilterRow(filters: SearchFilters, onChange: (SearchFilters) -> Unit) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
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
                        )
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            choice.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.label)) },
                                    leadingIcon = { if (option.selected) Icon(Icons.Filled.Check, contentDescription = null) },
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
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (data.items.isEmpty() && !data.hasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.search_empty), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                }
            }
            items(
                data.items,
                key = { it.key },
                span = { if (it is SearchItem.ChannelItem) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { item ->
                when (item) {
                    is SearchItem.VideoItem -> VideoCard(item.video, onClick = { onPlay(item.video) })
                    is SearchItem.ChannelItem -> ChannelRow(item.channel, onClick = { onOpenChannel(item.channel) })
                    is SearchItem.PlaylistItem -> PlaylistRow(item.playlist, onClick = { onOpenPlaylist(item.playlist) })
                }
            }
            if (data.loadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() }
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
                item { Text(stringResource(R.string.settings_blocked_empty), Modifier.padding(16.dp)) }
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
        if (topics.isEmpty()) item { Text(stringResource(R.string.settings_topics_empty), Modifier.padding(16.dp)) }
        itemsIndexed(topics) { index, topic ->
            ListItem(
                headlineContent = { Text(topic.name) },
                supportingContent = { Text(topic.keywords.joinToString(", "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                trailingContent = {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.topic_remove))
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
    override fun PlaylistPage(title: String, vm: PlaylistViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        val subscription by vm.subscription.collectAsStateWithLifecycle()
        val reorder by vm.reorder.collectAsStateWithLifecycle()
        val loadedTitle = (state as? UiState.Ready)?.data?.title
        val shownTitle = title.ifBlank { loadedTitle ?: stringResource(R.string.playlist_default_title) }
        val editing = reorder as? Reorder.Editing
        BackHandler(enabled = editing != null) { vm.cancelReorder() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(shownTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
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
                                    Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.reorder))
                                }
                            is Reorder.Editing -> TextButton(onClick = vm::saveOrder, enabled = !r.saving) {
                                Text(stringResource(if (r.saving) R.string.reorder_saving else R.string.reorder_save))
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (editing != null) {
                    ReorderList(editing, onMove = vm::move)
                } else {
                    Loadable(state, onRetry = vm::reload) { content ->
                        val shown = subscription as? PlaylistViewModel.Subscription.Shown
                        val loadFailed = (reorder as? Reorder.Available)?.loadFailed == true
                        VideoGrid(
                            content = content,
                            emptyText = stringResource(R.string.list_empty),
                            onNearEnd = vm::loadMore,
                            onPlay = onPlay,
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
    }

    @Composable
    private fun ReorderList(editing: Reorder.Editing, onMove: (Int, Int) -> Unit) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
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
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Thumbnail(video.thumbnailUrl, Modifier.width(104.dp))
                    Text(
                        video.title,
                        style = MaterialTheme.typography.bodyMedium,
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(title.ifBlank { stringResource(R.string.channel_default_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = { BackButton(onBack) },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = vm::toggleBlocked) {
                        Text(stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block))
                    }
                }
                if (blocked) {
                    Hint(stringResource(R.string.channel_blocked_hint))
                }
                Loadable(state, onRetry = vm::reload) { content ->
                    Column(Modifier.fillMaxSize()) {
                        if (content.playlists.isNotEmpty()) {
                            PrimaryTabRow(selectedTabIndex = tab) {
                                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.channel_videos)) })
                                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.channel_playlists)) })
                            }
                        }
                        if (tab == 1 && content.playlists.isNotEmpty()) {
                            PlaylistGrid(content.playlists, onOpenPlaylist)
                        } else {
                            VideoGrid(content.videos, emptyText = stringResource(R.string.list_empty), onNearEnd = vm::loadMore, onPlay = onPlay)
                        }
                    }
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

    @Composable
    private fun VideoGrid(
        content: VideoListContent,
        emptyText: String,
        onNearEnd: () -> Unit,
        onPlay: (Video) -> Unit,
        header: (@Composable () -> Unit)? = null,
    ) {
        val grid = rememberLazyGridState()
        NearEnd(grid, content.videos.size, onNearEnd)
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
                val badge = if (video.isLive) stringResource(R.string.search_live) else video.durationMs.takeIf { it > 0 }?.let(::formatDuration)
                if (badge != null) {
                    Text(
                        badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .background(if (video.isLive) Color(0xCCCC0000) else Color(0xCC000000), RoundedCornerShape(4.dp))
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
    private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = channel.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(channel.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                channel.info?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }

    @Composable
    private fun BackButton(onBack: () -> Unit) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
    private fun Hint(text: String, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
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
