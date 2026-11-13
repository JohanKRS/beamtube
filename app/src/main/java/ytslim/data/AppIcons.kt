package ytslim.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ytslim.R

/**
 * As cores do ícone do app. O desenho é sempre a fresta com o facho de luz; muda só a cor.
 * Cada uma é um activity-alias no AndroidManifest.xml (`alias`), com o ícone e o banner da TV dela.
 * Os recursos saem de tools/make_icons.py.
 */
enum class AppIcon(
    val alias: String,
    @StringRes val label: Int,
    @DrawableRes val foreground: Int,
    @ColorRes val top: Int,
    @ColorRes val bottom: Int,
    /** Degradê do canto de cima à esquerda ao de baixo à direita, em vez de vertical. */
    val diagonal: Boolean = false,
) {
    NIGHT("ytslim.LauncherNight", R.string.icon_night, R.drawable.ic_launcher_night_foreground, R.color.launcher_night_top, R.color.launcher_night_bottom),
    BLACK("ytslim.LauncherBlack", R.string.icon_black, R.drawable.ic_launcher_black_foreground, R.color.launcher_black_top, R.color.launcher_black_bottom),
    TEAL("ytslim.LauncherTeal", R.string.icon_teal, R.drawable.ic_launcher_teal_foreground, R.color.launcher_teal_top, R.color.launcher_teal_bottom),
    PLUM("ytslim.LauncherPlum", R.string.icon_plum, R.drawable.ic_launcher_plum_foreground, R.color.launcher_plum_top, R.color.launcher_plum_bottom),
    MUSTARD("ytslim.LauncherMustard", R.string.icon_mustard, R.drawable.ic_launcher_mustard_foreground, R.color.launcher_mustard_top, R.color.launcher_mustard_bottom),
    VIVID("ytslim.LauncherVivid", R.string.icon_vivid, R.drawable.ic_launcher_vivid_foreground, R.color.launcher_vivid_top, R.color.launcher_vivid_bottom, diagonal = true),
    RED("ytslim.LauncherRed", R.string.icon_red, R.drawable.ic_launcher_red_foreground, R.color.launcher_red_top, R.color.launcher_red_bottom),
    ;

    companion object {
        /** O que vem ligado no manifest. */
        val DEFAULT = NIGHT
    }
}

/**
 * Troca o ícone ligando o alias da cor escolhida e desligando os outros.
 * A escolha fica no próprio Android (estado dos componentes), não nas configurações do app,
 * então a tela sempre mostra o ícone que o launcher está usando.
 */
object AppIcons {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun current(): AppIcon = AppIcon.entries.firstOrNull { isEnabled(it) } ?: AppIcon.DEFAULT

    fun select(icon: AppIcon) {
        val pm = context.packageManager
        // Liga o novo antes de desligar os outros, para o app nunca ficar sem entrada no launcher.
        pm.setComponentEnabledSetting(component(icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        AppIcon.entries.filter { it != icon }.forEach {
            pm.setComponentEnabledSetting(component(it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    private fun isEnabled(icon: AppIcon): Boolean = when (context.packageManager.getComponentEnabledSetting(component(icon))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT
        else -> false
    }

    private fun component(icon: AppIcon) = ComponentName(context.packageName, icon.alias)
}
