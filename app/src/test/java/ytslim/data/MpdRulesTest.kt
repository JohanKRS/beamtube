package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MpdRulesTest {
    private val video = """<AdaptationSet id="0" mimeType="video/webm" subsegmentAlignment="true"><Role schemeIdUri="urn:mpeg:DASH:role:2011" value="main" /><Representation id="248" codecs="vp9"><BaseURL>https://v/248</BaseURL></Representation></AdaptationSet>"""
    private val audio = """<AdaptationSet id="1" mimeType="audio/mp4" lang="ja"><Representation id="140" codecs="mp4a.40.2"><BaseURL>https://v/140</BaseURL></Representation></AdaptationSet>"""
    private fun caption(id: Int, lang: String) =
        """<AdaptationSet id="$id" mimeType="text/vtt" lang="$lang"><Role schemeIdUri="urn:mpeg:DASH:role:2011" value="subtitle" /><Representation id="$id" bandwidth="268" codecs="wvtt"><BaseURL>https://www.youtube.com/api/timedtext?v=x&amp;lang=ja&amp;fmt=vtt&amp;tlang=$lang</BaseURL></Representation></AdaptationSet>"""

    private fun mpd(vararg sets: String) =
        """<?xml version="1.0" encoding="UTF-8"?><MPD><Period>${sets.joinToString("\n")}</Period></MPD>"""

    @Test
    fun captionsLeaveTheManifestAndTheRestStays() {
        val xml = mpd(video, audio, caption(2, "日本語"), caption(3, "English*"))
        val out = MpdRules.withoutCaptions(xml.toByteArray()).toString(Charsets.UTF_8)
        assertFalse(out.contains("text/vtt"))
        assertFalse(out.contains("timedtext"))
        assertTrue(out.contains(video))
        assertTrue(out.contains(audio))
        assertEquals(mpd(video, audio), out.replace("\n</Period>", "</Period>"))
    }

    @Test
    fun aManifestWithoutCaptionsIsUntouched() {
        val bytes = mpd(video, audio).toByteArray()
        assertSame(bytes, MpdRules.withoutCaptions(bytes))
    }
}
