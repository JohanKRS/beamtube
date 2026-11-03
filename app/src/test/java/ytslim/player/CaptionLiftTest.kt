package ytslim.player

import android.app.Activity
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** A legenda sobe acima da barra de baixo só com os controles abertos, e só o que precisa. */
@RunWith(RobolectricTestRunner::class)
class CaptionLiftTest {
    /**
     * A tela do player em px, numa Activity (a subida usa post e animação, que só rodam com a view
     * na janela): o vídeo com a legenda dentro e, por cima, o nome do capítulo e a barra.
     */
    private inner class Screen(width: Int, height: Int, videoHeight: Int, barHeight: Int, chapterHeight: Int = 0) {
        private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val video = FrameLayout(activity)
        val captions = View(activity)
        val bottom = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val chapter = View(activity).apply { visibility = if (chapterHeight > 0) View.VISIBLE else View.GONE }
        val island = View(activity)
        val lift = CaptionLift(captions, listOf(chapter, island))

        init {
            video.addView(captions, FrameLayout.LayoutParams(-1, -1))
            root.addView(video, FrameLayout.LayoutParams(-1, videoHeight, Gravity.CENTER))
            bottom.addView(chapter, LinearLayout.LayoutParams(-1, chapterHeight))
            bottom.addView(island, LinearLayout.LayoutParams(-1, barHeight))
            root.addView(bottom, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            activity.setContentView(root, ViewGroup.LayoutParams(width, height))
            settle()
        }

        fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    @Test
    fun closedControlsLeaveTheCaptionsAlone() {
        val screen = Screen(width = 2000, height = 1000, videoHeight = 1000, barHeight = 150)
        screen.lift.setOpen(false)
        screen.settle()
        assertEquals(0, screen.lift.lift())
        assertEquals(0f, screen.captions.translationY)
    }

    @Test
    fun openControlsLiftTheCaptionsAboveTheBar() {
        val screen = Screen(width = 2000, height = 1000, videoHeight = 1000, barHeight = 150)
        screen.lift.setOpen(true)
        screen.settle()
        assertEquals(150, screen.lift.lift())
        assertEquals(-150f, screen.captions.translationY)
    }

    @Test
    fun theChapterNameCountsAsPartOfTheBar() {
        val screen = Screen(width = 2000, height = 1000, videoHeight = 1000, barHeight = 150, chapterHeight = 50)
        screen.lift.setOpen(true)
        screen.settle()
        assertEquals(-200f, screen.captions.translationY)
    }

    @Test
    fun aVideoThatEndsAboveTheBarDoesNotMove() {
        // O celular em pé: o vídeo 16:9 no meio da tela, longe da barra.
        val screen = Screen(width = 1000, height = 2000, videoHeight = 560, barHeight = 300)
        screen.lift.setOpen(true)
        screen.settle()
        assertEquals(0f, screen.captions.translationY)
    }

    @Test
    fun onlyThePartBehindTheBarCounts() {
        // O vídeo termina 100 px acima do fundo e a barra tem 150: sobe 50.
        val screen = Screen(width = 2000, height = 1200, videoHeight = 1000, barHeight = 150)
        screen.lift.setOpen(true)
        screen.settle()
        assertEquals(-50f, screen.captions.translationY)
    }

    @Test
    fun closingTheControlsBringsTheCaptionsBack() {
        val screen = Screen(width = 2000, height = 1000, videoHeight = 1000, barHeight = 150)
        screen.lift.setOpen(true)
        screen.settle()
        screen.lift.setOpen(false)
        screen.settle()
        assertEquals(0f, screen.captions.translationY)
    }

    @Test
    fun aTallerBarWhileOpenLiftsMore() {
        // A barra muda de altura com os controles abertos (girar a tela passa para duas linhas).
        val screen = Screen(width = 2000, height = 1000, videoHeight = 1000, barHeight = 150)
        screen.lift.setOpen(true)
        screen.settle()
        screen.island.layoutParams = screen.island.layoutParams.apply { height = 250 }
        screen.island.requestLayout()
        screen.settle()
        assertEquals(-250f, screen.captions.translationY)
    }
}
