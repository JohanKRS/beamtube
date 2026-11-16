package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.MediaSubtitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ytslim.data.CaptionLanguage.APP
import ytslim.data.CaptionLanguage.ENGLISH
import ytslim.data.CaptionLanguage.ORIGINAL

/**
 * As legendas como o motor entrega ([PlaybackRepository.captionsOf]): as do vídeo, a automática com
 * "*" no nome, e as traduções da automática (com `tlang`, e a do próprio idioma com a URL dela).
 * Até a 1.0.11 o "*" tirava a automática da lista (achado do Johan, 2026-11-15).
 */
@RunWith(RobolectricTestRunner::class)
class EngineCaptionsTest {
    private class Sub(
        private val url: String,
        private val lang: String,
        private val name: String,
        private val kind: String? = null,
        private val vss: String = (if (kind == "asr") "a." else ".") + lang,
    ) : MediaSubtitle {
        override fun getBaseUrl() = url
        override fun isTranslatable() = true
        override fun getLanguageCode() = lang
        override fun getVssId() = vss
        override fun getName() = name
        override fun getMimeType() = "text/vtt"
        override fun getCodecs() = "wvtt"
        override fun getType() = kind
    }

    private fun url(lang: String, asr: Boolean = false) =
        "https://www.youtube.com/api/timedtext?v=WBFuccQSnyk&lang=$lang${if (asr) "&kind=asr" else ""}&fmt=vtt"

    /** O que o motor monta: as faixas do vídeo e depois a automática traduzida para cada idioma. */
    private fun engine(tracks: List<Sub>, origin: Sub, translations: List<String>) =
        tracks + translations.map { lang ->
            val sameLanguage = lang == origin.languageCode
            Sub(if (sameLanguage) origin.baseUrl else origin.baseUrl + "&tlang=$lang", lang, "$lang*", origin.type, origin.vssId)
        }

    @Test
    fun theAutomaticCaptionStaysAndTheTranslationsAreMarked() {
        // O clipe do exemplo do Johan: japonês, com legendas feitas em inglês e japonês, e a automática.
        val asr = Sub(url("ja", asr = true), "ja", "Japanese (auto-generated)*", kind = "asr")
        val subs = engine(
            listOf(Sub(url("ko"), "ko", "Korean"), Sub(url("en"), "en", "English"), Sub(url("ja"), "ja", "Japanese"), asr),
            origin = asr,
            translations = listOf("en", "ja", "pt"),
        )
        val captions = PlaybackRepository.captionsOf(subs)
        // A tradução para o japonês é a própria automática: sai repetida.
        assertEquals(listOf("ko", "en", "ja", "ja", "en", "pt"), captions.map { it.language })
        val auto = captions.single { it.auto && !it.translated }
        assertEquals("ja", auto.language)
        assertEquals("Japanese (auto-generated)", auto.name)
        assertEquals(listOf("en", "pt"), captions.filter { it.translated }.map { it.language })

        val original = SubtitleRules.originalLanguage(null, captions)
        assertEquals("ja", original)
        val options = SubtitleRules.options(captions, "pt", original)
        assertEquals("en", SubtitleRules.choose(options, ENGLISH)?.caption?.language)
        assertFalse(SubtitleRules.choose(options, ENGLISH)!!.caption.translated)
        // Original: a feita por pessoa em japonês, não a automática.
        val ja = SubtitleRules.choose(options, ORIGINAL)!!.caption
        assertEquals("ja", ja.language)
        assertFalse(ja.auto)
        // Português: tradução da japonesa feita por pessoa.
        val pt = SubtitleRules.choose(options, APP)!!.caption
        assertTrue(pt.translated)
        assertTrue(pt.url.endsWith("lang=ja&fmt=vtt&tlang=pt"))
    }

    @Test
    fun aVideoWithOnlyTheAutomaticCaptionHasCaptions() {
        // Até a 1.0.11, sem nenhuma legenda: a automática era a única, e saía como tradução.
        val asr = Sub(url("en", asr = true), "en", "English (auto-generated)*", kind = "asr")
        val captions = PlaybackRepository.captionsOf(engine(listOf(asr), origin = asr, translations = listOf("en", "pt", "es")))
        val original = SubtitleRules.originalLanguage(null, captions)
        assertEquals("en", original)
        val options = SubtitleRules.options(captions, "pt", original)
        assertEquals("en", SubtitleRules.choose(options, ORIGINAL)?.caption?.language)
        assertEquals("en", SubtitleRules.choose(options, ENGLISH)?.caption?.language)
        val pt = SubtitleRules.choose(options, APP)!!.caption
        assertEquals("pt", pt.language)
        assertTrue(pt.translated)
    }
}
