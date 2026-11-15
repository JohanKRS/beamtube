package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ytslim.data.CaptionLanguage.APP
import ytslim.data.CaptionLanguage.ENGLISH
import ytslim.data.CaptionLanguage.ORIGINAL

class SubtitleRulesTest {
    private fun caption(lang: String, auto: Boolean = false, translatable: Boolean = true, translated: Boolean = false) =
        Caption("https://x/api/timedtext?v=abc&lang=$lang&kind=${if (auto) "asr" else ""}", "text/vtt", lang, lang, auto, translatable, translated)

    @Test
    fun appLanguageThenOriginalThenEnglish() {
        // Vídeo em japonês, app em português: a tradução para o português, a fala em japonês e a feita em inglês.
        val list = listOf(caption("ja", auto = true), caption("en"))
        val options = SubtitleRules.options(list, "pt", "ja")
        assertEquals(listOf("pt", "ja", "en"), options.map { it.language })
        assertEquals(listOf(setOf(APP), setOf(ORIGINAL), setOf(ENGLISH)), options.map { it.choices })
        assertTrue(options[0].caption.translated)
        assertTrue(options[0].caption.url.endsWith("&lang=ja&kind=asr&tlang=pt"))
        assertFalse(options[1].caption.translated)
        assertEquals(false, options[2].caption.auto)
    }

    @Test
    fun anEnglishVideoShowsOnlyTheAppLanguageAndEnglish() {
        val list = listOf(caption("en", auto = true), caption("pt-BR"))
        val options = SubtitleRules.options(list, "pt", "en")
        assertEquals(listOf("pt", "en"), options.map { it.language })
        // A feita por pessoa em português vale mais que traduzir.
        assertEquals("pt-BR", options[0].caption.language)
        assertEquals(setOf(ORIGINAL, ENGLISH), options[1].choices)
    }

    @Test
    fun aVideoInTheAppLanguageShowsItAndEnglish() {
        val options = SubtitleRules.options(listOf(caption("pt", auto = true)), "pt", "pt")
        assertEquals(listOf("pt", "en"), options.map { it.language })
        assertEquals(setOf(APP, ORIGINAL), options[0].choices)
        assertTrue(options[1].caption.url.endsWith("tlang=en"))
    }

    @Test
    fun translationsStartFromHumanCaptionsInTheOriginalLanguage() {
        val list = listOf(caption("de", auto = true), caption("de"), caption("fr"))
        val options = SubtitleRules.options(list, "pt", "de")
        assertEquals("https://x/api/timedtext?v=abc&lang=de&kind=&tlang=pt", options.first().caption.url)
    }

    @Test
    fun dubbedSpeechIsNotTakenForEnglish() {
        // A automática em inglês de um vídeo japonês dublado é a fala da dublagem: o inglês vem da tradução.
        val list = listOf(caption("ja", auto = true), caption("en", auto = true))
        val english = SubtitleRules.options(list, "pt", "ja").first { ENGLISH in it.choices }
        assertTrue(english.caption.translated)
        assertTrue(english.caption.url.contains("lang=ja"))
    }

    @Test
    fun theOriginalIsNeverATranslation() {
        // Sem legenda em japonês, nada de "Original" traduzido de outra.
        val options = SubtitleRules.options(listOf(caption("en")), "pt", "ja")
        assertEquals(listOf("pt", "en"), options.map { it.language })
    }

    @Test
    fun theEngineTranslationsAreLeftOut() {
        val list = listOf(caption("en", auto = true), caption("pt", auto = true, translated = true))
        val pt = SubtitleRules.options(list, "pt", "en").first()
        assertTrue(pt.caption.translated)
        assertTrue(pt.caption.url.contains("lang=en"))
        assertEquals("en", SubtitleRules.originalLanguage(null, list.reversed()))
    }

    @Test
    fun withoutTranslationsTheVideoKeepsTheCaptionItHas() {
        val list = listOf(caption("de", translatable = false))
        val options = SubtitleRules.options(list, "pt", "ja")
        assertEquals(1, options.size)
        assertEquals("de", options[0].language)
        assertEquals(CaptionLanguage.entries.toSet(), options[0].choices)
        assertTrue(SubtitleRules.options(emptyList(), "pt", "ja").isEmpty())
    }

    @Test
    fun theSavedChoiceFallsBackToTheOriginal() {
        val options = SubtitleRules.options(listOf(caption("en", auto = true, translatable = false)), "pt", "en")
        assertEquals(listOf("en"), options.map { it.language })
        assertEquals("en", SubtitleRules.choose(options, APP)?.language)
        assertEquals("en", SubtitleRules.choose(options, ORIGINAL)?.language)
        assertNull(SubtitleRules.choose(emptyList(), ORIGINAL))
    }

    @Test
    fun withoutCaptionsInTheOriginalTheAppLanguageShows() {
        // Um vídeo em japonês só com a legenda em inglês feita por pessoa: "Original" fica com a do idioma do app.
        val options = SubtitleRules.options(listOf(caption("en")), "pt", "ja")
        assertEquals(listOf("pt", "en"), options.map { it.language })
        assertEquals("pt", SubtitleRules.choose(options, ORIGINAL)?.language)
        assertEquals("en", SubtitleRules.choose(options, ENGLISH)?.language)
        assertTrue(SubtitleRules.choose(options, APP)!!.caption.translated)
    }

    @Test
    fun translatedUrlsCarryOneTlang() {
        assertEquals("https://x/t?lang=en&tlang=pt", SubtitleRules.translatedUrl("https://x/t?lang=en", "pt"))
        assertEquals("https://x/t?lang=en&tlang=es", SubtitleRules.translatedUrl("https://x/t?lang=en&tlang=pt", "es"))
        assertEquals("https://x/t?tlang=pt", SubtitleRules.translatedUrl("https://x/t", "pt"))
    }

    @Test
    fun originalLanguageComesFromTheAudioTrackOrTheSpeech() {
        assertEquals("en‐US", SubtitleRules.originalLanguage("en‐US (original)", emptyList()))
        assertEquals("en", SubtitleRules.base("en‐US"))
        assertEquals("ja", SubtitleRules.originalLanguage(null, listOf(caption("pt"), caption("ja", auto = true))))
    }
}
