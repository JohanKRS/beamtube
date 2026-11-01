package ytslim.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.test.core.app.ApplicationProvider
import com.liskovsoft.mediaserviceinterfaces.data.SearchOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Valida o motor contra o YouTube de verdade, sem login (o que dá para checar fora do aparelho).
 *
 * Roda só com: ./gradlew :app:testAppDebugUnitTest -Pytslim.online=true --tests '*EngineOnlineTest*'
 */
@RunWith(RobolectricTestRunner::class)
class EngineOnlineTest {

    @Before
    fun setUp() {
        assumeTrue("teste online desligado", System.getProperty("ytslim.online") == "true")
        // O motor em debug loga o corpo de toda resposta (inclusive JS de vários MB): silencia.
        quietLoggers.forEach { it.level = java.util.logging.Level.WARNING }
        Engine.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun signInCodeIsIssued() {
        val code = try {
            withMainLooper { AuthRepository.signIn().first() }
        } finally {
            printEngineWarnings()
        }
        println("[online] código de login: $code")
        assertTrue(code is AuthRepository.SignInEvent.Code && code.code.isNotBlank())
    }

    /**
     * Passa por todos os clientes do YouTube que o motor conhece. Aqui no JVM não existe o V8,
     * então os clientes que precisam decifrar URLs falham; no aparelho eles funcionam.
     * O que importa: algum cliente entrega formatos DASH, o MPD do motor passa no parser do
     * Media3 e a faixa original continua marcada depois do parser.
     */
    @Test
    fun multiAudioVideoHasOriginalTrackInManifest() = checkVideo(MULTI_AUDIO_VIDEO, expectOriginal = true)

    @Test
    fun regularVideoBuildsManifest() = checkVideo(REGULAR_VIDEO, expectOriginal = false)

    @Test
    fun publicPlaylistOpensByIdAndPages() = withMainLooper {
        val found = Engine.content.getSearch("lofi hip hop", SearchOptions.TYPE_PLAYLIST).orEmpty()
            .flatMap { it.mediaItems.orEmpty() }
        found.take(5).forEach { println("[online] busca: tipo=${it.type} playlist=${it.playlistId} canal=${it.channelId} ${it.title}") }
        val playlistItem = found.firstOrNull { it.playlistId != null || it.channelId?.startsWith("VL") == true }
        assertNotNull("busca sem playlists", playlistItem)
        val playlistId = playlistItem!!.playlistId ?: playlistItem.channelId!!.removePrefix("VL")
        println("[online] playlist: ${playlistItem.title} ($playlistId)")

        // É assim que uma inscrição em playlist vai abrir a playlist: só pelo id.
        val page = LibraryRepository.playlistVideos(
            Playlist(browseId = "VL$playlistId", params = null, playlistId = playlistId, title = "", info = null, thumbnailUrl = null),
        )
        println("[online] vídeos na primeira página: ${page.videos.size}, tem mais: ${page.hasMore}")
        page.videos.take(3).forEach { println("[online]   ${it.id} | ${it.title} | canal=${it.channelId} ${it.channelName}") }
        assertTrue(page.videos.isNotEmpty())
        assertTrue("vídeos sem canal", page.videos.any { it.channelId != null })
        if (page.hasMore) {
            val more = LibraryRepository.more(page)
            println("[online] segunda página: ${more.videos.size}")
        }
    }

    /** A busca com filtros, as sugestões e a página do canal (achado pela própria busca). */
    @Test
    fun searchFiltersSuggestionsAndChannelPage() = withMainLooper {
        val mixed = SearchRepository.search("3blue1brown", SearchFilters())
        println("[online] busca: ${mixed.items.size} itens, tem mais: ${mixed.hasMore}")
        mixed.items.take(6).forEach { println("[online]   $it".take(160)) }
        assertTrue(mixed.items.any { it is SearchItem.VideoItem })
        assertTrue(mixed.hasMore)
        val more = SearchRepository.more(mixed)
        println("[online] segunda página: ${more.items.size}")
        assertTrue(more.items.isNotEmpty())

        assertTrue("playlist lida como vídeo", mixed.items.filterIsInstance<SearchItem.VideoItem>().none { it.video.durationMs == 0L })

        // Duração + tipo + data: a tabela do motor não tem essa combinação.
        val long = SearchRepository.search(
            "linear algebra",
            SearchFilters(date = UploadDate.YEAR, duration = SearchDuration.OVER_20, type = ResultType.VIDEO, sort = SortBy.VIEWS),
        )
        val videos = long.items.filterIsInstance<SearchItem.VideoItem>().map { it.video }
        println("[online] filtrada: ${long.items.size} itens; minutos: ${videos.take(8).map { it.durationMs / 60_000 }}; ${videos.take(4).map { it.info }}")
        assertTrue(videos.isNotEmpty())
        assertTrue("tipo ignorado", long.items.all { it is SearchItem.VideoItem })
        assertTrue("filtro de duração ignorado", videos.filter { it.durationMs > 0 }.all { it.durationMs >= 20 * 60_000 })

        val suggestions = SearchRepository.suggestions("kotlin corou")
        println("[online] sugestões: $suggestions")
        assertTrue(suggestions.isNotEmpty())

        val channels = SearchRepository.search("3blue1brown", SearchFilters(type = ResultType.CHANNEL))
            .items.filterIsInstance<SearchItem.ChannelItem>()
        println("[online] canais: ${channels.take(3).map { it.channel.id + " " + it.channel.title }}")
        val channel = channels.first().channel
        val page = ChannelRepository.channel(channel.id)
        println("[online] canal ${channel.title}: ${page.videos.videos.size} vídeos, tem mais: ${page.videos.hasMore}, " +
            "playlists: ${page.playlists.take(5).map { it.title }}")
        assertTrue(page.videos.videos.isNotEmpty())
        assertTrue(page.videos.videos.none { it.isShorts })
    }

    private fun checkVideo(videoId: String, expectOriginal: Boolean) {
        var manifests = 0
        var originalSurvived = false
        var needsV8 = 0
        for (i in 0 until CLIENTS_TO_TRY) {
            // Sem V8 nenhum cliente passa; não adianta esperar os 12.
            if (needsV8 >= 2 && manifests == 0) break
            val attempt = runCatching { Engine.mediaItems.getFormatInfo(videoId) }
            if (attempt.exceptionOrNull()?.message?.contains("J2V8") == true) needsV8++
            val info = attempt.getOrNull()
            val client = info?.clientInfo?.clientName
            when {
                info == null -> println("[online] $videoId: erro (${attempt.exceptionOrNull()?.message?.take(90)})")
                info.isUnplayable -> println("[online] $videoId [$client]: indisponível (${info.playabilityReason})")
                else -> {
                    val stream = PlaybackRepository.pickStream(info)
                    val labels = info.adaptiveFormats.orEmpty().filter { it.mimeType?.startsWith("audio/") == true }
                        .mapNotNull { it.language }.distinct()
                    println("[online] $videoId [$client]: ${stream?.let { it::class.simpleName } ?: "nada tocável (sabr=${info.containsSabrFormats()})"}, áudio=$labels")
                    if (stream is Stream.DashManifest) {
                        manifests++
                        val manifest = DashManifestParser().parse(Uri.EMPTY, stream.xml.inputStream())
                        val sets = manifest.getPeriod(0).adaptationSets
                        val video = sets.filter { it.type == C.TRACK_TYPE_VIDEO }
                        val audioLanguages = sets.filter { it.type == C.TRACK_TYPE_AUDIO }
                            .map { it.representations.first().format.language }
                        video.forEach { set ->
                            println("[online]     vídeo ${set.representations.first().format.codecs}: " +
                                set.representations.map { it.format.height }.distinct().sortedDescending())
                        }
                        println("[online]     áudio no Media3: $audioLanguages")
                        val index = AudioRules.pickOriginalGroup(audioLanguages)
                        if (index != null) {
                            originalSurvived = true
                            println("[online]     escolhido: ${audioLanguages[index]}")
                        }
                    }
                }
            }
            Engine.service.switchNextClientNow()
        }
        printEngineWarnings()
        // O V8 (j2v8) só tem binários para Android: aqui no JVM nenhum cliente passa da decifragem.
        assumeTrue("precisa do V8, só roda no aparelho", manifests > 0 || needsV8 == 0)
        assertTrue("nenhum cliente entregou formatos DASH", manifests > 0)
        if (expectOriginal) assertTrue("a faixa original não chegou ao Media3", originalSurvived)
    }

    /**
     * Os Observables do motor entregam o resultado na thread principal. No Robolectric o looper
     * principal só anda quando alguém manda, então o teste roda o bloco em outra thread e vai
     * girando o looper até terminar.
     */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun <T> withMainLooper(timeoutMs: Long = 120_000, block: suspend () -> T): T {
        val job = kotlinx.coroutines.GlobalScope.async(kotlinx.coroutines.Dispatchers.IO) { block() }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!job.isCompleted) {
            check(System.currentTimeMillis() < deadline) { "tempo esgotado" }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        return runBlocking { job.await() }
    }

    private fun printEngineWarnings() {
        ShadowLog.getLogs().filter { it.type >= android.util.Log.WARN }.takeLast(15).forEach {
            println("[motor] ${it.tag}: ${it.msg?.take(200)} ${it.throwable?.toString()?.take(200) ?: ""}")
        }
    }

    /** O resumo vai para app/build/online-report.txt (a saída padrão fica cheia de log do motor). */
    private fun println(line: String) {
        report.appendText(line + "\n")
    }

    private companion object {
        val report = java.io.File("build/online-report.txt")
        // Referência forte: o java.util.logging esquece a configuração de loggers sem referência.
        val quietLoggers = listOf("okhttp3.OkHttpClient", "okhttp3").map { java.util.logging.Logger.getLogger(it) }

        // MrBeast publica com várias faixas de áudio (original em inglês + dublagens).
        const val MULTI_AUDIO_VIDEO = "0e3GPea1Tyg"
        const val REGULAR_VIDEO = "jNQXAC9IVRw"
        const val CLIENTS_TO_TRY = 12
    }
}
