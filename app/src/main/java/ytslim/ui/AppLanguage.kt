package ytslim.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import ytslim.R
import java.util.Locale

/**
 * O idioma do app, escolhido em Settings (pedido do Johan, 2026-11-13): o do aparelho, inglês,
 * português ou espanhol. O nome de cada idioma aparece nele mesmo ([native]), para quem não
 * entende o idioma atual achar o seu.
 */
enum class Language(val tag: String?, val native: String?, @StringRes val label: Int?) {
    SYSTEM(null, null, R.string.language_system),
    ENGLISH("en", "English", null),
    PORTUGUESE("pt-BR", "Português", null),
    SPANISH("es", "Español", null),
}

/**
 * Aplica o idioma escolhido. As telas (Activities) recebem uma configuração por cima da do sistema
 * ([applyTo], chamado no `attachBaseContext`), que se soma às mudanças de rotação e tamanho que
 * elas mesmas tratam; o resto do app (avisos feitos fora das telas, o player) usa os recursos do
 * [Application], atualizados em [applyToApp]. Guardado nas preferências "look", lidas na hora,
 * antes de qualquer tela abrir.
 */
object AppLanguage {
    private const val PREFS = "look"
    private const val KEY = "language"

    /** Os idiomas do aparelho e o padrão do Java, guardados antes de o app trocar qualquer um. */
    private var systemLocales: LocaleListCompat? = null
    private var systemDefault: Locale? = null

    fun current(context: Context): Language {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return Language.entries.firstOrNull { it.name == name } ?: Language.SYSTEM
    }

    private fun localeOf(context: Context): Locale? = current(context).tag?.let(Locale::forLanguageTag)

    /** No `attachBaseContext` de cada Activity, logo depois do `super`. */
    fun applyTo(activity: ContextThemeWrapper) {
        val locale = localeOf(activity) ?: return
        activity.applyOverrideConfiguration(Configuration().apply { setLocale(locale) })
    }

    /**
     * No `onCreate` do [Application], antes de tudo: guarda os idiomas do aparelho, que ali ainda
     * não foram trocados pelo app, e aplica a escolha.
     */
    fun init(app: Application) {
        systemLocales = ConfigurationCompat.getLocales(app.resources.configuration)
        systemDefault = Locale.getDefault()
        applyToApp(app)
    }

    /**
     * Aplica a escolha aos recursos do [Application]. Com o idioma do aparelho, não mexe em nada, a
     * não ser para desfazer uma escolha anterior feita com o app aberto.
     */
    @Suppress("DEPRECATION")
    private fun applyToApp(app: Application) {
        val res = app.resources
        val locale = localeOf(app)
        val current = ConfigurationCompat.getLocales(res.configuration)
        val config = Configuration(res.configuration)
        if (locale == null) {
            val system = systemLocales ?: return
            systemDefault?.let(Locale::setDefault)
            if (current == system) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList.forLanguageTags(system.toLanguageTags()))
            } else {
                system[0]?.let(config::setLocale)
            }
        } else {
            Locale.setDefault(locale)
            if (current[0] == locale && current.size() == 1) return
            config.setLocale(locale)
        }
        res.updateConfiguration(config, res.displayMetrics)
    }

    /** O [Application] recebeu uma configuração nova (o idioma do aparelho pode ter mudado). */
    fun onAppConfigurationChanged(app: Application, config: Configuration) {
        if (current(app) == Language.SYSTEM) {
            systemLocales = ConfigurationCompat.getLocales(config)
            systemDefault = systemLocales?.get(0) ?: systemDefault
        }
        applyToApp(app)
    }

    /** Grava a escolha e refaz a tela para ela já aparecer no idioma novo. */
    fun select(activity: Activity, language: Language) {
        if (current(activity) == language) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).commit()
        applyToApp(activity.application)
        activity.recreate()
    }
}
