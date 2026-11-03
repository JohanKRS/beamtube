package ytslim.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ytslim.data.ChannelPhotos
import ytslim.data.Playlist
import ytslim.data.SearchFilters
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
        val ages = listOf("2 hours ago", "9 hours ago", "1 day ago", "2 days ago", "4 days ago", "6 days ago", "1 week ago", "2 weeks ago")
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
                info = "$channel • ${compact(views)} views • ${ages[i % ages.size]}",
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

    private fun shot(name: String, content: @Composable () -> Unit) {
        // Fotos dos canais "inscritos" (as dos cards): todas menos uma, para aparecer também a inicial.
        ChannelPhotos.putAll(videos.mapNotNull { it.channelId }.distinct().drop(1).associateWith { "avatar:$it" })
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides previews) {
                content()
            }
        }
        compose.waitForIdle()
        // O captureToImage espera um quadro do sistema que o Robolectric não manda: desenha a tela direto.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val out = File(dir, "out").apply { mkdirs() }
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun feed(list: List<Video> = videos) = VideoListContent(list, hasMore = true, loadingMore = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSubscriptions() = shot("phone-subscriptions") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) { MobileUi.VideoGrid(feed(), "", {}, {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneRecommended() = shot("phone-recommended") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.RECOMMENDED, {}, {}) {
                MobileUi.VideoGrid(feed(videos.drop(6)), "", {}, {}, {}, header = {
                    MobileUi.FilterRow(feedFilterChoices(SearchFilters()), onChange = {}, padding = PaddingValues())
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

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSubscribedPlaylist() = shot("phone-playlist-subscribe") {
        MobileUi.Theme {
            MobileUi.DetailFrame("Essence of linear algebra", navigation = { MobileUi.BackButton {} }) {
                MobileUi.VideoRows(
                    feed(videos.filter { it.channelName == "3Blue1Brown" } + videos.take(6)), {}, {}, {}, showSaved = true,
                    header = { MobileUi.SubscribeHeader(PlaylistViewModel.Subscription.Shown(subscribed = true)) {} },
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
    fun tvSubscriptions() = shot("tv-subscriptions") {
        TvUi.Theme {
            TvUi.Home(HomeTab.SUBSCRIPTIONS, {}, {}) {
                TvUi.VideoGrid(feed(), "", {}, {}, {}, takeFocus = false, onFocusTaken = {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvRecommended() = shot("tv-recommended") {
        TvUi.Theme {
            TvUi.Home(HomeTab.RECOMMENDED, {}, {}) {
                Column {
                    TvUi.FilterButtons(feedFilterChoices(SearchFilters()), onChange = {})
                    Spacer(Modifier.height(16.dp))
                    TvUi.VideoGrid(feed(videos.drop(6)), "", {}, {}, {}, takeFocus = false, onFocusTaken = {})
                }
            }
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
            MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1800dp-xxhdpi")
    fun phoneSettingsLong() = shot("phone-settings-long") {
        MobileUi.Theme {
            MobileUi.Home(HomeTab.SETTINGS, {}, {}) { MobileUi.Settings(SettingsViewModel(), {}, {}) }
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

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvSettings() = shot("tv-settings") {
        TvUi.Theme {
            TvUi.Home(HomeTab.SETTINGS, {}, {}) { TvUi.Settings(SettingsViewModel(), {}, {}) }
        }
    }

    /**
     * O player de verdade (PlayerActivity) com os controles abertos e uma miniatura no lugar do
     * vídeo. Sem login, a pílula de cima (like, Save, Subscribe) aparece à mão.
     */
    private fun playerShot(name: String, tv: Boolean = false, jumped: Boolean = false, scrub: Boolean = false) {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        // O qualificador "television" não chega ao UiModeManager do Robolectric: sem isso o player
        // da TV sairia como o de toque (uma linha, botão de girar).
        if (tv) org.robolectric.Shadows.shadowOf(context.packageManager)
            .setSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK, true)
        val v = videos[1]
        val intent = ytslim.player.PlayerActivity.intent(context, v.id, v.title)
        val activity = org.robolectric.Robolectric.buildActivity(ytslim.player.PlayerActivity::class.java, intent).setup().get()
        val playerView = activity.findViewById<androidx.media3.ui.PlayerView>(ytslim.R.id.player_view)
        val thumb = BitmapFactory.decodeFile(File(dir, "thumbs/${v.id}.jpg").path)
        playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.background =
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
        // Sem rede, o título às vezes some quando o carregamento falha no meio da foto: fixa ele.
        playerView.findViewById<android.widget.TextView>(ytslim.R.id.player_title)?.text = v.title
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

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun playerTvJumped() = playerShot("player-tv-jumped", tv = true, jumped = true)
}
