package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicRulesTest {
    private fun video(
        id: String,
        title: String = id,
        channel: String = "Some channel",
        channelId: String = "UC$id",
        shorts: Boolean = false,
        live: Boolean = false,
    ) = Video(
        id = id, title = title, channelName = channel, channelId = channelId, thumbnailUrl = null, info = null,
        published = null, durationMs = 600_000, isLive = live, isUpcoming = false, isShorts = shorts,
        percentWatched = 0, playlistId = null, playlistIndex = -1,
    )

    private val sports = Topic("Sports", listOf("futebol", "copa do mundo"))
    private val ai = Topic("AI", listOf("ai", "inteligência artificial"))

    @Test
    fun normalizesCaseAccentsAndPunctuation() {
        assertEquals("inteligencia artificial", TopicRules.normalize("  Inteligência   ARTIFICIAL! "))
        assertEquals("c plus plus em 2026", TopicRules.normalize("C++ em 2026"))
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertTrue(TopicRules.matches("O melhor do FUTEBOL brasileiro", sports))
        assertTrue(TopicRules.matches("Resumo da Copa do Mundo", sports))
        assertTrue(TopicRules.matches("How AI works", ai))
        assertTrue(TopicRules.matches("Inteligencia artificial na prática", ai))
        assertFalse(TopicRules.matches("My aim is true", ai))
        assertFalse(TopicRules.matches("Copa de café", sports))
    }

    @Test
    fun blockedChannelsAndExcludedTopicsAreLeftOut() {
        val filters = ContentFilters(excluded = listOf(sports), preferred = emptyList(), blocked = listOf(BlockedChannel("UCbad", "Bad")))
        assertTrue(TopicRules.isExcluded(video("x", channelId = "UCbad"), filters))
        assertTrue(TopicRules.isExcluded(video("y", channel = "Canal do Futebol"), filters))
        assertFalse(TopicRules.isExcluded(video("z", title = "Kotlin coroutines"), filters))
    }

    @Test
    fun blockedChannelsAreFoundByNameWhenTheIdIsMissing() {
        val filters = ContentFilters(excluded = emptyList(), preferred = emptyList(), blocked = listOf(BlockedChannel("UCbad", "Canal Ruim")))
        val noId = video("a").copy(channelId = null, channelName = "Canal ruim • 2M views • 1 year ago")
        assertTrue(TopicRules.isBlocked(noId, filters))
        assertFalse(TopicRules.isBlocked(noId.copy(channelName = "Outro canal • 1K views"), filters))
    }

    @Test
    fun recommendationsPutPreferredFirstWithinEachPage() {
        val filters = ContentFilters(excluded = listOf(sports), preferred = listOf(ai), blocked = emptyList())
        val page1 = listOf(
            video("1", "Cooking pasta"),
            video("2", "AI news"),
            video("3", "Futebol hoje"),
            video("4", "Short", shorts = true),
            video("5", "Live now", live = true),
            video("6", "AI agents"),
        )
        val page2 = listOf(video("7", "Gardening"), video("2", "AI news"), video("8", "Learning AI"))
        val ids = TopicRules.recommend(listOf(page1, page2), filters).map { it.id }
        assertEquals(listOf("2", "6", "1", "8", "7"), ids)
    }

    @Test
    fun keywordsAreSplitByCommas() {
        assertEquals(listOf("a", "b c", "d"), TopicRules.parseKeywords(" a, b c ,, d, a "))
    }
}
