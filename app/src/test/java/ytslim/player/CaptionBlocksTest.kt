package ytslim.player

import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A legenda automática do YouTube rola: cada linha nova entra embaixo e empurra a de cima. O Johan
 * pediu blocos (2026-11-04): duas linhas de uma vez, ou uma quando só há uma, trocadas inteiras.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class CaptionBlocksTest {
    /** Uma legenda no formato que o YouTube manda (fmt=vtt): o texto de cima vazio é um espaço. */
    private fun vtt(vararg cues: String) = (listOf("WEBVTT", "Kind: captions", "Language: en", "") + cues).joinToString("\n")

    private fun cue(from: String, to: String, top: String, bottom: String) =
        "$from --> $to align:start position:0%\n$top\n$bottom\n"

    /** A legenda automática de verdade: as linhas com as palavras marcadas e as trocas de 10 ms. */
    private val rolling = vtt(
        cue("00:00:00.000", "00:00:01.790", " ", "hi<00:00:00.240><c> everyone</c><00:00:00.599><c> welcome</c>"),
        cue("00:00:01.790", "00:00:01.800", "hi everyone welcome", " "),
        cue("00:00:01.800", "00:00:04.309", "hi everyone welcome", "back<00:00:02.040><c> to</c><00:00:02.200><c> the</c><00:00:02.360><c> channel</c>"),
        cue("00:00:04.309", "00:00:04.319", "back to the channel", " "),
        cue("00:00:04.319", "00:00:06.470", "back to the channel", "today<00:00:04.600><c> we</c><00:00:04.800><c> are</c><00:00:05.000><c> making</c>"),
        cue("00:00:06.470", "00:00:06.480", "today we are making", " "),
        cue("00:00:06.480", "00:00:09.000", "today we are making", "a<00:00:06.700><c> simple</c><00:00:07.000><c> soup</c>"),
        cue("00:00:09.000", "00:00:09.010", "a simple soup", " "),
        cue("00:00:09.010", "00:00:11.000", "a simple soup", "with<00:00:09.300><c> five</c><00:00:09.600><c> things</c>"),
        // Uma pausa na fala: nada na tela até 0:20.
        cue("00:00:20.000", "00:00:22.000", " ", "first<00:00:20.300><c> chop</c><00:00:20.600><c> the</c><00:00:20.900><c> onion</c>"),
    )

    private fun parse(text: String, options: SubtitleParser.OutputOptions = SubtitleParser.OutputOptions.allCues()): List<CuesWithTiming> {
        val out = mutableListOf<CuesWithTiming>()
        BlockCaptionParser(WebvttParser()).parse(text.toByteArray(), options) { out += it }
        return out.sortedBy { it.startTimeUs }
    }

    private fun List<CuesWithTiming>.shown() = map { Triple(it.startTimeUs / 1000, it.endTimeUs / 1000, it.cues.single().text.toString()) }

    @Test
    fun twoLinesAtATimeReplacedTogether() {
        assertEquals(
            listOf(
                Triple(0L, 4_319L, "hi everyone welcome\nback to the channel"),
                Triple(4_319L, 9_010L, "today we are making\na simple soup"),
                // A linha antes da pausa fica sozinha: a seguinte só vem depois.
                Triple(9_010L, 11_000L, "with five things"),
                Triple(20_000L, 22_000L, "first chop the onion"),
            ),
            parse(rolling).shown(),
        )
    }

    @Test
    fun theBlocksSitWhereTheCaptionWas() {
        val block = parse(rolling).first().cues.single()
        // A última linha de baixo, como a legenda do YouTube; o CaptionRules centraliza depois.
        assertEquals(-1f, block.line)
        assertEquals(Cue.LINE_TYPE_NUMBER, block.lineType)
    }

    @Test
    fun startingInTheMiddleStillGetsEveryBlock() {
        // Voltar a um vídeo começado: o extrator pede a partir do ponto, mas o bloco depende do todo.
        assertEquals(parse(rolling).shown(), parse(rolling, SubtitleParser.OutputOptions.cuesAfterThenRemainingCuesBefore(5_000_000)).shown())
    }

    @Test
    fun aSlowSecondLineGetsItsOwnBlock() {
        val slow = vtt(
            cue("00:00:00.000", "00:00:07.000", " ", "one"),
            cue("00:00:07.000", "00:00:09.000", "one", "two"),
            cue("00:00:09.000", "00:00:11.000", "two", "three"),
            cue("00:00:11.000", "00:00:13.000", "three", "four"),
            cue("00:00:13.000", "00:00:15.000", "four", "five"),
        )
        // "two" começa 7 s depois de "one": cedo demais para mostrar junto.
        assertEquals(
            listOf(
                Triple(0L, 7_000L, "one"),
                Triple(7_000L, 11_000L, "two\nthree"),
                Triple(11_000L, 15_000L, "four\nfive"),
            ),
            parse(slow).shown(),
        )
    }

    @Test
    fun captionsMadeByPeoplePassAsTheyCame() {
        val manual = vtt(
            cue("00:00:01.000", "00:00:03.000", "Where are you going?", "Home."),
            cue("00:00:03.500", "00:00:05.000", "Now?", "Right now."),
            "00:00:06.000 --> 00:00:08.000\nI'll come with you.\n",
            cue("00:00:08.000", "00:00:10.000", "Fine.", "Let's go."),
        )
        val parsed = mutableListOf<CuesWithTiming>()
        WebvttParser().parse(manual.toByteArray(), SubtitleParser.OutputOptions.allCues()) { parsed += it }
        assertEquals(parsed.shown(), parse(manual).shown())
    }
}
