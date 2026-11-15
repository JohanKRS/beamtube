package ytslim.ui

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ytslim.data.SettingsRepository
import ytslim.data.Users

/**
 * As abas que abrem por trás logo depois das Inscrições ([TabWarmup]) sobem sem derrubar o app.
 *
 * Na 1.0.11 o `seen` das Recomendações era declarado depois do `init`. O viewModelScope roda no
 * Main.immediate, então a coleta que o `init` começa rodou ali mesmo, dentro do construtor, e achou
 * o `seen` nulo: o app fechava 1,7 s depois de abrir, toda vez (log da TCL, 2026-11-15).
 */
@RunWith(RobolectricTestRunner::class)
class TabViewModelsTest {
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SettingsRepository.init(context)
        Users.init(context)
    }

    /** O que chegaria ao "FATAL EXCEPTION" no aparelho: uma exceção sem dono numa coroutine. */
    private fun crashesWhile(block: () -> Unit): List<Throwable> {
        val thread = Thread.currentThread()
        val before = thread.uncaughtExceptionHandler
        val crashes = mutableListOf<Throwable>()
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> crashes += e }
        try {
            block()
            repeat(20) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(25)
            }
        } finally {
            thread.uncaughtExceptionHandler = before
        }
        return crashes
    }

    private fun opens(create: () -> ViewModel) = assertEquals(emptyList<Throwable>(), crashesWhile { create() })

    @Test
    fun recommendedOpensAndFollowsTheGrid() {
        assertEquals(
            emptyList<Throwable>(),
            crashesWhile {
                val vm = RecommendedViewModel()
                vm.seen(4)
                vm.onScreen(true)
                vm.onScreen(false)
            },
        )
    }

    @Test
    fun exploreOpens() = opens { ExploreViewModel() }

    @Test
    fun historyOpens() = opens { HistoryViewModel() }

    @Test
    fun playlistsOpen() = opens { PlaylistsViewModel() }
}
