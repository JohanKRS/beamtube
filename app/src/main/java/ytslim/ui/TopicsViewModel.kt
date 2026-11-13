package ytslim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ytslim.data.FirstRun
import ytslim.data.SettingsRepository
import ytslim.data.SuggestedTopic
import ytslim.data.Topic

/**
 * A tela de tópicos (1.0): no primeiro uso, depois do login, e pelos Ajustes. Dois passos, cada um
 * uma lista de sugestões para marcar: o que ver mais (sobe nas Recomendações) e o que ver menos
 * (sai das Recomendações e da Explore). Um tópico fica num passo só: o que foi para um some do outro.
 * Os tópicos criados à mão nos Ajustes não aparecem aqui e não são mexidos.
 */
class TopicsViewModel : ViewModel() {
    enum class Step { MORE, LESS }

    data class State(
        val step: Step = Step.MORE,
        val more: Set<SuggestedTopic> = emptySet(),
        val less: Set<SuggestedTopic> = emptySet(),
        val loaded: Boolean = false,
    ) {
        /** As sugestões do passo atual, sem as marcadas no outro. */
        val shown: List<SuggestedTopic>
            get() = when (step) {
                Step.MORE -> SuggestedTopic.forMore.filterNot { it in less }
                Step.LESS -> SuggestedTopic.forLess.filterNot { it in more }
            }

        val chosen: Set<SuggestedTopic> get() = if (step == Step.MORE) more else less
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val filters = SettingsRepository.current().filters
            _state.update { s ->
                s.copy(
                    more = filters.preferred.mapNotNullTo(LinkedHashSet()) { SuggestedTopic.byId(it.id) },
                    less = filters.excluded.mapNotNullTo(LinkedHashSet()) { SuggestedTopic.byId(it.id) },
                    loaded = true,
                )
            }
        }
    }

    fun toggle(topic: SuggestedTopic) = _state.update { s ->
        when (s.step) {
            Step.MORE -> s.copy(more = s.more.toggled(topic))
            Step.LESS -> s.copy(less = s.less.toggled(topic))
        }
    }

    fun next() = _state.update { it.copy(step = Step.LESS) }

    /** Do segundo passo para o primeiro. false se já está no primeiro (aí o voltar sai da tela). */
    fun back(): Boolean {
        if (_state.value.step == Step.MORE) return false
        _state.update { it.copy(step = Step.MORE) }
        return true
    }

    /**
     * Grava as escolhas. [name] dá o nome do tópico no idioma do app. Um tópico sugerido que já
     * estava na lista continua como estava (as palavras podem ter sido mudadas nos Ajustes).
     */
    fun save(name: (SuggestedTopic) -> String, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val filters = SettingsRepository.current().filters
            fun merge(current: List<Topic>, chosen: Set<SuggestedTopic>): List<Topic> {
                val own = current.filter { SuggestedTopic.byId(it.id) == null }
                val kept = current.associateBy { it.id }
                val picked = SuggestedTopic.entries.filter { it in chosen }.map { kept[it.id] ?: it.topic(name(it)) }
                return picked + own
            }
            SettingsRepository.setTopics(preferred = merge(filters.preferred, s.more), excluded = merge(filters.excluded, s.less))
            FirstRun.setTopicsChosen()
            onDone()
        }
    }

    /** Primeiro uso: seguir sem escolher nada (a tela não volta a abrir sozinha). */
    fun skip(onDone: () -> Unit) {
        FirstRun.setTopicsChosen()
        onDone()
    }

    private fun Set<SuggestedTopic>.toggled(topic: SuggestedTopic) = if (topic in this) this - topic else this + topic
}
