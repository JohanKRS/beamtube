package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRulesTest {
    private var next = 0

    private fun watched(title: String, channel: String) = Video(
        id = "v${next++}", title = title, channelName = channel, channelId = "c-$channel", thumbnailUrl = null, info = null,
        published = null, durationMs = 0, isLive = false, isUpcoming = false, isShorts = false, percentWatched = 0,
        playlistId = null, playlistIndex = 0,
    )

    private val history = listOf(
        watched("Star Wars: the history of the Jedi", "Lore Hub"),
        watched("Why Star Wars changed cinema", "Film Essays"),
        watched("Ranking every Star Wars movie", "Movie Talk"),
        watched("Como fazer pão de fermentação natural", "Cozinha da Vó"),
        watched("Pão caseiro fácil, sem sova", "Receitas Rápidas"),
        watched("O segredo do pão de fermentação natural", "Padaria em Casa"),
        // Só um canal fala de "Kubernetes": é o canal, não um tema.
        watched("Kubernetes in 10 minutes", "Cloud Guy"),
        watched("Kubernetes networking explained", "Cloud Guy"),
        watched("Kubernetes vs Docker", "Cloud Guy"),
        // O nome do canal no título não vira tema.
        watched("Lore Hub: the Sith", "Lore Hub"),
        watched("Lore Hub: the Empire", "Lore Hub"),
        watched("Lore Hub Q&A", "Lore Hub"),
    )

    @Test
    fun findsWhatRepeatsAcrossChannels() {
        val themes = ThemeRules.themes(history)
        val names = themes.map { it.name }
        // O par vale mais que as palavras sozinhas, e a forma dos títulos é mantida.
        assertEquals("Star Wars", names.first())
        assertTrue("Pão" in names)
        assertFalse(names.any { it.equals("Star", ignoreCase = true) || it.equals("Wars", ignoreCase = true) })
        assertFalse(names.any { it.equals("Kubernetes", ignoreCase = true) })
        assertFalse(names.any { it.contains("Lore", ignoreCase = true) || it.contains("Hub", ignoreCase = true) })
    }

    @Test
    fun skipsCommonWordsAndNumbers() {
        val names = ThemeRules.themes(history).map { it.name.lowercase() }
        listOf("the", "de", "como", "every", "why", "10").forEach { assertFalse(it, it in names) }
    }

    @Test
    fun hiddenThemesDoNotComeBack() {
        val names = ThemeRules.themes(history, exclude = setOf("star wars")).map { it.key }
        assertFalse("star wars" in names)
    }

    @Test
    fun shortAcronymsCount() {
        val ai = listOf(
            watched("AI is changing music", "A"),
            watched("The truth about AI", "B"),
            watched("AI tools I use every day", "C"),
        )
        assertEquals(listOf("AI"), ThemeRules.themes(ai).map { it.name })
    }
}
