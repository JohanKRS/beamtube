package ytslim.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Os cartões só deslizam quando vídeos saem da lista ou chegam no fim; numa lista nova, não voam. */
class ListMotionTest {
    private val before = listOf("a", "b", "c", "d")

    @Test
    fun removalsAndTheNextPageSlide() {
        assertTrue(ListMotion.animates(before, before))
        assertTrue(ListMotion.animates(before, listOf("a", "c", "d")))
        assertTrue(ListMotion.animates(before, listOf("b", "d")))
        assertTrue(ListMotion.animates(before, before + listOf("e", "f")))
        assertTrue(ListMotion.animates(before, listOf("a", "c", "d", "e")))
        assertTrue(ListMotion.animates(emptyList(), before))
    }

    @Test
    fun aRefreshedListDoesNotFly() {
        // Vídeos novos no topo e os de antes descendo (Inscrições, Recomendações atualizadas).
        assertFalse(ListMotion.animates(before, listOf("x", "a", "b", "c")))
        // As recomendações voltam embaralhadas.
        assertFalse(ListMotion.animates(before, listOf("c", "a", "y", "b")))
        assertFalse(ListMotion.animates(before, listOf("b", "a")))
    }
}
