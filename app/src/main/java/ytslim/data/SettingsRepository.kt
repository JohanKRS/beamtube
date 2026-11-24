package ytslim.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.builtins.ListSerializer

/** O que fazer com um vídeo aberto por link de outro app (WhatsApp, navegador, Compartilhar). */
enum class ExternalLinks { NO_HISTORY, SAVE_HISTORY, ASK }

/**
 * O que o player faz quando o vídeo acaba sem próximo na sequência (1.0, pedido do Johan): a tela
 * de sugestões, que volta sozinha depois de um tempo ([UpNext]); voltar direto para a tela de onde
 * o vídeo foi aberto; tocar a primeira sugestão; ou ficar parado no fim, como até a 0.7.1.
 */
enum class EndAction { SUGGESTIONS, BACK, NEXT, STAY }

@Immutable
data class Settings(
    /** Lives e agendados no feed de Inscrições. Desligado por padrão. */
    val showLives: Boolean = false,
    val externalLinks: ExternalLinks = ExternalLinks.NO_HISTORY,
    /** Última escolha do botão de legenda (liga e desliga); vale para os próximos vídeos. */
    val captionsOn: Boolean = false,
    /** Settings > Captions: em que idioma a legenda aparece quando ligada (o do app, o original ou inglês). */
    val captionLanguage: CaptionLanguage = CaptionLanguage.ORIGINAL,
    /** Tópicos excluídos e preferidos e canais bloqueados (Recomendações, busca e playlists inscritas). */
    val filters: ContentFilters = ContentFilters(),
    /** Para onde vai o botão "Save" do player. */
    val saveTarget: SaveTarget = SaveTarget(),
    /** Painel de dormir do player: desligar a TV (ou apagar a tela) quando os vídeos acabarem. */
    val turnOffWhenDone: Boolean = false,
    /** Settings > When a video ends. */
    val endAction: EndAction = EndAction.SUGGESTIONS,
)

/** A playlist do botão "Save": "Assistir mais tarde" por padrão. */
@Immutable
data class SaveTarget(val playlistId: String = WATCH_LATER, val title: String? = null) {
    companion object {
        const val WATCH_LATER = "WL"
    }
}

/**
 * Configurações de cada usuário ([Users]), guardadas no aparelho: o usuário aberto tem as dele, e
 * trocar de usuário troca as configurações. O que é do aparelho (idioma, cores, ícone) fica fora daqui.
 */
object SettingsRepository {
    private val SHOW_LIVES = booleanPreferencesKey("show_lives")
    private val EXTERNAL_LINKS = stringPreferencesKey("external_links")
    private val CAPTIONS_ON = booleanPreferencesKey("captions_on")
    private val CAPTION_LANGUAGE = stringPreferencesKey("caption_language")
    private val EXCLUDED_TOPICS = stringPreferencesKey("excluded_topics")
    private val PREFERRED_TOPICS = stringPreferencesKey("preferred_topics")
    private val BLOCKED_CHANNELS = stringPreferencesKey("blocked_channels")
    private val SAVE_TARGET_ID = stringPreferencesKey("save_target_id")
    private val SAVE_TARGET_TITLE = stringPreferencesKey("save_target_title")
    private val TURN_OFF_WHEN_DONE = booleanPreferencesKey("turn_off_when_done")
    private val END_ACTION = stringPreferencesKey("end_action")

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

    private val topics = ListSerializer(Topic.serializer())
    private val channels = ListSerializer(BlockedChannel.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private class FilterCache(
        val excludedRaw: String?,
        val preferredRaw: String?,
        val blockedRaw: String?,
        val filters: ContentFilters,
    )

    @Volatile private var filterCache: FilterCache? = null
    @Volatile private var shared: SharedFlow<Settings>? = null

    private lateinit var context: Context

    /**
     * Um DataStore por arquivo ("settings" na vaga de antes da 1.0.7, "settings_1"… nas outras). O
     * mesmo arquivo nunca tem dois abertos (o DataStore não deixa), nem entre os testes.
     */
    private val stores = HashMap<String, DataStore<Preferences>>()

    fun init(context: Context) {
        this.context = context.applicationContext
        filterCache = null
        shared = buildShared()
    }

    private fun storeFor(slot: String): DataStore<Preferences> {
        val file = context.preferencesDataStoreFile(Users.prefsName("settings", slot))
        return synchronized(stores) {
            stores.getOrPut(file.absolutePath) { PreferenceDataStoreFactory.create(produceFile = { file }) }
        }
    }

    /** As configurações do usuário aberto. */
    private val store: DataStore<Preferences> get() = storeFor(Users.slot)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildShared(): SharedFlow<Settings> = Users.slots
        .flatMapLatest { storeFor(it).data }
        .map(::fromPreferences)
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** As configurações do usuário aberto, compartilhadas entre os ViewModels (decodificadas fora da thread da tela). */
    val settings: Flow<Settings>
        get() = shared ?: buildShared().also { shared = it }

    private fun fromPreferences(p: Preferences): Settings = Settings(
        showLives = p[SHOW_LIVES] ?: false,
        externalLinks = p[EXTERNAL_LINKS]?.let { runCatching { ExternalLinks.valueOf(it) }.getOrNull() }
            ?: ExternalLinks.NO_HISTORY,
        captionsOn = p[CAPTIONS_ON] ?: false,
        captionLanguage = p[CAPTION_LANGUAGE]?.let { runCatching { CaptionLanguage.valueOf(it) }.getOrNull() }
            ?: CaptionLanguage.ORIGINAL,
        filters = filtersFrom(p[EXCLUDED_TOPICS], p[PREFERRED_TOPICS], p[BLOCKED_CHANNELS]),
        saveTarget = p[SAVE_TARGET_ID]?.let { SaveTarget(it, p[SAVE_TARGET_TITLE]) } ?: SaveTarget(),
        turnOffWhenDone = p[TURN_OFF_WHEN_DONE] ?: false,
        endAction = p[END_ACTION]?.let { runCatching { EndAction.valueOf(it) }.getOrNull() } ?: EndAction.SUGGESTIONS,
    )

    /** Evita redecodificar JSON e recriar os índices do [ContentFilters] quando só outra preferência mudou. */
    private fun filtersFrom(excludedRaw: String?, preferredRaw: String?, blockedRaw: String?): ContentFilters {
        val cached = filterCache
        if (cached != null && cached.excludedRaw == excludedRaw && cached.preferredRaw == preferredRaw && cached.blockedRaw == blockedRaw) {
            return cached.filters
        }
        val filters = ContentFilters(
            // Desde a 1.0 não há tópicos prontos: cada um escolhe os seus ([SuggestedTopic]).
            excluded = excludedRaw?.let { decode(it, topics) }.orEmpty(),
            preferred = preferredRaw?.let { decode(it, topics) }.orEmpty(),
            blocked = blockedRaw?.let { decode(it, channels) }.orEmpty(),
        )
        filterCache = FilterCache(excludedRaw, preferredRaw, blockedRaw, filters)
        return filters
    }

    private fun <T> decode(text: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { AppJson.decodeFromString(serializer, text) }.getOrNull()

    suspend fun current(): Settings = fromPreferences(store.data.first())

    suspend fun setShowLives(value: Boolean) = store.edit { it[SHOW_LIVES] = value }

    suspend fun setExternalLinks(value: ExternalLinks) = store.edit { it[EXTERNAL_LINKS] = value.name }

    suspend fun setCaptionsOn(value: Boolean) = store.edit { it[CAPTIONS_ON] = value }

    /** Settings > Captions: o idioma da legenda nos próximos vídeos ([CaptionLanguage]). */
    suspend fun setCaptionLanguage(language: CaptionLanguage) = store.edit { it[CAPTION_LANGUAGE] = language.name }

    suspend fun setTurnOffWhenDone(value: Boolean) = store.edit { it[TURN_OFF_WHEN_DONE] = value }

    suspend fun setEndAction(value: EndAction) = store.edit { it[END_ACTION] = value.name }

    /** true depois que o aviso da primeira vez na Explore foi fechado. */
    val exploreIntroSeen: Flow<Boolean>
        get() = store.data.map { it[EXPLORE_INTRO_SEEN] ?: false }.distinctUntilChanged()

    suspend fun setExploreIntroSeen() = store.edit { it[EXPLORE_INTRO_SEEN] = true }

    suspend fun setExcludedTopics(value: List<Topic>) = store.edit { it[EXCLUDED_TOPICS] = AppJson.encodeToString(topics, value) }

    suspend fun setPreferredTopics(value: List<Topic>) = store.edit { it[PREFERRED_TOPICS] = AppJson.encodeToString(topics, value) }

    /** Grava as duas listas de uma vez (a tela de tópicos). */
    suspend fun setTopics(preferred: List<Topic>, excluded: List<Topic>) = store.edit {
        it[PREFERRED_TOPICS] = AppJson.encodeToString(topics, preferred)
        it[EXCLUDED_TOPICS] = AppJson.encodeToString(topics, excluded)
    }

    suspend fun block(channel: BlockedChannel) = store.edit { p ->
        val current = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty()
        p[BLOCKED_CHANNELS] = AppJson.encodeToString(channels, current.filterNot { it.id == channel.id } + channel)
    }

    suspend fun unblock(channelId: String) = store.edit { p ->
        val current = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty()
        p[BLOCKED_CHANNELS] = AppJson.encodeToString(channels, current.filterNot { it.id == channelId })
    }

    /** O YouTube aceitou o "Don't recommend channel" de [channelId] (se ele ainda estiver bloqueado). */
    suspend fun markOnAccount(channelId: String) = store.edit { p ->
        val current = p[BLOCKED_CHANNELS]?.let { decode(it, channels) }.orEmpty()
        if (current.none { it.id == channelId && !it.onAccount }) return@edit
        p[BLOCKED_CHANNELS] = AppJson.encodeToString(channels, current.map { if (it.id == channelId) it.copy(onAccount = true) else it })
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
