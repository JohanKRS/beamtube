package ytslim.ui.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.ui.HomeTab

/**
 * As abas do celular lado a lado ([MobileUi.Home]): uma aba composta fora de hora (a que aparece
 * pela borda ao deslizar, ou a que o pager adianta) não conta como aberta. Era assim que o aviso
 * da Explore aparecia ao chegar nos Recomendados (Johan, 1.0.0). O adiantamento do pager não roda
 * no Robolectric; o gesto pela metade mostra a mesma situação.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SwipeTabsTest {
    @get:Rule val compose = createComposeRule()

    private var tab by mutableStateOf(HomeTab.RECOMMENDED)
    private val shown = mutableMapOf<HomeTab, Boolean>()

    private fun open() {
        compose.setContent {
            MobileUi.Theme {
                MobileUi.Home(tab, { tab = it }, {}) { current ->
                    val isShown = LocalTabShown.current
                    SideEffect { shown[current] = isShown }
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun aTabPeekingInFromTheSideIsNotOpen() {
        open()
        compose.onRoot().performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-width / 30f, 0f)) }
        }
        compose.waitForIdle()
        // A Explore já está composta, aparecendo pela borda, mas a aba aberta ainda é a dos Recomendados.
        assertEquals(false, shown[HomeTab.EXPLORE])
        assertEquals(true, shown[HomeTab.RECOMMENDED])
        compose.onRoot().performTouchInput { up() }
    }

    @Test
    fun theTabSwipedToIsOpen() {
        open()
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(HomeTab.EXPLORE, tab)
        assertEquals(true, shown[HomeTab.EXPLORE])
    }
}
