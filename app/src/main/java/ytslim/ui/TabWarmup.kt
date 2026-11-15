package ytslim.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * As abas que ainda não abriram, carregadas por trás depois das Inscrições (1.0.11, pedido do Johan:
 * "carregar de maneira bem inteligente", primeiro a parte de cima, que abre visível, e depois aos
 * poucos o resto, com o app sempre responsivo).
 *
 * - **Uma aba de cada vez** ([oneAtATime]): a seguinte só começa quando a anterior mostrou alguma
 *   coisa, para os pedidos não disputarem a rede e o processador com o que está na tela. Até a
 *   1.0.10 as abas começavam todas juntas.
 * - **A primeira tela na frente** ([firstScreen]): de cada lista só as miniaturas dos primeiros
 *   vídeos são baixadas antes de a aba abrir. O resto vem com a aba aberta, conforme a lista rola
 *   ([PrefetchThumbnails]).
 */
object TabWarmup {
    /** Os vídeos que aparecem ao abrir uma aba: duas fileiras de quatro na TV (no celular, sobra). */
    const val FIRST_SCREEN = 8

    /** Quanto esperar por uma aba antes de começar a seguinte (sem rede, ela não segura as outras). */
    const val STEP_TIMEOUT_MS = 4_000L

    /**
     * Abre as [tabs] em ordem: cada uma começa a carregar ao ser aberta, e a seguinte espera a
     * anterior sair do "carregando" (com a lista guardada no aparelho, é na hora) ou [timeoutMs].
     */
    suspend fun oneAtATime(tabs: List<() -> StateFlow<UiState<*>>>, timeoutMs: Long = STEP_TIMEOUT_MS) {
        tabs.forEachIndexed { index, open ->
            val state = open()
            // Depois da última não há o que esperar.
            if (index < tabs.lastIndex) withTimeoutOrNull(timeoutMs) { state.first { it !is UiState.Loading } }
        }
    }

    /**
     * As miniaturas da primeira tela de uma lista, cada vez que elas mudam (a lista guardada e,
     * depois, a da conta). Mais páginas chegando no fim não mudam nada aqui.
     */
    fun firstScreen(state: Flow<UiState<VideoListContent>>): Flow<List<String>> = state
        .map { ready -> (ready as? UiState.Ready)?.data?.videos.orEmpty().take(FIRST_SCREEN).mapNotNull { it.thumbnailUrl } }
        .distinctUntilChanged()
}
