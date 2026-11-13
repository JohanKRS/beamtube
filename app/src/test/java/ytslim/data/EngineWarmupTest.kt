package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** O motor preparado antes do primeiro vídeo: o que roda, em que ordem, e o que não é refeito. */
@RunWith(RobolectricTestRunner::class)
class EngineWarmupTest {
    private class Fake(
        var tokenExpired: Boolean = true,
        var player: String? = "https://www.youtube.com/s/player/abc/base.js",
        var failing: Set<String> = emptySet(),
        var solved: Boolean = true,
    ) : EngineWarmup.Parts {
        val calls = mutableListOf<String>()

        private fun step(name: String) {
            calls += name
            if (name in failing) throw IllegalStateException("$name failed")
        }

        override fun client(): Any = "TV".also { step("client") }
        override fun core(client: Any) = step("core")
        override fun auth() = step("auth")
        override fun tokenExpired(client: Any) = tokenExpired
        override fun token(client: Any): Boolean {
            step("token")
            tokenExpired = false
            return true
        }
        override fun playerUrl(): String? = player
        override fun solver(client: Any): Boolean {
            step("solver")
            return solved
        }
    }

    @Before
    fun forget() = EngineWarmup.reset()

    @Test
    fun theFirstRunPreparesEverythingInTheOrderAVideoNeedsIt() {
        val parts = Fake()
        val log = EngineWarmup.run(parts)
        assertEquals(listOf("client", "core", "auth", "token", "solver"), parts.calls)
        assertTrue(log, log.startsWith("client TV, core "))
    }

    @Test
    fun whatIsStillGoodIsNotDoneAgain() {
        val parts = Fake()
        EngineWarmup.run(parts)
        parts.calls.clear()
        EngineWarmup.run(parts)
        // O token ainda vale e o player do YouTube é o mesmo: só as conferências baratas.
        assertEquals(listOf("client", "core", "auth"), parts.calls)
    }

    @Test
    fun anExpiredTokenIsMadeAgain() {
        val parts = Fake()
        EngineWarmup.run(parts)
        parts.calls.clear()
        parts.tokenExpired = true
        EngineWarmup.run(parts)
        assertEquals(listOf("client", "core", "auth", "token"), parts.calls)
    }

    @Test
    fun aNewYouTubePlayerIsPreparedAgain() {
        val parts = Fake()
        EngineWarmup.run(parts)
        parts.calls.clear()
        parts.player = "https://www.youtube.com/s/player/xyz/base.js"
        EngineWarmup.run(parts)
        assertEquals(listOf("client", "core", "auth", "solver"), parts.calls)
    }

    @Test
    fun onePartFailingDoesNotStopTheOthers() {
        val parts = Fake(failing = setOf("core", "token"))
        val log = EngineWarmup.run(parts)
        assertEquals(listOf("client", "core", "auth", "token", "solver"), parts.calls)
        assertTrue(log, log.contains("core failed") && log.contains("po token failed"))
    }

    @Test
    fun aSolverThatDidNotWorkIsTriedAgainNextTime() {
        val parts = Fake(solved = false)
        val log = EngineWarmup.run(parts)
        assertTrue(log, log.contains("solver not ready"))
        parts.calls.clear()
        EngineWarmup.run(parts)
        assertTrue(parts.calls.contains("solver"))
    }

    @Test
    fun withoutTheEngineNothingRuns() {
        val parts = Fake(failing = setOf("client"))
        assertTrue(EngineWarmup.run(parts).startsWith("no client"))
        assertEquals(listOf("client"), parts.calls)
    }
}
