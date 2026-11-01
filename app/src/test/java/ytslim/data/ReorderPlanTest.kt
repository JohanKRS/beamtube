package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReorderPlanTest {
    /** Aplica os movimentos como o YouTube aplica: tira o vídeo e põe antes do sucessor (ou no fim). */
    private fun apply(original: List<String>, moves: List<Move>): List<String> {
        val list = original.toMutableList()
        for (move in moves) {
            list.remove(move.videoId)
            if (move.successor == null) list.add(move.videoId) else list.add(list.indexOf(move.successor), move.videoId)
        }
        return list
    }

    /** Tamanho da maior subsequência crescente, por força bruta de programação dinâmica. */
    private fun lisLength(values: List<Int>): Int {
        val best = IntArray(values.size) { 1 }
        for (i in values.indices) for (j in 0 until i) if (values[j] < values[i]) best[i] = maxOf(best[i], best[j] + 1)
        return best.maxOrNull() ?: 0
    }

    @Test
    fun sameOrderNeedsNoMoves() {
        assertEquals(emptyList<Move>(), ReorderPlan.moves(listOf("a", "b", "c"), listOf("a", "b", "c")))
        assertEquals(emptyList<Move>(), ReorderPlan.moves(emptyList(), emptyList()))
    }

    @Test
    fun oneVideoUpOrDownIsOneMove() {
        assertEquals(listOf(Move("c", "a")), ReorderPlan.moves(listOf("a", "b", "c"), listOf("c", "a", "b")))
        assertEquals(listOf(Move("a", null)), ReorderPlan.moves(listOf("a", "b", "c"), listOf("b", "c", "a")))
        assertEquals(listOf(Move("b", "a")), ReorderPlan.moves(listOf("a", "b", "c"), listOf("b", "a", "c")))
    }

    @Test
    fun randomOrdersArriveWithTheFewestMoves() {
        val random = Random(42)
        repeat(500) {
            val n = random.nextInt(1, 40)
            val original = (0 until n).map { "v$it" }
            val final = original.shuffled(random)
            val moves = ReorderPlan.moves(original, final)
            assertEquals(final, apply(original, moves))
            val positions = final.map { original.indexOf(it) }
            assertEquals(n - lisLength(positions), moves.size)
            assertTrue(moves.all { it.successor != it.videoId })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesADifferentSetOfVideos() {
        ReorderPlan.moves(listOf("a", "b"), listOf("a", "c"))
    }
}
