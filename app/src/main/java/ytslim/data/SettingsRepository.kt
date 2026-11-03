package ytslim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** O que fazer com um vídeo aberto por link de outro app (WhatsApp, navegador, Compartilhar). */
enum class ExternalLinks { NO_HISTORY, SAVE_HISTORY, ASK }

data class Settings(
    /** Lives e agendados no feed de Inscrições. Desligado por padrão. */
    val showLives: Boolean = false,
    val externalLinks: ExternalLinks = ExternalLinks.NO_HISTORY,
    /** Última escolha do botão de legenda; vale para os próximos vídeos. */
    val captionsOn: Boolean = false,
    /** Tópicos excluídos e preferidos e canais bloqueados (Recomendações, busca e playlists inscritas). */
    val filters: ContentFilters = ContentFilters(),
    /** Para onde vai o botão "Save" do player. */
    val saveTarget: SaveTarget = SaveTarget(),
    /** Painel de dormir do player: desligar a TV (ou apagar a tela) quando os vídeos acabarem. */
    val turnOffWhenDone: Boolean = false,
)

/** A playlist do botão "Save": "Assistir mais tarde" por padrão. */
data class SaveTarget(val playlistId: String = WATCH_LATER, val title: String? = null) {
    companion object {
        const val WATCH_LATER = "WL"
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Configurações do app, guardadas no aparelho. */
object SettingsRepository {
    private val SHOW_LIVES = booleanPreferencesKey("show_lives")
    private val EXTERNAL_LINKS = stringPreferencesKey("external_links")
    private val CAPTIONS_ON = booleanPreferencesKey("captions_on")
    private val EXCLUDED_TOPICS = stringPreferencesKey("excluded_topics")
    private val PREFERRED_TOPICS = stringPreferencesKey("preferred_topics")
    private val BLOCKED_CHANNELS = stringPreferencesKey("blocked_channels")
    private val SAVE_TARGET_ID = stringPreferencesKey("save_target_id")
    private val SAVE_TARGET_TITLE = stringPreferencesKey("save_target_title")
    private val TURN_OFF_WHEN_DONE = booleanPreferencesKey("turn_off_when_done")

    /** A Explore já explicou o que é (o aviso que aparece só na primeira vez). */
    private val EXPLORE_INTRO_SEEN = booleanPreferencesKey("explore_intro_seen")

    /**
     * O que a Explore guardava até a 0.5.1: as categorias da 0.5.0, os temas do histórico e os
     * temas escondidos. A Explore agora vem dos canais destacados ([ExploreRepository]).
     */
    private val OLD_EXPLORE = listOf(
        stringPreferencesKey("explore_categories"),
        stringPreferencesKey("explore_themes"),
        stringSetPreferencesKey("explore_hidden"),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val topics = ListSerializer(Topic.serializer())
    private val channels = ListSerializer(BlockedChannel.serializer())

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
                filters = ContentFilters(
                    // Sem nada salvo, valem as listas padrão; uma lista vazia salva é uma escolha.
                    excluded = p[EXCLUDED_TOPICS]?.let { decode(it, topics) } ?: TopicRules.DEFAULT_EXCLUDED,
                    preferred = p[PREFERRED_TOPICS]?.let { decode(it, topics) } ?: TopicRules.DEFAULT_PREFERRED,
                    blocked = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty(),
                ),
                saveTarget = p[SAVE_TARGET_ID]?.let { SaveTarget(it, p[SAVE_TARGET_TITLE]) } ?: SaveTarget(),
                turnOffWhenDone = p[TURN_OFF_WHEN_DONE] ?: false,
            )
        }

    private fun <T> decode(text: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    suspend fun current(): Settings = settings.first()

    suspend fun setShowLives(value: Boolean) = store.edit { it[SHOW_LIVES] = value }

    suspend fun setExternalLinks(value: ExternalLinks) = store.edit { it[EXTERNAL_LINKS] = value.name }

    suspend fun setCaptionsOn(value: Boolean) = store.edit { it[CAPTIONS_ON] = value }

    suspend fun setTurnOffWhenDone(value: Boolean) = store.edit { it[TURN_OFF_WHEN_DONE] = value }

    /** true depois que o aviso da primeira vez na Explore foi fechado. */
    val exploreIntroSeen: Flow<Boolean>
        get() = store.data.map { it[EXPLORE_INTRO_SEEN] ?: false }

    suspend fun setExploreIntroSeen() = store.edit { it[EXPLORE_INTRO_SEEN] = true }

    suspend fun setExcludedTopics(value: List<Topic>) = store.edit { it[EXCLUDED_TOPICS] = json.encodeToString(topics, value) }

    suspend fun setPreferredTopics(value: List<Topic>) = store.edit { it[PREFERRED_TOPICS] = json.encodeToString(topics, value) }

    suspend fun block(channel: BlockedChannel) = store.edit { p ->
        val current = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty()
        p[BLOCKED_CHANNELS] = json.encodeToString(channels, current.filterNot { it.id == channel.id } + channel)
    }

    suspend fun unblock(channelId: String) = store.edit { p ->
        val current = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty()
        p[BLOCKED_CHANNELS] = json.encodeToString(channels, current.filterNot { it.id == channelId })
    }

    /** Apaga o que a Explore antiga deixou guardado (uma vez, ao abrir o app). */
    suspend fun dropOldExplore() {
        if (OLD_EXPLORE.none { it in store.data.first() }) return
        store.edit { p -> OLD_EXPLORE.forEach { p.remove(it) } }
    }

    suspend fun setSaveTarget(value: SaveTarget) = store.edit { p ->
        p[SAVE_TARGET_ID] = value.playlistId
        value.title?.let { p[SAVE_TARGET_TITLE] = it } ?: p.remove(SAVE_TARGET_TITLE)
    }
}
