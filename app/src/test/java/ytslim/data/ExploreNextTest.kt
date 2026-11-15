package ytslim.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A Explore abre com o sorteio preparado na abertura anterior (1.0.11, pedido do Johan): ele volta
 * do arquivo como foi guardado, com a mesma escolha dos vídeos na tela, e um velho não aparece.
 */
@RunWith(RobolectricTestRunner::class)
class ExploreNextTest {
    private fun video(id: String) = Video(
        id = id, title = "Video $id", channelName = "Channel $id", channelId = "UC$id", thumbnailUrl = null, info = "1K views",
        published = "3 years ago", durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private val draw = ExploreDraw(
        channels = listOf(
            ChannelVideos(FeaturedChannel("UCa", "Channel a", "@a", "https://x/a.jpg"), listOf(video("a1"), video("a2")), listOf(video("a3"))),
            ChannelVideos(FeaturedChannel("UCb", "Channel b"), listOf(video("b1")), emptyList()),
        ),
        ids = listOf("UCa", "UCb", "UCc"),
        hasMore = true,
        seed = 42,
    )

    @Before
    fun setUp() {
        Users.init(ApplicationProvider.getApplicationContext())
        Users.file(ExploreRepository.NEXT_FILE).delete()
    }

    @Test
    fun theSavedDrawComesBackTheSame() {
        runBlocking {
            assertNull(ExploreRepository.next())
            ExploreRepository.saveNext(draw)
            val saved = ExploreRepository.next()!!
            assertEquals(draw.ids, saved.ids)
            assertEquals(draw.channels, saved.channels)
            assertEquals(true, saved.hasMore)
            // O mesmo sorteio na tela: a escolha dos vídeos depende só da semente.
            val filters = ContentFilters()
            assertEquals(ExploreRules.screen(listOf(draw), filters).map { it.id }, ExploreRules.screen(listOf(saved), filters).map { it.id })
        }
    }

    @Test
    fun anOldOrEmptyDrawShowsNothing() {
        runBlocking {
            ExploreRepository.saveNext(draw, nowMs = System.currentTimeMillis() - ExploreRepository.NEXT_MAX_AGE_MS - 60_000)
            assertNull(ExploreRepository.next())
            ExploreRepository.saveNext(ExploreDraw(emptyList(), emptyList(), hasMore = false, seed = 1))
            assertNull(ExploreRepository.next())
        }
    }
}
