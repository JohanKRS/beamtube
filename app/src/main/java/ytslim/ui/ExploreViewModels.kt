package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ytslim.data.AppScope
import ytslim.data.Category
import ytslim.data.CategoryResults
import ytslim.data.CategoryRules
import ytslim.data.CategorySearch
import ytslim.data.SearchFilters
import ytslim.data.Settings
import ytslim.data.SettingsRepository
import ytslim.data.TopicRules
import ytslim.data.Video
import java.util.UUID

/** Criar, mudar e apagar categorias: o mesmo nas duas telas (Explore e a página da categoria). */
object CategoryEditing {
    /**
     * Grava a categoria [id] (ou uma nova, com id null) e devolve o id. Uma categoria nova (ou com
     * palavras novas) busca as miniaturas do mosaico logo em seguida.
     */
    fun save(id: String?, name: String, keywords: List<String>, filters: SearchFilters): String {
        val categoryId = id ?: UUID.randomUUID().toString()
        AppScope.launch {
            val old = SettingsRepository.categories.first().firstOrNull { it.id == categoryId }
            val category = (old ?: Category(categoryId, name.trim(), keywords))
                .copy(name = name.trim(), keywords = keywords)
                .withFilters(filters)
            SettingsRepository.saveCategory(category)
            if (old == null || old.keywords != keywords || old.filters != filters) refreshThumbnails(category)
        }
        return categoryId
    }

    fun delete(id: String) {
        AppScope.launch { SettingsRepository.deleteCategory(id) }
    }

    /** Busca a primeira página só para o mosaico do cartão (sem mexer em nada se falhar). */
    suspend fun refreshThumbnails(category: Category) {
        val videos = runCatching { CategorySearch.first(category).videos }.getOrNull() ?: return
        storeThumbnails(category.id, videos)
    }

    suspend fun storeThumbnails(id: String, videos: List<Video>) {
        val thumbs = CategoryRules.thumbnails(videos.filterNot { it.isShorts })
        if (thumbs.isEmpty()) return
        SettingsRepository.updateCategory(id) { if (it.thumbnails == thumbs) it else it.copy(thumbnails = thumbs) }
    }
}

/** A aba Explore: as categorias do usuário. As que ainda não têm mosaico buscam as miniaturas. */
class ExploreViewModel : ViewModel() {
    /** null enquanto lê do aparelho. */
    val categories: StateFlow<List<Category>?> = SettingsRepository.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            categories.filterNotNull().first()
                .filter { it.thumbnails.isEmpty() && it.keywords.isNotEmpty() }
                .forEach { CategoryEditing.refreshThumbnails(it) }
        }
    }

    fun save(id: String?, name: String, keywords: List<String>, filters: SearchFilters) {
        CategoryEditing.save(id, name, keywords, filters)
    }

    fun delete(id: String) = CategoryEditing.delete(id)
}

/**
 * A página de uma categoria: a busca de cada palavra-chave, alternadas. Shorts e canais
 * bloqueados ficam de fora. Mudar os filtros (ou as palavras) grava na categoria e busca de novo.
 */
class CategoryViewModel(private val id: String) : ViewModel(), Refreshable {
    /** null depois que a categoria foi apagada. */
    val category: StateFlow<Category?> = SettingsRepository.categories
        .map { list -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** A categoria foi apagada (em outra tela): a página avisa em vez de esperar para sempre. */
    val gone: StateFlow<Boolean> = SettingsRepository.categories
        .map { list -> list.none { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val filters: StateFlow<SearchFilters> = category
        .map { it?.filters ?: SearchFilters() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchFilters())

    private sealed interface Raw {
        data object Loading : Raw
        data class Error(val message: String) : Raw
        data class Loaded(val results: CategoryResults, val loadingMore: Boolean) : Raw
    }

    private val raw = MutableStateFlow<Raw>(Raw.Loading)
    private val settings = SettingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val state: StateFlow<UiState<VideoListContent>> = combine(raw, settings, filters) { raw, settings, filters ->
        when (raw) {
            Raw.Loading -> UiState.Loading
            is Raw.Error -> UiState.Error(raw.message)
            is Raw.Loaded -> UiState.Ready(VideoListContent(shown(raw.results.videos, settings, filters), raw.results.hasMore, raw.loadingMore))
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _refreshing = MutableStateFlow(false)
    override val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private var job: Job? = null
    private var autoPages = 0

    init {
        // Busca de novo quando as palavras ou os filtros da categoria mudam (não quando só o mosaico muda).
        viewModelScope.launch {
            category.filterNotNull()
                .map { it.keywords to it.filters }
                .distinctUntilChanged()
                .collect { load(quiet = false) }
        }
    }

    private fun shown(videos: List<Video>, settings: Settings, filters: SearchFilters): List<Video> =
        videos.filter {
            !it.isShorts && !TopicRules.isBlocked(it, settings.filters) &&
                (!filters.duration.checkedLocally || filters.duration.matches(it.durationMs))
        }

    fun reload() = load(quiet = false)

    override fun refresh() = load(quiet = raw.value is Raw.Loaded)

    fun setFilters(filters: SearchFilters) {
        if (filters == this.filters.value) return
        viewModelScope.launch { SettingsRepository.updateCategory(id) { it.withFilters(filters) } }
    }

    private fun load(quiet: Boolean) {
        val current = category.value ?: return
        job?.cancel()
        autoPages = 0
        _refreshing.value = quiet
        if (!quiet) raw.value = Raw.Loading
        job = viewModelScope.launch {
            raw.value = runCatching { CategorySearch.first(current) }.fold(
                { results ->
                    CategoryEditing.storeThumbnails(id, results.videos)
                    Raw.Loaded(results, loadingMore = false)
                },
                { Raw.Error(it.toMessage()) },
            )
            _refreshing.value = false
            fillScreen()
        }
    }

    fun loadMore() {
        autoPages = 0
        more()
    }

    private fun more() {
        val loaded = raw.value as? Raw.Loaded ?: return
        if (loaded.loadingMore || _refreshing.value || !loaded.results.hasMore) return
        raw.value = loaded.copy(loadingMore = true)
        job = viewModelScope.launch {
            val next = runCatching { CategorySearch.more(loaded.results) }.getOrNull()
            raw.value = Raw.Loaded(next ?: CategoryResults(loaded.results.videos, emptyList()), loadingMore = false)
            fillScreen()
        }
    }

    /** Com "30 min ou mais" (e afins) o app corta resultados: se sobrou pouco, busca mais sozinho. */
    private fun fillScreen() {
        val loaded = raw.value as? Raw.Loaded ?: return
        if (!loaded.results.hasMore || autoPages >= MAX_AUTO_PAGES) return
        if (shown(loaded.results.videos, settings.value, filters.value).size >= MIN_ON_SCREEN) return
        autoPages++
        more()
    }

    fun edit(name: String, keywords: List<String>, filters: SearchFilters) {
        CategoryEditing.save(id, name, keywords, filters)
    }

    fun delete() = CategoryEditing.delete(id)

    private companion object {
        const val MIN_ON_SCREEN = 12
        const val MAX_AUTO_PAGES = 4
    }
}
