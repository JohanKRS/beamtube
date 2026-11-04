package ytslim.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.PlaceholderDataSource
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class StartLowSelectionTest {
    private fun video(height: Int, bitrate: Int) = Format.Builder()
        .setId("$height")
        .setSampleMimeType(MimeTypes.VIDEO_VP9)
        .setWidth(height * 16 / 9)
        .setHeight(height)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .build()

    private val group = TrackGroup(video(720, 1_500_000), video(1080, 3_000_000), video(2160, 16_000_000))

    /** Um pedaço já carregado na fila (o conteúdo não importa para a seleção). */
    private fun piece(): MediaChunk = object : MediaChunk(
        PlaceholderDataSource.INSTANCE, DataSpec(Uri.parse("https://example.com/v")), video(1080, 3_000_000),
        C.SELECTION_REASON_INITIAL, null, 0, 5_000_000, 0, null,
    ) {
        override fun isLoadCompleted() = true
        override fun cancelLoad() = Unit
        override fun load() = Unit
    }

    private fun StartLowSelection.next(queue: List<MediaChunk>): Int {
        updateSelectedTrack(0, 0, 0, queue, emptyArray<MediaChunkIterator>())
        return selectedFormat.height
    }

    @Test
    fun firstPieceAtTheStartTrackThenTheTargetForGood() {
        val selection = StartLowSelection(group, intArrayOf(2, 1), StartLowSelection.TYPE)
        assertEquals(1080, selection.selectedFormat.height)
        assertEquals(1080, selection.next(emptyList()))
        val loaded = listOf(piece())
        assertEquals(2160, selection.next(loaded))
        assertEquals(2160, selection.next(loaded + loaded))
    }

    @Test
    fun aJumpOutsideTheBufferStartsLowAgain() {
        val selection = StartLowSelection(group, intArrayOf(1, 2), StartLowSelection.TYPE)
        assertEquals(2160, selection.next(listOf(piece())))
        assertEquals(1080, selection.next(emptyList()))
    }
}
