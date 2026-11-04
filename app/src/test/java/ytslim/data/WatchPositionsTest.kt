package ytslim.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Voltar a um vídeo começado retoma de onde ele parou (achado do Johan na TV, 2026-11-04). */
@RunWith(RobolectricTestRunner::class)
class WatchPositionsTest {
    @Before
    fun setUp() = runTest {
        WatchPositions.init(ApplicationProvider.getApplicationContext())
        WatchPositions.clear()
    }

    @Test
    fun aVideoStartedHereContinuesWhereItStopped() = runTest {
        WatchPositions.save("abc", positionMs = 312_000, durationMs = 900_000)
        assertEquals(312_000, WatchPositions.startAt("abc", hintMs = 0))
    }

    @Test
    fun theYouTubeTimeCountsForVideosStartedElsewhere() = runTest {
        assertEquals(95_000, WatchPositions.startAt("other", hintMs = 95_000))
        assertEquals(0, WatchPositions.startAt("never", hintMs = 0))
    }

    @Test
    fun whatThisDeviceSawWinsOverTheCard() = runTest {
        // O card pode ser de uma lista lida antes de ver mais um pedaço aqui.
        WatchPositions.save("abc", positionMs = 400_000, durationMs = 900_000)
        assertEquals(400_000, WatchPositions.startAt("abc", hintMs = 120_000))
    }

    @Test
    fun aVideoSeenToTheEndStartsOver() = runTest {
        WatchPositions.save("abc", positionMs = 890_000, durationMs = 900_000)
        assertEquals(0, WatchPositions.startAt("abc", hintMs = 0))
    }

    @Test
    fun theSpotsSurviveTheAppClosing() = runTest {
        WatchPositions.save("abc", positionMs = 200_000, durationMs = 900_000)
        // Abrir de novo lê do arquivo.
        WatchPositions.init(ApplicationProvider.getApplicationContext())
        val fresh = WatchPositions::class.java.getDeclaredField("spots").apply { isAccessible = true }
        fresh.set(WatchPositions, null)
        assertEquals(200_000, WatchPositions.startAt("abc", hintMs = 0))
    }

    @Test
    fun theRules() {
        assertEquals(0, ResumeRules.startAt(positionMs = 6_000, durationMs = 600_000))
        assertEquals(60_000, ResumeRules.startAt(positionMs = 60_000, durationMs = 600_000))
        assertEquals(0, ResumeRules.startAt(positionMs = 580_000, durationMs = 600_000))
        // Sem a duração (o tempo do card), só o começo conta.
        assertEquals(580_000, ResumeRules.startAt(positionMs = 580_000, durationMs = 0))
    }
}
