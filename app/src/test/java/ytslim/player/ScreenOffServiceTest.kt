package ytslim.player

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

/**
 * O que os Ajustes da TV usam para o botão YouTube do controle (1.0.4): se o serviço de
 * Acessibilidade está ligado, se o botão já foi aprendido e a tela de ajustes que abre.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenOffServiceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun service(packageName: String, name: String) = AccessibilityServiceInfo().apply {
        val resolve = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                this.packageName = packageName
                this.name = name
            }
        }
        ReflectionHelpers.setField(this, "mResolveInfo", resolve)
    }

    private fun enabledServices(vararg services: AccessibilityServiceInfo) {
        shadowOf(app.getSystemService(AccessibilityManager::class.java)).setEnabledAccessibilityServiceList(services.toList())
    }

    @Test
    fun offUntilAndroidListsTheService() {
        assertFalse(ScreenOffService.isOn(app))
        // Outro serviço do mesmo app não conta, nem o mesmo nome em outro app.
        enabledServices(service(app.packageName, "ytslim.player.Other"), service("com.example", ScreenOffService::class.java.name))
        assertFalse(ScreenOffService.isOn(app))
        enabledServices(service(app.packageName, ScreenOffService::class.java.name))
        assertTrue(ScreenOffService.isOn(app))
    }

    @Test
    fun knowsTheButtonOnceACodeIsSaved() {
        val prefs = app.getSharedPreferences("remote_keys", Context.MODE_PRIVATE)
        assertFalse(ScreenOffService.knowsYouTubeButton(app))
        prefs.edit().putStringSet("youtube_button", setOf("293")).commit()
        assertTrue(ScreenOffService.knowsYouTubeButton(app))
        prefs.edit().clear().commit()
    }

    @Test
    fun opensAccessibilitySettings() {
        val shadow = shadowOf(app)
        shadow.checkActivities(true)
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), activity("accessibility"))
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Settings.ACTION_SETTINGS), activity("settings"))
        assertTrue(ScreenOffService.openSettings(app))
        val started = shadow.nextStartedActivity
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, started.action)
        // Fora de uma tela (o contexto do app), precisa de uma tarefa nova.
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun fallsBackToTheGeneralSettings() {
        val shadow = shadowOf(app)
        shadow.checkActivities(true)
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Settings.ACTION_SETTINGS), activity("settings"))
        assertTrue(ScreenOffService.openSettings(app))
        assertEquals(Settings.ACTION_SETTINGS, shadow.nextStartedActivity.action)
    }

    @Test
    fun saysWhenNoSettingsOpen() {
        val shadow = shadowOf(app)
        shadow.checkActivities(true)
        assertFalse(ScreenOffService.openSettings(app))
        assertNull(shadow.nextStartedActivity)
    }

    private fun activity(name: String) = ResolveInfo().apply {
        activityInfo = android.content.pm.ActivityInfo().apply {
            packageName = "com.android.tv.settings"
            this.name = "com.android.tv.settings.$name"
            applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = "com.android.tv.settings" }
        }
    }
}
