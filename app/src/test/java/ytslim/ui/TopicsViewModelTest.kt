package ytslim.ui

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ytslim.data.FirstRun
import ytslim.data.SettingsRepository
import ytslim.data.SuggestedTopic
import ytslim.data.Topic
import ytslim.ui.TopicsViewModel.Step

/** A tela de tópicos: dois passos, um tópico num passo só, e os criados à mão continuam. */
@RunWith(RobolectricTestRunner::class)
class TopicsViewModelTest {
    private val mine = Topic("Woodturning", listOf("lathe", "torno"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SettingsRepository.init(context)
        FirstRun.init(context)
        context.getSharedPreferences("first_run", 0).edit().clear().commit()
        runBlocking {
            SettingsRepository.setTopics(
                preferred = listOf(mine, SuggestedTopic.SCIENCE.topic("Science").copy(keywords = listOf("science", "physics"))),
                excluded = listOf(SuggestedTopic.POLITICS.topic("Politics")),
            )
        }
    }

    private fun idle() {
        repeat(20) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(25)
        }
    }

    @Test
    fun startsFromTheSavedTopicsAndKeepsEachTopicInOneStep() {
        val vm = TopicsViewModel()
        idle()
        val start = vm.state.value
        assertTrue(start.loaded)
        assertEquals(setOf(SuggestedTopic.SCIENCE), start.more)
        assertEquals(setOf(SuggestedTopic.POLITICS), start.less)
        // "Politics" está no ver menos: não aparece no ver mais.
        assertFalse(SuggestedTopic.POLITICS in start.shown)

        vm.toggle(SuggestedTopic.HISTORY)
        vm.next()
        assertEquals(Step.LESS, vm.state.value.step)
        assertFalse(SuggestedTopic.HISTORY in vm.state.value.shown)
        assertEquals(SuggestedTopic.POLITICS, vm.state.value.shown.first { it in vm.state.value.chosen })
        assertTrue(vm.back())
        assertFalse(vm.back())
    }

    @Test
    fun savingKeepsHandMadeTopicsAndEditedKeywords() {
        assertFalse(FirstRun.topicsChosen)
        val vm = TopicsViewModel()
        idle()
        vm.toggle(SuggestedTopic.HISTORY)
        vm.next()
        vm.toggle(SuggestedTopic.POLITICS)
        vm.toggle(SuggestedTopic.SPORTS)
        var done = false
        vm.save({ "name:" + it.name }) { done = true }
        idle()
        assertTrue(done)
        assertTrue(FirstRun.topicsChosen)

        val filters = runBlocking { SettingsRepository.current().filters }
        // Os sugeridos na ordem das sugestões, os da pessoa depois; Science mantém as palavras dela.
        assertEquals(listOf("name:HISTORY", "Science", "Woodturning"), filters.preferred.map { it.name })
        assertEquals(listOf("science", "physics"), filters.preferred.first { it.name == "Science" }.keywords)
        assertEquals(listOf("name:SPORTS"), filters.excluded.map { it.name })
    }

    @Test
    fun skippingOnlyMarksTheFirstRunAsDone() {
        val vm = TopicsViewModel()
        idle()
        var done = false
        vm.skip { done = true }
        assertTrue(done)
        assertTrue(FirstRun.topicsChosen)
        assertEquals(2, runBlocking { SettingsRepository.current().filters.preferred.size })
    }
}
