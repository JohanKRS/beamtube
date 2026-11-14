package ytslim.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ytslim.R
import ytslim.data.AppIcon
import ytslim.data.BlockedChannel
import ytslim.data.SettingsRepository
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ytslim.data.ChannelPhotos
import ytslim.data.Playlist
import ytslim.data.SearchFilters
import ytslim.data.SuggestedTopic
import ytslim.data.Video
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi
import java.io.File

/**
 * Fotos das telas, para comparar o visual sem o aparelho (Robolectric com os gráficos nativos).
 * Só roda com `-Pytslim.screenshots=<pasta>`: a pasta tem `videos.json` (id, title, channel,
 * channelId, published, views), `thumbs/<id>.jpg` e `avatars/<channelId>.jpg`, e as fotos saem
 * em `<pasta>/out/`.
 */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenShots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val dir = File(System.getProperty("ytslim.screenshots").orEmpty())

    private val videos: List<Video> by lazy {
        val durations = listOf(754_000L, 1_312_000L, 498_000L, 2_105_000L, 917_000L, 1_843_000L, 365_000L, 3_620_000L)
        // O texto de baixo vem pronto do YouTube, no idioma do app; aqui, à mão nos três idiomas.
        val language = org.robolectric.RuntimeEnvironment.getApplication()
            .resources.configuration.locales[0].language
        val (ages, viewsWord) = when (language) {
            "pt" -> listOf("há 2 horas", "há 9 horas", "há 1 dia", "há 2 dias", "há 4 dias", "há 6 dias", "há 1 semana", "há 2 semanas") to "visualizações"
            "es" -> listOf("hace 2 horas", "hace 9 horas", "hace 1 día", "hace 2 días", "hace 4 días", "hace 6 días", "hace 1 semana", "hace 2 semanas") to "vistas"
            else -> listOf("2 hours ago", "9 hours ago", "1 day ago", "2 days ago", "4 days ago", "6 days ago", "1 week ago", "2 weeks ago") to "views"
        }
        Json.parseToJsonElement(File(dir, "videos.json").readText()).jsonArray.mapIndexed { i, e ->
            val o = e.jsonObject
            val channel = o["channel"]!!.jsonPrimitive.content.removeSuffix(" – In a Nutshell")
            val views = o["views"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            Video(
                id = o["id"]!!.jsonPrimitive.content,
                title = o["title"]!!.jsonPrimitive.content.removeSuffix(" - Computerphile"),
                channelName = channel,
                channelId = o["channelId"]!!.jsonPrimitive.content,
                thumbnailUrl = "thumb:" + o["id"]!!.jsonPrimitive.content,
                info = "$channel • ${compact(views)} $viewsWord • ${ages[i % ages.size]}",
                published = ages[i % ages.size],
                durationMs = durations[i % durations.size],
                isLive = false,
                isUpcoming = false,
                isShorts = false,
                percentWatched = if (i % 5 == 2) 35 else if (i % 7 == 3) 100 else 0,
                playlistId = null,
                playlistIndex = i,
            )
        }.let { list ->
            // Os canais misturados, como no feed.
            list.withIndex().sortedBy { (i, _) -> (i % 4) * 100 + i / 4 }.map { it.value }
        }
    }

    private fun compact(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
        n >= 1_000 -> "${n / 1_000}K"
        else -> n.toString()
    }

    private val playlists: List<Playlist> by lazy {
        val v = videos
        listOf(
            Playlist("VLWL", null, "WL", "Watch later", "712 videos", "thumb:" + v[0].id),
            Playlist("VLPL1", null, "PL1", "Sleep", "48 videos", "thumb:" + v[5].id),
            Playlist("VLPL2", null, "PL2", "Engineering deep dives", "Real Engineering • 31 videos", "thumb:" + v[9].id),
            Playlist("VLPL3", null, "PL3", "Essence of linear algebra", "3Blue1Brown • 16 videos", "thumb:" + v[14].id),
            Playlist("VLPL4", null, "PL4", "Cooking", "22 videos", "thumb:" + v[20].id),
        )
    }

    private val previews = AsyncImagePreviewHandler { request ->
        val data = request.data.toString()
        val file = when {
            data.startsWith("thumb:") -> File(dir, "thumbs/${data.removePrefix("thumb:")}.jpg")
            data.startsWith("avatar:") -> File(dir, "avatars/${data.removePrefix("avatar:")}.jpg")
            else -> null
        }
        file?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImage() } ?: Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF2C2D31.toInt()) }.asImage()
    }

    private fun shot(name: String, action: () -> Unit = {}, content: @Composable () -> Unit) {
        // Fotos dos canais "inscritos" (as dos cards): todas menos uma, para aparecer também a inicial.
        ChannelPhotos.putAll(videos.mapNotNull { it.channelId }.distinct().drop(1).associateWith { "avatar:$it" })
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides previews) {
                content()
            }
        }
        compose.waitForIdle()
        action()
        compose.waitForIdle()
        // O captureToImage espera um quadro do sistema que o Robolectric não manda: desenha a tela direto.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val out = File(dir, "out").apply { mkdirs() }
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun feed(list: List<Video> = videos) = VideoListContent(list, hasMore = true, loadingMore = false)

    /** A cor de destaque é global ([Look.accent]): cada foto começa e termina no âmbar. */
    @After
    fun resetAccent() {
        Look.accent = Accent.DEFAULT
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneAccent() {
        Look.accent = Accent.TEAL
        phoneExploreShot("phone-accent-teal")
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvAccent() {
        Look.accent = Accent.PLUM
        tvExploreShot("tv-accent-plum")
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneAccent() {
        Look.accent = Accent.RED
        playerShot("player-phone-accent-red")
    }

    /** A marca como o Android a desenha: o ícone de cada cor (no recorte redondo) e o banner da TV. */
    @Test
    @Config(qualifiers = "w1060dp-h1080dp-xhdpi")
    fun brand() = shot("brand") {
        val banners = listOf(
            R.drawable.tv_banner_night, R.drawable.tv_banner_black, R.drawable.tv_banner_teal, R.drawable.tv_banner_plum,
            R.drawable.tv_banner_mustard, R.drawable.tv_banner_vivid, R.drawable.tv_banner_red,
        )
        Column(Modifier.fillMaxSize().background(Color(0xFF202124)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppIcon.entries.forEach { AppIconPreview(it, 120.dp) }
            }
            banners.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(320.dp, 180.dp).clip(RoundedCornerShape(12.dp))) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_launcher_monochrome), contentDescription = null, modifier = Modifier.size(108.dp))
                Image(painterResource(R.drawable.ic_notification), contentDescription = null, modifier = Modifier.size(48.dp))
            }
            // O logotipo desenhado pelo app (a barra de cima da TV, o login e o trilho do tablet).
            Row(
                Modifier.background(Look.Ground).padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(40.sp)
                Wordmark(22.sp)
                BeamGlyph(Modifier.size(width = 40.dp, height = 30.dp))
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSubscriptions() = shot("phone-subscriptions") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { MobileUi.VideoGrid(feed(), "", {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneExplore() = phoneExploreShot("phone-explore")

    private fun phoneExploreShot(name: String) = shot(name) {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.EXPLORE, {}, {}) {
                MobileUi.VideoGrid(feed(videos.drop(6)), "", {}, {}, {}, header = {
                    MobileUi.FilterRow(exploreFilterChoices(SearchFilters()), onChange = {}, padding = PaddingValues())
                })
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phonePlaylists() = shot("phone-playlists") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.PLAYLISTS, {}, {}) { MobileUi.PlaylistGrid(playlists, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneHistory() = shot("phone-history") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.HISTORY, {}, {}) {
                MobileUi.VideoRows(feed(videos.drop(3)), {}, {}, {}, showSaved = true, header = null, onRemove = {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneWatchLater() = shot("phone-watch-later") {
        MobileUi.Theme {
            MobileUi.DetailFrame("Watch later", navigation = { MobileUi.BackButton {} }) {
                MobileUi.VideoRows(
                    feed(videos.drop(8)), {}, {}, {}, showSaved = false, header = null, onRemove = {},
                    onMove = { _, _ -> }, onDrop = {}, onMoveToTop = {},
                )
            }
        }
    }

    /** Deslizando o segundo vídeo para a direita (leva ao topo), com o dedo ainda na tela. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneWatchLaterToTop() = watchLaterShot("phone-watch-later-to-top") {
        compose.onAllNodes(hasClickAction() and hasAnyChild(hasText(videos[9].title)), useUnmergedTree = true).onFirst().performTouchInput {
            down(centerLeft + androidx.compose.ui.geometry.Offset(40f, 0f))
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(width * 0.03f, 0f)) }
        }
    }

    /** Segurando o terceiro vídeo e arrastando para baixo, com o dedo ainda na tela. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneWatchLaterLifted() = watchLaterShot("phone-watch-later-lifted") {
        compose.onAllNodes(hasClickAction() and hasAnyChild(hasText(videos[10].title)), useUnmergedTree = true).onFirst().performTouchInput {
            down(centerLeft + androidx.compose.ui.geometry.Offset(60f, 0f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            repeat(6) { moveBy(androidx.compose.ui.geometry.Offset(0f, height * 0.06f)) }
        }
    }

    private fun watchLaterShot(name: String, action: () -> Unit) = shot(name, action) {
        val list = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(videos.drop(8)) }
        MobileUi.Theme {
            MobileUi.DetailFrame("Watch later", navigation = { MobileUi.BackButton {} }) {
                MobileUi.VideoRows(
                    feed(list.value), {}, {}, {}, showSaved = false, header = null, onRemove = {},
                    onMove = { from, to -> list.value = list.value.toMutableList().apply { add(to, removeAt(from)) } },
                    onDrop = {}, onMoveToTop = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSubscribedPlaylist() = shot("phone-playlist-subscribe") {
        MobileUi.Theme {
            MobileUi.DetailFrame("Essence of linear algebra", navigation = { MobileUi.BackButton {} }) {
                MobileUi.VideoRows(
                    feed((videos.filter { it.channelName == "3Blue1Brown" } + videos.take(6)).distinctBy { it.id }), {}, {}, {}, showSaved = true,
                    header = { MobileUi.SubscribeHeader(Subscription.Shown(subscribed = true)) {} },
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h1480dp-xhdpi")
    fun tabletPortrait() = shot("tablet-portrait") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { MobileUi.VideoGrid(feed(), "", {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w1480dp-h924dp-xhdpi")
    fun tabletLandscape() = shot("tablet-landscape") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { MobileUi.VideoGrid(feed(), "", {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvSubscriptions() = tvSubscriptionsShot("tv-subscriptions")

    private fun tvSubscriptionsShot(name: String) = shot(name) {
        TvUi.Theme {
            TvUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) {
                TvUi.VideoGrid(feed(), "", {}, {}, {}, takeFocus = false, onFocusTaken = {})
            }
        }
    }

    /** A tela do fim do vídeo ([ytslim.player.EndScreen]) por cima do último quadro. */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvEndScreen() = endScreenShot("tv-end-screen", tv = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun phoneEndScreen() = endScreenShot("player-phone-end-screen", tv = false)

    private fun endScreenShot(name: String, tv: Boolean) {
        val end = ytslim.player.EndScreen(org.robolectric.RuntimeEnvironment.getApplication(), tv, onPlay = {}, onBack = {})
        end.show(videos.filter { !it.isShorts && !it.isLive }.drop(2).take(ytslim.data.UpNext.MAX), playNext = false)
        val frame = BitmapFactory.decodeFile(File(dir, "thumbs/${videos[1].id}.jpg").path).asImageBitmap()
        shot(name) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Image(frame, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                end.Content()
            }
        }
    }

    /** As abas da TV numa linha só, com os nomes mais compridos (espanhol e português). */
    @Test
    @Config(qualifiers = "es-w960dp-h540dp-land-television-xhdpi")
    fun tvSubscriptionsSpanish() = tvSubscriptionsShot("tv-subscriptions-es")

    @Test
    @Config(qualifiers = "pt-w960dp-h540dp-land-television-xhdpi")
    fun tvSubscriptionsPortuguese() = tvSubscriptionsShot("tv-subscriptions-pt")

    /** Com a letra maior da Acessibilidade, as abas continuam numa linha só (a letra delas diminui). */
    @Test
    @Config(qualifiers = "es-w960dp-h540dp-land-television-xhdpi", fontScale = 1.3f)
    fun tvSubscriptionsLargeText() = tvSubscriptionsShot("tv-subscriptions-es-large")

    @Test
    @Config(qualifiers = "pt-w960dp-h540dp-land-television-xhdpi", fontScale = 2f)
    fun tvSubscriptionsLargestText() = tvSubscriptionsShot("tv-subscriptions-pt-largest")

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvExplore() = tvExploreShot("tv-explore")

    private fun tvExploreShot(name: String) = shot(name) {
        TvUi.Theme {
            TvUi.Home(HomeTab.EXPLORE, {}, {}) {
                Column {
                    TvUi.FilterButtons(exploreFilterChoices(SearchFilters()), onChange = {})
                    Spacer(Modifier.height(16.dp))
                    TvUi.VideoGrid(feed(videos.drop(6)), "", {}, {}, {}, takeFocus = false, onFocusTaken = {})
                }
            }
        }
    }

    /** A página de um canal (Veritasium), com a inscrição e as abas Vídeos e Playlists. */
    private fun channel(subscribed: Boolean) = Triple(
        "Veritasium",
        UiState.Ready(ChannelViewModel.Content(feed((videos.filter { it.channelName == "Veritasium" } + videos.drop(8)).distinctBy { it.id }), playlists.drop(1))),
        Subscription.Shown(subscribed = subscribed),
    )

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvChannel() = shot("tv-channel") {
        val (title, state, subscription) = channel(subscribed = true)
        TvUi.Theme { TvUi.ChannelPage(title, state, false, subscription, {}, {}, {}, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvChannelSubscribe() = shot("tv-channel-subscribe") {
        val (title, state, subscription) = channel(subscribed = false)
        TvUi.Theme { TvUi.ChannelPage(title, state, false, subscription, {}, {}, {}, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneChannel() = shot("phone-channel") {
        val (title, state, subscription) = channel(subscribed = false)
        MobileUi.Theme { MobileUi.ChannelPage(title, state, false, subscription, {}, {}, {}, null, {}, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneChannelSubscribed() = shot("phone-channel-subscribed") {
        val (title, state, subscription) = channel(subscribed = true)
        MobileUi.Theme { MobileUi.ChannelPage(title, state, false, subscription, {}, {}, {}, null, {}, {}, {}, {}) }
    }

    /** O canal recém-bloqueado, com o aviso que diz o que aconteceu na conta (1.0.3). */
    private fun blockedNotice(sent: Boolean): Notice {
        val res = org.robolectric.RuntimeEnvironment.getApplication().resources
        val id = if (sent) R.string.channel_blocked_account_notice else R.string.channel_blocked_notice
        return Notice(res.getString(id), 1L, long = true)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvChannelBlocked() = shot("tv-channel-blocked") {
        val (title, state, subscription) = channel(subscribed = false)
        TvUi.Theme {
            TvUi.ChannelPage(title, state, true, subscription, {}, {}, {}, {}, {}, {})
            TvUi.NoticePill(blockedNotice(sent = true))
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneChannelBlocked() = shot("phone-channel-blocked") {
        val (title, state, subscription) = channel(subscribed = false)
        MobileUi.Theme {
            MobileUi.ChannelPage(title, state, true, subscription, {}, {}, {}, null, {}, {}, {}, {})
            MobileUi.NoticePill(blockedNotice(sent = true))
        }
    }

    @Test
    @Config(qualifiers = "pt-w411dp-h891dp-xxhdpi")
    fun phoneChannelBlockedLaterPortuguese() = shot("phone-channel-blocked-later-pt") {
        val (title, state, subscription) = channel(subscribed = false)
        MobileUi.Theme {
            MobileUi.ChannelPage(title, state, true, subscription, {}, {}, {}, null, {}, {}, {}, {})
            MobileUi.NoticePill(blockedNotice(sent = false))
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvPlaylists() = shot("tv-playlists") {
        TvUi.Theme {
            TvUi.Home(HomeTab.PLAYLISTS, {}, {}) { TvUi.PlaylistGrid(playlists, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSettings() = shot("phone-settings") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    fun phoneSettingsLong() = shot("phone-settings-long") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSearch() = shot("phone-search") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SEARCH, {}, {}) {
                Column {
                    MobileUi.FilterRow(searchFilterChoices(SearchFilters()), onChange = {})
                    val items = listOf<ytslim.data.SearchItem>(
                        ytslim.data.SearchItem.ChannelItem(ytslim.data.Channel(videos[2].channelId!!, "Veritasium", "17.2M subscribers", "avatar:" + videos[2].channelId)),
                    ) + videos.take(5).map { ytslim.data.SearchItem.VideoItem(it) } +
                        ytslim.data.SearchItem.PlaylistItem(playlists[3])
                    MobileUi.SearchResults(SearchViewModel.Results("veritasium", items, true, false), {}, {}, {}, {})
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneLoading() = shot("phone-loading") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.RECOMMENDED, {}, {}) {
                MobileUi.Loadable<Unit>(UiState.Loading, {}, placeholder = { MobileUi.VideoGridSkeleton() }) {}
            }
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvLoading() = shot("tv-loading") {
        TvUi.Theme {
            TvUi.Home(HomeTab.EXPLORE, {}, {}) { TvUi.GridSkeleton() }
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvFocused() = shot("tv-focused") {
        TvUi.Theme {
            TvUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) {
                TvUi.VideoGrid(feed(), "", {}, {}, {}, takeFocus = true, onFocusTaken = {})
            }
        }
    }

    /** A tela de tópicos de uma instalação nova, com algumas sugestões já marcadas ([picks] falso: nenhuma). */
    private fun topicsVm(lessStep: Boolean, picks: Boolean = true): TopicsViewModel {
        val vm = TopicsViewModel()
        repeat(100) {
            if (vm.state.value.loaded) return@repeat
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        if (!picks) return vm
        listOf(SuggestedTopic.DOCUMENTARIES, SuggestedTopic.HISTORY, SuggestedTopic.SCIENCE, SuggestedTopic.SPACE, SuggestedTopic.ENGINEERING, SuggestedTopic.SLEEP)
            .forEach(vm::toggle)
        if (lessStep) {
            vm.next()
            listOf(SuggestedTopic.POLITICS, SuggestedTopic.CELEBRITIES, SuggestedTopic.REACTIONS, SuggestedTopic.PRANKS).forEach(vm::toggle)
        }
        return vm
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneTopics() = shot("phone-topics") { MobileUi.Theme { MobileUi.Topics(topicsVm(lessStep = false), firstRun = true) {} } }

    /** Sem nada marcado: as sugestões precisam estar no mesmo lugar que na foto com algumas marcadas. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneTopicsNone() = shot("phone-topics-none") { MobileUi.Theme { MobileUi.Topics(topicsVm(lessStep = false, picks = false), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneTopicsLess() = shot("phone-topics-less") { MobileUi.Theme { MobileUi.Topics(topicsVm(lessStep = true), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "pt-w411dp-h891dp-xxhdpi")
    fun phoneTopicsPortuguese() = shot("phone-topics-pt") { MobileUi.Theme { MobileUi.Topics(topicsVm(lessStep = false), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "es-w960dp-h540dp-land-television-xhdpi")
    fun tvTopicsSpanish() = shot("tv-topics-es") { TvUi.Theme { TvUi.Topics(topicsVm(lessStep = false), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvTopics() = shot("tv-topics") { TvUi.Theme { TvUi.Topics(topicsVm(lessStep = false), firstRun = true) {} } }

    /** Sem nada marcado: as sugestões precisam estar no mesmo lugar que na foto com algumas marcadas. */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvTopicsNone() = shot("tv-topics-none") { TvUi.Theme { TvUi.Topics(topicsVm(lessStep = false, picks = false), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvTopicsLess() = shot("tv-topics-less") { TvUi.Theme { TvUi.Topics(topicsVm(lessStep = true), firstRun = true) {} } }

    /** O foco numa sugestão da primeira linha: o botão cresce e não pode sair cortado em cima. */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvTopicsFocused() = shot("tv-topics-focused", action = {
        // Uma tecla do controle põe o Compose no modo de teclado, onde os botões da TV aceitam o foco.
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNode(hasText("Video essays") and hasClickAction(), useUnmergedTree = false).requestFocus()
    }) { TvUi.Theme { TvUi.Topics(topicsVm(lessStep = false), firstRun = true) {} } }

    @Test
    @Config(qualifiers = "pt-w411dp-h891dp-xxhdpi")
    fun phoneSubscriptionsPortuguese() = shot("phone-subscriptions-pt") {
        MobileUi.Theme { MobileUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { MobileUi.VideoGrid(feed(), "", {}, {}, {}) } }
    }

    @Test
    @Config(qualifiers = "pt-w411dp-h2200dp-xxhdpi")
    fun phoneSettingsPortuguese() = shot("phone-settings-pt") {
        MobileUi.Theme { MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}, {}) } }
    }

    /**
     * Configurações com dois canais bloqueados (1.0.3): um que o YouTube já aceitou na conta e um
     * que ainda está só no aparelho. A tela é alta para o grupo aparecer sem rolar.
     */
    private fun withBlocked(name: String, content: @Composable () -> Unit) {
        kotlinx.coroutines.runBlocking {
            SettingsRepository.block(BlockedChannel("UCblocked1", "Daily Drama Recap"))
            SettingsRepository.markOnAccount("UCblocked1")
            SettingsRepository.block(BlockedChannel("UCblocked2", "Loud Reactions"))
        }
        try {
            shot(name, content = content)
        } finally {
            kotlinx.coroutines.runBlocking {
                SettingsRepository.unblock("UCblocked1")
                SettingsRepository.unblock("UCblocked2")
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    fun phoneSettingsBlocked() = withBlocked("phone-settings-blocked") {
        MobileUi.Theme { MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}, {}) } }
    }

    @Test
    @Config(qualifiers = "pt-w411dp-h2200dp-xxhdpi")
    fun phoneSettingsBlockedPortuguese() = withBlocked("phone-settings-blocked-pt") {
        MobileUi.Theme { MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}, {}) } }
    }

    @Test
    @Config(qualifiers = "w960dp-h2000dp-television-xhdpi")
    fun tvSettingsBlocked() = withBlocked("tv-settings-blocked") {
        TvUi.Theme { TvUi.Home(HomeTab.SETTINGS, {}, {}) { TvUi.Settings(SettingsViewModel(), {}, {}, {}) } }
    }

    /**
     * O grupo do botão YouTube do controle (1.0.4) nos três estados: o serviço de Acessibilidade
     * desligado (com o foco na linha), ligado à espera do botão e pronto. A tela é alta para o grupo
     * aparecer sem rolar.
     */
    private fun remoteShot(name: String, on: Boolean, learned: Boolean, focus: String? = null) {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("remote_keys", android.content.Context.MODE_PRIVATE)
        val manager = org.robolectric.Shadows.shadowOf(app.getSystemService(android.view.accessibility.AccessibilityManager::class.java))
        if (on) {
            val info = android.accessibilityservice.AccessibilityServiceInfo()
            val resolve = android.content.pm.ResolveInfo().apply {
                serviceInfo = android.content.pm.ServiceInfo().apply {
                    packageName = app.packageName
                    this.name = ytslim.player.ScreenOffService::class.java.name
                }
            }
            org.robolectric.util.ReflectionHelpers.setField(info, "mResolveInfo", resolve)
            manager.setEnabledAccessibilityServiceList(listOf(info))
        }
        if (learned) prefs.edit().putStringSet("youtube_button", setOf("293")).commit()
        try {
            shot(name, action = {
                if (focus != null) {
                    compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
                    compose.onNode(hasText(focus) and hasClickAction(), useUnmergedTree = false).requestFocus()
                }
            }) {
                TvUi.Theme { TvUi.Home(HomeTab.SETTINGS, {}, {}) { TvUi.Settings(SettingsViewModel(), {}, {}, {}) } }
            }
        } finally {
            manager.setEnabledAccessibilityServiceList(emptyList())
            prefs.edit().clear().commit()
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h2600dp-television-xhdpi")
    fun tvSettingsRemoteOff() = remoteShot("tv-settings-remote-off", on = false, learned = false, focus = "Open Accessibility settings")

    @Test
    @Config(qualifiers = "es-w960dp-h2600dp-television-xhdpi")
    fun tvSettingsRemoteWaitingSpanish() = remoteShot("tv-settings-remote-waiting-es", on = true, learned = false)

    @Test
    @Config(qualifiers = "pt-w960dp-h2600dp-television-xhdpi")
    fun tvSettingsRemoteReadyPortuguese() = remoteShot("tv-settings-remote-ready-pt", on = true, learned = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvSettings() = shot("tv-settings") {
        TvUi.Theme {
            TvUi.Home(HomeTab.SETTINGS, {}, {}) { TvUi.Settings(SettingsViewModel(), {}, {}, {}) }
        }
    }

    /**
     * O player de verdade (PlayerActivity) com os controles abertos e uma miniatura no lugar do
     * vídeo. Sem login, a pílula de cima (like, Save, Subscribe) aparece à mão.
     */
    /** Uma legenda automática como o YouTube manda (fmt=vtt): cada linha nova entra embaixo, a cada 2 s. */
    private val rollingCaptions = listOf(
        "WEBVTT", "",
        "00:00:00.000 --> 00:00:02.000 align:start position:0%", " ", "the grid has to balance supply and demand", "",
        "00:00:02.000 --> 00:00:04.000 align:start position:0%", "the grid has to balance supply and demand", "every second of every day", "",
        "00:00:04.000 --> 00:00:06.000 align:start position:0%", "every second of every day", "so when the sun goes down", "",
        "00:00:06.000 --> 00:00:08.000 align:start position:0%", "so when the sun goes down", "something else has to take over", "",
    ).joinToString("\n")

    private fun playerShot(
        name: String,
        tv: Boolean = false,
        jumped: Boolean = false,
        scrub: Boolean = false,
        captions: Boolean = false,
        hidden: Boolean = false,
        live: Boolean = false,
        channelFocused: Boolean = false,
        captionsAtMs: Long = 3_000,
        rolling: Boolean = false,
        /** "opening": o vídeo acabou de ser aberto (miniatura e círculo); "buffering": parou para carregar no meio. */
        loading: String? = null,
        /** O painel do modo dormir aberto (brilho, o que tocar depois, desligar ao terminar). */
        sleep: Boolean = false,
        /** O brilho do modo dormir já escolhido (1.0.4: os controles seguem, sem ficar abaixo de 10%). */
        brightness: Int = 100,
    ) {
        ytslim.player.SleepBrightness.set(brightness)
        try {
            playerShotAt(name, tv, jumped, scrub, captions, hidden, live, channelFocused, captionsAtMs, rolling, loading, sleep)
        } finally {
            ytslim.player.SleepBrightness.set(100)
        }
    }

    private fun playerShotAt(
        name: String,
        tv: Boolean,
        jumped: Boolean,
        scrub: Boolean,
        captions: Boolean,
        hidden: Boolean,
        live: Boolean,
        channelFocused: Boolean,
        captionsAtMs: Long,
        rolling: Boolean,
        loading: String?,
        sleep: Boolean,
    ) {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        // O qualificador "television" não chega ao UiModeManager do Robolectric: sem isso o player
        // da TV sairia como o de toque (uma linha, botão de girar).
        if (tv) org.robolectric.Shadows.shadowOf(context.packageManager)
            .setSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK, true)
        val v = videos[1]
        val intent = ytslim.player.PlayerActivity.intent(context, v.id, v.title, published = v.published)
        val activity = org.robolectric.Robolectric.buildActivity(ytslim.player.PlayerActivity::class.java, intent).setup().get()
        val playerView = activity.findViewById<androidx.media3.ui.PlayerView>(ytslim.R.id.player_view)
        val thumb = BitmapFactory.decodeFile(File(dir, "thumbs/${v.id}.jpg").path)
        // A tela de carregando ([PlayerLoading]): nas fotos do vídeo tocando, o primeiro quadro já veio.
        val loadingView = activity.window.decorView.let { root ->
            fun find(view: android.view.View): ytslim.player.PlayerLoading? = when (view) {
                is ytslim.player.PlayerLoading -> view
                is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
                else -> null
            }
            find(root)!!
        }
        when (loading) {
            // A miniatura do card (aqui não há rede para o carregador de imagens buscá-la).
            "opening" -> (loadingView.getChildAt(0) as android.widget.ImageView).setImageBitmap(thumb)
            "buffering" -> {
                loadingView.stop()
                loadingView.setBusy(true)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500))
            }
            else -> loadingView.stop()
        }
        if (loading != "opening") playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.background =
            android.graphics.drawable.BitmapDrawable(activity.resources, thumb)
        playerView.findViewById<android.view.View>(ytslim.R.id.player_actions)?.visibility = android.view.View.VISIBLE
        playerView.findViewById<android.view.View>(ytslim.R.id.player_save)?.visibility = android.view.View.VISIBLE
        playerView.findViewById<android.view.View>(ytslim.R.id.player_captions)?.visibility = android.view.View.VISIBLE
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_channel)?.apply { text = v.channelName; visibility = android.view.View.VISIBLE }
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_quality_button)?.apply { text = if (tv) "2160p" else "1080p"; visibility = android.view.View.VISIBLE }
        playerView.showController()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        // Os capítulos de 0e3GPea1Tyg (lidos em 2026-11-03), numa barra de 15:17 parada em 4:12.
        val bar = playerView.findViewById<ytslim.player.ChapterTimeBar>(androidx.media3.ui.R.id.exo_progress)
        bar.setDuration(917_000); bar.setPosition(252_000); bar.setBufferedPosition(400_000)
        bar.setChapters(listOf(0L, 240_000L, 460_000L, 624_000L, 676_000L))
        playerView.findViewById<android.widget.TextView>(androidx.media3.ui.R.id.exo_position)?.text = "4:12"
        playerView.findViewById<android.widget.TextView>(androidx.media3.ui.R.id.exo_duration)?.text = "15:17"
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_chapter)?.apply {
            text = "Honeycomb"; visibility = if (scrub) android.view.View.GONE else android.view.View.VISIBLE
        }
        if (scrub) {
            val preview = playerView.findViewById<android.view.View>(ytslim.R.id.player_preview)
            playerView.findViewById<android.widget.ImageView>(ytslim.R.id.player_preview_image).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                setImageBitmap(BitmapFactory.decodeFile(File(dir, "thumbs/${videos[4].id}.jpg").path))
            }
            playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_preview_chapter).text = "Tug Of War"
            playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_preview_time).text = "8:12"
            preview.visibility = android.view.View.VISIBLE
            bar.setPosition(492_000)
            relayout(activity)
            ytslim.player.PlayerActivity::class.java.getDeclaredMethod("placePreview", Long::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(activity, 492_000L)
        }
        if (jumped) {
            ytslim.player.PlayerActivity::class.java
                .getDeclaredMethod("flash", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(activity, activity.getString(ytslim.R.string.player_jumped_ahead), ytslim.R.drawable.ic_skip_ahead, android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL)
            // O aviso some numa animação que o Robolectric termina na hora: segura ele na tela.
            (ytslim.player.PlayerActivity::class.java.getDeclaredField("seekBadge").apply { isAccessible = true }.get(activity) as android.view.View).apply {
                animate().cancel(); alpha = 1f; visibility = android.view.View.VISIBLE
            }
        }
        if (live) {
            // O que o renderLive faz numa live atrás do ponto ao vivo (aqui não há live tocando).
            playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_position).visibility = android.view.View.GONE
            playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_duration).visibility = android.view.View.GONE
            playerView.findViewById<android.view.View>(ytslim.R.id.player_chapter).visibility = android.view.View.GONE
            playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_live).apply {
                visibility = android.view.View.VISIBLE
                compoundDrawableTintList = android.content.res.ColorStateList.valueOf(context.getColor(ytslim.R.color.player_live))
            }
            bar.setChapters(emptyList())
        }
        if (channelFocused) {
            // O Robolectric fica no modo de toque, onde só o foco "de toque" pega: como o controle na TV.
            playerView.findViewById<android.view.View>(ytslim.R.id.player_channel).apply { isFocusableInTouchMode = true }.requestFocus()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        if (sleep) {
            playerView.findViewById<android.view.View>(ytslim.R.id.player_sleep).performClick()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(600))
        }
        if (hidden) {
            playerView.hideController()
            // O fade dos controles e a descida da legenda.
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1))
        }
        if (captions) {
            // A legenda automática do YouTube como ela chega ([rollingCaptions]), em blocos ou ([rolling]) rolando.
            val parser = androidx.media3.extractor.text.webvtt.WebvttParser()
                .let { if (rolling) it else ytslim.player.BlockCaptionParser(it) }
            val timeline = mutableListOf<androidx.media3.extractor.text.CuesWithTiming>()
            parser.parse(rollingCaptions.toByteArray(), androidx.media3.extractor.text.SubtitleParser.OutputOptions.allCues()) { timeline += it }
            val cues = timeline.first { captionsAtMs * 1000 >= it.startTimeUs && captionsAtMs * 1000 < it.endTimeUs }.cues
            (ytslim.player.PlayerActivity::class.java.getDeclaredField("captionsView").apply { isAccessible = true }.get(activity) as androidx.media3.ui.SubtitleView)
                .setCues(ytslim.player.CaptionRules.centered(cues))
        }
        if (loading == "opening") {
            // Como o player abre: a barra no começo, sem o que só chega com o vídeo e a página dele.
            bar.setDuration(0); bar.setPosition(0); bar.setBufferedPosition(0); bar.setChapters(emptyList())
            playerView.findViewById<android.widget.TextView>(androidx.media3.ui.R.id.exo_position)?.text = "0:00"
            playerView.findViewById<android.widget.TextView>(androidx.media3.ui.R.id.exo_duration)?.text = "0:00"
            for (id in listOf(ytslim.R.id.player_chapter, ytslim.R.id.player_actions, ytslim.R.id.player_save, ytslim.R.id.player_captions, ytslim.R.id.player_quality_button)) {
                playerView.findViewById<android.view.View>(id)?.visibility = android.view.View.GONE
            }
        }
        // Sem rede, o título às vezes some quando o carregamento falha no meio da foto: fixa ele.
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_title)?.text = v.title
        // A linha do canal como fica com a página do vídeo lida (o canal é o botão ao lado).
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_detail)?.text = spacedDotsForViews("· ${v.published}")
        relayout(activity)
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val out = File(dir, "out").apply { mkdirs() }
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Mede e arruma a tela de novo (o Robolectric não roda o quadro seguinte sozinho). */
    private fun relayout(activity: android.app.Activity) {
        activity.window.decorView.apply {
            measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY),
            )
            layout(left, top, right, bottom)
        }
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneLandscape() = playerShot("player-phone-landscape")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneOpening() = playerShot("player-phone-opening", loading = "opening")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneOpeningClean() = playerShot("player-phone-opening-clean", loading = "opening", hidden = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneBuffering() = playerShot("player-phone-buffering", loading = "buffering", hidden = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvOpening() = playerShot("player-tv-opening", tv = true, loading = "opening")

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvOpeningClean() = playerShot("player-tv-opening-clean", tv = true, loading = "opening", hidden = true)

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun playerPhonePortrait() = playerShot("player-phone-portrait")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneJumped() = playerShot("player-phone-jumped", jumped = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneScrub() = playerShot("player-phone-scrub", scrub = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvScrub() = playerShot("player-tv-scrub", tv = true, scrub = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTv() = playerShot("player-tv", tv = true)

    /** O brilho do modo dormir nos controles (1.0.4): em 25% e em 0% (o vídeo apaga, os controles ficam em 10%). */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvDim25() = playerShot("player-tv-dim-25", tv = true, sleep = true, brightness = 25)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvDim0() = playerShot("player-tv-dim-0", tv = true, sleep = true, brightness = 0)

    /** As telas do app com o brilho do modo dormir em 25% (1.0.4); a camada vem da MainActivity. */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvSubscriptionsDim25() {
        ytslim.player.SleepBrightness.set(25)
        try {
            shot("tv-subscriptions-dim-25", action = { ytslim.player.SleepBrightness.applyTo(compose.activity.window) }) {
                TvUi.Theme { TvUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { TvUi.VideoGrid(feed(), "", {}, {}, {}, takeFocus = false, onFocusTaken = {}) } }
            }
        } finally {
            ytslim.player.SleepBrightness.set(100)
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvSleep() = playerShot("player-tv-sleep", tv = true, sleep = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneSleep() = playerShot("player-phone-sleep", sleep = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvChannelFocused() = playerShot("player-tv-channel-focused", tv = true, channelFocused = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvJumped() = playerShot("player-tv-jumped", tv = true, jumped = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneLive() = playerShot("player-phone-live", live = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvLive() = playerShot("player-tv-live", tv = true, live = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneCaptions() = playerShot("player-phone-captions", captions = true)

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun playerPhoneCaptionsHidden() = playerShot("player-phone-captions-hidden", captions = true, hidden = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvCaptions() = playerShot("player-tv-captions", tv = true, captions = true)

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvCaptionsHidden() = playerShot("player-tv-captions-hidden", tv = true, captions = true, hidden = true)

    /** A mesma legenda em 1, 3, 5 e 7 s: rolando (como chega) e em blocos (como o app mostra). */
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvCaptionsOverTime() = listOf(true, false).forEach { rolling ->
        listOf(1_000L, 3_000L, 5_000L, 7_000L).forEach { at ->
            playerShot(
                "player-tv-captions-${if (rolling) "rolling" else "blocks"}-${at / 1000}s",
                tv = true, captions = true, hidden = true, captionsAtMs = at, rolling = rolling,
            )
        }
    }
}
