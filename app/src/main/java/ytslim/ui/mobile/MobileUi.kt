package ytslim.ui.mobile

import androidx.compose.ui.res.stringResource
import ytslim.R
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ytslim.data.AuthRepository
import ytslim.data.Playlist
import ytslim.data.Video
import ytslim.ui.PlaylistsViewModel
import ytslim.ui.Screens
import ytslim.ui.SignInViewModel
import ytslim.ui.UiState
import ytslim.ui.VideoListViewModel
import ytslim.ui.formatDuration

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

    @Composable
    override fun Playlists(vm: PlaylistsViewModel, onOpen: (Playlist) -> Unit, onSignedOut: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.playlists)) },
                    actions = {
                        IconButton(onClick = { vm.signOut(onSignedOut) }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.sign_out))
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Loadable(state, onRetry = vm::reload) { playlists ->
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(playlists, key = { it.browseId }) { playlist ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpen(playlist) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Thumbnail(playlist.thumbnailUrl, Modifier.width(160.dp))
                                Column(Modifier.padding(horizontal = 12.dp)) {
                                    Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    playlist.info?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun VideoList(title: String, vm: VideoListViewModel, onPlay: (Video) -> Unit, onBack: () -> Unit) {
        val state by vm.state.collectAsStateWithLifecycle()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Loadable(state, onRetry = vm::reload) { content ->
                    val grid = rememberLazyGridState()
                    val nearEnd by remember {
                        derivedStateOf {
                            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            last >= grid.layoutInfo.totalItemsCount - 6
                        }
                    }
                    LaunchedEffect(nearEnd, content.videos.size) { if (nearEnd) vm.loadMore() }
                    LazyVerticalGrid(
                        state = grid,
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(content.videos, key = { it.id + it.playlistIndex }) { video ->
                            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPlay(video) }) {
                                Box {
                                    Thumbnail(video.thumbnailUrl, Modifier.fillMaxWidth())
                                    if (video.durationMs > 0) {
                                        Text(
                                            formatDuration(video.durationMs),
                                            color = androidx.compose.ui.graphics.Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                                .background(androidx.compose.ui.graphics.Color(0xCC000000), RoundedCornerShape(4.dp))
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
                                (video.info ?: video.channelName)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (content.loadingMore) {
                            item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() } }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Thumbnail(url: String?, modifier: Modifier) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
