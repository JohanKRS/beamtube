package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** As sugestões da tela de tópicos (1.0): palavras válidas e que pegam títulos nas três línguas. */
class TopicCatalogTest {
    private fun matches(title: String, topic: SuggestedTopic) = TopicRules.matches(title, topic.topic("x"))

    @Test
    fun everyKeywordIsUsableAndNotRepeated() {
        for (topic in SuggestedTopic.entries) {
            val normalized = topic.keywords.map(TopicRules::normalize)
            assertTrue("${topic.name} has an empty keyword", normalized.none { it.isEmpty() })
            assertEquals("${topic.name} repeats a keyword", normalized.size, normalized.toSet().size)
        }
    }

    @Test
    fun theStoredTopicPointsBackToTheSuggestion() {
        val stored = SuggestedTopic.SPACE.topic("Espaço")
        assertEquals("Espaço", stored.name)
        assertEquals(SuggestedTopic.SPACE, SuggestedTopic.byId(stored.id))
        assertEquals(null, SuggestedTopic.byId(null))
        assertEquals(null, SuggestedTopic.byId("GONE"))
    }

    @Test
    fun catchesTitlesInEnglishPortugueseAndSpanish() {
        assertTrue(matches("What happens inside a black hole?", SuggestedTopic.SPACE))
        assertTrue(matches("O que acontece dentro de um buraco negro", SuggestedTopic.SPACE))
        assertTrue(matches("¿Qué hay dentro de un agujero negro?", SuggestedTopic.SPACE))
        assertTrue(matches("Melhores momentos: Flamengo 2 x 1 Palmeiras | Brasileirão", SuggestedTopic.SPORTS))
        assertTrue(matches("Receta de pan casero en 10 minutos", SuggestedTopic.COOKING))
        assertTrue(matches("Documental: la Segunda Guerra Mundial en color", SuggestedTopic.DOCUMENTARIES))
        assertTrue(matches("Documental: la Segunda Guerra Mundial en color", SuggestedTopic.HISTORY))
        assertTrue(matches("Reagindo aos piores vídeos da internet", SuggestedTopic.REACTIONS))
        assertTrue(matches("10 hours of rain sounds for sleep", SuggestedTopic.SLEEP))
    }

    @Test
    fun leavesOutAmbiguousWords() {
        // "esa" (espanhol: "essa") não é a agência espacial; "taller" (inglês) não é oficina.
        assertFalse(matches("Esa es la pregunta", SuggestedTopic.SPACE))
        assertFalse(matches("Why are people getting taller?", SuggestedTopic.DIY))
        assertFalse(matches("The engineering challenge of the century", SuggestedTopic.PRANKS))
    }

    @Test
    fun theLessStepStartsWithWhatPeopleUsuallyLeaveOut() {
        assertEquals(SuggestedTopic.entries.size, SuggestedTopic.forLess.toSet().size)
        assertTrue(SuggestedTopic.forLess.first().lessFirst)
        assertFalse(SuggestedTopic.forMore.first().lessFirst)
    }
}
