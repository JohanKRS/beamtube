package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleRulesTest {
    private fun caption(lang: String, auto: Boolean = false) = Caption("https://x/$lang$auto", "text/vtt", lang, lang, auto)

    @Test
    fun prefersHumanCaptionsInTheDeviceLanguage() {
        val list = listOf(caption("en", auto = true), caption("en"), caption("pt-BR"))
        assertEquals("pt-BR", SubtitleRules.pick(list, "pt", "en")?.language)
    }

    @Test
    fun thenTheOriginalLanguage() {
        val list = listOf(caption("de"), caption("en"), caption("en", auto = true))
        assertEquals("en", SubtitleRules.pick(list, "pt", "en")?.let { it.language to it.auto }?.first)
        assertEquals(false, SubtitleRules.pick(list, "pt", "en")?.auto)
    }

    @Test
    fun autoCaptionsOnlyWhenThereIsNothingElse() {
        val list = listOf(caption("es", auto = true), caption("en", auto = true))
        assertEquals("en", SubtitleRules.pick(list, "pt", "en")?.language)
        assertNull(SubtitleRules.pick(emptyList(), "pt", "en"))
    }

    @Test
    fun originalLanguageComesFromTheAudioTrackOrTheSpeech() {
        assertEquals("en‐US", SubtitleRules.originalLanguage("en‐US (original)", emptyList()))
        assertEquals("en", SubtitleRules.base("en‐US"))
        assertEquals("ja", SubtitleRules.originalLanguage(null, listOf(caption("pt"), caption("ja", auto = true))))
    }
}
