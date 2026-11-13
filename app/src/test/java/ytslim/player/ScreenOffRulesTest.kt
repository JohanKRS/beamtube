package ytslim.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tocar com a tela desligada (pedido do Johan, 2026-11-11): só no celular e no tablet, só com a tela apagada. */
class ScreenOffRulesTest {
    @Test
    fun turningTheScreenOffKeepsTheVideoPlaying() {
        assertTrue(ScreenOffRules.keepsPlaying(touch = true, screenOn = false, playWhenReady = true))
    }

    @Test
    fun aPausedVideoStaysPaused() {
        assertFalse(ScreenOffRules.keepsPlaying(touch = true, screenOn = false, playWhenReady = false))
    }

    @Test
    fun leavingForAnotherAppStillPauses() {
        // Home ou outro app: a tela continua acesa.
        assertFalse(ScreenOffRules.keepsPlaying(touch = true, screenOn = true, playWhenReady = true))
    }

    @Test
    fun onTheTvTurningOffPauses() {
        assertFalse(ScreenOffRules.keepsPlaying(touch = false, screenOn = false, playWhenReady = true))
    }

    @Test
    fun onTheTvLeavingClosesThePlayer() {
        // A TV desligada, Home ou outro app: ao voltar, o app está na lista (pedido do Johan, 2026-11-13).
        assertTrue(ScreenOffRules.closesOnStop(touch = false, changingConfigurations = false, inOwnSettings = false))
    }

    @Test
    fun onTheTvTheAccessibilitySettingsOfTurnOffKeepThePlayer() {
        assertFalse(ScreenOffRules.closesOnStop(touch = false, changingConfigurations = false, inOwnSettings = true))
    }

    @Test
    fun aRecreatedScreenKeepsThePlayer() {
        assertFalse(ScreenOffRules.closesOnStop(touch = false, changingConfigurations = true, inOwnSettings = false))
    }

    @Test
    fun onThePhoneLeavingKeepsThePlayer() {
        // Mini player e tela desligada tocando (PlaybackService).
        assertFalse(ScreenOffRules.closesOnStop(touch = true, changingConfigurations = false, inOwnSettings = false))
    }
}
