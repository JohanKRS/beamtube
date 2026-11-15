package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A lista não troca com a aba na tela (1.0.11, pedido do Johan). */
class QuietSwapTest {
    @Test
    fun offScreenTheNewListGoesInAtOnce() {
        val swap = QuietSwap<String>()
        assertEquals("fresh", swap.arrived("fresh"))
        // Nada ficou esperando: abrir e sair da aba não troca mais nada.
        assertNull(swap.onScreen(true))
        assertNull(swap.onScreen(false))
    }

    @Test
    fun onScreenItWaitsUntilTheTabLeaves() {
        val swap = QuietSwap<String>()
        swap.onScreen(true)
        assertNull(swap.arrived("fresh"))
        assertEquals("fresh", swap.onScreen(false))
        // Entrou uma vez só.
        assertNull(swap.onScreen(true))
        assertNull(swap.onScreen(false))
    }

    @Test
    fun theWaitingListCanBeTakenForTheEndOfTheList() {
        val swap = QuietSwap<String>()
        swap.onScreen(true)
        swap.arrived("first")
        // Uma mais nova fica no lugar da que esperava.
        swap.arrived("second")
        assertEquals("second", swap.take())
        assertNull(swap.take())
        assertNull(swap.onScreen(false))
    }
}
