package ytslim.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import ytslim.data.Video

/**
 * A lista nova que chega com a guardada na tela entra **logo abaixo do que a pessoa já viu**
 * (1.0.11, ideia do Johan: a guardada aparecia e logo era trocada pela da conta, bem na hora de
 * escolher um vídeo). As fileiras vistas ficam onde estão, e o que vem abaixo delas já é a lista
 * nova; quando a aba sai da tela, as antigas saem e fica só a nova. Com a aba fora da tela, a nova
 * entra inteira na hora.
 */
object FreshBelow {
    /** Sem saber o que foi visto (a grade ainda não mediu), ficam as três primeiras fileiras da TV. */
    const val FIRST_ROWS = 12

    /** Os vídeos da lista da tela ([shown]) que ficam no lugar: os [seen] primeiros, até onde a pessoa viu. */
    fun keep(shown: List<Video>, seen: Int): List<Video> = shown.take(if (seen > 0) seen else FIRST_ROWS)

    /**
     * A lista como fica guardada para a próxima abertura: na frente os vídeos que a pessoa ainda
     * não viu (os de mais abaixo), e os já vistos ([seen], pelo id) depois, cada grupo na ordem de
     * antes. Assim, se a lista guardada aparecer antes da nova, ela não abre igual à da última vez
     * (sugestão do Johan, 1.0.11).
     */
    fun unseenFirst(videos: List<Video>, seen: Set<String>): List<Video> {
        val (old, new) = videos.partition { it.id in seen }
        return new + old
    }
}

/**
 * Avisa até onde a grade já mostrou: quantos itens, contando do começo, até o último da última
 * fileira na tela (uma fileira cortada conta inteira). Para o [FreshBelow].
 */
@Composable
fun SeenRows(grid: LazyGridState, onSeen: (Int) -> Unit) {
    val report by rememberUpdatedState(onSeen)
    LaunchedEffect(grid) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { report(it + 1) }
    }
}
