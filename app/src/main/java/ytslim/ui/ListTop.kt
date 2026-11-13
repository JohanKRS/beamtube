package ytslim.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * O começo de uma grade de vídeos quando a lista muda.
 *
 * - Parada no começo, ela continua no começo quando chegam vídeos novos em cima (a lista da conta
 *   depois da guardada, o puxar para atualizar). Sozinha, a grade seguraria o primeiro vídeo de antes
 *   no topo, com os novos escondidos acima dele.
 * - Quando [VideoListContent.jumpToTop] muda (as Inscrições atualizadas sozinhas depois de o app
 *   ficar um tempo fora), ela sobe até o primeiro vídeo, de onde estiver.
 *
 * Nos dois casos [onTop] roda em seguida (na TV, o foco vai para o primeiro vídeo).
 */
@Composable
fun ListTop(grid: LazyGridState, content: VideoListContent, onTop: suspend () -> Unit = {}) {
    val firstKey = content.videos.firstOrNull()?.let { it.id + it.playlistIndex }
    val shown = remember { arrayOfNulls<String>(1) }
    val kept = remember { intArrayOf(0) }
    // Na composição, antes da grade medir a lista nova (é quando o requestScrollToItem vale).
    val keptTimes = remember(firstKey) {
        val before = shown[0]
        shown[0] = firstKey
        // Sem observar: ler a posição aqui faria a grade recompor a cada rolagem.
        val atTop = Snapshot.withoutReadObservation { grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset == 0 }
        if (before != null && firstKey != null && atTop) {
            grid.requestScrollToItem(0)
            ++kept[0]
        } else {
            kept[0]
        }
    }
    LaunchedEffect(keptTimes) { if (keptTimes > 0) onTop() }

    var jumped by rememberSaveable { mutableIntStateOf(content.jumpToTop) }
    LaunchedEffect(content.jumpToTop) {
        if (content.jumpToTop == jumped) return@LaunchedEffect
        jumped = content.jumpToTop
        grid.scrollToItem(0)
        onTop()
    }
}
