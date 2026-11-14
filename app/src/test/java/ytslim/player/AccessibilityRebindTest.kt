package ytslim.player

import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * O serviço do botão YouTube ligado nas configurações mas desconectado depois de uma atualização do
 * app (TCL do Johan, 2026-11-14): o app pede ao Android para ligá-lo de novo.
 */
@RunWith(RobolectricTestRunner::class)
class AccessibilityRebindTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val me = "${app.packageName}/ytslim.player.ScreenOffService"

    private fun enabledInSettings(value: String?) {
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value)
    }

    private val appState: Int get() = app.packageManager.getApplicationEnabledSetting(app.packageName)

    @Before
    fun reset() {
        AccessibilityRebind.lastNudgeAt = 0L
    }

    @After
    fun clear() {
        enabledInSettings(null)
        app.packageManager.setApplicationEnabledSetting(app.packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0)
    }

    @Test
    fun readsTheServiceFromTheAccessibilitySettings() {
        enabledInSettings(null)
        assertFalse(ScreenOffService.isEnabledInSettings(app))
        // A lista da TCL do Johan: o BeamTube no meio dos serviços do OmarchyTV.
        enabledInSettings("com.omarchytv.home/com.omarchytv.home.OmarchyRemote:$me:com.omarchytv.home/com.omarchytv.home.HomeOverride")
        assertTrue(ScreenOffService.isEnabledInSettings(app))
        enabledInSettings("com.omarchytv.home/com.omarchytv.home.OmarchyRemote")
        assertFalse(ScreenOffService.isEnabledInSettings(app))
    }

    @Test
    fun onInSettingsButNotConnectedAsksAndroidAgain() = runTest {
        enabledInSettings(me)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, appState)
        // Aqui ninguém conecta o serviço: o app pede, espera e devolve que continua desconectado.
        assertFalse(AccessibilityRebind.ensure(app, "test"))
        // O pacote trocou de estado (continua ativo: ativado no lugar do padrão), e é essa troca que
        // faz a Acessibilidade rever a lista.
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, appState)
        // Logo em seguida não pede de novo (um pedido por minuto).
        assertFalse(AccessibilityRebind.ensure(app, "test"))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, appState)
        // Mais tarde, a troca volta para o padrão.
        AccessibilityRebind.lastNudgeAt = 0L
        AccessibilityRebind.ensure(app, "test")
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, appState)
    }

    @Test
    fun offInSettingsLeavesThePackageAlone() = runTest {
        enabledInSettings("com.omarchytv.home/com.omarchytv.home.OmarchyRemote")
        assertFalse(AccessibilityRebind.ensure(app, "test"))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, appState)
    }
}
