package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExploreRulesTest {
    private fun channel(id: String, name: String, handle: String? = null) = FeaturedChannel(id, name, handle)

    private fun video(id: String, channel: String? = null, watched: Int = 0, shorts: Boolean = false) = Video(
        id = id, title = "Video $id", channelName = channel, channelId = null, thumbnailUrl = null, info = null,
        published = null, durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = shorts, percentWatched = watched,
        playlistId = null, playlistIndex = 0,
    )

    @Test
    fun siblingsOfTheSameCreatorAreNotDiscoveries() {
        // Traduções, segundo canal e podcast do mesmo criador (casos reais das páginas).
        assertTrue(ExploreRules.isSibling("Veritasium", "@veritasium", channel("a", "Veritasium em Português", "@VeritasiumPT")))
        assertTrue(ExploreRules.isSibling("Veritasium", "@veritasium", channel("b", "2veritasium", "@2veritasium")))
        assertTrue(ExploreRules.isSibling("Kurzgesagt – In a Nutshell", "@kurzgesagt", channel("c", "Dinge Erklärt – Kurzgesagt")))
        assertTrue(ExploreRules.isSibling("Kurzgesagt – In a Nutshell", null, channel("d", "Nightshift – Kurzgesagt After Dark")))
        assertTrue(ExploreRules.isSibling("Mentour Pilot", "@MentourPilot", channel("e", "Mentour's Black Box", "@MentourBlackBox")))
        assertTrue(ExploreRules.isSibling("Tom Scott", "@TomScottGo", channel("f", "Lateral with Tom Scott")))
        // Canais de outros criadores ficam.
        assertFalse(ExploreRules.isSibling("Veritasium", "@veritasium", channel("g", "Mentour Pilot", "@MentourPilot")))
        assertFalse(ExploreRules.isSibling("3Blue1Brown", "@3blue1brown", channel("h", "Numberphile", "@numberphile")))
        assertFalse(ExploreRules.isSibling("The Why Files", null, channel("i", "The Infographics Show")))
    }

    @Test
    fun candidatesLeaveOutSubscriptionsBlockedAndSiblings() {
        val sources = listOf(
            FeaturedSource("Veritasium", "@veritasium", 0, listOf(channel("pt", "Veritasium em Português"), channel("mp", "Mentour Pilot"), channel("sub", "Already Followed"))),
            FeaturedSource("3Blue1Brown", "@3blue1brown", 0, listOf(channel("np", "Numberphile"), channel("mp", "Mentour Pilot"), channel("bad", "Blocked One"))),
        )
        val candidates = ExploreRules.candidates(sources, subscribed = setOf("sub"), blocked = setOf("bad"))
        assertEquals(mapOf("mp" to 2, "np" to 1), candidates.associate { it.channel.id to it.weight })
    }

    @Test
    fun drawsAvoidTheLastChannelsAndFavourTheOftenFeatured() {
        val candidates = listOf(Candidate(channel("a", "A"), 10), Candidate(channel("b", "B"), 1), Candidate(channel("c", "C"), 1))
        // O que acabou de aparecer só volta se faltar canal.
        repeat(20) { seed ->
            val drawn = ExploreRules.draw(candidates, 2, avoid = setOf("a"), random = Random(seed)).map { it.id }
            assertEquals(setOf("b", "c"), drawn.toSet())
        }
        assertEquals(3, ExploreRules.draw(candidates, 3, avoid = setOf("a"), random = Random(1)).size)
        // Destacado por 10 canais sai quase sempre num sorteio de um.
        val firsts = (0 until 200).count { ExploreRules.draw(candidates, 1, emptySet(), Random(it)).single().id == "a" }
        assertTrue("saiu $firsts de 200", firsts > 140)
    }

    @Test
    fun eachDrawIsDifferent() {
        val candidates = (1..60).map { Candidate(channel("c$it", "Channel $it"), 1 + it % 3) }
        val one = ExploreRules.draw(candidates, 10, emptySet(), Random(1)).map { it.id }
        val two = ExploreRules.draw(candidates, 10, avoid = one.toSet(), random = Random(2)).map { it.id }
        assertTrue(one.intersect(two.toSet()).isEmpty())
    }

    @Test
    fun picksTwoRecentAndTwoOlderAndFillsWhenOneSideIsShort() {
        val c = channel("x", "X")
        val recent = (1..10).map { video("r$it") }
        val older = (1..10).map { video("o$it") }
        val picked = ExploreRules.pick(ChannelVideos(c, recent, older), ContentFilters(), Random(3))
        assertEquals(4, picked.size)
        assertEquals(listOf(true, false, true, false), picked.map { it.id.startsWith("r") })

        val fewOlder = ExploreRules.pick(ChannelVideos(c, recent, listOf(video("o1"))), ContentFilters(), Random(3))
        assertEquals(4, fewOlder.size)
        assertEquals(1, fewOlder.count { it.id.startsWith("o") })
    }

    @Test
    fun watchedShortsAndRepeatsStayOut() {
        val c = channel("x", "X")
        val recent = listOf(video("seen", watched = 95), video("short", shorts = true), video("ok"))
        val older = listOf(video("ok"), video("old"))
        val picked = ExploreRules.pick(ChannelVideos(c, recent, older), ContentFilters(), Random(1)).map { it.id }
        assertEquals(setOf("ok", "old"), picked.toSet())
    }

    @Test
    fun mixTakesOneFromEachChannelPerRound() {
        val mixed = ExploreRules.mix(listOf(listOf(video("a1"), video("a2")), listOf(video("b1")), listOf(video("c1"), video("a1"))))
        assertEquals(listOf("a1", "b1", "c1", "a2"), mixed.map { it.id })
    }

    @Test
    fun onlyTheChannelsOwnVideosWithItsIdAndName() {
        val c = channel("UC3b", "3Blue1Brown")
        val videos = listOf(video("own", "3Blue1Brown"), video("collab", "3Blue1Brown and PolyaMath"), video("guest", "Numberphile and 3Blue1Brown"), video("bare"))
        val own = ExploreRules.ownVideos(c, videos)
        assertEquals(listOf("own", "collab", "bare"), own.map { it.id })
        assertTrue(own.all { it.channelId == "UC3b" })
        assertEquals("3Blue1Brown", own.last().channelName)
    }

    @Test
    fun handleIsTheOneThatRepeats() {
        assertEquals("@veritasium", ExploreRules.handleOf(listOf("Veritasium • @veritasium", "Veritasium and 2swap • @veritasium", "fern and Veritasium • @fern-tv", null)))
        assertEquals("@VeritasiumPT", ExploreRules.handleOf(listOf("@VeritasiumPT • 209K subscribers")))
    }
}
