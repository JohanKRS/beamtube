package ytslim.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot

/**
 * O começo de uma grade de vídeos quando a lista muda.
 *
 * - Parada no começo, ela continua no começo quando chegam vídeos novos em cima (a lista da conta
 *   depois da guardada, o puxar para atualizar). Sozinha, a grade seguraria o primeiro vídeo de antes
 *   no topo, com os novos escondidos acima dele.
 * - Quando [VideoListContent.jumpToTop] muda (as Inscrições atualizadas sozinhas depois de o app
 *   ficar um tempo fora, ou as Recomendações ao tirar as fileiras antigas), ela sobe até o primeiro
 *   vídeo, de onde estiver.
 *
 * [onStartTop] roda ainda na composição, antes de a grade medir a lista nova (na TV, guarda se o
 * foco estava na grade antes de o card antigo sumir); [onTop] roda em seguida (o foco vai para o
 * primeiro vídeo).
 */
@Composable
fun ListTop(
    grid: LazyGridState,
    content: VideoListContent,
    onStartTop: () -> Unit = {},
    onTop: suspend () -> Unit = {},
) {
    val firstKey = content.videos.firstOrNull()?.let { it.id + it.playlistIndex }
    val shown = remember { arrayOfNulls<String>(1) }
    val topCount = remember { intArrayOf(0) }
    // IntArray salvo sem MutableState: atualiza na composição sem disparar recomposição extra.
    val jumped = rememberSaveable { intArrayOf(content.jumpToTop) }
    // Na composição, antes da grade medir a lista nova (é quando o requestScrollToItem vale).
    val goToTop = remember(firstKey, content.jumpToTop) {
        val before = shown[0]
        shown[0] = firstKey
        // ViewModel novo depois de o processo reiniciar: jumpToTop voltou a 0.
        if (content.jumpToTop < jumped[0]) jumped[0] = content.jumpToTop
        val jump = content.jumpToTop != jumped[0]
        if (jump) jumped[0] = content.jumpToTop
        // Sem observar: ler a posição aqui faria a grade recompor a cada rolagem.
        // Na TV, voltar até a primeira fileira pelo D-pad deixa o scrollOffset igual ao padding de
        // cima (GridTop, 8 dp), e não 0: isso continua sendo o topo da lista.
        val atTop = Snapshot.withoutReadObservation {
            grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset <= grid.layoutInfo.beforeContentPadding
        }
        val keep = before != null && firstKey != null && before != firstKey && atTop
        if (jump || keep) {
            onStartTop()
            grid.requestScrollToItem(0)
            ++topCount[0]
        } else {
            topCount[0]
        }
    }
    LaunchedEffect(goToTop) {
        if (goToTop == 0) return@LaunchedEffect
        if (grid.firstVisibleItemIndex != 0 || grid.firstVisibleItemScrollOffset != 0) grid.scrollToItem(0)
        onTop()
        // Mover o foco (ou o BringIntoView do card anterior) pode deslocar a grade: garante o topo.
        if (grid.firstVisibleItemIndex != 0 || grid.firstVisibleItemScrollOffset != 0) grid.scrollToItem(0)
    }
}
