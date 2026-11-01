package ytslim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** O que fazer com um vídeo aberto por link de outro app (WhatsApp, navegador, Compartilhar). */
enum class ExternalLinks { NO_HISTORY, SAVE_HISTORY, ASK }

data class Settings(
    /** Lives e agendados no feed de Inscrições. Desligado por padrão. */
    val showLives: Boolean = false,
    val externalLinks: ExternalLinks = ExternalLinks.NO_HISTORY,
    /** Última escolha do botão de legenda; vale para os próximos vídeos. */
    val captionsOn: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Configurações do app, guardadas no aparelho. */
object SettingsRepository {
    private val SHOW_LIVES = booleanPreferencesKey("show_lives")
    private val EXTERNAL_LINKS = stringPreferencesKey("external_links")
    private val CAPTIONS_ON = booleanPreferencesKey("captions_on")

    private lateinit var store: DataStore<Preferences>

    fun init(context: Context) {
        store = context.applicationContext.settingsStore
    }

    val settings: Flow<Settings>
        get() = store.data.map { p ->
            Settings(
                showLives = p[SHOW_LIVES] ?: false,
                externalLinks = p[EXTERNAL_LINKS]?.let { runCatching { ExternalLinks.valueOf(it) }.getOrNull() }
                    ?: ExternalLinks.NO_HISTORY,
                captionsOn = p[CAPTIONS_ON] ?: false,
            )
        }

    suspend fun current(): Settings = settings.first()

    suspend fun setShowLives(value: Boolean) = store.edit { it[SHOW_LIVES] = value }

    suspend fun setExternalLinks(value: ExternalLinks) = store.edit { it[EXTERNAL_LINKS] = value.name }

    suspend fun setCaptionsOn(value: Boolean) = store.edit { it[CAPTIONS_ON] = value }
}
