package ytslim.ui

import android.os.Looper
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ytslim.data.SuggestedTopic
import ytslim.ui.mobile.MobileUi
import ytslim.ui.tv.TvUi
import kotlin.math.abs

/**
 * A tela de tópicos (Johan, 1.0.4): marcar uma sugestão não pode mudar nenhuma de lugar. O visto
 * deixava a marcada mais larga e empurrava as seguintes para a linha de baixo. E as linhas vão até a
 * borda direita, menos a última.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopicsLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val picks = listOf(
        SuggestedTopic.DOCUMENTARIES, SuggestedTopic.HISTORY, SuggestedTopic.SCIENCE, SuggestedTopic.SPACE,
        SuggestedTopic.ENGINEERING, SuggestedTopic.SLEEP, SuggestedTopic.AI, SuggestedTopic.CARS,
    )

    private fun loadedVm(): TopicsViewModel {
        val vm = TopicsViewModel()
        repeat(100) {
            if (vm.state.value.loaded) return vm
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        return vm
    }

    /** Onde está cada sugestão, pelo nome. */
    private fun chips(vm: TopicsViewModel): Map<String, Rect> {
        compose.waitForIdle()
        val res = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        val labels = vm.state.value.shown.map { res.getString(it.label) }.toSet()
        return compose.onAllNodes(hasClickAction(), useUnmergedTree = false).fetchSemanticsNodes()
            .mapNotNull { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: return@mapNotNull null
                // Sem o corte da rolagem: no celular as últimas linhas ficam abaixo da tela.
                if (text in labels) text to Rect(node.positionInRoot, node.size.toSize()) else null
            }
            .toMap()
            .also { assertEquals("todas as sugestões na tela", labels.size, it.size) }
    }

    private fun assertStableAndJustified(vm: TopicsViewModel) {
        val before = chips(vm)
        picks.forEach(vm::toggle)
        val marked = chips(vm)
        assertEquals("marcar não muda nada de lugar", before, marked)
        picks.take(3).forEach(vm::toggle)
        assertEquals("desmarcar também não", before, chips(vm))

        // As linhas, menos a última, terminam todas na mesma borda.
        val rows = before.values.groupBy { it.top }.toSortedMap().values.toList()
        assertTrue("mais de uma linha", rows.size > 2)
        val ends = rows.dropLast(1).map { row -> row.maxOf { it.right } }
        ends.forEach { assertTrue("linha até a borda: $ends", abs(it - ends.first()) <= 1f) }
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun tvSuggestionsStayInPlace() {
        val vm = loadedVm()
        compose.setContent { TvUi.Theme { TvUi.Topics(vm, firstRun = true) {} } }
        assertStableAndJustified(vm)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneSuggestionsStayInPlace() {
        val vm = loadedVm()
        compose.setContent { MobileUi.Theme { MobileUi.Topics(vm, firstRun = true) {} } }
        assertStableAndJustified(vm)
    }
}
