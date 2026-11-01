package ytslim.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** O seletor de ícone: cada cor é um alias da MainActivity, e só o escolhido aparece no launcher. */
@RunWith(RobolectricTestRunner::class)
class AppIconsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = AppIcons.init(context)

    private fun launcherEntries(category: String): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(context.packageName)
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }

    private fun assertOnlyInLaunchers(icon: AppIcon) {
        assertEquals(icon, AppIcons.current())
        assertEquals(listOf(icon.alias), launcherEntries(Intent.CATEGORY_LAUNCHER))
        assertEquals(listOf(icon.alias), launcherEntries(Intent.CATEGORY_LEANBACK_LAUNCHER))
    }

    @Test
    fun everyColorIsAnAliasOfMainActivity() {
        AppIcon.entries.forEach {
            val info = context.packageManager.getActivityInfo(
                ComponentName(context.packageName, it.alias),
                PackageManager.MATCH_DISABLED_COMPONENTS,
            )
            assertEquals(it.name, "ytslim.MainActivity", info.targetActivity)
        }
    }

    @Test
    fun startsWithTheDefaultIcon() = assertOnlyInLaunchers(AppIcon.DEFAULT)

    @Test
    fun selectingAnIconLeavesOnlyThatOneInTheLauncher() {
        AppIcons.select(AppIcon.TEAL)
        assertOnlyInLaunchers(AppIcon.TEAL)
        AppIcons.select(AppIcon.VIVID)
        assertOnlyInLaunchers(AppIcon.VIVID)
        AppIcons.select(AppIcon.NIGHT)
        assertOnlyInLaunchers(AppIcon.NIGHT)
    }
}
